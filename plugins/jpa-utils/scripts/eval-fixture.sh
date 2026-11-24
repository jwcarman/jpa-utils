#!/usr/bin/env bash
# The eval fixture: a new Spring Boot app (evals/fixture/) that uses jpa-utils as built at <commit>,
# written into <dir> (outside the repository) so it builds with no network and no home directory, as
# in an eval's sandbox:
#   - jpa-utils at <commit> installed into <dir>/.m2repo with its sources jar (as Maven Central has
#     it, and agents read it), with every artifact the app's build needs
#     (filled by one online build); the copy's ./mvnw points Maven at it;
#   - its own Maven and JDK under .mvn/, run by ./mvnw (the wrapper would download Maven into
#     ~/.m2, which the sandbox cannot read);
#   - git init and one commit, so a run's changes show as a diff.
# Then eval-preflight.sh proves an offline build in the sandbox. Prints <dir>.
#
# Usage: JAVA_HOME=<JDK 25> eval-fixture.sh <commit> <dir>
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "usage: $(basename "$0") <commit> <dir>" >&2
  exit 2
fi
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd -P)"
commit="$(git -C "$root" rev-parse "$1")"
dir="$2"
[ ! -e "$dir" ] || { echo "eval-fixture: $dir exists" >&2; exit 1; }
[ -x "${JAVA_HOME:-}/bin/java" ] || { echo "eval-fixture: set JAVA_HOME to a JDK 25" >&2; exit 1; }
mkdir -p "$dir"
dir="$(cd "$dir" && pwd -P)"
work="$(mktemp -d)"
trap 'git -C "$root" worktree remove --force "$work/jpa-utils" 2>/dev/null; rm -rf "$work"' EXIT

echo "installing jpa-utils ${commit:0:7} into the fixture's repository" >&2
git -C "$root" worktree add -q --detach "$work/jpa-utils" "$commit"
version="$(cd "$work/jpa-utils" && MAVEN_USER_HOME="$work/mvnhome" ./mvnw -B -q help:evaluate \
  -Dexpression=project.version -DforceStdout "-Dmaven.repo.local=$dir/.m2repo")"
(cd "$work/jpa-utils" && MAVEN_USER_HOME="$work/mvnhome" ./mvnw -B -q org.apache.maven.plugins:maven-source-plugin:3.3.1:jar-no-fork install -DskipTests -DskipITs \
  -Dmaven.javadoc.skip=true -Dgpg.skip=true "-Dmaven.repo.local=$dir/.m2repo") >&2

cp -R "$root/plugins/jpa-utils/evals/fixture/." "$dir/"
sed -i.bak "s|<jpa-utils.version>[^<]*</jpa-utils.version>|<jpa-utils.version>$version</jpa-utils.version>|" "$dir/pom.xml"
rm -f "$dir/pom.xml.bak"

mkdir -p "$dir/.mvn"
maven_home="$(dirname "$(dirname "$(find "$work/mvnhome/wrapper/dists" -type f -path '*/bin/mvn' | head -n 1)")")"
[ -x "$maven_home/bin/mvn" ] || { echo "eval-fixture: no Maven under $work/mvnhome" >&2; exit 1; }
cp -R "$maven_home" "$dir/.mvn/maven"
cp -R "$JAVA_HOME" "$dir/.mvn/jdk"
cat >"$dir/mvnw" <<'MVNW'
#!/bin/sh
# Maven and a JDK, bundled with this copy of the app so it builds without a download.
base="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="$base/.mvn/jdk"
PATH="$JAVA_HOME/bin:$PATH"
export JAVA_HOME PATH
mkdir -p "$base/.mvn/home"
# The repository is the copy's own wherever the copy is (this Maven does not expand variables in
# .mvn/jvm.config).
MAVEN_OPTS="-Duser.home=$base/.mvn/home -Dmaven.repo.local=$base/.m2repo ${MAVEN_OPTS:-}"
export MAVEN_OPTS
exec "$base/.mvn/maven/bin/mvn" "$@"
MVNW
chmod +x "$dir/mvnw"
printf '%s\n' '.m2repo/' '.mvn/maven/' '.mvn/jdk/' '.mvn/home/' >>"$dir/.gitignore"

echo "building the fixture once, to fill its repository" >&2
(cd "$dir" && ./mvnw -B -q clean verify) >&2
(cd "$dir" && ./mvnw -B -q -o clean verify) >&2
rm -rf "$dir/target"

git -C "$dir" init -q -b main
git -C "$dir" add -A
git -C "$dir" -c user.name=eval-fixture -c user.email=eval-fixture@example.invalid \
  commit -q -m "A new app, with jpa-utils ${commit:0:7}"

"$root/plugins/jpa-utils/scripts/eval-preflight.sh" "$dir"
echo "$dir"
