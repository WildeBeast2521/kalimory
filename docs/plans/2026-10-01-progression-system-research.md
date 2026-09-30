# Progression system: research and options

- **Status:** Decided on 2026-10-01. The owner accepted the recommendations, with the refinements recorded in ADRs 0005 to 0008. The brief is `docs/plans/future-progression-system-brief.md`.
- **Date:** 2026-10-01
- **Owner-facing page:** https://claude.ai/artifact/PE43qfu7eVcwi5M8meKnWe (private), with the same content, a live demonstration prototype and the questions.

## 1. What the app has today

Upstream already carries a small progression model, and the plan should build on it rather than beside it:

- **Groups as chains.** An exercise can belong to a group (`Exercise.group`), with a level from 0 to 10 (`Exercise.sortOrder`, shown as "Lv"). A group with levelled exercises is a ladder, for example Push-up at level 1 and One-Arm Push-up at level 5 in `examples/sample_exercises.csv`.
- **Targets as standards.** `targetSets` and `targetValue` are the standard for an exercise. The Challenge tab (`calculateChallengeStatus`) checks whether the latest session met it, counts "clear days", and scores sessions with a weight of 1.3 per level.
- **History.** Every workout source writes v2 sessions with snapshots, so progress per exercise is reliable (ADR 0002).
- **What is missing:**
  - built-in exercises;
  - an explicit order of harder and easier variations beyond a free-form group;
  - a suggestion of what to do today;
  - weekly goals;
  - demonstrations.

## 2. How others model progression

| Source | Model | Move-on rule | Notes |
|:---|:---|:---|:---|
| Convict Conditioning (book) | 6 movements, 10 steps each | Three standards per step: beginner, intermediate, progression (wall push-ups: 1×10, 2×25, 3×50) | Copyrighted book. The idea of steps with standards is common practice; its text and exact numbers are not ours to copy. |
| r/bodyweightfitness Recommended Routine | Ordered progressions per movement | Work at 3×5–8. Move on at 3×8; holds move on at 3×30 s. | The most widely used free routine. The wiki text carries no explicit licence, so it is used as a reference only. |
| Overcoming Gravity (book) | Charts of 16 levels across skills, with standards | Level-based | Copyrighted charts. The idea of a numeric level that is comparable across chains is useful. |
| Caliverse, Calisteniapp (commercial apps) | Skill trees and skill paths (pull-up, muscle-up, handstand, lever, planche) | Unlock after clean sets at the step before | Paywalled; a "skill tree" is the norm in this category. |
| Ironvellum (open source, GPLv3+, Kotlin and Compose) | 14 paths, 106 techniques, each gated on the one before and carrying a written standard | Standard per technique | Licence compatible with ours. The source of its data is not documented. |
| SkillForge (open source) | RPG-style tree with XP and unlocks | XP and prerequisites | No licence found, so not usable. |

Evidence worth building in:
- **Double progression.** Add reps within a range, then move to a harder variation. It fits bodyweight training, where load changes by changing the exercise.
- **Rep ranges.** Growth is similar across roughly 5 to 30 reps when sets end near failure (Schoenfeld and colleagues). Pure strength favours low reps, which in bodyweight means harder variations sooner.
- **Weekly volume.** Hypertrophy rises with weekly sets per muscle. The 2017 meta-analysis found 10 or more sets per muscle per week clearly ahead of fewer. About 5 to 9 sets gave roughly 80% of the gain, with diminishing returns towards 20.

## 3. Catalogue sourcing and licences

| Source | Licence | Usable? |
|:---|:---|:---|
| free-exercise-db (yuhonas) | Unlicense (public domain) | Risky. It says it came from `wrkout/exercises.json`, which states no original source. It is mostly gym exercises and includes photos. |
| wger exercise data | Per exercise: mostly CC BY-SA, older entries 3.0 | Only CC BY-SA 4.0 is one-way compatible with GPLv3 (Creative Commons and FSF, 2015). Entries under 3.0 are not. The data is mixed and needs per-entry checks and attribution. |
| Ironvellum | GPLv3+ | Compatible, but the provenance of its techniques is undocumented. |
| Books (Convict Conditioning, Overcoming Gravity) | All rights reserved | No text or tables. |

**Recommendation:** write our own catalogue. Exercise names and the fact that one variation is harder than another are not copyrightable. Descriptions, cues and standards are written fresh, in our own words, and every entry records its authorship. Open datasets are used only as a checklist to spot missing exercises, never as copied text.

## 4. Options

### Progression model
- **A. Chains.** Each movement is an ordered list of steps with standards, as in the books and the Recommended Routine. Simple, and it maps directly onto today's groups and levels.
- **B. Skill tree.** Prerequisites across chains, as in the apps. Expressive, but heavier to build, explain and test.
- **C. Chains, plus a few cross-links for skills (recommended).** Chains are the backbone. A handful of skills (muscle-up, front lever, planche, handstand push-up) name prerequisites from other chains. Every step also carries a numeric difficulty (1 to 10), so different chains can be compared and weighted, the way upstream already weights levels.

### Standards
- **Two per step (recommended):** a "working" standard to start the step and a "move on" standard to finish it. Defaults: dynamic 3×5 then 3×8, holds 3×10 s then 3×30 s, adjustable per step and by the user.
- Or three per step, Convict Conditioning style. That is more to show and explain, for little gain.

### Where the catalogue lives
- **Recommended:** a versioned asset in the app (JSON), with stable string ids such as `push.incline`. A user's library exercise links to a catalogue entry through a new nullable `catalogId`. History keeps its snapshots, so catalogue updates never rewrite the past.
- Or seed catalogue rows into the database: heavier migrations, and every catalogue update becomes a data change.

### Custom exercises
- Today's groups and levels become the user's own chains. A custom exercise can also be placed into a built-in chain, "between these two steps".

### Daily goal
- **Recommended:** a suggestion on Today, never automatic. It covers today's movements (from the user's routine and recovery: about 48 hours before the same movement again), the step for each, and a target from double progression, for example "Incline push-ups, 3×7 (last time 3×6)". It is one tap to start, and easy to change or skip.

### Weekly goal
- **Recommended:** the user picks sessions per week (2 to 6). Today's "This week: 2 of 7" becomes "2 of 4". Later, sets per movement pattern per week could be shown against the evidence-based 10 or more.

### The Challenge tab
- **Recommended:** it becomes "Progressions". Each chain shows its current step, how close it is to the move-on standard, and what comes next. The step standard does the job today's challenge target does, so nothing is lost.

### Personal bests and trends
- Best set per exercise (already on the summary), best step per chain, and a chain-level trend over time, using the step difficulty.

### Demonstrations
- **A. Own skeleton format (recommended).**
  - A simple figure drawn in Compose from a few keyframes of joint angles, a few kilobytes per exercise.
  - It follows light, dark and wallpaper colours, and "Remove animations" (showing a still pose).
  - No new library. One figure makes every demonstration consistent.
  - Authoring is by keyframes, helped by a small preview tool.
- **B. Lottie.** Hand-drawn in Glaxnimate (GPLv3, open source) and played with lottie-compose (Apache-2.0, about 130 KB). Richer drawings, but each of about 150 exercises must be animated by hand, and matching the theme needs extra work.
- **C. None at first.** Still pose pictures, animation later.

### Scope of catalogue v1 (proposal)
About 150 steps in about 20 chains:
- horizontal and vertical push;
- vertical and horizontal pull;
- dips;
- squat;
- hinge and posterior chain;
- core (anti-extension, flexion, rotation);
- handstand;
- L-sit and compression;
- bridge and mobility;
- conditioning;
- skills: muscle-up, front lever, back lever, planche, human flag.

## 5. Proposed phases

1. **ADRs and data model.** The progression model, catalogue format and licence, stable ids, the `catalogId` link, user chains, and the backup format change. All migrations are additive.
2. **Catalogue v1.** Write the core chains (push, pull, legs, core) with our own descriptions, cues and standards, in English first and then the other locales.
3. **Progressions screen.** The Challenge tab replacement, plus "add from catalogue" in Library.
4. **Suggestions.** Today's suggestion, and the weekly goal on the week strip.
5. **Demonstrations.** The skeleton format, the renderer, and a preview and authoring tool; then the poses.
6. **Expansion.** Skills with cross-links, the remaining chains, and trends.

Each phase is its own set of verified slices, as in Task 8.

## 6. Open questions for the owner

- **P1:** Progression model: A (chains), B (tree) or C (chains plus skill cross-links)?
- **P2:** Two standards per step, or three?
- **P3:** Write our own catalogue, using open datasets only as a checklist?
- **P4:** Catalogue as an asset with a `catalogId` link?
- **P5:** Groups and levels become the user's own chains, with custom exercises placeable into built-in chains?
- **P6:** Daily goal as a suggestion on Today, never automatic?
- **P7:** Weekly goal as sessions per week?
- **P8:** Replace the Challenge tab with Progressions?
- **P9:** Demonstrations: own skeleton format, Lottie, or none at first?
- **P10:** The v1 scope and the phase order above?

## Sources

- free-exercise-db: https://github.com/yuhonas/free-exercise-db
- exercises.json: https://github.com/wrkout/exercises.json
- wger: https://github.com/wger-project/wger
- CC BY-SA 4.0 one-way compatible with GPLv3: https://creativecommons.org/2015/10/08/cc-by-sa-4-0-now-one-way-compatible-with-gplv3/
- Recommended Routine: https://redditbwf.github.io/wiki/recommended_routine.html
- Convict Conditioning notes: https://gist.github.com/avar/1575165
- Overcoming Gravity charts: https://www.calisthenics-101.co.uk/wp-content/uploads/2020/05/Overcoming-Gravity-2nd-Edition-Exercise-Charts.pdf
- Ironvellum: https://github.com/AlexMollard/Ironvellum
- SkillForge: https://github.com/Herofresh/SkillForge
- Caliverse review: https://calisthenicsworldwide.com/apps/caliverse-review/
- Calisthenics apps compared: https://fitloop.app/blog/best-calisthenics-apps
- Double progression: https://legionathletics.com/double-progression/
- Rep ranges: https://www.scienceforsport.com/hypertrophy-the-optimum-rep-range/
- Weekly volume: https://www.strongerbyscience.com/research-spotlight-volume-returns/
- lottie-compose: https://central.sonatype.com/artifact/com.airbnb.android/lottie-compose
- Rive Android runtime: https://rive.app/docs/runtimes/android/android
- Glaxnimate: https://apps.kde.org/glaxnimate/
