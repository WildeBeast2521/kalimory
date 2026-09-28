package io.github.gonbei774.calisthenicsmemory.ui.screens.today

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import io.github.gonbei774.calisthenicsmemory.ui.screens.formatRecordsForClipboard
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields

/**
 * Today (ADR 0004): the week at a glance, then the one thing to do next, then what else is
 * waiting, then what was done today. Only the next thing is loud; the rest stays quiet.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodayScreen(
    viewModel: TrainingViewModel,
    onResume: (ResumableWorkout) -> Unit,
    onOpenTask: (TodoTask) -> Unit,
    onOpenToDo: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrain: () -> Unit = {},
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val exercises by viewModel.exercises.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val programs by viewModel.programs.collectAsState()
    val intervalPrograms by viewModel.intervalPrograms.collectAsState()
    val todoTasks by viewModel.todoTasks.collectAsState()
    val history by viewModel.history.collectAsState()
    val intervalHistory by viewModel.intervalHistory.collectAsState()
    val today = remember { LocalDate.now() }

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

    val week = remember(today, locale) { weekOf(today, WeekFields.of(locale).firstDayOfWeek) }
    val trainedDays = remember(history, intervalHistory) {
        (history.map { it.date } + intervalHistory.map { it.record.date })
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
    }
    val doneToday = remember(history, today) {
        history.filter { it.date == today.toString() }
            .groupBy { it.exerciseId }
            .map { (exerciseId, sets) -> exerciseId to sets.sortedWith(compareBy({ it.time }, { it.setNumber })) }
            // In the order they were done.
            .sortedBy { (_, sets) -> sets.first().time }
    }

    val workoutLabel = stringResource(R.string.home_workout)
    val programLabel = stringResource(R.string.today_kind_program)
    val intervalLabel = stringResource(R.string.interval_label)
    val groupLabel = stringResource(R.string.group)
    val savedForLater = stringResource(R.string.today_saved_for_later)

    data class Item(val icon: ImageVector, val name: String, val kind: String, val open: () -> Unit)

    val resumeItems = resumable.map { workout ->
        when (workout) {
            is ResumableWorkout.Single ->
                Item(Icons.Filled.PlayArrow, exerciseNames.getValue(workout.exerciseId), workoutLabel) { onResume(workout) }
            is ResumableWorkout.Program -> Item(
                Icons.AutoMirrored.Filled.List,
                programNames.getValue(workout.programId),
                if (workout.savedByUser) "$programLabel · $savedForLater" else programLabel,
            ) { onResume(workout) }
            is ResumableWorkout.Interval ->
                Item(Icons.Filled.Refresh, intervalNames.getValue(workout.programId), intervalLabel) { onResume(workout) }
        }
    }
    val dueItems = dueTasks.mapNotNull { task ->
        val (icon, name, kind) = when (task.type) {
            TodoTask.TYPE_EXERCISE -> Triple(Icons.Filled.CheckCircle, exerciseNames[task.referenceId], workoutLabel)
            TodoTask.TYPE_GROUP -> Triple(Icons.Filled.Star, groupNames[task.referenceId], groupLabel)
            TodoTask.TYPE_PROGRAM -> Triple(Icons.AutoMirrored.Filled.List, programNames[task.referenceId], programLabel)
            TodoTask.TYPE_INTERVAL -> Triple(Icons.Filled.Refresh, intervalNames[task.referenceId], intervalLabel)
            // Tasks whose target is gone or whose type is unknown stay visible on the To Do screen.
            else -> return@mapNotNull null
        }
        name?.let { Item(icon, it, kind) { onOpenTask(task) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        // Date and title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(PrimaryDestination.TODAY.label),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val trainedThisWeek = week.count { it in trainedDays }
        val weekSummary = stringResource(R.string.today_week_summary, trainedThisWeek, week.size)
        Column {
            WeekStrip(week, trainedDays, today, locale, weekSummary)
            Spacer(Modifier.height(Spacing.s))
            Text(weekSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // The one thing to do next.
        val heroFromDue = resumeItems.isEmpty() && dueItems.isNotEmpty()
        when {
            resumeItems.isNotEmpty() -> resumeItems.first().let {
                HeroCard(stringResource(R.string.today_resume_title), it.name, it.kind, stringResource(R.string.resume_button), it.icon, emphasised = true, onClick = it.open)
            }
            heroFromDue -> dueItems.first().let {
                HeroCard(stringResource(R.string.today_next_up), it.name, it.kind, stringResource(R.string.today_start), it.icon, emphasised = true, onClick = it.open)
            }
            else -> HeroCard(
                stringResource(R.string.today_nothing_planned), stringResource(R.string.today_start_workout), stringResource(R.string.today_start_workout_detail),
                stringResource(R.string.today_start), Icons.Filled.PlayArrow, emphasised = false, onClick = onOpenTrain,
            )
        }
        resumeItems.drop(1).forEach { TodayRow(it.icon, it.name, it.kind, it.open) }

        // What else is due; the hero already shows the first when nothing waits to be resumed.
        val otherDue = if (heroFromDue) dueItems.drop(1) else dueItems
        if (otherDue.isNotEmpty() || !heroFromDue) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeading(stringResource(R.string.today_due_title), Modifier.weight(1f))
                TextButton(onClick = onOpenToDo) { Text(stringResource(R.string.today_all_todos)) }
            }
            if (otherDue.isEmpty()) {
                Text(stringResource(R.string.today_due_none), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column { otherDue.forEach { TodayRow(it.icon, it.name, it.kind, it.open) } }
            }
        }

        // What was done today, as chalk tallies.
        if (doneToday.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeading(stringResource(R.string.today_done_title), Modifier.weight(1f))
                TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.today_see_progress)) }
            }
            // Long-press copies a plain-text summary, as the old dashboard card did.
            val clipboard = LocalClipboardManager.current
            val summary = remember(doneToday, exercises) { formatRecordsForClipboard(doneToday.flatMap { it.second }, exercises) }
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .combinedClickable(onClick = onOpenHistory, onLongClick = {
                        clipboard.setText(AnnotatedString(summary))
                        viewModel.showSnackbar(UiMessage.CopiedToClipboard)
                    }),
            ) {
                Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                    doneToday.forEach { (exerciseId, sets) ->
                        val name = exerciseNames[exerciseId] ?: return@forEach
                        val values = sets.map { set -> set.valueLeft?.let { "R${set.valueRight} L$it" } ?: set.valueRight.toString() }
                        DoneRow(name, values)
                    }
                }
            }
        }
    }
}

@Composable
private fun DoneRow(name: String, values: List<String>) {
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "$name: ${values.joinToString(", ")}" }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TallyMarks(values.size, MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(Spacing.s))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            values.forEach { value ->
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        value,
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
                    )
                }
            }
        }
    }
}
