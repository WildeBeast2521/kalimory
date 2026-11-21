package io.github.gonbei774.calisthenicsmemory.data

import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.screens.raiseLaterTargets as raiseSingle
import org.junit.Assert.assertEquals
import org.junit.Test

/** Beating the target on a set raises the targets of the sets still to do, and never lowers them. */
class RaiseLaterTargetsTest {
    private fun p(exercise: Int, n: Int, target: Int, side: String? = null, done: Int? = null, skipped: Boolean = false, warm: Boolean = false) =
        ProgramWorkoutSet(
            exerciseIndex = exercise, setNumber = n, side = side, targetValue = target, intervalSeconds = 0,
            isCompleted = done != null, actualValue = done ?: 0, isSkipped = skipped, isWarmUp = warm,
        )

    @Test fun `later sets of the same exercise are raised to what was done`() {
        val sets = mutableListOf(p(0, 1, 10, done = 14), p(0, 2, 10), p(0, 3, 12), p(1, 1, 10))
        sets.raiseLaterTargets(0)
        assertEquals(listOf(10, 14, 14, 10), sets.map { it.targetValue })
    }

    @Test fun `it never lowers a target`() {
        val sets = mutableListOf(p(0, 1, 10, done = 6), p(0, 2, 10))
        sets.raiseLaterTargets(0)
        assertEquals(10, sets[1].targetValue)
    }

    @Test fun `each side follows its own side`() {
        val sets = mutableListOf(p(0, 1, 8, "Right", done = 12), p(0, 1, 8, "Left"), p(0, 2, 8, "Right"))
        sets.raiseLaterTargets(0)
        assertEquals(listOf(8, 8, 12), sets.map { it.targetValue })
    }

    @Test fun `a set with no target stays without one`() {
        val sets = mutableListOf(p(0, 1, 0, done = 14), p(0, 2, 0))
        sets.raiseLaterTargets(0)
        assertEquals(0, sets[1].targetValue)
    }

    @Test fun `a set with no target never counts as reached`() {
        assertEquals(false, reached(0, 0))
        assertEquals(false, reached(30, 0))
        assertEquals(true, reached(10, 10))
        assertEquals(false, reached(9, 10))
    }

    @Test fun `a warm-up or a skipped set raises nothing, and done sets are untouched`() {
        val warm = mutableListOf(p(0, 1, 10, done = 20, warm = true), p(0, 2, 10))
        warm.raiseLaterTargets(0)
        assertEquals(10, warm[1].targetValue)
        val skipped = mutableListOf(p(0, 1, 10, skipped = true), p(0, 2, 10))
        skipped.raiseLaterTargets(0)
        assertEquals(10, skipped[1].targetValue)
        val settled = mutableListOf(p(0, 1, 10, done = 15), p(0, 2, 10, done = 9), p(0, 3, 10, skipped = true))
        settled.raiseLaterTargets(0)
        assertEquals(listOf(10, 10, 10), settled.map { it.targetValue })
    }

    @Test fun `a single workout raises later sets the same way`() {
        fun w(n: Int, target: Int, done: Int? = null, side: String? = null) =
            WorkoutSet(setNumber = n, side = side, targetValue = target, actualValue = done ?: 0, isCompleted = done != null)
        val sets = mutableListOf(w(1, 10, done = 13), w(2, 10), w(3, 15))
        raiseSingle(sets, 0)
        assertEquals(listOf(10, 13, 15), sets.map { it.targetValue })
    }
}
