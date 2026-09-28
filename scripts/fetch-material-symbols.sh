#!/usr/bin/env bash
# Fetches the Material Symbols (Rounded, 24 px) listed in scripts/material-symbols.txt as
# Android vector drawables into app/src/main/res/drawable/ms_<name>.xml (and ms_<name>_fill.xml).
# Source: https://github.com/google/material-design-icons (Apache-2.0), pinned to one commit
# so the output is reproducible. Re-run after editing the manifest; it rewrites every ms_* file.
set -euo pipefail
cd "$(dirname "$0")/.."

commit="bd8cb85bd4bad964fe6918f79665bb40c3a8efef"
base="https://raw.githubusercontent.com/google/material-design-icons/$commit/symbols/android"
out="app/src/main/res/drawable"
mkdir -p "$out"
rm -f "$out"/ms_*.xml

fetch() {
  local url="$1" file="$2"
  curl -fsSL "$url" -o "$file" || { echo "error: could not fetch $url" >&2; exit 1; }
}

grep -v '^\s*#' scripts/material-symbols.txt | grep -v '^\s*$' | while read -r name variant; do
  fetch "$base/$name/materialsymbolsrounded/${name}_24px.xml" "$out/ms_${name}.xml"
  if [ "${variant:-}" = "fill" ]; then
    fetch "$base/$name/materialsymbolsrounded/${name}_fill1_24px.xml" "$out/ms_${name}_fill.xml"
  fi
done
echo "Fetched $(ls "$out"/ms_*.xml | wc -l) Material Symbols into $out"
