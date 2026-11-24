#!/usr/bin/env bash
# Runs a measurement: eval cases under `claude plugin eval`, two arms, then scores every run by
# script into files score.py reads. One arm (a baseline, or any single version of the skill):
#   --ablation none                       the plugin as it is, B batches of N runs
# Two ways to have two arms:
#   one plugin, --ablation with-without   the eval runs each case with and without the plugin
#   --a <plugin dir> --b <plugin dir>     two versions of the skill (e.g. --a a git worktree at the
#                                         old commit), alternated in batches: A, B, A, B, ...
# Every run's work is scored on a copy carrying the fixture's untouched .git:
#   <batch>/<case>-<arm>-<n>.accept.txt   the case's hidden tests (PASS accept:... / FAIL accept:...)
#   <batch>/<case>-<arm>-<n>.check.txt    the fixture's own build (PASS build / FAIL build)
#   <batch>/<case>-<arm>-<n>.diff         what the run changed
#   <batch>/<case>-<arm>-<n>.trace.jsonl  the run's transcript
#
# Usage: run-evals.sh --a <plugin dir> [--b <plugin dir>] --fixture <dir> [--batches B] [--runs N]
#                     [--model M] [--ablation none] [--dry-run] <out dir> <case>...
#   Without --b: one batch per case, both arms (with and without --a's plugin), N runs each.
#   With --ablation none: B batches of N runs into <out>/b1, <out>/b2, ... (score.py <out> 'b*').
#   With --b: B batches per arm of N runs each, alternating, into <out>/a-b1, <out>/b-b1, ...
set -uo pipefail

# ADAPT: the scorers. ACCEPT runs a case's hidden tests: `$ACCEPT <case dir> <work dir>` printing
# one PASS/FAIL accept:<test> line per test. BUILD runs the fixture's build: `$BUILD <work dir>`
# printing "PASS build" or "FAIL build" (and anything else you check, as PASS/FAIL lines).
ACCEPT="${ACCEPT:-$(cd "$(dirname "$0")/../evals/scripts" && pwd)/accept.sh}"
BUILD="${BUILD:-$(cd "$(dirname "$0")/../evals/scripts" && pwd)/build.sh}"
# ADAPT: paths left out of each run's diff (generated files that only add noise).
diff_exclude=(":!.mvn/home")

a="" b="" fixture="" batches=1 runs=5 model=haiku dry=false single=false
while [ "$#" -gt 0 ]; do
  case "$1" in
    --a) a="$2"; shift 2 ;;
    --b) b="$2"; shift 2 ;;
    --fixture) fixture="$2"; shift 2 ;;
    --batches) batches="$2"; shift 2 ;;
    --runs) runs="$2"; shift 2 ;;
    --model) model="$2"; shift 2 ;;
    --dry-run) dry=true; shift ;;
    --ablation)
      [ "${2:-}" = none ] || { echo "run-evals: --ablation takes only none (one arm)" >&2; exit 2; }
      single=true; shift 2 ;;
    *) break ;;
  esac
done
if [ -z "$a" ] || [ -z "$fixture" ] || [ "$#" -lt 2 ] || { $single && [ -n "$b" ]; }; then
  echo "usage: $(basename "$0") --a <plugin dir> [--b <plugin dir> | --ablation none] --fixture <dir> [--batches B] [--runs N] [--model M] [--dry-run] <out dir> <case>..." >&2
  exit 2
fi
out="$1"; shift
[ -d "$fixture/.git" ] || { echo "run-evals: $fixture has no .git (a run's diff is against it)" >&2; exit 2; }

# One `claude plugin eval` call, then scoring of each run it made.
evaluate() { # <plugin dir> <ablation> <batch dir> <case>
  local plugin="$1" ablation="$2" batch="$3" case="$4"
  if $dry; then echo "eval $(basename "$batch") $case plugin=$plugin ablation=$ablation runs=$runs model=$model"; return; fi
  mkdir -p "$batch"
  echo "== $(basename "$batch") $case $(date +%T)"
  (cd "$plugin" && claude plugin eval . --scaffold --trust-plugin --ablation "$ablation" --runs "$runs" \
    --model "$model" --concurrency 3 --case "$case" --allow-tools Bash Write Edit --no-publish \
    --keep-temp --json "$batch/$case.json" --output-dir "$batch/$case-out" >"$batch/$case.log" 2>&1)
  echo "eval exit $?"
  python3 -c '
import json, sys
for c in json.load(open(sys.argv[1]))["cases"]:
    for arm, rs in c["arms"].items():
        for i, r in enumerate(rs):
            print(arm, i + 1, (r.get("tracePath") or "").rsplit("/out/", 1)[0] or "-")
' "$batch/$case.json" | while read -r arm n kept; do
    local tag="$batch/$case-$arm-$n" work
    if [ "$kept" = "-" ] || [ ! -d "$kept" ]; then
      echo "FAIL build (the run left no work)" >"$tag.check.txt"; continue
    fi
    chmod 700 "$kept" "$kept/sealed" 2>/dev/null
    cp "$kept/out/trace.jsonl" "$tag.trace.jsonl" 2>/dev/null
    work="$(mktemp -d)/work"
    rsync -a --exclude /.git "$kept/sealed/home/cwd/" "$work/"
    cp -R "$fixture/.git" "$work/.git"
    (cd "$work" && git add -A -N . && git diff -- . "${diff_exclude[@]}") >"$tag.diff" 2>/dev/null
    git -C "$work" reset -q
    [ -d "$plugin/evals/$case/acceptance" ] && "$ACCEPT" "$plugin/evals/$case" "$work" >"$tag.accept.txt" 2>&1
    "$BUILD" "$work" >"$tag.check.txt" 2>&1
    local passed total
    passed="$(grep -c '^PASS' "$tag.accept.txt" 2>/dev/null)"; total="$(grep -cE '^(PASS|FAIL)' "$tag.accept.txt" 2>/dev/null)"
    echo "  $(basename "$tag"): accept ${passed:-0}/${total:-0}, $(grep -m1 -E '^(PASS|FAIL) build' "$tag.check.txt")"
    rm -rf "$(dirname "$work")"
    chmod -R u+w "$kept" 2>/dev/null; rm -rf "$kept"
  done
}

if $single; then
  for i in $(seq 1 "$batches"); do
    for case in "$@"; do evaluate "$a" none "$out/b$i" "$case"; done
  done
elif [ -z "$b" ]; then
  for case in "$@"; do evaluate "$a" with-without "$out/b1" "$case"; done
else
  for i in $(seq 1 "$batches"); do
    for case in "$@"; do
      evaluate "$a" none "$out/a-b$i" "$case"
      evaluate "$b" none "$out/b-b$i" "$case"
    done
  done
fi
$dry || echo "done $(date +%T); score: score.py $out 'a-b*' 'b-b*' (or 'b1:without' 'b1:with'; one arm: 'b*')"
