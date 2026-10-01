# Backup validation

Rule: every JSON backup this app exports must restore on this app. Import follows ADR 0003: retain and report, never silently drop or coerce.

`BackupService.parse` decodes the backup and splits findings into two classes.

## Rejected (the database itself could not hold them)

- Wrong `app` or a backup version outside 1–11.
- Non-positive or duplicate ids in any collection.
- A duplicate group name or a duplicate exercise name/type pair (unique indexes).
- References backed by foreign keys: a record, program exercise, or interval program exercise pointing at a missing exercise or program; a program loop pointing at a missing program; a program exercise pointing at a missing loop.
- v2 history (format 9):
  - non-positive or duplicate ids;
  - an enum code no version of the app defines;
  - a session exercise whose session is missing, or whose non-null exercise or group link is missing;
  - a set entry whose session exercise is missing;
  - a duplicate order within a session or session exercise;
  - a duplicate legacy record and side pair.

A rejected backup is never written; current data is untouched.

## Accepted and reported (`BackupAnomaly`)

The database can hold these values, and older versions or interrupted operations could leave them. They are restored unchanged and returned in `ParsedBackup.anomalies`. The import preview shows how many there are.

| `BackupAnomalyKind` | Meaning |
|:---|:---|
| `EXERCISE_MISSING_GROUP` | An exercise's group name has no group row. After the restore, and at every start-up, `ExerciseGroupDao.restoreMissingGroups` adds the missing group row so the exercise is listed again. |
| `TODO_MISSING_TARGET` | A todo task's exercise, group, program, or interval program is missing. |
| `TODO_UNKNOWN_TYPE` | A todo task type the app does not know. |
| `TODO_INVALID_REPEAT_DAYS` | `repeatDays` is not empty or distinct day numbers 1–7. Readers skip the malformed tokens (`TodoTask.parseRepeatDays`). |
| `PROGRAM_EXERCISE_FOREIGN_LOOP` | A program exercise uses a loop that belongs to another program. |
| `V2_NEGATIVE_VALUE` | A v2 set entry has a negative metric or target. The app never writes one; Room cannot declare a CHECK constraint. |
| `UNKNOWN_CATALOGUE_ID` | An exercise or chain placement names a catalogue step or chain this app does not know, likely from a newer catalogue (format 11). |
| `V2_LEGACY_RECORD_MISSING` | A v2 set entry is linked to a legacy record that is not in the backup. The compatibility history treats linked entries as copies, so this set would not show. |

`BackupRoundTripTest` exports a database that holds each anomaly, restores the backup, and compares the two snapshots.

## Preventing new anomalies

Group rename and delete and exercise delete each run as one Room transaction (`ExerciseGroupDao.renameGroupAndExercises`, `ExerciseGroupDao.deleteGroupAndUngroupExercises`, `ExerciseDao.deleteExerciseAndTodoTasks`). `ExerciseDao.insertExercise` aborts on a duplicate name and type. It no longer replaces the existing row, which had cascade-deleted that row's history.

## Format 9: v2 workout history

Format 9 adds `workoutSessions`, `sessionExercises`, and `setEntries`. Enum values are written as their stable database codes. Export reads the v2 tables in the same transaction as the legacy tables. Restore replaces them in the same transaction, clearing v2 first and inserting it last. Restoring a format 1–8 backup therefore also removes v2 history, which matches the "overwrite all data" warning. Older app versions reject format 9 files as an unsupported version rather than silently dropping the v2 data.

## Format 10: interval settings

Format 10 adds four optional fields to each workout session: `intervalWorkSeconds`, `intervalRestSeconds`, `intervalRounds` and `intervalRoundRestSeconds`. They mirror database version 24 and record the interval settings an `INTERVAL_TEMPLATE` workout ran with; other sessions leave them out. A format 9 file restores with them empty. Older app versions reject format 10 files as unsupported rather than dropping the settings. A negative setting is accepted and reported as `V2_NEGATIVE_VALUE`, like a negative set value.

## Format 11: progression links

Format 11 (ADR 0007) adds an optional `catalogId` to each exercise, linking it to a catalogue step, and a `chainPlacements` list placing custom exercises in built-in chains. These mirror database version 25.
- **Rejected:** a `catalogId` shared by two exercises (a unique index), a duplicate or non-positive placement `exerciseId`, and a placement whose exercise is missing (a foreign key).
- **Unknown ids:** an unknown catalogue step or chain id is kept and reported as `UNKNOWN_CATALOGUE_ID`, because a newer catalogue may define it.
- **Older backups:** a format 1–10 backup restores with no links or placements. Older app versions reject format 11 as unsupported rather than dropping the links.
