# Backup validation

Rule: every JSON backup this app exports must restore on this app. Import follows ADR 0003: retain and report, never silently drop or coerce.

`BackupService.parse` decodes the backup and splits findings into two classes.

## Rejected (the database itself could not hold them)

- Wrong `app` or a backup version outside 1–8.
- Non-positive or duplicate ids in any collection.
- A duplicate group name or a duplicate exercise name/type pair (unique indexes).
- References backed by foreign keys: a record, program exercise, or interval program exercise pointing at a missing exercise or program; a program loop pointing at a missing program; a program exercise pointing at a missing loop.

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

`BackupRoundTripTest` exports a database that holds each anomaly, restores the backup, and compares the two snapshots.

## Preventing new anomalies

Group rename and delete and exercise delete each run as one Room transaction (`ExerciseGroupDao.renameGroupAndExercises`, `ExerciseGroupDao.deleteGroupAndUngroupExercises`, `ExerciseDao.deleteExerciseAndTodoTasks`). `ExerciseDao.insertExercise` aborts on a duplicate name and type. It no longer replaces the existing row, which had cascade-deleted that row's history.
