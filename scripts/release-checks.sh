#!/usr/bin/env bash
# The checks a release must pass before anything is built, shared by scripts/release.sh (before it
# tags) and .github/workflows/release.yml (before it signs). Prints the version it checked.
#
#   scripts/release-checks.sh [tag]   # with a tag, also checks that it names the versionName
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "release-checks: $*" >&2; exit 1; }

version_name="$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' app/build.gradle.kts)"
version_code="$(sed -n 's/^ *versionCode = \([0-9]*\)/\1/p' app/build.gradle.kts)"
[ -n "$version_name" ] && [ -n "$version_code" ] || fail "cannot read the version from app/build.gradle.kts"
if [ -n "${1:-}" ] && [ "$1" != "v$version_name" ]; then
    fail "tag $1 does not match versionName $version_name in app/build.gradle.kts"
fi

github="https://github.com/WildeBeast2521/kalimory"
grep -q "^## \[$version_name\] - [0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}$" CHANGELOG.md \
    || fail "CHANGELOG.md needs a dated entry: ## [$version_name] - YYYY-MM-DD"
# Keep a Changelog 1.1.0 and every link, including [Unreleased] comparing this version with HEAD.
python3 -I scripts/changelog-links.py --check >/dev/null \
    || fail "CHANGELOG.md breaks the rules; see python3 -I scripts/changelog-links.py --check"
grep -qxF "[Unreleased]: $github/compare/v$version_name...HEAD" CHANGELOG.md \
    || fail "$version_name is not the newest version in CHANGELOG.md"

changelog_file="fastlane/metadata/android/en-US/changelogs/$version_code.txt"
[ -f "$changelog_file" ] || fail "$changelog_file is missing"
[ "$(wc -c < "$changelog_file")" -le 500 ] || fail "$changelog_file is longer than 500 characters"
if grep -qiE "claude|generated with|co-authored" "$changelog_file"; then
    fail "$changelog_file contains tool attribution"
fi

# The notes render: the section exists, is not empty and carries no attribution.
python3 -I scripts/release-notes.py "$version_name" > /dev/null

echo "$version_name"
