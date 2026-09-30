package io.github.gonbei774.calisthenicsmemory.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The ducking windows in [SoundPlayer] match the generated sounds (scripts/sounds). */
class SoundFilesTest {
    private val raw = File("src/main/res/raw")

    /** Length of an Ogg Vorbis file, from the granule position of its last page. */
    private fun lengthMs(name: String): Long {
        val bytes = File(raw, "sound_$name.ogg").readBytes()
        val last = (bytes.size - 4 downTo 0).first { i ->
            bytes[i] == 'O'.code.toByte() && bytes[i + 1] == 'g'.code.toByte() && bytes[i + 2] == 'g'.code.toByte() && bytes[i + 3] == 'S'.code.toByte()
        }
        val granule = ByteBuffer.wrap(bytes, last + 6, 8).order(ByteOrder.LITTLE_ENDIAN).long
        return granule * 1000 / 44_100
    }

    @Test fun `every sound is present and short`() {
        listOf("countdown", "go", "set_done", "rep", "hold_tick").forEach { name ->
            assertTrue(name, lengthMs(name) in 30..1_500)
        }
    }

    @Test fun `ducking lasts as long as each sound`() {
        assertEquals(SoundPlayer.COUNTDOWN_MS, lengthMs("countdown"))
        assertEquals(SoundPlayer.GO_MS, lengthMs("go"))
        assertEquals(SoundPlayer.SET_DONE_MS, lengthMs("set_done"))
        assertEquals(SoundPlayer.HOLD_TICK_MS, lengthMs("hold_tick"))
    }
}
