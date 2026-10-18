#!/usr/bin/env python3
"""Writes a release's notes: its CHANGELOG section in .github/release-notes-template.md.

    python3 -I scripts/release-notes.py <version> <previous-tag> <certificate-sha256>

The text of a GitHub release is exactly the version's CHANGELOG section plus the template's footer
(AGENTS.md, "Changelog and release notes"). It fails when the section is missing or empty, when a
placeholder is left unfilled, or when the notes carry tool attribution, so a release is never drafted
with the wrong text.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent


def section(changelog: str, version: str) -> str:
    match = re.search(rf"^## \[{re.escape(version)}\] - \d{{4}}-\d{{2}}-\d{{2}}\n(.*?)(?=^## \[|^\[[^\]]+\]: |\Z)",
                      changelog, re.MULTILINE | re.DOTALL)
    if not match:
        sys.exit(f"release-notes: CHANGELOG.md has no dated section ## [{version}] - YYYY-MM-DD")
    body = match.group(1).strip()
    if not body:
        sys.exit(f"release-notes: the CHANGELOG section for {version} is empty")
    return body


def main() -> None:
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    version, previous_tag, cert = sys.argv[1:]
    notes = (ROOT / ".github/release-notes-template.md").read_text(encoding="utf-8")
    values = {
        "NOTES": section((ROOT / "CHANGELOG.md").read_text(encoding="utf-8"), version),
        "VERSION": version,
        "TAG": f"v{version}",
        "PREVIOUS_TAG": previous_tag,
        "CERT_SHA256": cert,
    }
    for key, value in values.items():
        notes = notes.replace("{{" + key + "}}", value)
    left = re.findall(r"\{\{[A-Z_]+\}\}", notes)
    if left:
        sys.exit(f"release-notes: unfilled placeholders {left}")
    if re.search(r"claude|generated with|co-authored", notes, re.IGNORECASE):
        sys.exit("release-notes: the notes contain tool attribution; remove it from CHANGELOG.md")
    sys.stdout.write(notes)


if __name__ == "__main__":
    main()
