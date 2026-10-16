#!/usr/bin/env bash
# Regenerates gradle/verification-metadata.xml, the SHA-256 checksums Gradle checks for every
# dependency, plugin and tool it downloads. Run it after any dependency change (including a
# Dependabot pull request), then review the diff: each changed checksum should belong to a
# dependency the change meant to update.
#
# It starts from an empty list, so entries nothing uses any more disappear, then runs the tasks
# that resolve everything: the build, lint, unit tests (with Robolectric's framework jar), the
# Baseline Profile module and one instrumented test (Android's test runner is only resolved
# then). aapt2 ships one jar per operating system and Gradle records only this machine's, so the
# Linux, macOS and Windows checksums are added from Google's Maven repository.
#
# Needs: JDK 17+, the Android SDK, network access and an emulator or device (ANDROID_SERIAL,
# default emulator-5554).
# Usage: scripts/update-dependency-verification.sh [extra Gradle arguments]
set -euo pipefail

cd "$(dirname "$0")/.."

export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5554}"
metadata="gradle/verification-metadata.xml"

fail() {
    echo "error: $*" >&2
    exit 1
}

adb get-state >/dev/null 2>&1 || fail "no device at $ANDROID_SERIAL for the instrumented test"

cat > "$metadata" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<verification-metadata xmlns="https://schema.gradle.org/dependency-verification" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="https://schema.gradle.org/dependency-verification https://schema.gradle.org/dependency-verification/dependency-verification-1.3.xsd">
   <configuration>
      <verify-metadata>true</verify-metadata>
      <verify-signatures>false</verify-signatures>
      <trusted-artifacts>
         <!-- Sources and javadoc that IDEs download; they are never part of a build. -->
         <trust file=".*-javadoc[.]jar" regex="true"/>
         <trust file=".*-sources[.]jar" regex="true"/>
      </trusted-artifacts>
   </configuration>
   <components/>
</verification-metadata>
XML

./gradlew --write-verification-metadata sha256 \
    help assembleDebug assembleRelease lintDebug testDebugUnitTest :baselineprofile:assemble "$@"
# One quick instrumented test is enough to resolve the test runner.
./gradlew --write-verification-metadata sha256 :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=io.github.gonbei774.calisthenicsmemory.ExampleInstrumentedTest "$@"

aapt2_version="$(sed -nE 's/.*<component group="com.android.tools.build" name="aapt2" version="([^"]+)">.*/\1/p' "$metadata")"
[ -n "$aapt2_version" ] || fail "aapt2 is not in $metadata"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
for os in linux osx windows; do
    jar="aapt2-$aapt2_version-$os.jar"
    curl -fsSL -o "$tmp/$jar" "https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/$aapt2_version/$jar" \
        || fail "could not download $jar"
done
python3 -I - "$metadata" "$aapt2_version" "$tmp" <<'PY'
import hashlib, pathlib, re, sys
path, version, folder = pathlib.Path(sys.argv[1]), sys.argv[2], pathlib.Path(sys.argv[3])
text = path.read_text()
start = text.index(f'<component group="com.android.tools.build" name="aapt2" version="{version}">')
end = text.index("</component>", start)
component = text[start:end]
for os_name in ("linux", "osx", "windows"):
    jar = f"aapt2-{version}-{os_name}.jar"
    digest = hashlib.sha256((folder / jar).read_bytes()).hexdigest()
    recorded = re.search(rf'<artifact name="{re.escape(jar)}">\s*<sha256 value="([0-9a-f]+)"', component)
    if recorded:
        # The one Gradle downloaded here must match Google's copy, or something is wrong.
        if recorded.group(1) != digest:
            sys.exit(f"error: {jar} checksum differs from Google's Maven repository")
        continue
    entry = (f'         <artifact name="{jar}">\n'
             f'            <sha256 value="{digest}" origin="Google Maven, for builds on other systems"/>\n'
             f'         </artifact>\n')
    # Right after the component's opening line.
    opening = component.index("\n") + 1
    component = component[:opening] + entry + component[opening:]
for os_name in ("linux", "osx", "windows"):
    if f'<artifact name="aapt2-{version}-{os_name}.jar">' not in component:
        sys.exit(f"error: no checksum for aapt2 on {os_name}")
path.write_text(text[:start] + component + text[end:])
PY

echo "update-dependency-verification: done; review 'git diff $metadata' before committing"
