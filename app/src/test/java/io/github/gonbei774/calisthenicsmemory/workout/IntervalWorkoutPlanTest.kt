package io.github.gonbei774.calisthenicsmemory.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalWorkoutPlanTest {
    private fun ids(work: Int, rest: Int, rounds: Int, roundRest: Int, exercises: Int) =
        IntervalWorkoutPlan.build(work, rest, rounds, roundRest, exercises).map { it.id }

    @Test fun `steps follow the legacy advance rules`() {
        assertEquals(
            listOf(
                "prepare",
                "work:1:0", "rest:1:0", "work:1:1", "roundRest:1",
                "work:2:0", "rest:2:0", "work:2:1",
            ),
            ids(work = 20, rest = 10, rounds = 2, roundRest = 60, exercises = 2),
        )
    }

    @Test fun `zero rests and round rests are left out`() {
        assertEquals(listOf("prepare", "work:1:0", "work:1:1", "work:2:0", "work:2:1"), ids(20, 0, 2, 0, 2))
    }

    @Test fun `durations are in milliseconds`() {
        val steps = IntervalWorkoutPlan.build(20, 10, 1, 0, 2)
        assertEquals(
            listOf(5_000L, 20_000L, 10_000L, 20_000L),
            steps.map { (it.kind as StepKind.Countdown).durationMillis },
        )
    }

    @Test fun `steps describe themselves and give the legacy stop progress`() {
        val steps = IntervalWorkoutPlan.build(20, 10, 3, 60, 2)
        fun at(id: String) = IntervalWorkoutPlan.describe(steps.single { it.id == id }, 2)

        assertEquals(IntervalStep(IntervalStepType.WORK, 2, 1), at("work:2:1"))
        assertEquals(IntervalStep(IntervalStepType.ROUND_REST, 1, 1), at("roundRest:1"))

        assertEquals(0 to 0, IntervalWorkoutPlan.progressWhenStopped(at("prepare"), 2))
        assertEquals(1 to 1, IntervalWorkoutPlan.progressWhenStopped(at("work:2:1"), 2))
        assertEquals(1 to 1, IntervalWorkoutPlan.progressWhenStopped(at("rest:2:0"), 2))
        assertEquals(1 to 2, IntervalWorkoutPlan.progressWhenStopped(at("roundRest:1"), 2))
    }

    @Test fun `a finished last round counts as a round on the result screen`() {
        // Stopped in the rest after round 2's only exercise: history says 2/3, so must the result.
        assertEquals(2 to 0, IntervalWorkoutPlan.normalized(1 to 1, 1))
        assertEquals(2 to 0, IntervalWorkoutPlan.normalized(1 to 2, 2))
        assertEquals(1 to 1, IntervalWorkoutPlan.normalized(1 to 1, 2))
        assertEquals(0 to 0, IntervalWorkoutPlan.normalized(0 to 0, 0))
    }

    @Test fun `a full run through the reducer ends at the program's total duration`() {
        val steps = IntervalWorkoutPlan.build(20, 10, 2, 60, 2)
        var state = WorkoutReducer.reduce(WorkoutState(steps), WorkoutEvent.Start(0)).state
        state = WorkoutReducer.reduce(state, WorkoutEvent.Tick(10_000_000)).state
        assertEquals(WorkoutStatus.FINISHED, state.status)
        // 5 prepare + 2 x (20 + 10 + 20) + 60 round rest = 165 s, all steps completed.
        assertEquals(165_000L, state.results.sumOf { it.elapsedMillis })
        assertEquals(steps.size, state.results.count { it.outcome == StepOutcome.COMPLETED })
    }
}
