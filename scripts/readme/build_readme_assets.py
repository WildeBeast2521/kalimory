"""Builds the README's images into .github/readme/ as self-contained SVG.

Text is converted to outlines from the app's own font (Onest, OFL), so the images look the same on
every machine. Feature icons are Material Symbols Rounded (Apache-2.0), pinned to the commit the app
uses. Animations are CSS inside each SVG; they stop for readers who ask for reduced motion, and the
still frame is the finished picture.

Usage (needs fontTools):
    python3 -m pip install fonttools
    python3 scripts/readme/build_readme_assets.py
"""
import os
import re
import urllib.request

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, ".github", "readme")
FONT = os.path.join(ROOT, "app/src/main/res/font/onest_variable.ttf")
SYMBOLS = "https://raw.githubusercontent.com/google/material-design-icons/bd8cb85bd4bad964fe6918f79665bb40c3a8efef/symbols/web"

# The app's palette (ui/theme/CalmPalette.kt).
LIGHT = dict(bg="#F2F4F1", card="#FAFBF9", ink="#1F2A27", muted="#4A5551", line="#C4CCC7", spruce="#2F6B5E", soft="#CFE6DE", brass="#B07A22", chalk="#F2F4F1")
DARK = dict(bg="#141B19", card="#1B2422", ink="#E3EAE6", muted="#A9B5B0", line="#34413D", spruce="#7DB8A9", soft="#1E4F45", brass="#E2B96B", chalk="#F2F4F1")
ICON_BG, ICON_FG, ICON_MARK = "#2F6B5E", "#F2F4F1", "#E2B96B"

_fonts = {}


def font(weight):
    if weight not in _fonts:
        _fonts[weight] = instantiateVariableFont(TTFont(FONT), {"wght": weight})
    return _fonts[weight]


def text_path(text, x, y, size, weight=400, anchor="start", tracking=0.0):
    """Outline of one line of text with its baseline at y. Returns (path data, width)."""
    f = font(weight)
    upm = f["head"].unitsPerEm
    cmap, glyphs, hmtx = f.getBestCmap(), f.getGlyphSet(), f["hmtx"]
    scale = size / upm
    names = [cmap.get(ord(c), ".notdef") for c in text]
    width = sum(hmtx[n][0] for n in names) * scale + tracking * size * max(len(names) - 1, 0)
    cx = x - width / 2 if anchor == "middle" else x - width if anchor == "end" else x
    pen = SVGPathPen(glyphs, ntos=lambda v: f"{v:.1f}".rstrip("0").rstrip("."))
    for n in names:
        glyphs[n].draw(TransformPen(pen, (scale, 0, 0, -scale, cx, y)))
        cx += hmtx[n][0] * scale + tracking * size
    return pen.getCommands(), width


def write(name, svg):
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name), "w") as fh:
        fh.write(svg)
    print("wrote", name, len(svg), "bytes")


REDUCED = "@media (prefers-reduced-motion: reduce){*{animation:none!important}}"

# The Climb, on the launcher icon's 108-unit grid: four columns and the marker's seat on each.
COLUMNS = [(34, 67), (44, 58), (54, 49), (64, 40)]
SEATS = [(x + 5, top - 6.5) for x, top in COLUMNS]


def climb_icon(px, x, y):
    """The animated launcher icon, px wide, with its top-left corner at (x, y)."""
    s = px / 72
    cols = "".join(
        f'<rect class="col c{i}" x="{cx}" y="{top}" width="{11 if i < 3 else 10}" height="{76 - top}" fill="{ICON_FG}"/>'
        for i, (cx, top) in enumerate(COLUMNS))
    top_x, top_y = SEATS[-1]
    return (f'<g transform="translate({x} {y}) scale({s:.4f}) translate(-18 -18)">'
            f'<rect x="18" y="18" width="72" height="72" rx="20" fill="{ICON_BG}"/>'
            f'<g transform="translate(54 54) scale(1.1) translate(-54 -54)">{cols}'
            f'<circle class="glow" cx="{top_x}" cy="{top_y}" r="4.6" fill="{ICON_MARK}"/>'
            f'<circle class="mark" cx="{top_x}" cy="{top_y}" r="4.6" fill="{ICON_MARK}"/></g></g>')


def climb_css():
    # Columns rise once. The marker then hops up the steps on a loop, rests on top, and starts again.
    rise = "".join(f".c{i}{{animation:rise .7s cubic-bezier(.34,1.56,.64,1) {0.15 * i:.2f}s both}}" for i in range(4))
    tx, ty = SEATS[-1]
    frames, t = [], 0.0
    stops = [(0, 0), (9, 0)]
    for i in range(1, 4):
        stops += [(9 + 10 * i - 5, i - 0.5), (9 + 10 * i, i)]
    keys = []
    for pct, pos in stops:
        lo, hi = int(pos), min(int(pos) + 1, 3)
        if pos == int(pos):
            sx, sy = SEATS[int(pos)]
            lift = 0
        else:
            (ax, ay), (bx, by) = SEATS[lo], SEATS[hi]
            sx, sy, lift = (ax + bx) / 2, (ay + by) / 2, 7
        keys.append(f"{pct}%{{transform:translate({sx - tx:.1f}px,{sy - ty - lift:.1f}px);opacity:1}}")
    keys += ["78%{transform:none;opacity:1}", "86%{transform:none;opacity:0}",
             f"87%{{transform:translate({SEATS[0][0] - tx:.1f}px,{SEATS[0][1] - ty:.1f}px);opacity:0}}",
             f"100%{{transform:translate({SEATS[0][0] - tx:.1f}px,{SEATS[0][1] - ty:.1f}px);opacity:1}}"]
    return ("@keyframes rise{from{transform:scaleY(0)}to{transform:scaleY(1)}}"
            ".col{transform-box:fill-box;transform-origin:50% 100%}" + rise +
            "@keyframes hop{" + "".join(keys) + "}"
            ".mark{animation:hop 6s ease-in-out .9s infinite both}"
            "@keyframes glow{0%,40%{opacity:0;transform:scale(1)}48%{opacity:.55;transform:scale(1)}70%{opacity:0;transform:scale(3.2)}100%{opacity:0}}"
            ".glow{transform-box:fill-box;transform-origin:50% 50%;opacity:0;animation:glow 6s ease-out .9s infinite}")


def banner(theme, name):
    c = LIGHT if theme == "light" else DARK
    w, h = 1280, 400
    word, ww = text_path("Kalimory", 400, 205, 128, 700, tracking=-0.02)
    tag, _ = text_path("Calm, offline bodyweight training.", 404, 268, 36, 400)
    note, _ = text_path("Plan it. Run it. Remember it.", 404, 318, 26, 500)
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}" role="img" aria-label="Kalimory: calm, offline bodyweight training">'
           f'<style>{climb_css()}{REDUCED}</style>'
           f'<rect width="{w}" height="{h}" rx="36" fill="{c["card"]}"/>'
           f'<rect x="1" y="1" width="{w - 2}" height="{h - 2}" rx="35" fill="none" stroke="{c["line"]}" stroke-width="2"/>'
           f'<clipPath id="card"><rect width="{w}" height="{h}" rx="36"/></clipPath>'
           f'<path clip-path="url(#card)" d="M960 400V336H1040V272H1120V208H1200V144H1280V400Z" fill="{c["line"]}" opacity=".35"/>'
           f'{climb_icon(240, 96, 80)}'
           f'<path d="{word}" fill="{c["ink"]}"/><path d="{tag}" fill="{c["muted"]}"/><path d="{note}" fill="{c["brass"]}"/>'
           f'</svg>')
    write(name, svg)


PUSH_CHAIN = [("Wall", 1), ("Incline", 2), ("Knee", 2), ("Push-up", 3), ("Diamond", 4), ("Decline", 4), ("Archer", 6), ("One-arm", 8)]
CURRENT = 3  # The marker sits on Push-up.
CYCLE = 9  # Seconds per loop of the chain picture.


def chain(theme, name):
    c = LIGHT if theme == "light" else DARK
    w, h = 1280, 420
    left, base, colw, gap, unit = 72, 330, 122, 22, 26
    parts, css = [], []
    title, _ = text_path("The Push-up chain", left, 64, 30, 600)
    sub, _ = text_path("Eight steps from level 1 to level 8. You move on when a session meets the standard.", left, 100, 20, 400)
    parts += [f'<path d="{title}" fill="{c["ink"]}"/>', f'<path d="{sub}" fill="{c["muted"]}"/>']
    seats = []
    for i, (label, level) in enumerate(PUSH_CHAIN):
        x = left + i * (colw + gap)
        top = base - level * unit - 16
        done, here = i < CURRENT, i == CURRENT
        fill = c["spruce"] if done else c["soft"] if here else "none"
        stroke = c["brass"] if here else c["spruce"] if done else c["line"]
        parts.append(f'<rect class="bar b{i}" x="{x}" y="{top}" width="{colw}" height="{base - top}" rx="14" fill="{fill}" stroke="{stroke}" stroke-width="{3 if here else 2}"/>')
        num, _ = text_path(str(level), x + colw / 2, top + 30, 20, 700, anchor="middle")
        numfill = (c["chalk"] if theme == "light" else c["bg"]) if done else c["ink"]
        parts.append(f'<path class="lab b{i}" d="{num}" fill="{numfill}"/>')
        lab, _ = text_path(label, x + colw / 2, base + 36, 20, 600 if here else 500, anchor="middle")
        parts.append(f'<path d="{lab}" fill="{c["ink"] if here else c["muted"]}"/>')
        seats.append((x + colw / 2, top - 22))
        css.append(f".b{i}{{animation:grow {CYCLE}s cubic-bezier(.34,1.4,.64,1) {0.1 * i:.1f}s infinite both}}")
    sx, sy = seats[CURRENT]
    parts.append(f'<circle class="mark" cx="{sx}" cy="{sy}" r="12" fill="{c["brass"]}"/>')
    hint, _ = text_path("You are here. Move on at 3 × 12.", sx, sy - 26, 20, 500, anchor="middle")
    parts.append(f'<path class="hint" d="{hint}" fill="{c["brass"]}"/>')
    # One loop: the bars rise, the marker hops up to the current step, the hint shows, all rest,
    # then fade and start again. The still frame (reduced motion) is the finished picture.
    first_x, first_y = seats[0][0] - sx, seats[0][1] - sy
    keys = [f"0%,14%{{transform:translate({first_x:.0f}px,{first_y:.0f}px);opacity:0}}",
            f"17%{{transform:translate({first_x:.0f}px,{first_y:.0f}px);opacity:1}}"]
    for step in range(1, CURRENT + 1):
        (ax, ay), (bx, by) = seats[step - 1], seats[step]
        start = 17 + 9 * (step - 1)
        keys.append(f"{start + 4.5:g}%{{transform:translate({(ax + bx) / 2 - sx:.0f}px,{min(ay, by) - sy - 34:.0f}px);opacity:1}}")
        keys.append(f"{start + 9:g}%{{transform:translate({bx - sx:.0f}px,{by - sy:.0f}px);opacity:1}}")
    keys += ["86%{transform:none;opacity:1}", "94%,100%{transform:none;opacity:0}"]
    css += ["@keyframes grow{0%{transform:scaleY(0);opacity:0}8%,86%{transform:scaleY(1);opacity:1}94%,100%{transform:scaleY(1);opacity:0}}",
            ".bar,.lab{transform-box:fill-box;transform-origin:50% 100%}",
            "@keyframes walk{" + "".join(keys) + "}",
            f".mark{{animation:walk {CYCLE}s ease-in-out infinite both}}",
            "@keyframes show{0%,46%{opacity:0}52%,86%{opacity:1}94%,100%{opacity:0}}",
            f".hint{{animation:show {CYCLE}s ease infinite both}}"]
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}" role="img" aria-label="The Push-up chain: eight steps from wall push-up to one-arm push-up">'
           f'<style>{"".join(css)}{REDUCED}</style>'
           f'<rect width="{w}" height="{h}" rx="32" fill="{c["card"]}"/>'
           f'<rect x="1" y="1" width="{w - 2}" height="{h - 2}" rx="31" fill="none" stroke="{c["line"]}" stroke-width="2"/>'
           + "".join(parts) + "</svg>")
    write(name, svg)


FEATURES = ["today", "timer", "history", "stairs", "insights", "download", "translate", "palette", "lock", "wifi_off"]


def feature_icons():
    for n in FEATURES:
        raw = urllib.request.urlopen(f"{SYMBOLS}/{n}/materialsymbolsrounded/{n}_24px.svg").read().decode()
        d = re.search(r' d="([^"]+)"', raw).group(1)
        # Most symbols draw on a 960-unit grid shifted up by 960; some older ones on a plain 24-unit
        # grid with no viewBox. Fit either into the 32-unit centre of the tile.
        vb = re.search(r'viewBox="([^"]+)"', raw)
        minx, miny, vw, _ = (float(v) for v in vb.group(1).split()) if vb else (0.0, 0.0, 24.0, 24.0)
        k = 32 / vw
        svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64">'
               f'<rect width="64" height="64" rx="20" fill="{ICON_BG}"/>'
               f'<g transform="translate({16 - minx * k:g} {16 - miny * k:g}) scale({k:.5f})"><path d="{d}" fill="{ICON_FG}"/></g></svg>')
        write(f"icon-{n}.svg", svg)


if __name__ == "__main__":
    banner("light", "banner-light.svg")
    banner("dark", "banner-dark.svg")
    chain("light", "chain-light.svg")
    chain("dark", "chain-dark.svg")
    feature_icons()
