package io.github.gonbei774.calisthenicsmemory.data.figure

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The muscular body (owner, 2026-10-02: "a human model which is at least muscular"). Each part is
 * lofted: a run of cross-section rings along its bones, wider where the muscles bulge. Drawn from
 * any angle, neighbouring rings give the part's true outline, so a V-tapered torso, deltoids,
 * biceps, quads and calves read from every camera.
 */

/** One cross-section: an ellipse around [center] spanned by the scaled axes [a] and [b]. */
data class Ring(val center: Vec3, val a: Vec3, val b: Vec3) {
    fun point(theta: Float): Vec3 = center + a * cos(theta) + b * sin(theta)
}

/** A lofted body part, with the bone segments whose muscles sit on it. */
class BodyPart(val rings: List<Ring>, val segments: List<Pair<Joint, Joint>>)

/** Half-widths in metres along a part, from 0 at its first joint to 1 at its last. */
private class Profile(vararg val points: Pair<Float, Float>) {
    fun at(t: Float): Float {
        val i = points.indexOfLast { it.first <= t }.coerceIn(0, points.size - 2)
        val (t0, r0) = points[i]
        val (t1, r1) = points[i + 1]
        val f = ((t - t0) / (t1 - t0)).coerceIn(0f, 1f)
        // Smooth between points, so the muscles swell rather than step.
        val s = f * f * (3 - 2 * f)
        return r0 + (r1 - r0) * s
    }
}

object FigureBody {

    // Limb profiles: a deltoid cap and biceps, a forearm thick below the elbow, quads high on the
    // thigh, a calf bulge below the knee.
    private val upperArm = Profile(0f to 0.068f, 0.15f to 0.062f, 0.3f to 0.05f, 0.52f to 0.056f, 0.85f to 0.04f, 1f to 0.037f)
    private val forearm = Profile(0f to 0.04f, 0.2f to 0.048f, 0.65f to 0.031f, 1f to 0.025f)
    private val thigh = Profile(0f to 0.1f, 0.3f to 0.094f, 0.62f to 0.076f, 0.9f to 0.056f, 1f to 0.05f)
    private val shin = Profile(0f to 0.05f, 0.22f to 0.064f, 0.45f to 0.05f, 0.82f to 0.032f, 1f to 0.03f)
    private val neck = Profile(0f to 0.058f, 1f to 0.05f)

    /**
     * The torso's cross-sections from the pelvis (0) to the base of the neck (1): side-to-side
     * half-width and front-to-back half-depth. Narrow waist, broad chest and lats, sloping traps.
     */
    private val torsoWidth = Profile(0f to 0.155f, 0.18f to 0.138f, 0.38f to 0.127f, 0.62f to 0.18f, 0.82f to 0.21f, 0.94f to 0.18f, 1f to 0.09f)
    private val torsoDepth = Profile(0f to 0.1f, 0.2f to 0.094f, 0.4f to 0.09f, 0.65f to 0.112f, 0.82f to 0.122f, 0.94f to 0.098f, 1f to 0.068f)

    private val torsoStations = (0..20).map { it / 20f }
    private val limbStations = (0..14).map { it / 14f }

    /** Every part for one pose. */
    fun parts(s: SolvedSkeleton): List<BodyPart> {
        val torsoRight = (s[Joint.RIGHT_SHOULDER] - s[Joint.LEFT_SHOULDER]).normalized()
        val torsoUp = (s[Joint.NECK] - s[Joint.PELVIS]).normalized()
        val torsoForward = torsoRight.cross(torsoUp).normalized()
        val hipRight = (s[Joint.RIGHT_HIP] - s[Joint.LEFT_HIP]).normalized()
        val hipForward = hipRight.cross(torsoUp).normalized()

        val parts = mutableListOf<BodyPart>()

        // The torso follows the spine through the chest joint.
        val pelvis = s[Joint.PELVIS]
        val chest = s[Joint.CHEST]
        val neckBase = s[Joint.NECK]
        val split = 0.62f
        val torsoRings = torsoStations.map { t ->
            val center = if (t <= split) pelvis + (chest - pelvis) * (t / split) else chest + (neckBase - chest) * ((t - split) / (1 - split))
            // The lower torso turns with the hips, the upper with the shoulders.
            val right = if (t < 0.3f) hipRight else torsoRight
            val forward = if (t < 0.3f) hipForward else torsoForward
            // The chest sits a little forward of the spine.
            val chestForward = forward * (if (t in 0.55f..0.9f) 0.012f else 0f)
            Ring(center + chestForward, right * torsoWidth.at(t), forward * torsoDepth.at(t))
        }
        parts += BodyPart(torsoRings, listOf(Joint.PELVIS to Joint.CHEST, Joint.CHEST to Joint.NECK))

        parts += limb(s, Joint.NECK, Joint.HEAD, neck, torsoForward, end = 0.55f)
        parts += head(s, torsoForward)

        for (side in listOf("LEFT", "RIGHT")) {
            fun j(name: String) = Joint.valueOf("${side}_$name")
            parts += limb(s, j("SHOULDER"), j("ELBOW"), upperArm, torsoForward)
            parts += limb(s, j("ELBOW"), j("WRIST"), forearm, torsoForward)
            parts += flat(s, j("WRIST"), j("HAND"), 0.042f, 0.018f, torsoForward)
            parts += limb(s, j("HIP"), j("KNEE"), thigh, hipForward)
            parts += limb(s, j("KNEE"), j("ANKLE"), shin, hipForward)
            parts += flat(s, j("ANKLE"), j("TOE"), 0.038f, 0.028f, Vec3.UP, tip = 0.6f)
        }
        return parts
    }

    /** Two directions at right angles to [axis], the first as close to [reference] as possible. */
    private fun around(axis: Vec3, reference: Vec3): Pair<Vec3, Vec3> {
        var u = reference - axis * reference.dot(axis)
        if (u.length() < 0.2f) u = Vec3.UP - axis * Vec3.UP.dot(axis)
        if (u.length() < 0.2f) u = Vec3.FORWARD - axis * Vec3.FORWARD.dot(axis)
        u = u.normalized()
        return u to axis.cross(u).normalized()
    }

    private fun limb(s: SolvedSkeleton, from: Joint, to: Joint, profile: Profile, reference: Vec3, end: Float = 1f): BodyPart {
        val a = s[from]
        val b = s[to]
        val (u, v) = around((b - a).normalized(), reference)
        val rings = limbStations.map { t ->
            val r = profile.at(t)
            Ring(a + (b - a) * (t * end), u * r, v * r)
        }
        return BodyPart(rings, listOf(from to to))
    }

    /** Hands and feet: flattened, tapering towards the fingers or toes ([tip] of the base width). */
    private fun flat(s: SolvedSkeleton, from: Joint, to: Joint, width: Float, thickness: Float, reference: Vec3, tip: Float = 0.8f): BodyPart {
        val a = s[from]
        val b = s[to]
        val (u, v) = around((b - a).normalized(), reference)
        val rings = listOf(0f, 0.5f, 1f).map { t ->
            val k = 1f - (1f - tip) * t
            Ring(a + (b - a) * t, v * (width * k), u * (thickness * k))
        }
        return BodyPart(rings, listOf(from to to))
    }

    /** An egg-shaped head, slightly deeper than wide, rounded top and bottom. */
    private fun head(s: SolvedSkeleton, reference: Vec3): BodyPart {
        val center = s[Joint.HEAD]
        val up = (center - s[Joint.NECK]).normalized()
        val (forward, side) = around(up, reference)
        val height = Proportions.HEAD_RADIUS * 1.05f
        val rings = listOf(-1f, -0.85f, -0.55f, -0.2f, 0.2f, 0.55f, 0.85f, 1f).map { h ->
            val k = sqrt((1 - h * h).coerceAtLeast(0f))
            // The face sits forward of the centre at jaw height.
            val jaw = if (h < 0f) forward * (0.012f * -h) else Vec3.ZERO
            Ring(center + up * (h * height) + jaw, side * (0.085f * k), forward * (0.098f * k))
        }
        return BodyPart(rings, listOf(Joint.NECK to Joint.HEAD))
    }

    /** The radius of the part at [t] along the segment, for placing muscles on its surface. */
    fun radiusAt(from: Joint, to: Joint, t: Float): Float {
        val name = from.name.substringAfter('_')
        return when {
            from == Joint.PELVIS -> torsoDepth.at(t * 0.62f)
            from == Joint.CHEST -> torsoDepth.at(0.62f + t * 0.38f)
            from == Joint.NECK -> neck.at(t)
            name == "SHOULDER" -> upperArm.at(t)
            name == "ELBOW" -> forearm.at(t)
            name == "HIP" -> thigh.at(t)
            name == "KNEE" -> shin.at(t)
            else -> 0.03f
        }
    }
}
