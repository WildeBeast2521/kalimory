# Overhaul status

Durable handoff for the multi-session overhaul. Update at every verified checkpoint. Record only verified facts.

## Current phase

Start flows. Branch `work/start-flows`, based on master `569be6b`.

- **Single-workout setup:** it starts ready. Sets and target come from the exercise's own targets, or the defaults Start already fell back to (3 × 10, and 5 s per rep for dynamic exercises). Start is no longer disabled with blank fields and no reason; "Apply Exercise Settings" still re-applies the targets.
- **Program start screen:**
  - one Start, pinned to the bottom with the play icon and always in view;
  - the small top-bar Start and the end-of-list Start are removed;
  - the settings section starts collapsed.

Verification:
- the full local gate (131/131 instrumented tests on API 29); `UnifiedWorkoutFlowTest` still starts single and program workouts;
- screenshots of the program start screen and the single setup.

## Previous phase: small audit fixes (merged)

PR #66 merged as `569be6b`.

## Previous phase: program runs use the in-workout kit (merged)

PR #65 merged as `bdd1221`.

## Previous phase: Baseline Profiles and Macrobenchmark (merged)

PR #64 merged as `5f03984`.

## Previous phase: Settings links point to this repository (merged)

PR #63 merged as `cf0865d`.

## Previous phase: workout run copy fixes (merged)

PR #62 merged as `5f529d6`.

## Previous phase: remove AppColors (merged)

PR #61 merged as `bc57a4b`.

## Previous phase: program and interval runs on the calm theme (merged)

PR #60 merged as `a3947cf`.

## Previous phase: data screens on the calm theme (merged)

PR #59 merged as `8a62c4c`.

## Previous phase: To Do and Record on the calm theme (merged)

PR #58 merged as `05fca7c`.

## Previous phase: Library editors on the calm theme (merged)

PR #57 merged as `ed4acdd`.

## Previous phase: Settings on the calm theme (merged)

PR #56 merged as `6d1619e`.

## Previous phase: Progress redesign (merged)

PR #55 merged as `79675a6`.

## Previous phase: Today polish (merged)

PR #54 merged as `d37a656`.

## Previous phase: Record saves sets as entered (merged)

PR #53 merged as `0cc58c9`.

## Previous phase: last backup time and keep screen on (merged)

PR #52 merged as `7151057`.

## Previous phase: catch-up audit recorded (merged)

PR #51 merged as `07b87a3`.

## Previous phase: glyph icons, second pass (merged)

PR #50 merged as `383aafe`.

## Previous phase: finish replacing glyph icons (merged)

PR #49 merged as `d82ddaf`.

## Previous phase: Quick start goes straight to choosing an exercise (merged)

PR #48 merged as `977baa9`.

## Improvement ideas (beyond upstream)

The owner said the overhaul need not mirror upstream (2026-09-28).
- Small items (S) become their own slices, or fold into the redesign of their screen.
- Medium (M) and large (L) items wait for the review with the owner at the end of Task 8, unless noted.

### Catch-up audit of earlier slices (2026-09-28)

The audit was a user walkthrough on the API 29 emulator with synthetic data. It found two defects, both fixed:
- glyph icons that PR #44 missed (PRs #49 and #50);
- the redundant mode picker (PR #48).

**Data safety** (owner decisions, 2026-09-28):
- Show the last backup time: done in PR #52.
- No reminders and no automatic backups: owner decision.
- The safety backup before an import already existed; the audit missed it.
- The backup description and the warning icon: fixed in PR #52.

**Today:**
- Done in PR #54: (S) A program hero shows only "Program"; add the exercise count and the estimate.
- Done in PR #54: (S) Due exercise rows say "Workout"; show the target (for example 3 x 12) or when it was last done.
- Done in PR #54: (S) Due rows lack the trailing start mark that Train rows have.
- Done in PR #54: (S) The gap between the "Due today" heading and its rows is too big; fixed on Train, not here.
- Done in PR #54: (S) Tabular figures make "15" read as "1 5" in value chips. Use them only where digits sit in columns.
- (M) Tapping a day in the week strip could open that day's history.
- (M) "Done today" has no totals, and no comparison with the last session.
- (M/L) "This week: 2 of 7" implies a seven-day goal. Weekly goals belong with the progression system's goals.

**Train and Library:**
- Done in PR #54: (S) A program with no exercises shows "~0 min".
- (S) Estimates disagree between screens for the same program (~12, ~13 and ~16 min, depending on the screen and the prefill mode). Label them consistently, or explain the difference.
- Done in PR #57: the exercise list title. Done in PR #66: the empty "Favorite" group is hidden.
- (S) Program list rows have no summary (exercise count, estimate), and tapping a row does nothing.
- (S) The interval list shows no total duration.
- Done in PR #66: per-exercise rest now reads "Rest" where it said "Interval".
- (M) Show when each exercise or program was last done.

**Workouts:**
- Done in PR #67: single-workout setup starts with targets or defaults filled in.
- Done in PR #67: the program start screen has one pinned Start and collapsed settings.
- Done in PR #65: the program run shows "Set 1/6" within the exercise.
- Done in PR #62: the two interval Start buttons, the missing spaces, and "NEXT" in capitals. Done in PR #66: the "Confirm" title.
- Program runs use the kit (PR #65). The interval run keeps its own timer screen for now; it is already on theme roles.
- Done in PR #53: (M) Record ("Log a past workout") behaves like a live session: a "NOW" badge, and every set must be completed before Record is enabled. For logging the past, allow saving all sets as shown.
- (L, owner-approved, after the item above) Log a past workout with several exercises in one session. The v2 model supports this, and ManualWorkoutWriter handles one exercise today.

**Progress (feeds its redesign):**
- Done in PR #55: (S) The week label reads "Sep 22 – 27" while the strip shows the 22nd to the 28th; check whether today is left out.
- Done in PR #55: (S) "1 sets"; the plural is missing.
- Done in PR #55: (S) Dates show as "2026-09-25"; use the locale's format.
- Done in PR #55: (S) "6reps" is missing a space. The right and left sides use arbitrary colours (green and purple).
- Done in PR #66: the Progress exercise sheet pads for the navigation bar.
- (S/M) Graph starts empty until an exercise is picked; default to the most recent exercise.
- (M) "1 Week" is a rolling seven days, while Today uses the calendar week and the first-day setting.
- (M) The Challenge tab does not say how to set a challenge, and overlaps with the progression goals.
- (L) Trends and personal bests (progression system).

**Settings:**
- Done in PR #63 (owner: "Yes, point"): source and issue links go to this repository, and upstream attribution stays.
- "Keep screen on" now defaults to on: owner-approved, done in PR #52.
- (M) Changing the language needs a restart. Per-app language (AndroidX) could apply it at once.

**Known limitations and ADR deferrals, re-judged:**
- The ADR 0001 non-goals (accounts, social features, hosted AI, cloud sync, telemetry) come from the owner's brief and its privacy rules, not from upstream. They stay.
- (M) "A recovery zip can only be restored manually" is a real gap. An in-app restore from a recovery zip would complete the corruption-recovery story.
- The other known limitations are technical safeguards, not product gaps.

**Performance:**
- Measured in PR #64. The 4 s was a debug build; release takes about 1.3 s on emulators. The Baseline Profile cuts tab-switching jank (P99 −55% on API 36). Frame timing inside a workout needs seeded data (follow-up).

## Previous phase: in-workout redesign (merged)

PR #47 merged as `4beda29`.

## Previous phase: Train and Library redesign (merged)

PR #46 merged as `2d41a69`.

## Previous phase: first day of the week (merged)

PR #45 merged as `b5d746e`. A Settings choice (Automatic, Monday, Saturday or Sunday) drives Today's week strip, the Progress calendar and the To Do repeat-day picker order.

## CI debt (merged without GitHub Actions)

GitHub Actions stopped starting jobs on 2026-09-27. The annotation says account payments failed or the spending limit needs raising. The owner decided: "Merge locally verified PRs for now, rerun CI later."

Each PR below passed the full local gate before merging:
- `testDebugUnitTest lintDebug assembleDebug`;
- `connectedDebugAndroidTest` on `floor_api29`;
- the schema check, when the schema changed.

Once Actions runs again, re-run CI on `master`, clear this list, and go back to merging only on green CI.

- PR #34: database 24 and backup format 10, interval settings.
- PR #35: interval workouts to v2.
- PR #36: unified workout flow tests.
- PR #37: wake-lock test fix for API 36.
- PR #38: design-system options plan (documentation only).
- PR #39: UI technology evaluation (documentation only).
- PR #40: design decision (documentation only).
- PR #41: toolchain upgrade.
- PR #42: design tokens.
- PR #43: Today redesign.
- PR #44: Material Symbols icons.
- PR #45: first day of the week.
- PR #46: Train and Library redesign.
- PR #47: in-workout redesign.
- PR #48: Quick start goes straight to choosing an exercise.
- PR #49: finish replacing glyph icons.
- PR #50: glyph icons, second pass.
- PR #51: catch-up audit (documentation only).
- PR #52: last backup time and keep-screen-on default.
- PR #53: Record saves sets as entered.
- PR #54: Today polish.
- PR #55: Progress redesign.
- PR #56: Settings on the calm theme.
- PR #57: Library editors on the calm theme.
- PR #58: To Do and Record on the calm theme.
- PR #59: data screens on the calm theme.
- PR #60: program and interval runs on the calm theme.
- PR #61: remove AppColors.
- PR #62: workout run copy fixes.
- PR #63: Settings links point to this repository.
- PR #64: Baseline Profiles and Macrobenchmark.
- PR #65: program runs use the in-workout kit.
- PR #66: small audit fixes.
- PR #67: start flows.

## Owner direction for later (2026-09-27)

- **One icon library** (2026-09-28): done in PR #44 with Material Symbols Rounded (Apache-2.0).

- **GitHub issue templates** (to-do; none exist yet, and `.github/` holds only `workflows`). Add `.github/ISSUE_TEMPLATE/` with:
  - a bug report asking for app version, Android version and device, and steps;
  - a feature request;
  - a `config.yml`.

  The bug template must tell reporters not to attach backups, database files or personal workout data, and to share only synthetic examples. The app collects no telemetry, so the template is the only diagnostic channel. Blank issues can stay allowed.
- **Project wiki** (deferred until the app is complete). Build a wiki modelled on the upstream project's Codeberg wiki, written once the overhaul is finished so it documents the final screens and flows. Keep upstream attribution where its structure or content is reused, and never push to upstream.

- The Task 8 UI must look premium and perform well. Use the installed design skills.
- **The design system is decided** (ADR 0004 decision 5): the app's own calm, focused design language on Material 3 Expressive, at the latest versions.
- A progression system (a full ranked catalogue, progressions, custom exercises, daily goals, demonstrations) comes after Task 8. It will be planned in depth first. Brief: `docs/plans/future-progression-system-brief.md`.

## Previous phase: program v2 write (merged)

PR #33 merged as `b4e8174`.

## Previous phase: manual-record v2 write (merged)

PR #32 merged as `4a145d0`.

## Previous phase: single-workout v2 write (merged)

PR #30 merged as `b4eb4cd`; PR #31 (to-do notes) as `6437fea`.

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

1. Research and write the UI/UX design-system plan for the owner to decide (ADR 0004 decision 2). No further visual work before that decision.
2. Re-run CI on `master` once GitHub Actions works again (see "CI debt").
3. Later, owner to-dos: GitHub issue templates, the progression system (plan first), and the wiki (after the app is complete).
