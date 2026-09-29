# Overhaul status

Durable handoff for the multi-session overhaul. Keep it current: update it in every slice, record only verified facts, and delete what stops being true. Full details of finished work live in each PR and in git history, not here.

Last reviewed: 2026-09-30, at PR #71.

## Where things stand

- **Tasks 1 to 7 of the bootstrap plan are done.** They cover data safety, Room schema and migration hardening, durable workout timers with resume, and the additive v2 workout model with backup format 10.
- **Task 8, the new UI, is done.** The app has four destinations (Today, Train, Progress, Library) and one workout flow. Every screen uses the calm design (ADR 0004 decision 5). The Baseline Profile and Macrobenchmark are measured (PR #64).
- **Every workout source writes v2 sessions**, and history reads legacy and v2 together (`docs/development/v2-workout-history.md`).
- **Database version 24. Backup format 10.** See `docs/development/supported-database-versions.md` and `docs/development/backup-validation.md`.
- **Toolchain:** Gradle 9.8, AGP 9.4.1 (built-in Kotlin), Kotlin 2.4.20, Room 2.8.5 with KSP, compileSdk 37, targetSdk 35, minSdk 26, Compose BOM 2026.09.00, material3 1.5.0-alpha29. The source of truth is `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- **Not released.** `versionName` is still upstream's 1.26.0.

## Next work (owner-approved at the end-of-redesign review, 2026-09-29)

The review page, with live motion previews: https://claude.ai/artifact/6JM7EwpckEkJfCkPP3hC52. The owner's answer: "Do all except M4." The summary screen is "fine as you showed". Do the items in this order, one verified slice each:

1. **MO, the motion package.**
   - **Done in PR #71:**
     - Screens slide forward and back. `AnimatedContent` runs over `currentScreen`, and `Screen.depth()` in `ui/navigation/ScreenMotion.kt` picks the direction. `ScreenDepthTest` checks that every back destination slides back.
     - Tabs fade through.
     - The navigation bar slides away on pushed screens.
     - The theme uses `MotionScheme.expressive()`.
     - The Baseline Profile is regenerated.
   - **PR #71 verification on emulators:**
     - Recorded with `animator_duration_scale 10`, and checked with the scale at 0 (Remove animations), where screens switch at once.
     - Tab-switch benchmark on API 36 (median of 10), against master: per-frame CPU time P50 +5%, P90 +8%, P99 −23%. The crossfade draws two screens briefly. On this software-rendered emulator every frame overruns either way.
   - **Differences from the review page:**
     - The navigation indicator keeps Material's own grow-in animation rather than gliding between tabs. A glide needs a custom bar.
     - Predictive back, where the back swipe scrubs the slide, is not done. Every screen declares its own back destination and side effects in `MainActivity`, so scrubbing needs those moved into one place first. That is a follow-up.
   - **Next slice:** timer and count digits roll, a finished set turns spruce with a drawn tick and a haptic (with a setting to turn vibration off), and buttons change shape when pressed.
2. **M2 with SP.**
   - M2: a workout summary screen after the last set. Rings close, totals count up, the week dot fills, and a personal best appears in brass. There is no confetti. It includes the "Done today" totals and a comparison with the last session.
   - SP: a large "this week" number on Today, brass kept for in-progress and personal bests only, and a soft glow in dark mode.
3. **M1:** tapping a day in the week strip shows that day's history. **M3:** show when each exercise and program was last done.
4. **L1:** log a past workout with several exercises. `ManualWorkoutWriter` handles one exercise today; the v2 model supports more.
5. **M5:** restore from a recovery zip inside the app.
6. **Also approved:**
   - M6: Progress "1 Week" follows the calendar week and the first-day setting, as Today does.
   - L2: interval runs on the workout kit.
   - The small fixes listed below.

- **Not doing:** M4, changing the language without a restart.
- **Waiting for the progression system (PG):** weekly goals (Today's "This week: 2 of 7"), the Challenge tab's purpose, and trends and personal bests in depth.

## Open small fixes

- Estimates disagree between screens for the same program (about 12, 13 and 16 min). Label them consistently, or explain the difference.
- The Graph axis says "1 reps"; the unit suffix is not pluralised.
- The period chips on Progress clip at the screen edge, with no hint that they scroll.
- `program_result_zero_warning` says zero-value sets "will not be saved". v2 stores them as skipped sets and hides them from history. Reword it in every locale.
- `util/WakeLockManager.kt` is unused. Remove it in a cleanup change.

Log new ideas here while working. Small ones become their own slice; ask the owner before larger features.

## Owner direction for later

- **Progression system:** the biggest planned upgrade. The brief is `docs/plans/future-progression-system-brief.md`. Do not start until the owner says so, and plan it in depth first.
- **GitHub issue templates:** none exist yet (`.github/` holds only `workflows`). Add `.github/ISSUE_TEMPLATE/` with:
  - a bug report asking for the app version, the Android version and device, and steps;
  - a feature request;
  - a `config.yml`.

  The bug template must tell reporters not to attach backups, database files or personal workout data. Blank issues can stay allowed.
- **Project wiki:** after the app is complete, modelled on upstream's Codeberg wiki, keeping attribution where its structure or content is reused.
- **README, screenshots and the changelog** still describe upstream v1.26.0. Rewrite them for the new app at release time, together with the wiki.

## CI debt (merged without GitHub Actions)

GitHub Actions stopped starting jobs on 2026-09-27: the account's payment failed, or its spending limit needs raising. The owner decided: "Merge locally verified PRs for now, rerun CI later."

Each PR below passed the full local gate before merging (see "How to verify"). Once Actions runs again, re-run CI on `master`, clear this list, and go back to merging only on green CI.

PRs #34 to #71. PR #70 changed documentation only.

## How to verify (the local gate)

```bash
export ANDROID_SERIAL=emulator-5554 JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ANDROID_HOME="$HOME/Android/Sdk"
adb shell pm clear io.github.gonbei774.calisthenicsmemory   # seeded data breaks ProgressHistoryTest
./gradlew testDebugUnitTest lintDebug assembleDebug :app:connectedDebugAndroidTest
```

- Expected as of PR #71: 340 unit tests, 131 instrumented tests on `floor_api29` (API 29), and "Lint found 6 warnings" (baselined dependency-version notices).
- Also run `scripts/check-room-schemas.sh` when a schema changes.
- **Always pin `ANDROID_SERIAL`.** The owner's own phone, which holds real data, can appear on wireless adb. Never install, test, clear or seed on it.
- Start the emulator headless: `emulator -avd floor_api29 -no-window -no-audio -no-boot-anim -read-only -no-snapshot -gpu swiftshader_indirect`. Other local AVDs: `s1_api28`, `s1_api36`, `r1_api28_arm64`.
- Regenerate the Baseline Profile with `./gradlew :app:generateBaselineProfile` (run `adb root` first). Benchmarks need API 31 or newer for frame timing.
- A local `assembleRelease` fails without a keystore. Use the `nonMinifiedRelease` or benchmark variants, which are debug-signed.
- `gh` has two accounts. Use `GH_TOKEN=$(gh auth token --user WildeBeast2521)` for this repository.

## Known limitations

- Corruption found after start-up can still crash that screen. The next start shows the recovery screen.
- A recovery zip can only be restored manually (M5 above).
- When a non-empty `-wal` or `-journal` file exists, the header guard defers to Room. Room fails closed, but SQLite may apply the pending journal.
- Migrations 13→14, 14→15 and 18→19 drop and rebuild tables. They are safe because Room enables `foreign_keys` only in `onOpen`, after migrations. Tests cover this.
- Legacy writers remain: the CSV record import and the history editor for legacy rows. `V2Backfill` has not run. It belongs to the later step that retires `training_records`.
- The schema check's base comparison is skipped for `workflow_dispatch` runs.
- GitHub branch protection is unavailable on the private free plan.

## Merged PRs

Merge commit, then summary. Each PR description holds its verification.

| PR | Merge | Summary |
|---:|:---|:---|
| 1 | `6aa07a3` | Phase 0: data-safe foundation, destructive fallback removed |
| 2 | `0efcfb1` | Room schema export and migration coverage 9 to 21 |
| 3, 5 | `956f19a`, `5519fe5` | Status records |
| 4 | `c619f0a` | Every exported backup restores; no orphan or cascade loss |
| 6 | `583484a` | Keep the database when SQLite reports corruption |
| 7 | `7420a28` | Recovery screen when the database cannot open |
| 8 | `c74e000` | Atomic share import; removable orphan to-dos |
| 9 | `eaa7dd9` | Recreate missing group rows |
| 10 to 18 | `8085783` to `a15647a` | Task 6: workout reducer, monotonic timers, resume for every mode, wake-lock release |
| 19 to 23 | `10d7d35` to `d4914b4` | Task 7: v2 tables (databases 22 and 23), backfill, compatibility reader, backup format 9 |
| 24, 25 | `44f03b8`, `e3d1f27` | Material 3 Expressive and the four destinations |
| 26 to 29 | `dc660cc` to `c2d0402` | Today resume and due items; history on the compatibility reader; v2 editing |
| 30 to 35 | `b4eb4cd` to `9af9219` | Single, manual, program and interval workouts write v2 (database 24, backup format 10) |
| 36, 37 | `9515236`, `4c4e575` | Unified workout flow tests; wake-lock test on API 36 |
| 38 to 40 | `872b5d9` to `d280468` | Design-system research and decision (ADR 0004 decision 5) |
| 41 | `878c7fb` | Toolchain upgrade (AGP 9, Kotlin 2.4, compileSdk 37) |
| 42 to 45 | `1d900c7` to `2bfdb8d` | Calm tokens, Today redesign, Material Symbols, first day of the week |
| 46 to 50 | `2d41a69` to `383aafe` | Train, Library and workout redesign; quick start; glyph icons removed |
| 51 | `07b87a3` | Catch-up audit of earlier slices |
| 52, 53 | `7151057`, `0cc58c9` | Last backup time; screen kept on by default; Record saves sets as entered |
| 54 to 61 | `d37a656` to `bc57a4b` | Every remaining screen moved to the calm theme; `AppColors` removed |
| 62, 63 | `5f529d6`, `cf0865d` | Workout copy fixes; Settings links point to this repository |
| 64 | `5f03984` | Baseline Profile and Macrobenchmark |
| 65 to 69 | `bdd1221` to `01f3ffa` | Program runs on the workout kit; audit fixes; start flows; list summaries; Graph axis |
