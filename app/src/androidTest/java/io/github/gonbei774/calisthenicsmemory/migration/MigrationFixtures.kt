package io.github.gonbei774.calisthenicsmemory.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.BackupSnapshot
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import org.junit.Assert.assertEquals

/**
 * One synthetic data set, written in the column layout of each historical
 * schema version (see app/schemas), and the exact state it must have after
 * migrating to the current version.
 *
 * Columns are only written when they exist at the seeded version, so every
 * value that a migration derives or defaults is asserted rather than supplied.
 */
object MigrationFixtures {
    const val CURRENT_VERSION = AppDatabase.CURRENT_VERSION

    val TABLES_BY_VERSION: Map<Int, List<String>> = (9..CURRENT_VERSION).associateWith { version ->
        buildList {
            add("exercises")
            add("training_records")
            add("exercise_groups")
            if (version >= 12) add("todo_tasks")
            if (version >= 13) add("programs")
            if (version >= 13) add("program_exercises")
            if (version >= 15) add("program_loops")
            if (version >= 18) add("interval_programs")
            if (version >= 18) add("interval_program_exercises")
            if (version >= 18) add("interval_records")
            if (version >= 22) add("workout_sessions")
            if (version >= 22) add("session_exercises")
            if (version >= 22) add("set_entries")
        }
    }

    fun seed(db: SupportSQLiteDatabase, version: Int) {
        require(version in 9..CURRENT_VERSION) { "no fixture for version $version" }

        // Group names cover upper and lower case, which SQLite orders by byte value.
        for ((id, name) in listOf(1L to "Push", 2L to "Pull", 3L to "Legs", 4L to "abs")) {
            db.row("exercise_groups", version, "id" to id, "name" to name, "displayOrder" to (21 to groupDisplayOrder(id)))
        }

        // Exercises 1 and 3 tie on (group, sortOrder); exercises 4 and 7 share a name across types.
        exercise(db, version, 1, "Wall Push-up", "Dynamic", "Push", 2, "Bilateral", 3, 10, false)
        exercise(db, version, 2, "Incline Push-up", "Dynamic", "Push", 1, "Bilateral", 3, 8, true)
        exercise(db, version, 3, "Knee Push-up", "Dynamic", "Push", 2, "Bilateral", null, null, false)
        exercise(db, version, 4, "Plank", "Isometric", null, 0, "Bilateral", null, 60, false)
        exercise(db, version, 5, "Bulgarian Split Squat", "Dynamic", null, 0, "Unilateral", 3, 8, false)
        exercise(db, version, 6, "Dead Hang", "Isometric", "Pull", 1, "Bilateral", 2, 30, false)
        exercise(db, version, 7, "Plank", "Dynamic", null, 0, "Bilateral", null, null, false)

        record(db, version, 1, 1, 12, null, 1, "2025-01-05", "07:30", "")
        record(db, version, 2, 1, 10, null, 2, "2025-01-05", "07:30", "")
        record(db, version, 3, 5, 8, 7, 1, "2025-01-05", "07:40", "left weaker")
        record(db, version, 4, 4, 45, null, 1, "2025-01-06", "18:00", "")
        record(db, version, 5, 6, 20, null, 1, "2025-01-06", "18:10", "")

        if (version >= 12) {
            // Non-sequential IDs prove the IDs survive table rebuilds.
            for ((id, exerciseId, sortOrder) in listOf(Triple(7L, 2L, 0), Triple(3L, 5L, 1), Triple(1L, 1L, 2))) {
                if (version < 19) {
                    db.insertRow("todo_tasks", "id" to id, "exerciseId" to exerciseId, "sortOrder" to sortOrder)
                } else {
                    db.row(
                        "todo_tasks", version,
                        "id" to id, "type" to TodoTask.TYPE_EXERCISE, "referenceId" to exerciseId, "sortOrder" to sortOrder,
                        "repeatDays" to (20 to if (id == 3L) "1,3,5" else ""),
                        "lastCompletedDate" to (20 to if (id == 3L) "2025-01-05" else null),
                    )
                }
            }
            if (version >= 19) {
                db.row("todo_tasks", version, "id" to 9L, "type" to TodoTask.TYPE_PROGRAM, "referenceId" to 1L, "sortOrder" to 3, "repeatDays" to (20 to ""))
            }
        }

        if (version >= 13) {
            for ((id, name) in listOf(1L to "Beginner Push", 2L to "Empty Program")) {
                if (version == 13) {
                    db.insertRow("programs", "id" to id, "name" to name, "timerMode" to 1, "startInterval" to 10)
                } else {
                    db.insertRow("programs", "id" to id, "name" to name)
                }
            }
            if (version >= 15) {
                db.insertRow("program_loops", "id" to 1L, "programId" to 1L, "sortOrder" to 2, "rounds" to 3, "restBetweenRounds" to 120)
            }
            programExercise(db, version, 1, 1, 2, 0, 3, 10, 90, null)
            programExercise(db, version, 2, 1, 1, 1, 2, 12, 60, null)
            if (version >= 15) programExercise(db, version, 3, 1, 3, 2, 1, 8, 60, 1)
        }

        if (version >= 18) {
            db.insertRow("interval_programs", "id" to 1L, "name" to "Tabata", "workSeconds" to 20, "restSeconds" to 10, "rounds" to 8, "roundRestSeconds" to 60)
            db.insertRow("interval_program_exercises", "id" to 1L, "programId" to 1L, "exerciseId" to 4L, "sortOrder" to 0)
            db.insertRow("interval_program_exercises", "id" to 2L, "programId" to 1L, "exerciseId" to 7L, "sortOrder" to 1)
            db.insertRow(
                "interval_records",
                "id" to 1L, "programName" to "Tabata", "date" to "2025-01-07", "time" to "06:00",
                "workSeconds" to 20, "restSeconds" to 10, "rounds" to 8, "roundRestSeconds" to 60,
                "completedRounds" to 8, "completedExercisesInLastRound" to 0, "exercisesJson" to "[]", "comment" to null,
            )
        }
    }

    /** The exact current-version contents after migrating a database seeded at [sourceVersion]. */
    fun expectedSnapshot(sourceVersion: Int): BackupSnapshot {
        val s = sourceVersion
        return BackupSnapshot(
            groups = listOf(1L to "Push", 2L to "Pull", 3L to "Legs", 4L to "abs")
                .map { (id, name) -> ExerciseGroup(id, name, groupDisplayOrder(id)) },
            exercises = listOf(
                Exercise(1, "Wall Push-up", "Dynamic", "Push", 2, 1, "Bilateral", 3, 10, false,
                    restInterval = if (s >= 10) 90 else null, repDuration = if (s >= 10) 3 else null,
                    description = if (s >= 17) "Hands on wall" else null),
                Exercise(2, "Incline Push-up", "Dynamic", "Push", 1, 0, "Bilateral", 3, 8, true),
                Exercise(3, "Knee Push-up", "Dynamic", "Push", 2, 2, "Bilateral", null, null, false),
                Exercise(4, "Plank", "Isometric", null, 0, 1, "Bilateral", null, 60, false),
                Exercise(5, "Bulgarian Split Squat", "Dynamic", null, 0, 0, "Unilateral", 3, 8, false,
                    weightTrackingEnabled = s >= 11),
                Exercise(6, "Dead Hang", "Isometric", "Pull", 1, 0, "Bilateral", 2, 30, false,
                    assistanceTrackingEnabled = s >= 16),
                Exercise(7, "Plank", "Dynamic", null, 0, 1, "Bilateral", null, null, false),
            ),
            records = listOf(
                TrainingRecord(1, 1, 12, null, 1, "2025-01-05", "07:30", ""),
                TrainingRecord(2, 1, 10, null, 2, "2025-01-05", "07:30", ""),
                TrainingRecord(3, 5, 8, 7, 1, "2025-01-05", "07:40", "left weaker", weightG = if (s >= 11) 5000 else null),
                TrainingRecord(4, 4, 45, null, 1, "2025-01-06", "18:00", ""),
                TrainingRecord(5, 6, 20, null, 1, "2025-01-06", "18:10", "", assistanceG = if (s >= 16) 2500 else null),
            ),
            programs = if (s >= 13) listOf(Program(1, "Beginner Push"), Program(2, "Empty Program")) else emptyList(),
            programExercises = if (s >= 13) {
                listOfNotNull(
                    ProgramExercise(1, 1, 2, 0, 3, 10, 90, null),
                    ProgramExercise(2, 1, 1, 1, 2, 12, 60, null),
                    if (s >= 15) ProgramExercise(3, 1, 3, 2, 1, 8, 60, 1) else null,
                )
            } else emptyList(),
            programLoops = if (s >= 15) listOf(ProgramLoop(1, 1, 2, 3, 120)) else emptyList(),
            intervalPrograms = if (s >= 18) listOf(IntervalProgram(1, "Tabata", 20, 10, 8, 60)) else emptyList(),
            intervalProgramExercises = if (s >= 18) {
                listOf(IntervalProgramExercise(1, 1, 4, 0), IntervalProgramExercise(2, 1, 7, 1))
            } else emptyList(),
            intervalRecords = if (s >= 18) {
                listOf(IntervalRecord(1, "Tabata", "2025-01-07", "06:00", 20, 10, 8, 60, 8, 0, "[]", null))
            } else emptyList(),
            todoTasks = if (s >= 12) {
                listOfNotNull(
                    TodoTask(1, TodoTask.TYPE_EXERCISE, 1, 2),
                    TodoTask(3, TodoTask.TYPE_EXERCISE, 5, 1,
                        repeatDays = if (s >= 20) "1,3,5" else "", lastCompletedDate = if (s >= 20) "2025-01-05" else null),
                    TodoTask(7, TodoTask.TYPE_EXERCISE, 2, 0),
                    if (s >= 19) TodoTask(9, TodoTask.TYPE_PROGRAM, 1, 3) else null,
                )
            } else emptyList(),
        )
    }

    /** Row counts of every table present at [version] after [seed]. */
    fun seededRowCounts(version: Int): Map<String, Int> = mapOf(
        "exercises" to 7,
        "training_records" to 5,
        "exercise_groups" to 4,
        "todo_tasks" to if (version >= 19) 4 else 3,
        "programs" to 2,
        "program_exercises" to if (version >= 15) 3 else 2,
        "program_loops" to 1,
        "interval_programs" to 1,
        "interval_program_exercises" to 2,
        "interval_records" to 1,
    ).filterKeys { it in TABLES_BY_VERSION.getValue(version) }

    fun rowCounts(db: SupportSQLiteDatabase, tables: Collection<String>): Map<String, Int> =
        tables.associateWith { table ->
            db.query("SELECT COUNT(*) FROM `$table`").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
        }

    fun assertNoForeignKeyViolations(db: SupportSQLiteDatabase) {
        val violations = mutableListOf<String>()
        db.query("PRAGMA foreign_key_check").use { cursor ->
            while (cursor.moveToNext()) {
                violations += (0 until cursor.columnCount).joinToString(",") { cursor.getString(it) ?: "null" }
            }
        }
        assertEquals("PRAGMA foreign_key_check rows", emptyList<String>(), violations)
    }

    // Name order under SQLite's BINARY collation: "Legs" < "Pull" < "Push" < "abs".
    private fun groupDisplayOrder(id: Long): Int = when (id) {
        3L -> 0
        2L -> 1
        1L -> 2
        4L -> 3
        else -> error("unknown group $id")
    }

    private fun exercise(
        db: SupportSQLiteDatabase, version: Int, id: Long, name: String, type: String, group: String?,
        sortOrder: Int, laterality: String, targetSets: Int?, targetValue: Int?, isFavorite: Boolean,
    ) {
        val expected = expectedSnapshot(version).exercises.single { it.id == id }
        db.row(
            "exercises", version,
            "id" to id, "name" to name, "type" to type, "group" to group, "sortOrder" to sortOrder,
            "laterality" to laterality, "targetSets" to targetSets, "targetValue" to targetValue,
            "isFavorite" to if (isFavorite) 1 else 0,
            "displayOrder" to (10 to expected.displayOrder),
            "restInterval" to (10 to expected.restInterval),
            "repDuration" to (10 to expected.repDuration),
            "distanceTrackingEnabled" to (11 to if (expected.distanceTrackingEnabled) 1 else 0),
            "weightTrackingEnabled" to (11 to if (expected.weightTrackingEnabled) 1 else 0),
            "assistanceTrackingEnabled" to (16 to if (expected.assistanceTrackingEnabled) 1 else 0),
            "description" to (17 to expected.description),
        )
    }

    private fun record(
        db: SupportSQLiteDatabase, version: Int, id: Long, exerciseId: Long, valueRight: Int, valueLeft: Int?,
        setNumber: Int, date: String, time: String, comment: String,
    ) {
        val expected = expectedSnapshot(version).records.single { it.id == id }
        db.row(
            "training_records", version,
            "id" to id, "exerciseId" to exerciseId, "valueRight" to valueRight, "valueLeft" to valueLeft,
            "setNumber" to setNumber, "date" to date, "time" to time, "comment" to comment,
            "distanceCm" to (11 to expected.distanceCm),
            "weightG" to (11 to expected.weightG),
            "assistanceG" to (16 to expected.assistanceG),
        )
    }

    private fun programExercise(
        db: SupportSQLiteDatabase, version: Int, id: Long, programId: Long, exerciseId: Long, sortOrder: Int,
        sets: Int, targetValue: Int, intervalSeconds: Int, loopId: Long?,
    ) {
        db.row(
            "program_exercises", version,
            "id" to id, "programId" to programId, "exerciseId" to exerciseId, "sortOrder" to sortOrder,
            "sets" to sets, "targetValue" to targetValue, "intervalSeconds" to intervalSeconds,
            "loopId" to (15 to loopId),
        )
    }

    /**
     * Inserts one row. A value given as `(sinceVersion to value)` is written
     * only when the column exists at [version]; other values are always written.
     */
    private fun SupportSQLiteDatabase.row(table: String, version: Int, vararg columns: Pair<String, Any?>) {
        val present = columns.mapNotNull { (column, value) ->
            if (value is Pair<*, *> && value.first is Int) {
                if (version >= value.first as Int) column to value.second else null
            } else {
                column to value
            }
        }
        insertRow(table, *present.toTypedArray())
    }

    private fun SupportSQLiteDatabase.insertRow(table: String, vararg columns: Pair<String, Any?>) {
        for ((column, value) in columns) {
            require(value == null || value is Long || value is Int || value is String) {
                "unsupported value $value for $table.$column"
            }
        }
        // Column names are quoted because the historical schemas use the keyword `group`.
        val names = columns.joinToString(", ") { "`${it.first}`" }
        val placeholders = columns.joinToString(", ") { "?" }
        execSQL("INSERT INTO `$table` ($names) VALUES ($placeholders)", columns.map { it.second }.toTypedArray())
    }
}
