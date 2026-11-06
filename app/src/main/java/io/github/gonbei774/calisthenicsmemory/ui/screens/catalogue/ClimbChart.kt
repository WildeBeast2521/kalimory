package io.github.gonbei774.calisthenicsmemory.ui.screens.catalogue

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.progression.ChainClimb
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.formatDate
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A chain's climb (#161): the highest step started over time, with a hollow mark where a step was
 * started and a filled one where its standard was first met. The drawing is decorative; TalkBack
 * reads one sentence, and the step rows below list every date in words.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClimbChart(climb: ChainClimb, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val summary = if (climb.firstStep == climb.highestStep) {
        stringResource(R.string.climb_summary_same, climb.firstStep, formatDate(climb.start, locale))
    } else {
        stringResource(R.string.climb_summary, climb.firstStep, formatDate(climb.start, locale), climb.highestStep, formatDate(climb.highestOn, locale))
    }
    val lowest = climb.marks.minOf { it.step }
    val highest = climb.marks.maxOf { it.step }
    val started = MaterialTheme.colorScheme.tertiary
    val met = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = surface) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                stringResource(R.string.climb_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            Column(Modifier.clearAndSetSemantics { contentDescription = summary }, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row {
                    Column(Modifier.height(CHART_HEIGHT).padding(end = Spacing.s), verticalArrangement = Arrangement.SpaceBetween) {
                        StepLabel(highest)
                        if (lowest != highest) StepLabel(lowest)
                    }
                    // The dates sit under the drawing itself, so they line up with its ends.
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        ClimbCanvas(climb, lowest, highest, started, met, surface, Modifier.fillMaxWidth().height(CHART_HEIGHT))
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DateLabel(formatDate(climb.start, locale))
                            if (climb.end != climb.start) DateLabel(formatDate(climb.end, locale))
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    LegendItem(stringResource(R.string.climb_legend_started)) { Mark(started, filled = false, surface) }
                    LegendItem(stringResource(R.string.climb_legend_met)) { Mark(met, filled = true, surface) }
                }
            }
        }
    }
}

@Composable
private fun StepLabel(step: Int) {
    Text(stringResource(R.string.climb_step, step), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DateLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = Spacing.s)) { swatch() }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Mark(color: Color, filled: Boolean, surface: Color) {
    Canvas(Modifier.size(12.dp)) { drawMark(center, color, filled, surface) }
}

@Composable
private fun ClimbCanvas(
    climb: ChainClimb,
    lowest: Int,
    highest: Int,
    started: Color,
    met: Color,
    surface: Color,
    modifier: Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier) {
        val pad = 8.dp.toPx()
        val span = ChronoUnit.DAYS.between(climb.start, climb.end).coerceAtLeast(1).toFloat()
        // Time runs from the start edge, so it reads the same way as the dates below in either direction.
        fun x(date: LocalDate): Float {
            val fraction = ChronoUnit.DAYS.between(climb.start, date) / span
            val px = pad + fraction * (size.width - 2 * pad)
            return if (rtl) size.width - px else px
        }
        fun y(step: Int): Float =
            if (highest == lowest) size.height / 2 else pad + (highest - step).toFloat() / (highest - lowest) * (size.height - 2 * pad)

        val line = Path()
        climb.levels.forEachIndexed { index, (date, step) ->
            if (index == 0) {
                line.moveTo(x(date), y(step))
            } else {
                line.lineTo(x(date), y(climb.levels[index - 1].second))
                line.lineTo(x(date), y(step))
            }
        }
        line.lineTo(x(climb.end), y(climb.highestStep))
        drawPath(line, met, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        climb.marks.forEach { mark -> drawMark(Offset(x(mark.date), y(mark.step)), if (mark.met) met else started, mark.met, surface) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMark(at: Offset, color: Color, filled: Boolean, surface: Color) {
    val radius = 5.dp.toPx()
    if (filled) {
        drawCircle(color, radius, at)
    } else {
        drawCircle(surface, radius, at)
        drawCircle(color, radius - 1.dp.toPx(), at, style = Stroke(width = 2.dp.toPx()))
    }
}

private val CHART_HEIGHT = 120.dp
