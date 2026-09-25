#!/usr/bin/env bash
# Verifies the committed Room schemas under app/schemas:
#   1. With SCHEMA_BASE_REF set, fails when any schema JSON committed at that
#      ref was modified, deleted, renamed, or changed in type since (new files are allowed).
#   2. Regenerates the current schema and fails when the result differs from
#      HEAD or adds a file.
#   3. Validates every schema file and compares the current schema's identity
#      hash with the generated AppDatabase_Impl.
# Usage: [SCHEMA_BASE_REF=<git ref>] scripts/check-room-schemas.sh [extra Gradle arguments]
set -euo pipefail

cd "$(dirname "$0")/.."

schema_root="app/schemas"
schema_dir="$schema_root/io.github.gonbei774.calisthenicsmemory.data.AppDatabase"
database_source="app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt"
generated_database="app/build/generated/ksp/debug/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase_Impl.java"

fail() {
    echo "error: $*" >&2
    exit 1
}

if [ -n "${SCHEMA_BASE_REF:-}" ]; then
    git rev-parse --verify --quiet "$SCHEMA_BASE_REF^{commit}" >/dev/null \
        || fail "SCHEMA_BASE_REF '$SCHEMA_BASE_REF' is not a commit in this checkout."
    changed="$(git diff --name-only --diff-filter=MDRT "$SCHEMA_BASE_REF...HEAD" -- "$schema_dir")"
    if [ -n "$changed" ]; then
        echo "$changed" >&2
        fail "committed Room schemas were modified, deleted, renamed, or changed in type since $SCHEMA_BASE_REF. Committed schemas are immutable."
    fi
fi

if ! git diff --quiet HEAD -- "$schema_root" || [ -n "$(git ls-files --others --exclude-standard -- "$schema_root")" ]; then
    git status --short -- "$schema_root" >&2
    fail "$schema_root differs from HEAD before generation. Commit or discard those changes first."
fi

# --rerun forces KSP to run even when its task is up to date, so the schema is
# rewritten from the current entities.
./gradlew :app:kspDebugKotlin --rerun "$@"

restore_hint="Restore the committed files with: git checkout HEAD -- $schema_root && git clean -f -- $schema_root"

if ! git diff --exit-code HEAD -- "$schema_root"; then
    echo "error: Room schema generation changed committed schemas (see diff above)." >&2
    echo "A changed current schema requires a database version bump, a migration, and a new schema file." >&2
    fail "$restore_hint"
fi

untracked="$(git ls-files --others --exclude-standard -- "$schema_root")"
if [ -n "$untracked" ]; then
    echo "$untracked" >&2
    echo "error: Room schema generation produced uncommitted schema files (listed above)." >&2
    echo "After a version bump, commit the new schema file, then run this check again." >&2
    fail "$restore_hint"
fi

current_version="$(grep -m 1 -oE '^[[:space:]]*version[[:space:]]*=[[:space:]]*[0-9]+' "$database_source" | grep -oE '[0-9]+$' || true)"
[ -n "$current_version" ] || fail "could not read the database version from $database_source"
[ -f "$generated_database" ] || fail "generated database implementation not found at $generated_database"

python3 - "$schema_dir" "$current_version" "$generated_database" <<'PY'
import json
import pathlib
import re
import sys

schema_dir = pathlib.Path(sys.argv[1])
current_version = int(sys.argv[2])
generated_database = pathlib.Path(sys.argv[3]).read_text(encoding="utf-8")
errors = []
invalid = set()

paths = sorted(schema_dir.glob("*.json"))
for path in paths:
    if not path.stem.isdigit():
        errors.append(f"{path}: file name is not a database version")
        continue
    if int(path.stem) > current_version:
        errors.append(f"{path}: newer than the current database version {current_version}")
    text = path.read_text(encoding="utf-8")
    try:
        database = json.loads(text)["database"]
    except (ValueError, KeyError) as error:
        errors.append(f"{path}: not a Room schema ({error})")
        invalid.add(path)
        continue
    if str(database.get("version")) != path.stem:
        errors.append(f"{path}: database.version {database.get('version')} does not match the file name")
    if not database.get("identityHash"):
        errors.append(f"{path}: missing identityHash")
    if not database.get("entities"):
        errors.append(f"{path}: no entities")
    if re.search(r"(/home/|/Users/|/mnt/|/tmp/|[A-Za-z]:\\\\)", text):
        errors.append(f"{path}: contains an absolute local path")

# Room leaves an existing schema file untouched when its tables are equal, so
# compare the identity hash with the one compiled into the generated database.
current_schema = schema_dir / f"{current_version}.json"
if not current_schema.is_file():
    errors.append(f"missing schema for current database version {current_version}")
elif current_schema not in invalid:
    current_hash = json.loads(current_schema.read_text(encoding="utf-8"))["database"].get("identityHash")
    if f"'{current_hash}'" not in generated_database:
        errors.append(f"{current_schema}: identityHash {current_hash} does not match the generated AppDatabase_Impl")

if errors:
    print("\n".join(f"error: {e}" for e in errors), file=sys.stderr)
    sys.exit(1)
names = sorted((p.name for p in paths), key=lambda name: int(name.split(".")[0]))
print(f"Room schemas OK: current version {current_version}, files: {', '.join(names)}")
PY
