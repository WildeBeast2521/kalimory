# Overhaul status

Durable handoff for the multi-session overhaul. Update at every verified checkpoint. Record only verified facts.

## Current phase

Corruption preservation, branch `work/corruption-preservation`, based on master `5519fe5`. The user chose "copy aside, keep live" (2026-09-25). The PR follows these checks.

- `fix: keep the database when SQLite reports corruption`: Room's default `onCorruption` deleted the database. On the emulator, a database with a damaged header was silently replaced by an empty one. Now the files are copied once to `<name>.corrupt` and the live file is kept.
- Verification: `testDebugUnitTest lintDebug assembleDebug compileDebugAndroidTestKotlin` PASS (234 unit tests, no new lint findings). `connectedDebugAndroidTest` on `floor_api29` (API 29): 70 tests, 0 failures. `CorruptDatabaseTest` failed before the fix.

## Previous phase: backup restorability and mutation atomicity (merged)

PR #4 merged as `c619f0a`; PR CI run 36129605040 and post-merge `master` run 36130284869 passed. The user chose "accept and warn" for anomalies the database can hold (2026-09-25).

Commits merged by PR #4:

- `b4fb7ab` fix: tolerate malformed todo repeat days when reading
- `3220edd` fix: restore every backup the app can export
- `f86a2ba` fix: make group and exercise deletion and renaming atomic
- `dba8969` fix: never replace an exercise on a duplicate insert
- `4cdd0b5` style: use an int state holder for the import anomaly count

Verification actually run before merge:

- `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug :app:compileDebugAndroidTestKotlin`: PASS (232 unit tests). Lint reports 15 unbaselined dependency-version warnings from existing build files; none comes from this branch.
- `./gradlew connectedDebugAndroidTest` on AVD `floor_api29` (API 29, x86_64): 69 tests, 0 failures.
- `SCHEMA_BASE_REF=origin/master scripts/check-room-schemas.sh`: PASS.
- Red first: the duplicate-insert test failed before `dba8969` (no exception, history deleted). The old unit tests show that master rejected each anomaly class that is now accepted.

The contract is documented in `docs/development/backup-validation.md`.

## Previous phase: Room schema and migration hardening (merged)

Room schema and migration hardening is complete. PR #2 merged into `master` as `0efcfb1`.

## Commits merged by PR #2

- `5301586` docs: declare supported database versions
- `7ea6e1e` docs: tighten unsupported database recovery guidance
- `adb2d2c` build: export current Room schema
- `b3af5c2` build: enforce committed Room schema immutability
- `e8708b9` build: close remaining schema check gaps
- `0500dd2`, `d69c98e`, `59820da` test: recover Room schemas 9–13, 14–17, 18–20
- `c531d4f` refactor: centralize Room migration registration
- `248f9b2` docs: add overhaul status handoff
- `4d32660` fix: refuse unsupported databases before opening them
- `b4ca727` test: cover every supported Room migration path
- `f0a3f06` test: assert exact values produced by each migration
- `5d18990` ci: run instrumentation tests on an API 29 emulator

## Verification actually run (2026-09-25, local, JDK Temurin 17)

- `./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug :app:compileDebugAndroidTestKotlin --stacktrace`: PASS at `4d32660` sources (230 unit tests, 0 failures). Later commits change only androidTest sources, CI config, and docs.
- `./gradlew --no-daemon :app:connectedDebugAndroidTest` on AVD `floor_api29` (Android SDK built for x86_64, API 29), started with `-read-only -no-snapshot`: 63 tests, 0 failures, at `f0a3f06` sources.
- `SCHEMA_BASE_REF=6aa07a3 scripts/check-room-schemas.sh --no-daemon`: PASS with schemas 9–21.
- GitHub Actions run 36120561721 (workflow_dispatch at `248f9b2`): success, including the schema check with all 13 files.
- GitHub Actions run 36123425188 (PR #2 at `2c940f2`): `Verify debug build` passed (schema check against the PR base, unit tests, lint, debug build, androidTest compilation); `Instrumentation tests (API 29 emulator)` passed, running 63 tests.
- Negative checks: drift check fails on tampered identity hash, tampered field, committed modification or deletion, bad base ref; `MigrationRegistrationTest` fails without `MIGRATION_15_16`; `MigrationPathTest` from 9 fails when the 9→10 tie-break is reversed; before the header guard, opening a version 8 or 22 rollback-journal database changed header bytes 18, 19, 27, 95.
- Each historical schema is byte-identical to the output of `scripts/recover-historical-room-schema.sh` for its introducing commit.

## Reviews

- Phase A: external review requested changes (4 Important, 10 Minor); all fixed except M-7 (build-cache input tracking, deferred while caching is off). Re-review approved; its 4 Minor notes were fixed in `e8708b9`.
- Phases B–F: reviewed inline. The user asked not to use subagents because of usage limits.

## Known limitations

- A corrupt database still makes queries that touch the damaged pages fail, which may crash the app. There is no in-app recovery or export for the `<name>.corrupt` copy yet.

- Share (community) import is not transactional; a failure midway leaves the rows imported so far.
- Orphan and unknown-type todo tasks are kept but not shown on the ToDo screen, so the user cannot delete them there.
- Exercises whose group name has no group row may not appear under any group in the exercise list; not yet verified.

- An unsupported database still crashes the app at startup (now with `UnsupportedDatabaseVersionException` and no file changes). There is no user-facing recovery screen yet.
- When a non-empty `-wal` or `-journal` exists, the header guard defers to Room; Room fails closed but SQLite may apply the pending journal.
- A corrupt database file reaches SQLite's default error handler, which deletes the file. Not yet addressed.
- Migrations 13→14, 14→15, and 18→19 drop and rebuild tables. They are safe because Room enables `foreign_keys` only in `onOpen`, after migrations; this is covered by the production-path tests.
- The schema check's base comparison is skipped for `workflow_dispatch` runs.
- GitHub branch protection is unavailable on the private free plan; merge gate is reviewed PR plus green CI.
- The local `gh` active account is `i252165-crypto`, which cannot see this repository. Commands use `GH_TOKEN=$(gh auth token --user WildeBeast2521)` for this repository without switching the global account.

## Environment notes

- Local AVDs exist (`floor_api29`, `s1_api28`, `s1_api36`, `r1_api28_arm64`) and `/dev/kvm` is available, so instrumentation tests can run headless locally. Record the AVD/API used.

## Next task

1. Return to the plan (`docs/plans/2026-08-30-overhaul-bootstrap.md`). Candidates:
   - a user-facing screen for `UnsupportedDatabaseVersionException`;
   - a user-reachable export of the raw database, and of the `.corrupt` copy, for manual recovery;
   - making todo tasks with a missing target visible so the user can delete them;
   - a transactional share import;
   - Task 6 (durable timer/workout state-machine spike).

Files likely involved next: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt`, `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/ToDoScreen.kt`, `app/src/main/java/io/github/gonbei774/calisthenicsmemory/viewmodel/TrainingViewModel.kt` (`importCommunityShare`).
