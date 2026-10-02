# Overhaul status

Durable handoff for the multi-session overhaul. Keep it current: update it in every slice, record only verified facts, and delete what stops being true. Full details of finished work live in each PR and in git history, not here.

Last reviewed: 2026-10-02, at PR #106.

## Where things stand

- **Tasks 1 to 7 of the bootstrap plan are done.** They cover data safety, Room schema and migration hardening, durable workout timers with resume, and the additive v2 workout model with backup format 10.
- **Task 8, the new UI, is done.** The app has four destinations (Today, Train, Progress, Library) and one workout flow. Every screen uses the calm design (ADR 0004 decision 5). The Baseline Profile and Macrobenchmark are measured (PR #64).
- **Every workout source writes v2 sessions**, and history reads legacy and v2 together (`docs/development/v2-workout-history.md`).
- **Database version 25. Backup format 11.** See `docs/development/supported-database-versions.md` and `docs/development/backup-validation.md`.
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
     - Predictive back, done in PR #82: `MainActivity` computes every screen's `backTarget` in one place, and a `PredictiveBackHandler` scrubs a `SeekableTransitionState`. Releasing commits; cancelling animates back. Handlers inside screens still take precedence.
     - `PrimaryNavigationTest.predictiveBackCanBeCancelledOrCompleted` drives it through the dispatcher.
     - A live edge swipe on the API 36 emulator was not checked, because its software-rendered System UI stopped responding.
   - **Done in PR #72, in the workout kit, so single and program runs both get it:**
     - `RollingNumber` rolls changed digits up on dials and counts.
     - The last three seconds of a countdown pulse the ring.
     - The count and the set segments ease into their new colours.
     - `SetDoneBadge` draws its tick and fires a `Confirm` haptic. The new Settings switch "Vibrate When a Set Is Done" (on by default, 10 locales) turns the haptic off. `SetDoneHapticTest` covers the setting.
     - The main workout button and the step buttons squeeze their corners when pressed (`ButtonShapes`, `IconButtonDefaults.shapes()`).
   - **Verification:**
     - Checked on the API 29 emulator at an animator scale of 3.
     - That emulator logs no vibrations at all, so the haptic is proven by the test with a recording `HapticFeedback`, not on a device.
     - Frame timing inside a workout is still unmeasured. The benchmark journeys do not start a workout.
   - **Still open from MO:** predictive-back scrubbing, and a gliding navigation indicator.
2. **M2 with SP.**
   - **Done in PR #73: the workout summary.**
     - After a live single, program or interval workout is saved, `Screen.WorkoutSummary` opens over wherever the workout returns. Done or Back goes there.
     - What it shows: the totals count up (sets, reps, time held, minutes), each exercise's ring closes, today fills in on the week strip, and a new personal best lands in brass.
     - `WorkoutSummaryBuilder` is pure and unit-tested. A best is the highest single set compared with all earlier history. Weighted, assisted and distance sets are left out.
     - The last session appears as a plain fact, "Last time: 72 reps", not a signed difference. The first emulator run showed "−56" after a one-set workout, which read as a scolding.
     - The "sets recorded" snackbar for single and program workouts is gone, because the summary confirms the save.
   - **Done in PR #74: SP and the "Done today" totals.**
     - Today opens with this week's sets as a large rolling number, for example "11 sets this week". It uses proportional figures, because tabular ones split "11". The "Done today" card starts with totals ("8 sets, 93 reps").
     - Brass now means only in-progress or achievement: workout states, today's ring, personal bests and fully completed interval workouts. About 100 other uses changed:
       - favourite stars and interval markers became stone (secondary);
       - zero-value warnings moved to the secondary container;
       - text-field focus, checkboxes, tabs, To Do start buttons and loop tags became spruce (primary).
     - In the dark theme, a running timer dial glows faintly in its own colour. The summary rings are too small for a glow to show, so they have none.
     - `TodayScreenTest` now scrolls to the resume card before tapping it, because the hero number pushes it lower.
   - **Seen, not fixed:** the review screen before saving also says "Workout Complete", so the phrase appears twice in a row. Retitle it.
   - SP: a large "this week" number on Today, brass kept for in-progress and personal bests only, and a soft glow in dark mode.
3. **M1, done in PR #75:** tapping a day up to today in Today's week strip opens Progress › Calendar with that day selected (`ViewScreen(focusDate)`).
   - Each day is an equal-width cell, a button that names the full date and whether it was trained. `TodayScreenTest.tappingADayOpensItsHistoryInProgress` covers it.
   - **M3, done in PR #77:** "Last done Sep 27" (or "Done today") under exercises and programs.
     - Where: Train's program and interval rows, the program and interval lists, Today's due rows (not the hero card), and the Quick start exercise picker.
     - Sources: exercises from the merged history; programs and interval programs from `observeTemplateLastRuns` over the v2 sessions that name them.
     - Program runs saved before v2 carry no program link, so they are not counted rather than guessed from names.
     - `TemplateLastRunTest` covers the query.
4. **L1, done in PR #78:** log a past workout with several exercises.
   - "Add another exercise" on the entry screen sets the current exercise aside and returns to the list.
   - An "In this workout" card lists what is already entered. The list's "Record N exercises" button, or Record on the next exercise, saves them all as one MANUAL session with one occurrence per exercise, in order (`ManualWorkoutWriter.write(list)`).
   - Every entry takes the workout's current date, time and comment when it is recorded, so a date changed on a later exercise applies to all.
   - Leaving the list with exercises set aside asks before discarding them.
   - Tests: `ManualWorkoutWriterTest` (2 new) and `UnifiedWorkoutFlowTest.pastWorkoutWithSeveralExercisesIsOneSession`.
5. **M5, done in PR #79:** restore from a recovery zip inside the app, on the recovery screen and in Settings › Complete Backup.
   - The zip is staged and checked first: supported version and integrity. The current files are kept in a `before-restore` folder, which later recovery exports include.
   - A failed move is rolled back, and the app restarts.
   - `RecoveryRestoreTest` (4 tests) and an emulator walkthrough through the system file picker cover it.
6. **Also approved:**
   - M6, done in PR #76:
     - `Period.startDate(today, firstDay)` makes "1 Week" the calendar week from the user's first day. It applies to the Calendar, List, Graph (charts and statistics) and Challenge, so "this week" means the same everywhere. For example, the Progress week total now matches Today's hero number.
     - The Calendar's week row shows the whole week, with later days still to come.
     - `calculateStatistics` had also counted 8 days. That is fixed, and `PeriodStartTest` covers the helper.
     - Seen, not fixed: future days in the Calendar week row look like empty past days. Today outlines them instead.
   - L2, done in PR #81: interval runs use the workout kit.
     - `TimerDial` brings rolling digits, the last-seconds pulse and the dark-mode glow. `WorkoutHeader` has one segment per round, and `WorkoutStatus` names the phase.
     - Skip is the quiet kit button, and Stop is an outlined error button.
     - Phase labels are in sentence case ("Work", "Rest"), and the Next label is no longer brass.
     - Interval list estimates round like program estimates.

- **Not doing:** M4, changing the language without a restart.
- **Waiting for the progression system (PG):** trends and personal bests in depth. The Challenge tab became Progressions in PR #91.

## Progression system (decided 2026-10-01)

- **Research:** `docs/plans/2026-10-01-progression-system-research.md`. The owner page is https://claude.ai/artifact/PE43qfu7eVcwi5M8meKnWe.
- **Owner answers:**
  - The recommended options (P1–P10) are accepted.
  - free-exercise-db supplies facts only (names, muscles, equipment, level); instructions are rewritten and its photos are not used.
  - Demonstrations are a human figure with the worked muscles in red, plus a muscle map.
  - The system never restricts the user.
- **Decisions:**
  - ADR 0005: the progression model and the freedom rule.
  - ADR 0006: catalogue content, licensing, format and ids. Sources are recorded in `docs/catalogue-sources.md`.
  - ADR 0007: the data model. Database 25 adds `catalogId` and `chain_placements`; backup format 11.
  - ADR 0008: demonstrations. A muscle map, and our own keyframe figure with red working muscles.
- **Phases:**
  1. Data model: done in PR #88. It adds database 25 (`exercises.catalogId` with a unique index, and `chain_placements`) and backup format 11, with `ChainPlacementDao`.
     - Tests: a 24→25 migration test, a format 11 round trip, and validation tests.
     - Community share carrying `catalogId`: done in PR #92.
     - Anomalies for unknown catalogue ids: done in PR #89.
  2. Catalogue v1, core chains: in progress.
     - Done in PR #89: 9 chains and 53 steps (push-up, handstand push-up, dip, pull-up, row, squat, hip hinge, core, leg raise), generated from `scripts/catalogue/catalogue_spec.py`.
     - Every step has a difficulty, working and move-on standards, muscles, equipment, a description and cues, all in our own words.
     - `CatalogueTest` enforces the ADR 0006 rules. Backups report `UNKNOWN_CATALOGUE_ID`.
     - Translated in PR #96 into all nine other languages (`res/values-*/catalogue_strings.xml`, written by hand). The missing-translation allowance is gone, and `CatalogueTranslationTest` checks that every locale has every string, with the same number of cue lines. The generator only writes English, so a changed entry must be updated in each locale too.
     - These translations were written without a native speaker's review; Weblate contributors can improve them.
  3. Progressions screen in place of Challenge, and "add from catalogue": done.
     - Done in PR #90: Library has an "Exercise catalogue" row. It lists the chains, each chain lists its steps easiest first, and a step opens a sheet with its description, cues, muscles, equipment and standards.
     - "Add to my exercises" (`CatalogueLibrary`) makes an ordinary exercise: the chain becomes its group, the difficulty its level, the move-on standard its target. An exercise with the same name and kind (ignoring case) is linked instead of duplicated; one already linked to another step is left alone.
     - Tests: `CatalogueLibraryTest`.
     - Done in PR #91: the Challenge tab is now "Progressions".
       - "Your progressions" lists every chain with a step in the library. Each card shows the current step, how close the latest session came to the move-on standard, and the next step. A card opens its chain.
       - The current step is the hardest linked step that has been trained, or the easiest linked step before any training.
       - The move-on standard is the exercise's own target when set, so a target the user changed counts.
       - It is met when enough sets each reach the value. Progress counts the best sets, each up to that value.
       - The card then reads "Ready for the next step". It only suggests; nothing is locked.
       - The exercise targets the Challenge tab showed are kept below, under "Targets".
       - The rules are pure functions in `data/progression/ChainProgress.kt` (`Progressions`).
       - The Progress tab is remembered while a chain opened from it is on top.
       - Tests: `ProgressionsTest` (unit) and `ProgressionsViewTest`.
     - Following, done in PR #97:
       - A chain with a step in the library is followed unless the user turns off "Follow this progression" on its catalogue screen.
       - Unfollowed chains leave Progressions and Today's suggestions. Their training still rests the movement pattern.
       - With every chain unfollowed, Progressions says so instead of pointing to adding a step.
       - The preference stores the unfollowed chain ids; ADR 0007 records this.
     - Custom exercises placed in built-in chains (`chain_placements`) are not shown yet; there is no screen to place them.
     - Done in PR #92: community share carries `catalogId`.
       - Export writes each exercise's link. Older apps ignore the field, so the share format stays at version 1.
       - Validation rejects a malformed id or two exercises with the same id. It keeps an id this catalogue does not know.
       - Import links a new exercise only when no exercise here holds that step. Existing exercises are never changed by an import.
       - Tests: `CommunityShareCatalogIdTest` (unit) and `CommunityShareImporterTest`.
  4. Suggestions and the weekly goal: done.
     - Done in PR #93: the weekly goal.
       - Settings → "Weekly goal" offers Off, or 2 to 6 of 7 days. It is kept in `ProgressionPreferences`, not in backups, like the other settings.
       - With a goal, Today reads "This week: 2 of 4", and "Goal reached" once it is met. Without one it shows days trained of 7, as before.
       - Tests: `WeeklyGoalTest`.
     - Done in PR #94: today's suggestions.
       - Today has a "Suggested today" section: one row per followed chain, least recently trained first. Each row shows the step, its chain and target, and why.
       - A chain rests when it, or another chain of the same movement pattern, was trained today or yesterday. History holds dates, not hours, so rest is counted in days.
       - The target is double progression: the weakest of the counted sets plus one rep, or 5 s for a hold, up to the move-on standard. When fewer sets than the standard were done, all sets aim at the weakest first.
       - Once the standard is met, the next step is suggested at its working standard. If that step is not in the library, the row opens its chain.
       - After two sessions short of the working standard, the row names the easier step. It is only offered.
       - Tapping a row starts the exercise. The close button dismisses the chain until tomorrow.
       - Settings → "Suggestions on Today" turns them off entirely.
       - The rules are pure functions in `data/progression/Suggestions.kt`.
       - Tests: `SuggestionsTest` (unit) and `TodaySuggestionsTest`.
       - Done in PR #95: starting a suggestion opens the workout at the suggested sets and value. It comes before the exercise's own target and the "previous record" pre-fill, and the user can still change it. It applies only to the suggested exercise, not to one picked after going back.
  5. Demonstrations: in progress.
     - Done in PR #98: the muscle map on each step's sheet. It shows the body from the front and the back, with the main muscles in red and the helping muscles in lighter red.
       - The outlines come from react-body-highlighter (MIT). They are recorded in `docs/catalogue-sources.md` and listed in Open Source Licenses.
       - Lats and middle back share the drawing's one upper-back region.
       - The map is decorative: the same muscles are listed as text below it.
       - Tests: `MuscleMapTest`.
     - The owner chose the figure's look on 2026-10-01:
       - a three-quarter view, turned about 35 degrees and seen slightly from above, with near limbs darker, far limbs lighter and a floor shadow;
       - a body built from visible muscle shapes, with the worked ones in red.
     - Done in PR #99, stage 1 of 3: the engine, previewed in debug builds only.
       - `data/figure/` holds a 3D skeleton solved from joint angles, with hands or feet pinned by a two-bone solver; keyframed motions eased between poses; and an orthographic camera. Each motion can turn the camera, for example push-ups at 55 degrees.
       - `FigureView` draws depth-sorted tapered limbs with near/far shading and a floor shadow. With "Remove animations" on, it shows the hardest pose, still.
       - Settings → "Figure preview (debug)" exists in debug builds only. It shows the sample squat and push-up, with camera sliders.
       - Tests: `SkeletonTest`.
     - Done in PR #100, stage 2: the muscles.
       - `FigureMuscles` places a muscle lens on each body part for all 17 catalogue muscles, facing front, back, outward or inward. Lenses facing away from the camera are hidden, and those seen side-on narrow.
       - Worked muscles are red and helping ones lighter red, as on the muscle map. A part that carries a worked muscle is tinted towards red, so it reads at a distance.
       - The body is bulkier than in stage 1, so the muscles show.
       - Tests: `FigureMusclesTest`.
     - Owner feedback, 2026-10-02: stage 2 "looks like a mannequin". The owner wants "a human model which is at least muscular", to be reworked in a later session. Until then the figure stays in the debug preview, not in the user-facing UI.
     - Stage 3 (motion data), in progress:
       - Done in PR #101: the push-up chain. All 8 steps are built from one plank solver: the body turns about the toes or knees with the hands pinned, and the top and bottom come from the arm's reach.
       - Props (`Prop`) are drawn behind the figure: a bench for the incline and decline push-ups, and a wall for the wall push-up (seen from the side and a little behind).
       - Pinned hands now lie flat on their support.
       - `FigureMotion.stillAt` sets the hardest point; the archer push-up's is a quarter of the way through.
       - `StepMotions.forStep(id)` is the registry. The debug preview lists every step that has a motion.
       - Tests: `StepMotionsTest` (pins held, nothing through the floor, loops closed, push-ups go down).
       - Done in PR #102: the squat chain, 7 steps.
         - Feet are pinned. A pole is drawn for the assisted steps and a bench for the Bulgarian split squat.
         - Single-leg steps hold the free leg by its angles.
         - `poseAt(1f)` now returns the last keyframe exactly, so loops close without rounding drift.
       - Done in PR #103: the pull-up chain, 9 steps.
         - The hands are pinned to a bar prop. The body goes from a straight-arm hang to the chin over the bar, slightly behind it.
         - Negatives lower slowly and return quickly. The archer pulls towards each hand in turn; the one-arm steps keep the free arm by the side.
         - Elbows on an overhead grip now bend down and out, rather than backwards.
         - The scapular pull-up shows as a small rise, because the skeleton has no shoulder shrug.
       - Done in PR #104: the row chain, 5 steps. Rows are the push-up plank turned over: leaning back from the heels, hands on a bar over the chest.
         - The plank geometry now lives in a shared `Plank` helper.
         - Elbows follow one rule: pulling (hand above the shoulder) bends them towards the floor; pushing bends them back.
         - Debug builds accept `adb shell am start -n io.github.gonbei774.calisthenicsmemory/.MainActivity --es figure_step <step id>`, which opens the figure preview on that step for authoring checks.
       - Done in PR #105: the dip (4) and handstand push-up (5) chains, with parallel bars, a bench, and a wall with a cushion.
         - The pike push-ups are seen from the side, because the raised hips hide the head and arms from other angles.
       - Done in PR #106: the hip hinge (6), core (4) and leg raise (5) chains. Every one of the 53 catalogue steps now has a motion, which `StepMotionsTest` enforces.
         - Lying moves use a face-up body. In bridges the neck and arms counter-rotate, so the head and arms stay on the floor.
         - Nordic curls kneel with the shins flat and the ankles under an anchor prop.
         - Hanging leg raises keep the shoulders fixed under the pull-up bar.
     - Stage 3 is complete. The figure is still debug-only.
     - Next: the owner's muscular human style. After that, the figure goes on each step's sheet.
  6. Expansion: skills, the remaining chains, trends.

## Workout sounds (owner decisions, 2026-09-30)

- Review page: https://claude.ai/artifact/BYcDwNk4adB5YgSH2v5Uqz. The owner answered: "Same as you recommend."
  - S1: the "wood and brass" set.
  - S2: separate rep and hold-tick sounds.
  - S3: a short "done".
  - S4: duck other audio during cues.
  - S5: no "Classic" option.
- **Done in PR #84.** Upstream's three beeps are replaced by five sounds (`res/raw/sound_*.ogg`), all in D major:
  - countdown: a wood block;
  - go: a brass bell;
  - done: a marimba phrase ending on the bell, 1.3 s instead of 2.5 s;
  - rep: a soft high tick;
  - hold tick: a low wood tick.
- `SoundPlayer` requests transient "may duck" audio focus for each cue except the rep tick, and releases it when the sound ends. This was checked on the emulator through `MediaFocusControl`.
- The sounds are our own work, synthesised by `scripts/sounds/generate_workout_sounds.py`. `scripts/sounds/build-sounds.sh` encodes them byte-for-byte reproducibly.
- `SoundFilesTest` keeps the ducking windows equal to the sound lengths.
- Not heard by a person in this session: the owner listened on the review page, and the shipped files come from the same generator.

## Onboarding (done in PR #85)

- `WelcomeGuide` is a four-page welcome, built to the plan the owner approved on 2026-09-30, minus the sounds and vibration explanation.
- It shows once on first launch (`OnboardingPreferences`), and upgrades from upstream see it too. Skip is always there, and Back steps to the previous page. Settings › "Welcome guide" reopens it.
- Pages:
  1. What the app is.
  2. The four places.
  3. How a workout runs, pictured with the real `TimerDial`.
  4. Your data, where the backup path is built from the app's own Settings and backup labels. It ends with "Add your first exercise" (opens Library › Exercises) or "Go to Today".
- Strings are in all 10 locales. Pages have headings and announce their position.
- Tests:
  - `WelcomeGuideTest` covers first launch, Skip, the next launch, walking to the end and reopening from Settings.
  - `WelcomeSeenRule` keeps the other launch tests on Today.
  - The Baseline Profile journeys tap Skip on a fresh install.

## Open small fixes

Done in PR #80:
- Program estimates agree everywhere. Every screen now uses the start countdown setting and the same rounding (`ProgramTimeEstimator.startCountdownSeconds`, `formatMinutes`). The start screen differs only when a run's values change.
- The Graph axis shows numbers only and names the unit once, so "1 reps" is gone.
- The Progress period chips scroll to the screen edge and fade while more lie beyond.
- The screen before saving is titled "Check your sets", so "Workout Complete" no longer appears twice.
- Future days in the Calendar week row are faint and cannot be selected.
- `program_result_zero_warning` now says 0 sets are saved as skipped and hidden from history. It is in all 10 locales.

Still open:
- A gliding navigation indicator (from MO). It needs a custom bar; Material's grow-in animation is used meanwhile.

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

PRs #34 to #106. PRs #70, #86 and #87 changed documentation only.

## How to verify (the local gate)

```bash
export ANDROID_SERIAL=emulator-5554 JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ANDROID_HOME="$HOME/Android/Sdk"
adb shell pm clear io.github.gonbei774.calisthenicsmemory   # seeded data breaks ProgressHistoryTest
./gradlew testDebugUnitTest lintDebug assembleDebug :app:connectedDebugAndroidTest
```

- Expected as of PR #106: 422 unit tests, 163 instrumented tests on `floor_api29` (API 29), and "Lint found 4 warnings" or 6. The count varies with lint's online version lookup: two `GradleDependency` notices come and go. The rest are `NewerVersionAvailable` and `UseTomlInstead`.
- Also run `scripts/check-room-schemas.sh` when a schema changes.
- **Always pin `ANDROID_SERIAL`.** The owner's own phone, which holds real data, can appear on wireless adb. Never install, test, clear or seed on it.
- Start the emulator headless: `emulator -avd floor_api29 -no-window -no-audio -no-boot-anim -read-only -no-snapshot -gpu swiftshader_indirect`. Other local AVDs: `s1_api28`, `s1_api36`, `r1_api28_arm64`.
- Regenerate the Baseline Profile with `./gradlew :app:generateBaselineProfile` (run `adb root` first). Benchmarks need API 31 or newer for frame timing.
- A local `assembleRelease` fails without a keystore. Use the `nonMinifiedRelease` or benchmark variants, which are debug-signed.
- `gh` has two accounts. Use `GH_TOKEN=$(gh auth token --user WildeBeast2521)` for this repository.

## Known limitations

- Corruption found after start-up can still crash that screen. The next start shows the recovery screen.
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
