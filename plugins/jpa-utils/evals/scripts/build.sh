#!/usr/bin/env bash
# The fixture's own build on a run's work, as it stands (its own tests included), offline.
# Prints "PASS build" or "FAIL build" followed by the first error lines.
#
# Usage: build.sh <work dir>
set -uo pipefail

if [ "$#" -ne 1 ] || [ ! -x "$1/mvnw" ]; then
  echo "usage: $(basename "$0") <work dir>" >&2
  exit 2
fi
copy="$(mktemp -d)/work"
trap 'rm -rf "$(dirname "$copy")"' EXIT
cp -R "$1" "$copy"
if (cd "$copy" && rm -rf target && ./mvnw -B -o verify) >"$copy.log" 2>&1; then
  echo "PASS build"
else
  echo "FAIL build"
  grep -E '^\[ERROR\]' "$copy.log" | grep -v -E 'Help 1|^\[ERROR\] *$|re-run Maven|full stack trace|possible solutions' | head -n 12
fi
