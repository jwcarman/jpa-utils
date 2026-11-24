#!/usr/bin/env bash
# Proves an eval fixture builds in the eval's sandbox before any run is spent on it: on a copy, under
# macOS's sandbox-exec with what `claude plugin eval`'s sandbox takes away (no reading of the home
# directory, every request sent to a proxy that refuses it), `./mvnw -B -o verify` must pass. The
# fixture itself is never touched.
#
# Usage: eval-preflight.sh <fixture dir>
set -euo pipefail

if [ "$#" -ne 1 ] || [ ! -x "$1/mvnw" ]; then
  echo "usage: $(basename "$0") <fixture dir>" >&2
  exit 2
fi
fixture="$(cd "$1" && pwd -P)"
if ! command -v sandbox-exec >/dev/null; then
  echo "eval-preflight: no sandbox-exec (macOS only); the fixture is not proven" >&2
  exit 0
fi
copy="$(mktemp -d)/fixture"
cp -R "$fixture" "$copy"
trap 'rm -rf "$(dirname "$copy")"' EXIT
export HTTP_PROXY=http://127.0.0.1:9 HTTPS_PROXY=http://127.0.0.1:9 http_proxy=http://127.0.0.1:9 https_proxy=http://127.0.0.1:9
profile="(version 1)(allow default)(deny file-read* (subpath \"$HOME\"))"
log="$(dirname "$copy")/preflight.log"
if (cd "$copy" && sandbox-exec -p "$profile" ./mvnw -B -o verify) >"$log" 2>&1 && ! grep -q '^\[ERROR\]' "$log"; then
  echo "eval-preflight: ok   ./mvnw -o verify in the sandbox" >&2
else
  echo "eval-preflight: FAIL ./mvnw -o verify in the sandbox" >&2
  grep -E '^\[ERROR\]|sandbox|denied' "$log" | head -n 15 | sed 's/^/  /' >&2
  exit 1
fi
