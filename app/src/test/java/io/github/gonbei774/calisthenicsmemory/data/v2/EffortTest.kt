package io.github.gonbei774.calisthenicsmemory.data.v2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EffortTest {
    @Test fun `ratings 0 to 4 and no rating are valid, nothing else`() {
        (0..4).forEach { assertTrue("$it", Effort.isValid(it)) }
        assertTrue(Effort.isValid(null))
        assertFalse(Effort.isValid(-1))
        assertFalse(Effort.isValid(5))
    }

    @Test fun `a stored value outside the scale reads as not rated or as the top step`() {
        assertEquals(null, Effort.normalized(-1))
        assertEquals(4, Effort.normalized(9))
        assertEquals(2, Effort.normalized(2))
        assertEquals(null, Effort.normalized(null))
    }

    @Test fun `only 0 or 1 left is hard, and no rating is never hard`() {
        assertTrue(Effort.isHard(0))
        assertTrue(Effort.isHard(1))
        assertFalse(Effort.isHard(2))
        assertFalse(Effort.isHard(null))
        assertFalse(Effort.isHard(-1))
    }

    @Test fun `every set counts as work except a warm-up`() {
        fun set(warmUp: Boolean) = HistorySet(
            HistorySource.V2, null, emptyList(), 1, "2026-03-09", "07:00", 1, 10, null, "", null, null, null, warmUp = warmUp,
        )
        assertTrue(set(false).countsAsWork)
        assertFalse(set(true).countsAsWork)
    }
}
