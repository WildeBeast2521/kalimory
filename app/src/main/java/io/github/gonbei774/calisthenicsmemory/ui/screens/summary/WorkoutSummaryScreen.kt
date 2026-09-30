package io.github.gonbei774.calisthenicsmemory.ui.screens.summary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseResult
import io.github.gonbei774.calisthenicsmemory.data.v2.PersonalBest
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSourceType
import io.github.gonbei774.calisthenicsmemory.data.v2.WorkoutSummary
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.WorkoutPrimaryButton
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.WeekStrip
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.weekOf
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.theme.firstDayOfWeek
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import java.time.LocalDate
import kotlin.math.roundToInt

private val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val Overshoot = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 * The one celebration in the app (M2): after a live workout, each exercise's ring closes in
 * turn, the totals count up, today fills in on the week, and a new personal best lands in
 * brass. Every step is a timed animation, so "Remove animations" shows it complete at once.
 */
@Composable
fun WorkoutSummaryScreen(viewModel: TrainingViewModel, sessionId: Long, onDone: () -> Unit) {
    var summary by remember(sessionId) { mutableStateOf<WorkoutSummary?>(null) }
    LaunchedEffect(sessionId) { summary = viewModel.loadWorkoutSummary(sessionId) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.l, vertical = Spacing.l),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            summary?.let { SummaryContent(viewModel, it) }
        }
        Spacer(Modifier.height(Spacing.l))
        WorkoutPrimaryButton(stringResource(R.string.summary_done), onDone)
    }
}

@Composable
private fun SummaryContent(viewModel: TrainingViewModel, summary: WorkoutSummary) {
    val title = remember { Animatable(0f) }
    val counts = remember { Animatable(0f) }
    val todayFill = remember { Animatable(0f) }
    val best = remember { Animatable(0f) }
    LaunchedEffect(Unit) { title.animateTo(1f, tween(400, easing = Decelerate)) }
    LaunchedEffect(Unit) { counts.animateTo(1f, tween(900, delayMillis = 150, easing = Decelerate)) }
    LaunchedEffect(Unit) { todayFill.animateTo(1f, tween(420, delayMillis = 250 + summary.exercises.size * 140 + 400, easing = Overshoot)) }
    LaunchedEffect(Unit) { best.animateTo(1f, tween(520, delayMillis = 350 + summary.exercises.size * 140 + 600, easing = Overshoot)) }

    // Heading: what was done, then the one line that matters.
    Column(Modifier.graphicsLayer { alpha = title.value; translationY = (1f - title.value) * 24.dp.toPx() }) {
        val source = summary.title ?: summary.exercises.singleOrNull()?.name
        if (source != null) {
            Text(source, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            stringResource(R.string.workout_summary_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
    }

    Totals(summary, counts.value)

    summary.personalBests.forEach { BestChip(it, best.value) }

    if (summary.exercises.isNotEmpty()) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(vertical = Spacing.s)) {
                summary.exercises.forEachIndexed { index, exercise ->
                    ExerciseRow(exercise, interval = summary.sourceType == WorkoutSourceType.INTERVAL_TEMPLATE, delayMillis = 250 + index * 140)
                }
            }
        }
    }

    // The week, with today filling in as the last beat.
    val history by viewModel.history.collectAsState()
    val intervalHistory by viewModel.intervalHistory.collectAsState()
    val today = remember { LocalDate.now() }
    val week = weekOf(today, firstDayOfWeek())
    val trained = remember(history, intervalHistory) {
        (history.map { it.date } + intervalHistory.map { it.record.date })
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet() + today
    }
    val weekSummary = stringResource(R.string.today_week_summary, week.count { it in trained }, week.size)
    Column {
        WeekStrip(week, trained, today, LocalConfiguration.current.locales[0], weekSummary, todayFill = todayFill.value)
        Spacer(Modifier.height(Spacing.s))
        Text(weekSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Totals(summary: WorkoutSummary, progress: Float) {
    val secondsUnit = stringResource(R.string.unit_seconds)
    data class Tile(val label: String, val value: Int, val format: (Int) -> String)
    val tiles = buildList {
        add(Tile(stringResource(R.string.summary_sets), summary.completedSets) { "$it" })
        if (summary.totalRepetitions > 0) add(Tile(stringResource(R.string.summary_reps), summary.totalRepetitions) { "$it" })
        if (summary.totalHoldSeconds > 0) {
            val format = stringResource(R.string.value_with_unit)
            add(Tile(stringResource(R.string.summary_hold), summary.totalHoldSeconds) { String.format(format, it, secondsUnit) })
        }
        summary.durationMinutes?.let {
            val format = stringResource(R.string.stats_total_minutes_format)
            add(Tile(stringResource(R.string.summary_time), it) { value -> String.format(format, value) })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        tiles.forEach { tile ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = "${tile.label}: ${tile.format(tile.value)}" },
            ) {
                Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.m)) {
                    Text(tile.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        tile.format((tile.value * progress).roundToInt()),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun BestChip(best: PersonalBest, progress: Float) {
    val unit = stringResource(if (best.kind == ExerciseKind.DYNAMIC) R.string.unit_reps else R.string.unit_seconds)
    val value = stringResource(R.string.value_with_unit, best.value, unit)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.graphicsLayer {
            alpha = progress.coerceIn(0f, 1f)
            scaleX = 0.9f + 0.1f * progress; scaleY = scaleX
            translationY = (1f - progress) * 12.dp.toPx()
        },
    ) {
        Text(
            stringResource(R.string.summary_new_best, best.exerciseName, value),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
        )
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseResult, interval: Boolean, delayMillis: Int) {
    val done = if (exercise.plannedSets > 0) exercise.sets.size.toFloat() / exercise.plannedSets else 1f
    val ring = remember { Animatable(0f) }
    LaunchedEffect(Unit) { ring.animateTo(done, tween(520, delayMillis = delayMillis, easing = Decelerate)) }

    val unit = stringResource(if (exercise.kind == ExerciseKind.DYNAMIC) R.string.unit_reps else R.string.unit_seconds)
    val setsLine = when {
        interval -> pluralStringResource(R.plurals.set_count, exercise.sets.size, exercise.sets.size)
        exercise.sets.size > 1 && exercise.sets.all { it == exercise.sets.first() && it.left == null } ->
            "${exercise.sets.size} × ${stringResource(R.string.value_with_unit, exercise.sets.first().right, unit)}"
        else -> exercise.sets.joinToString(", ") { set -> set.left?.let { "${set.right}/$it" } ?: "${set.right}" } + " " + unit
    }
    val lastTimeLine = exercise.lastTimeTotal?.takeIf { !interval }?.let { total ->
        stringResource(R.string.summary_last_time, stringResource(R.string.value_with_unit, total, unit))
    }

    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val fill = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Canvas(Modifier.size(36.dp)) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(fill, -90f, 360f * ring.value, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(setsLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (lastTimeLine != null) {
                Text(lastTimeLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
