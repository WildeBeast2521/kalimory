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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.data.figure.FigureMuscles
import androidx.compose.ui.platform.LocalContext
import io.github.gonbei774.calisthenicsmemory.data.figure.FigureCamera
import io.github.gonbei774.calisthenicsmemory.data.figure.FigureMotion
import io.github.gonbei774.calisthenicsmemory.data.figure.Joint
import io.github.gonbei774.calisthenicsmemory.data.figure.Projected
import io.github.gonbei774.calisthenicsmemory.data.figure.Prop
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
fun FigureView(
    motion: FigureMotion,
    modifier: Modifier = Modifier,
    primary: Set<Muscle> = emptySet(),
    secondary: Set<Muscle> = emptySet(),
    playing: Boolean = true,
    camera: FigureCamera = motion.camera,
) {
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
    // The same reds as the muscle map: main muscles red, helping muscles lighter.
    val red = MaterialTheme.colorScheme.error
    val colors = FigureColors(body, background, shadow, red, red.copy(alpha = 0.45f).compositeOver(body))

    // Framed on the figure; large props such as a wall are cut at the edges.
    Canvas(modifier.clipToBounds()) {
        val pose = progress?.let(motion::poseAt) ?: motion.stillPose
        drawFigure(SkeletonSolver.solve(pose), camera, bounds, colors, primary, secondary, motion.props)
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

private class FigureColors(val body: Color, val background: Color, val shadow: Color, val red: Color, val lightRed: Color)

/**
 * A body part between two joints, drawn as a tapered capsule; radii in metres. [inner] parts sit
 * inside the torso and are drawn before it, so they never cover the torso's muscles.
 */
private class Segment(val from: Joint, val to: Joint, val fromRadius: Float, val toRadius: Float, val inner: Boolean = false)

private val segments = listOf(
    Segment(Joint.PELVIS, Joint.CHEST, 0.135f, 0.15f),
    Segment(Joint.CHEST, Joint.NECK, 0.15f, 0.09f),
    Segment(Joint.LEFT_SHOULDER, Joint.RIGHT_SHOULDER, 0.075f, 0.075f, inner = true),
    Segment(Joint.LEFT_HIP, Joint.RIGHT_HIP, 0.095f, 0.095f, inner = true),
    Segment(Joint.NECK, Joint.HEAD, 0.055f, 0.055f),
) + listOf("LEFT", "RIGHT").flatMap { side ->
    fun j(name: String) = Joint.valueOf("${side}_$name")
    listOf(
        Segment(j("SHOULDER"), j("ELBOW"), 0.062f, 0.045f),
        Segment(j("ELBOW"), j("WRIST"), 0.047f, 0.032f),
        Segment(j("WRIST"), j("HAND"), 0.032f, 0.03f),
        Segment(j("HIP"), j("KNEE"), 0.1f, 0.062f),
        Segment(j("KNEE"), j("ANKLE"), 0.062f, 0.038f),
        Segment(j("ANKLE"), j("TOE"), 0.035f, 0.025f),
    )
}

private fun DrawScope.drawFigure(
    skeleton: SolvedSkeleton,
    camera: FigureCamera,
    bounds: FigureBounds,
    colors: FigureColors,
    primary: Set<Muscle>,
    secondary: Set<Muscle>,
    props: List<Prop>,
) {
    val scale = min(size.width / (bounds.maxX - bounds.minX), size.height / (bounds.maxY - bounds.minY))
    val offsetX = (size.width - (bounds.maxX - bounds.minX) * scale) / 2
    val offsetY = (size.height - (bounds.maxY - bounds.minY) * scale) / 2
    fun screen(p: Projected) = Offset(offsetX + (p.x - bounds.minX) * scale, offsetY + (bounds.maxY - p.y) * scale)
    fun screen(p: Vec3) = screen(camera.project(p))

    // The floor shadow: each part flattened onto the floor, in one flat tone so overlaps do not darken.
    val floorShade = lerp(colors.background, colors.shadow, 0.08f)
    segments.forEach { s ->
        val a = skeleton[s.from]
        val b = skeleton[s.to]
        capsule(screen(Vec3(a.x, 0f, a.z)), screen(Vec3(b.x, 0f, b.z)), s.fromRadius * scale * 1.2f, s.toRadius * scale * 1.2f, floorShade)
    }

    // Equipment first: the figure leans on it or stands on it, so it sits behind.
    props.forEach { prop -> drawProp(prop, camera, ::screen, colors) }

    val muscles = FigureMuscles.place(skeleton) { from, to ->
        segments.first { it.from == from && it.to == to }.let { it.fromRadius to it.toRadius }
    }.groupBy { it.shape.from to it.shape.to }
    val toward = camera.towardViewer

    // Far parts first, near parts last; far parts fade a little towards the background.
    val projected = skeleton.all.mapValues { camera.project(it.value) }
    val depths = projected.values.map { it.depth }
    val near = depths.max()
    val far = depths.min()
    fun nearness(depth: Float) = if (near - far < 1e-4f) 1f else ((depth - far) / (near - far)).coerceIn(0f, 1f)
    fun shade(color: Color, depth: Float) = lerp(lerp(color, colors.background, 0.45f), color, nearness(depth))

    val parts = segments.map { s ->
        val depth = (projected.getValue(s.from).depth + projected.getValue(s.to).depth) / 2
        s to if (s.inner) depth - 10f else depth
    } + listOf(null to projected.getValue(Joint.HEAD).depth)
    parts.sortedBy { it.second }.forEach { (s, depth) ->
        // A part carrying a worked muscle takes a little of its red, so it reads from a distance.
        val onPart = s?.let { muscles[it.from to it.to].orEmpty().map { m -> m.shape.muscle } }.orEmpty()
        val tint = when {
            onPart.any { it in primary } -> lerp(colors.body, colors.red, 0.3f)
            onPart.any { it in secondary } -> lerp(colors.body, colors.red, 0.12f)
            else -> colors.body
        }
        val color = shade(tint, depth)
        if (s == null) {
            drawCircle(color, Proportions.HEAD_RADIUS * scale, screen(projected.getValue(Joint.HEAD)))
            return@forEach
        }
        capsule(screen(projected.getValue(s.from)), screen(projected.getValue(s.to)), s.fromRadius * scale, s.toRadius * scale, color)
        // The muscles on this part that face the viewer, as raised lenses; worked ones in red.
        muscles[s.from to s.to].orEmpty().forEach { placed ->
            val facing = placed.normal.dot(toward)
            if (facing <= 0.08f) return@forEach
            val base = when (placed.shape.muscle) {
                in primary -> colors.red
                in secondary -> colors.lightRed
                else -> lerp(colors.body, colors.background, 0.3f)
            }
            val radius = (s.fromRadius + s.toRadius) / 2
            // A muscle seen side-on narrows; one facing the viewer spans most of the part.
            val halfWidth = radius * placed.shape.width * scale * (0.3f + 0.7f * facing) * 0.85f
            lens(screen(placed.start), screen(placed.end), halfWidth, shade(base, depth), shade(lerp(colors.body, colors.shadow, 0.25f), depth))
        }
    }
}

/** A box seen from the camera: only the faces turned towards the viewer, lit from above. */
private fun DrawScope.drawProp(prop: Prop, camera: FigureCamera, screen: (Vec3) -> Offset, colors: FigureColors) {
    val (a, b) = prop.min to prop.max
    fun corner(x: Boolean, y: Boolean, z: Boolean) = Vec3(if (x) b.x else a.x, if (y) b.y else a.y, if (z) b.z else a.z)
    val faces = listOf(
        Vec3.UP to listOf(corner(false, true, false), corner(true, true, false), corner(true, true, true), corner(false, true, true)),
        Vec3.FORWARD to listOf(corner(false, false, true), corner(true, false, true), corner(true, true, true), corner(false, true, true)),
        Vec3.BACK to listOf(corner(false, false, false), corner(true, false, false), corner(true, true, false), corner(false, true, false)),
        Vec3.RIGHT to listOf(corner(true, false, false), corner(true, false, true), corner(true, true, true), corner(true, true, false)),
        Vec3.RIGHT * -1f to listOf(corner(false, false, false), corner(false, false, true), corner(false, true, true), corner(false, true, false)),
    )
    val toward = camera.towardViewer
    faces.filter { (normal, _) -> normal.dot(toward) > 0f }.forEach { (normal, corners) ->
        val light = if (normal == Vec3.UP) 0.18f else 0.32f
        val path = Path().apply {
            corners.map(screen).forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            close()
        }
        drawPath(path, lerp(colors.background, colors.shadow, light))
    }
}

/** A muscle belly: a pointed oval from [a] to [b], [halfWidth] wide at its middle, with a thin edge. */
private fun DrawScope.lens(a: Offset, b: Offset, halfWidth: Float, fill: Color, edge: Color) {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 1f || halfWidth < 0.5f) return
    val nx = -dy / length
    val ny = dx / length
    val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
    // A quadratic curve reaches half its control point's offset, so the control sits at twice the width.
    val path = Path().apply {
        moveTo(a.x, a.y)
        quadraticTo(mid.x + nx * halfWidth * 2, mid.y + ny * halfWidth * 2, b.x, b.y)
        quadraticTo(mid.x - nx * halfWidth * 2, mid.y - ny * halfWidth * 2, a.x, a.y)
        close()
    }
    drawPath(path, fill)
    drawPath(path, edge, style = Stroke(width = 1.dp.toPx()))
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
