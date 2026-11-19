package io.github.gonbei774.calisthenicsmemory.ui.screens

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import org.junit.Assert.assertEquals
import org.junit.Test

/** Ending a workout early leaves out the sets never reached, and keeps the ones skipped on purpose. */
class DropUnreachedTest {
    private fun set(n: Int, done: Boolean = false, skipped: Boolean = false, value: Int = 0) =
        WorkoutSet(setNumber = n, side = null, targetValue = 10, actualValue = value, isCompleted = done, isSkipped = skipped)

    private fun session(vararg sets: WorkoutSet) = WorkoutSession(
        exercise = Exercise(id = 1, name = "Push-up", type = "Dynamic"), totalSets = sets.size, targetValue = 10, repDuration = 2,
        startInterval = 0, intervalDuration = 0, sets = sets.toMutableList(),
    )

    @Test fun `sets never reached are dropped and the count follows`() {
        val s = session(set(1, done = true, value = 10), set(2), set(3))
        dropUnreached(s, from = 1)
        assertEquals(listOf(1), s.sets.map { it.setNumber })
        assertEquals(1, s.totalSets)
    }

    @Test fun `a set skipped on purpose stays skipped`() {
        val s = session(set(1, skipped = true), set(2), set(3))
        dropUnreached(s, from = 1)
        assertEquals(listOf(1), s.sets.map { it.setNumber })
        assertEquals(true, s.sets[0].isSkipped)
    }

    @Test fun `the set in progress with a value is kept`() {
        val s = session(set(1, done = true, value = 10), set(2, value = 6), set(3))
        dropUnreached(s, from = 1)
        assertEquals(listOf(1, 2), s.sets.map { it.setNumber })
    }

    @Test fun `ending with nothing done keeps one skipped set, not an empty workout`() {
        val s = session(set(1), set(2), set(3))
        dropUnreached(s, from = 0)
        assertEquals(1, s.sets.size)
        assertEquals(true, s.sets[0].isSkipped)
        assertEquals(1, s.totalSets)
    }

    @Test fun `nothing is dropped when every set was reached`() {
        val s = session(set(1, done = true, value = 10), set(2, skipped = true))
        dropUnreached(s, from = 0)
        assertEquals(2, s.sets.size)
    }
}
