package io.github.gonbei774.calisthenicsmemory.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.UnsupportedDatabaseVersionException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Fresh installs open at the current version; unsupported databases fail closed. */
@RunWith(AndroidJUnit4::class)
class DatabaseOpenPolicyTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "database-open-policy"

    @After
    fun deleteDatabase() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun freshInstallCreatesCurrentVersion() {
        context.deleteDatabase(dbName)
        val database = AppDatabase.build(context, dbName)
        try {
            val id = runBlocking {
                database.exerciseDao().insertExercise(Exercise(name = "Pull-up", type = "Dynamic"))
            }
            val snapshot = runBlocking { database.backupDao().snapshot() }
            assertEquals(listOf(Exercise(id = id, name = "Pull-up", type = "Dynamic")), snapshot.exercises)
            assertEquals(MigrationFixtures.CURRENT_VERSION, database.openHelper.readableDatabase.version)
            MigrationFixtures.assertNoForeignKeyViolations(database.openHelper.writableDatabase)
        } finally {
            database.close()
        }
    }

    @Test
    fun versionOlderThanSupportedFailsClosedAndKeepsBytes() {
        assertUnsupportedVersionFailsClosed(AppDatabase.OLDEST_SUPPORTED_VERSION - 1, writeAheadLog = false)
        assertUnsupportedVersionFailsClosed(AppDatabase.OLDEST_SUPPORTED_VERSION - 1, writeAheadLog = true)
    }

    @Test
    fun versionNewerThanCurrentFailsClosedAndKeepsBytes() {
        assertUnsupportedVersionFailsClosed(AppDatabase.CURRENT_VERSION + 1, writeAheadLog = false)
        assertUnsupportedVersionFailsClosed(AppDatabase.CURRENT_VERSION + 1, writeAheadLog = true)
    }

    private fun assertUnsupportedVersionFailsClosed(version: Int, writeAheadLog: Boolean) {
        val label = "version $version, WAL $writeAheadLog"
        val file = context.getDatabasePath(dbName)
        context.deleteDatabase(dbName)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            if (writeAheadLog) db.enableWriteAheadLogging()
            db.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY NOT NULL, name TEXT NOT NULL)")
            db.execSQL("INSERT INTO exercises (id, name) VALUES (1, 'Synthetic squat')")
            db.version = version
        }
        val filesBefore = siblings(file).associateWith { File(file.parentFile, it).readBytes().toList() }

        val error = assertThrows(label, UnsupportedDatabaseVersionException::class.java) {
            AppDatabase.build(context, dbName).close()
        }
        assertEquals(label, version, error.installedVersion)

        val filesAfter = siblings(file).associateWith { File(file.parentFile, it).readBytes().toList() }
        assertEquals("database files after refusing $label", filesBefore, filesAfter)
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(label, version, db.version)
            db.rawQuery("SELECT id, name FROM exercises", null).use { cursor ->
                assertEquals(label, 1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
                assertEquals("Synthetic squat", cursor.getString(1))
            }
        }
    }

    private fun siblings(file: File): List<String> =
        file.parentFile!!.list()!!.filter { it.startsWith(file.name) }.sorted()
}
