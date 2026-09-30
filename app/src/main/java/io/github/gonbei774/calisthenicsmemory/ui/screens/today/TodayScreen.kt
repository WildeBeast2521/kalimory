package io.github.gonbei774.calisthenicsmemory.ui.screens.today

import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
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
import io.github.gonbei774.calisthenicsmemory.ui.screens.train.describe
import io.github.gonbei774.calisthenicsmemory.ui.screens.train.rememberProgramSummaries
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.screens.train.lastDoneText
import io.github.gonbei774.calisthenicsmemory.ui.theme.WorkoutNumerals
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.RollingNumber
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.ui.theme.firstDayOfWeek
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
    onOpenDay: (LocalDate) -> Unit = {},
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

    val firstDay = firstDayOfWeek()
    val week = remember(today, firstDay) { weekOf(today, firstDay) }
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

    val programSummaries = rememberProgramSummaries(viewModel, programs, exercises)
    val exercisesById = remember(exercises) { exercises.associateBy { it.id } }
    val workoutLabel = stringResource(R.string.home_workout)
    val repsUnit = stringResource(R.string.unit_reps)
    val secondsUnit = stringResource(R.string.unit_seconds)
    // What a due exercise asks for (3 sets × 12 reps), when it has a target.
    @Composable
    fun exerciseDetail(exerciseId: Long): String {
        val exercise = exercisesById[exerciseId] ?: return workoutLabel
        val sets = exercise.targetSets ?: return workoutLabel
        val value = exercise.targetValue ?: return workoutLabel
        val unit = if (exercise.type == "Dynamic") repsUnit else secondsUnit
        val format = if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format
        return stringResource(format, sets, value, unit)
    }
    val programLabel = stringResource(R.string.today_kind_program)
    val intervalLabel = stringResource(R.string.interval_label)
    val groupLabel = stringResource(R.string.group)
    val savedForLater = stringResource(R.string.today_saved_for_later)
    // How big a program is (3 exercise(s) · ~13 min); "Program" until that is known.
    @Composable
    fun programDetail(programId: Long): String = programSummaries[programId].describe().ifEmpty { programLabel }

    val lastDone by viewModel.lastDone.collectAsState()
    data class Item(val icon: ImageVector, val name: String, val kind: String, val note: String? = null, val open: () -> Unit)

    val resumeItems = resumable.map { workout ->
        when (workout) {
            is ResumableWorkout.Single ->
                Item(AppIcons.Workout, exerciseNames.getValue(workout.exerciseId), workoutLabel) { onResume(workout) }
            is ResumableWorkout.Program -> Item(
                AppIcons.Program,
                programNames.getValue(workout.programId),
                if (workout.savedByUser) "$programLabel · $savedForLater" else programLabel,
            ) { onResume(workout) }
            is ResumableWorkout.Interval ->
                Item(AppIcons.Interval, intervalNames.getValue(workout.programId), intervalLabel) { onResume(workout) }
        }
    }
    val dueItems = dueTasks.mapNotNull { task ->
        val (icon, name, kind) = when (task.type) {
            TodoTask.TYPE_EXERCISE -> Triple(AppIcons.Done, exerciseNames[task.referenceId], exerciseDetail(task.referenceId))
            TodoTask.TYPE_GROUP -> Triple(AppIcons.FavoriteFilled, groupNames[task.referenceId], groupLabel)
            TodoTask.TYPE_PROGRAM -> Triple(AppIcons.Program, programNames[task.referenceId], programDetail(task.referenceId))
            TodoTask.TYPE_INTERVAL -> Triple(AppIcons.Interval, intervalNames[task.referenceId], intervalLabel)
            // Tasks whose target is gone or whose type is unknown stay visible on the To Do screen.
            else -> return@mapNotNull null
        }
        // What is due is easier to judge knowing when it was last done.
        val note = when (task.type) {
            TodoTask.TYPE_EXERCISE -> lastDoneText(lastDone.exercises[task.referenceId])
            TodoTask.TYPE_PROGRAM -> lastDoneText(lastDone.programs[task.referenceId])
            TodoTask.TYPE_INTERVAL -> lastDoneText(lastDone.intervals[task.referenceId])
            else -> null
        }
        name?.let { Item(icon, it, kind, note) { onOpenTask(task) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.l, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
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
                Icon(AppIcons.Settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        val trainedThisWeek = week.count { it in trainedDays }
        val weekSummary = stringResource(R.string.today_week_summary, trainedThisWeek, week.size)
        Column {
            // The week's work as one big number: the anchor of the page.
            val weekDates = remember(week) { week.map { it.toString() }.toSet() }
            val setsThisWeek = remember(history, weekDates) { history.count { it.date in weekDates } }
            val setsLabel = pluralStringResource(R.plurals.today_week_sets, setsThisWeek)
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "$setsThisWeek $setsLabel" },
            ) {
                // Proportional figures: tabular ones leave a gap inside "11" at this size.
                RollingNumber("$setsThisWeek", style = WorkoutNumerals.copy(fontSize = 64.sp, lineHeight = 68.sp, fontFeatureSettings = "pnum"), color = MaterialTheme.colorScheme.onBackground)
                Text(
                    setsLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.s, bottom = Spacing.m),
                )
            }
            Spacer(Modifier.height(Spacing.m))
            WeekStrip(week, trainedDays, today, locale, weekSummary, onDayClick = onOpenDay)
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
                stringResource(R.string.today_start), AppIcons.Workout, emphasised = false, onClick = onOpenTrain,
            )
        }
        if (resumeItems.size > 1) {
            RowGroup { resumeItems.drop(1).forEach { TodayRow(it.icon, it.name, it.kind, trailing = AppIcons.Play, onClick = it.open) } }
        }

        // What else is due; the hero already shows the first when nothing waits to be resumed.
        val otherDue = if (heroFromDue) dueItems.drop(1) else dueItems
        if (otherDue.isNotEmpty() || !heroFromDue) {
            Section(stringResource(R.string.today_due_title), onOpenToDo, stringResource(R.string.today_all_todos)) {
                if (otherDue.isEmpty()) {
                    Text(stringResource(R.string.today_due_none), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    RowGroup { otherDue.forEach { TodayRow(it.icon, it.name, it.kind, trailing = AppIcons.Play, note = it.note, onClick = it.open) } }
                }
            }
        }

        // What was done today, as chalk tallies.
        if (doneToday.isNotEmpty()) Section(stringResource(R.string.today_done_title), onOpenHistory, stringResource(R.string.today_see_progress)) {
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
                    // Today's totals first: sets, then reps and time held where there are any.
                    val todaySets = doneToday.flatMap { it.second }
                    val reps = todaySets.filter { exercisesById[it.exerciseId]?.type == "Dynamic" }.sumOf { it.valueRight + (it.valueLeft ?: 0) }
                    val held = todaySets.filter { exercisesById[it.exerciseId]?.type == "Isometric" }.sumOf { it.valueRight + (it.valueLeft ?: 0) }
                    val totals = buildList {
                        add(pluralStringResource(R.plurals.set_count, todaySets.size, todaySets.size))
                        if (reps > 0) add(stringResource(R.string.value_with_unit, reps, repsUnit))
                        if (held > 0) add(stringResource(R.string.value_with_unit, held, secondsUnit))
                    }
                    Text(totals.joinToString(", "), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
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
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
                    )
                }
            }
        }
    }
}
