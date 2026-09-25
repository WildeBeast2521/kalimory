package io.github.gonbei774.calisthenicsmemory.workout

/** Inputs to [WorkoutReducer]. Every event carries the monotonic time it happened. */
sealed interface WorkoutEvent {
    val atMillis: Long

    data class Start(override val atMillis: Long) : WorkoutEvent
    data class Pause(override val atMillis: Long) : WorkoutEvent
    data class Resume(override val atMillis: Long) : WorkoutEvent

    /** Recomputes time; completes countdown steps whose deadline has passed. */
    data class Tick(override val atMillis: Long) : WorkoutEvent

    /** Completes [stepIndex]; rejected when the current step is no longer that step. */
    data class CompleteStep(override val atMillis: Long, val stepIndex: Int) : WorkoutEvent

    /** Skips [stepIndex]; rejected when the current step is no longer that step. */
    data class SkipStep(override val atMillis: Long, val stepIndex: Int) : WorkoutEvent

    /** Moves to [stepIndex] without recording a result for the current step. */
    data class GoToStep(override val atMillis: Long, val stepIndex: Int) : WorkoutEvent

    data class Abandon(override val atMillis: Long) : WorkoutEvent
}

/** Result of applying an event: the new state, or the unchanged state and why the event was refused. */
sealed interface Transition {
    val state: WorkoutState

    data class Accepted(override val state: WorkoutState) : Transition
    data class Rejected(override val state: WorkoutState, val reason: RejectionReason) : Transition
}

enum class RejectionReason {
    TIME_WENT_BACKWARDS,
    NOT_READY,
    NOT_ACTIVE,
    NOT_RUNNING,
    NOT_PAUSED,
    STALE_STEP,
    INVALID_STEP,
}
