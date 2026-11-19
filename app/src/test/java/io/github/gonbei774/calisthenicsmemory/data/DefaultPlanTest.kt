package io.github.gonbei774.calisthenicsmemory.data

import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import org.junit.Assert.assertEquals
import org.junit.Test

/** The starting plan for an exercise: its own target, else one set from the last session, never 0. */
class DefaultPlanTest {
    private fun set(right: Int, left: Int? = null, warmUp: Boolean = false) = HistorySet(
        source = HistorySource.LEGACY, legacyRecordId = 1, setEntryIds = emptyList(), exerciseId = 1, date = "2026-03-09", time = "10:00",
        setNumber = 1, valueRight = right, valueLeft = left, comment = "", distanceCm = null, weightG = null, assistanceG = null, warmUp = warmUp,
    )

    @Test fun `the exercise's own target wins`() {
        assertEquals(PlanDefault(3, 12), defaultPlan(3, 12, listOf(set(20))))
    }

    @Test fun `no target is one set with the best reps of the last session`() {
        assertEquals(PlanDefault(1, 14), defaultPlan(null, null, listOf(set(10), set(14), set(12))))
    }

    @Test fun `the larger side of a one-sided set counts`() {
        assertEquals(PlanDefault(1, 9), defaultPlan(null, null, listOf(set(7, left = 9))))
    }

    @Test fun `warm-ups do not set the default`() {
        assertEquals(PlanDefault(1, 10), defaultPlan(null, null, listOf(set(30, warmUp = true), set(10))))
    }

    @Test fun `no history gives one set and no invented value`() {
        assertEquals(PlanDefault(1, null), defaultPlan(null, null, emptyList()))
    }

    @Test fun `a zero never comes out`() {
        assertEquals(PlanDefault(1, null), defaultPlan(0, 0, listOf(set(0))))
    }

    @Test fun `only the sets or only the value of a target is used on its own`() {
        assertEquals(PlanDefault(4, 15), defaultPlan(4, null, listOf(set(15))))
        assertEquals(PlanDefault(1, 8), defaultPlan(null, 8, emptyList()))
    }
}
