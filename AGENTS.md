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
  `RollingNumber`, `WorkoutPrimaryButton`, `FitText`) instead of a local variant. `ConsistencyGuardTest`
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
- **Lint.** There is no baseline: every lint warning is an error and fails the build. Fix the finding,
  or suppress it at the line with a comment saying why the rule does not apply there. Never add a
  baseline or disable a check to get a change through. The only checks turned off are the ones that
  report newer library, plugin or target API releases (Dependabot handles those).
- **Dependency checksums.** `gradle/verification-metadata.xml` holds the SHA-256 of everything
  Gradle downloads; a missing or different checksum fails the build. After any dependency change,
  run `scripts/update-dependency-verification.sh` (needs the emulator) and review the diff: every
  changed entry must belong to the update. Never trust a group or delete the file to get past a
  failure.
- **Screenshots.** `app/src/test/screenshots` holds golden images of the shared components and
  the four main screens in Light, Dark and AMOLED; `testDebugUnitTest` fails when one changes.
  Screens read "today" through `AppClock`, which the tests fix to a set date. After a deliberate visual
  change, run `./gradlew recordRoborazziDebug`, look at every changed image and commit them with
  the change. Never re-record to make an unexplained difference pass.

## Strings and translations

Ten languages ship: English plus `ar de es fr it ja ru uk zh-rCN`. Every user-visible string is a
resource, counts use `<plurals>`, and English uses sentence case. A new key goes into every
locale in the same change; mark machine-assisted translations for native review in the release
log. Arabic is right-to-left: check layouts in RTL.

Translation happens in Hosted Weblate, which commits to this repository through pull requests.
`glossary/<language>.tbx` is the project glossary (the Weblate component "Glossary", slug `terms`):
terms such as "set" and "rep" with the translation each language uses, and names like Kalimory
that stay untranslated. Edit it in Weblate, not by hand, so the two never disagree.

## Accessibility

Check every new or changed screen against this list; the tests named enforce it where they can.

- **Labels.** Everything that can be tapped says what it is: its text, or a `contentDescription` on an
  icon-only control. Decorative icons beside a text have `contentDescription = null`.
- **Touch targets.** At least 48 × 48 dp, counting Compose's widening and the space to neighbours.
- **Large text.** At 200 % font size on a 360 dp phone no text is cut off, shortened with "…" or split
  inside a word. One-line labels in a fixed width use `FitText`; a crowded row changes its layout
  when `isLargeFontScale()` instead of shrinking text out of reach.
- **Contrast.** Text at least 4.5:1 on what it sits on, borders at least 3:1, in Light, Dark and
  AMOLED; wallpaper colours go through `withReadableText`.
- **Right to left and TalkBack.** Check the screen in Arabic, and that TalkBack reads it in order.
- **Tests.** Add a new screen to `MainScreenAccessibilityTest`/`LargeTextTest` or
  `WorkoutScreensAccessibilityTest` (tap targets via `tapTargetProblems`, text via `textFitProblems`);
  `CalmPaletteContrastTest` covers the colours.

## Verification

```bash
# Every change (needs an API 29 emulator for the last task):
./gradlew testDebugUnitTest lintDebug assembleDebug :app:connectedDebugAndroidTest

# When CHANGELOG.md changes (CI runs it too):
python3 -I scripts/changelog-links.py --check

# When a Room schema or migration changes:
scripts/check-room-schemas.sh --stacktrace

# Release candidate (signed only when keystore.properties is present):
./gradlew assembleRelease
```

Expect all unit and instrumented tests to pass and lint to report nothing (any finding fails the
build). Cold start is measured on pull requests that change the app or the build: the
Startup workflow (`.github/workflows/startup.yml`) runs `StartupBenchmark` without and with the
Baseline Profile on the API 29 emulator and comments the table (`scripts/startup-report.py`). It
never fails on the numbers, which vary between emulator runs; look at the trend, and treat a clear
slowdown as a bug to explain before merging. Add or update a test with each fix: JVM tests for logic in `workout/`, `data/` and
pure helpers; instrumented tests for screens, database and file round trips. Tests are
deterministic and offline. A known flaky test is named in the release log, never disabled.

## Changes and cleanup

- Inspect before editing; keep each change small and coherent; use Conventional Commits.
- Remove code, resources or assets only after proving they are unused (references, lint, tests).
  Never delete migrations, schemas or recovery tooling because they look old.
- Keep README, CHANGELOG, wiki, Fastlane metadata and this guide true to the shipped app.
- Dependabot opens grouped update pull requests each month (`.github/dependabot.yml`). It cannot
  update the dependency checksums, so its Gradle pull requests fail until
  `scripts/update-dependency-verification.sh` runs on the branch and the result is pushed. Review the
  release notes, let CI pass, and merge one group at a time; a major update gets its own pull request.
- `ROADMAP.md` mirrors the GitHub milestones as a checklist: when an issue is added, moved or
  closed, update its line in the same pull request. A closed issue is ticked with the pull request
  that closed it, and the milestone's "done" count follows; an issue closed as not planned leaves
  the list. The README's roadmap section is the short overview: change it only when a milestone's
  theme or status changes (for example, when one ships and the next is in progress).

## Versions and releases

The first Kalimory release is 1.0.0 (versionCode 1). For a release:

1. Bump `versionCode` (always +1) and `versionName` in `app/build.gradle.kts`. For a minor release
   (x.y.0), first regenerate the Baseline Profile on the API 29 emulator with
   `./gradlew :app:generateReleaseBaselineProfile` and commit the changed
   `app/src/release/generated/baselineProfiles/` files, so the new screens start fast too.
2. Date the CHANGELOG entry (`## [x.y.z] - YYYY-MM-DD`), keep an empty `## [Unreleased]` above it,
   then run `python3 -I scripts/changelog-links.py`, which rewrites the link definitions at the
   bottom. Add
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (500 characters at most).
3. Merge to master, then run `scripts/release.sh --dry-run` and, when it passes,
   `scripts/release.sh`. It runs `scripts/release-checks.sh`, shows the release notes, and tags
   and pushes `v<versionName>`.
4. The tag starts `.github/workflows/release.yml`. It runs the whole CI (unit tests, lint, builds
   and the API 29 instrumented tests), then the release checks again, builds the APK signed with
   the key from the `release` environment's secrets, verifies it (certificate, package, version,
   no INTERNET) and drafts the GitHub release with the APK and its SHA-256.
5. Read the draft, then publish it. Download the run's `mapping-x.y.z` artifact (kept 90 days) and
   keep it to read stack traces.

The release build is minified with R8 (`proguard-rules.pro` keeps only line numbers; the app uses
no reflection).

After a crash, the app keeps a report on the phone and offers to share it on the next launch
(`data/CrashReports.kt`; never sent automatically, left out of Android backups). A shared report
names its version; save its trace to a file and turn it back into source names with
`$ANDROID_HOME/cmdline-tools/latest/bin/retrace mapping-x.y.z.txt trace.txt`.

Signing needs the release key: in CI, the `release` environment's secrets, which only `v*` tags can
use; locally, `keystore.properties`, which only the maintainer's machine has. Without either,
`assembleRelease` builds an unsigned APK (what F-Droid builds from source). The certificate's
SHA-256 is in `.github/release-certificate.sha256`; a release signed with any other key is refused.

## Changelog and release notes

One standard, for every release:

- `CHANGELOG.md` follows [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) and Semantic
  Versioning. The latest version comes first, under an `## [Unreleased]` section that is always
  there. A release is `## [x.y.z] - YYYY-MM-DD` with the sections that apply, in this order:
  `### Added`, `### Changed`, `### Deprecated`, `### Removed`, `### Fixed`, `### Security`.
- Every heading links from the bottom of the file, as Keep a Changelog does: `[Unreleased]` compares
  the latest tag with `HEAD`, each version compares with the one before it, and 1.0.0 links to its
  tag. The original app's releases, headed `Calisthenics Memory x.y.z`, link to their Codeberg tags.
  `scripts/changelog-links.py` writes these links; with `--check` (run by CI and the release checks)
  it also enforces the headings, dates, section types and order, and the pull request at the end of
  every entry from 1.0.1 on.
- Deprecations, removals and anything that breaks what users have (a backup an older version cannot
  read, a removed setting) are always listed, never left to the commit log.
- A release pulled for a serious bug or security problem keeps its section, marked
  `## [x.y.z] - YYYY-MM-DD [YANKED]`, and its GitHub release title gets ` [YANKED]`; the next
  release says why under `### Fixed` or `### Security`.
- Each bullet is one change a user can notice, written as a plain sentence in the present tense,
  ending with the pull request that made it: `([#143](https://github.com/WildeBeast2521/kalimory/pull/143))`.
  Tests, refactors, CI and documentation stay out unless they change what users get.
- Add bullets in the same pull request as the change, under `## [Unreleased]`; the release renames
  it to the version and date.
- The Fastlane changelog `changelogs/<versionCode>.txt` (500 characters at most) is a two- or
  three-sentence summary of the same section, for app stores.
- The GitHub release is titled `vx.y.z`. Its text is that version's CHANGELOG block exactly as the
  file has it (the `## [x.y.z] - YYYY-MM-DD` heading, its sections and its link definition, so the
  heading links to the comparison), followed by the footer in `.github/release-notes-template.md`
  (how to verify the APK). `scripts/release-notes.py` writes it, so CHANGELOG.md is the one source.
  Never edit a release page by hand: correct CHANGELOG.md instead, and the Release notes workflow
  (`.github/workflows/release-notes.yml`) rewrites every published release that no longer matches
  (`--check-published` reports drift, `--sync` fixes it by hand). No tool or AI attribution appears
  in the CHANGELOG, the Fastlane changelog or the release; the scripts refuse notes that contain it.
- Assets: `kalimory-x.y.z.apk` and `kalimory-x.y.z.apk.sha256`, in lower case.

## Wiki

`docs/wiki` is the source of the GitHub wiki. Edit pages there with the change they describe; after
the merge, `scripts/sync-wiki.sh` publishes them (links rewritten, sidebar and footer added).

