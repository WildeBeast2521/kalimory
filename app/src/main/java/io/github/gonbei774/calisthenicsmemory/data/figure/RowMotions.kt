package io.github.gonbei774.calisthenicsmemory.data.figure

import io.github.gonbei774.calisthenicsmemory.data.figure.Plank.ARM
import io.github.gonbei774.calisthenicsmemory.data.figure.Plank.Pivot
import io.github.gonbei774.calisthenicsmemory.data.figure.Plank.pelvisAt
import io.github.gonbei774.calisthenicsmemory.data.figure.Plank.pitchFor
import io.github.gonbei774.calisthenicsmemory.data.figure.Plank.shoulderAt
import io.github.gonbei774.calisthenicsmemory.data.figure.Plank.toes

/**
 * The row chain's motions (ADR 0008, decision 3): the push-up plank turned over. The straight body
 * leans back from the heels, hands pinned on a bar in front of the chest, and pulls the chest up.
 */
internal object RowMotions {

    /** The bar sits over the shoulders at [designPitch]; the arms hang straight at the bottom. */
    private fun row(
        pivot: Pivot,
        barHeight: Float,
        designPitch: Float,
        halfGrip: Float = Proportions.SHOULDER_HALF_WIDTH + 0.04f,
        topReach: Float = 0.24f,
    ): Pair<FigureMotion, Vec3> {
        val barZ = shoulderAt(pivot, designPitch).z
        val hand = Vec3(halfGrip, barHeight, barZ)
        val bottom = pitchFor(pivot, hand, ARM * 0.97f, -89f..-3f)
        val top = pitchFor(pivot, hand, topReach, -89f..-3f)
        fun pose(pitch: Float) = pivot.feet(
            FigurePose(
                pelvis = pelvisAt(pivot, pitch),
                bodyPitch = pitch,
                neckFlex = 20f,
                leftHandPin = Vec3(-hand.x, hand.y, hand.z),
                rightHandPin = hand,
            )
        )
        val motion = FigureMotion(
            listOf(Keyframe(0f, pose(bottom)), Keyframe(0.5f, pose(top)), Keyframe(1f, pose(bottom))),
            durationMillis = 2800,
            camera = FigureCamera(yaw = 60f, pitch = 14f),
            props = listOf(barAt(barHeight, barZ)),
        )
        return motion to hand
    }

    private fun barAt(height: Float, z: Float) = Prop(Vec3(-0.75f, height, z - 0.02f), Vec3(0.75f, height + 0.035f, z + 0.02f))

    /** Heels on the floor, toes up, ahead of the bar. */
    private fun heels(height: Float = 0f) = toes(height, z = 0.9f)

    val incline = row(heels(), barHeight = 1.05f, designPitch = -40f).first

    val horizontal = row(heels(), barHeight = 0.9f, designPitch = -68f).first

    val feetElevated = run {
        val (motion, _) = row(heels(0.45f), barHeight = 1.05f, designPitch = -82f)
        motion.copy(props = motion.props + Prop(Vec3(-0.35f, 0f, 0.75f), Vec3(0.35f, 0.45f, 1.15f)))
    }

    /** A wide grip; the body pulls towards one hand, then the other, the far arm nearly straight. */
    val archer: FigureMotion = run {
        val (base, _) = row(heels(), barHeight = 0.9f, designPitch = -68f, halfGrip = 0.5f, topReach = 0.32f)
        val bottom = base.keyframes[0].pose
        val top = base.keyframes[1].pose
        fun toward(sign: Float) = top.copy(pelvis = top.pelvis + Vec3(sign * 0.12f, 0f, 0f), bodyRoll = sign * 8f)
        FigureMotion(
            listOf(Keyframe(0f, bottom), Keyframe(0.25f, toward(1f)), Keyframe(0.5f, bottom), Keyframe(0.75f, toward(-1f)), Keyframe(1f, bottom)),
            durationMillis = 5200,
            camera = base.camera,
            stillAt = 0.25f,
            props = base.props,
        )
    }

    /** One hand on the bar in line with the chest, feet wide against the twist. */
    val oneArm: FigureMotion = run {
        val (base, _) = row(toes(0f, z = 0.9f, feetHalfWidth = 0.3f), barHeight = 0.9f, designPitch = -68f, halfGrip = 0.06f, topReach = 0.26f)
        fun oneHanded(pose: FigurePose) = pose.copy(
            leftHandPin = null,
            leftArm = LimbAngles(flex = 10f, abduct = 20f, bend = 20f),
            bodyRoll = -10f,
        )
        base.copy(keyframes = base.keyframes.map { Keyframe(it.at, oneHanded(it.pose)) }, durationMillis = 3200)
    }

    val byStep: Map<String, FigureMotion> = mapOf(
        "row.incline" to incline,
        "row.horizontal" to horizontal,
        "row.feet_elevated" to feetElevated,
        "row.archer" to archer,
        "row.one_arm" to oneArm,
    )
}
