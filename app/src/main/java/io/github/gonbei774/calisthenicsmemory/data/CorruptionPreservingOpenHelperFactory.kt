package io.github.gonbei774.calisthenicsmemory.data

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.io.File
import java.io.IOException

/**
 * Wraps the framework open helper so that SQLite corruption never deletes the
 * database. The default [SupportSQLiteOpenHelper.Callback.onCorruption] deletes
 * the file on any corruption report, which would erase all training history.
 * Instead, the damaged files are copied aside once and the live file is left in
 * place, so undamaged data stays readable and exportable.
 */
internal class CorruptionPreservingOpenHelperFactory(
    private val delegate: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory(),
) : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper =
        delegate.create(
            SupportSQLiteOpenHelper.Configuration(
                configuration.context,
                configuration.name,
                PreservingCallback(configuration.callback),
                configuration.useNoBackupDirectory,
                configuration.allowDataLossOnRecovery,
            )
        )

    private class PreservingCallback(private val delegate: SupportSQLiteOpenHelper.Callback) :
        SupportSQLiteOpenHelper.Callback(delegate.version) {
        override fun onConfigure(db: SupportSQLiteDatabase) = delegate.onConfigure(db)
        override fun onCreate(db: SupportSQLiteDatabase) = delegate.onCreate(db)
        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
            delegate.onUpgrade(db, oldVersion, newVersion)
        override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
            delegate.onDowngrade(db, oldVersion, newVersion)
        override fun onOpen(db: SupportSQLiteDatabase) = delegate.onOpen(db)

        // Deliberately does not call the default implementation, which deletes the file.
        override fun onCorruption(db: SupportSQLiteDatabase) {
            val path = db.path
            Log.e(TAG, "SQLite reported corruption in $path; the database is kept, not deleted")
            if (path == null || path == ":memory:") return
            try {
                val copy = DatabaseQuarantine.preserve(File(path))
                if (copy != null) Log.e(TAG, "Copied the corrupt database files to $copy")
            } catch (e: IOException) {
                Log.e(TAG, "Could not copy the corrupt database files aside", e)
            }
        }
    }

    private companion object {
        const val TAG = "CalisthenicsMemoryDb"
    }
}

/** Copies a database file and its SQLite sidecar files aside, without modifying them. */
internal object DatabaseQuarantine {
    private val SIDECAR_SUFFIXES = listOf("", "-wal", "-shm", "-journal")

    private const val ACKNOWLEDGED_MARKER = "ACKNOWLEDGED"

    fun directoryFor(database: File): File = File(database.parentFile, "${database.name}.corrupt")

    /** True when a corruption copy exists that the user has not yet been told about. */
    fun needsAttention(database: File): Boolean {
        val directory = directoryFor(database)
        return directory.isDirectory && !File(directory, ACKNOWLEDGED_MARKER).exists()
    }

    /** Records that the user saw the corruption notice. The copy itself is kept. */
    fun acknowledge(database: File) {
        val directory = directoryFor(database)
        if (directory.isDirectory) File(directory, ACKNOWLEDGED_MARKER).createNewFile()
    }

    /**
     * Copies [database] and its sidecar files into [directoryFor]. The first copy is
     * kept: when the directory already exists, nothing is copied and null is
     * returned, so repeated corruption reports cannot overwrite the earliest copy
     * or fill the disk. A partial copy is removed before the error is rethrown.
     */
    fun preserve(database: File): File? {
        val directory = directoryFor(database)
        if (directory.exists()) return null
        val staging = File(database.parentFile, "${database.name}.corrupt.tmp")
        staging.deleteRecursively()
        try {
            if (!staging.mkdirs()) throw IOException("Cannot create $staging")
            for (suffix in SIDECAR_SUFFIXES) {
                val source = File(database.path + suffix)
                if (source.isFile) source.copyTo(File(staging, source.name))
            }
            if (!staging.renameTo(directory)) throw IOException("Cannot rename $staging to $directory")
        } catch (e: IOException) {
            staging.deleteRecursively()
            throw e
        }
        return directory
    }
}
