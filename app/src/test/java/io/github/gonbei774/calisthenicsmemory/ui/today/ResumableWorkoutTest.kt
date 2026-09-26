package io.github.gonbei774.calisthenicsmemory.ui.today

import io.github.gonbei774.calisthenicsmemory.data.ProgramSessionCheckpoint
import io.github.gonbei774.calisthenicsmemory.ui.screens.SingleSessionCheckpoint
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.ResumableWorkout
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.ResumeSources
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.resumableWorkouts
import io.github.gonbei774.calisthenicsmemory.workout.IntervalSessionContext
import io.github.gonbei774.calisthenicsmemory.workout.StepKind
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutCheckpoint
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutState
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutStep
import org.junit.Assert.assertEquals
import org.junit.Test

class ResumableWorkoutTest {
    private val single = SingleSessionCheckpoint(
        exerciseId = 5, totalSets = 1, targetValue = 10, repDuration = null, startInterval = 0, intervalDuration = 60,
        sets = emptyList(), comment = "", isAutoMode = false, isDynamicCountSoundEnabled = false,
        currentSetIndex = 0, atConfirmation = false, fromToDo = false, savedAtWallMillis = 300,
    )
    private val program = ProgramSessionCheckpoint(
        programId = 3, currentSetIndex = 0, atResult = false, sets = emptyList(), comment = "", savedAtWallMillis = 100,
    )
    private val interval = WorkoutCheckpoint(
        state = WorkoutState(listOf(WorkoutStep("work", StepKind.Countdown(20_000)))),
        savedAtMonotonicMillis = 0, savedAtWallMillis = 400, bootCount = null,
        interval = IntervalSessionContext(
            programId = 7, programName = "Tabata", workSeconds = 20, restSeconds = 10, rounds = 8, roundRestSeconds = 60,
            exercises = emptyList(),
        ),
    )
    private val all = ResumeSources(single, program, savedProgramId = 4, savedProgramAtWallMillis = 200, interval = interval)
    private val empty = ResumeSources(null, null, null, 0, null)

    private fun resolve(
        sources: ResumeSources,
        exerciseIds: Set<Long> = setOf(5),
        programIds: Set<Long> = setOf(3, 4),
        intervalProgramIds: Set<Long> = setOf(7),
    ) = resumableWorkouts(sources, exerciseIds, programIds, intervalProgramIds)

    @Test fun `nothing saved means nothing to resume`() {
        assertEquals(emptyList<ResumableWorkout>(), resolve(empty))
    }

    @Test fun `every slot is listed, newest first`() {
        assertEquals(
            listOf(
                ResumableWorkout.Interval(7, 400),
                ResumableWorkout.Single(5, 300),
                ResumableWorkout.Program(4, savedByUser = true, savedAtWallMillis = 200),
                ResumableWorkout.Program(3, savedByUser = false, savedAtWallMillis = 100),
            ),
            resolve(all),
        )
    }

    @Test fun `entries whose exercise or program was deleted are left out`() {
        assertEquals(emptyList<ResumableWorkout>(), resolve(all, emptySet(), emptySet(), emptySet()))
        assertEquals(
            listOf(ResumableWorkout.Program(3, savedByUser = false, savedAtWallMillis = 100)),
            resolve(all, exerciseIds = emptySet(), programIds = setOf(3), intervalProgramIds = emptySet()),
        )
    }

    @Test fun `a timer checkpoint without interval context is not an interval workout`() {
        assertEquals(emptyList<ResumableWorkout>(), resolve(empty.copy(interval = interval.copy(interval = null))))
    }
}
