package io.github.gonbei774.calisthenicsmemory.ui.screens.train

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.util.ProgramTimeEstimator
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

/** How many exercises a program has and roughly how long it takes, in whole minutes rounded up. */
data class ProgramSummary(val exerciseCount: Int, val minutes: Int)

/** Summaries for [programs], read in the background and updated when programs or exercises change. */
@Composable
fun rememberProgramSummaries(
    viewModel: TrainingViewModel,
    programs: List<Program>,
    exercises: List<Exercise>,
): Map<Long, ProgramSummary> {
    var summaries by remember { mutableStateOf(emptyMap<Long, ProgramSummary>()) }
    LaunchedEffect(programs, exercises) {
        val byId = exercises.associateBy { it.id }
        summaries = programs.associate { program ->
            val pes = viewModel.getProgramExercisesSync(program.id)
            val loops = viewModel.getProgramLoopsSync(program.id)
            val used = pes.mapNotNull { pe -> byId[pe.exerciseId]?.let { pe.exerciseId to it } }.toMap()
            val seconds = ProgramTimeEstimator.estimateSeconds(pes, loops, used, startCountdownSeconds = 0)
            program.id to ProgramSummary(pes.size, (seconds + 59) / 60)
        }
    }
    return summaries
}

/** "3 exercise(s) · ~13 min", or that the program is still empty. Blank until the summary is read. */
@Composable
fun ProgramSummary?.describe(): String = when {
    this == null -> ""
    exerciseCount == 0 -> stringResource(R.string.program_no_exercises)
    else -> stringResource(R.string.program_exercise_count, exerciseCount) + " · " +
        stringResource(R.string.program_estimated_time, minutes)
}

/** "2 exercises · 20s/10s · 8 rounds · ~7 min" for an interval routine with [exerciseCount] exercises. */
@Composable
fun intervalSummary(interval: IntervalProgram, exerciseCount: Int): String {
    val summary = stringResource(
        R.string.interval_summary_format, exerciseCount, interval.workSeconds, interval.restSeconds, interval.rounds,
    )
    val seconds = ProgramTimeEstimator.estimateIntervalSeconds(interval, exerciseCount)
    return if (seconds > 0) summary + " · " + stringResource(R.string.program_estimated_time, (seconds + 59) / 60) else summary
}
