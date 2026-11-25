package io.github.gonbei774.calisthenicsmemory.ui.screens.review

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MuscleGroup
import io.github.gonbei774.calisthenicsmemory.data.catalogue.group
import io.github.gonbei774.calisthenicsmemory.data.review.MuscleVolumeSummary
import io.github.gonbei774.calisthenicsmemory.data.review.SetCounts
import io.github.gonbei774.calisthenicsmemory.data.review.TrendBucket
import io.github.gonbei774.calisthenicsmemory.data.review.TrendSpan
import io.github.gonbei774.calisthenicsmemory.data.review.WeeklyReview
import io.github.gonbei774.calisthenicsmemory.ui.components.common.ConnectedChoices
import io.github.gonbei774.calisthenicsmemory.ui.components.common.RowGroup
import io.github.gonbei774.calisthenicsmemory.ui.components.common.SectionHeading
import io.github.gonbei774.calisthenicsmemory.ui.components.muscles.muscleLabel
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.formatDate
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** Everything the over-time view needs from its screen: the span, the buckets once computed, and what is chosen. */
internal class TrendUi(
    val span: TrendSpan,
    val onSpan: (TrendSpan) -> Unit,
    /** Null while the buckets are being computed. */
    val buckets: List<TrendBucket>?,
    /** Null until one is chosen: then the group with the most sets in the span. */
    val group: MuscleGroup?,
    val onGroup: (MuscleGroup) -> Unit,
    /** Null for the whole group. */
    val muscle: Muscle?,
    val onMuscle: (Muscle?) -> Unit,
)

@Composable
internal fun TrendSpanChips(selected: TrendSpan, onSpan: (TrendSpan) -> Unit) {
    ConnectedChoices(
        options = TrendSpan.entries,
        selected = selected,
        onSelect = { it?.let(onSpan) },
        label = {
            stringResource(
                when (it) {
                    TrendSpan.MONTH -> R.string.period_one_month
                    TrendSpan.THREE_MONTHS -> R.string.period_three_months
                    TrendSpan.YEAR -> R.string.period_one_year
                    TrendSpan.ALL -> R.string.period_all
                }
            )
        },
    )
}

/**
 * Sets a week for one muscle group or one of its muscles, bucket by bucket (ADR 0015): chips to
 * choose, a chart with the 10 a week mark, every bucket in words below it, and what is not on the
 * muscles ([notOnMap]). Nothing is stored; the numbers come from [TrendUi.buckets].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrendBody(trend: TrendUi, notOnMap: MuscleVolumeSummary) {
    val buckets = trend.buckets ?: return
    val group = trend.group ?: remember(buckets) { bestGroup(buckets) }
    val muscles = remember(group) { Muscle.entries.filter { it.group == group } }
    val muscle = trend.muscle?.takeIf { it in muscles }
    val series = remember(buckets, group, muscle) {
        buckets.map { (if (muscle != null) it.muscles[muscle] else it.groups[group]) ?: SetCounts(0, 0) }
    }
    val rates = remember(series, buckets) { series.mapIndexed { i, counts -> counts.weighted / buckets[i].weeks } }
    val name = muscle?.let { muscleLabel(it) } ?: groupLabel(group)
    val locale = LocalConfiguration.current.locales[0]

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                MuscleGroup.entries.forEach { FilterChip(selected = it == group, onClick = { trend.onGroup(it) }, label = { Text(groupLabel(it)) }) }
            }
            if (muscles.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    FilterChip(selected = muscle == null, onClick = { trend.onMuscle(null) }, label = { Text(stringResource(R.string.body_map_trend_all_muscles)) })
                    muscles.forEach { FilterChip(selected = it == muscle, onClick = { trend.onMuscle(it) }, label = { Text(muscleLabel(it)) }) }
                }
            }
        }

        if (series.all { it.weighted == 0.0 }) {
            Text(stringResource(R.string.body_map_trend_empty, name), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                SectionHeading(stringResource(R.string.body_map_trend_unit))
                TrendChart(
                    rates = rates,
                    description = stringResource(
                        R.string.body_map_trend_summary, name, formatDate(buckets.first().start, locale), formatDate(buckets.last().endInclusive, locale),
                        rates.last().roundToInt(), rates.max().roundToInt(),
                    ),
                )
                // The mark and how a set is counted: the same cited lines as the weekly review and the body map.
                Text(stringResource(R.string.weekly_review_reference), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.body_map_counting), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                SectionHeading(stringResource(if (trend.span.byMonth) R.string.body_map_trend_by_month else R.string.body_map_trend_by_week))
                RowGroup {
                    buckets.indices.reversed().forEach { i ->
                        val counts = series[i]
                        val soFar = i == buckets.lastIndex
                        val detail = listOfNotNull(
                            counts.helping.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.body_map_helping, it, it) },
                            stringResource(R.string.body_map_trend_so_far).takeIf { soFar },
                        ).joinToString(", ").ifEmpty { null }
                        ReviewRow(
                            name = bucketLabel(buckets[i].start, trend.span.byMonth, locale),
                            value = pluralStringResource(R.plurals.set_count, counts.direct, counts.direct),
                            detail = detail,
                        )
                    }
                }
            }
        }
        NotOnMap(notOnMap)
    }
}

/** The group with the most weighted sets over all buckets; chest when there are none. */
internal fun bestGroup(buckets: List<TrendBucket>): MuscleGroup =
    MuscleGroup.entries.maxByOrNull { g -> buckets.sumOf { (it.groups[g]?.weighted ?: 0.0) } }
        ?.takeIf { g -> buckets.any { (it.groups[g]?.weighted ?: 0.0) > 0.0 } } ?: MuscleGroup.CHEST

@Composable
private fun bucketLabel(start: LocalDate, byMonth: Boolean, locale: java.util.Locale): String =
    if (byMonth) "${start.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${start.year}"
    else stringResource(R.string.body_map_trend_week, formatDate(start, locale))

@Composable
internal fun groupLabel(group: MuscleGroup): String = stringResource(
    when (group) {
        MuscleGroup.CHEST -> R.string.muscle_chest
        MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        MuscleGroup.BACK -> R.string.muscle_group_back
        MuscleGroup.ARMS -> R.string.muscle_group_arms
        MuscleGroup.CORE -> R.string.muscle_group_core
        MuscleGroup.LEGS -> R.string.muscle_group_legs
        MuscleGroup.CALVES -> R.string.muscle_calves
    }
)

/**
 * Columns of sets a week, one per bucket, the last still running (drawn lighter, and said "so far"
 * in the rows below). The line marks 10 a week. It has no text of its own beyond axis numerals;
 * [description] is what a screen reader gets, and the rows below carry every number. It runs from
 * the start edge, so it is mirrored in a right-to-left layout.
 */
@Composable
private fun TrendChart(rates: List<Double>, description: String) {
    val bar = MaterialTheme.colorScheme.primary
    val mark = MaterialTheme.colorScheme.outline
    val axis = MaterialTheme.colorScheme.onSurfaceVariant
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(Spacing.l)
                .clearAndSetSemantics { contentDescription = description },
        ) {
            val reference = WeeklyReview.REFERENCE_SETS.toDouble()
            val top = max(ceil(max(rates.max(), reference) / 5.0) * 5.0, 5.0)
            val labelWidth = 28.dp.toPx()
            val plotWidth = size.width - labelWidth
            val plotLeft = if (rtl) 0f else labelWidth
            val slot = plotWidth / rates.size
            val barWidth = slot * 0.7f
            fun y(value: Double) = (size.height * (1 - value / top)).toFloat()
            // Slot i runs from the start edge: left to right, or right to left.
            fun x(i: Int) = if (rtl) plotLeft + plotWidth - (i + 1) * slot + (slot - barWidth) / 2 else plotLeft + i * slot + (slot - barWidth) / 2

            drawLine(mark, Offset(plotLeft, y(reference)), Offset(plotLeft + plotWidth, y(reference)), strokeWidth = 2.dp.toPx())
            rates.forEachIndexed { i, rate ->
                if (rate <= 0.0) return@forEachIndexed
                drawRoundRect(
                    color = bar,
                    topLeft = Offset(x(i), y(rate)),
                    size = Size(barWidth, size.height - y(rate)),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    alpha = if (i == rates.lastIndex) 0.5f else 1f,
                )
            }
            val paint = Paint().apply {
                isAntiAlias = true
                color = axis.toArgb()
                textSize = with(density) { 12.sp.toPx() }
                textAlign = if (rtl) Paint.Align.LEFT else Paint.Align.RIGHT
            }
            val labelX = if (rtl) plotLeft + plotWidth + 4.dp.toPx() else labelWidth - 4.dp.toPx()
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText("0", labelX, size.height, paint)
                canvas.nativeCanvas.drawText(reference.roundToInt().toString(), labelX, y(reference) - 3.dp.toPx(), paint)
                if (top > reference) canvas.nativeCanvas.drawText(top.roundToInt().toString(), labelX, paint.textSize, paint)
            }
        }
    }
}
