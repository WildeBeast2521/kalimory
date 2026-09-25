#!/usr/bin/env bash
# Regenerates the current Room schema and fails if any committed schema under
# app/schemas changed or a new schema appeared. Also validates every schema file.
# Usage: scripts/check-room-schemas.sh [extra Gradle arguments]
set -euo pipefail

cd "$(dirname "$0")/.."

schema_root="app/schemas"
schema_dir="$schema_root/io.github.gonbei774.calisthenicsmemory.data.AppDatabase"
database_source="app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt"
generated_database="app/build/generated/ksp/debug/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase_Impl.java"

if ! git diff --quiet -- "$schema_root" || [ -n "$(git ls-files --others --exclude-standard -- "$schema_root")" ]; then
    echo "error: $schema_root has uncommitted changes before generation; commit or discard them first." >&2
    exit 1
fi

# --rerun forces KSP to run even when its task is up to date, so the schema is
# rewritten from the current entities.
./gradlew :app:kspDebugKotlin --rerun "$@"

if ! git diff --exit-code --stat -- "$schema_root"; then
    echo "error: Room schema generation changed committed schemas (see diff above)." >&2
    echo "Committed schemas are immutable. A changed current schema requires a database version bump and migration." >&2
    exit 1
fi

untracked="$(git ls-files --others --exclude-standard -- "$schema_root")"
if [ -n "$untracked" ]; then
    echo "error: Room schema generation produced uncommitted schema files:" >&2
    echo "$untracked" >&2
    exit 1
fi

current_version="$(sed -nE 's/^[[:space:]]*version[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' "$database_source" | head -n 1)"
if [ -z "$current_version" ]; then
    echo "error: could not read the database version from $database_source" >&2
    exit 1
fi

python3 - "$schema_dir" "$current_version" "$generated_database" <<'PY'
import json
import pathlib
import re
import sys

schema_dir = pathlib.Path(sys.argv[1])
current_version = int(sys.argv[2])
generated_database = pathlib.Path(sys.argv[3]).read_text(encoding="utf-8")
errors = []

# Room leaves an existing schema file untouched when its tables are equal, so
# compare the identity hash with the one compiled into the generated database.
current_schema = schema_dir / f"{current_version}.json"
if not current_schema.is_file():
    errors.append(f"missing schema for current database version {current_version}")
else:
    current_hash = json.loads(current_schema.read_text(encoding="utf-8"))["database"]["identityHash"]
    if f"'{current_hash}'" not in generated_database:
        errors.append(f"{current_schema}: identityHash {current_hash} does not match the generated AppDatabase_Impl")

for path in sorted(schema_dir.glob("*.json")):
    text = path.read_text(encoding="utf-8")
    try:
        database = json.loads(text)["database"]
    except (ValueError, KeyError) as error:
        errors.append(f"{path}: not a Room schema ({error})")
        continue
    if str(database.get("version")) != path.stem:
        errors.append(f"{path}: database.version {database.get('version')} does not match file name")
    if not database.get("identityHash"):
        errors.append(f"{path}: missing identityHash")
    if not database.get("entities"):
        errors.append(f"{path}: no entities")
    if re.search(r"(/home/|/Users/|/mnt/|[A-Za-z]:\\\\)", text):
        errors.append(f"{path}: contains an absolute local path")

if errors:
    print("\n".join(f"error: {e}" for e in errors), file=sys.stderr)
    sys.exit(1)
print(f"Room schemas OK: current version {current_version}, files: "
      + ", ".join(p.name for p in sorted(schema_dir.glob('*.json'))))
PY
