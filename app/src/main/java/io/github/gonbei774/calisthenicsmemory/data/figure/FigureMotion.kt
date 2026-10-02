package io.github.gonbei774.calisthenicsmemory.data.figure

import kotlin.math.cos
import kotlin.math.PI

/** Equipment the figure uses, such as a bench or a wall: a box between two opposite corners. */
data class Prop(val min: Vec3, val max: Vec3)

/** A pose at a point in the loop, from 0 (start) to 1 (end, which should match the start). */
data class Keyframe(val at: Float, val pose: FigurePose)

/**
 * One exercise's demonstration: a looping sequence of keyframes, its length, and the camera it is
 * best seen from (ADR 0008, decision 2).
 */
data class FigureMotion(
    val keyframes: List<Keyframe>,
    val durationMillis: Int,
    val camera: FigureCamera = FigureCamera(),
    /** Where in the loop the hardest point is, shown when animations are off. */
    val stillAt: Float = 0.5f,
    /** The bench, wall or bar the motion uses, drawn behind the figure. */
    val props: List<Prop> = emptyList(),
) {
    init {
        require(keyframes.size >= 2) { "A motion needs at least two keyframes" }
        require(keyframes.first().at == 0f && keyframes.last().at == 1f) { "Keyframes run from 0 to 1" }
        require(keyframes.zipWithNext().all { (a, b) -> a.at < b.at }) { "Keyframes are in order" }
    }

    /** The pose at [progress] through the loop, eased in and out between keyframes. */
    fun poseAt(progress: Float): FigurePose {
        val t = progress.coerceIn(0f, 1f)
        // The ends are the keyframes themselves, so the loop closes without rounding drift.
        if (t >= 1f) return keyframes.last().pose
        val index = keyframes.indexOfLast { it.at <= t }.coerceAtMost(keyframes.size - 2)
        val from = keyframes[index]
        val to = keyframes[index + 1]
        val local = ((t - from.at) / (to.at - from.at)).coerceIn(0f, 1f)
        return lerp(from.pose, to.pose, ease(local))
    }

    /** The pose shown when animations are off: the hardest point of the movement. */
    val stillPose: FigurePose get() = poseAt(stillAt)

    companion object {
        /** Slow at both ends, like a controlled repetition. */
        fun ease(t: Float): Float = ((1 - cos(t * PI)) / 2).toFloat()

        fun lerp(a: FigurePose, b: FigurePose, t: Float) = FigurePose(
            pelvis = lerp(a.pelvis, b.pelvis, t),
            bodyPitch = lerp(a.bodyPitch, b.bodyPitch, t),
            bodyYaw = lerp(a.bodyYaw, b.bodyYaw, t),
            bodyRoll = lerp(a.bodyRoll, b.bodyRoll, t),
            spineFlex = lerp(a.spineFlex, b.spineFlex, t),
            neckFlex = lerp(a.neckFlex, b.neckFlex, t),
            leftArm = lerp(a.leftArm, b.leftArm, t),
            rightArm = lerp(a.rightArm, b.rightArm, t),
            leftLeg = lerp(a.leftLeg, b.leftLeg, t),
            rightLeg = lerp(a.rightLeg, b.rightLeg, t),
            leftHandPin = lerpPin(a.leftHandPin, b.leftHandPin, t),
            rightHandPin = lerpPin(a.rightHandPin, b.rightHandPin, t),
            leftFootPin = lerpPin(a.leftFootPin, b.leftFootPin, t),
            rightFootPin = lerpPin(a.rightFootPin, b.rightFootPin, t),
        )

        private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        private fun lerp(a: Vec3, b: Vec3, t: Float) = a + (b - a) * t
        private fun lerp(a: LimbAngles, b: LimbAngles, t: Float) =
            LimbAngles(lerp(a.flex, b.flex, t), lerp(a.abduct, b.abduct, t), lerp(a.bend, b.bend, t))

        /** A pin held in both keyframes moves between them; one held in only one stays where it is. */
        private fun lerpPin(a: Vec3?, b: Vec3?, t: Float): Vec3? = when {
            a != null && b != null -> lerp(a, b, t)
            else -> a ?: b
        }
    }
}
