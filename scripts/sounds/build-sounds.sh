#!/usr/bin/env bash
# Regenerates the workout sounds in app/src/main/res/raw from generate_workout_sounds.py.
# Needs python3 and ffmpeg with libvorbis. The generator is deterministic; the OGG files are
# committed so the build itself needs neither tool.
set -euo pipefail
cd "$(dirname "$0")/../.."
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
python3 scripts/sounds/generate_workout_sounds.py "$work"
mkdir -p app/src/main/res/raw
for name in countdown go set_done rep hold_tick; do
  ffmpeg -loglevel error -y -i "$work/$name.wav" -map_metadata -1 -fflags +bitexact -flags:a +bitexact -c:a libvorbis -q:a 5 -serial_offset 1 "app/src/main/res/raw/sound_$name.ogg"
done
echo "Wrote $(ls app/src/main/res/raw/sound_*.ogg | wc -l) sounds into app/src/main/res/raw"
