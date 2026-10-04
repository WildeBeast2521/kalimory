#!/usr/bin/env bash
# Publishes docs/wiki to the repository's GitHub wiki, which mirrors it exactly.
#
#   scripts/sync-wiki.sh            # push the pages
#   scripts/sync-wiki.sh --dry-run  # build the pages in a temporary folder and show the diff
#
# docs/wiki is the source: edit pages there, in the same pull request as the app change, and run
# this after the merge. Pages are rewritten for the wiki (links lose ".md"; links to repository
# files become absolute), and a sidebar and footer are added. The wiki repository exists only
# after its first page has been saved once in the browser (Wiki tab, "Create the first page").
set -euo pipefail
cd "$(dirname "$0")/.."

repo="https://github.com/WildeBeast2521/kalimory"
dry_run=false
[ "${1:-}" = "--dry-run" ] && dry_run=true

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
git clone --quiet "$repo.wiki.git" "$work/wiki" \
    || { echo "sync-wiki: no wiki repository yet; save its first page in the browser once" >&2; exit 1; }

# Mirror exactly: pages removed from docs/wiki disappear from the wiki too.
find "$work/wiki" -maxdepth 1 -name '*.md' -delete

for page in docs/wiki/*.md; do
    sed -E \
        -e 's#\]\(([A-Za-z0-9-]+)\.md(\#[^)]*)?\)#](\1\2)#g' \
        -e "s#\]\(\.\./\.\./([^)]+)\)#](${repo}/blob/master/\1)#g" \
        -e 's#They are kept in the repository under `docs/wiki/`#They are kept in the repository under [`docs/wiki/`]('"${repo}"'/tree/master/docs/wiki)#' \
        "$page" > "$work/wiki/$(basename "$page")"
done

{
    echo "**[Kalimory](Home)**"
    echo
    for page in docs/wiki/*.md; do
        name="$(basename "$page" .md)"
        [ "$name" = "Home" ] && continue
        title="$(sed -n '1s/^# //p' "$page")"
        echo "- [${title:-$name}]($name)"
    done
} > "$work/wiki/_Sidebar.md"

cat > "$work/wiki/_Footer.md" <<EOF
These pages are generated from [\`docs/wiki\`](${repo}/tree/master/docs/wiki). Corrections are welcome as pull requests there.
EOF

cd "$work/wiki"
git add -A
if git diff --cached --quiet; then
    echo "sync-wiki: the wiki is already up to date"
    exit 0
fi
git diff --cached --stat
if $dry_run; then
    echo "sync-wiki: dry run, nothing pushed"
    exit 0
fi
source_commit="$(git -C "$OLDPWD" rev-parse --short HEAD)"
git -c user.name=WildeBeast2521 -c user.email=182007783+WildeBeast2521@users.noreply.github.com \
    commit --quiet -m "Sync from docs/wiki at $source_commit"
git push --quiet origin HEAD
echo "sync-wiki: published"
