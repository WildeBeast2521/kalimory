package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.ui.screens.BackupFileIo
import io.github.gonbei774.calisthenicsmemory.ui.screens.BackupSizeLimitException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class BackupFileIoTest {
    @Test fun `null stream factories fail truthfully`() {
        assertThrows(IOException::class.java) { BackupFileIo.readUtf8({ null }) }
        assertThrows(IOException::class.java) { BackupFileIo.writeUtf8({ null }, "data") }
    }

    @Test fun `reads and writes exact UTF-8 and closes streams`() {
        val value = "筋力 café 😀\n"
        var inputClosed = false
        val input = object : ByteArrayInputStream(value.toByteArray(StandardCharsets.UTF_8)) {
            override fun close() { inputClosed = true; super.close() }
        }
        assertEquals(value, BackupFileIo.readUtf8({ input }))
        assertTrue(inputClosed)

        val output = TrackingOutputStream()
        BackupFileIo.writeUtf8({ output }, value)
        assertArrayEquals(value.toByteArray(StandardCharsets.UTF_8), output.bytes.toByteArray())
        assertEquals(1, output.flushes)
        assertTrue(output.closed)
    }

    @Test fun `write flush and close failures are propagated`() {
        listOf(FailurePoint.WRITE, FailurePoint.FLUSH, FailurePoint.CLOSE).forEach { point ->
            val output = TrackingOutputStream(point)
            val error = assertThrows(IOException::class.java) {
                BackupFileIo.writeUtf8({ output }, "backup")
            }
            assertEquals(point.name, error.message)
            if (point != FailurePoint.CLOSE) assertTrue("stream must be closed after $point failure", output.closed)
        }
    }

    @Test fun `accepts size limit boundary and rejects one byte over`() {
        val bytes = "éab".toByteArray(StandardCharsets.UTF_8) // four bytes, three characters
        assertEquals("éab", BackupFileIo.readUtf8({ ByteArrayInputStream(bytes) }, maxBytes = 4))
        val error = assertThrows(BackupSizeLimitException::class.java) {
            BackupFileIo.readUtf8({ ByteArrayInputStream(bytes) }, maxBytes = 3)
        }
        assertEquals(3, error.limitBytes)
    }

    private enum class FailurePoint { WRITE, FLUSH, CLOSE }

    private class TrackingOutputStream(
        private val failure: FailurePoint? = null
    ) : OutputStream() {
        val bytes = ByteArrayOutputStream()
        var flushes = 0
        var closed = false

        override fun write(value: Int) {
            if (failure == FailurePoint.WRITE) throw IOException(FailurePoint.WRITE.name)
            bytes.write(value)
        }

        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            if (failure == FailurePoint.WRITE) throw IOException(FailurePoint.WRITE.name)
            bytes.write(buffer, offset, length)
        }

        override fun flush() {
            flushes++
            if (failure == FailurePoint.FLUSH) throw IOException(FailurePoint.FLUSH.name)
        }

        override fun close() {
            closed = true
            if (failure == FailurePoint.CLOSE) throw IOException(FailurePoint.CLOSE.name)
        }
    }
}
