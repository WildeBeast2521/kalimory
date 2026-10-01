package io.github.gonbei774.calisthenicsmemory.ui.components.figure

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import io.github.gonbei774.calisthenicsmemory.data.figure.FigureCamera
import io.github.gonbei774.calisthenicsmemory.data.figure.FigureMotion
import io.github.gonbei774.calisthenicsmemory.data.figure.Joint
import io.github.gonbei774.calisthenicsmemory.data.figure.Projected
import io.github.gonbei774.calisthenicsmemory.data.figure.SkeletonSolver
import io.github.gonbei774.calisthenicsmemory.data.figure.SolvedSkeleton
import io.github.gonbei774.calisthenicsmemory.data.figure.Vec3
import io.github.gonbei774.calisthenicsmemory.data.figure.Proportions
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The demonstration figure (ADR 0008, decision 2), seen in the owner's three-quarter view. It loops
 * the motion, or shows its still pose when the system's "Remove animations" is on or [playing] is
 * false. Decorative: each step's cues describe the movement in words.
 */
@Composable
fun FigureView(motion: FigureMotion, modifier: Modifier = Modifier, playing: Boolean = true, camera: FigureCamera = motion.camera) {
    val context = LocalContext.current
    val animationsOn = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val progress = if (animationsOn && playing) {
        rememberInfiniteTransition(label = "figure").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(motion.durationMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "figure progress",
        ).value
    } else {
        null
    }
    // Frame the whole loop once, so the figure does not jump around as it moves.
    val bounds = remember(motion, camera) { FigureBounds.of(motion, camera) }
    val body = MaterialTheme.colorScheme.onSurfaceVariant
    val background = MaterialTheme.colorScheme.surface
    val shadow = MaterialTheme.colorScheme.onSurface

    Canvas(modifier) {
        val pose = progress?.let(motion::poseAt) ?: motion.stillPose
        drawFigure(SkeletonSolver.solve(pose), camera, bounds, body, background, shadow)
    }
}

/** The projected extent of a motion over its whole loop, floor shadow included. */
internal data class FigureBounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
    companion object {
        fun of(motion: FigureMotion, camera: FigureCamera): FigureBounds {
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            for (i in 0..24) {
                val skeleton = SkeletonSolver.solve(motion.poseAt(i / 24f))
                for (p in skeleton.all.values + skeleton.all.values.map { Vec3(it.x, 0f, it.z) }) {
                    val s = camera.project(p)
                    minX = min(minX, s.x); maxX = max(maxX, s.x)
                    minY = min(minY, s.y); maxY = max(maxY, s.y)
                }
            }
            // Room for the head and limb thickness.
            val pad = Proportions.HEAD_RADIUS + 0.06f
            return FigureBounds(minX - pad, maxX + pad, minY - pad, maxY + pad)
        }
    }
}

/** A body part between two joints, drawn as a tapered capsule; radii in metres. */
private class Segment(val from: Joint, val to: Joint, val fromRadius: Float, val toRadius: Float)

private val segments = listOf(
    Segment(Joint.PELVIS, Joint.CHEST, 0.12f, 0.13f),
    Segment(Joint.CHEST, Joint.NECK, 0.13f, 0.08f),
    Segment(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, 0.065f, 0.065f),
    Segment(Joint.LEFT_HIP, Joint.RIGHT_HIP, 0.085f, 0.085f),
    Segment(Joint.NECK, Joint.HEAD, 0.05f, 0.05f),
) + listOf("LEFT", "RIGHT").flatMap { side ->
    fun j(name: String) = Joint.valueOf("${side}_$name")
    listOf(
        Segment(j("SHOULDER"), j("ELBOW"), 0.05f, 0.04f),
        Segment(j("ELBOW"), j("WRIST"), 0.04f, 0.03f),
        Segment(j("WRIST"), j("HAND"), 0.03f, 0.028f),
        Segment(j("HIP"), j("KNEE"), 0.08f, 0.055f),
        Segment(j("KNEE"), j("ANKLE"), 0.052f, 0.035f),
        Segment(j("ANKLE"), j("TOE"), 0.035f, 0.025f),
    )
}

private fun DrawScope.drawFigure(
    skeleton: SolvedSkeleton,
    camera: FigureCamera,
    bounds: FigureBounds,
    body: Color,
    background: Color,
    shadow: Color,
) {
    val scale = min(size.width / (bounds.maxX - bounds.minX), size.height / (bounds.maxY - bounds.minY))
    val offsetX = (size.width - (bounds.maxX - bounds.minX) * scale) / 2
    val offsetY = (size.height - (bounds.maxY - bounds.minY) * scale) / 2
    fun screen(p: Projected) = Offset(offsetX + (p.x - bounds.minX) * scale, offsetY + (bounds.maxY - p.y) * scale)

    // The floor shadow: each part flattened onto the floor, in one flat tone so overlaps do not darken.
    val floorShade = lerp(background, shadow, 0.08f)
    segments.forEach { s ->
        val a = skeleton[s.from]
        val b = skeleton[s.to]
        capsule(
            screen(camera.project(Vec3(a.x, 0f, a.z))), screen(camera.project(Vec3(b.x, 0f, b.z))),
            s.fromRadius * scale * 1.2f, s.toRadius * scale * 1.2f, floorShade,
        )
    }

    // Far parts first, near parts last; far parts fade a little towards the background.
    val projected = skeleton.all.mapValues { camera.project(it.value) }
    val depths = projected.values.map { it.depth }
    val near = depths.max()
    val far = depths.min()
    val parts = segments.map { s ->
        val depth = (projected.getValue(s.from).depth + projected.getValue(s.to).depth) / 2
        s to depth
    } + listOf(null to projected.getValue(Joint.HEAD).depth)
    parts.sortedBy { it.second }.forEach { (s, depth) ->
        val nearness = if (near - far < 1e-4f) 1f else (depth - far) / (near - far)
        val color = lerp(lerp(body, background, 0.45f), body, nearness)
        if (s == null) {
            drawCircle(color, Proportions.HEAD_RADIUS * scale, screen(projected.getValue(Joint.HEAD)))
        } else {
            capsule(screen(projected.getValue(s.from)), screen(projected.getValue(s.to)), s.fromRadius * scale, s.toRadius * scale, color)
        }
    }
}

/** A tapered capsule: two end circles joined by their outer edges. */
private fun DrawScope.capsule(a: Offset, b: Offset, ra: Float, rb: Float, color: Color) {
    drawCircle(color, ra, a)
    drawCircle(color, rb, b)
    val dx = b.x - a.x
    val dy = b.y - a.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 0.5f) return
    val nx = -dy / length
    val ny = dx / length
    val path = Path().apply {
        moveTo(a.x + nx * ra, a.y + ny * ra)
        lineTo(b.x + nx * rb, b.y + ny * rb)
        lineTo(b.x - nx * rb, b.y - ny * rb)
        lineTo(a.x - nx * ra, a.y - ny * ra)
        close()
    }
    drawPath(path, color)
}
