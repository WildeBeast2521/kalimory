package io.github.gonbei774.calisthenicsmemory.ui.components.muscles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.review.VolumeTier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle

/** Where each catalogue muscle (ADR 0006) sits on the drawn body. */
internal fun muscleRegions(muscle: Muscle): Set<BodyRegion> = when (muscle) {
    Muscle.ABDOMINALS -> setOf(BodyRegion.ABS, BodyRegion.OBLIQUES)
    Muscle.ABDUCTORS -> setOf(BodyRegion.ABDUCTORS)
    Muscle.ADDUCTORS -> setOf(BodyRegion.ADDUCTORS)
    Muscle.BICEPS -> setOf(BodyRegion.BICEPS)
    Muscle.CALVES -> setOf(BodyRegion.CALVES, BodyRegion.SOLEUS)
    Muscle.CHEST -> setOf(BodyRegion.CHEST)
    Muscle.FOREARMS -> setOf(BodyRegion.FOREARMS)
    Muscle.GLUTES -> setOf(BodyRegion.GLUTES)
    Muscle.HAMSTRINGS -> setOf(BodyRegion.HAMSTRINGS)
    // The drawn back has one upper-back area, shared by the lats and the middle back.
    Muscle.LATS, Muscle.MIDDLE_BACK -> setOf(BodyRegion.UPPER_BACK)
    Muscle.LOWER_BACK -> setOf(BodyRegion.LOWER_BACK)
    Muscle.NECK -> setOf(BodyRegion.NECK)
    Muscle.QUADRICEPS -> setOf(BodyRegion.QUADRICEPS)
    Muscle.SHOULDERS -> setOf(BodyRegion.FRONT_DELTOIDS, BodyRegion.BACK_DELTOIDS)
    Muscle.TRAPS -> setOf(BodyRegion.TRAPEZIUS)
    Muscle.TRICEPS -> setOf(BodyRegion.TRICEPS)
}

/**
 * The muscle map (ADR 0008, decision 1): the body from the front and the back, with the muscles
 * doing the work in red and the ones helping in a lighter red. It is decorative: the same muscles
 * are always listed as text next to it.
 */
@Composable
fun MuscleMap(primary: Set<Muscle>, secondary: Set<Muscle>, modifier: Modifier = Modifier) {
    val main = remember(primary) { primary.flatMap(::muscleRegions).toSet() }
    val helping = remember(secondary, main) { secondary.flatMap(::muscleRegions).toSet() - main }
    val body = MaterialTheme.colorScheme.surfaceContainerHighest
    val red = MaterialTheme.colorScheme.error
    // Blended over the body colour, so it reads the same on any surface.
    val lightRed = red.copy(alpha = 0.4f).compositeOver(body)
    BodyFigure(fill = { region -> if (region in main) red else if (region in helping) lightRed else body }, modifier = modifier)
}

/**
 * Sets a week per muscle (ADR 0011) as three shades of the accent, on the same figure. Decorative:
 * the body map lists every muscle and its sets as text.
 */
@Composable
fun VolumeMap(
    tiers: Map<Muscle, VolumeTier>,
    modifier: Modifier = Modifier,
    /** Muscles to outline as not worked; a region is outlined only when no muscle drawn there was worked. */
    notWorked: Set<Muscle> = emptySet(),
) {
    val regions = remember(tiers) { regionTiers(tiers) }
    val outlined = remember(regions, notWorked) { notWorked.flatMap(::muscleRegions).filterTo(mutableSetOf()) { regions[it] == null } }
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val none = tierColor(VolumeTier.NONE)
    val low = tierColor(VolumeTier.LOW)
    val mid = tierColor(VolumeTier.MID)
    val high = tierColor(VolumeTier.HIGH)
    BodyFigure(
        fill = { region ->
            when (regions[region] ?: VolumeTier.NONE) {
                VolumeTier.NONE -> none
                VolumeTier.LOW -> low
                VolumeTier.MID -> mid
                VolumeTier.HIGH -> high
            }
        },
        stroke = { region -> if (region in outlined) outline else null },
        modifier = modifier,
    )
}

/** Each drawn region's shade: the highest of the muscles drawn there (the lats and middle back share one). */
internal fun regionTiers(tiers: Map<Muscle, VolumeTier>): Map<BodyRegion, VolumeTier> {
    val regions = mutableMapOf<BodyRegion, VolumeTier>()
    tiers.forEach { (muscle, tier) ->
        muscleRegions(muscle).forEach { region -> regions[region] = maxOf(regions[region] ?: VolumeTier.NONE, tier) }
    }
    return regions
}

/** The fill for a shade, shared by the figure and the legend; blended over the body colour. */
@Composable
internal fun tierColor(tier: VolumeTier): Color {
    val body = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent = MaterialTheme.colorScheme.primary
    return when (tier) {
        VolumeTier.NONE -> body
        VolumeTier.LOW -> accent.copy(alpha = 0.3f).compositeOver(body)
        VolumeTier.MID -> accent.copy(alpha = 0.6f).compositeOver(body)
        VolumeTier.HIGH -> accent
    }
}

/** The body from the front and the back, each region filled by [fill] and outlined by [stroke] where it gives a colour. */
@Composable
internal fun BodyFigure(fill: (BodyRegion) -> Color, modifier: Modifier = Modifier, stroke: (BodyRegion) -> Color? = { null }) {
    val front = remember { BodyMapData.front.map { it.region to it.toPath() } }
    val back = remember { BodyMapData.back.map { it.region to it.toPath() } }
    Canvas(modifier.aspectRatio((2 * BodyMapData.WIDTH + GAP) / BodyMapData.HEIGHT)) {
        val unit = size.height / BodyMapData.HEIGHT
        fun DrawScope.side(shapes: List<Pair<BodyRegion, Path>>, left: Float) {
            translate(left = left) {
                scale(unit, unit, pivot = Offset.Zero) {
                    shapes.forEach { (region, path) ->
                        drawPath(path, fill(region))
                        // The scale applies to the stroke too, so its width is divided back to 1.5 dp.
                        stroke(region)?.let { drawPath(path, it, style = Stroke(width = 1.5.dp.toPx() / unit)) }
                    }
                }
            }
        }
        side(front, 0f)
        side(back, (BodyMapData.WIDTH + GAP) * unit)
    }
}

/** A muscle's name in the app's language. */
@Composable
internal fun muscleLabel(muscle: Muscle): String = stringResource(
    when (muscle) {
        Muscle.ABDOMINALS -> R.string.muscle_abdominals
        Muscle.ABDUCTORS -> R.string.muscle_abductors
        Muscle.ADDUCTORS -> R.string.muscle_adductors
        Muscle.BICEPS -> R.string.muscle_biceps
        Muscle.CALVES -> R.string.muscle_calves
        Muscle.CHEST -> R.string.muscle_chest
        Muscle.FOREARMS -> R.string.muscle_forearms
        Muscle.GLUTES -> R.string.muscle_glutes
        Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
        Muscle.LATS -> R.string.muscle_lats
        Muscle.LOWER_BACK -> R.string.muscle_lower_back
        Muscle.MIDDLE_BACK -> R.string.muscle_middle_back
        Muscle.NECK -> R.string.muscle_neck
        Muscle.QUADRICEPS -> R.string.muscle_quadriceps
        Muscle.SHOULDERS -> R.string.muscle_shoulders
        Muscle.TRAPS -> R.string.muscle_traps
        Muscle.TRICEPS -> R.string.muscle_triceps
    }
)

private const val GAP = 12f

private fun BodyShape.toPath(): Path = Path().apply {
    moveTo(points[0], points[1])
    for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1])
    close()
}
