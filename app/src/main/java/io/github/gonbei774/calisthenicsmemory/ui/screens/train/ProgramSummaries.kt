package io.github.gonbei774.calisthenicsmemory.ui.screens.train

import io.github.gonbei774.calisthenicsmemory.util.AppClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.text.format.DateUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.ZoneId
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.util.ProgramTimeEstimator
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

/** How many exercises a program has and roughly how long its next run takes, in whole minutes. */
data class ProgramSummary(val exerciseCount: Int, val minutes: Int)

/** Summaries for [programs], read in the background and updated when programs or exercises change. */
@Composable
fun rememberProgramSummaries(
    viewModel: TrainingViewModel,
    programs: List<Program>,
    exercises: List<Exercise>,
): Map<Long, ProgramSummary> {
    var summaries by remember { mutableStateOf(emptyMap<Long, ProgramSummary>()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    // History feeds the next run's values, so a finished workout updates the estimate.
    val history by viewModel.history.collectAsState()
    LaunchedEffect(programs, exercises, history) {
        val preferences = io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences(context)
        val countdown = ProgramTimeEstimator.startCountdownSeconds(preferences)
        val prefill = preferences.isPrefillPreviousRecordEnabled()
        val byId = exercises.associateBy { it.id }
        summaries = programs.associate { program ->
            val pes = viewModel.getProgramExercisesSync(program.id)
            val loops = viewModel.getProgramLoopsSync(program.id)
            // The same sets and values the program's start screen will open with.
            val seconds = ProgramTimeEstimator.estimateNextRunSeconds(pes, loops, byId, countdown, prefill, viewModel::getLatestSession)
            program.id to ProgramSummary(pes.size, ProgramTimeEstimator.formatMinutes(seconds))
        }
    }
    return summaries
}

/** "3 exercise(s) · ~13 min", or that the program is still empty. Blank until the summary is read. */
@Composable
fun ProgramSummary?.describe(): String = when {
    this == null -> ""
    exerciseCount == 0 -> stringResource(R.string.program_no_exercises)
    else -> pluralStringResource(R.plurals.exercise_count, exerciseCount, exerciseCount) + " · " +
        stringResource(R.string.program_estimated_time, minutes)
}

/** "2 exercises · 20s/10s · 8 rounds · ~7 min" for an interval routine with [exerciseCount] exercises. */
@Composable
fun intervalSummary(interval: IntervalProgram, exerciseCount: Int): String {
    val summary = pluralStringResource(R.plurals.exercise_count, exerciseCount, exerciseCount) + " · " + stringResource(
        R.string.interval_summary_format, interval.workSeconds, interval.restSeconds, interval.rounds,
    )
    val seconds = ProgramTimeEstimator.estimateIntervalSeconds(interval, exerciseCount)
    // Rounded like program estimates, so both kinds of list read the same way.
    return if (seconds > 0) summary + " · " + stringResource(R.string.program_estimated_time, ProgramTimeEstimator.formatMinutes(seconds)) else summary
}

/** "Last done Sep 27", or "Done today"; null when never done. */
@Composable
fun lastDoneText(date: LocalDate?): String? {
    date ?: return null
    val today = AppClock.today()
    if (date == today) return stringResource(R.string.last_done_today)
    // Short and localised; the year appears only when it is not this year.
    val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val formatted = DateUtils.formatDateTime(LocalContext.current, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
    return stringResource(R.string.last_done, formatted)
}
