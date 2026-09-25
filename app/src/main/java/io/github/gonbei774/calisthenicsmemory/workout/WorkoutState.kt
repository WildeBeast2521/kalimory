package io.github.gonbei774.calisthenicsmemory.workout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Durable workout state. All times are milliseconds on a monotonic clock
 * ([MonotonicClock]); elapsed time is always computed from "now", never counted
 * from render ticks.
 */
@Serializable
data class WorkoutState(
    val steps: List<WorkoutStep>,
    val status: WorkoutStatus = WorkoutStatus.READY,
    val stepIndex: Int = 0,
    val timer: StepTimer = StepTimer.NotStarted,
    val results: List<StepResult> = emptyList(),
    /** Monotonic time of the last accepted event; later events may not go back before it. */
    val lastEventAtMillis: Long? = null,
) {
    init {
        require(steps.isNotEmpty()) { "A workout needs at least one step" }
        require(stepIndex in steps.indices) { "stepIndex $stepIndex is outside ${steps.indices}" }
    }

    val currentStep: WorkoutStep get() = steps[stepIndex]

    /** Time spent in the current step at [nowMillis], excluding paused time. */
    fun elapsedInStep(nowMillis: Long): Long = when (val t = timer) {
        StepTimer.NotStarted -> 0
        is StepTimer.Running -> t.accumulatedMillis + (nowMillis - t.sinceMillis).coerceAtLeast(0)
        is StepTimer.Paused -> t.accumulatedMillis
    }

    /** Remaining time of a countdown step at [nowMillis], or null for a stopwatch step. */
    fun remainingInStep(nowMillis: Long): Long? = when (val kind = currentStep.kind) {
        is StepKind.Countdown -> (kind.durationMillis - elapsedInStep(nowMillis)).coerceAtLeast(0)
        StepKind.Stopwatch -> null
    }
}

@Serializable
enum class WorkoutStatus { READY, ACTIVE, FINISHED, ABANDONED }

@Serializable
data class WorkoutStep(val id: String, val kind: StepKind)

@Serializable
sealed interface StepKind {
    /** Ends by itself when the duration has elapsed, for example a rest or a timed hold. */
    @Serializable
    @SerialName("countdown")
    data class Countdown(val durationMillis: Long) : StepKind {
        init {
            require(durationMillis > 0) { "Countdown duration must be positive" }
        }
    }

    /** Runs until the user completes or skips it, for example a set of reps. */
    @Serializable
    @SerialName("stopwatch")
    data object Stopwatch : StepKind
}

@Serializable
sealed interface StepTimer {
    @Serializable
    @SerialName("notStarted")
    data object NotStarted : StepTimer

    @Serializable
    @SerialName("running")
    data class Running(val sinceMillis: Long, val accumulatedMillis: Long) : StepTimer

    @Serializable
    @SerialName("paused")
    data class Paused(val accumulatedMillis: Long) : StepTimer
}

@Serializable
data class StepResult(val stepIndex: Int, val outcome: StepOutcome, val elapsedMillis: Long)

@Serializable
enum class StepOutcome { COMPLETED, SKIPPED }
