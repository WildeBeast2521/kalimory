package io.github.gonbei774.calisthenicsmemory.ui.components.muscles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    val front = remember { BodyMapData.front.map { it.region to it.toPath() } }
    val back = remember { BodyMapData.back.map { it.region to it.toPath() } }

    Canvas(modifier.aspectRatio((2 * BodyMapData.WIDTH + GAP) / BodyMapData.HEIGHT)) {
        val unit = size.height / BodyMapData.HEIGHT
        fun DrawScope.side(shapes: List<Pair<BodyRegion, Path>>, left: Float) {
            translate(left = left) {
                scale(unit, unit, pivot = Offset.Zero) {
                    shapes.forEach { (region, path) ->
                        drawPath(path, if (region in main) red else if (region in helping) lightRed else body)
                    }
                }
            }
        }
        side(front, 0f)
        side(back, (BodyMapData.WIDTH + GAP) * unit)
    }
}

private const val GAP = 12f

private fun BodyShape.toPath(): Path = Path().apply {
    moveTo(points[0], points[1])
    for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1])
    close()
}
