# ADR 0004: UI overhaul direction

- **Status:** Accepted
- **Date:** 2026-09-26

## Context

Task 8 of `docs/plans/2026-08-30-overhaul-bootstrap.md` delivers the visible overhaul: the four destinations from ADR 0001 and one workout flow. The plan assumed a local feature flag and a Material 3 bottom bar. Before any screen was built, the owner made the decisions below.

## Decisions

1. **No feature flag.** The app is not published until the overhaul is complete, so the new UI replaces the old one directly. The plan's flag and its "flag off" acceptance items, including the Task 7 item in `docs/development/v2-workout-history.md`, no longer apply. Legacy screens stay reachable until their replacements reach parity, and are removed only after an explicit parity review. Data safety rules are unchanged: no data is deleted or hidden, and every change stays exportable and restorable.

2. **Material 3 Expressive.** The owner chose it after comparison with apps that use it:
   - mpvEx: Compose, `material3:1.5.0-alpha15`, Expressive components.
   - ArrMatey: Compose Multiplatform, `material3:1.5.0-alpha14`, `MaterialExpressiveTheme`.
   - Obtainium: Flutter; it builds its own Expressive-style theme and is not directly comparable.

   Stable `material3:1.4.0` ships the Expressive APIs (`MaterialExpressiveTheme`, `ButtonGroup`, `LoadingIndicator` and others) as `internal`. They are public only in the 1.5 alphas. `1.5.0-alpha19` and later declare `minCompileSdk=37` and require Android Gradle Plugin 9.1, whereas this project uses compileSdk 35 and AGP 8.13. The app therefore pins **`material3:1.5.0-alpha18`**, the newest release that fits the current toolchain. It resolves the other Compose libraries to `1.11.0-beta02`.

3. **Navigation:** a bottom bar with the four destinations.

4. **Today** shows the resumable in-progress workout first, then today's to-dos and scheduled items, then a short summary of recent activity.

## Consequences

- Alpha and beta Compose artifacts carry more risk of bugs and API changes than stable ones. The version is pinned exactly, and upgrades are deliberate and verified: unit tests, lint, and instrumented tests on an emulator.
- Moving past alpha18, or to a stable release that exposes the Expressive APIs, requires the AGP 9 and compileSdk 37 upgrade as a separate step.
- `material3` alphas no longer bring in `material-icons-core`, so the app declares it directly (`1.7.8`, the final release).
- The Compose lint check `NonObservableLocale` is now active. Composables read the locale from `LocalConfiguration`, so text updates when the language changes.
