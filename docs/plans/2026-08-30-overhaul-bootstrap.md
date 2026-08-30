# Calisthenics Memory Overhaul Bootstrap Plan

> **For Hermes:** Execute this plan task-by-task, preserving the pending checkboxes until each acceptance gate has actually been run and verified.

**Goal:** Establish a private, reproducible, data-safe foundation for an offline personal calisthenics trainer, then introduce a unified durable workout model and UX without losing v1.26.0 data.

**Architecture:** Keep the existing single `:app` Kotlin/Compose/Room application and add seams inside it. Harden backup and Room migrations before introducing additive v2 entities. Build one durable workout state machine that specialized ad-hoc, program, and interval presentations can drive.

**Tech stack:** Kotlin 2.0.21, Android Gradle Plugin 8.13.0, Java 17, Jetpack Compose, Room 2.6.1, kotlinx.serialization, JUnit 4, AndroidX instrumentation/Compose tests (`gradle/libs.versions.toml:1-10`; `app/build.gradle.kts:85-135`).

**Global constraints:** GPL-3.0, offline-first, no telemetry, portable versioned export, no accounts/social/hosted AI/video hosting/cloud sync, no premature Gradle-module split, and no DI framework prerequisite.

**Command convention:** Run repository commands from the repository root with:

```bash
export JAVA_HOME="$HOME/.local/share/jdks/temurin-17"
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

Every command block below, including every Android/Gradle command, inherits these exported values. Every implementation item below is intentionally **pending**. Do not mark an item complete from intent or from another branch's report.

**Resolved foundation choices:** GitHub is the private host, `WildeBeast2521/CalisthenicsMemory` is the private repository, GitHub is `origin`, Codeberg is the fetch-only/no-push `upstream`, and GitHub Actions at `.github/workflows/android-ci.yml` is the selected CI path.

**Unresolved decisions and verification:** Declare which legacy database versions are supported; define the timezone policy for legacy local timestamps; confirm device/emulator availability for instrumentation; and verify/configure default-branch protection. No build, test, device, or branch-protection success is claimed by this plan.

---

## Task 1: Bootstrap the private working repository

**Files:**
- Inspect: `LICENSE`
- Inspect: `README.md`
- Inspect: `.gitignore`
- Create or modify only if needed: `CONTRIBUTING.md`
- Create (mandatory pending deliverable): `docs/development/private-repository.md`

The completed bootstrap baseline is concrete: the private GitHub repository is `WildeBeast2521/CalisthenicsMemory`; its `origin` was verified private; Codeberg is retained as a fetch-only/no-push `upstream`; upstream GPL-3.0 history and attribution were preserved; and all 35 tags were pushed to the private GitHub repository. These are repository facts, not completed implementation tasks in this plan.

- [ ] Configure available default-branch protections now, including reviewed changes and non-force-push protection; defer required CI status enforcement until Task 5 has created and successfully run `.github/workflows/android-ci.yml`.
- [ ] Audit ignores for `local.properties`, `keystore.properties`, `*.jks`, `*.keystore`, Android Studio state, APK/AAB outputs, database files, and exported personal backup files.
- [ ] Create `docs/development/private-repository.md` and document the private/fetch-only remote setup, fork/update workflow, branch-protection state, and the rule that fixtures contain synthetic data only.

**Commands:**

```bash
git status --short --branch
git remote -v
git log -5 --oneline
git check-ignore -v local.properties keystore.properties test.jks personal-backup.json
```

**Acceptance gate:** Reconfirm the recorded private/fetch-only remote configuration without printing secrets; verify the currently available review and non-force-push protections without requiring a not-yet-created CI status; create `docs/development/private-repository.md`; keep GPL files/history present; and complete a clean credential/personal-data scan of tracked files plus the documented synthetic-fixture workflow. Required CI status enforcement remains a Task 5 gate.

**Commit:**

```bash
git add .gitignore CONTRIBUTING.md docs/development/private-repository.md
git commit -m "chore: bootstrap private overhaul repository"
```

## Task 2: Capture a reproducible v1.26.0 baseline

**Files:**
- Inspect: `settings.gradle.kts`
- Inspect: `gradle/libs.versions.toml`
- Inspect: `app/build.gradle.kts`
- Create in Task 5 and use as the selected CI path: `.github/workflows/android-ci.yml`
- Create: `docs/development/baseline-v1.26.0.md`

The app currently declares version `1.26.0`, min SDK 26, target/compile SDK 35, and Java/Kotlin target 17 (`app/build.gradle.kts:20-31,70-77`). Record host prerequisites and actual command output without claiming unavailable device tests passed. GitHub Actions is the selected CI system, with the concrete workflow path `.github/workflows/android-ci.yml`.

- [ ] Record the base commit SHA, Gradle/AGP/Kotlin/JDK versions, Android SDK packages, and clean-tree state.
- [ ] Run the debug assemble, JVM unit tests, lint, and dependency report with the pinned JDK.
- [ ] On an API 26+ emulator/device, run instrumentation tests; record device/API identity and failures separately.
- [ ] Save no generated build products or personal device data in Git.

**Commands:**

```bash
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew --version
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew clean :app:assembleDebug
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:testDebugUnitTest
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:lintDebug
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:dependencies
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest
```

**Acceptance gate:** `assembleDebug`, JVM tests, and lint have captured exit codes and logs; connected tests are either verified on a named device/API or explicitly recorded as not run/blocked. Baseline failures become tracked issues before behavior changes.

**Commit:**

```bash
git add docs/development/baseline-v1.26.0.md
git commit -m "docs: record v1.26.0 build baseline"
```

## Task 3: Phase 0 data-safety contract

**Files:**
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/BackupDao.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/viewmodel/BackupService.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/BackupFileIo.kt`
- Modify: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/viewmodel/TrainingViewModel.kt`
- Modify: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/BackupScreen.kt`
- Modify: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt`
- Add: `app/src/test/java/io/github/gonbei774/calisthenicsmemory/BackupValidationTest.kt`
- Add: `app/src/test/java/io/github/gonbei774/calisthenicsmemory/BackupFileIoTest.kt`
- Add: `app/src/androidTest/java/io/github/gonbei774/calisthenicsmemory/BackupDaoTest.kt`

The current UI starts a save-before-import flow (`BackupScreen.kt:78-115`) and limits imports to 50 MB before decoding (`BackupScreen.kt:117-168`), but implementation must prove that the backup is writable/readable and that destructive replacement cannot start after cancellation or validation failure. Preserve the existing JSON v8 format and readers exactly; Phase 0 introduces no speculative v9 envelope.

- [ ] Write failing tests for JSON v8 validation, malformed JSON, missing references, invalid metrics, oversized input, and round-trip preservation.
- [ ] Make export deterministic enough for semantic comparison and include every persisted user-owned entity.
- [ ] Return typed results from backup validation, file I/O, snapshot, and replacement operations; parse and validate before any mutation.
- [ ] Require a successful, readable pre-import backup for complete-overwrite import; verify it can be decoded before enabling final confirmation.
- [ ] Make `BackupDao` own transactional snapshot/replace behavior so replacement is atomic, and report typed success/warning/error results with entity counts.
- [ ] Test cancellation and injected failures before, during, and after staging; current data must remain readable.
- [ ] Remove `.fallbackToDestructiveMigration()` immediately in Phase 0 so missing or unsupported migrations fail closed without erasing database bytes.

**Commands:**

```bash
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:testDebugUnitTest --tests '*BackupValidationTest'
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:testDebugUnitTest --tests '*BackupFileIoTest'
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.gonbei774.calisthenicsmemory.BackupDaoTest
```

**Acceptance gate:** Synthetic full export/import round trips preserve semantic entity counts and values; malformed/cancelled imports do not delete current rows; a readable pre-import backup is required and tested; no fixture contains personal data.

**Frequent commits:**

```bash
git commit -m "test: define backup validation contract"
git commit -m "feat: validate backups before replacement"
git commit -m "test: cover transactional import recovery"
```

## Task 4: Harden Room schema and migration coverage

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt`
- Modify: `app/src/androidTest/java/io/github/gonbei774/calisthenicsmemory/MigrationTest.kt`
- Create: `app/src/androidTest/java/io/github/gonbei774/calisthenicsmemory/AllMigrationPathsTest.kt`
- Create: `app/src/androidTest/java/io/github/gonbei774/calisthenicsmemory/MigrationAnomalyTest.kt`
- Create: `docs/development/supported-database-versions.md`
- Create and commit generated schemas under: `app/schemas/io.github.gonbei774.calisthenicsmemory.data.AppDatabase/`
- Add synthetic legacy databases under: `app/src/androidTest/assets/databases/`

Room currently has schema export disabled and destructive fallback enabled (`AppDatabase.kt:11-15,33-42`). Existing migration tests focus on 10-to-11 (`MigrationTest.kt:14-17`) and do not prove the full registered 9-to-21 chain.

Task 4 must not begin until pending Phase 0 has removed destructive fallback. This task expands migration evidence and recovery UX; it does not re-enable destructive erasure.

- [ ] Before accepting fixture or matrix work, create `docs/development/supported-database-versions.md` with the explicit supported source-version list and the fail-closed recovery policy for unsupported versions.
- [ ] Configure KSP/Room schema output and commit the current schema as the baseline for all future versions.
- [ ] Build synthetic fixtures for each source version declared supported in `docs/development/supported-database-versions.md` and edge cases: orphans, unknown strings, duplicate-looking names, malformed date/time, nullable metrics, unilateral records, and interval JSON.
- [ ] Write a migration matrix that opens each source version at latest, validates schema, runs foreign-key checks, and compares row counts/representative values.
- [ ] Add structured anomaly reporting; never silently coerce or discard a row.
- [ ] Add fresh-install and unsupported-version tests proving unsupported databases fail closed and preserve their bytes.

**Commands:**

```bash
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:kspDebugKotlin
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.gonbei774.calisthenicsmemory.MigrationTest
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.gonbei774.calisthenicsmemory.AllMigrationPathsTest
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.gonbei774.calisthenicsmemory.MigrationAnomalyTest
```

**Acceptance gate:** `docs/development/supported-database-versions.md` exists and explicitly defines supported source versions and recovery policy; fixtures and the migration matrix cover and compare against that declaration; every declared supported source version reaches latest without row loss in synthetic fixtures; schema validation and foreign-key checks pass; anomalies are counted with reasons; unsupported versions fail closed and preserve the documented recovery options; the Phase-0 removal of destructive fallback remains enforced.

**Frequent commits:**

```bash
git commit -m "build: export Room schemas"
git commit -m "test: add supported migration matrix"
git commit -m "feat: report migration anomalies"

```

## Task 5: Establish CI gates

**Files:**
- Create: `.github/workflows/android-ci.yml`
- Create: `docs/development/ci.md`

GitHub Actions at `.github/workflows/android-ci.yml` is the mandatory CI implementation for this overhaul.

- [ ] Pin Java 17 and document Android SDK/cache setup without committing credentials.
- [ ] Add separate jobs for formatting/static checks (if adopted), JVM tests, lint, and debug assembly.
- [ ] Add emulator instrumentation for migration/import tests, or a documented required scheduled pipeline if the CI platform cannot support acceleration.
- [ ] Upload test/lint reports as private artifacts with bounded retention; never upload personal backups or signing material.
- [ ] Add dependency/license review consistent with GPL distribution and the existing AboutLibraries setup (`app/build.gradle.kts:13-18,115-117`).
- [ ] After `.github/workflows/android-ci.yml` has been created and has completed successfully at least once, require its named CI status on protected branches.

**Commands:**

```bash
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest
```

**Acceptance gate:** A clean private-remote commit triggers `.github/workflows/android-ci.yml`; required non-device jobs pass; migration/import instrumentation runs in a required or scheduled named gate; only after that successful workflow run, its named status is enforced on the protected default branch; logs/artifacts contain no secrets or personal data.

**Commit:**

```bash
git add .github/workflows/android-ci.yml docs/development/ci.md
git commit -m "ci: gate Android build tests and migrations"
```

## Task 6: Spike a durable timer and workout state machine

**Files:**
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/workout/WorkoutState.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/workout/WorkoutEvent.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/workout/WorkoutReducer.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/workout/MonotonicClock.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/workout/WorkoutCheckpointStore.kt`
- Modify after spike proves the contract: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt`
- Keep as adapter until retired: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/SavedWorkoutState.kt`
- Create: `app/src/test/java/io/github/gonbei774/calisthenicsmemory/workout/WorkoutReducerTest.kt`
- Create: `app/src/test/java/io/github/gonbei774/calisthenicsmemory/workout/WorkoutTimerRecoveryTest.kt`

The current foreground service only keeps the process awake; timer logic remains in a screen (`WorkoutTimerService.kt:17-25`). Compose components decrement remembered counters after `delay(1000L)` (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/components/program/ProgramIntervalComponents.kt:49-64,205-226`). The spike must separate elapsed-time truth from rendering ticks.

- [ ] Write reducer tests first for start, pause, resume, tick/recompute, complete, skip, navigation, process recreation, wall-clock changes, and duplicate events.
- [ ] Model transitions as pure events/state; reject invalid transitions explicitly.
- [ ] Compute remaining time from a monotonic deadline while running, not by counting callbacks; persist enough epoch/checkpoint context to recover after process death.
- [ ] Keep foreground service/notification integration as an adapter; do not move domain decisions into the service.
- [ ] Persist checkpoints atomically and version their serialization.
- [ ] Run a manual API 26+ screen-off/background/process-recreation protocol and record observed drift and recovery.

**Commands:**

```bash
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:testDebugUnitTest --tests 'io.github.gonbei774.calisthenicsmemory.workout.*'
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=io.github.gonbei774.calisthenicsmemory.workout
```

**Acceptance gate:** Pure tests prove deterministic transitions; paused time does not elapse; delayed rendering does not add timer drift; versioned state restores to an explainable state after recreation; service teardown releases resources; the spike adds no networking or DI framework.

**Frequent commits:**

```bash
git commit -m "test: specify durable workout transitions"
git commit -m "feat: add pure workout state reducer"
git commit -m "feat: checkpoint timer state durably"
git commit -m "test: verify timer recovery semantics"
```

## Task 7: Add the v2 workout schema and conservative backfill

**Files:**
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/v2/WorkoutSessionEntity.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/v2/SessionExerciseEntity.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/v2/SetEntryEntity.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/v2/WorkoutEnums.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/v2/WorkoutTypeConverters.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/v2/WorkoutSessionDao.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/v2/V2Backfill.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/v2/V2MigrationReport.kt`
- Modify: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt`
- Modify: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/viewmodel/TrainingViewModel.kt`
- Create: `app/src/androidTest/java/io/github/gonbei774/calisthenicsmemory/V2MigrationTest.kt`
- Create: `app/src/androidTest/java/io/github/gonbei774/calisthenicsmemory/V2CompatibilityReadTest.kt`
- Create: `app/src/test/java/io/github/gonbei774/calisthenicsmemory/data/v2/V2BackfillTest.kt`
- Modify backup tests/fixtures under: `app/src/test/resources/backups/`

- [ ] Write schema and converter tests for stable enum codes, foreign keys, indexes, snapshots, typed metric columns, constraints, and epoch-millisecond fields.
- [ ] Add `WorkoutSession`, `SessionExercise`, and `SetEntry` tables in one additive migration; retain all legacy tables/columns/readers.
- [ ] Add current-template/source links plus name/configuration snapshots; use `groupId` for stable current linkage and retain group-name snapshots.
- [ ] Implement conservative legacy conversion with explicit timezone policy and provenance. Never invent seconds, durations, exact set times, or unsupported session grouping.
- [ ] Emit anomaly counts/reasons for malformed/ambiguous records and leave recoverable data on the legacy reader.
- [ ] Add dual-read comparison tooling for counts and representative values; avoid dual writes unless one short, transactional compatibility owner is documented.
- [ ] Before enabling any v2 write path, make the fallback/legacy history UI consume a compatibility read path that returns both retained legacy sessions and v2-created sessions; do not require dual writes.
- [ ] Add `V2CompatibilityReadTest` proving a workout written only to v2 remains visible after the unified-UX feature flag is disabled and the fallback/legacy UI is restored.
- [ ] Extend JSON export/import so v2 and retained legacy data are portable and versioned.

**Commands:**

```bash
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:testDebugUnitTest --tests '*V2BackfillTest'
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.gonbei774.calisthenicsmemory.V2MigrationTest
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.gonbei774.calisthenicsmemory.V2CompatibilityReadTest
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:testDebugUnitTest --tests '*Backup*'
```

**Acceptance gate:** Additive migration preserves every legacy row; valid new sessions round trip through Room and backup; snapshots survive source edits/deletion; malformed and ambiguous legacy records are reported rather than guessed; before v2 writes are enabled, fallback/legacy UI reads both legacy and v2-only sessions through the tested compatibility read path, including after the feature flag is disabled, without requiring dual writes.

**Frequent commits:**

```bash
git commit -m "test: specify v2 workout schema"
git commit -m "feat: add additive workout session tables"
git commit -m "feat: backfill legacy records conservatively"
git commit -m "feat: export and restore v2 workout data"
```

## Task 8: Deliver the unified workout UX behind a reversible flag

**Files:**
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/navigation/PrimaryDestination.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/today/TodayScreen.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/train/TrainScreen.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/progress/ProgressScreen.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/library/LibraryScreen.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/train/UnifiedWorkoutScreen.kt`
- Create: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/train/WorkoutViewModel.kt`
- Modify: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/MainActivity.kt`
- Adapt, do not immediately delete: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/WorkoutScreen.kt`
- Adapt, do not immediately delete: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/ProgramExecutionScreen.kt`
- Adapt, do not immediately delete: `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/IntervalExecutionScreen.kt`
- Create: `app/src/androidTest/java/io/github/gonbei774/calisthenicsmemory/UnifiedWorkoutFlowTest.kt`
- Create: `app/src/androidTest/java/io/github/gonbei774/calisthenicsmemory/PrimaryNavigationTest.kt`

The current activity owns a large sealed-screen switch (`MainActivity.kt:202-208,248-320,447+`). Introduce the four product destinations incrementally and retain a fallback path until parity is verified.

- [ ] Add Today, Train, Progress, and Library destinations with state restoration and accessible navigation semantics.
- [ ] Route ad-hoc, program, and interval starts into one `WorkoutViewModel`/state-machine contract while preserving mode-specific controls.
- [ ] Persist every material transition through the v2 repository/checkpoint boundary; UI recomposition must not be the owner of workout truth.
- [ ] Show source/template context from links and history from snapshots.
- [ ] Add Compose tests for start, record, pause, background, resume, skip, finish, abandon, back navigation, and process recreation across all three source types.
- [ ] Verify backup/export from the new UI and legacy-history visibility.
- [ ] Ship behind a local reversible feature flag until parity and migration gates pass; disabling it restores fallback/legacy UI whose compatibility reader still shows both legacy and v2-created sessions; collect no telemetry.
- [ ] Remove legacy screens/readers only in later commits after a released compatibility window and explicit data-parity review.

**Commands:**

```bash
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:testDebugUnitTest
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.gonbei774.calisthenicsmemory.UnifiedWorkoutFlowTest
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.gonbei774.calisthenicsmemory.PrimaryNavigationTest
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:lintDebug :app:assembleDebug
```

**Acceptance gate:** On API 26+ and a current API emulator/device, all three workout source types use one durable session lifecycle, survive recreation/backgrounding, write queryable v2 history, remain exportable/restorable offline, and can fall back without deleting or hiding v2-created data. The feature-flag-off test proves fallback/legacy UI compatibility reads both legacy and v2 sessions. Today/Train/Progress/Library satisfy the boundary in ADR 0001.

**Frequent commits:**

```bash
git commit -m "feat: add primary trainer destinations"
git commit -m "feat: unify ad-hoc workout execution"
git commit -m "feat: adapt program workouts to sessions"
git commit -m "feat: adapt interval workouts to sessions"
git commit -m "test: cover unified workout recovery"
```

## Final release gate

- [ ] Re-run clean assemble, JVM tests, lint, the complete migration matrix, import/restore tests, and unified Compose flows with the pinned JDK.
- [ ] Manually test airplane-mode operation, fresh install, v1.26.0 upgrade, pre-import backup, failed import recovery, timer background/recreation, export, and restore on named devices/API levels.
- [ ] Compare legacy/v2 row counts and representative history; review every anomaly category.
- [ ] Confirm GPL notices/source obligations, no unexpected network permission/dependency, no credentials/personal fixtures, and documented data format versions.
- [ ] Produce release notes that state migration behavior, backup recommendation, known anomalies, and rollback limits without claiming unexecuted tests passed.

```bash
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
JAVA_HOME="$HOME/.local/share/jdks/temurin-17" ./gradlew :app:connectedDebugAndroidTest
```

**Release acceptance:** Every required automated command has archived real output, every manual scenario has a named device/API result, blockers are explicit, backups restore successfully, and no supported migration path can silently destroy data.
