package io.github.gonbei774.calisthenicsmemory.workout

import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.Abandon
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.CompleteStep
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.GoToStep
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.Pause
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.Resume
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.SkipStep
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.Start
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.Tick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutReducerTest {
    // Set (stopwatch), 60 s rest (countdown), set (stopwatch), 30 s hold (countdown).
    private val steps = listOf(
        WorkoutStep("set-1", StepKind.Stopwatch),
        WorkoutStep("rest-1", StepKind.Countdown(60_000)),
        WorkoutStep("set-2", StepKind.Stopwatch),
        WorkoutStep("hold", StepKind.Countdown(30_000)),
    )
    private val ready = WorkoutState(steps)

    private fun WorkoutState.apply(vararg events: WorkoutEvent): WorkoutState =
        events.fold(this) { state, event ->
            val transition = WorkoutReducer.reduce(state, event)
            assertTrue("$event was rejected: $transition", transition is Transition.Accepted)
            transition.state
        }

    private fun assertRejected(state: WorkoutState, event: WorkoutEvent, reason: RejectionReason) {
        val transition = WorkoutReducer.reduce(state, event)
        assertEquals(Transition.Rejected(state, reason), transition)
    }

    @Test fun `start runs the first step from the start time`() {
        val state = ready.apply(Start(1_000))
        assertEquals(WorkoutStatus.ACTIVE, state.status)
        assertEquals(StepTimer.Running(sinceMillis = 1_000, accumulatedMillis = 0), state.timer)
        assertEquals(4_500, state.elapsedInStep(5_500))
    }

    @Test fun `paused time does not elapse`() {
        val state = ready.apply(Start(0), Pause(10_000), Resume(70_000))
        assertEquals(10_000, state.elapsedInStep(70_000))
        assertEquals(15_000, state.elapsedInStep(75_000))
        val paused = state.apply(Pause(80_000))
        assertEquals(20_000, paused.elapsedInStep(500_000))
    }

    @Test fun `sub-second pauses keep partial seconds`() {
        val state = ready.apply(Start(0), Pause(1_400), Resume(5_000), Pause(5_700))
        assertEquals(2_100, state.elapsedInStep(9_000))
    }

    @Test fun `a countdown completes at its deadline even when the tick arrives late`() {
        // Set completed at 20 s; rest runs 20 s..80 s; the tick arrives at 95 s.
        val state = ready.apply(Start(0), CompleteStep(20_000, 0), Tick(95_000))
        assertEquals(2, state.stepIndex)
        assertEquals(StepTimer.Running(sinceMillis = 80_000, accumulatedMillis = 0), state.timer)
        assertEquals(15_000, state.elapsedInStep(95_000))
        assertEquals(
            listOf(StepResult(0, StepOutcome.COMPLETED, 20_000), StepResult(1, StepOutcome.COMPLETED, 60_000)),
            state.results,
        )
    }

    @Test fun `delayed ticks add no drift across many ticks`() {
        var state = ready.apply(Start(0), CompleteStep(10_000, 0))
        // Irregular, late ticks during the 60 s rest.
        for (at in listOf(11_300L, 13_900L, 30_000L, 69_999L)) state = state.apply(Tick(at))
        assertEquals(1, state.stepIndex)
        assertEquals(1L, state.remainingInStep(69_999))
        state = state.apply(Tick(70_000))
        assertEquals(2, state.stepIndex)
        assertEquals(StepTimer.Running(70_000, 0), state.timer)
    }

    @Test fun `ticks while paused change nothing`() {
        val paused = ready.apply(Start(0), CompleteStep(10_000, 0), Pause(20_000))
        val ticked = paused.apply(Tick(500_000))
        assertEquals(paused.copy(lastEventAtMillis = 500_000), ticked)
        assertEquals(50_000L, ticked.remainingInStep(500_000))
    }

    @Test fun `the last countdown finishes the workout at its deadline`() {
        val state = ready.apply(Start(0), CompleteStep(10_000, 0), CompleteStep(40_000, 1), CompleteStep(50_000, 2), Tick(200_000))
        assertEquals(WorkoutStatus.FINISHED, state.status)
        assertEquals(StepResult(3, StepOutcome.COMPLETED, 30_000), state.results.last())
        assertEquals(StepTimer.NotStarted, state.timer)
    }

    @Test fun `completing a countdown early records the time spent`() {
        val state = ready.apply(Start(0), CompleteStep(10_000, 0), CompleteStep(25_000, 1))
        assertEquals(StepResult(1, StepOutcome.COMPLETED, 15_000), state.results.last())
        assertEquals(2, state.stepIndex)
    }

    @Test fun `skip records a skipped result and moves on`() {
        val state = ready.apply(Start(0), SkipStep(3_000, 0))
        assertEquals(listOf(StepResult(0, StepOutcome.SKIPPED, 3_000)), state.results)
        assertEquals(1, state.stepIndex)
        assertEquals(StepTimer.Running(3_000, 0), state.timer)
    }

    @Test fun `advancing while paused keeps the next step paused`() {
        val state = ready.apply(Start(0), Pause(5_000), CompleteStep(9_000, 0))
        assertEquals(StepTimer.Paused(0), state.timer)
        assertEquals(StepResult(0, StepOutcome.COMPLETED, 5_000), state.results.single())
    }

    @Test fun `navigation moves without recording a result`() {
        val state = ready.apply(Start(0), GoToStep(4_000, 3))
        assertEquals(3, state.stepIndex)
        assertEquals(emptyList<StepResult>(), state.results)
        assertEquals(StepTimer.Running(4_000, 0), state.timer)
        assertRejected(state, GoToStep(5_000, 4), RejectionReason.INVALID_STEP)
    }

    @Test fun `duplicate and stale events are rejected without changing state`() {
        val active = ready.apply(Start(0))
        assertRejected(active, Start(1_000), RejectionReason.NOT_READY)
        assertRejected(active, Resume(1_000), RejectionReason.NOT_PAUSED)
        val paused = active.apply(Pause(2_000))
        assertRejected(paused, Pause(3_000), RejectionReason.NOT_RUNNING)

        // A second "complete set 1" arrives after the first already advanced.
        val advanced = active.apply(CompleteStep(5_000, 0))
        assertRejected(advanced, CompleteStep(5_100, 0), RejectionReason.STALE_STEP)
        assertRejected(advanced, SkipStep(5_100, 0), RejectionReason.STALE_STEP)
    }

    @Test fun `a completion aimed at a countdown that already ended is stale`() {
        // The rest ended at 70 s; the user taps "done" on it at 75 s.
        val inRest = ready.apply(Start(0), CompleteStep(10_000, 0))
        assertRejected(inRest, CompleteStep(75_000, 1), RejectionReason.STALE_STEP)
    }

    @Test fun `time going backwards is rejected`() {
        val state = ready.apply(Start(10_000))
        assertRejected(state, Tick(9_999), RejectionReason.TIME_WENT_BACKWARDS)
        assertRejected(state, Pause(5_000), RejectionReason.TIME_WENT_BACKWARDS)
    }

    @Test fun `finished and abandoned workouts accept only ticks`() {
        val abandoned = ready.apply(Start(0), Abandon(1_000))
        assertEquals(WorkoutStatus.ABANDONED, abandoned.status)
        assertRejected(abandoned, Resume(2_000), RejectionReason.NOT_ACTIVE)
        assertRejected(abandoned, CompleteStep(2_000, 0), RejectionReason.NOT_ACTIVE)
        assertEquals(abandoned.copy(lastEventAtMillis = 2_000), abandoned.apply(Tick(2_000)))
        assertRejected(ready, Pause(0), RejectionReason.NOT_ACTIVE)
    }

    @Test fun `the same events always produce the same state`() {
        val events = arrayOf(Start(0), Pause(1_234), Resume(9_999), CompleteStep(15_000, 0), Tick(80_000), SkipStep(81_000, 2), Tick(500_000))
        assertEquals(ready.apply(*events), ready.apply(*events))
        assertEquals(WorkoutStatus.FINISHED, ready.apply(*events).status)
    }
}
