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

    private fun migrate(from: Int, to: Int): SupportSQLiteDatabase {
        helper.createDatabase(dbName, from).use { db -> MigrationFixtures.seed(db, from) }
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
}
