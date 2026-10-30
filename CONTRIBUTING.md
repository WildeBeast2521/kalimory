# Contributing

Bug reports, catalogue corrections and ideas are welcome. Please use the issue templates on this repository.

Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md). For a security problem, don't open an issue: follow [SECURITY.md](SECURITY.md) to report it privately.

## Ground rules

These keep the app what it is. Changes that break them cannot be accepted.

- **Offline.** The app has no `INTERNET` permission and never will. No feature may need the network. CI fails when the built APK asks for any permission the app's manifest does not declare.
- **No tracking.** No analytics, telemetry, ads, Firebase, Crashlytics or Google Play Services.
- **Your data is safe.** Database migrations are additive only. Never invent timestamps or workout groupings that old data does not contain.
- **Nothing is locked.** Progressions guide; they never stop anyone training what they want.
- **Private data stays out of the repository.** Fixtures and examples use made-up data only. Never commit backups, exports, keystores, `local.properties` or tokens.

## Before a pull request

Open an issue first for:

- database schema changes;
- new permissions;
- new dependencies;
- large features, such as a new screen or a changed workflow.

Keep each pull request to one change, and use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:` and so on). If you used an AI coding assistant, say so in the description.

## Building and the local check

Requires JDK 17 or later and the Android SDK. Instrumented tests run on an emulator; the project uses an API 29 image.

```bash
./gradlew assembleDebug

# The full check that every pull request must pass:
./gradlew testDebugUnitTest lintDebug assembleDebug :app:connectedDebugAndroidTest
```

`testDebugUnitTest` also compares the shared components with the golden images in `app/src/test/screenshots`. After a deliberate visual change, run `./gradlew recordRoborazziDebug` and commit the updated images with the change. The store and README images come from `scripts/screenshots/update-screenshots.sh`.

Run `scripts/check-room-schemas.sh` when a Room schema changes, and `scripts/update-dependency-verification.sh` when a dependency changes (Gradle checks the SHA-256 of everything it downloads against `gradle/verification-metadata.xml`). Lint has no baseline and fails on any warning: fix the finding, or suppress it at the line with a comment saying why. Do not add a baseline, weaken validation or skip tests to make the check pass.

## Repository guide

[AGENTS.md](AGENTS.md) explains the invariants, generated content, translations, verification and release rules in more detail, for people and coding agents alike.

## Where things are

```
app/src/main/java/.../calisthenicsmemory/
  data/          Room database, v2 workout model, backups, catalogue, progression logic
  viewmodel/     TrainingViewModel
  ui/            Compose screens, components and theme
  service/       Foreground timer service
  workout/       Workout sessions and checkpoints
app/src/test/          Unit tests
app/src/androidTest/   Instrumented tests
app/schemas/           Exported Room schemas
baselineprofile/       Baseline Profile generator
docs/wiki/             User guide
scripts/               Catalogue, muscle map, sound, icon, README image and schema tools
fastlane/              Store metadata
```

## The exercise catalogue

The catalogue is generated. Edit `scripts/catalogue/catalogue_spec.py`, then run:

```bash
python3 scripts/catalogue/generate_catalogue.py .
```

This writes `Catalogue.kt` and the English `catalogue_strings.xml`. Never edit those two by hand: CI fails when they differ from what the spec generates. Add the same keys to every `values-*/catalogue_strings.xml`; `CatalogueTranslationTest` and lint check that none are missing. Catalogue text must be written for this project. See `docs/wiki/Credits.md`.

## Translations

The app ships in ten languages. English strings live in `app/src/main/res/values/`, and each language has its own `values-*` folder.

Translations are made on [Hosted Weblate](https://hosted.weblate.org/engage/kalimory/): the app strings, the exercise catalogue and the store listing. Corrections and reviews from native speakers are especially welcome there; Weblate sends them to this repository as pull requests. Please change existing translations in Weblate rather than by hand, so the two never disagree. A new English string still goes into every `values-*` folder in the same pull request (see [AGENTS.md](AGENTS.md)).

The glossary in `glossary/` lists the words each language uses for terms such as "set" and "rep"; it is edited in Weblate too.

## License

By contributing, you agree that your contributions are licensed under the [GNU General Public License v3.0](LICENSE).
