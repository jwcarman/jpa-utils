#!/usr/bin/env bash
# The case's scaffold: the new app from $JPA_EVAL_FIXTURE (or evals/fixture.local, since claude plugin
# eval runs this with a cleaned environment), with books-list's reference solution laid over it and
# committed, so the run starts from an app that already lists books.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
local_file="$here/../fixture.local"
if [ -z "${JPA_EVAL_FIXTURE:-}" ] && [ -f "$local_file" ]; then JPA_EVAL_FIXTURE="$(head -n 1 "$local_file")"; fi
: "${JPA_EVAL_FIXTURE:?set JPA_EVAL_FIXTURE or write evals/fixture.local: a directory eval-fixture.sh wrote}"
[ -d "$JPA_EVAL_FIXTURE/.git" ] || { echo "$JPA_EVAL_FIXTURE is not a fixture (no .git)" >&2; exit 1; }
cp -R "$JPA_EVAL_FIXTURE/." .
cp -R "$here/../books-list/reference/." .
git add -A
git -c user.name=eval-fixture -c user.email=eval-fixture@example.invalid commit -q -m "Books, listed"
