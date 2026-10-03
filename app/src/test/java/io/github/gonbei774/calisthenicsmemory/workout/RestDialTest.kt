package io.github.gonbei774.calisthenicsmemory.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class RestDialTest {

    @Test fun `plus ten seconds back to the planned rest refills the ring`() {
        // A 90 s rest at 60 s left, then +10 s three times: 90 s left again, a full ring.
        assertEquals(1f, restDialProgress(plannedMillis = 90_000, remainingMillis = 90_000), 0f)
        assertEquals(60f / 90f, restDialProgress(90_000, 60_000), 1e-6f)
    }

    @Test fun `time beyond the plan stays a full ring and shrinks from there`() {
        assertEquals(1f, restDialProgress(90_000, 100_000), 0f)
    }

    @Test fun `an ended or empty rest is an empty ring`() {
        assertEquals(0f, restDialProgress(90_000, -500), 0f)
        assertEquals(0f, restDialProgress(0, 0), 0f)
    }
}
