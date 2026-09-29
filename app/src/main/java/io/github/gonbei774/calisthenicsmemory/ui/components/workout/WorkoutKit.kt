@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.gonbei774.calisthenicsmemory.ui.components.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.icons.AppIcons
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalmPalette
import io.github.gonbei774.calisthenicsmemory.ui.theme.Spacing
import io.github.gonbei774.calisthenicsmemory.ui.theme.WorkoutNumerals

/**
 * Shared parts of the in-workout screens. Colour means state, as in [CalmPalette]: the set in
 * progress and getting ready for it use tertiary (brass, active), rest uses the quiet
 * secondary, and anything done uses primary (spruce).
 */
object WorkoutTone {
    val work: Color @Composable get() = MaterialTheme.colorScheme.tertiary
    val prepare: Color @Composable get() = MaterialTheme.colorScheme.tertiary
    val rest: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val done: Color @Composable get() = MaterialTheme.colorScheme.primary
}

/**
 * The exercise, with the set as a quiet line, an optional [caption] (a program's loop round)
 * and one segment per set: done sets filled, the current one in [accent], the rest to come.
 */
@Composable
fun WorkoutHeader(
    exerciseName: String,
    setLabel: String,
    setNumber: Int,
    totalSets: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = Spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            exerciseName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            setLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
        if (totalSets in 2..MAX_SEGMENTS) {
            SetSegments(setNumber, totalSets, accent, Modifier.padding(top = Spacing.m))
        }
    }
}

private const val MAX_SEGMENTS = 20

@Composable
private fun SetSegments(setNumber: Int, totalSets: Int, accent: Color, modifier: Modifier = Modifier) {
    val done = WorkoutTone.done
    val upcoming = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        // Decorative: the set line above already says which set this is.
        modifier = modifier.widthIn(max = 280.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (number in 1..totalSets) {
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        when {
                            number < setNumber -> done
                            number == setNumber -> accent
                            else -> upcoming
                        }
                    )
            )
        }
    }
}

/**
 * A countdown ring with the time in the middle. Tapping it pauses or resumes; while paused
 * the ring and number dim and a play symbol shows how to continue.
 *
 * @param progress how much of the ring is left, from 1 (full) to 0.
 */
@Composable
fun TimerDial(
    progress: Float,
    value: String,
    accent: Color,
    paused: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 264.dp,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val toggleLabel = stringResource(if (paused) R.string.resume_button else R.string.pause_button)
    val pausedLabel = stringResource(R.string.pause_button)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Circle.toShape())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = toggleLabel,
                onClick = onToggle,
            )
            .semantics {
                contentDescription = value
                if (paused) stateDescription = pausedLabel
            },
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(
                accent.copy(alpha = if (paused) 0.35f else 1f),
                -90f,
                360f * progress.coerceIn(0f, 1f),
                false,
                topLeft,
                arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                style = WorkoutNumerals,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(if (paused) 0.4f else 1f),
            )
            if (paused) PausedMark()
        }
    }
}

/**
 * A big count (reps so far) with its unit. Tapping it pauses or resumes. The number turns
 * [reachedColor] once the target is reached.
 */
@Composable
fun CountDisplay(
    value: Int,
    unit: String?,
    reached: Boolean,
    paused: Boolean,
    onToggle: (() -> Unit)?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val toggleLabel = stringResource(if (paused) R.string.resume_button else R.string.pause_button)
    val tap = if (onToggle != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
            onClickLabel = toggleLabel,
            onClick = onToggle,
        )
    } else Modifier
    Column(modifier.then(tap), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.alpha(if (paused) 0.4f else 1f)) {
            Text(
                "$value",
                style = if (compact) WorkoutNumerals else WorkoutNumerals.copy(fontSize = 120.sp, lineHeight = 124.sp),
                color = if (reached) WorkoutTone.done else MaterialTheme.colorScheme.onSurface,
            )
            if (unit != null) {
                Text(
                    unit,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.s, bottom = 22.dp),
                )
            }
        }
        if (paused) PausedMark()
    }
}

@Composable
private fun PausedMark() {
    Surface(
        shape = MaterialShapes.Cookie9Sided.toShape(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(AppIcons.Play, contentDescription = null, modifier = Modifier.size(28.dp))
        }
    }
}

/** "Set 2/5", or "Set 2/5 - Right" for one side of a one-sided exercise. */
@Composable
fun setLabel(setNumber: Int, totalSets: Int, side: String?): String {
    val sideText = when (side) {
        "Right" -> stringResource(R.string.side_right)
        "Left" -> stringResource(R.string.side_left)
        else -> null
    }
    return if (sideText != null) {
        stringResource(R.string.set_format_with_side, setNumber, totalSets, sideText)
    } else {
        stringResource(R.string.set_format, setNumber, totalSets)
    }
}

/** What is happening now (getting ready, resting), in the colour of that state. */
@Composable
fun WorkoutStatus(text: String, color: Color) {
    Text(text = text, style = MaterialTheme.typography.headlineMedium, color = color)
}

/** Skipping the rest of a timer: a quiet action, below the main one. */
@Composable
fun WorkoutSkipButton(onSkip: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
        Icon(AppIcons.SkipNext, contentDescription = null)
        Spacer(Modifier.size(Spacing.s))
        Text(stringResource(R.string.skip_button), style = MaterialTheme.typography.titleMedium)
    }
}

/** A rest's −10 s and +10 s adjustments; they change only this rest. */
@Composable
fun RestAdjustButtons(onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
        androidx.compose.material3.FilledTonalButton(onClick = onMinus, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.minus_10sec), style = MaterialTheme.typography.titleMedium)
        }
        androidx.compose.material3.FilledTonalButton(onClick = onPlus, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.plus_10sec), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** A large round step button (− or +) beside a count. */
@Composable
fun StepButton(icon: ImageVector, contentDescription: String, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(64.dp),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(32.dp))
    }
}

/** The one main action at the bottom of an in-workout screen, within thumb reach. */
@Composable
fun WorkoutPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = AppIcons.Check) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.size(Spacing.s))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * The expressive moment (ADR 0004): after a completed set, a badge with a check springs in
 * at the start of the rest. Animations follow the system "remove animations" setting.
 */
@Composable
fun SetDoneBadge(label: String, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(56.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
