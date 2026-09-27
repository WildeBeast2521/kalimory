# Overhaul status

Durable handoff for the multi-session overhaul. Update at every verified checkpoint. Record only verified facts.

## Current phase

Task 8, slice 7: single-exercise workouts are saved as v2 sessions. Branch `work/v2-single-write`, based on master `c2d0402`.

See "Write path: single-exercise workouts" and "Backfill timing (revised)" in `docs/development/v2-workout-history.md`.

Verification:
- `SingleWorkoutWriterTest` (JVM, 6 tests): the mapping.
- `SingleWorkoutWriteTest` (database, 2 tests): the write, and how history shows it.
- The checkpoint round trip and an old checkpoint without a start time.
- A real workout on the emulator: 4 sets, one completed with the Complete button, one typed on the confirmation screen, two aborted. The result was one session with the observed times, 2 completed and 2 skipped sets, no legacy row, and it appeared in Today and Progress.

## Owner direction for later (2026-09-27)

- **GitHub issue templates** (to-do; none exist yet, and `.github/` holds only `workflows`). Add `.github/ISSUE_TEMPLATE/` with:
  - a bug report asking for app version, Android version and device, and steps;
  - a feature request;
  - a `config.yml`.

  The bug template must tell reporters not to attach backups, database files or personal workout data, and to share only synthetic examples. The app collects no telemetry, so the template is the only diagnostic channel. Blank issues can stay allowed.

- The Task 8 UI must look premium and perform well. Use the installed design skills.
- **The design system is reopened.** The owner does not want Material 3 Expressive locked in: the current implementation may be replaced if something is better. Before more visual work, write a full UI/UX research plan and compare the options (M3 Expressive, custom on Compose Foundation, others) on look, performance, accessibility, maintenance and license. The owner decides from that plan. Until then, only structural work continues (navigation, data paths). Its visuals are provisional.
- A progression system (a full ranked catalogue, progressions, custom exercises, daily goals, demonstrations) comes after Task 8. It will be planned in depth first. Brief: `docs/plans/future-progression-system-brief.md`.

## Previous phase: v2 history editing (merged)

PR #29 merged as `c2d0402`.

## Previous phase: remaining history readers (merged)

PR #28 merged as `59a7c09`.

## Previous phase: history screens on the compatibility reader (merged)

PR #27 merged as `3529674`.

## Previous phase: Today resume and due to-dos (merged)

PR #26 merged as `dc660cc`.

## Previous phase: primary destinations (merged)

PR #25 merged as `e3d1f27`.

## Previous phase: Material 3 Expressive dependency (merged)

PR #24 merged as `44f03b8`.

## Previous phase: Task 7 closed (merged)

PR #23 merged as `d4914b4` (documentation only).

## Previous phase: v2 in the JSON backup, Task 7 slice 4 (merged)

PR #22 merged as `6404b91`: backup format 9.

## Previous phase: compatibility reader, Task 7 slice 3 (merged)

PR #21 merged as `53061da`.

## Previous phase: conservative backfill, Task 7 slice 2 (merged)

PR #20 merged as `b808a24`: database 23 and `V2Backfill`.

## Previous phase: v2 tables, Task 7 slice 1 (merged)

PR #19 merged as `10d7d35`: database 22 with `workout_sessions`, `session_exercises`, and `set_entries`.

## Previous phase: timer service wake lock, completing Task 6 (merged)

PR #18 merged as `a15647a`.

## Previous phase: single-exercise checkpoint and resume (merged)

PR #17 merged as `9eef2af`.

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

1. Save manual records (Record Training) as v2 sessions. The user picks the date and time, so they are MINUTE precision.
2. Save program workouts as v2 sessions (PROGRAM_TEMPLATE, with the loop and round structure), then interval workouts (INTERVAL_TEMPLATE). Then add `UnifiedWorkoutFlowTest`.
3. Before any further visual work: research and write the UI/UX design-system plan (ADR 0004 decision 2 is reopened). Build it only after the owner chooses.

Files likely involved next: `ui/screens/RecordScreen.kt`, `ui/screens/ProgramExecutionScreen.kt`, `util/ProgramExecutionUtils.kt`, `data/v2/`.
