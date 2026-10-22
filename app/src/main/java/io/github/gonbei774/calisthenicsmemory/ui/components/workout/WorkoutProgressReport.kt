package io.github.gonbei774.calisthenicsmemory.ui.components.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutPhase
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutProgress
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutProgressBus

/**
 * Publishes a workout step's progress for the notification when one of [keys] changes: the step
 * starting, pausing or resuming, or its count changing. It does not publish on every frame; a
 * running countdown is sent once with its end time, and the system ticks it.
 */
@Composable
fun ReportWorkoutProgress(vararg keys: Any?, progress: (nowWallMillis: Long) -> WorkoutProgress) {
    val current by rememberUpdatedState(progress)
    LaunchedEffect(*keys) { WorkoutProgressBus.publish(current(System.currentTimeMillis())) }
}

/** The progress of a countdown step (get ready, rest, a hold) with [remainingMillis] left. */
fun countdownProgress(
    nowWallMillis: Long,
    title: String,
    detail: String,
    phase: WorkoutPhase,
    remainingMillis: Long,
    paused: Boolean,
    stepsDone: Int,
    stepsTotal: Int,
) = WorkoutProgress(
    title = title,
    detail = detail,
    phase = phase,
    countdownEndsAtMillis = if (paused) null else nowWallMillis + remainingMillis.coerceAtLeast(0),
    pausedRemainingMillis = if (paused) remainingMillis.coerceAtLeast(0) else null,
    paused = paused,
    stepsDone = stepsDone,
    stepsTotal = stepsTotal,
)

/** Reps counted so far against the set's goal. */
fun repsProgress(
    title: String,
    detail: String,
    done: Int,
    target: Int,
    paused: Boolean,
    stepsDone: Int,
    stepsTotal: Int,
) = WorkoutProgress(
    title = title, detail = detail, phase = WorkoutPhase.REPS, repsDone = done, repsTarget = target,
    paused = paused, stepsDone = stepsDone, stepsTotal = stepsTotal,
)

/**
 * A hold counting down to its target. Once the target is reached the screen stays at 0, so the
 * notification shows no timer instead of one running into negative time.
 */
fun holdProgress(
    nowWallMillis: Long,
    title: String,
    detail: String,
    targetSeconds: Int,
    elapsedMillis: Long,
    paused: Boolean,
    stepsDone: Int,
    stepsTotal: Int,
): WorkoutProgress {
    val remaining = targetSeconds * 1_000L - elapsedMillis
    return if (remaining > 0) {
        countdownProgress(nowWallMillis, title, detail, WorkoutPhase.HOLD, remaining, paused, stepsDone, stepsTotal)
    } else {
        WorkoutProgress(title, detail, WorkoutPhase.HOLD, paused = paused, stepsDone = stepsDone, stepsTotal = stepsTotal)
    }
}
