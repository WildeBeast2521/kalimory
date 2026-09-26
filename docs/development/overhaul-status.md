# Overhaul status

Durable handoff for the multi-session overhaul. Update at every verified checkpoint. Record only verified facts.

## Current phase

Single-exercise checkpoint and resume, branch `work/single-checkpoint`, based on master `d1cbb29`. The PR follows these checks.

- `feat: resume single-exercise workouts after process death`.
- Verification:
  - Gate PASS (281 unit tests).
  - `connectedDebugAndroidTest` on `floor_api29`: 78 tests, 0 failures.
  - Kill during a set and kill on the confirmation screen, both followed by resume and record, on the same AVD.

## Previous phase: program checkpoint and resume (merged)

PR #16 merged as `d1cbb29`.

## Previous phase: single-exercise execution timers (merged)

PR #15 merged as `de51f90`.

## Previous phase: program execution timers (merged)

PR #14 merged as `7d250b0`; PR CI run 36230206658 and post-merge `master` run 36230602665 passed.

## Previous phase: interval resume timing (merged)

PR #13 merged as `aa4bd46`; the reboot and clock-change protocol is recorded in `docs/development/workout-timer-spike.md`.

## Previous phase: interval checkpoint and resume (merged)

PR #12 merged as `a18b10b`; PR CI run 36169050766 and post-merge `master` run 36169697792 passed.

## Previous phase: interval screen on the reducer (merged)

PR #11 merged as `f1e3086`; PR CI run 36164763728 and post-merge `master` run 36165538007 passed.

## Previous phase: Task 6 spike (merged)

PR #10 merged as `8085783`; PR CI run 36160581697 and post-merge `master` run 36161423810 passed. It added the pure reducer, checkpoint, recovery, and atomic store.

## Previous phase: missing group rows (merged)

PR #9 merged as `eaa7dd9`; PR CI run 36156661163 (77 emulator tests) and post-merge `master` run 36157361490 passed. The user chose "recreate group rows" (2026-09-25).

## Earlier phase: share import atomicity and hidden todo tasks (merged)

PR #8 merged as `c74e000`; PR CI run 36149723855 (75 emulator tests) and post-merge `master` run 36150610815 passed.

## Previous phase: database recovery screen (merged)

PR #7 merged as `7420a28`; PR CI run 36143132719 (73 emulator tests) and post-merge `master` run 36143969836 passed. Unsupported, unopenable, or corruption-reported databases show `DatabaseUnavailableScreen`, which exports the raw files as a zip.

## Previous phase: corruption preservation (merged)

PR #6 merged as `583484a`. PR CI run 36139303039 (70 emulator tests) and post-merge `master` run 36140056456 passed. The user chose "copy aside, keep live" (2026-09-25). Room's default `onCorruption` deleted the database; now the files are copied once to `<name>.corrupt` and the live file is kept.

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

- Corruption found after start-up (damaged pages first touched by a later query) can still crash that screen. The next start shows the recovery screen, because the `.corrupt` copy needs attention.
- A recovery zip can only be restored manually. There is no in-app import for it.
- When a non-empty `-wal` or `-journal` exists, the header guard defers to Room; Room fails closed but SQLite may apply the pending journal.
- Migrations 13→14, 14→15, and 18→19 drop and rebuild tables. They are safe because Room enables `foreign_keys` only in `onOpen`, after migrations; this is covered by the production-path tests.
- The schema check's base comparison is skipped for `workflow_dispatch` runs.
- GitHub branch protection is unavailable on the private free plan; merge gate is reviewed PR plus green CI.
- The local `gh` active account is `i252165-crypto`, which cannot see this repository. Commands use `GH_TOKEN=$(gh auth token --user WildeBeast2521)` for this repository without switching the global account.

## Environment notes

- Local AVDs exist (`floor_api29`, `s1_api28`, `s1_api36`, `r1_api28_arm64`) and `/dev/kvm` is available, so instrumentation tests can run headless locally. Record the AVD/API used.

## Next task

1. Make `WorkoutTimerService` an adapter.
   - It shows the current step and remaining time from the reducer state, and relays pause and resume. It holds no domain logic.
   - Verify that it releases its wake lock when stopped and when the task is removed.
   - Record the result, then close Task 6 in `docs/development/workout-timer-spike.md`.
2. Then Task 7 of `docs/plans/2026-08-30-overhaul-bootstrap.md`: the v2 workout schema and conservative backfill. Read ADRs 0002 and 0003 first.

Files likely involved next: `service/WorkoutTimerService.kt`, `util/WakeLockManager.kt`, `AndroidManifest.xml`.
