# v2 workout history (Task 7)

Status: storage layer complete (Task 7). History screens read legacy and v2 together, and v2 history can be edited. Single-exercise workouts write v2 (Task 8).

## What exists

| Piece | Where | Evidence |
|:---|:---|:---|
| Tables `workout_sessions`, `session_exercises`, `set_entries` (database 22) | `data/v2/*Entity.kt`, `MIGRATION_21_22` | `V2SchemaTest`, migration matrix 9→23 |
| Per-side legacy link (database 23) | `MIGRATION_22_23` | `V2SchemaTest`, 22→23 edge |
| Stable enum codes | `data/v2/WorkoutEnums.kt` | `WorkoutEnumsTest` |
| Conservative legacy conversion and report | `data/v2/V2Backfill.kt`, `V2MigrationReport.kt` | `V2BackfillTest`, `V2BackfillRunTest` |
| Compatibility history (legacy plus v2-only, each set once) | `data/v2/CompatibilityHistory.kt` | `CompatibilityHistoryTest`, `V2CompatibilityReadTest` |
| JSON backup format 9 | `viewmodel/BackupV2.kt`, `BackupService`, `BackupDao` | `BackupValidationTest`, `BackupRoundTripTest` |

## Task 7 acceptance gate

- The additive migration preserves every legacy row. **Met:** every source version 9–22 reaches 23 with an identical legacy snapshot.
- Valid new sessions round trip through Room and backup. **Met.**
- Snapshots survive source edits and deletion. **Met:** exercise and group links become null, and name and group snapshots remain.
- Malformed and ambiguous legacy records are reported, not guessed. **Met.**
- Before v2 writes are enabled, the legacy UI reads legacy and v2-only sessions through the tested compatibility path, including with the feature flag off. **Partly met.** The reader and its test exist. There is no feature flag or v2 write yet, so the "flag off" half is verified in Task 8, when the flag is introduced. *Superseded by ADR 0004: there is no feature flag. The compatibility reader remains the tested path for reading legacy and v2 history together.*

## When the backfill runs (decision)

`V2Backfill` does **not** run yet, neither at start-up nor anywhere else. The reasons:

- Every screen still writes `training_records`.
- A v2 copy of a legacy record is not updated when that record is later edited. When the record is deleted, the copy becomes a hidden ghost: the compatibility reader hides copies, and backup validation reports it as `V2_LEGACY_RECORD_MISSING`.
- No screen reads v2 yet, so copies made now would have no consumer and could only drift.

The backfill therefore runs at the Task 8 cutover, when v2 becomes the write path for new workouts. It runs once, in the same step as a dual-read comparison, and its report is kept and shown to the user. It is idempotent, so an interrupted cutover can be retried.

## Write path: single-exercise workouts (Task 8)

Finished single-exercise workouts (Train, then Workout, then Single) are saved as one v2 session through `SingleWorkoutWriter`. They are not written to `training_records`. Manual recording and program and interval workouts still write legacy records until their own slices.

How a workout maps to rows, without inventing anything:
- **Start:** stamped when the first set or its countdown begins, and kept in the checkpoint. If it was not observed (a checkpoint written before this change), the save time stands in for it and the session is marked MINUTE precision.
- **End:** the save time.
- **Set completion time:** stamped when a set is completed, or when a set with a value is interrupted by an abort. A value typed on the confirmation screen has no time.
- **Values:** a set with a value is COMPLETED. A set without one is SKIPPED and stored; the legacy save dropped it.
- **Units:** isometric values are whole seconds, as entered. The exercise's group, name, kind and laterality are kept as snapshots.
- **To-dos:** a to-do the workout came from is completed after the session is written, so the group check sees the new sets. The two used to run concurrently.

History shows a v2 set at its session's start time. One saved workout therefore appears as one row group, as a legacy save does.

Known gaps:
- The confirmation screen still says zero-value sets "will not be saved". They are stored as skipped sets and are not shown in history. Reword this with the new UI.
- A unilateral set with only a left value shows the left value in the right-side column of the legacy-shaped screens. The legacy save dropped such a set.

## Backfill timing (revised)

The compatibility reader already shows legacy and v2 history together, and edits reach each row in its own store. Moving single workouts to v2 therefore needs no backfill. The one-time `V2Backfill` and dual-read comparison now belong to the step that retires the legacy table and its editor. That is later than the Task 8 cutover planned above.
