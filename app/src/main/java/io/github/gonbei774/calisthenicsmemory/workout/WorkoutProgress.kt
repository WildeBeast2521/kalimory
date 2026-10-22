package io.github.gonbei774.calisthenicsmemory.workout

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What a running workout is doing now, as its notification shows it. */
enum class WorkoutPhase { GET_READY, REPS, HOLD, WORK, REST }

/**
 * A running workout's progress for the notification: what it is ([title], such as the exercise or
 * routine), where it is ([detail], such as "Set 2 of 4"), the [phase], the reps counted against
 * their goal, and how much of the workout is done.
 *
 * A countdown is given by when it ends ([countdownEndsAtMillis], wall clock) while it runs, so the
 * system can show it ticking without the app updating every second; while paused it is the time
 * left ([pausedRemainingMillis]).
 */
data class WorkoutProgress(
    val title: String,
    val detail: String,
    val phase: WorkoutPhase,
    val repsDone: Int? = null,
    val repsTarget: Int? = null,
    val countdownEndsAtMillis: Long? = null,
    val pausedRemainingMillis: Long? = null,
    val paused: Boolean = false,
    val stepsDone: Int = 0,
    val stepsTotal: Int = 0,
)

/** The words the notification uses, from the app's strings; a plain interface keeps the text testable. */
interface WorkoutNotificationLabels {
    fun phase(phase: WorkoutPhase): String
    fun reps(done: Int, target: Int): String
    val paused: String
    fun pausedWithTimeLeft(time: String): String
    fun time(millis: Long): String
}

/** The notification's text for a [WorkoutProgress]. */
data class WorkoutNotificationContent(
    val title: String,
    val text: String,
    val subText: String,
    /** Wall-clock end of a countdown the system shows ticking; null shows no timer. */
    val countdownEndsAtMillis: Long?,
    val progressDone: Int,
    val progressTotal: Int,
) {
    companion object {
        fun of(progress: WorkoutProgress, labels: WorkoutNotificationLabels): WorkoutNotificationContent {
            val reps = if (progress.repsDone != null && progress.repsTarget != null && progress.repsTarget > 0) {
                labels.reps(progress.repsDone, progress.repsTarget)
            } else null
            val text = when {
                progress.paused && progress.pausedRemainingMillis != null ->
                    labels.pausedWithTimeLeft(labels.time(progress.pausedRemainingMillis))
                progress.paused -> listOfNotNull(labels.paused, reps).joinToString(" · ")
                reps != null -> reps
                else -> labels.phase(progress.phase)
            }
            return WorkoutNotificationContent(
                title = progress.title,
                text = text,
                subText = progress.detail,
                countdownEndsAtMillis = progress.countdownEndsAtMillis.takeUnless { progress.paused },
                progressDone = progress.stepsDone.coerceIn(0, maxOf(progress.stepsTotal, 0)),
                progressTotal = maxOf(progress.stepsTotal, 0),
            )
        }
    }
}

/**
 * The latest progress of the running workout. Workout screens publish to it; the timer service
 * shows it in its one notification, and the screens clear it when they close.
 */
object WorkoutProgressBus {
    private val current = MutableStateFlow<WorkoutProgress?>(null)
    val progress: StateFlow<WorkoutProgress?> = current.asStateFlow()

    fun publish(progress: WorkoutProgress) {
        current.value = progress
    }

    fun clear() {
        current.value = null
    }
}
