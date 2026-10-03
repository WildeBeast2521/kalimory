@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.gonbei774.calisthenicsmemory.ui.components.workout

import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
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
            // A segment eases into its new state as a set finishes.
            val color by animateColorAsState(
                when {
                    number < setNumber -> done
                    number == setNumber -> accent
                    else -> upcoming
                },
                label = "set segment",
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color)
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
    // In the dark theme a running dial glows faintly in its own colour, so the screen feels lit.
    val glow = MaterialTheme.colorScheme.background.luminance() < 0.5f && !paused
    val toggleLabel = stringResource(if (paused) R.string.resume_button else R.string.pause_button)
    val pausedLabel = stringResource(R.string.pause_button)
    // The last three seconds of a countdown each give the ring a short pulse, with the beeps.
    val pulse = remember { Animatable(1f) }
    val finalSeconds = !paused && value.toIntOrNull() in 1..3
    LaunchedEffect(value, finalSeconds) {
        if (finalSeconds) {
            pulse.snapTo(1.35f)
            pulse.animateTo(1f, tween(360))
        }
    }
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
        if (glow) {
            Canvas(Modifier.size(size)) {
                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent)), radius = this.size.minDimension / 2)
            }
        }
        // Expressive wavy ring: it ripples while the clock runs and lies flat when paused. The
        // last-seconds pulse scales the drawn ring rather than widening its stroke, so the wave
        // path is not rebuilt every frame (a widening stroke made short countdowns stutter).
        val stroke = with(LocalDensity.current) { Stroke(16.dp.toPx(), cap = StrokeCap.Round) }
        // Kept across frames, so the indicator does not see a new amplitude function each tick.
        val amplitude = remember(paused) { { p: Float -> if (paused) 0f else WavyProgressIndicatorDefaults.indicatorAmplitude(p) } }
        CircularWavyProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val scale = 1f + (pulse.value - 1f) * 0.12f
                    scaleX = scale
                    scaleY = scale
                }
                .clearAndSetSemantics {},
            color = accent.copy(alpha = if (paused) 0.35f else 1f),
            trackColor = track,
            stroke = stroke,
            trackStroke = stroke,
            // A long, slow wave keeps the dial calm.
            amplitude = amplitude,
            wavelength = 84.dp,
            waveSpeed = 24.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RollingNumber(
                value,
                // Three digits (a 240 s rest) would touch the ring at full size, so they step down,
                // scaled with the dial.
                style = if (value.length >= 3) WorkoutNumerals.copy(fontSize = 72.sp * (size / 264.dp), lineHeight = 80.sp * (size / 264.dp)) else WorkoutNumerals,
                color = MaterialTheme.colorScheme.onSurface,
                // The dial already announces the value.
                modifier = Modifier.alpha(if (paused) 0.4f else 1f).clearAndSetSemantics {},
            )
            if (paused) PausedMark()
        }
    }
}

/**
 * A number whose changed digits roll up into place, like a mechanical counter. Digits are
 * matched from the right, so "10" to "9" moves only the digits that change.
 */
@Composable
fun RollingNumber(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    Row(modifier) {
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        (slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(150))) using
                            SizeTransform(clip = false)
                    },
                    label = "digit",
                ) { digit ->
                    Text(digit.toString(), style = style, color = color)
                }
            }
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
            val countColor by animateColorAsState(
                if (reached) WorkoutTone.done else MaterialTheme.colorScheme.onSurface,
                label = "count",
            )
            RollingNumber(
                "$value",
                style = if (compact) WorkoutNumerals else WorkoutNumerals.copy(fontSize = 120.sp, lineHeight = 124.sp),
                color = countColor,
                // One text node for the whole number, not one per digit.
                modifier = Modifier.clearAndSetSemantics { text = AnnotatedString("$value") },
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

/**
 * A rest's controls as an Expressive floating toolbar at the bottom of the screen: −10 s and
 * +10 s change only this rest, and Skip ends it.
 */
@Composable
fun RestToolbar(onMinus: () -> Unit, onPlus: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    HorizontalFloatingToolbar(expanded = true, modifier = modifier) {
        androidx.compose.material3.TextButton(onClick = onMinus, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.minus_10sec), style = MaterialTheme.typography.titleMedium)
        }
        androidx.compose.material3.TextButton(onClick = onPlus, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.plus_10sec), style = MaterialTheme.typography.titleMedium)
        }
        androidx.compose.material3.FilledTonalButton(
            onClick = onSkip,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(AppIcons.SkipNext, contentDescription = null)
            Spacer(Modifier.size(Spacing.s))
            Text(stringResource(R.string.skip_button), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** A large round step button (− or +) beside a count. */
@Composable
fun StepButton(icon: ImageVector, contentDescription: String, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        // Pressing squeezes the circle into a rounded square (Material 3 Expressive).
        shapes = IconButtonDefaults.shapes(),
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
        // Pressing squeezes the corners, so the main action answers the thumb.
        shapes = ButtonShapes(shape = MaterialTheme.shapes.large, pressedShape = MaterialTheme.shapes.small),
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
 * The expressive moment (ADR 0004): after a completed set, a badge springs in at the start of
 * the rest, its tick draws itself, and a short vibration confirms the set unless the user
 * turned that off. Animations follow the system "remove animations" setting.
 */
@Composable
fun SetDoneBadge(label: String, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0.4f) }
    val tick = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (WorkoutPreferences(context).isSetDoneVibrationEnabled()) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(Unit) {
        tick.animateTo(1f, tween(360, delayMillis = 120))
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
            val tickColor = MaterialTheme.colorScheme.onPrimaryContainer
            Canvas(Modifier.size(28.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.53f)
                    lineTo(size.width * 0.4f, size.height * 0.72f)
                    lineTo(size.width * 0.8f, size.height * 0.3f)
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val drawn = Path()
                measure.getSegment(0f, measure.length * tick.value, drawn, true)
                drawPath(drawn, tickColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
        Text(label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
