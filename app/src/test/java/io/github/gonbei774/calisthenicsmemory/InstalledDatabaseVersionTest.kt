package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.InstalledDatabaseVersion
import io.github.gonbei774.calisthenicsmemory.data.UnsupportedDatabaseVersionException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer

class InstalledDatabaseVersionTest {
    @get:Rule val folder = TemporaryFolder()

    private val supported = 9..21

    private fun database(userVersion: Int, size: Int = 4096): File {
        val bytes = ByteArray(size)
        "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII).copyInto(bytes)
        ByteBuffer.wrap(bytes, 60, 4).putInt(userVersion)
        return File(folder.root, "app.db").apply { writeBytes(bytes) }
    }

    @Test fun `reads user_version from the header`() {
        assertEquals(21, InstalledDatabaseVersion.read(database(21)))
        assertEquals(0x01020304, InstalledDatabaseVersion.read(database(0x01020304)))
    }

    @Test fun `missing, short, and non-SQLite files are not authoritative`() {
        assertNull(InstalledDatabaseVersion.read(File(folder.root, "absent.db")))
        assertNull(InstalledDatabaseVersion.read(database(8, size = 99)))
        val notSqlite = folder.newFile("other.db").apply { writeBytes(ByteArray(4096) { 7 }) }
        assertNull(InstalledDatabaseVersion.read(notSqlite))
    }

    @Test fun `pending WAL or rollback journal content defers to SQLite`() {
        val db = database(8)
        File(db.path + "-wal").writeBytes(ByteArray(0))
        assertEquals(8, InstalledDatabaseVersion.read(db))
        File(db.path + "-wal").writeBytes(ByteArray(32))
        assertNull(InstalledDatabaseVersion.read(db))
        File(db.path + "-wal").delete()
        File(db.path + "-journal").writeBytes(ByteArray(512))
        assertNull(InstalledDatabaseVersion.read(db))
    }

    @Test fun `supported, new, and unreadable databases pass`() {
        for (version in listOf(0, 9, 15, 21)) {
            folder.root.listFiles()!!.forEach { it.delete() }
            InstalledDatabaseVersion.requireSupported(database(version), supported)
        }
        InstalledDatabaseVersion.requireSupported(File(folder.root, "absent.db"), supported)
    }

    @Test fun `unsupported versions are refused without modifying the file`() {
        for (version in listOf(1, 8, 22, -1)) {
            folder.root.listFiles()!!.forEach { it.delete() }
            val db = database(version)
            val before = db.readBytes()
            val error = assertThrows(UnsupportedDatabaseVersionException::class.java) {
                InstalledDatabaseVersion.requireSupported(db, supported)
            }
            assertEquals(version, error.installedVersion)
            assertArrayEquals(before, db.readBytes())
            assertEquals(listOf("app.db"), folder.root.list()!!.toList())
        }
    }
}
