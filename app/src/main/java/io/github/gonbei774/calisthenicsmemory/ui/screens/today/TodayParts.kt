package io.github.gonbei774.calisthenicsmemory.ui.screens.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import io.github.gonbei774.calisthenicsmemory.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn

/** A heading for a section of Today. */
@Composable
internal fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * The week so far, one mark per day: filled where you trained, outlined where you did not,
 * today ringed. Reading it takes a glance, which is all Today should ask. [todayFill] runs from
 * 0 to 1 to grow today's mark into place as a moment (the workout summary); elsewhere it is 1.
 * With [onDayClick], each day up to today opens that day's history.
 */
@Composable
internal fun WeekStrip(
    days: List<LocalDate>,
    trained: Set<LocalDate>,
    today: LocalDate,
    locale: Locale,
    description: String,
    todayFill: Float = 1f,
    onDayClick: ((LocalDate) -> Unit)? = null,
) {
    val trainedLabel = stringResource(R.string.today_day_trained)
    Row(
        // Without taps the strip reads as one summary; with them, each day speaks for itself.
        modifier = Modifier.fillMaxWidth().then(
            if (onDayClick == null) Modifier.clearAndSetSemantics { contentDescription = description } else Modifier
        ),
    ) {
        days.forEach { day ->
            val done = day in trained
            val isToday = day == today
            val future = day.isAfter(today)
            val tap = if (onDayClick != null && !future) {
                val date = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
                Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button) { onDayClick(day) }
                    .clearAndSetSemantics {
                        contentDescription = if (done) String.format(trainedLabel, date) else date
                        role = Role.Button
                        onClick { onDayClick(day); true }
                    }
            } else Modifier
            // Equal cells, so each day's touch target is as wide as the row allows.
            Column(Modifier.weight(1f).then(tap).padding(vertical = Spacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.s))
                // Today carries a brass ring; days still to come are only outlined.
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape) else Modifier)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .then(
                            when {
                                done && isToday -> todayFill.coerceIn(0f, 1f).let { fill ->
                                    Modifier
                                        .graphicsLayer { scaleX = 0.5f + 0.5f * fill; scaleY = scaleX }
                                        .background(lerp(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primary, fill))
                                }
                                done -> Modifier.background(MaterialTheme.colorScheme.primary)
                                future -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                else -> Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            }
                        ),
                )
            }
        }
    }
}

/** The days of the week containing [today], starting on the locale's first day. */
internal fun weekOf(today: LocalDate, firstDay: DayOfWeek): List<LocalDate> {
    val shift = (today.dayOfWeek.value - firstDay.value + 7) % 7
    val start = today.minusDays(shift.toLong())
    return (0L until 7L).map { start.plusDays(it) }
}

/**
 * Sets drawn as chalk tallies: four strokes and a fifth across them, the way sets are
 * counted on a wall. Purely visual; the caller describes the count for screen readers.
 */
@Composable
internal fun TallyMarks(count: Int, color: Color, modifier: Modifier = Modifier) {
    val groups = (count + 4) / 5
    Canvas(modifier.height(22.dp).width((groups * 40).dp)) {
        val stroke = 2.5.dp.toPx()
        val gap = 7.dp.toPx()
        val groupWidth = 40.dp.toPx()
        for (g in 0 until groups) {
            val inGroup = minOf(5, count - g * 5)
            val x0 = g * groupWidth + stroke
            for (i in 0 until minOf(4, inGroup)) {
                val x = x0 + i * gap
                drawLine(color, start = androidx.compose.ui.geometry.Offset(x, 2f), end = androidx.compose.ui.geometry.Offset(x, size.height - 2f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            if (inGroup == 5) {
                drawLine(
                    color,
                    start = androidx.compose.ui.geometry.Offset(x0 - gap * 0.6f, size.height * 0.75f),
                    end = androidx.compose.ui.geometry.Offset(x0 + 3 * gap + gap * 0.6f, size.height * 0.25f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/** An icon set in one of Material 3 Expressive's organic shapes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ShapeBadge(icon: ImageVector, container: Color, content: Color, organic: Boolean = true) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(if (organic) MaterialShapes.Cookie9Sided.toShape() else CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}

/** One tappable row: badge, name and what it is. */
@Composable
internal fun TodayRow(icon: ImageVector, name: String, kind: String, trailing: ImageVector? = null, note: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeBadge(icon, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.width(Spacing.l))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(kind, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (note != null) {
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Icon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Related rows grouped on one quiet surface, as Train and Library list them. */
@Composable
internal fun RowGroup(content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(vertical = Spacing.s)) { content() }
    }
}

/**
 * The one thing to do next, large: a workout to resume, or the first thing due. Everything
 * else on Today stays quiet around it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HeroCard(
    label: String,
    title: String,
    detail: String,
    action: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // One card for every "what to do now" moment (resume, next up, start from scratch), on Today
    // and Train alike: a tonal container with the app's primary only on the badge and the button.
    val container = scheme.surfaceContainerHigh
    val content = scheme.onSurface
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth(),
    ) {
      Box {
        // One large, faint organic shape in the corner gives the card depth without noise.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 72.dp, y = (-56).dp)
                .size(240.dp)
                .rotate(18f)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(content.copy(alpha = 0.07f))
        )
        Column(Modifier.padding(Spacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeBadge(
                    icon,
                    container = scheme.primaryContainer,
                    content = scheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(Spacing.m))
                Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            }
            Spacer(Modifier.height(Spacing.xl))
            Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(Spacing.xs))
            Text(detail, style = MaterialTheme.typography.bodyLarge, color = content.copy(alpha = 0.8f))
            Spacer(Modifier.height(Spacing.xl))
            Button(
                onClick = onClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                // The play icon every Start button in the app carries.
                Icon(AppIcons.Play, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(action, style = MaterialTheme.typography.labelLarge)
            }
        }
      }
    }
}

/** A heading kept close to its rows, with a way to see and edit everything in it when there is one. */
@Composable
internal fun Section(title: String, onOpenAll: (() -> Unit)?, actionLabel: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionHeading(title, Modifier.weight(1f))
            if (onOpenAll != null) {
                TextButton(onClick = onOpenAll) { Text(actionLabel ?: stringResource(R.string.train_see_all)) }
            }
        }
        content()
    }
}
