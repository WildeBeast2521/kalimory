package io.github.gonbei774.calisthenicsmemory.ui.screens.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.ui.navigation.DestinationPage
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import io.github.gonbei774.calisthenicsmemory.ui.screens.TodayDashboardCard
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Today (ADR 0004): unfinished workouts first, then what is due today, then today's records. */
@Composable
fun TodayScreen(
    viewModel: TrainingViewModel,
    onResume: (ResumableWorkout) -> Unit,
    onOpenTask: (TodoTask) -> Unit,
    onOpenToDo: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val exercises by viewModel.exercises.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val programs by viewModel.programs.collectAsState()
    val intervalPrograms by viewModel.intervalPrograms.collectAsState()
    val todoTasks by viewModel.todoTasks.collectAsState()
    val records by viewModel.records.collectAsState()
    val today = remember { LocalDate.now() }
    val todayRecords = remember(records, today) { records.filter { it.date == today.toString() } }

    // Read each time Today is shown, so a workout finished or interrupted elsewhere is reflected.
    var resumeSources by remember { mutableStateOf<ResumeSources?>(null) }
    LaunchedEffect(Unit) {
        resumeSources = withContext(Dispatchers.IO) { ResumeSources.load(context) }
    }

    val exerciseNames = remember(exercises) { exercises.associate { it.id to it.name } }
    val groupNames = remember(groups) { groups.associate { it.id to it.name } }
    val programNames = remember(programs) { programs.associate { it.id to it.name } }
    val intervalNames = remember(intervalPrograms) { intervalPrograms.associate { it.id to it.name } }

    val resumable = remember(resumeSources, exerciseNames, programNames, intervalNames) {
        resumeSources?.let { resumableWorkouts(it, exerciseNames.keys, programNames.keys, intervalNames.keys) }.orEmpty()
    }
    val dueTasks = remember(todoTasks, today) { todoTasks.filter { it.isDueOn(today) } }

    val workoutLabel = stringResource(R.string.home_workout)
    val programLabel = stringResource(R.string.today_kind_program)
    val intervalLabel = stringResource(R.string.interval_label)
    val groupLabel = stringResource(R.string.group)
    val savedForLater = stringResource(R.string.today_saved_for_later)

    DestinationPage(
        title = stringResource(PrimaryDestination.TODAY.label),
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
            }
        },
    ) {
        if (resumable.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SectionHeading(
                        stringResource(R.string.today_resume_title),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    resumable.forEach { workout ->
                        val (icon, name, kind) = when (workout) {
                            is ResumableWorkout.Single ->
                                Triple(Icons.Filled.PlayArrow, exerciseNames.getValue(workout.exerciseId), workoutLabel)
                            is ResumableWorkout.Program -> Triple(
                                Icons.AutoMirrored.Filled.List,
                                programNames.getValue(workout.programId),
                                if (workout.savedByUser) "$programLabel · $savedForLater" else programLabel,
                            )
                            is ResumableWorkout.Interval ->
                                Triple(Icons.Filled.Refresh, intervalNames.getValue(workout.programId), intervalLabel)
                        }
                        TodayRow(icon, name, kind, onClick = { onResume(workout) })
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionHeading(stringResource(R.string.today_due_title), modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenToDo) { Text(stringResource(R.string.today_all_todos)) }
        }
        val dueRows = dueTasks.mapNotNull { task ->
            when (task.type) {
                TodoTask.TYPE_EXERCISE -> exerciseNames[task.referenceId]?.let { Triple(Icons.Filled.CheckCircle, it, workoutLabel) }
                TodoTask.TYPE_GROUP -> groupNames[task.referenceId]?.let { Triple(Icons.Filled.Star, it, groupLabel) }
                TodoTask.TYPE_PROGRAM -> programNames[task.referenceId]?.let { Triple(Icons.AutoMirrored.Filled.List, it, programLabel) }
                TodoTask.TYPE_INTERVAL -> intervalNames[task.referenceId]?.let { Triple(Icons.Filled.Refresh, it, intervalLabel) }
                // Tasks whose target is gone or whose type is unknown stay visible on the To Do screen.
                else -> null
            }?.let { task to it }
        }
        if (dueRows.isEmpty()) {
            Text(
                stringResource(R.string.today_due_none),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    dueRows.forEach { (task, row) ->
                        val (icon, name, kind) = row
                        TodayRow(icon, name, kind, onClick = { onOpenTask(task) })
                    }
                }
            }
        }

        TodayDashboardCard(records = todayRecords, exercises = exercises, onNavigateToView = onOpenHistory)
    }
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun TodayRow(icon: ImageVector, name: String, kind: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(kind) },
        leadingContent = { Icon(icon, contentDescription = null) },
        // Follow the enclosing card's content color, so rows read correctly on the resume card too.
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = LocalContentColor.current,
            supportingColor = LocalContentColor.current,
            leadingIconColor = LocalContentColor.current,
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
