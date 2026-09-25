package io.github.gonbei774.calisthenicsmemory.migration

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Every supported source version reaches the current version through
 * [AppDatabase.ALL_MIGRATIONS], both under [MigrationTestHelper] schema
 * validation and through the production [AppDatabase.build] configuration.
 */
@RunWith(Parameterized::class)
class MigrationPathTest(private val sourceVersion: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "from {0}")
        fun sourceVersions(): List<Int> =
            (AppDatabase.OLDEST_SUPPORTED_VERSION until MigrationFixtures.CURRENT_VERSION).toList()
    }

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val helperDbName = "migration-path-helper-$sourceVersion"
    private val roomDbName = "migration-path-room-$sourceVersion"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @After
    fun deleteDatabases() {
        context.deleteDatabase(helperDbName)
        context.deleteDatabase(roomDbName)
    }

    @Test
    fun migratesToCurrentSchemaWithoutLosingRows() {
        helper.createDatabase(helperDbName, sourceVersion).use { db ->
            MigrationFixtures.seed(db, sourceVersion)
        }

        val db = helper.runMigrationsAndValidate(
            helperDbName, MigrationFixtures.CURRENT_VERSION, true, *AppDatabase.ALL_MIGRATIONS,
        )
        MigrationFixtures.assertNoForeignKeyViolations(db)
        val tables = MigrationFixtures.TABLES_BY_VERSION.getValue(MigrationFixtures.CURRENT_VERSION)
        val expected = MigrationFixtures.seededRowCounts(sourceVersion)
        assertEquals(tables.associateWith { expected[it] ?: 0 }, MigrationFixtures.rowCounts(db, tables))
        db.close()
    }

    @Test
    fun productionBuilderMigratesAndReadsExactData() {
        helper.createDatabase(roomDbName, sourceVersion).use { db ->
            MigrationFixtures.seed(db, sourceVersion)
        }

        val database = AppDatabase.build(context, roomDbName)
        try {
            val snapshot = runBlocking { database.backupDao().snapshot() }
            assertEquals(MigrationFixtures.expectedSnapshot(sourceVersion), snapshot)
            MigrationFixtures.assertNoForeignKeyViolations(database.openHelper.writableDatabase)
        } finally {
            database.close()
        }
    }
}
