#!/usr/bin/env bash
# Fails when a built APK asks for a permission the app does not declare itself.
#
# Kalimory is offline and private (AGENTS.md, "Invariants"): no network, Google Play Services,
# analytics or tracking. A library can merge permissions into the APK through its own manifest
# (for example an advertising-ID or network-state permission), unnoticed in the app's sources.
# Every <uses-permission> the APK requests must therefore be one that
# app/src/main/AndroidManifest.xml declares, or the signature permission androidx.core adds for
# receivers that are not exported (<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION).
# A permission added on purpose goes into the app's manifest, where review sees it.
#
#   scripts/check-apk-permissions.sh <app.apk>
#
# Needs aapt from the Android build tools ($ANDROID_HOME or $ANDROID_SDK_ROOT).
set -euo pipefail

apk="${1:?usage: scripts/check-apk-permissions.sh <app.apk>}"
root=$(cd "$(dirname "$0")/.." && pwd)
manifest="$root/app/src/main/AndroidManifest.xml"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$sdk" ] || { echo "check-apk-permissions: set ANDROID_HOME" >&2; exit 2; }
aapt="$(find "$sdk/build-tools" -type f -name aapt -print -quit)"
[ -n "$aapt" ] || { echo "check-apk-permissions: the Android build tools do not provide aapt" >&2; exit 2; }

dump="$("$aapt" dump permissions "$apk")"
package="$(printf '%s\n' "$dump" | sed -n "s/^package: //p")"
[ -n "$package" ] || { echo "check-apk-permissions: could not read the package of $apk" >&2; exit 2; }

declared="$(sed -n 's/.*<uses-permission[^>]*android:name="\([^"]*\)".*/\1/p' "$manifest" | sort -u)"
allowed="$(printf '%s\n%s\n' "$declared" "$package.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" | sort -u)"
requested="$(printf '%s\n' "$dump" | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p" | sort -u)"

unexpected="$(comm -23 <(printf '%s\n' "$requested") <(printf '%s\n' "$allowed"))"
if [ -n "$unexpected" ]; then
    echo "check-apk-permissions: $apk asks for permissions the app does not declare:" >&2
    printf '%s\n' "$unexpected" | sed 's/^/  /' >&2
    echo "A dependency probably merged them in. Remove them (tools:node=\"remove\" in the app's" >&2
    echo "manifest) or, if the app needs one, declare it in app/src/main/AndroidManifest.xml." >&2
    exit 1
fi
echo "check-apk-permissions: $(printf '%s\n' "$requested" | wc -l) permissions, all declared by the app"
