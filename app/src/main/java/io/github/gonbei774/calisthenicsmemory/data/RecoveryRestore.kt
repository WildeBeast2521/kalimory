package io.github.gonbei774.calisthenicsmemory.data

import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Restores the raw database from a recovery file (the zip the recovery screen exports).
 *
 * Nothing is deleted. The zip is unpacked into a staging folder, and only the database and its
 * sidecar files are taken from it. The staged copy must open, pass SQLite's integrity check and
 * carry a supported version. Only then are the current files moved aside, into a
 * `before-restore` folder that later recovery exports include, and the staged database put in
 * their place. The caller closes the open database first and restarts the app afterwards.
 */
internal object RecoveryRestore {
    private val SIDECAR_SUFFIXES = listOf("", "-wal", "-shm", "-journal")
    private const val MAX_BYTES = 512L * 1024 * 1024

    enum class Problem { NO_DATABASE, TOO_LARGE, UNSUPPORTED_VERSION, DAMAGED }

    sealed interface Check {
        data class Ready(val staged: File, val version: Int) : Check
        data class Rejected(val problem: Problem, val version: Int? = null) : Check
    }

    /** Folder that keeps the files a restore replaced, named by [stamp]. */
    fun beforeRestoreDirectory(database: File, stamp: String): File =
        File(database.parentFile, "${database.name}.before-restore-$stamp")

    /** The before-restore folders next to [database], oldest first. */
    fun beforeRestoreDirectories(database: File): List<File> =
        database.parentFile?.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("${database.name}.before-restore-") }
            .sortedBy { it.name }

    /**
     * Unpacks the database files for [databaseName] from [zip] into a fresh [staging] folder and
     * checks them. Entries with any other name, including paths, are ignored.
     */
    fun stageAndCheck(zip: InputStream, databaseName: String, staging: File, supported: IntRange): Check {
        staging.deleteRecursively()
        if (!staging.mkdirs()) throw IOException("Cannot create $staging")
        val wanted = SIDECAR_SUFFIXES.map { databaseName + it }.toSet()
        var total = 0L
        ZipInputStream(zip).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                if (entry.isDirectory || entry.name !in wanted) continue
                File(staging, entry.name).outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BYTES) return Check.Rejected(Problem.TOO_LARGE)
                        output.write(buffer, 0, read)
                    }
                }
            }
        }
        val staged = File(staging, databaseName)
        if (!staged.isFile || staged.length() == 0L) return Check.Rejected(Problem.NO_DATABASE)
        return check(staged, supported)
    }

    /**
     * Opens the staged copy (never the live database), applies any journal it carries, and folds
     * its write-ahead log into the main file so that file alone holds the data.
     */
    private fun check(staged: File, supported: IntRange): Check {
        // The default handler deletes a corrupt file; this one leaves the staged copy alone.
        val keep = DatabaseErrorHandler { }
        val database = try {
            SQLiteDatabase.openDatabase(staged.path, null, SQLiteDatabase.OPEN_READWRITE, keep)
        } catch (e: Exception) {
            return Check.Rejected(Problem.DAMAGED)
        }
        try {
            val integrity = try {
                database.rawQuery("PRAGMA integrity_check", null).use { if (it.moveToFirst()) it.getString(0) else null }
            } catch (e: Exception) {
                null
            }
            if (integrity != "ok") return Check.Rejected(Problem.DAMAGED)
            val version = database.version
            if (version !in supported) return Check.Rejected(Problem.UNSUPPORTED_VERSION, version)
            database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
            return Check.Ready(staged, version)
        } finally {
            database.close()
        }
    }

    /**
     * Moves the current files of [database] aside into [beforeRestoreDirectory] and puts the
     * checked [staged] file in their place. The copy lands under a temporary name first, so the
     * live name only ever holds a complete file.
     */
    fun install(staged: File, database: File, stamp: String) {
        // Copy first: if the disk is full, nothing has moved yet.
        val incoming = File(database.path + ".restoring")
        staged.copyTo(incoming, overwrite = true)
        val aside = beforeRestoreDirectory(database, stamp)
        if (!aside.mkdirs()) throw IOException("Cannot create $aside")
        val moved = mutableListOf<Pair<File, File>>()
        try {
            for (suffix in SIDECAR_SUFFIXES) {
                val current = File(database.path + suffix)
                val target = File(aside, current.name)
                if (current.exists()) {
                    if (!current.renameTo(target)) throw IOException("Cannot move $current aside")
                    moved += current to target
                }
            }
            if (!incoming.renameTo(database)) throw IOException("Cannot move $incoming to $database")
        } catch (e: IOException) {
            // Put the current files back, so a failed restore leaves the database as it was.
            moved.asReversed().forEach { (original, target) -> target.renameTo(original) }
            incoming.delete()
            throw e
        }
    }
}
