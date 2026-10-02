package io.github.gonbei774.calisthenicsmemory.data.figure

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A straight body turning about its feet or knees, as in push-ups and rows (ADR 0008, decision 3).
 * The pelvis and shoulders follow from the body pitch, so a motion's top and bottom can be found
 * from the arm's reach.
 */
internal object Plank {

    const val SHOULDER_DROP = 0.03f
    const val ARM = Proportions.UPPER_ARM + Proportions.FOREARM

    /** Where the plank turns: the toes on the floor or a bench, or the knees. */
    class Pivot(val point: Vec3, val toHip: Float, val feet: (FigurePose) -> FigurePose)

    /** On the toes, feet [feetHalfWidth] from the middle; wider feet shorten the straight leg's reach. */
    fun toes(height: Float, z: Float = -0.82f, feetHalfWidth: Float = 0.1f): Pivot {
        val spread = feetHalfWidth - Proportions.PELVIS_HALF_WIDTH
        val leg = Proportions.THIGH + Proportions.SHIN
        return Pivot(
            point = Vec3(0f, height + Proportions.ANKLE_HEIGHT, z),
            toHip = sqrt(leg * leg - spread * spread) * 0.995f,
        ) { it.copy(leftFootPin = Vec3(-feetHalfWidth, height, z), rightFootPin = Vec3(feetHalfWidth, height, z)) }
    }

    /** On the knees, shins raised about 30 degrees, so the solver bends each knee onto the floor. */
    fun knees(z: Float = -0.43f): Pivot {
        val knee = Vec3(0f, 0.06f, z)
        val foot = knee + Vec3(0f, Proportions.SHIN * 0.5f - Proportions.ANKLE_HEIGHT, -Proportions.SHIN * 0.866f)
        return Pivot(point = knee, toHip = Proportions.THIGH) {
            it.copy(leftFootPin = foot + Vec3(-0.1f, 0f, 0f), rightFootPin = foot + Vec3(0.1f, 0f, 0f))
        }
    }

    fun pelvisAt(pivot: Pivot, pitch: Float) =
        pivot.point + Vec3(0f, cos(pitch.rad()), sin(pitch.rad())) * pivot.toHip

    fun shoulderAt(pivot: Pivot, pitch: Float) =
        pelvisAt(pivot, pitch) + Vec3(0f, cos(pitch.rad()), sin(pitch.rad())) * (Proportions.TORSO - SHOULDER_DROP)

    /**
     * The body pitch within [range] at which the shoulders are [reach] from the hand. Positive
     * pitches lean forward (push-ups), negative ones lean back (rows).
     */
    fun pitchFor(pivot: Pivot, hand: Vec3, reach: Float, range: ClosedFloatingPointRange<Float>): Float {
        var best = range.start
        var bestError = Float.MAX_VALUE
        var pitch = range.start
        while (pitch <= range.endInclusive) {
            val s = shoulderAt(pivot, pitch)
            val dx = hand.x - Proportions.SHOULDER_HALF_WIDTH
            val d = sqrt(dx * dx + (hand.y - s.y) * (hand.y - s.y) + (hand.z - s.z) * (hand.z - s.z))
            val error = kotlin.math.abs(d - reach)
            if (error < bestError) {
                bestError = error
                best = pitch
            }
            pitch += 0.25f
        }
        return best
    }
}
