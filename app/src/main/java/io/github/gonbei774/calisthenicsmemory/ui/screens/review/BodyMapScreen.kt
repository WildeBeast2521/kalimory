package io.github.gonbei774.calisthenicsmemory.ui.screens.review

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.data.review.MuscleVolume
import io.github.gonbei774.calisthenicsmemory.data.review.MuscleVolumeSummary
import io.github.gonbei774.calisthenicsmemory.data.review.VolumeTier
import io.github.gonbei774.calisthenicsmemory.ui.components.common.CalmTopBar
import io.github.gonbei774.calisthenicsmemory.ui.components.muscles.VolumeMap
import io.github.gonbei774.calisthenicsmemory.ui.components.muscles.muscleLabel
import io.github.gonbei774.calisthenicsmemory.ui.components.muscles.tierColor
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.RowGroup
import io.github.gonbei774.calisthenicsmemory.ui.screens.today.SectionHeading
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.Period
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.startDate
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.theme.firstDayOfWeek
import io.github.gonbei774.calisthenicsmemory.util.rememberToday
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

/**
 * The body map (ADR 0011): sets per muscle for a period, shaded on a front and back figure and
 * listed by muscle, from history and each exercise's catalogue step. Nothing is stored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMapScreen(viewModel: TrainingViewModel, onNavigateBack: () -> Unit) {
    val history by viewModel.history.collectAsState()
    val intervalHistory by viewModel.intervalHistory.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val today = rememberToday()
    val firstDay = firstDayOfWeek()
    var periodName by rememberSaveable { mutableStateOf(Period.OneWeek.name) }
    var viewName by rememberSaveable { mutableStateOf(BodyMapView.SETS.name) }
    val period = Period.valueOf(periodName)
    val start = period.startDate(today, firstDay)
    val summary = remember(start, today, history, intervalHistory, exercises) {
        MuscleVolume.build(start, today, history, intervalHistory, exercises)
    }

    val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
        topBar = { CalmTopBar(title = stringResource(R.string.body_map_title), onBack = onNavigateBack, scrollBehavior = topBarScroll) },
    ) { padding ->
        BodyMapContent(
            summary = summary,
            weeks = period.days / 7.0,
            period = period,
            onPeriod = { periodName = it.name },
            view = BodyMapView.valueOf(viewName),
            onView = { viewName = it.name },
            modifier = Modifier.padding(padding),
        )
    }
}

/** What the body map shows: sets per muscle, or the trainable muscles that got none. */
internal enum class BodyMapView(@StringRes val label: Int) {
    SETS(R.string.body_map_view_sets),
    NOT_WORKED(R.string.body_map_view_untrained),
}

/** The body map without state: shown by the screen and by its tests. */
@Composable
internal fun BodyMapContent(
    summary: MuscleVolumeSummary,
    /** The period's length in weeks; shades compare sets a week. */
    weeks: Double,
    period: Period,
    onPeriod: (Period) -> Unit,
    modifier: Modifier = Modifier,
    view: BodyMapView = BodyMapView.SETS,
    onView: (BodyMapView) -> Unit = {},
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                ViewChips(view, onView)
                PeriodChips(period, onPeriod)
            }
            if (summary.isEmpty) {
                Text(stringResource(R.string.body_map_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            val tiers = remember(summary, weeks) { MuscleVolume.tiers(summary, weeks) }
            val notWorked = remember(summary) { MuscleVolume.notWorked(summary) }
            // Decorative: the list below says the same in words.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VolumeMap(
                    tiers,
                    Modifier.widthIn(max = 320.dp).fillMaxWidth(0.85f).clearAndSetSemantics {},
                    notWorked = if (view == BodyMapView.NOT_WORKED) notWorked.toSet() else emptySet(),
                )
            }
            if (view == BodyMapView.NOT_WORKED) {
                NotWorked(notWorked)
                return@Column
            }
            Legend()
            if (summary.muscles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    SectionHeading(stringResource(R.string.body_map_by_muscle))
                    RowGroup {
                        summary.muscles.forEach { sets ->
                            ReviewRow(
                                name = muscleLabel(sets.muscle),
                                value = pluralStringResource(R.plurals.set_count, sets.direct, sets.direct),
                                detail = sets.helping.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.body_map_helping, it, it) },
                                leading = { Swatch(tiers[sets.muscle] ?: VolumeTier.NONE) },
                            )
                        }
                    }
                }
            }
            NotOnMap(summary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ViewChips(selected: BodyMapView, onView: (BodyMapView) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        BodyMapView.entries.forEach { view ->
            FilterChip(selected = view == selected, onClick = { onView(view) }, label = { Text(stringResource(view.label)) })
        }
    }
}

/** Trainable muscles with no set in the period, outlined on the figure and listed by name. */
@Composable
private fun NotWorked(muscles: List<Muscle>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionHeading(stringResource(R.string.body_map_untrained_heading))
        if (muscles.isEmpty()) {
            Text(stringResource(R.string.body_map_untrained_none), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            RowGroup { muscles.forEach { ReviewRow(name = muscleLabel(it), value = "", detail = null) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodChips(selected: Period, onPeriod: (Period) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Period.entries.forEach { period ->
            FilterChip(selected = period == selected, onClick = { onPeriod(period) }, label = { Text(stringResource(period.displayNameResId)) })
        }
    }
}

/** What the three shades mean, and how a set is counted. */
@Composable
private fun Legend() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        listOf(
            VolumeTier.LOW to R.string.body_map_tier_low,
            VolumeTier.MID to R.string.body_map_tier_mid,
            VolumeTier.HIGH to R.string.body_map_tier_high,
        ).forEach { (tier, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.padding(end = Spacing.m)) { Swatch(tier) }
                Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(stringResource(R.string.body_map_counting), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A shade's colour as a small disc, outlined so the lightest one still shows on any background. */
@Composable
private fun Swatch(tier: VolumeTier) {
    Box(
        Modifier
            .size(16.dp)
            .clearAndSetSemantics {}
            .background(tierColor(tier), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}

/** Sets that are not on the figure, listed so none goes missing. */
@Composable
private fun NotOnMap(summary: MuscleVolumeSummary) {
    if (summary.mobilitySets + summary.otherSets + summary.intervalSets == 0) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionHeading(stringResource(R.string.body_map_not_on_map))
        RowGroup {
            if (summary.mobilitySets > 0) {
                ReviewRow(
                    stringResource(R.string.weekly_review_mobility),
                    pluralStringResource(R.plurals.set_count, summary.mobilitySets, summary.mobilitySets),
                    stringResource(R.string.body_map_mobility_detail),
                )
            }
            if (summary.otherSets > 0) {
                ReviewRow(
                    stringResource(R.string.weekly_review_other),
                    pluralStringResource(R.plurals.set_count, summary.otherSets, summary.otherSets),
                    stringResource(R.string.weekly_review_other_detail),
                )
            }
            if (summary.intervalSets > 0) {
                ReviewRow(
                    stringResource(R.string.weekly_review_intervals),
                    pluralStringResource(R.plurals.set_count, summary.intervalSets, summary.intervalSets),
                    stringResource(R.string.weekly_review_intervals_detail),
                )
            }
        }
    }
}
