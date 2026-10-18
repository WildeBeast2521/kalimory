#!/usr/bin/env python3
"""Writes a release's text from CHANGELOG.md, the one source for it, and keeps published releases in step.

    python3 -I scripts/release-notes.py <version>            print the text for v<version>
    python3 -I scripts/release-notes.py --check-published    exit 1 if a published release differs
    python3 -I scripts/release-notes.py --sync               rewrite the published releases that differ

The text is the version's block exactly as CHANGELOG.md has it: the `## [x.y.z] - YYYY-MM-DD`
heading, its sections and its link definition, so the heading links to the comparison with the
version before it. Then comes .github/release-notes-template.md's footer: how to verify the APK,
with the certificate from .github/release-certificate.sha256. Nothing else goes on the release page
(AGENTS.md, "Changelog and release notes").

The two release modes use the gh CLI (GH_TOKEN in CI). Drafts are left alone; the release workflow
writes theirs.
"""
import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REPO = "WildeBeast2521/kalimory"


def notes(version: str) -> str:
    changelog = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    block = re.search(rf"^## \[{re.escape(version)}\] - \d{{4}}-\d{{2}}-\d{{2}}( \[YANKED\])?\n.*?(?=^## \[|^\[[^\]]+\]: |\Z)",
                      changelog, re.MULTILINE | re.DOTALL)
    if not block:
        sys.exit(f"release-notes: CHANGELOG.md has no dated section ## [{version}] - YYYY-MM-DD")
    body = block.group(0).strip()
    if "\n### " not in body:
        sys.exit(f"release-notes: the CHANGELOG section for {version} is empty")
    link = re.search(rf"^\[{re.escape(version)}\]: \S+$", changelog, re.MULTILINE)
    if not link:
        sys.exit(f"release-notes: CHANGELOG.md has no link for [{version}]; run scripts/changelog-links.py")
    text = (ROOT / ".github/release-notes-template.md").read_text(encoding="utf-8")
    values = {
        "CHANGELOG": body + "\n\n" + link.group(0),
        "VERSION": version,
        "CERT_SHA256": (ROOT / ".github/release-certificate.sha256").read_text(encoding="utf-8").strip(),
    }
    for key, value in values.items():
        text = text.replace("{{" + key + "}}", value)
    left = re.findall(r"\{\{[A-Z_0-9]+\}\}", text)
    if left:
        sys.exit(f"release-notes: unfilled placeholders {left}")
    if re.search(r"claude|generated with|co-authored", text, re.IGNORECASE):
        sys.exit("release-notes: the notes contain tool attribution; remove it from CHANGELOG.md")
    return text.rstrip("\n") + "\n"


def normalise(text: str) -> str:
    return "\n".join(line.rstrip() for line in text.replace("\r\n", "\n").strip().split("\n"))


def published():
    out = subprocess.run(["gh", "api", "--paginate", f"repos/{REPO}/releases?per_page=100"],
                         check=True, capture_output=True, text=True).stdout
    releases = json.loads(out.replace("][", ","))
    return [r for r in releases if not r["draft"] and re.fullmatch(r"v\d+\.\d+\.\d+", r["tag_name"])]


def drifted():
    for release in published():
        expected = notes(release["tag_name"][1:])
        if normalise(release["body"] or "") != normalise(expected):
            yield release["tag_name"], expected


def main() -> None:
    args = sys.argv[1:]
    if args == ["--check-published"]:
        stale = [tag for tag, _ in drifted()]
        if stale:
            print(f"::error::published releases differ from CHANGELOG.md: {', '.join(stale)}. "
                  "Run python3 -I scripts/release-notes.py --sync")
            sys.exit(1)
        print("every published release matches CHANGELOG.md")
    elif args == ["--sync"]:
        changed = 0
        for tag, expected in drifted():
            with tempfile.NamedTemporaryFile("w", suffix=".md", encoding="utf-8", delete=False) as f:
                f.write(expected)
            subprocess.run(["gh", "release", "edit", tag, "-R", REPO, "--notes-file", f.name], check=True,
                           capture_output=True)
            Path(f.name).unlink()
            print(f"updated {tag}")
            changed += 1
        print(f"{changed} release(s) updated" if changed else "every published release matches CHANGELOG.md")
    elif len(args) == 1 and not args[0].startswith("-"):
        sys.stdout.write(notes(args[0]))
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
