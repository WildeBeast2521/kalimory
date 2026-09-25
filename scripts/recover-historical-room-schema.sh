#!/usr/bin/env bash
# Generates the Room schema of a historical commit in a disposable worktree.
#
# The only changes made to the historical sources, inside the disposable
# worktree, are:
#   1. exportSchema = false -> exportSchema = true in AppDatabase.kt
#   2. a ksp { arg("room.schemaLocation", "$projectDir/schemas") } block
#      appended to app/build.gradle.kts
# The worktree is removed afterwards. The script never writes to app/schemas;
# review the output, then copy it there.
#
# Usage: scripts/recover-historical-room-schema.sh <commit> <expected-version> <output-dir>
set -euo pipefail

[ $# -eq 3 ] || { echo "usage: $0 <commit> <expected-version> <output-dir>" >&2; exit 2; }
commit=$1
version=$2
output_dir=$(mkdir -p "$3" && cd "$3" && pwd)

repo=$(cd "$(dirname "$0")/.." && git rev-parse --show-toplevel)
worktree=$(mktemp -d "${TMPDIR:-/tmp}/room-schema-$commit.XXXXXX")
rmdir "$worktree"
git -C "$repo" worktree add --detach "$worktree" "$commit" >/dev/null
trap 'git -C "$repo" worktree remove --force "$worktree"' EXIT

if [ -f "$repo/local.properties" ]; then
    cp "$repo/local.properties" "$worktree/local.properties"
fi

database="$worktree/app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt"
grep -qE "^[[:space:]]*version[[:space:]]*=[[:space:]]*$version([^0-9]|$)" "$database" \
    || { echo "error: $commit does not declare database version $version" >&2; exit 1; }
sed -i 's/exportSchema = false/exportSchema = true/' "$database"
grep -q "exportSchema = true" "$database" \
    || { echo "error: could not enable exportSchema in $commit" >&2; exit 1; }
printf '\nksp {\n    arg("room.schemaLocation", "$projectDir/schemas")\n}\n' >> "$worktree/app/build.gradle.kts"

echo "Temporary changes in $commit:"
git -C "$worktree" diff --stat

(cd "$worktree" && ./gradlew --no-daemon -q :app:kspDebugKotlin)

generated=$(find "$worktree/app/schemas" -name '*.json')
if [ "$(printf '%s\n' "$generated" | wc -l)" -ne 1 ] || [ "$(basename "$generated")" != "$version.json" ]; then
    echo "error: unexpected schema output: $generated" >&2
    exit 1
fi
cp "$generated" "$output_dir/$version.json"
echo "Wrote $output_dir/$version.json from $commit"
