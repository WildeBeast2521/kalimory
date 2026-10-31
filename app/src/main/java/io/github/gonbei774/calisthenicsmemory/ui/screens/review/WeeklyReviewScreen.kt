package io.github.gonbei774.calisthenicsmemory.ui.screens.review

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.review.MovementFamily
import io.github.gonbei774.calisthenicsmemory.data.review.WeekReview
import io.github.gonbei774.calisthenicsmemory.data.review.WeeklyReview
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmTopBar
import io.github.gonbei774.calisthenicsmemory.ui.components.common.isLargeFontScale
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.RollingNumber
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.RowGroup
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.SectionHeading
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.weekOf
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.theme.WorkoutNumerals
import io.github.gonbei774.calisthenicsmemory.ui.theme.firstDayOfWeek
import io.github.gonbei774.calisthenicsmemory.util.rememberToday
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The weekly review (ADR 0010): one week's days and sets by movement, from history only. Opens
 * on the current week; previous and next move a week at a time, back to the first trained week.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewScreen(viewModel: TrainingViewModel, onNavigateBack: () -> Unit) {
    val history by viewModel.history.collectAsState()
    val intervalHistory by viewModel.intervalHistory.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val goal by viewModel.weeklyGoal.collectAsState()
    val today = rememberToday()
    val firstDay = firstDayOfWeek()
    val locale = LocalConfiguration.current.locales[0]

    // A day inside the shown week, kept across rotation; null follows today.
    var anchor by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = anchor?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    val currentStart = weekOf(today, firstDay).first()
    val weekStart = weekOf(shown, firstDay).first().coerceAtMost(currentStart)

    val review = remember(weekStart, history, intervalHistory, exercises) {
        WeeklyReview.build(weekStart, history, intervalHistory, exercises)
    }
    val firstTrained = remember(history, intervalHistory) { WeeklyReview.firstTrainedDay(history, intervalHistory) }
    val dates = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val heading = when (weekStart) {
        currentStart -> stringResource(R.string.weekly_review_this_week)
        currentStart.minusWeeks(1) -> stringResource(R.string.weekly_review_last_week)
        else -> null
    }

    val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
        topBar = { CalmTopBar(title = stringResource(R.string.weekly_review_title), onBack = onNavigateBack, scrollBehavior = topBarScroll) },
    ) { padding ->
        WeeklyReviewContent(
            review = review,
            heading = heading,
            range = stringResource(R.string.weekly_review_range, weekStart.format(dates), weekStart.plusDays(6).format(dates)),
            goal = goal,
            canGoBack = WeeklyReview.canGoBack(weekStart, firstTrained),
            canGoForward = WeeklyReview.canGoForward(weekStart, currentStart),
            onPrevious = { anchor = weekStart.minusWeeks(1).toString() },
            onNext = { anchor = weekStart.plusWeeks(1).toString() },
            modifier = Modifier.padding(padding),
        )
    }
}

/** The review of one week, without state: shown by the screen and by its tests. */
@Composable
internal fun WeeklyReviewContent(
    review: WeekReview,
    /** "This week", "Last week", or null to show only the range. */
    heading: String?,
    /** The localized first and last day. */
    range: String,
    goal: Int?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A surface, not a plain background, so the buttons get the matching content colour.
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            WeekHeader(heading, range, canGoBack, canGoForward, onPrevious, onNext)
            if (review.isEmpty) {
                Text(
                    stringResource(R.string.weekly_review_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Totals(review, goal)
                SetsByMovement(review)
            }
        }
    }
}

@Composable
private fun WeekHeader(
    heading: String?,
    range: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().semantics { isTraversalGroup = true }, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, enabled = canGoBack) {
            Icon(AppIcons.Previous, contentDescription = stringResource(R.string.weekly_review_previous))
        }
        // The week is read before the buttons around it.
        Column(Modifier.weight(1f).semantics { traversalIndex = -1f }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                heading ?: range,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (heading != null) {
                Text(range, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(AppIcons.Forward, contentDescription = stringResource(R.string.weekly_review_next))
        }
    }
}

@Composable
private fun Totals(review: WeekReview, goal: Int?) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        val setsLabel = pluralStringResource(R.plurals.weekly_review_sets, review.totalSets)
        // The same number Today shows for the current week.
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.clearAndSetSemantics { contentDescription = "${review.totalSets} $setsLabel" },
        ) {
            RollingNumber("${review.totalSets}", style = WorkoutNumerals.copy(fontSize = 64.sp, lineHeight = 68.sp), color = MaterialTheme.colorScheme.onBackground)
            Text(
                setsLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.s, bottom = Spacing.m),
            )
        }
        Text(
            if (goal == null) pluralStringResource(R.plurals.weekly_review_days, review.daysTrained, review.daysTrained)
            else pluralStringResource(R.plurals.weekly_review_days_goal, goal, review.daysTrained, goal),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetsByMovement(review: WeekReview) {
    // One scale for every row, so bars compare; the mark at 10 sits halfway on a quiet week.
    val scale = maxOf(BAR_MIN_SCALE, review.families.maxOfOrNull { it.sets } ?: 0, review.otherSets, review.intervalSets)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionHeading(stringResource(R.string.weekly_review_by_movement))
        RowGroup {
            review.families.forEach { (family, sets) ->
                SetsRow(stringResource(family.label()), detail = null, sets, showMark = family.hasReference, scale)
            }
            if (review.otherSets > 0) {
                SetsRow(stringResource(R.string.weekly_review_other), stringResource(R.string.weekly_review_other_detail), review.otherSets, showMark = false, scale)
            }
            if (review.intervalSets > 0) {
                SetsRow(stringResource(R.string.weekly_review_intervals), stringResource(R.string.weekly_review_intervals_detail), review.intervalSets, showMark = false, scale)
            }
        }
        if (review.families.any { it.family.hasReference }) {
            Text(
                stringResource(R.string.weekly_review_reference),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One group: its name and count, read as one item, over a bar that is only a picture of the count. */
@Composable
private fun SetsRow(name: String, detail: String?, sets: Int, showMark: Boolean, scale: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        val count = pluralStringResource(R.plurals.set_count, sets, sets)
        val nameText: @Composable (Modifier) -> Unit = { m ->
            Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = m)
        }
        val countText: @Composable () -> Unit = {
            Text(count, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // With a large font the name and count share no line, so neither is squeezed.
        if (isLargeFontScale()) {
            nameText(Modifier)
            countText()
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                nameText(Modifier.weight(1f).padding(end = Spacing.s))
                countText()
            }
        }
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SetsBar(sets, scale, showMark)
    }
}

/**
 * The count as a bar filling from the start edge, with the 10-set mark on [showMark] groups.
 * Both are laid out by weight, never by an x offset, so they mirror in right-to-left languages.
 */
@Composable
private fun SetsBar(sets: Int, scale: Int, showMark: Boolean) {
    Box(Modifier.fillMaxWidth().height(MARK_HEIGHT).clearAndSetSemantics {}, contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(BAR_HEIGHT).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            val fraction = (sets.toFloat() / scale).coerceIn(0f, 1f)
            if (fraction > 0f) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
        }
        if (showMark) {
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                Spacer(Modifier.weight(WeeklyReview.REFERENCE_SETS.toFloat()))
                Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
                Spacer(Modifier.weight((scale - WeeklyReview.REFERENCE_SETS).toFloat()))
            }
        }
    }
}

@StringRes
private fun MovementFamily.label(): Int = when (this) {
    MovementFamily.PUSH -> R.string.weekly_review_push
    MovementFamily.PULL -> R.string.weekly_review_pull
    MovementFamily.LEGS -> R.string.weekly_review_legs
    MovementFamily.CORE -> R.string.weekly_review_core
    MovementFamily.CONDITIONING -> R.string.weekly_review_conditioning
    MovementFamily.MOBILITY -> R.string.weekly_review_mobility
}

// Twice the reference, so the mark sits halfway until a group passes 20 sets.
private const val BAR_MIN_SCALE = 2 * WeeklyReview.REFERENCE_SETS
private val BAR_HEIGHT = 6.dp
private val MARK_HEIGHT = 14.dp
