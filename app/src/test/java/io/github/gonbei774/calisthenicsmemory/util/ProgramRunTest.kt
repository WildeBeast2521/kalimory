package io.github.gonbei774.calisthenicsmemory.util

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.data.v2.BodySide
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.Laterality
import io.github.gonbei774.calisthenicsmemory.data.v2.ProgramWorkoutWriter
import io.github.gonbei774.calisthenicsmemory.data.v2.SetEntryStatus
import io.github.gonbei774.calisthenicsmemory.data.v2.TimePrecision
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgramRunTest {
    private val pushUp = Exercise(1, "Push-up", "Dynamic", group = "Push")
    private val lunge = Exercise(2, "Lunge", "Dynamic", laterality = "Unilateral")
    private val plank = Exercise(3, "Plank", "Isometric")

    private fun set(
        exerciseIndex: Int, number: Int, value: Int, side: String? = null, loopId: Long? = null, round: Int = 1,
        done: Boolean = true, skipped: Boolean = false, at: Long? = 1_000L,
    ) = ProgramWorkoutSet(
        exerciseIndex, number, side, targetValue = 10, actualValue = value, isCompleted = done, isSkipped = skipped,
        intervalSeconds = 0, loopId = loopId, roundNumber = round, totalRounds = if (loopId != null) 2 else 1,
        weightG = 1_000, completedAtWallMillis = if (done) at else null,
    )

    private val session = ProgramExecutionSession(
        program = Program(9, "Upper"),
        exercises = listOf(
            ProgramExercise(21, 9, 1, 0, sets = 1, targetValue = 10, loopId = 4) to pushUp,
            ProgramExercise(22, 9, 2, 1, sets = 1, targetValue = 8) to lunge,
            ProgramExercise(23, 9, 3, 2, sets = 1, targetValue = 30) to plank,
        ),
        sets = mutableListOf(
            set(0, 1, 10, loopId = 4, round = 1),
            set(0, 1, 9, loopId = 4, round = 2, at = 2_000),
            set(1, 1, 8, side = "Right"),
            set(1, 1, 0, side = "Left", done = false, skipped = true),
            // Never reached: neither completed nor skipped.
            set(2, 1, 0, done = false),
        ),
        comment = "【Program】Upper",
        startedAtWallMillis = 100,
    )

    @Test fun `each occurrence keeps its recorded sets, loop rounds included`() {
        val run = session.toProgramRun(listOf(ExerciseGroup(5, "Push")), savedAtWallMillis = 5_000)
        assertEquals(listOf(21L, 22L, 23L), run.exercises.map { it.programExerciseId })
        assertEquals(listOf(1 to 10, 2 to 9), run.exercises[0].sets.map { it.roundNumber to it.value })
        assertEquals(listOf(null to BodySide.RIGHT, null to BodySide.LEFT), run.exercises[1].sets.map { it.roundNumber to it.side })
        assertEquals(emptyList<Any>(), run.exercises[2].sets)
        assertEquals(5L, run.exercises[0].groupId)
        assertEquals(Laterality.UNILATERAL, run.exercises[1].laterality)
        assertEquals(ExerciseKind.ISOMETRIC, run.exercises[2].kind)
        assertEquals(100L, run.startedAtWallMillis)
        // Two push-up rounds and one lunge set had a value.
        assertEquals(3, run.completedSetCount)
    }

    @Test fun `rows name the program and leave out exercises with no recorded set`() {
        val rows = ProgramWorkoutWriter.rows(session.toProgramRun(emptyList(), savedAtWallMillis = 5_000))
        assertEquals(WorkoutSourceType.PROGRAM_TEMPLATE, rows.session.sourceType)
        assertEquals(9L, rows.session.sourceTemplateId)
        assertEquals("Upper", rows.session.sourceNameSnapshot)
        assertEquals(TimePrecision.EXACT, rows.session.timePrecision)
        assertEquals(listOf(0, 1), rows.exercises.map { it.first.orderIndex })
        assertEquals(listOf(21L, 22L), rows.exercises.map { it.first.sourceProgramExerciseId })
        val (pushUpSets, lungeSets) = rows.exercises.map { it.second }
        assertEquals(listOf(1, 2), pushUpSets.map { it.roundNumber })
        assertEquals(listOf(1_000L, 2_000L), pushUpSets.map { it.completedAtEpochMillis })
        assertEquals(listOf(SetEntryStatus.COMPLETED, SetEntryStatus.SKIPPED), lungeSets.map { it.status })
        assertNull(lungeSets[1].completedAtEpochMillis)
        assertEquals(listOf(1_000, 1_000), lungeSets.map { it.addedWeightGrams })
    }
}
