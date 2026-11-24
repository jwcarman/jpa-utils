#!/usr/bin/env bash
# A case's hidden acceptance tests on a run's work: on a copy, the run's own tests are put back as
# the fixture had them (a run's broken test must not hide its code's result), the case's
# acceptance/java is added, and only those tests run. Prints one line per hidden test:
#   PASS accept:<Class>.<test>   or   FAIL accept:<Class>.<test> <first line of why>
# When the code does not compile, every hidden test is a FAIL carrying the compile error.
#
# Usage: accept.sh <case dir> <work dir (with the fixture's .git)>
set -uo pipefail

if [ "$#" -ne 2 ] || [ ! -d "$1/acceptance/java" ] || [ ! -x "$2/mvnw" ]; then
  echo "usage: $(basename "$0") <case dir> <work dir>" >&2
  exit 2
fi
case_dir="$(cd "$1" && pwd)"
copy="$(mktemp -d)/work"
trap 'rm -rf "$(dirname "$copy")"' EXIT
cp -R "$2" "$copy"
cd "$copy"
rm -rf src/test target
git checkout -q HEAD -- src/test 2>/dev/null || true
mkdir -p src/test/java
cp -R "$case_dir/acceptance/java/." src/test/java/

./mvnw -B -o test -Dtest='app.acceptance.**' -Dsurefire.failIfNoSpecifiedTests=false >mvn.log 2>&1

python3 - "$case_dir/acceptance/java" target/surefire-reports mvn.log <<'PY'
import glob, os, re, sys
import xml.etree.ElementTree as ET
src, reports, log = sys.argv[1:4]
expected = []
for path in sorted(glob.glob(os.path.join(src, "**", "*.java"), recursive=True)):
    cls = os.path.basename(path)[:-5]
    text = open(path).read()
    for m in re.finditer(r"@Test\s+(?:public\s+)?void\s+(\w+)\s*\(", text):
        expected.append(f"{cls}.{m.group(1)}")
results = {}
for path in glob.glob(os.path.join(reports, "TEST-*.xml")):
    for tc in ET.parse(path).getroot().iter("testcase"):
        cls = tc.get("classname", "").rsplit(".", 1)[-1]
        name = f"{cls}.{tc.get('name', '').split('(')[0]}"
        bad = tc.find("failure")
        if bad is None:
            bad = tc.find("error")
        if bad is None and tc.find("skipped") is not None:
            results[name] = "skipped"
        elif bad is None:
            results[name] = None
        else:
            why = (bad.get("message") or bad.get("type") or "failed").strip().splitlines()
            results[name] = (why[0] if why else "failed")[:200]
lines = open(log, errors="replace").read().splitlines()
compile_err = next((l for l in lines if l.startswith("[ERROR]") and ".java:[" in l), None) or next(
    (l for l in lines if l.startswith("[ERROR]") and "COMPILATION" in l), None)
startup = next((l for l in lines if "APPLICATION FAILED TO START" in l or "Failed to load ApplicationContext" in l), None)
for name in expected:
    if name in results and results[name] is None:
        print(f"PASS accept:{name}")
    elif name in results:
        print(f"FAIL accept:{name} {results[name]}")
    else:
        why = compile_err or startup or "did not run"
        print(f"FAIL accept:{name} {why[:200]}")
PY
