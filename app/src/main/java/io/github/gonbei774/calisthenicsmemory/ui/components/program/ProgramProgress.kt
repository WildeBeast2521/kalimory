package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.setLabel

/** Where a program set is, as its notification shows it: the set of its exercise, and the round of a loop. */
@Composable
internal fun programSetDetail(session: ProgramExecutionSession, setIndex: Int): String {
    val set = session.sets.getOrNull(setIndex) ?: return ""
    val exerciseSets = session.sets
        .filter { it.exerciseIndex == set.exerciseIndex && it.roundNumber == set.roundNumber }
        .map { it.setNumber }
        .distinct()
    val label = setLabel(set.setNumber, exerciseSets.size, set.side)
    return if (set.loopId != null && set.totalRounds > 1) {
        "$label · " + stringResource(R.string.loop_round_current, set.roundNumber, set.totalRounds)
    } else label
}

/** The name of the exercise a program set belongs to. */
internal fun ProgramExecutionSession.exerciseNameOf(setIndex: Int): String =
    sets.getOrNull(setIndex)?.let { exercises.getOrNull(it.exerciseIndex)?.second?.name }.orEmpty()

/** Sets finished or skipped so far, for the notification's progress bar. */
internal fun ProgramExecutionSession.setsDone(): Int = sets.count { it.isCompleted || it.isSkipped }
