package io.github.gonbei774.calisthenicsmemory.data

import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MuscleSetConvertersTest {
    private val converters = MuscleSetConverters()

    @Test fun `no muscles is an empty text and back`() {
        assertEquals("", converters.musclesToText(emptySet()))
        assertEquals(emptySet<Muscle>(), converters.textToMuscles(""))
    }

    @Test fun `muscles are written in enum order by code, whatever order they were chosen in`() {
        assertEquals("biceps,lower back", converters.musclesToText(linkedSetOf(Muscle.LOWER_BACK, Muscle.BICEPS)))
    }

    @Test fun `every muscle survives a round trip`() {
        val all = Muscle.entries.toSet()
        assertEquals(all, converters.textToMuscles(converters.musclesToText(all)))
    }

    @Test fun `an unknown code is refused rather than guessed`() {
        assertThrows(IllegalArgumentException::class.java) { converters.textToMuscles("biceps,wings") }
    }
}
