package io.github.gonbei774.calisthenicsmemory.data

import java.io.File
import java.nio.ByteBuffer

/** Thrown before Room opens an installed database whose version this build cannot migrate. */
class UnsupportedDatabaseVersionException(val installedVersion: Int, supported: IntRange) : IllegalStateException(
    "Installed database version $installedVersion is not supported by this build " +
        "(supported: ${supported.first}-${supported.last}). The database was not opened or modified."
)

/**
 * Reads the installed database version from the SQLite file header without
 * opening the database, so an unsupported database can be refused before
 * SQLite or Room writes to it (for example, when switching it to WAL mode).
 */
internal object InstalledDatabaseVersion {
    private val MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    private const val HEADER_SIZE = 100
    private const val USER_VERSION_OFFSET = 60

    /**
     * The header's user_version, or null when the header alone is not
     * authoritative: no file, a file that is not a complete SQLite header, or a
     * non-empty WAL or rollback journal that SQLite has yet to apply.
     */
    fun read(file: File): Int? {
        if (!file.isFile) return null
        if (hasContent(File(file.path + "-wal")) || hasContent(File(file.path + "-journal"))) return null
        val header = ByteArray(HEADER_SIZE)
        var filled = 0
        file.inputStream().use { input ->
            while (filled < HEADER_SIZE) {
                val count = input.read(header, filled, HEADER_SIZE - filled)
                if (count < 0) break
                filled += count
            }
        }
        if (filled < HEADER_SIZE || !header.copyOf(MAGIC.size).contentEquals(MAGIC)) return null
        return ByteBuffer.wrap(header, USER_VERSION_OFFSET, 4).int
    }

    /** Throws [UnsupportedDatabaseVersionException] when [file] holds a version outside [supported]. */
    fun requireSupported(file: File, supported: IntRange) {
        val version = read(file) ?: return
        // Version 0 is a file SQLite created but Room has not initialized yet.
        if (version != 0 && version !in supported) throw UnsupportedDatabaseVersionException(version, supported)
    }

    private fun hasContent(file: File) = file.isFile && file.length() > 0
}
