package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.DatabaseFileExport
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.RecoveryRestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Restoring a recovery file replaces the database only after it checks out, and keeps the old files. */
@RunWith(AndroidJUnit4::class)
class RecoveryRestoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val suffix = System.nanoTime()
    private val sourceName = "restore_source_$suffix"
    private val targetName = "restore_target_$suffix"
    private val staging = File(context.cacheDir, "restore-test-$suffix")
    private val supported = AppDatabase.OLDEST_SUPPORTED_VERSION..AppDatabase.CURRENT_VERSION

    @After
    fun cleanUp() {
        for (name in listOf(sourceName, targetName)) {
            val file = context.getDatabasePath(name)
            listOf("", "-wal", "-shm", "-journal", ".restoring").forEach { File(file.path + it).delete() }
            RecoveryRestore.beforeRestoreDirectories(file).forEach { it.deleteRecursively() }
        }
        staging.deleteRecursively()
    }

    private fun databaseWith(name: String, exercise: String): File = runBlocking {
        val database = AppDatabase.build(context, name)
        database.exerciseDao().insertExercise(Exercise(name = exercise, type = "Dynamic"))
        database.close()
        context.getDatabasePath(name)
    }

    /** A recovery zip whose entries carry the target's name, as a real export from this app would. */
    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private fun names(file: File): List<String> =
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT name FROM exercises ORDER BY name", null).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
        }

    @Test
    fun aGoodFileReplacesTheDatabaseAndTheOldFilesAreKept() {
        val source = databaseWith(sourceName, "Restored squat")
        val target = databaseWith(targetName, "Current push-up")

        val check = RecoveryRestore.stageAndCheck(
            ByteArrayInputStream(zipOf(targetName to source.readBytes())), targetName, staging, supported,
        )
        assertTrue(check is RecoveryRestore.Check.Ready)
        RecoveryRestore.install((check as RecoveryRestore.Check.Ready).staged, target, "test")

        assertEquals(listOf("Restored squat"), names(target))
        val kept = File(RecoveryRestore.beforeRestoreDirectory(target, "test"), targetName)
        assertEquals(listOf("Current push-up"), names(kept))
        // A later recovery export carries the replaced files too.
        assertTrue(DatabaseFileExport.entries(target).any { (name, _) -> name.startsWith("$targetName.before-restore-test/") })
        // And Room opens the restored file.
        runBlocking {
            val reopened = AppDatabase.build(context, targetName)
            assertEquals(listOf("Restored squat"), reopened.exerciseDao().getAllExercises().first().map { it.name })
            reopened.close()
        }
    }

    @Test
    fun aDamagedFileIsRefusedAndNothingChanges() {
        val target = databaseWith(targetName, "Current push-up")
        val garbage = "SQLite format 3\u0000".toByteArray() + ByteArray(4096) { 7 }

        val check = RecoveryRestore.stageAndCheck(ByteArrayInputStream(zipOf(targetName to garbage)), targetName, staging, supported)
        assertEquals(RecoveryRestore.Check.Rejected(RecoveryRestore.Problem.DAMAGED), check)
        assertEquals(listOf("Current push-up"), names(target))
    }

    @Test
    fun aNewerVersionIsRefused() {
        val source = databaseWith(sourceName, "Future squat")
        SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = 99 }

        val check = RecoveryRestore.stageAndCheck(ByteArrayInputStream(zipOf(targetName to source.readBytes())), targetName, staging, supported)
        assertEquals(RecoveryRestore.Check.Rejected(RecoveryRestore.Problem.UNSUPPORTED_VERSION, 99), check)
    }

    @Test
    fun otherEntriesAreIgnoredAndNeverWrittenOutside() {
        val escape = File(staging.parentFile, "escaped-$suffix")
        val check = RecoveryRestore.stageAndCheck(
            ByteArrayInputStream(zipOf("../escaped-$suffix" to byteArrayOf(1), "other.db" to byteArrayOf(2))),
            targetName, staging, supported,
        )
        assertEquals(RecoveryRestore.Check.Rejected(RecoveryRestore.Problem.NO_DATABASE), check)
        assertFalse(escape.exists())
    }
}
