package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Result of opening the database before any screen uses it. */
sealed interface DatabaseStartupState {
    data object Ready : DatabaseStartupState
    data class Unsupported(val installedVersion: Int) : DatabaseStartupState
    data class OpenFailed(val details: String) : DatabaseStartupState
    /** The database opens, but SQLite reported corruption earlier and a copy was set aside. */
    data object CorruptionReported : DatabaseStartupState
}

/**
 * Opens the database once at start-up, so a database that cannot be used leads to a
 * recovery screen instead of a crash inside a screen. Runs migrations like any first
 * open and adds group rows that exercises reference but lack; never deletes data.
 */
internal object DatabaseStartupCheck {
    fun run(
        context: Context,
        name: String = AppDatabase.DATABASE_NAME,
        open: () -> AppDatabase = { AppDatabase.getDatabase(context) },
    ): DatabaseStartupState {
        val state = try {
            val database = open()
            database.openHelper.writableDatabase
                .query("SELECT COUNT(*) FROM sqlite_master")
                .use { it.moveToFirst() }
            restoreMissingGroups(database)
            DatabaseStartupState.Ready
        } catch (e: UnsupportedDatabaseVersionException) {
            DatabaseStartupState.Unsupported(e.installedVersion)
        } catch (e: Exception) {
            Log.e("CalisthenicsMemoryDb", "The database could not be opened", e)
            DatabaseStartupState.OpenFailed(e.toString())
        }
        if (state == DatabaseStartupState.Ready && DatabaseQuarantine.needsAttention(context.getDatabasePath(name))) {
            return DatabaseStartupState.CorruptionReported
        }
        return state
    }
}

/** Adds missing group rows; a failure is logged and never blocks the caller. */
internal fun restoreMissingGroups(database: AppDatabase) {
    try {
        val restored = database.exerciseGroupDao().restoreMissingGroups()
        if (restored > 0) Log.w("CalisthenicsMemoryDb", "Added $restored missing group rows used by exercises")
    } catch (e: Exception) {
        Log.e("CalisthenicsMemoryDb", "Could not add missing group rows", e)
    }
}

/** Collects the raw database files, any corruption copy and restored-over files, for a user-initiated export. */
internal object DatabaseFileExport {
    private val SIDECAR_SUFFIXES = listOf("", "-wal", "-shm", "-journal")

    /** Zip entry name to file, for the database, its sidecars, and the corruption copy. */
    fun entries(database: File): List<Pair<String, File>> {
        val live = SIDECAR_SUFFIXES.map { File(database.path + it) }.filter { it.isFile }.map { it.name to it }
        // The corruption copy and the files earlier restores replaced travel with the export.
        val kept = listOf(DatabaseQuarantine.directoryFor(database)) + RecoveryRestore.beforeRestoreDirectories(database)
        val copies = kept.flatMap { directory ->
            directory.listFiles().orEmpty().filter { it.isFile }.sortedBy { it.name }.map { "${directory.name}/${it.name}" to it }
        }
        return live + copies
    }

    fun writeZip(entries: List<Pair<String, File>>, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            for ((name, file) in entries) {
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}
