package io.github.gonbei774.calisthenicsmemory.ui.screens.train

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.HeroCard
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.RowGroup
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.Section
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.SectionHeading
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.TodayRow
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

/**
 * Train: start anything from one place. A quick single-exercise workout leads; your programs
 * and interval routines follow and start directly; logging a past workout comes last.
 */
@Composable
fun TrainScreen(
    viewModel: TrainingViewModel,
    onStartWorkout: () -> Unit,
    onRecordManually: () -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenIntervals: () -> Unit,
    onStartProgram: (Long) -> Unit,
    onEditProgram: (Long) -> Unit,
    onStartInterval: (Long) -> Unit,
) {
    val exercises by viewModel.exercises.collectAsState()
    val programs by viewModel.programs.collectAsState()
    val intervalPrograms by viewModel.intervalPrograms.collectAsState()

    val programSummaries = rememberProgramSummaries(viewModel, programs, exercises)
    val intervalCounts = remember { mutableStateMapOf<Long, Int>() }
    LaunchedEffect(intervalPrograms) {
        intervalPrograms.forEach { intervalCounts[it.id] = viewModel.getIntervalProgramExercisesSync(it.id).size }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Text(
            stringResource(PrimaryDestination.TRAIN.label),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )

        HeroCard(
            label = stringResource(R.string.train_quick_label),
            title = stringResource(R.string.home_workout),
            detail = stringResource(R.string.single_mode_description),
            action = stringResource(R.string.today_start),
            icon = AppIcons.Workout,
            emphasised = true,
            onClick = onStartWorkout,
        )

        Section(stringResource(R.string.program_list_title), onOpenPrograms) {
            if (programs.isEmpty()) {
                EmptyLine(stringResource(R.string.train_no_programs))
            } else {
                RowGroup {
                    programs.forEach { program ->
                        // An empty program has nothing to start, so it opens for editing.
                        val empty = programSummaries[program.id]?.exerciseCount == 0
                        TodayRow(
                            AppIcons.Program,
                            program.name,
                            programSummaries[program.id].describe(),
                            trailing = if (empty) AppIcons.Forward else AppIcons.Play,
                        ) { if (empty) onEditProgram(program.id) else onStartProgram(program.id) }
                    }
                }
            }
        }

        Section(stringResource(R.string.interval_list_title), onOpenIntervals) {
            if (intervalPrograms.isEmpty()) {
                EmptyLine(stringResource(R.string.train_no_intervals))
            } else {
                RowGroup {
                    intervalPrograms.forEach { interval ->
                        TodayRow(
                            AppIcons.Interval,
                            interval.name,
                            stringResource(
                                R.string.interval_summary_format,
                                intervalCounts[interval.id] ?: 0, interval.workSeconds, interval.restSeconds, interval.rounds,
                            ),
                            trailing = AppIcons.Play,
                        ) { onStartInterval(interval.id) }
                    }
                }
            }
        }

        Section(stringResource(R.string.train_log_title), onOpenAll = null) {
            RowGroup {
                TodayRow(
                    AppIcons.RecordManually,
                    stringResource(R.string.home_record),
                    stringResource(R.string.train_log_detail),
                    trailing = AppIcons.Forward,
                    onClick = onRecordManually,
                )
            }
        }
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
