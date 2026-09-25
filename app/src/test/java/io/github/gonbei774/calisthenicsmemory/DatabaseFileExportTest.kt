package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.DatabaseFileExport
import io.github.gonbei774.calisthenicsmemory.data.DatabaseQuarantine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

class DatabaseFileExportTest {
    @get:Rule val folder = TemporaryFolder()

    private fun file(name: String, bytes: ByteArray) = File(folder.root, name).apply { writeBytes(bytes) }

    @Test fun `exports live files and the corruption copy byte for byte`() {
        val db = file("app.db", byteArrayOf(1, 2))
        file("app.db-wal", byteArrayOf(3))
        file("unrelated.db", byteArrayOf(9))
        DatabaseQuarantine.preserve(db)
        db.writeBytes(byteArrayOf(5))

        val entries = DatabaseFileExport.entries(db)
        assertEquals(listOf("app.db", "app.db-wal", "app.db.corrupt/app.db", "app.db.corrupt/app.db-wal"), entries.map { it.first })

        val zipped = ByteArrayOutputStream().also { DatabaseFileExport.writeZip(entries, it) }.toByteArray()
        val unzipped = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipped)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                unzipped[entry.name] = zip.readBytes()
            }
        }
        assertEquals(entries.map { it.first }, unzipped.keys.toList())
        assertArrayEquals(byteArrayOf(5), unzipped.getValue("app.db"))
        assertArrayEquals(byteArrayOf(1, 2), unzipped.getValue("app.db.corrupt/app.db"))
        assertArrayEquals(byteArrayOf(3), unzipped.getValue("app.db-wal"))
    }

    @Test fun `acknowledging keeps the copy and clears the notice`() {
        val db = file("app.db", byteArrayOf(1))
        assertFalse(DatabaseQuarantine.needsAttention(db))
        DatabaseQuarantine.preserve(db)
        assertTrue(DatabaseQuarantine.needsAttention(db))

        DatabaseQuarantine.acknowledge(db)

        assertFalse(DatabaseQuarantine.needsAttention(db))
        assertArrayEquals(byteArrayOf(1), File(DatabaseQuarantine.directoryFor(db), "app.db").readBytes())
    }
}
