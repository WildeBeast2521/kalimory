package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.DatabaseQuarantine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DatabaseQuarantineTest {
    @get:Rule val folder = TemporaryFolder()

    private fun file(name: String, bytes: ByteArray) = File(folder.root, name).apply { writeBytes(bytes) }

    @Test fun `copies the database and existing sidecars without changing them`() {
        val db = file("app.db", byteArrayOf(1, 2, 3))
        file("app.db-wal", byteArrayOf(4))
        file("other.db", byteArrayOf(9))

        val copy = DatabaseQuarantine.preserve(db)!!

        assertEquals(File(folder.root, "app.db.corrupt"), copy)
        assertEquals(listOf("app.db", "app.db-wal"), copy.list()!!.sorted())
        assertArrayEquals(byteArrayOf(1, 2, 3), File(copy, "app.db").readBytes())
        assertArrayEquals(byteArrayOf(4), File(copy, "app.db-wal").readBytes())
        assertArrayEquals(byteArrayOf(1, 2, 3), db.readBytes())
        assertEquals(listOf("app.db", "app.db-wal", "app.db.corrupt", "other.db"), folder.root.list()!!.sorted())
    }

    @Test fun `keeps the first copy on later reports`() {
        val db = file("app.db", byteArrayOf(1))
        DatabaseQuarantine.preserve(db)
        db.writeBytes(byteArrayOf(2))

        assertNull(DatabaseQuarantine.preserve(db))
        assertArrayEquals(byteArrayOf(1), File(DatabaseQuarantine.directoryFor(db), "app.db").readBytes())
    }
}
