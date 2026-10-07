#!/usr/bin/env bash
# Builds, verifies and publishes a Kalimory release on GitHub from the current master.
#
#   scripts/release.sh --dry-run   # every check and the signed APK, but no tag and no release
#   scripts/release.sh             # the same, then tags v<versionName> and creates the release
#
# The version comes from app/build.gradle.kts. Before running, bump versionCode/versionName,
# date the CHANGELOG entry ("## [x.y.z] - YYYY-MM-DD") and add
# fastlane/metadata/android/en-US/changelogs/<versionCode>.txt, then merge to master.
#
# Needs: JDK 17+, the Android SDK (ANDROID_HOME), an API 29 emulator for the instrumented tests
# (ANDROID_SERIAL, default emulator-5554), the gh CLI signed in, and keystore.properties pointing
# at the release key. Nothing secret is printed or committed.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { echo "release: $*" >&2; exit 1; }
step() { echo; echo "== $*"; }

# Gradle can use JAVA_HOME directly, but Android build-tools launchers such as apksigner
# execute `java` through PATH. Make both resolution paths consistent.
[ -n "${JAVA_HOME:-}" ] || fail "JAVA_HOME is not set (JDK 17+ required)"
[ -x "$JAVA_HOME/bin/java" ] || fail "JAVA_HOME/bin/java is not executable: $JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

dry_run=false
[ "${1:-}" = "--dry-run" ] && dry_run=true

# The release certificate's fingerprint (public, not a secret). Android refuses updates signed
# with another key, so a mismatch stops the release.
expected_cert="bf768448fb8d632ffcc3c08321912cad47426fc2a094aa796e33ccf6cb083eae"
export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5554}"
build_tools="$(ls -d "${ANDROID_HOME:?ANDROID_HOME is not set}"/build-tools/* | sort -V | tail -1)"

version_name="$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' app/build.gradle.kts)"
version_code="$(sed -n 's/^ *versionCode = \([0-9]*\)/\1/p' app/build.gradle.kts)"
tag="v$version_name"
[ -n "$version_name" ] && [ -n "$version_code" ] || fail "cannot read the version from app/build.gradle.kts"

step "Checking the repository for $tag (versionCode $version_code)"
[ "$(git rev-parse --abbrev-ref HEAD)" = "master" ] || fail "release from master"
[ -z "$(git status --porcelain)" ] || fail "the working tree is not clean"
git fetch --quiet origin master --tags
[ "$(git rev-parse HEAD)" = "$(git rev-parse origin/master)" ] || fail "master is not in sync with origin/master"
git rev-parse --quiet --verify "refs/tags/$tag" >/dev/null && fail "tag $tag already exists"
[ -f keystore.properties ] || fail "keystore.properties is missing; the release must be signed"
grep -q "^## \[$version_name\] - [0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}$" CHANGELOG.md \
    || fail "CHANGELOG.md needs a dated entry: ## [$version_name] - YYYY-MM-DD"
changelog_file="fastlane/metadata/android/en-US/changelogs/$version_code.txt"
[ -f "$changelog_file" ] || fail "$changelog_file is missing"
[ "$(wc -c < "$changelog_file")" -le 500 ] || fail "$changelog_file is longer than 500 characters"
adb get-state >/dev/null 2>&1 || fail "no emulator at $ANDROID_SERIAL for the instrumented tests"

step "Room schemas"
scripts/check-room-schemas.sh --stacktrace

step "Tests, lint and builds"
adb shell pm clear io.github.wildebeast2521.kalimory >/dev/null 2>&1 || true
./gradlew clean
./gradlew testDebugUnitTest lintDebug assembleDebug :app:connectedDebugAndroidTest assembleRelease --stacktrace

apk="app/build/outputs/apk/release/app-release.apk"
[ -f "$apk" ] || fail "no signed release APK at $apk"

step "Verifying the APK"
cert="$("$build_tools/apksigner" verify --print-certs "$apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
[ "$cert" = "$expected_cert" ] || fail "signed with an unexpected certificate"
badging="$("$build_tools/aapt2" dump badging "$apk")"
grep -q "package: name='io.github.wildebeast2521.kalimory' versionCode='$version_code' versionName='$version_name'" <<<"$badging" \
    || fail "package name or version in the APK does not match"
if "$build_tools/aapt2" dump permissions "$apk" | grep -q "android.permission.INTERNET"; then
    fail "the APK requests INTERNET"
fi

out="build/release"
mkdir -p "$out"
asset="$out/Kalimory-$version_name.apk"
cp "$apk" "$asset"
(cd "$out" && sha256sum "Kalimory-$version_name.apk" > "Kalimory-$version_name.apk.sha256")
# R8's mapping file turns a reported stack trace back into source names. It stays with the
# maintainer (it is large and not needed by users), next to the APK it belongs to.
cp app/build/outputs/mapping/release/mapping.txt "$out/mapping-$version_name.txt"

# Release notes: the version's CHANGELOG section, without its heading.
notes="$out/notes-$version_name.md"
awk -v v="$version_name" '
    $0 ~ "^## \\[" v "\\]" { on = 1; next }
    on && /^## \[/ { exit }
    on { print }
' CHANGELOG.md > "$notes"
[ -s "$notes" ] || fail "the CHANGELOG section for $version_name is empty"
# The release page is the CHANGELOG section plus a fixed footer (see AGENTS.md, "Changelog and
# release notes"); nothing else is added by hand, and no tool attribution belongs in it.
previous_tag="$(git describe --tags --abbrev=0 --match 'v*' HEAD 2>/dev/null || true)"
{
    echo
    echo "---"
    echo
    [ -n "$previous_tag" ] && echo "**Full changelog:** https://github.com/WildeBeast2521/kalimory/compare/$previous_tag...$tag" && echo
    echo "**Verify the download:** \`sha256sum -c Kalimory-$version_name.apk.sha256\`. The APK is signed with the release key, certificate SHA-256 \`$expected_cert\`."
} >> "$notes"
if grep -qiE "claude|generated with|co-authored" "$notes"; then
    fail "the release notes contain tool attribution; remove it from CHANGELOG.md"
fi

echo "APK: $asset"
cat "$out/Kalimory-$version_name.apk.sha256"

if $dry_run; then
    step "Dry run: no tag and no release were created"
    exit 0
fi

step "Tagging $tag and publishing the GitHub release"
git tag -a "$tag" -m "Kalimory $version_name"
git push origin "$tag"
gh release create "$tag" "$asset" "$out/Kalimory-$version_name.apk.sha256" \
    --title "Kalimory $version_name" --notes-file "$notes" --verify-tag
echo "Released $tag"
