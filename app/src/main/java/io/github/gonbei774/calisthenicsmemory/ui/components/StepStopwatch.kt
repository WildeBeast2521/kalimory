package io.github.gonbei774.calisthenicsmemory.ui.components

import io.github.gonbei774.calisthenicsmemory.workout.MonotonicClock
import io.github.gonbei774.calisthenicsmemory.workout.StepKind
import io.github.gonbei774.calisthenicsmemory.workout.StepTimer
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutEvent
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutReducer
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutState
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutStep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

/**
 * Pause-exact elapsed time for one set or rest, kept by [WorkoutReducer] against a
 * [MonotonicClock]. Unlike a counter advanced after each delay(1000), it does not
 * drift and keeps partial seconds across pauses.
 */
@Stable
class StepStopwatch(private val clock: MonotonicClock, startPaused: Boolean = false) {
    private var state by mutableStateOf(
        WorkoutReducer.reduce(WorkoutState(listOf(WorkoutStep("step", StepKind.Stopwatch))), WorkoutEvent.Start(clock.nowMillis())).state
    )
    private var nowMillis by mutableLongStateOf(clock.nowMillis())

    init {
        if (startPaused) setPaused(true)
    }

    /** Elapsed time, excluding pauses, as of the last [tick] or pause change. */
    val elapsedMillis: Long get() = state.elapsedInStep(nowMillis)

    /** Whole seconds elapsed, as a count-up display shows them. */
    val elapsedSeconds: Int get() = (elapsedMillis / 1_000).toInt()

    val isPaused: Boolean get() = state.timer is StepTimer.Paused

    fun setPaused(paused: Boolean) {
        val now = clock.nowMillis()
        val event = if (paused) WorkoutEvent.Pause(now) else WorkoutEvent.Resume(now)
        // Pausing while paused, or resuming while running, is rejected and changes nothing.
        state = WorkoutReducer.reduce(state, event).state
        nowMillis = now
    }

    fun tick() {
        val now = clock.nowMillis()
        state = WorkoutReducer.reduce(state, WorkoutEvent.Tick(now)).state
        nowMillis = now
    }
}

/** Whole seconds a countdown shows for [remainingMillis]: 0.1 s left still shows 1. */
fun countdownSeconds(remainingMillis: Long): Int = ((remainingMillis.coerceAtLeast(0) + 999) / 1_000).toInt()

/**
 * A [StepStopwatch] that follows [paused] and refreshes every 100 ms while in the
 * composition. [onTick] runs in the ticking coroutine after every refresh, so sounds
 * and automatic completion keep working while the screen is off and nothing
 * recomposes. Callers reset the stopwatch by placing it under a `key(...)`.
 */
@Composable
fun rememberStepStopwatch(
    paused: Boolean,
    clock: MonotonicClock = MonotonicClock.SYSTEM,
    onTick: CoroutineScope.(StepStopwatch) -> Unit = {},
): StepStopwatch {
    val stopwatch = remember { StepStopwatch(clock, startPaused = paused) }
    val currentOnTick by rememberUpdatedState(onTick)
    LaunchedEffect(stopwatch, paused) { stopwatch.setPaused(paused) }
    LaunchedEffect(stopwatch) {
        while (true) {
            stopwatch.tick()
            currentOnTick(stopwatch)
            delay(100L)
        }
    }
    return stopwatch
}
