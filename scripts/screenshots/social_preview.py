"""Writes the social preview (GitHub's link card) as SVG, from two README screenshots.

The text is outlined from the app's font with the README builder's helpers, so the card looks the
same on every machine. update-screenshots.sh renders the SVG to the 2560 x 1280 PNG GitHub asks for.

Usage (needs fontTools):
    python3 scripts/screenshots/social_preview.py <light|dark> <front.png> <back.png> <out.svg>
"""
import base64
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "readme"))
from build_readme_assets import DARK, LIGHT, climb_icon, text_path  # noqa: E402

W, H = 2560, 1280
PHONE_W, PHONE_H, BEZEL = 520, 924, 16  # A 540 x 960 screenshot, shown at 96 %.
CHIPS = ["No internet", "29 progressions", "10 languages"]


def image(path):
    with open(path, "rb") as fh:
        return "data:image/png;base64," + base64.b64encode(fh.read()).decode()


def phone(c, shot, x, y, angle, name):
    w, h = PHONE_W + 2 * BEZEL, PHONE_H + 2 * BEZEL
    return (f'<g transform="rotate({angle} {x + w / 2} {y + h / 2})">'
            f'<rect x="{x + 6}" y="{y + 18}" width="{w}" height="{h}" rx="72" fill="{c["ink"]}" opacity=".10"/>'
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="72" fill="{c["card"]}" stroke="{c["line"]}" stroke-width="3"/>'
            f'<clipPath id="{name}"><rect x="{x + BEZEL}" y="{y + BEZEL}" width="{PHONE_W}" height="{PHONE_H}" rx="56"/></clipPath>'
            f'<image clip-path="url(#{name})" x="{x + BEZEL}" y="{y + BEZEL}" width="{PHONE_W}" height="{PHONE_H}" href="{image(shot)}"/>'
            f'</g>')


def card(theme, front, back):
    c = LIGHT if theme == "light" else DARK
    word, _ = text_path("Kalimory", 172, 760, 250, 700, tracking=-0.02)
    tag, _ = text_path("Calm, offline bodyweight training", 180, 880, 76, 400)
    chips, x = [], 180
    for label in CHIPS:
        d, tw = text_path(label, x + 44, 1004, 52, 500)
        chips.append(f'<rect x="{x}" y="944" width="{tw + 88}" height="88" rx="44" fill="{c["soft"]}"/>'
                     f'<path d="{d}" fill="{c["ink"]}"/>')
        x += tw + 88 + 28
    steps = "M1840 1280V1120H2020V960H2200V800H2380V640H2560V1280Z"
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">'
            f'<rect width="{W}" height="{H}" fill="{c["bg"]}"/>'
            f'<path d="{steps}" fill="{c["line"]}" opacity=".35"/>'
            f'{climb_icon(260, 180, 300)}'
            f'<path d="{word}" fill="{c["ink"]}"/><path d="{tag}" fill="{c["muted"]}"/>{"".join(chips)}'
            f'{phone(c, back, 1960, 150, 6, "back")}{phone(c, front, 1500, 230, -4, "front")}'
            f'</svg>')


if __name__ == "__main__":
    if len(sys.argv) != 5 or sys.argv[1] not in ("light", "dark"):
        sys.exit(__doc__)
    with open(sys.argv[4], "w") as out:
        out.write(card(*sys.argv[1:4]))
