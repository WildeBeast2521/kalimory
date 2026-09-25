package io.github.gonbei774.calisthenicsmemory.migration

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile

/** SQLite corruption never deletes the database; the damaged bytes are also copied aside once. */
@RunWith(AndroidJUnit4::class)
class CorruptDatabaseTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "corrupt-database"
    private val file: File get() = context.getDatabasePath(dbName)
    private val quarantine: File get() = File(file.parentFile, "$dbName.corrupt")

    @After
    fun cleanUp() {
        context.deleteDatabase(dbName)
        quarantine.deleteRecursively()
    }

    @Test
    fun corruptionKeepsTheDatabaseFileAndCopiesItAside() {
        context.deleteDatabase(dbName)
        quarantine.deleteRecursively()
        AppDatabase.build(context, dbName).also { database ->
            runBlocking { database.exerciseDao().insertExercise(Exercise(name = "Synthetic row", type = "Dynamic")) }
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            database.close()
        }
        // Overwrite the header, which SQLite reports as corruption when it opens the file.
        RandomAccessFile(file, "rw").use { it.seek(0); it.write(ByteArray(100) { 0x5A }) }
        val corrupted = file.readBytes()

        val database = AppDatabase.build(context, dbName)
        try {
            assertThrows(SQLiteException::class.java) { database.openHelper.writableDatabase }
        } finally {
            database.close()
        }

        assertTrue("database file must not be deleted", file.isFile)
        assertArrayEquals(corrupted, file.readBytes())
        val copy = File(quarantine, dbName)
        assertTrue("quarantine copy at $copy", copy.isFile)
        assertArrayEquals(corrupted, copy.readBytes())
    }
}
