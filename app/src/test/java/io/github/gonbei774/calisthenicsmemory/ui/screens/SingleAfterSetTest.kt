package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.data.AfterSet
import org.junit.Assert.assertEquals
import org.junit.Test

/** Where a single workout goes after a set, finished or skipped. */
class SingleAfterSetTest {
    private fun sets(n: Int, skippedAt: Int? = null) = List(n) {
        WorkoutSet(setNumber = it + 1, side = null, targetValue = 10, isSkipped = it == skippedAt)
    }

    @Test fun `a finished set with a rest goes to the rest`() {
        assertEquals(AfterSet(AfterSet.Kind.REST, 0, false), singleAfterSet(sets(3), 0, 60, 0))
    }

    @Test fun `a finished set with no rest goes straight to the next set`() {
        assertEquals(AfterSet(AfterSet.Kind.RUN, 1, false), singleAfterSet(sets(3), 0, 0, 0))
    }

    @Test fun `the last set ends the workout`() {
        assertEquals(AfterSet(AfterSet.Kind.RESULT, 2, false), singleAfterSet(sets(3), 2, 60, 5))
    }

    @Test fun `a skipped set has no rest and shows the countdown`() {
        val s = sets(3, skippedAt = 0)
        assertEquals(AfterSet(AfterSet.Kind.COUNTDOWN, 1, false), singleAfterSet(s, 0, 60, 5))
        assertEquals(AfterSet(AfterSet.Kind.RUN, 1, false), singleAfterSet(s, 0, 60, 0))
    }

    @Test fun `skipping the last set ends the workout`() {
        assertEquals(AfterSet(AfterSet.Kind.RESULT, 2, false), singleAfterSet(sets(3, skippedAt = 2), 2, 60, 5))
    }
}
