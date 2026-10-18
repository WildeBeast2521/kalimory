<!-- Keep each pull request to one change. For a schema change, a new permission, a new dependency or a large feature, open an issue first (see CONTRIBUTING.md). -->

## What changes

<!-- What a user or developer will notice, and why. Link the issue: "Closes #123". -->

## How it was checked

<!-- The commands you ran and their results, and the device or emulator you tried it on. -->

## Checklist

- [ ] The full check passes: `./gradlew testDebugUnitTest lintDebug assembleDebug :app:connectedDebugAndroidTest` (API 29 emulator).
- [ ] A test covers the change, or the description says why one isn't practical.
- [ ] New or changed strings are in all ten languages, counts use plurals, and machine-assisted translations are named here for native review.
- [ ] A change users can notice has a line in `CHANGELOG.md` under `## [Unreleased]`, ending with this pull request's link.
- [ ] The README, wiki (`docs/wiki`) and store text still describe the app correctly.
- [ ] After a deliberate visual change, the screenshot images are re-recorded and checked one by one.
- [ ] Nothing private is committed: no backups, exports, keystores, `local.properties` or tokens, and test data is made up.
- [ ] The app still has no internet permission, tracking or ads.
- [ ] If an AI coding assistant helped, it is named here.
