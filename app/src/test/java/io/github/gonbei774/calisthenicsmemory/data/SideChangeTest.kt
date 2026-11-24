package io.github.gonbei774.calisthenicsmemory.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SideChangeTest {
    private fun set(side: String?, round: Int, weight: Int?, number: Int = 1, exercise: Int = 0) =
        ProgramWorkoutSet(exerciseIndex = exercise, setNumber = number, side = side, roundNumber = round, targetValue = 5, intervalSeconds = 0, weightG = weight)

    private fun change(sets: List<ProgramWorkoutSet>, target: Int, value: Int?) =
        sets.withSideChange(sets[target], { it.weightG }, { s, v -> s.copy(weightG = v) }, value).map { it.weightG }

    @Test fun `a right change moves the left while the two were equal, in every round`() {
        val sets = listOf(set("Right", 1, 5_000), set("Left", 1, 5_000), set("Right", 2, 5_000), set("Left", 2, 5_000))
        assertEquals(listOf<Int?>(6_000, 6_000, 6_000, 6_000), change(sets, 0, 6_000))
    }

    @Test fun `a left that was set apart is not overwritten by a right change`() {
        val sets = listOf(set("Right", 1, 5_000), set("Left", 1, 9_000))
        assertEquals(listOf<Int?>(6_000, 9_000), change(sets, 0, 6_000))
    }

    @Test fun `a left change touches only the left side`() {
        val sets = listOf(set("Right", 1, 5_000), set("Left", 1, 5_000))
        assertEquals(listOf<Int?>(5_000, 4_000), change(sets, 1, 4_000))
    }

    @Test fun `another set or exercise is left alone, and a two-sided set changes as one`() {
        val sets = listOf(set(null, 1, 2_000), set(null, 1, 2_000, number = 2), set(null, 1, 2_000, exercise = 1))
        assertEquals(listOf<Int?>(3_000, 2_000, 2_000), change(sets, 0, 3_000))
    }
}
