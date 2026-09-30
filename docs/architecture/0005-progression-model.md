# ADR 0005: Progression model

- **Status:** Accepted
- **Date:** 2026-10-01
- **Context documents:** `docs/plans/future-progression-system-brief.md`, `docs/plans/2026-10-01-progression-system-research.md`

## Context

The owner asked for a built-in progression system: a catalogue of exercises ordered from easier to harder, that suggests what to train today and when to move on. Upstream already has a small version of this: groups of exercises with a level from 0 to 10 (`Exercise.group`, `Exercise.sortOrder`), a per-exercise target (`targetSets`, `targetValue`), and a Challenge tab that checks the target. The research compared books (Convict Conditioning, Overcoming Gravity), the r/bodyweightfitness Recommended Routine, and apps with skill trees. On 2026-10-01 the owner accepted the recommended options.

## Decisions

1. **The user stays free. This is a hard rule.** The progression system guides and never restricts.
   - Suggestions only suggest; nothing is started, changed or skipped automatically.
   - Skill links are hints, never locks. Every exercise stays available at all times.
   - Standards inform; they never block logging a set, a step, or an exercise.
   - Free logging, custom exercises, programs and intervals keep working exactly as they do.
   - A setting hides suggestions entirely.

   The owner confirmed this rule on 2026-10-01.

2. **Chains, plus a few skill links.** A *chain* is one movement's ordered list of *steps*, from easiest to hardest: for example, push from wall push-up to one-arm push-up.
   - Chains are the backbone.
   - A few skills name steps in other chains as *prerequisites* (for example, the muscle-up names a pull step and a dip step). These links are shown as guidance only (decision 1).
   - Every step carries a *difficulty* from 1 to 10 on one scale shared by all chains, so progress in different chains can be compared and weighted.

3. **Two standards per step.** A *working* standard is where the step starts, and a *move-on* standard is where it is mastered.
   - Defaults: dynamic steps 3×5 to 3×8; holds 3×10 s to 3×30 s. A step may set its own.
   - The user can change the move-on standard of any step in their library. It is the same field as today's exercise target, so existing targets keep their meaning.

4. **Double progression drives the next target.**
   - At a step, the next target is last session's sets and reps plus one rep (or a few seconds for holds), until the move-on standard is met.
   - When it is met in a session, the next step is suggested at its working standard.
   - When a step's working standard is missed in two sessions in a row, the easier step is offered as an option. It is never forced.

5. **The daily goal is a suggestion on Today.**
   - It covers the chains the user follows, least recently trained first.
   - It skips a chain trained in the last 48 hours: the same movement is rested for two days.
   - For each chain it names the step and the target from decision 4.
   - It is one tap to start and one tap to dismiss.

6. **The weekly goal is sessions per week.** The user picks 2 to 6, and Today's week strip reads "2 of 4" instead of "2 of 7". It is optional; without one, the strip shows days trained.

7. **Progressions replaces the Challenge tab.** Each followed chain shows its current step, the progress towards the move-on standard, and the next step. The standard does the job the challenge target does today, and challenge history stays readable.

8. **The user's own chains.**
   - Today's groups with levels are the user's own chains, unchanged.
   - A custom exercise can also be placed in a built-in chain, between two steps.
   - A built-in step added to the library links to the catalogue by a stable id (ADR 0007).

## Consequences

- The model builds on upstream's groups, levels and targets, so no existing data changes meaning.
- A single difficulty scale lets Progress weigh work across chains, as the Challenge tab's level weighting already does.
- The freedom rule rules out unlock gates, forced deloads or blocking prompts. Reviews check new screens against it.
- Suggestion rules are pure functions of the history and the catalogue, so they can be unit-tested without a device.
