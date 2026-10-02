#!/bin/sh
# Fetches the 3D figure's build tools into ~/.local/share/figure-pipeline (outside the repository):
# Blender 4.2 LTS (GPL), the MPFB2 add-on (GPL) and the MakeHuman system assets (CC0). None of
# them is shipped; only renders made from the CC0 assets are (ADR 0008, 2026-10-02 amendment).
set -eu
DIR="${FIGURE_PIPELINE_DIR:-$HOME/.local/share/figure-pipeline}"
mkdir -p "$DIR" && cd "$DIR"

fetch() { # url file sha256
    [ -f "$2" ] || curl -sSL -o "$2" "$1"
    echo "$3  $2" | sha256sum -c -
}
fetch https://download.blender.org/release/Blender4.2/blender-4.2.23-linux-x64.tar.xz blender.tar.xz \
    bea0eb3146be13eae6225409a117b215184f41b7f79e799f97cb3abb8f6dc404
fetch https://files.makehumancommunity.org/asset_packs/makehuman_system_assets/makehuman_system_assets_cc0.zip mh_assets_cc0.zip \
    b542127a8e25547c7c29c19f2d1d2adb9a664c80396ecd694095dbc8028a0107
fetch https://api.github.com/repos/makehumancommunity/mpfb2/zipball/v2.0.17 mpfb2.zip \
    c8ccfc6269f82cf95661ef81475ddd5a8586f70810ba58940a3d26f7fc281d17

[ -d blender-4.2.23-linux-x64 ] || tar xf blender.tar.xz

# A private Blender profile, so the user's own Blender settings are never touched.
export BLENDER_USER_CONFIG="$DIR/bconfig/config" BLENDER_USER_SCRIPTS="$DIR/bconfig/scripts" \
    BLENDER_USER_EXTENSIONS="$DIR/bconfig/extensions" BLENDER_USER_DATAFILES="$DIR/bconfig/datafiles"
if [ ! -d "$DIR/bconfig/extensions/user_default/mpfb" ]; then
    rm -rf mpfb-src && mkdir mpfb-src && unzip -q mpfb2.zip -d mpfb-src
    (cd mpfb-src/*/src/mpfb && rm -f "$DIR/mpfb-ext.zip" && zip -qr "$DIR/mpfb-ext.zip" .)
    ./blender-4.2.23-linux-x64/blender --command extension install-file -r user_default -e mpfb-ext.zip
fi
unzip -q -o mh_assets_cc0.zip -d "$DIR/bconfig/extensions/.user/user_default/mpfb/data"
echo "Tools ready in $DIR"
