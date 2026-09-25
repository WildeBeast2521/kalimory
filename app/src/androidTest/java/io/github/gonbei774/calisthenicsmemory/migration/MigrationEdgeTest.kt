package io.github.gonbei774.calisthenicsmemory.migration

import androidx.room.testing.MigrationTestHelper
import androidx.room.migration.Migration
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Each registered migration, alone, produces its target schema without losing rows. */
@RunWith(Parameterized::class)
class MigrationEdgeTest(@Suppress("unused") private val label: String, private val migration: Migration) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun migrations(): List<Array<Any>> =
            AppDatabase.ALL_MIGRATIONS.map { arrayOf("${it.startVersion} to ${it.endVersion}", it) }
    }

    private val start = migration.startVersion
    private val end = migration.endVersion
    private val dbName = "migration-edge-$start-$end"

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

    @Test
    fun migratesOneVersionWithoutLosingRows() {
        assertEquals("edge $start -> $end", start + 1, end)
        helper.createDatabase(dbName, start).use { db ->
            MigrationFixtures.seed(db, start)
        }

        val db = helper.runMigrationsAndValidate(dbName, end, true, migration)
        MigrationFixtures.assertNoForeignKeyViolations(db)
        val tables = MigrationFixtures.TABLES_BY_VERSION.getValue(end)
        val seeded = MigrationFixtures.seededRowCounts(start)
        assertEquals("row counts after $start -> $end", tables.associateWith { seeded[it] ?: 0 }, MigrationFixtures.rowCounts(db, tables))
        db.close()
    }
}
