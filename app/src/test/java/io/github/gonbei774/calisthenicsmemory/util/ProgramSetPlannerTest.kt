package io.github.gonbei774.calisthenicsmemory.util

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgramSetPlannerTest {

    private val pushUp = Exercise(id = 1, name = "Push-up", type = "Dynamic")
    private val plank = Exercise(id = 2, name = "Plank", type = "Isometric")
    private val lunge = Exercise(id = 3, name = "Lunge", type = "Dynamic", laterality = "Unilateral")

    private fun pe(exercise: Exercise, sortOrder: Int, sets: Int, target: Int, rest: Int = 60, loopId: Long? = null) =
        ProgramExercise(programId = 1, exerciseId = exercise.id, sortOrder = sortOrder, sets = sets, targetValue = target, intervalSeconds = rest, loopId = loopId)

    private fun last(exercise: Exercise, vararg values: Int, left: Int? = null) = values.mapIndexed { i, v ->
        HistorySet(
            source = HistorySource.V2, legacyRecordId = null, setEntryIds = emptyList(), exerciseId = exercise.id,
            date = "2026-10-01", time = "08:00", setNumber = i + 1, valueRight = v, valueLeft = left,
            comment = "", distanceCm = null, weightG = null, assistanceG = null,
        )
    }

    @Test fun `without prefill each exercise runs the program's sets and targets`() {
        val sets = ProgramSetPlanner.plan(
            listOf(pe(pushUp, 0, sets = 3, target = 10) to pushUp),
            emptyList(), mapOf(pushUp.id to last(pushUp, 12, 11)), prefill = false,
        )
        assertEquals(3, sets.size)
        assertEquals(listOf(10, 10, 10), sets.map { it.targetValue })
        assertEquals(listOf(12, 11, null), sets.map { it.previousValue })
    }

    @Test fun `with prefill a standalone exercise repeats its last session`() {
        val sets = ProgramSetPlanner.plan(
            listOf(pe(pushUp, 0, sets = 3, target = 10) to pushUp),
            emptyList(), mapOf(pushUp.id to last(pushUp, 12, 11)), prefill = true,
        )
        assertEquals(listOf(12, 11), sets.map { it.targetValue })
    }

    @Test fun `with prefill but no history the program's targets are used`() {
        val sets = ProgramSetPlanner.plan(listOf(pe(plank, 0, sets = 2, target = 30) to plank), emptyList(), emptyMap(), prefill = true)
        assertEquals(listOf(30, 30), sets.map { it.targetValue })
    }

    @Test fun `a loop repeats its exercises for every round and rests after the last one`() {
        val loop = ProgramLoop(id = 7, programId = 1, sortOrder = 1, rounds = 2, restBetweenRounds = 90)
        val sets = ProgramSetPlanner.plan(
            listOf(
                pe(pushUp, 0, sets = 1, target = 10) to pushUp,
                pe(plank, 0, sets = 1, target = 30, loopId = 7) to plank,
                pe(pushUp, 1, sets = 1, target = 8, loopId = 7) to pushUp,
            ),
            listOf(loop), emptyMap(), prefill = false,
        )
        // Standalone push-up (sort 0) first, then the loop (sort 1): plank, push-up, plank, push-up.
        assertEquals(listOf(0, 1, 2, 1, 2), sets.map { it.exerciseIndex })
        assertEquals(listOf(1, 1, 1, 2, 2), sets.map { it.roundNumber })
        assertEquals(listOf(0, 0, 90, 0, 90), sets.map { it.loopRestAfterSeconds })
    }

    @Test fun `a loop keeps the program's set count even with a longer last session`() {
        val loop = ProgramLoop(id = 7, programId = 1, sortOrder = 0, rounds = 3, restBetweenRounds = 60)
        val sets = ProgramSetPlanner.plan(
            listOf(pe(pushUp, 0, sets = 1, target = 10, loopId = 7) to pushUp),
            listOf(loop), mapOf(pushUp.id to last(pushUp, 9, 9, 9)), prefill = true,
        )
        assertEquals(3, sets.size)
        assertEquals(listOf(9, 9, 9), sets.map { it.targetValue })
    }

    @Test fun `a one-sided exercise has a right and a left set for each set`() {
        val sets = ProgramSetPlanner.plan(listOf(pe(lunge, 0, sets = 2, target = 8) to lunge), emptyList(), emptyMap(), prefill = false)
        assertEquals(listOf("Right", "Left", "Right", "Left"), sets.map { it.side })
    }
}
