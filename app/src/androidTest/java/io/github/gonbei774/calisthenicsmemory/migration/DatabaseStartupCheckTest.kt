package io.github.gonbei774.calisthenicsmemory.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.DatabaseQuarantine
import io.github.gonbei774.calisthenicsmemory.data.DatabaseStartupCheck
import io.github.gonbei774.calisthenicsmemory.data.DatabaseStartupState
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile

@RunWith(AndroidJUnit4::class)
class DatabaseStartupCheckTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "startup-check"
    private val file: File get() = context.getDatabasePath(dbName)
    private val opened = mutableListOf<AppDatabase>()

    private fun check() = DatabaseStartupCheck.run(context, dbName) {
        AppDatabase.build(context, dbName).also { opened += it }
    }

    private fun closeOpened() {
        opened.forEach { it.close() }
        opened.clear()
    }

    @Before
    @After
    fun cleanUp() {
        closeOpened()
        context.deleteDatabase(dbName)
        DatabaseQuarantine.directoryFor(file).deleteRecursively()
    }

    @Test
    fun healthyAndFreshDatabasesAreReady() {
        assertEquals(DatabaseStartupState.Ready, check())
        closeOpened()
        assertEquals(DatabaseStartupState.Ready, check())
    }

    @Test
    fun unsupportedVersionIsReportedWithoutChangingTheFile() {
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY NOT NULL)")
            db.version = 8
        }
        val before = file.readBytes()

        assertEquals(DatabaseStartupState.Unsupported(8), check())
        assertArrayEquals(before, file.readBytes())
    }

    @Test
    fun corruptDatabaseFailsToOpenIsKeptAndThenNeedsAttention() {
        assertEquals(DatabaseStartupState.Ready, check())
        closeOpened()
        RandomAccessFile(file, "rw").use { it.seek(0); it.write(ByteArray(100) { 0x5A }) }
        val corrupted = file.readBytes()

        val state = check()
        assertTrue("state $state", state is DatabaseStartupState.OpenFailed)
        closeOpened()
        assertArrayEquals(corrupted, file.readBytes())
        assertTrue(DatabaseQuarantine.needsAttention(file))

        // After the user restores a working database, the earlier corruption is still announced once.
        context.deleteDatabase(dbName)
        assertEquals(DatabaseStartupState.CorruptionReported, check())
        DatabaseQuarantine.acknowledge(file)
        assertEquals(DatabaseStartupState.Ready, check())
    }
}
