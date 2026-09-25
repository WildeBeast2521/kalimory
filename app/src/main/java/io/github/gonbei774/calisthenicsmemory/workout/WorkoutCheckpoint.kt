package io.github.gonbei774.calisthenicsmemory.workout

import kotlinx.serialization.Serializable

/**
 * Enough context to rebuild a workout after process death. The monotonic clock
 * restarts at boot, so the wall clock and boot count are kept to recognise a reboot.
 */
@Serializable
data class WorkoutCheckpoint(
    val version: Int = CURRENT_VERSION,
    val state: WorkoutState,
    val savedAtMonotonicMillis: Long,
    val savedAtWallMillis: Long,
    /** Settings.Global.BOOT_COUNT when saved, or null when unknown. */
    val bootCount: Int?,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** How trustworthy the recovered times are. */
enum class RecoveryTiming {
    /** Same boot: monotonic times are still valid. */
    EXACT,

    /** Rebooted or boot unknown: time since the checkpoint was estimated from the wall clock. */
    APPROXIMATE_AFTER_REBOOT,

    /** Rebooted and the wall clock went backwards: no time is assumed to have passed. */
    APPROXIMATE_CLOCK_CHANGED,
}

data class RecoveredWorkout(val state: WorkoutState, val timing: RecoveryTiming)

object WorkoutRecovery {
    /**
     * Rebuilds the state for the current clocks. Callers then reduce a
     * [WorkoutEvent.Tick] at [nowMonotonicMillis] so countdowns that ended while the
     * process was dead complete at their own deadlines. Paused time never elapses.
     */
    fun recover(
        checkpoint: WorkoutCheckpoint,
        nowMonotonicMillis: Long,
        nowWallMillis: Long,
        bootCount: Int?,
    ): RecoveredWorkout {
        val sameBoot = bootCount != null && bootCount == checkpoint.bootCount &&
            nowMonotonicMillis >= checkpoint.savedAtMonotonicMillis
        if (sameBoot) return RecoveredWorkout(checkpoint.state, RecoveryTiming.EXACT)

        val wallElapsed = nowWallMillis - checkpoint.savedAtWallMillis
        val elapsed = wallElapsed.coerceAtLeast(0)
        // Map the saved monotonic timeline onto the new one, as if `elapsed` had passed.
        val shift = nowMonotonicMillis - (checkpoint.savedAtMonotonicMillis + elapsed)
        val timing = if (wallElapsed < 0) RecoveryTiming.APPROXIMATE_CLOCK_CHANGED else RecoveryTiming.APPROXIMATE_AFTER_REBOOT
        return RecoveredWorkout(checkpoint.state.shiftedBy(shift), timing)
    }

    private fun WorkoutState.shiftedBy(shift: Long): WorkoutState = copy(
        timer = when (val t = timer) {
            is StepTimer.Running -> t.copy(sinceMillis = t.sinceMillis + shift)
            else -> t
        },
        lastEventAtMillis = lastEventAtMillis?.plus(shift),
    )
}
