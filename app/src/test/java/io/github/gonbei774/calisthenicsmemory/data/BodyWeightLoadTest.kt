package io.github.gonbei774.calisthenicsmemory.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyWeightLoadTest {
    private val list = listOf(BodyWeight(1, "2026-10-11", 60_000), BodyWeight(2, "2026-12-10", 65_000), BodyWeight(3, "2027-01-02", 64_000))

    @Test fun `before the first entry there is no body weight`() {
        assertNull(BodyWeightLoad.on(LocalDate.of(2026, 10, 10), list))
        assertNull(BodyWeightLoad.on(LocalDate.of(2020, 1, 1), list))
        assertNull(BodyWeightLoad.on(LocalDate.of(2026, 10, 10), emptyList()))
    }

    @Test fun `an entry counts on its own date and until the next one, with no smoothing`() {
        assertEquals(60_000, BodyWeightLoad.on(LocalDate.of(2026, 10, 11), list))
        assertEquals(60_000, BodyWeightLoad.on(LocalDate.of(2026, 11, 30), list))
        assertEquals(60_000, BodyWeightLoad.on(LocalDate.of(2026, 12, 9), list))
        assertEquals(65_000, BodyWeightLoad.on(LocalDate.of(2026, 12, 10), list))
        assertEquals(65_000, BodyWeightLoad.on(LocalDate.of(2027, 1, 1), list))
    }

    @Test fun `the last entry counts for ever after it`() {
        assertEquals(64_000, BodyWeightLoad.on(LocalDate.of(2027, 1, 2), list))
        assertEquals(64_000, BodyWeightLoad.on(LocalDate.of(2030, 6, 1), list))
    }

    @Test fun `a stored date that cannot be read has no body weight`() {
        assertNull(BodyWeightLoad.on("2026/11/30", list))
        assertNull(BodyWeightLoad.on("", list))
        assertEquals(60_000, BodyWeightLoad.on("2026-11-30", list))
    }

    @Test fun `the load per rep is the share plus added weight minus assistance`() {
        assertEquals(39_000, BodyWeightLoad.perRepG(65, 60_000, null, null))
        assertEquals(44_000, BodyWeightLoad.perRepG(65, 60_000, 5_000, null))
        assertEquals(34_000, BodyWeightLoad.perRepG(65, 60_000, null, 5_000))
        assertEquals(60_000, BodyWeightLoad.perRepG(100, 60_000, null, null))
    }

    @Test fun `assistance larger than the share is no load, never a negative one`() {
        assertEquals(0, BodyWeightLoad.perRepG(10, 60_000, null, 30_000))
    }

    @Test fun `a heavier body gives a heavier load for the same share`() {
        assertEquals(39_000, BodyWeightLoad.perRepG(65, 60_000, null, null))
        assertEquals(42_250, BodyWeightLoad.perRepG(65, 65_000, null, null))
    }

    @Test fun `the load as a share of body weight`() {
        assertEquals(65, BodyWeightLoad.percentOfBody(39_000, 60_000))
        assertEquals(73, BodyWeightLoad.percentOfBody(44_000, 60_000))
        assertEquals(0, BodyWeightLoad.percentOfBody(10_000, 0))
    }
}
