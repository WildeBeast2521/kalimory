#!/usr/bin/env bash
# Regenerates the store and README screenshots and the social preview from the app itself (#158).
#
# The screens are drawn by StoreScreenshotsTest and StoreRestScreenshotTest under Robolectric with
# made-up data, so no device or emulator is needed and every run gives the same pictures. Needs JDK
# 17, ImageMagick 7 (magick), rsvg-convert and fontTools (python3 -m pip install fonttools).
#
#   scripts/screenshots/update-screenshots.sh
#
# Writes fastlane/metadata/android/en-US/images/phoneScreenshots/1-6.png (Light, 1080 x 1920),
# .github/readme/screens/<screen>-<light|dark>.png (540 x 960) and
# .github/readme/social-preview-<light|dark>.png (2560 x 1280). Look at every changed image before
# committing it. The social preview is uploaded by hand in the repository's settings.
set -euo pipefail

root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
for tool in magick rsvg-convert python3; do
    command -v "$tool" >/dev/null || { echo "update-screenshots: $tool is missing" >&2; exit 1; }
done
python3 -c 'import fontTools' 2>/dev/null || { echo "update-screenshots: fontTools is missing" >&2; exit 1; }

shots=$(mktemp -d)
trap 'rm -rf "$shots"' EXIT
KALIMORY_SCREENSHOTS="$shots" ./gradlew -q recordRoborazziDebug \
    --tests '*StoreScreenshotsTest*' --tests '*StoreRestScreenshotTest*'

# Store order: what a newcomer sees first.
order=(today rest progressions chain step calendar)
store=fastlane/metadata/android/en-US/images/phoneScreenshots
for i in "${!order[@]}"; do
    for theme in light dark; do
        [ -f "$shots/${order[$i]}-$theme.png" ] || { echo "update-screenshots: ${order[$i]}-$theme.png was not drawn" >&2; exit 1; }
    done
    magick "$shots/${order[$i]}-light.png" -strip "$store/$((i + 1)).png"
    for theme in light dark; do
        magick "$shots/${order[$i]}-$theme.png" -resize 540x960 -strip ".github/readme/screens/${order[$i]}-$theme.png"
    done
done

for theme in light dark; do
    python3 scripts/screenshots/social_preview.py "$theme" \
        ".github/readme/screens/today-$theme.png" ".github/readme/screens/progressions-$theme.png" "$shots/social-$theme.svg"
    rsvg-convert -w 2560 -h 1280 "$shots/social-$theme.svg" | magick - -strip ".github/readme/social-preview-$theme.png"
done
echo "update-screenshots: done; review the changed images with git status"
