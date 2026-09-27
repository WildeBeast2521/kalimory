# UI design system: options and recommendation

- **Status:** Decided on 2026-09-27 (ADR 0004, decision 5). The owner chose Jetpack Compose, with the app's own calm, focused design language on Material 3 Expressive at the latest versions available. That is option A's component base, with option B's own design system on top.
- **Date:** 2026-09-27

## Goal

The owner wants the app to look premium and to perform as well as possible. An existing implementation is not a constraint. Options are judged on six criteria:

1. **Look:** a distinctive, calm, premium identity rather than a stock template.
2. **Performance:** smooth frames during a workout, fast cold start, a small APK.
3. **Accessibility:** WCAG AA contrast, 200% font scale, TalkBack, 48 dp touch targets, reduced motion.
4. **Maintenance:** stable dependencies, upgrades that one person can keep up with, and a toolchain that stays current.
5. **License and distribution:** GPL-3.0-compatible, F-Droid-friendly, and no proprietary or network dependencies (ADR 0001).
6. **Fit:** works offline, in 10 languages including Arabic (right-to-left), from Android 8 (API 26).

## Owner answers so far

- **Mood:** calm and focused.
- **Font:** bundle an open-license font.
- **Dynamic colour:** a toggle. On, the app follows the wallpaper colours and accent; off, it uses its own palette.
- **Technology:** the owner asked for a from-scratch evaluation of every realistic UI technology, as if the app had no UI yet, before choosing. That evaluation is the next section. The component options further down (A–E) assume Compose.

## UI technology from scratch

There are two separate choices:
- the **UI technology**: how screens are built and drawn;
- the **design language**: how the app looks.

Material, for example, is a design language, and it exists for most technologies. The owner's goals are a premium look, the best performance, accessibility, and one maintainer, under the constraints of offline use, no telemetry, GPL-3.0 and F-Droid.

One fact dominates the comparison. The parts that make this app trustworthy are native Kotlin, and all of them stay whatever draws the screens:
- the Room database with 16 versions of migration history and tests;
- the backup and restore code;
- the workout timer service with its wake lock and notifications;
- the checkpoints that survive process death;
- the v2 history.

A non-Kotlin UI must reach all of it through a bridge, or duplicate it in another language.

| Technology | How it draws | Look ceiling | Performance | Accessibility | Fit with the existing Kotlin core | F-Droid and license | Cost for one maintainer |
|:---|:---|:---|:---|:---|:---|:---|:---|
| **Jetpack Compose** (Kotlin) | Native Android UI toolkit | High: any design language, including fully custom | Native. Baseline Profiles give fast start and smooth frames | Android accessibility built in (TalkBack, font scale, right-to-left) | Direct: same language and process, no bridge | Apache-2.0; standard Gradle build | Lowest: the app already uses it |
| **Android Views + XML** (Kotlin) | Older native toolkit | High, with more effort for motion and custom drawing | Native | Built in | Direct | Apache-2.0 | High: a step backwards; Google's own new work targets Compose |
| **Flutter** (Dart) | Its own rendering engine (Impeller) paints every pixel | Very high: pixel-level control, strong motion | Very good once warm; adds the engine to the APK (several MB) and to cold start | Good, but its own semantics layer must be tested on Android | Poor: the core stays Kotlin behind platform channels, or is rewritten in Dart (database, migrations, service) | BSD license; F-Droid builds Flutter apps and has build templates | Very high: a full UI rewrite plus a bridge or a second data layer |
| **React Native** (JavaScript/TypeScript) | Native widgets driven from JavaScript (Hermes engine) | High, with libraries for animation | Good on the New Architecture; the JavaScript runtime adds size and start-up work | Uses native accessibility | Poor: native modules for every core feature | MIT. F-Droid needs Hermes built to match exactly; some apps manage (Joplin, Mattermost) | Very high, plus a large npm dependency tree to audit in a privacy app |
| **Compose Multiplatform** (Kotlin) | Compose, also targeting iOS and desktop | Same as Compose | Same on Android | Same on Android | Direct | Apache-2.0 | Only worth it if iOS or desktop is wanted, which ADR 0001 does not ask for |
| **Web in a WebView** (Capacitor, Ionic) | HTML and CSS inside a browser view | High for layout, weaker for native motion and gestures | Weakest: WebView start-up and memory, and scrolling that is not native | Web accessibility, mapped imperfectly to TalkBack | Poor: the same bridge problem as React Native | MIT. Workable offline, but the web toolchain and dependencies are heavy | Very high |

### Conclusion

**Jetpack Compose is the best technology for this app.** It matches the others' look ceiling, including a fully bespoke design language. It has the best performance and accessibility on Android, and it is the only option that keeps the tested Kotlin core without a bridge or a rewrite.

Flutter is the only credible alternative for pure visual ambition. But its visual advantage over Compose is small, and it would mean rewriting the UI, and either duplicating the data layer or bridging to it, in a project whose priority is data safety.

The real design decision is therefore the **design language and component base within Compose**, which options A–E below cover. The recommendation stays **B**: stable Material 3 components for behaviour and accessibility, with the app's own calm, focused design language on top. The owner's answers (calm mood, bundled font, dynamic-colour toggle) already fit B.

## Current state (measured)

- **Components:** Compose with Material 3. `material3` is pinned to `1.5.0-alpha18` for the Expressive APIs (ADR 0004). That pin pulls Compose core to `1.11.0-beta02`.
- **Library status:**
  - The stable Compose BOM `2026.09.00` ships Material 3 **1.4.0** and Compose **1.12.1**, so the app currently runs an older core than stable, and a beta one.
  - Material 3 1.5 has reached `1.5.0-alpha29`, with no beta yet. The Expressive components are public only there.
  - Alphas after 18 need Android Gradle Plugin 9.1+ and compileSdk 37. AGP 9.4.1 is available, and the project uses AGP 8.13 and compileSdk 35.
- **Styling:**
  - Styling is mostly ad hoc: 423 hard-coded colour references in UI code, and a parallel `AppColors` palette read in 37 files.
  - Each legacy screen has its own accent colour: orange for workouts, purple for history, amber for to-dos.
  - There are no bundled fonts; the system font is used.
- **Destinations:** the new destinations (Today, Train, Progress, Library) use Material components and theme roles. The legacy screens inside them do not.
- **Measurement:** there is no performance baseline yet. There are no Baseline Profiles and no Macrobenchmark module.

The main cost of any option is therefore the same: moving roughly 30 screens from ad-hoc colours to one set of semantic tokens, with shared components. The choice of component library changes how those tokens are drawn, not whether that migration happens.

## Evidence on the apps the owner named

The owner named three apps as references:

| App | UI technology | Material 3 Expressive? |
|:---|:---|:---|
| mpvEx | Compose, `material3:1.5.0-alpha15` | Expressive components, on an alpha |
| ArrMatey | Compose Multiplatform, `material3:1.5.0-alpha14` | `MaterialExpressiveTheme`, on an alpha |
| Obtainium | Flutter | Its own Expressive-style theme; not Compose |

The Compose apps that use Expressive all depend on a Material 3 alpha. None of them ships it on a stable release, because no stable release exists yet.

## Options

### A. Material 3 Expressive, as now

Keep `MaterialExpressiveTheme`, and move to newer alphas by upgrading to AGP 9 and compileSdk 37.

- **Look:** the recognisable 2025 Google style: bouncy shapes, springy motion, large type. It is distinctive now, but becomes the Android default once 1.5 is stable, so it will not read as this app's own identity.
- **Performance:** fine; this is the same engine.
- **Accessibility:** strong Material defaults.
- **Maintenance:** weak until a stable release.
  - The app runs alpha and beta Compose.
  - Each alpha can change APIs.
  - Staying current forces the AGP 9 migration now.
  - There is no date for 1.5 stable.
- **License:** Apache-2.0. Fine.

### B. Stable Material 3 plus the app's own design system (recommended)

Move back to the stable BOM, which gives Material 3 1.4 and Compose 1.12.1. Build a small, explicit design system on top:

- **Colour:** semantic roles defined once, for light and dark, replacing the per-screen accents and `AppColors`.
- **Type:** a type scale with a bundled open-license font, used for large workout numbers and headings.
- **Shape and space:** one shape and spacing scale.
- **Motion:** one motion specification, honouring Android's "remove animations" setting.
- **Signature components:** components that give the app its character, built from Foundation and Material parts. For example the workout timer and rep counter, set cards, the Today resume card, and progress charts.

Material 3 1.4 components (dialogs, text fields, navigation bar, sheets) supply behaviour and accessibility. The design system supplies the look.

- **Look:** distinctive, because the identity sits in the tokens, the typography, the signature components and the motion, not in a library preset. When Material 3 1.5 is stable, its Expressive shapes and motion can be adopted without redoing the tokens.
- **Performance:** stable Compose 1.12. Adds Baseline Profiles, plus a Macrobenchmark for startup and for frame timing during a workout.
- **Accessibility:** Material behaviour, plus checks written into the design system: token contrast, font-scale previews, and minimum touch targets.
- **Maintenance:** stable dependencies. The AGP 9 upgrade can wait for its own planned slice. The design system is plain Kotlin that the project owns.
- **License:** Apache-2.0 components. Fonts must be under the SIL Open Font License, which may be bundled with GPL software.

### C. Fully custom components on Compose Foundation

Drop Material. Build every component on Foundation, optionally using **Compose Unstyled** (MIT, maintained) for accessible headless behaviour: focus, semantics and state.

- **Look:** full control; the most bespoke result.
- **Performance:** slightly smaller, since there is no Material.
- **Accessibility:** every dialog, sheet, text field and menu must be rebuilt and re-verified. Compose Unstyled helps, but coverage has to be proven screen by screen.
- **Maintenance:** the heaviest. It is several times the work of B for the same screens, and every platform behaviour (IME, predictive back, right-to-left) becomes the project's own responsibility.
- **License:** MIT. Fine.

### D. Miuix, a HyperOS-style library

**Miuix** is Apache-2.0, maintained, and built for Compose Multiplatform.

- **Look:** polished, but it is the Xiaomi HyperOS visual language. The app would look like another company's system UI rather than having its own identity.
- **Maintenance:** it tracks Compose Multiplatform release candidates and adds a large dependency surface.
- **Verdict:** not recommended, because it borrows another brand's identity.

### E. Lumo UI, a component generator you own

**Lumo UI** is Apache-2.0. It generates component source code into the project, which the project then owns.

- **Look and maintenance:** a middle ground between B and C. Its last change was in May 2026, so it is less active.
- **Verdict:** B achieves the same ownership without a generator.

Kiwi's **Orbit** Compose library is archived, so it is excluded.

## Recommendation

**Option B.** It gives the most premium, distinctive result for the maintenance one person can carry, on stable software.

- It removes the alpha and beta dependencies. The app currently runs an older and less stable Compose core than the stable release.
- The premium feel comes from things B puts under the project's control: typography, colour, spacing, motion, haptics, and a few well-designed signature screens (in-workout, Today, Progress).
- It keeps a clean path to Material 3 Expressive once it is stable, without locking the app into it.

If the owner still prefers A, the price is the AGP 9 and compileSdk 37 upgrade now, plus following alpha API changes until 1.5 is stable.

## UX principles for the redesign (any option)

- **In-workout first:**
  - Numbers readable at arm's length (count, time, set).
  - The primary action within thumb reach at the bottom.
  - Haptic confirmation for a completed rep or set.
  - Screen kept on.
  - High contrast in both themes.
- **One visual language:** the per-screen accent colours are replaced by semantic roles. Colour means state (active, done, skipped, warning), not which screen you are on.
- **Today leads:** resume, then what is due, then recent activity (ADR 0004). Show the day at a glance, with no dashboard clutter.
- **Progress is honest:** charts show only recorded data. Legacy minute-precision times and skipped sets appear as what they are.
- **Dark theme is first-class.** Dynamic colour (Material You wallpaper colours) is an owner choice (see Decisions needed).
- **Accessibility is part of "done":**
  - AA contrast for text (4.5:1) and controls (3:1).
  - Layouts that work at 200% font scale.
  - TalkBack labels.
  - 48 dp targets.
  - Motion reduced when the system asks.

## Rollout (after the decision)

1. **Tokens and theme:** colour, type (with the font), shape, spacing and motion. Include previews at 100% and 200% font scale in both themes, and a contrast test for every token pair.
2. **Measurement first:**
   - Add a Macrobenchmark module (cold start, and frame timing on the workout screen) and Baseline Profiles.
   - Record the numbers before changing screens, so improvements are measured, not assumed.
3. **Prototype three screens:** Today, the in-workout screen and Progress. Share them with the owner as screenshots before rolling the style out.
4. **Migrate screen by screen:** replace hard-coded colours with tokens. Add screenshot tests for regressions, choosing a tool whose license and offline use suit the project.
5. **Signature components and motion polish:** last, once the structure is settled.

## Decisions needed from the owner

1. **UI technology:** Jetpack Compose (recommended) or another technology from the table above.
2. **Component base within Compose:** A (Expressive alpha), B (stable Material 3 plus own design system, recommended), or C (fully custom).

Mood, font and dynamic colour are answered (see "Owner answers so far").
