package io.github.gonbei774.calisthenicsmemory.workout

import io.github.gonbei774.calisthenicsmemory.workout.RejectionReason.INVALID_STEP
import io.github.gonbei774.calisthenicsmemory.workout.RejectionReason.NOT_ACTIVE
import io.github.gonbei774.calisthenicsmemory.workout.RejectionReason.NOT_PAUSED
import io.github.gonbei774.calisthenicsmemory.workout.RejectionReason.NOT_READY
import io.github.gonbei774.calisthenicsmemory.workout.RejectionReason.NOT_RUNNING
import io.github.gonbei774.calisthenicsmemory.workout.RejectionReason.STALE_STEP
import io.github.gonbei774.calisthenicsmemory.workout.RejectionReason.TIME_WENT_BACKWARDS

/**
 * Pure workout transitions. No clocks, I/O, or Android types: the same state and
 * events always give the same result. A rejected event returns the unchanged state.
 */
object WorkoutReducer {
    fun reduce(state: WorkoutState, event: WorkoutEvent): Transition {
        val now = event.atMillis
        val last = state.lastEventAtMillis
        if (last != null && now < last) return Transition.Rejected(state, TIME_WENT_BACKWARDS)

        if (event is WorkoutEvent.Tick) {
            val ticked = if (state.status == WorkoutStatus.ACTIVE) catchUp(state, now) else state
            return accept(ticked, now)
        }
        if (state.status == WorkoutStatus.READY) {
            return when (event) {
                is WorkoutEvent.Start -> accept(
                    state.copy(status = WorkoutStatus.ACTIVE, timer = StepTimer.Running(now, 0)), now,
                )
                is WorkoutEvent.Abandon -> accept(state.copy(status = WorkoutStatus.ABANDONED), now)
                else -> Transition.Rejected(state, NOT_ACTIVE)
            }
        }
        if (state.status != WorkoutStatus.ACTIVE) return Transition.Rejected(state, NOT_ACTIVE)
        if (event is WorkoutEvent.Start) return Transition.Rejected(state, NOT_READY)

        // Apply any countdown that ended before this event, so the event sees the true current step.
        val current = catchUp(state, now)
        if (current.status != WorkoutStatus.ACTIVE) {
            return if (event is WorkoutEvent.Abandon) accept(current, now) else Transition.Rejected(state, NOT_ACTIVE)
        }
        return when (event) {
            is WorkoutEvent.Pause -> when (current.timer) {
                is StepTimer.Running -> accept(current.copy(timer = StepTimer.Paused(current.elapsedInStep(now))), now)
                else -> Transition.Rejected(state, NOT_RUNNING)
            }
            is WorkoutEvent.Resume -> when (val t = current.timer) {
                is StepTimer.Paused -> accept(current.copy(timer = StepTimer.Running(now, t.accumulatedMillis)), now)
                else -> Transition.Rejected(state, NOT_PAUSED)
            }
            is WorkoutEvent.CompleteStep ->
                if (event.stepIndex != current.stepIndex) Transition.Rejected(state, STALE_STEP)
                else accept(finishStep(current, StepOutcome.COMPLETED, now, now), now)
            is WorkoutEvent.SkipStep ->
                if (event.stepIndex != current.stepIndex) Transition.Rejected(state, STALE_STEP)
                else accept(finishStep(current, StepOutcome.SKIPPED, now, now), now)
            is WorkoutEvent.GoToStep ->
                if (event.stepIndex !in current.steps.indices) Transition.Rejected(state, INVALID_STEP)
                else accept(current.copy(stepIndex = event.stepIndex, timer = freshTimer(current.timer, now)), now)
            is WorkoutEvent.Abandon -> accept(current.copy(status = WorkoutStatus.ABANDONED), now)
            is WorkoutEvent.Start, is WorkoutEvent.Tick -> error("handled above")
        }
    }

    private fun accept(state: WorkoutState, now: Long) = Transition.Accepted(state.copy(lastEventAtMillis = now))

    /** Completes every running countdown whose deadline is at or before [now], each at its exact deadline. */
    private fun catchUp(state: WorkoutState, now: Long): WorkoutState {
        var current = state
        while (current.status == WorkoutStatus.ACTIVE) {
            val kind = current.currentStep.kind as? StepKind.Countdown ?: break
            val timer = current.timer as? StepTimer.Running ?: break
            val deadline = timer.sinceMillis + (kind.durationMillis - timer.accumulatedMillis)
            if (deadline > now) break
            current = finishStep(current, StepOutcome.COMPLETED, endedAt = deadline, nextStartsAt = deadline)
        }
        return current
    }

    /** Records the current step and moves on; the next step keeps the running or paused state. */
    private fun finishStep(state: WorkoutState, outcome: StepOutcome, endedAt: Long, nextStartsAt: Long): WorkoutState {
        val elapsed = when (val kind = state.currentStep.kind) {
            is StepKind.Countdown -> state.elapsedInStep(endedAt).coerceAtMost(kind.durationMillis)
            StepKind.Stopwatch -> state.elapsedInStep(endedAt)
        }
        val results = state.results + StepResult(state.stepIndex, outcome, elapsed)
        val next = state.stepIndex + 1
        return if (next > state.steps.lastIndex) {
            state.copy(status = WorkoutStatus.FINISHED, timer = StepTimer.NotStarted, results = results)
        } else {
            state.copy(stepIndex = next, timer = freshTimer(state.timer, nextStartsAt), results = results)
        }
    }

    private fun freshTimer(previous: StepTimer, startsAt: Long): StepTimer =
        if (previous is StepTimer.Paused) StepTimer.Paused(0) else StepTimer.Running(startsAt, 0)
}
