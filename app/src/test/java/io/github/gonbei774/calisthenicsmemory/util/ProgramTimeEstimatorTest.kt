package io.github.gonbei774.calisthenicsmemory.util

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.Program
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgramTimeEstimatorTest {

    private val pushUp = Exercise(id = 1, name = "Push-up", type = "Dynamic", repDuration = 2)
    private val plank = Exercise(id = 2, name = "Plank", type = "Isometric")
    private val lunge = Exercise(id = 3, name = "Lunge", type = "Dynamic", laterality = "Unilateral")
    private val loop = ProgramLoop(id = 7, programId = 1, sortOrder = 1, rounds = 2, restBetweenRounds = 90)
    private val pes = listOf(
        ProgramExercise(programId = 1, exerciseId = 1, sortOrder = 0, sets = 2, targetValue = 10, intervalSeconds = 60),
        ProgramExercise(programId = 1, exerciseId = 3, sortOrder = 2, sets = 1, targetValue = 8, intervalSeconds = 30),
        ProgramExercise(programId = 1, exerciseId = 2, sortOrder = 0, sets = 1, targetValue = 30, intervalSeconds = 45, loopId = 7),
    )
    private val byId = listOf(pushUp, plank, lunge).associateBy { it.id }

    @Test fun `a program's estimate adds work, rests and countdowns, without a rest after the last set`() {
        // Push-up 2 x (10 x 2 s) with 60 s rests; loop: plank 30 s twice, 45 s rest and 90 s
        // between rounds; lunge 8 x 3 s on each side with 30 s rests. 5 s countdown before each set.
        // Six sets: push-up x 2, plank x 2 (one per round), lunge right and left.
        // Work 40 + 60 + 24 + 24 = 148. Rests after every set but the last:
        // 60 + 60 + (45 + 90) + (45 + 90) + 30 = 420. Countdowns 6 x 5 = 30.
        assertEquals(148 + 420 + 30, ProgramTimeEstimator.estimateSeconds(pes, listOf(loop), byId, 5))
    }

    @Test fun `the start screen and the lists agree for the same planned sets`() {
        val pairs = pes.map { it to byId.getValue(it.exerciseId) }
        val planned = ProgramSetPlanner.plan(pairs, listOf(loop), emptyMap(), prefill = false)
        val session = ProgramExecutionSession(program = Program(id = 1, name = "P"), exercises = pairs, sets = planned.toMutableList(), loops = listOf(loop))
        assertEquals(
            ProgramTimeEstimator.estimateSeconds(session, 5),
            ProgramTimeEstimator.estimateSeconds(pes, listOf(loop), byId, 5),
        )
    }

    private fun lastSession(exerciseId: Long, vararg values: Int) = values.mapIndexed { i, v ->
        io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet(
            source = io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource.V2, legacyRecordId = null,
            setEntryIds = emptyList(), exerciseId = exerciseId, date = "2026-10-01", time = "08:00",
            setNumber = i + 1, valueRight = v, valueLeft = null, comment = "", distanceCm = null, weightG = null, assistanceG = null,
        )
    }

    @Test fun `the list shows the next run's estimate, the one its start screen opens with`() = kotlinx.coroutines.test.runTest {
        // Last time: push-up 3 sets of 15, more than the program's 2 x 10.
        val history = mapOf(pushUp.id to lastSession(pushUp.id, 15, 15, 15))
        val pairs = pes.map { it to byId.getValue(it.exerciseId) }
        val startScreen = ProgramExecutionSession(
            program = Program(id = 1, name = "P"), exercises = pairs, loops = listOf(loop),
            sets = ProgramSetPlanner.plan(pairs, listOf(loop), history, prefill = true).toMutableList(),
        )
        val list = ProgramTimeEstimator.estimateNextRunSeconds(pes, listOf(loop), byId, 5, prefill = true) { history[it].orEmpty() }
        assertEquals(ProgramTimeEstimator.estimateSeconds(startScreen, 5), list)
        // One more set and 5 more reps per set than the plan: the plan's estimate is shorter.
        assertEquals(true, list > ProgramTimeEstimator.estimateSeconds(pes, listOf(loop), byId, 5))
    }

    @Test fun `with prefill off the list shows the program's own estimate`() = kotlinx.coroutines.test.runTest {
        val history = mapOf(pushUp.id to lastSession(pushUp.id, 15, 15, 15))
        assertEquals(
            ProgramTimeEstimator.estimateSeconds(pes, listOf(loop), byId, 5),
            ProgramTimeEstimator.estimateNextRunSeconds(pes, listOf(loop), byId, 5, prefill = false) { history[it].orEmpty() },
        )
    }
}
