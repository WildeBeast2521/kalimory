package io.github.gonbei774.calisthenicsmemory.data.figure

import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle

/** Which way a muscle faces on its body part, relative to the body. */
enum class Facing { FRONT, BACK, OUTER, INNER }

/**
 * A muscle drawn on a body part (the owner's anatomical style, 2026-10-01): a lens along the part
 * from [start] to [end] (0 at [from], 1 at [to]), on its [facing] side, [width] times the part's
 * radius wide. [lateral] moves it sideways in metres, for paired muscles on the torso.
 */
data class MuscleShape(
    val muscle: Muscle,
    val from: Joint,
    val to: Joint,
    val start: Float,
    val end: Float,
    val facing: Facing,
    val width: Float,
    val lateral: Float = 0f,
)

/** A muscle shape placed in the world for one pose. */
data class PlacedMuscle(
    val shape: MuscleShape,
    /** The lens's two ends on the body's surface. */
    val start: Vec3,
    val end: Vec3,
    /** The surface direction the muscle faces. */
    val normal: Vec3,
)

object FigureMuscles {

    private fun limb(side: String, muscle: Muscle, from: String, to: String, start: Float, end: Float, facing: Facing, width: Float) =
        MuscleShape(muscle, Joint.valueOf("${side}_$from"), Joint.valueOf("${side}_$to"), start, end, facing, width)

    /** Every muscle shape on the figure, for both sides. */
    val shapes: List<MuscleShape> = buildList {
        // Torso, on the pelvis-to-chest and chest-to-neck parts.
        add(MuscleShape(Muscle.ABDOMINALS, Joint.PELVIS, Joint.CHEST, 0.12f, 1.0f, Facing.FRONT, 0.55f))
        add(MuscleShape(Muscle.LOWER_BACK, Joint.PELVIS, Joint.CHEST, 0.0f, 0.6f, Facing.BACK, 0.6f))
        for (sign in listOf(-1f, 1f)) {
            add(MuscleShape(Muscle.LATS, Joint.PELVIS, Joint.CHEST, 0.5f, 1.05f, Facing.BACK, 0.45f, lateral = sign * 0.075f))
            add(MuscleShape(Muscle.CHEST, Joint.CHEST, Joint.NECK, 0.0f, 0.75f, Facing.FRONT, 0.45f, lateral = sign * 0.065f))
        }
        add(MuscleShape(Muscle.MIDDLE_BACK, Joint.CHEST, Joint.NECK, 0.0f, 0.6f, Facing.BACK, 0.45f))
        add(MuscleShape(Muscle.TRAPS, Joint.CHEST, Joint.NECK, 0.5f, 1.05f, Facing.BACK, 0.7f))
        add(MuscleShape(Muscle.NECK, Joint.NECK, Joint.HEAD, 0.0f, 0.6f, Facing.FRONT, 0.7f))
        for (side in listOf("LEFT", "RIGHT")) {
            add(limb(side, Muscle.SHOULDERS, "SHOULDER", "ELBOW", -0.08f, 0.4f, Facing.OUTER, 1.0f))
            add(limb(side, Muscle.BICEPS, "SHOULDER", "ELBOW", 0.25f, 0.85f, Facing.FRONT, 0.8f))
            add(limb(side, Muscle.TRICEPS, "SHOULDER", "ELBOW", 0.2f, 0.85f, Facing.BACK, 0.8f))
            add(limb(side, Muscle.FOREARMS, "ELBOW", "WRIST", 0.05f, 0.6f, Facing.FRONT, 0.75f))
            add(limb(side, Muscle.FOREARMS, "ELBOW", "WRIST", 0.05f, 0.6f, Facing.BACK, 0.75f))
            add(limb(side, Muscle.GLUTES, "HIP", "KNEE", -0.08f, 0.3f, Facing.BACK, 1.1f))
            add(limb(side, Muscle.QUADRICEPS, "HIP", "KNEE", 0.12f, 0.85f, Facing.FRONT, 0.9f))
            add(limb(side, Muscle.HAMSTRINGS, "HIP", "KNEE", 0.2f, 0.85f, Facing.BACK, 0.8f))
            add(limb(side, Muscle.ADDUCTORS, "HIP", "KNEE", 0.05f, 0.5f, Facing.INNER, 0.6f))
            add(limb(side, Muscle.ABDUCTORS, "HIP", "KNEE", -0.02f, 0.35f, Facing.OUTER, 0.6f))
            add(limb(side, Muscle.CALVES, "KNEE", "ANKLE", 0.08f, 0.55f, Facing.BACK, 0.9f))
        }
    }

    /**
     * Places every shape on the solved pose. A part's front is the body's front, turned to sit at a
     * right angle to the part; a part pointing straight forward takes "up" as its front instead.
     */
    fun place(skeleton: SolvedSkeleton, radius: (Joint, Joint) -> Pair<Float, Float>): List<PlacedMuscle> {
        val torsoRight = (skeleton[Joint.RIGHT_SHOULDER] - skeleton[Joint.LEFT_SHOULDER]).normalized()
        val torsoUp = (skeleton[Joint.NECK] - skeleton[Joint.PELVIS]).normalized()
        val torsoForward = torsoRight.cross(torsoUp).normalized()
        val hipRight = (skeleton[Joint.RIGHT_HIP] - skeleton[Joint.LEFT_HIP]).normalized()
        val hipForward = hipRight.cross(torsoUp).normalized()

        return shapes.map { shape ->
            val a = skeleton[shape.from]
            val b = skeleton[shape.to]
            val axis = (b - a).normalized()
            val isLeg = shape.from.name.contains("HIP") || shape.from.name.contains("KNEE")
            val reference = if (isLeg) hipForward else torsoForward
            var front = reference - axis * reference.dot(axis)
            if (front.length() < 0.3f) front = torsoUp - axis * torsoUp.dot(axis)
            front = front.normalized()
            // Outward is away from the body's midline, at a right angle to the part and its front.
            val sideSign = when {
                shape.from.name.startsWith("LEFT") -> -1f
                shape.from.name.startsWith("RIGHT") -> 1f
                else -> 1f
            }
            val outward = (axis.cross(front) * -1f).let { if (it.dot(torsoRight) * sideSign < 0) it * -1f else it }.normalized()
            val normal = when (shape.facing) {
                Facing.FRONT -> front
                Facing.BACK -> front * -1f
                Facing.OUTER -> outward
                Facing.INNER -> outward * -1f
            }
            val (radiusA, radiusB) = radius(shape.from, shape.to)
            fun surface(t: Float): Vec3 {
                val r = radiusA + (radiusB - radiusA) * t.coerceIn(0f, 1f)
                return a + (b - a) * t + normal * (r * 0.7f) + torsoRight * shape.lateral
            }
            PlacedMuscle(shape, surface(shape.start), surface(shape.end), normal)
        }
    }
}
