# Overhaul status

Durable handoff for the multi-session overhaul. Update at every verified checkpoint. Record only verified facts.

## Current phase

Room schema and migration hardening is complete. PR #2 (https://github.com/WildeBeast2521/CalisthenicsMemory/pull/2) merged into `master` as `0efcfb1` after its CI run 36123425188 passed both jobs. The post-merge `master` run 36124399180 also passed both jobs. No phase is in progress.

| Phase | State |
|:---|:---|
| A — export current Room schema | Done, externally reviewed and approved |
| B — recover historical schemas 9–20 | Done, self-reviewed |
| C — centralize migration registration | Done, self-reviewed |
| D — repair and expand migration tests | Done, run on emulator |
| E — semantic migration preservation | Done, run on emulator |
| F — CI migration gate (emulator) | Done; PR CI ran 63 instrumentation tests on an API 29 emulator and passed |

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

1. Choose the next phase from `docs/plans/2026-08-30-overhaul-bootstrap.md` and the ADRs in `docs/architecture/`.
2. Candidates from this phase's limitations:
   - a user-facing screen for `UnsupportedDatabaseVersionException` instead of a start-up crash;
   - a corruption callback that preserves the database file instead of deleting it.
3. Start each on a new branch and worktree from `master`.

Files likely involved next: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt`, `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/InstalledDatabaseVersion.kt`, the start-up path that calls `AppDatabase.getDatabase`.
