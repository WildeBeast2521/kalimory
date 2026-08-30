# ADR 0002: Additive v2 workout domain model

- **Status:** Accepted
- **Date:** 2026-08-30

## Context

The current Room database is a single application database at version 21 with ten entities and schema export disabled (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt:11-16`). A `TrainingRecord` represents an individual set and stores date and minute-resolution time as strings; it has no durable workout/session identity (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/TrainingRecord.kt:20-32`). Exercises store domain categories such as `type` and `laterality` as free-form strings, and group membership as a group name string (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/Exercise.kt:11-28`).

Program execution already has an in-memory session and a sealed execution step hierarchy (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/ProgramExecutionModels.kt:34-53`), but completed program sets are flattened back into legacy records with one generated date/time for the save operation (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/util/ProgramExecutionUtils.kt:18-24`). `SavedWorkoutState` is a `SharedPreferences` checkpoint containing JSON-encoded sets, the program ID, current index, comment, and saved timestamp (`app/src/main/java/io/github/gonbei774/calisthenicsmemory/data/SavedWorkoutState.kt:8-37`). These structures are useful evidence, but they do not provide a durable, unified history for ad-hoc, program, and interval training.

## Decision

Introduce a v2 model **additively** in the existing Room database. The initial model has three first-class persisted entities, plus explicit linkage to existing library/template rows:

### `WorkoutSession`

A workout instance and lifecycle boundary.

- stable `id`;
- `status: WorkoutSessionStatus` with stable persisted codes such as `PLANNED`, `ACTIVE`, `PAUSED`, `COMPLETED`, and `ABANDONED`;
- `sourceType: WorkoutSourceType` such as `AD_HOC`, `PROGRAM_TEMPLATE`, `INTERVAL_TEMPLATE`, or `LEGACY_IMPORT`;
- nullable `sourceTemplateId` pointing to the originating program/interval/template when one exists; deletion of a source must not delete history;
- `sourceNameSnapshot` and any session-level configuration snapshot needed to explain what was run;
- `startedAtEpochMillis`, nullable `endedAtEpochMillis`, and `updatedAtEpochMillis` as epoch timestamps;
- optional note/comment and explicit migration/provenance flags where needed.

### `SessionExercise`

An ordered exercise occurrence inside one workout. Repeating the same library exercise creates distinct occurrences when order or configuration differs.

- stable `id` and `workoutSessionId` foreign key;
- nullable `exerciseId` linkage to the current library item, using history-preserving delete behavior;
- nullable template item/source linkage (for example `sourceProgramExerciseId`) where it is stable and meaningful;
- `orderIndex`, nullable `groupId`, and round/block linkage required by the executed structure;
- snapshots of user-visible and interpretation-critical fields, including exercise name, exercise type, laterality, target/default configuration, and group name where shown historically.

`groupId` is the stable relationship for current organization. A group-name snapshot preserves historical display even if a group is renamed or removed. Migration must not pretend that the existing `Exercise.group` string is already a valid foreign key.

### `SetEntry`

A durable set attempt/result owned by one `SessionExercise`.

- stable `id`, `sessionExerciseId`, `orderIndex`, `setNumber`, and optional `roundNumber`;
- `status: SetEntryStatus` such as `PENDING`, `COMPLETED`, or `SKIPPED`;
- `side: BodySide` such as `BILATERAL`, `RIGHT`, or `LEFT`;
- typed nullable metric columns: repetitions/count, duration milliseconds, distance in a documented integer base unit, added weight grams, and assistance grams;
- target snapshots in corresponding typed columns where the target must be recoverable;
- nullable `startedAtEpochMillis` and `completedAtEpochMillis`, note/comment, and explicit provenance for migrated entries.

A skipped set is represented by status, not by a magic metric value. Database constraints and repository validation reject impossible negative values and invalid lifecycle transitions.

### Type and time policy

- Kotlin enums/value types define exercise type, laterality/body side, source type, session status, set status, and metric semantics. Room converters persist stable, documented codes; enum ordinal values are never persisted.
- Epoch timestamps are integer milliseconds. Presentation converts them to the user's timezone. A later timezone field can be added only if a concrete historical/reporting requirement justifies it.
- Unit names appear in property/column names or typed wrappers. Conversion happens at input/output boundaries.
- Existing IDs remain `Long`; new keys and foreign keys use the same representation unless a measured portability requirement proves otherwise.

### Scope restraint

- Do **not** put extensible workout metrics into a generic JSON blob, entity-attribute-value table, or untyped key/value map. New supported metrics require an additive typed column/table migration and explicit import/export handling. JSON remains appropriate for versioned portable files and transient serialization, not the authoritative query model.
- Do **not** split the app into multiple Gradle modules during this foundation. The repository currently contains only `:app` (`settings.gradle.kts:22-23`); package boundaries and small interfaces are sufficient until measured build or ownership pressure exists.
- Do **not** introduce Hilt, Koin, or another dependency-injection framework as a prerequisite. Keep construction explicit and extract repositories/use cases only where the migration and unified workout flow need seams.

## Consequences

- Ad-hoc, program, and interval workouts can share lifecycle, persistence, history, and resume behavior.
- Source links support navigation and reuse; snapshots keep completed history truthful after library/template edits.
- Typed columns make progress queries, validation, export, and migrations more verbose but auditable and indexable.
- The schema will temporarily contain legacy and v2 representations, requiring explicit write/read ownership and reconciliation tests.
- A single app module and explicit construction reduce overhaul risk, though future modularization or DI remains possible through a separate evidence-based ADR.
- Epoch time improves ordering and duration precision for newly captured data, but legacy minute strings cannot be upgraded to precision they never contained.
