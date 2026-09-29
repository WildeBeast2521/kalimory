# Future plan brief: exercise progression system

- **Status:** Brief only. Not started. The owner asked for this to be planned in depth before any work begins.
- **Recorded:** 2026-09-27

## What the owner wants

The biggest upgrade the app will get: a built-in progression system that helps a person choose today's goal and move from one exercise to a harder or different one over time.

- **Catalogue.** A long, ideally complete, list of calisthenics exercises for every part of the body, stored in the app so it works offline.
- **Ranking.** Exercises ordered by difficulty, and grouped by movement pattern or variation, so a person can progress to the next exercise. The right model is still open: levels, progression chains, or something else.
- **Categories.** Body area, movement pattern, equipment, and other groupings the plan finds useful.
- **Custom exercises.** Keep letting users add their own, and let them place custom exercises in the categories and progressions.
- **Daily goals.** Use the catalogue and progressions to set the goal for a day.
- **Demonstrations.** Possibly an animated vector figure performing each exercise to show correct form.

## Items moved here from the redesign review (2026-09-29)

- Weekly goals. Today's "This week: 2 of 7" implies a seven-day goal.
- The Challenge tab: how a person sets a challenge, and how it relates to goals.
- Trends and personal bests in depth. The workout summary screen shows a simple personal best first.

## Constraints to respect when planning

- ADR 0001 is unchanged: offline, private, no accounts, no hosted AI, no telemetry. The catalogue and demonstrations ship inside the app.
- The license is GPL-3.0. Every catalogue entry, description and animation must be original work or carry a compatible license with its source recorded. Do not copy text, rankings or artwork from other apps or sites whose license does not allow it.
- Library edits must not rewrite history snapshots (ADR 0002). Built-in exercises need stable IDs, so updating the catalogue does not break existing records, goals or backups.
- Every schema change is additive, and backup and export must cover the new data.
- APK size: vector animations are preferred to video.

## Sequencing (recommended)

After the Task 8 overhaul: the four destinations, the unified workout flow on v2 sessions, and the premium visual design system. The progression system depends on:
- v2 sessions, to track progress per exercise;
- the Library destination, to browse the catalogue;
- Today, to show the daily goal;
- the design system, for its screens and animations.

Building it earlier would mean redoing it on the old screens and the old data path.

## Before building

Write a full plan with ADRs covering:
- the progression model;
- how the catalogue is sourced and licensed;
- the data model and stable IDs;
- how custom exercises join progressions;
- the animation format and pipeline;
- the goal and suggestion rules;
- accessibility, translations, and tests.
