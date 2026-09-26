# v2 workout history (Task 7)

Status: Task 7 is complete for the storage layer. Nothing in the app writes or reads v2 yet; Task 8 does.

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
