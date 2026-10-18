#!/usr/bin/env bash
# Starts a Kalimory release: checks that master is ready, then tags v<versionName> and pushes the
# tag. The Release workflow (.github/workflows/release.yml) then runs every check, builds and
# signs the APK, verifies it and drafts the GitHub release; you read the draft and publish it.
#
#   scripts/release.sh --dry-run   # the checks and the release notes, but no tag
#   scripts/release.sh             # the same, then tags and pushes
#
# Before running, bump versionCode/versionName, date the CHANGELOG entry and add its links, and add
# fastlane/metadata/android/en-US/changelogs/<versionCode>.txt, then merge to master (AGENTS.md,
# "Versions and releases"). Needs git and python3; nothing secret is read or printed.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "release: $*" >&2; exit 1; }
step() { echo; echo "== $*"; }

dry_run=false
[ "${1:-}" = "--dry-run" ] && dry_run=true

step "Checking the repository"
[ "$(git rev-parse --abbrev-ref HEAD)" = "master" ] || fail "release from master"
[ -z "$(git status --porcelain)" ] || fail "the working tree is not clean"
git fetch --quiet origin master --tags
[ "$(git rev-parse HEAD)" = "$(git rev-parse origin/master)" ] || fail "master is not in sync with origin/master"
version_name="$(scripts/release-checks.sh)"
tag="v$version_name"
git rev-parse --quiet --verify "refs/tags/$tag" >/dev/null && fail "tag $tag already exists"
previous="$(git describe --tags --abbrev=0 --match 'v*' HEAD 2>/dev/null || true)"
[ -n "$previous" ] || fail "no previous v* tag to compare with"

step "Release notes for $tag"
python3 -I scripts/release-notes.py "$version_name" "$previous" "$(cat .github/release-certificate.sha256)"

if $dry_run; then
    step "Dry run: no tag was created"
    exit 0
fi

step "Tagging $tag"
git tag -a "$tag" -m "$tag"
git push origin "$tag"
echo
echo "The Release workflow is building $tag: https://github.com/WildeBeast2521/kalimory/actions/workflows/release.yml"
echo "When it finishes, read the draft at https://github.com/WildeBeast2521/kalimory/releases and publish it,"
echo "and download the mapping-$version_name artifact from the run to read future stack traces."
