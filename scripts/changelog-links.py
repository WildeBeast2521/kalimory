#!/usr/bin/env python3
"""Keeps CHANGELOG.md in the Keep a Changelog 1.1.0 format, and writes its link definitions.

Adapted from Beanbook's tools/changelog-links.py. A heading such as `## [1.0.1] - 2026-10-05` is a
link only when the file ends with a definition for it. This script writes the definitions from the
headings alone:

    [Unreleased]: .../compare/v<newest>...HEAD
    [<version>]:  .../compare/v<previous>...v<version>     (the oldest: .../releases/tag/v<version>)
    [Calisthenics Memory <version>]: the original app's tag on Codeberg

Usage:
    python3 -I scripts/changelog-links.py           rewrite the definitions in place
    python3 -I scripts/changelog-links.py --check   exit 1 if the file breaks the rules below
                                                    (CI runs it on every push and release tag)

Rules checked, from https://keepachangelog.com/en/1.1.0/:
  * [Unreleased] comes first, then Kalimory's versions, newest first, each headed
    `## [x.y.z] - YYYY-MM-DD`, with ` [YANKED]` after the date for a release that was pulled;
  * sections are only Added, Changed, Deprecated, Removed, Fixed and Security, in that order, each
    at most once and none empty;
and one rule of this project: every entry under [Unreleased] and under 1.0.1 or later ends with the
pull request that made it, as `([#123](https://github.com/WildeBeast2521/kalimory/pull/123))`.
Kalimory 1.0.0 and the original app's releases ("Calisthenics Memory x.y.z") predate that rule.
"""
import re
import sys
from pathlib import Path

REPO = "https://github.com/WildeBeast2521/kalimory"
UPSTREAM = "https://codeberg.org/Gonbei774/CalisthenicsMemory"
FIRST_REFERENCED = (1, 0, 1)
TYPES = ("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security")

path = Path(__file__).resolve().parent.parent / "CHANGELOG.md"
text = path.read_text(encoding="utf-8")

HEADING = re.compile(r"^## \[(Unreleased|\d+\.\d+\.\d+|Calisthenics Memory \d+\.\d+\.\d+)\]")
RELEASE_HEADING = re.compile(r"^## \[(?:Calisthenics Memory )?\d+\.\d+\.\d+\] - \d{4}-\d{2}-\d{2}( \[YANKED\])?$")
DEFINITION = re.compile(r"^\[(Unreleased|\d+\.\d+\.\d+|Calisthenics Memory \d+\.\d+\.\d+)\]: \S+\s*$")
PULL_REQUEST = re.compile(r"\(\[#\d+\]\(" + re.escape(REPO) + r"/pull/\d+\)\)$")

headings = [m.group(1) for line in text.split("\n") if (m := HEADING.match(line))]
if not headings or headings[0] != "Unreleased":
    sys.exit("CHANGELOG.md must start its entries with an [Unreleased] heading")
versions = [h for h in headings if re.fullmatch(r"\d+\.\d+\.\d+", h)]
upstream = [h for h in headings if h.startswith("Calisthenics Memory ")]
if not versions:
    sys.exit("CHANGELOG.md has no released version")

wanted = [f"[Unreleased]: {REPO}/compare/v{versions[0]}...HEAD"]
for i, version in enumerate(versions):
    if i + 1 < len(versions):
        wanted.append(f"[{version}]: {REPO}/compare/v{versions[i + 1]}...v{version}")
    else:
        wanted.append(f"[{version}]: {REPO}/releases/tag/v{version}")
wanted += [f"[{h}]: {UPSTREAM}/releases/tag/v{h.removeprefix('Calisthenics Memory ')}" for h in upstream]

lines = text.rstrip("\n").split("\n")
while lines and (DEFINITION.match(lines[-1]) or not lines[-1].strip()):
    lines.pop()
body = "\n".join(lines)
expected = body + "\n\n" + "\n".join(wanted) + "\n"


def problems():
    found = []
    previous = None      # the Kalimory version above, to check newest first
    section = None       # the heading whose entries are being read
    kinds = []           # the ### headings seen under it
    entries = 0
    referenced = False   # whether this section's entries must cite a pull request
    upstream_seen = False

    def close_kind():
        if kinds and entries == 0:
            found.append(f"[{section}] the empty section ### {kinds[-1]}: leave it out")

    def close_section():
        close_kind()
        if section and [k for k in kinds if k in TYPES] != sorted((k for k in kinds if k in TYPES), key=TYPES.index):
            found.append(f"[{section}] sections out of order {kinds}: use {', '.join(TYPES)}")
        if len(kinds) != len(set(kinds)):
            found.append(f"[{section}] a section type appears twice in {kinds}")

    for line in body.split("\n"):
        if line.startswith("## "):
            close_section()
            kinds, entries = [], 0
            heading = HEADING.match(line)
            if not heading:
                found.append(f"not a version heading: {line[:80]}")
                section = None
                continue
            section = name = heading.group(1)
            if name == "Unreleased":
                referenced = True
                continue
            if not RELEASE_HEADING.match(line):
                found.append(f"{line[:80]!r}: write it as `## [x.y.z] - YYYY-MM-DD`")
            if name.startswith("Calisthenics Memory "):
                upstream_seen, referenced = True, False
                continue
            if upstream_seen:
                found.append(f"[{name}] comes below the original app's releases")
            number = tuple(map(int, name.split(".")))
            if previous is not None and number >= previous:
                found.append(f"[{name}] is not older than the version above it")
            previous = number
            referenced = number >= FIRST_REFERENCED
        elif line.startswith("### "):
            close_kind()
            kinds.append(line[4:].strip())
            entries = 0
            if kinds[-1] not in TYPES:
                found.append(f"[{section}] ### {kinds[-1]}: use one of {', '.join(TYPES)}")
        elif line.startswith("- ") and section:
            entries += 1
            if referenced and not PULL_REQUEST.search(line.rstrip()):
                found.append(f"[{section}] does not end with its pull request: {line[:80]}")
    close_section()
    return found


if "--check" in sys.argv[1:]:
    bad = problems()
    if bad:
        print("::error file=CHANGELOG.md::CHANGELOG.md breaks the Keep a Changelog rules:")
        print("\n".join(bad))
        sys.exit(1)
    if text != expected:
        print("::error file=CHANGELOG.md::the link definitions at the bottom do not match the headings. "
              "Run python3 -I scripts/changelog-links.py and commit the result.")
        sys.exit(1)
    print(f"CHANGELOG.md: {len(headings)} headings, all linked, Keep a Changelog 1.1.0")
else:
    path.write_text(expected, encoding="utf-8")
    print(f"wrote {len(wanted)} link definitions")
