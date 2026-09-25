package io.github.gonbei774.calisthenicsmemory.workout

import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.CompleteStep
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.Pause
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.Start
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent.Tick
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutTimerRecoveryTest {
    private val steps = listOf(
        WorkoutStep("set-1", StepKind.Stopwatch),
        WorkoutStep("rest-1", StepKind.Countdown(60_000)),
        WorkoutStep("set-2", StepKind.Stopwatch),
    )

    private fun reduce(state: WorkoutState, vararg events: WorkoutEvent) =
        events.fold(state) { s, e -> (WorkoutReducer.reduce(s, e) as Transition.Accepted).state }

    // Set 1 done at 20 s; the 60 s rest started at 20 s. Saved at 30 s monotonic / wall 1_000_030_000.
    private val inRest = reduce(WorkoutState(steps), Start(0), CompleteStep(20_000, 0))
    private fun checkpoint(state: WorkoutState = inRest, bootCount: Int? = 7) =
        WorkoutCheckpoint(state = state, savedAtMonotonicMillis = 30_000, savedAtWallMillis = 1_000_030_000, bootCount = bootCount)

    @Test fun `same boot recovery is exact and catches up at the true deadline`() {
        val recovered = WorkoutRecovery.recover(checkpoint(), nowMonotonicMillis = 95_000, nowWallMillis = 1_000_095_000, bootCount = 7)
        assertEquals(RecoveryTiming.EXACT, recovered.timing)

        val caughtUp = reduce(recovered.state, Tick(95_000))
        // Identical to a process that never died.
        assertEquals(reduce(inRest, Tick(95_000)), caughtUp)
        assertEquals(StepTimer.Running(80_000, 0), caughtUp.timer)
    }

    @Test fun `wall clock changes do not matter within the same boot`() {
        val recovered = WorkoutRecovery.recover(checkpoint(), nowMonotonicMillis = 40_000, nowWallMillis = 5, bootCount = 7)
        assertEquals(RecoveryTiming.EXACT, recovered.timing)
        assertEquals(20_000, recovered.state.elapsedInStep(40_000))
    }

    @Test fun `after a reboot elapsed time is estimated from the wall clock`() {
        // Rebooted; monotonic restarted. 30 s of wall time passed since the checkpoint.
        val recovered = WorkoutRecovery.recover(checkpoint(), nowMonotonicMillis = 5_000, nowWallMillis = 1_000_060_000, bootCount = 8)
        assertEquals(RecoveryTiming.APPROXIMATE_AFTER_REBOOT, recovered.timing)
        // 10 s into the rest at the checkpoint, plus 30 s.
        assertEquals(40_000, recovered.state.elapsedInStep(5_000))
        assertEquals(20_000L, recovered.state.remainingInStep(5_000))
        // Later events on the new timeline are accepted.
        assertEquals(2, reduce(recovered.state, Tick(25_000)).stepIndex)
    }

    @Test fun `an unknown boot count is treated as a reboot`() {
        val recovered = WorkoutRecovery.recover(checkpoint(bootCount = null), 95_000, 1_000_095_000, bootCount = null)
        assertEquals(RecoveryTiming.APPROXIMATE_AFTER_REBOOT, recovered.timing)
        assertEquals(75_000, recovered.state.elapsedInStep(95_000))
    }

    @Test fun `a wall clock that went backwards across a reboot assumes no time passed`() {
        val recovered = WorkoutRecovery.recover(checkpoint(), nowMonotonicMillis = 5_000, nowWallMillis = 999_000_000, bootCount = 8)
        assertEquals(RecoveryTiming.APPROXIMATE_CLOCK_CHANGED, recovered.timing)
        assertEquals(10_000, recovered.state.elapsedInStep(5_000))
    }

    @Test fun `paused time never elapses across recovery`() {
        val paused = reduce(inRest, Pause(30_000))
        for ((mono, wall, boot) in listOf(Triple(10_000_000L, 2_000_000_000L, 7), Triple(1_000L, 2_000_000_000L, 8))) {
            val recovered = WorkoutRecovery.recover(checkpoint(paused), mono, wall, boot)
            assertEquals(50_000L, recovered.state.remainingInStep(mono))
        }
    }
}
