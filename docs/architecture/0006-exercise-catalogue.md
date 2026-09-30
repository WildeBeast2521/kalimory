# ADR 0006: Exercise catalogue: content, licensing and format

- **Status:** Accepted
- **Date:** 2026-10-01

## Context

The progression system (ADR 0005) needs a built-in catalogue of exercises in chains, working offline. The app is GPL-3.0 (ADR 0001), so every entry must be our own work or carry a compatible licence with its source recorded.

The research (`docs/plans/2026-10-01-progression-system-research.md`) checked the candidate sources:
- **free-exercise-db** (Unlicense, 876 exercises). The owner likes it. Only about 188 of its exercises need no equipment, it lacks many calisthenics staples (pistol squat, levers, planche, L-sit, muscle-up, hollow body, Nordic curl), and its text and photos probably trace to a scraped commercial site.
- **wger:** per-exercise CC BY-SA, many entries under 3.0, which is not GPL-compatible.
- **Books:** all rights reserved.

## Decisions

1. **Facts from free-exercise-db; words are ours.** From free-exercise-db we take only facts:
   - exercise names;
   - primary and secondary muscles;
   - equipment;
   - force (push, pull, static);
   - mechanic (compound, isolation);
   - level.

   Descriptions, form cues and standards are written fresh, in our own words. Its photos are not used. Exercises it lacks are added by us. `docs/catalogue-sources.md` records where each kind of content comes from.

2. **The catalogue is compiled code plus string resources.**
   - The structure lives in a Kotlin catalogue file: chains, steps, difficulty, standards, muscles, equipment, kind, laterality and prerequisites.
   - Each step's name, description and cues are Android string resources in a separate `catalogue_strings.xml`, so they are translated like the rest of the app. Weblate can keep them as their own component.

   Compiled data needs no parser and fails the build on a missing string. Unit tests check the rules below.

3. **Stable ids.**
   - Every chain and step has a lowercase dotted id, for example the chain `push` and the step `push.incline`.
   - An id is never reused or renamed once released.
   - A retired step stays in the catalogue, marked retired, so links in history and backups keep resolving.

4. **One muscle vocabulary.** Muscles use a fixed set of stable codes, the one free-exercise-db uses: abdominals, abductors, adductors, biceps, calves, chest, forearms, glutes, hamstrings, lats, lower back, middle back, neck, quadriceps, shoulders, traps and triceps. The muscle map (ADR 0008) draws the same regions.

5. **Catalogue v1 scope.** About 150 steps in about 20 chains:
   - horizontal and vertical push;
   - vertical and horizontal pull;
   - dips;
   - squat;
   - hinge and posterior chain;
   - core: anti-extension, flexion and rotation;
   - handstand;
   - L-sit and compression;
   - bridge and mobility;
   - conditioning;
   - skills: muscle-up, front lever, back lever, planche and human flag.

   It is written in English first, then translated into the app's other nine languages.

6. **Tests guard the catalogue.**
   - Ids are unique and well formed.
   - Every step belongs to exactly one chain.
   - Difficulty never decreases along a chain.
   - Every working standard is below its move-on standard.
   - Prerequisites point at existing steps and form no cycle.
   - Every step has a name and cues.

## Consequences

- The catalogue grows through code review like any other change, with its sources recorded.
- Translators see catalogue text next to the rest of the app's strings.
- A catalogue update never rewrites history: sessions keep their snapshots (ADR 0002), and library exercises keep their own names unless the user changes them.
