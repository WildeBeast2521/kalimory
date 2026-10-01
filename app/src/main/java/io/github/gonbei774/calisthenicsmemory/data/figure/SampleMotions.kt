package io.github.gonbei774.calisthenicsmemory.data.figure

import kotlin.math.cos
import kotlin.math.sin

/**
 * Reference motions for the figure preview while the engine is built. The catalogue's own motions
 * come in later slices (ADR 0008, decision 3).
 */
object SampleMotions {

    private val leftFoot = Vec3(-0.12f, 0f, 0f)
    private val rightFoot = Vec3(0.12f, 0f, 0f)

    /** A bodyweight squat: feet pinned, hips back and down, arms forward for balance. */
    val squat: FigureMotion = run {
        val standing = FigurePose(
            pelvis = Vec3(0f, 0.93f, 0f),
            leftFootPin = leftFoot,
            rightFootPin = rightFoot,
            leftArm = LimbAngles(flex = 10f, bend = 10f),
            rightArm = LimbAngles(flex = 10f, bend = 10f),
        )
        val bottom = standing.copy(
            pelvis = Vec3(0f, 0.50f, -0.20f),
            spineFlex = 38f,
            neckFlex = -25f,
            leftArm = LimbAngles(flex = 95f, abduct = 5f),
            rightArm = LimbAngles(flex = 95f, abduct = 5f),
        )
        FigureMotion(listOf(Keyframe(0f, standing), Keyframe(0.5f, bottom), Keyframe(1f, standing)), durationMillis = 3200)
    }

    /**
     * A push-up: hands and toes pinned, the straight body turning about the toes. Seen more from the
     * head side, so the body line and the elbows both show.
     */
    val pushUp: FigureMotion = run {
        val toes = Vec3(0f, 0f, -0.82f)
        fun pose(pitch: Float): FigurePose {
            val legLength = Proportions.THIGH + Proportions.SHIN
            val rad = pitch.rad()
            val pelvis = toes + Vec3.UP * Proportions.ANKLE_HEIGHT + Vec3(0f, cos(rad), sin(rad)) * legLength
            return FigurePose(
                pelvis = pelvis,
                bodyPitch = pitch,
                neckFlex = -15f,
                leftFootPin = toes + Vec3(-0.1f, 0f, 0f),
                rightFootPin = toes + Vec3(0.1f, 0f, 0f),
                leftHandPin = Vec3(-0.22f, 0f, 0.46f),
                rightHandPin = Vec3(0.22f, 0f, 0.46f),
            )
        }
        val top = pose(pitch = 72f)
        val bottom = pose(pitch = 84f)
        FigureMotion(
            listOf(Keyframe(0f, top), Keyframe(0.5f, bottom), Keyframe(1f, top)),
            durationMillis = 2800,
            camera = FigureCamera(yaw = 55f, pitch = 14f),
        )
    }

    val all: List<Pair<String, FigureMotion>> = listOf("Squat" to squat, "Push-up" to pushUp)
}
