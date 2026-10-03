# Working on Kalimory

Kalimory is an offline Android app for bodyweight training (Kotlin, Jetpack Compose, Room). It is a
fork of Calisthenics Memory and installs beside it under `io.github.wildebeast2521.kalimory`. This
guide is for anyone changing the code, human or agent. `CONTRIBUTING.md` has the same ground rules
in short form; keep the two consistent.

## Invariants

These hold for every change. A change that breaks one is wrong, however good it looks.

- **Offline and private.** No `INTERNET` permission, no network library, telemetry, analytics, ads,
  Firebase, Crashlytics, Google Play Services, accounts or remote configuration. Files move only
  through the Storage Access Framework and share sheets the user starts.
- **User data is kept.** Room migrations are additive and keep every row. Never invent
  timestamps, workout groupings or values that old data does not contain. Backups, CSV, sharing
  and restore must round-trip what they claim to.
- **Nothing is locked.** Progressions and suggestions guide; they never stop anyone training any
  exercise. Do not add UI copy explaining that nothing is locked.
- **One behaviour everywhere.** Use the shared components (`AppFab`, `StartButton`,
  `NumberStepButton`/`NumberStepper`, `SwipeToDeleteBackground`, `CalmTopBar`, `TimerDial`,
  `RollingNumber`, `WorkoutPrimaryButton`) instead of a local variant. `ConsistencyGuardTest`
  fails when a screen brings its own.
- **No private material in git.** Never commit keystores, `keystore.properties`,
  `local.properties`, tokens, databases, real backups or exports. Fixtures use made-up data.

## Map

```
app/src/main/java/io/github/gonbei774/calisthenicsmemory/
  data/        Room entities, DAOs, migrations; v2 workout model (data/v2); backups; catalogue
  viewmodel/   TrainingViewModel and the backup service
  ui/          Compose screens (ui/screens), shared components (ui/components), theme
  service/     Foreground timer service (specialUse)
  workout/     Workout state machine, plans, checkpoints; pure and unit-tested
app/schemas/   Exported Room schemas: migration-test inputs, never edited by hand
app/src/test/          JVM unit tests
app/src/androidTest/   Instrumented tests (API 29 emulator)
baselineprofile/       Baseline Profile generator
scripts/       Catalogue generator, schema check, icon, sound and README tools
docs/wiki/     User guide (the only public documentation folder)
fastlane/      Store metadata
```

The Kotlin package stays `io.github.gonbei774.calisthenicsmemory` (upstream history); only the
application ID changed.

## Generated content

- **Catalogue.** Edit `scripts/catalogue/catalogue_spec.py`, then run
  `python3 scripts/catalogue/generate_catalogue.py .`. It writes `Catalogue.kt` and English
  `catalogue_strings.xml`. Add each new key to every `values-*/catalogue_strings.xml` by hand.
  Never edit generated output directly.
- **Room schemas.** Bump the database version, add a migration and let the build export the new
  schema. Committed schema files are immutable; `scripts/check-room-schemas.sh` enforces it.
- **Lint baseline.** Never regenerate `app/lint-baseline.xml` to hide new problems. Fix them.

## Strings and translations

Ten languages ship: English plus `ar de es fr it ja ru uk zh-rCN`. Every user-visible string is a
resource, counts use `<plurals>`, and English uses sentence case. A new key goes into every
locale in the same change; mark machine-assisted translations for native review in the release
log. Arabic is right-to-left: check layouts in RTL.

## Verification

```bash
# Every change (needs an API 29 emulator for the last task):
./gradlew testDebugUnitTest lintDebug assembleDebug :app:connectedDebugAndroidTest

# When a Room schema or migration changes:
scripts/check-room-schemas.sh --stacktrace

# Release candidate (signed only when keystore.properties is present):
./gradlew assembleRelease
```

Expect all unit and instrumented tests to pass and lint to report only the baseline's dependency
warnings. Add or update a test with each fix: JVM tests for logic in `workout/`, `data/` and
pure helpers; instrumented tests for screens, database and file round trips. Tests are
deterministic and offline. A known flaky test is named in the release log, never disabled.

## Changes and cleanup

- Inspect before editing; keep each change small and coherent; use Conventional Commits.
- Remove code, resources or assets only after proving they are unused (references, lint, tests).
  Never delete migrations, schemas or recovery tooling because they look old.
- Keep README, CHANGELOG, wiki, Fastlane metadata and this guide true to the shipped app.

## Versions and releases

The first Kalimory release is 1.0.0 (versionCode 1). Bump `versionCode` and `versionName` in
`app/build.gradle.kts` only when releasing, together with the CHANGELOG entry and a Fastlane
changelog file. The release runbook lives with the maintainer's release notes; publishing,
tagging and signing are done by the maintainer only.
