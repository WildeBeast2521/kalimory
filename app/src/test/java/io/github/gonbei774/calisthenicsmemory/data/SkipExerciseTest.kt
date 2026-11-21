package io.github.gonbei774.calisthenicsmemory.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Skip set and Skip exercise only mark sets skipped: no value, no finish time, nothing completed. */
class SkipExerciseTest {
    private fun set(exercise: Int, n: Int, round: Int = 1, side: String? = null, done: Boolean = false) = ProgramWorkoutSet(
        exerciseIndex = exercise, setNumber = n, side = side, targetValue = 10, intervalSeconds = 0,
        roundNumber = round, isCompleted = done, actualValue = if (done) 10 else 0,
    )

    @Test fun `a skipped set has no value and no finish time and is not done`() {
        val sets = mutableListOf(set(0, 1).copy(actualValue = 7, completedAtWallMillis = 5L))
        sets.markSkipped(0)
        assertTrue(sets[0].isSkipped)
        assertFalse(sets[0].isCompleted)
        assertEquals(0, sets[0].actualValue)
        assertEquals(null, sets[0].completedAtWallMillis)
    }

    @Test fun `a set already done is left alone`() {
        val sets = mutableListOf(set(0, 1, done = true))
        sets.markSkipped(0)
        assertFalse(sets[0].isSkipped)
        assertEquals(10, sets[0].actualValue)
    }

    @Test fun `skipping an exercise marks the rest of its run and returns the last index`() {
        val sets = mutableListOf(set(0, 1), set(0, 2), set(0, 3), set(1, 1))
        assertEquals(2, skipExercise(sets, 1))
        assertEquals(listOf(false, true, true, false), sets.map { it.isSkipped })
    }

    @Test fun `both sides of a one-sided set are skipped together`() {
        val sets = mutableListOf(set(0, 1, side = "Right"), set(0, 1, side = "Left"), set(1, 1))
        assertEquals(1, skipExercise(sets, 0))
        assertEquals(listOf(true, true, false), sets.map { it.isSkipped })
    }

    @Test fun `started on the second side only the rest is marked`() {
        val sets = mutableListOf(set(0, 1, side = "Right", done = true), set(0, 1, side = "Left"))
        assertEquals(1, skipExercise(sets, 1))
        assertEquals(listOf(false, true), sets.map { it.isSkipped })
    }

    @Test fun `only the current round of a loop is skipped`() {
        val sets = mutableListOf(set(0, 1, round = 1), set(1, 1, round = 1), set(0, 2, round = 2), set(1, 2, round = 2))
        skipExercise(sets, 0)
        assertEquals(listOf(true, false, false, false), sets.map { it.isSkipped })
    }

    @Test fun `skipping the last exercise leads to the result`() {
        val sets = mutableListOf(set(0, 1, done = true), set(1, 1), set(1, 2))
        val last = skipExercise(sets, 1)
        assertEquals(AfterSet(AfterSet.Kind.RESULT, last, false), afterSet(sets, last, false, 5))
    }

    @Test fun `skipping an exercise goes on to the next one with a countdown`() {
        val sets = mutableListOf(set(0, 1), set(0, 2), set(1, 1))
        val last = skipExercise(sets, 0)
        assertEquals(AfterSet(AfterSet.Kind.COUNTDOWN, 2, false), afterSet(sets, last, false, 5))
    }
}
