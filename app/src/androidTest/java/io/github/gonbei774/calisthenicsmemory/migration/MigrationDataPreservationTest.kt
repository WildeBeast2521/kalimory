package io.github.gonbei774.calisthenicsmemory.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exact values produced by individual migrations. */
@RunWith(AndroidJUnit4::class)
class MigrationDataPreservationTest {
    private val dbName = "migration-data-preservation"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @After
    fun deleteDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(dbName)
    }

    private fun migrate(from: Int, to: Int, extraSeed: (SupportSQLiteDatabase) -> Unit = {}): SupportSQLiteDatabase {
        helper.createDatabase(dbName, from).use { db ->
            MigrationFixtures.seed(db, from)
            extraSeed(db)
        }
        val migrations = AppDatabase.ALL_MIGRATIONS.filter { it.startVersion >= from && it.endVersion <= to }
        return helper.runMigrationsAndValidate(dbName, to, true, *migrations.toTypedArray())
    }

    private fun SupportSQLiteDatabase.rows(sql: String): List<List<Any?>> = query(sql).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add((0 until cursor.columnCount).map { column ->
                    when {
                        cursor.isNull(column) -> null
                        cursor.getType(column) == android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(column)
                        else -> cursor.getString(column)
                    }
                })
            }
        }
    }

    @Test
    fun migrate10To11DefaultsTrackingFlagsAndLeavesNewRecordColumnsNull() {
        val db = migrate(10, 11)

        assertEquals(
            List(7) { listOf<Any?>(0L, 0L) },
            db.rows("SELECT distanceTrackingEnabled, weightTrackingEnabled FROM exercises ORDER BY id"),
        )
        assertEquals(
            List(5) { listOf<Any?>(null, null) },
            db.rows("SELECT distanceCm, weightG FROM training_records ORDER BY id"),
        )
        assertEquals(
            listOf(listOf<Any?>(2L, "Incline Push-up", 3L, 1L)),
            db.rows("SELECT id, name, targetSets, isFavorite FROM exercises WHERE id = 2"),
        )

        // The new columns accept values after the migration.
        db.execSQL("UPDATE exercises SET distanceTrackingEnabled = 1 WHERE id = 7")
        db.execSQL(
            "INSERT INTO training_records (id, exerciseId, valueRight, valueLeft, setNumber, date, time, comment, distanceCm, weightG) " +
                "VALUES (6, 7, 1, NULL, 1, '2025-01-08', '10:30', '', 500, NULL)"
        )
        assertEquals(listOf(listOf<Any?>(500L, null)), db.rows("SELECT distanceCm, weightG FROM training_records WHERE id = 6"))
        db.close()
    }

    @Test
    fun migrate9To10OrdersGroupedBySortOrderThenIdAndUngroupedByName() {
        val db = migrate(9, 10)
        // Push: 2 (sortOrder 1), then 1 and 3 tie on sortOrder 2 and follow id order.
        // Ungrouped: "Bulgarian Split Squat", then both "Plank" rows share position 1.
        assertEquals(
            listOf(
                listOf<Any?>(1L, 1L, null, null),
                listOf<Any?>(2L, 0L, null, null),
                listOf<Any?>(3L, 2L, null, null),
                listOf<Any?>(4L, 1L, null, null),
                listOf<Any?>(5L, 0L, null, null),
                listOf<Any?>(6L, 0L, null, null),
                listOf<Any?>(7L, 1L, null, null),
            ),
            db.rows("SELECT id, displayOrder, restInterval, repDuration FROM exercises ORDER BY id"),
        )
        assertEquals(7L, db.rows("SELECT COUNT(*) FROM exercises").single().single())
        db.close()
    }

    @Test
    fun migrate12To13CreatesUsableProgramTablesWithDefaultsAndCascades() {
        val db = migrate(12, 13)
        db.execSQL("INSERT INTO programs (id, name) VALUES (1, 'Program')")
        db.execSQL("INSERT INTO program_exercises (id, programId, exerciseId, sortOrder, targetValue) VALUES (1, 1, 2, 0, 10)")
        assertEquals(listOf(listOf<Any?>(0L, 5L)), db.rows("SELECT timerMode, startInterval FROM programs"))
        assertEquals(listOf(listOf<Any?>(1L, 60L)), db.rows("SELECT sets, intervalSeconds FROM program_exercises"))

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM programs WHERE id = 1")
        assertEquals(0L, db.rows("SELECT COUNT(*) FROM program_exercises").single().single())
        db.close()
    }

    @Test
    fun migrate13To14RebuildsProgramsWithoutLosingChildRows() {
        val db = migrate(13, 14)
        assertEquals(
            listOf(listOf<Any?>(1L, "Beginner Push"), listOf<Any?>(2L, "Empty Program")),
            db.rows("SELECT * FROM programs ORDER BY id"),
        )
        assertEquals(
            listOf(
                listOf<Any?>(1L, 1L, 2L, 0L, 3L, 10L, 90L),
                listOf<Any?>(2L, 1L, 1L, 1L, 2L, 12L, 60L),
            ),
            db.rows("SELECT * FROM program_exercises ORDER BY id"),
        )
        MigrationFixtures.assertNoForeignKeyViolations(db)

        // The children still reference the rebuilt table.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM programs WHERE id = 1")
        assertEquals(0L, db.rows("SELECT COUNT(*) FROM program_exercises").single().single())
        db.close()
    }

    @Test
    fun migrate14To15KeepsProgramExercisesWithNullLoop() {
        val db = migrate(14, 15)
        assertEquals(
            listOf(
                listOf<Any?>(1L, 1L, 2L, 0L, 3L, 10L, 90L, null),
                listOf<Any?>(2L, 1L, 1L, 1L, 2L, 12L, 60L, null),
            ),
            db.rows("SELECT id, programId, exerciseId, sortOrder, sets, targetValue, intervalSeconds, loopId FROM program_exercises ORDER BY id"),
        )

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO program_loops (id, programId, sortOrder, rounds, restBetweenRounds) VALUES (1, 1, 2, 3, 120)")
        db.execSQL("UPDATE program_exercises SET loopId = 1 WHERE id = 2")
        db.execSQL("DELETE FROM program_loops WHERE id = 1")
        assertEquals(listOf(listOf<Any?>(1L)), db.rows("SELECT id FROM program_exercises"))
        db.close()
    }

    @Test
    fun migrate15To16DefaultsAssistanceTracking() {
        val db = migrate(15, 16)
        assertEquals(List(7) { listOf<Any?>(0L) }, db.rows("SELECT assistanceTrackingEnabled FROM exercises ORDER BY id"))
        assertEquals(List(5) { listOf<Any?>(null) }, db.rows("SELECT assistanceG FROM training_records ORDER BY id"))
        db.close()
    }

    @Test
    fun migrate16To17LeavesDescriptionNull() {
        val db = migrate(16, 17)
        assertEquals(List(7) { listOf<Any?>(null) }, db.rows("SELECT description FROM exercises ORDER BY id"))
        db.close()
    }

    @Test
    fun migrate17To18CreatesUsableIntervalTablesThatCascade() {
        val db = migrate(17, 18)
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO interval_programs (id, name, workSeconds, restSeconds, rounds, roundRestSeconds) VALUES (1, 'Tabata', 20, 10, 8, 60)")
        db.execSQL("INSERT INTO interval_program_exercises (id, programId, exerciseId, sortOrder) VALUES (1, 1, 4, 0)")
        db.execSQL("INSERT INTO interval_program_exercises (id, programId, exerciseId, sortOrder) VALUES (2, 1, 7, 1)")
        db.execSQL(
            "INSERT INTO interval_records (programName, date, time, workSeconds, restSeconds, rounds, roundRestSeconds, " +
                "completedRounds, completedExercisesInLastRound, exercisesJson) VALUES ('Tabata', '2025-01-07', '06:00', 20, 10, 8, 60, 8, 0, '[]')"
        )
        assertEquals(listOf(listOf<Any?>(null)), db.rows("SELECT comment FROM interval_records"))

        db.execSQL("DELETE FROM exercises WHERE id = 7")
        assertEquals(listOf(listOf<Any?>(1L)), db.rows("SELECT id FROM interval_program_exercises"))
        db.execSQL("DELETE FROM interval_programs WHERE id = 1")
        assertEquals(0L, db.rows("SELECT COUNT(*) FROM interval_program_exercises").single().single())
        db.close()
    }

    @Test
    fun migrate18To19ConvertsTodoExerciseIdToExerciseReference() {
        val db = migrate(18, 19)
        assertEquals(
            listOf(
                listOf<Any?>(1L, "EXERCISE", 1L, 2L),
                listOf<Any?>(3L, "EXERCISE", 5L, 1L),
                listOf<Any?>(7L, "EXERCISE", 2L, 0L),
            ),
            db.rows("SELECT id, type, referenceId, sortOrder FROM todo_tasks ORDER BY id"),
        )
        db.close()
    }

    @Test
    fun migrate18To19KeepsTodoRowsWhoseExerciseNoLongerExists() {
        // todo_tasks has no foreign key, so a stale reference is possible; the migration must keep it.
        val db = migrate(18, 19) { it.execSQL("INSERT INTO todo_tasks (id, exerciseId, sortOrder) VALUES (20, 999, 5)") }
        assertEquals(
            listOf(listOf<Any?>(20L, "EXERCISE", 999L, 5L)),
            db.rows("SELECT id, type, referenceId, sortOrder FROM todo_tasks WHERE id = 20"),
        )
        assertEquals(4L, db.rows("SELECT COUNT(*) FROM todo_tasks").single().single())
        db.close()
    }

    @Test
    fun migrate19To20DefaultsRepeatDaysAndLastCompletedDate() {
        val db = migrate(19, 20)
        assertEquals(
            List(4) { listOf<Any?>("", null) },
            db.rows("SELECT repeatDays, lastCompletedDate FROM todo_tasks ORDER BY id"),
        )
        db.close()
    }

    @Test
    fun migrate20To21OrdersGroupsByBinaryName() {
        val db = migrate(20, 21)
        // Names are unique, so there are no ties; SQLite's BINARY collation puts "abs" last.
        assertEquals(
            listOf(
                listOf<Any?>(1L, "Push", 2L),
                listOf<Any?>(2L, "Pull", 1L),
                listOf<Any?>(3L, "Legs", 0L),
                listOf<Any?>(4L, "abs", 3L),
            ),
            db.rows("SELECT id, name, displayOrder FROM exercise_groups ORDER BY id"),
        )
        db.close()
    }
}
