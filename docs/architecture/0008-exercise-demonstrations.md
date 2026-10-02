# ADR 0008: Exercise demonstrations

- **Status:** Accepted in part. Decision 1 (the muscle map) stands. Decisions 2 to 4 (the animated figure) were withdrawn by the owner on 2026-10-02.
- **Date:** 2026-10-01

## Context

The owner wants each exercise demonstrated in the standard fitness-app way: a human figure performing it, with the worked muscles highlighted in red (owner, 2026-10-01). The app is offline and GPL, and APK size matters (vector preferred to video, per the brief). Options researched:
- our own keyframe figure;
- Lottie drawn in Glaxnimate;
- 3D renders from a MakeHuman body in Blender;
- none at first.

## Decisions

1. **Muscle map for every exercise.**
   - A front and back body shows the primary muscles in red and the secondary muscles in a lighter red.
   - The body regions come from an MIT-licensed open body map, adapted into Compose vector paths and recorded in `docs/catalogue-sources.md`. MIT works with our GPL.
   - Regions use the muscle codes of ADR 0006.

2. **Animated figure for every exercise, on our own keyframe engine.**
   - The figure is built from tapered body shapes, a head, and hands and feet, drawn in Compose.
   - Each exercise is a few keyframes of joint angles, with hands or feet pinned to the floor or bar by a two-bone solver.
   - The working muscles are overlaid in red, deepening with the effort of the movement.
   - The figure uses the theme's colours and follows light, dark and wallpaper colour. With the system's "Remove animations" on, it shows a still pose.
   - A web page from the owner's review on 2026-10-01 prototyped it; the app version gets proper proportions.

3. **Format.**
   - Keyframes live in the Kotlin catalogue next to their step: joint angles, pinned points, timing and the muscles to highlight.
   - There are no binary assets, and no new library.
   - A small preview tool (a debug screen, or a web page generated from the same data) makes authoring about 150 exercises practical.

4. **3D renders are a later, optional upgrade.** A MakeHuman body (exported models are CC0) animated in Blender could render realistic clips. It is kept for later: it costs much more time per exercise and roughly 20 to 40 MB of app size.

## Withdrawal, 2026-10-02: no animated figure

An animated figure was built and then dropped.
- PRs #99 to #107: a procedural figure drawn in the app, with motions for all 53 steps. The owner judged it a mannequin.
- PRs #108 to #110: a pre-rendered 3D model, built from MakeHuman assets in Blender. Hand placement and similar details could not be made right reliably.

On 2026-10-02 the owner stopped the work: "Pull the plug on these rendering models. I don't want models. Forget that idea." All of that code was removed in PR #111, which also removed the debug preview and the build scripts.

What remains is decision 1. Each step's sheet shows the muscle map, with the worked muscles in red, and its text: the description, the cues and the muscles. Any future demonstration needs a new decision.

## Consequences

- The muscle map costs a few kilobytes and needs no dependency.
- The muscle data behind the red highlights is the same data the catalogue stores, so the map and the step text agree.
- Accessibility: every demonstration has a text alternative (the step's cues and the muscles worked), so it is never the only source of information.
