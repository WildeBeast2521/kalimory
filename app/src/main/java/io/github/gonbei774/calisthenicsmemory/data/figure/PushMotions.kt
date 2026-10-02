package io.github.gonbei774.calisthenicsmemory.data.figure

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The push-up chain's motions (ADR 0008, decision 3). Every variation is the same plank: a straight
 * body turning about its toes (or knees), with the hands pinned on the floor, a bench or a wall.
 * The top and bottom are found from the arm's reach, so each variation's proportions stay right.
 */
internal object PushMotions {

    private const val SHOULDER_DROP = 0.03f
    private const val ARM = Proportions.UPPER_ARM + Proportions.FOREARM

    /** Where the plank turns: the toes on the floor or a bench, or the knees. */
    private class Pivot(val point: Vec3, val toHip: Float, val feet: (FigurePose) -> FigurePose)

    /** On the toes, feet [feetHalfWidth] from the middle; wider feet shorten the straight leg's reach. */
    private fun toes(height: Float, z: Float = -0.82f, feetHalfWidth: Float = 0.1f): Pivot {
        val spread = feetHalfWidth - Proportions.PELVIS_HALF_WIDTH
        val leg = Proportions.THIGH + Proportions.SHIN
        return Pivot(
            point = Vec3(0f, height + Proportions.ANKLE_HEIGHT, z),
            toHip = sqrt(leg * leg - spread * spread) * 0.995f,
        ) { it.copy(leftFootPin = Vec3(-feetHalfWidth, height, z), rightFootPin = Vec3(feetHalfWidth, height, z)) }
    }

    /** On the knees, shins raised about 30 degrees, so the solver bends each knee onto the floor. */
    private fun knees(z: Float = -0.43f): Pivot {
        val knee = Vec3(0f, 0.06f, z)
        val foot = knee + Vec3(0f, Proportions.SHIN * 0.5f - Proportions.ANKLE_HEIGHT, -Proportions.SHIN * 0.866f)
        return Pivot(point = knee, toHip = Proportions.THIGH) {
            it.copy(leftFootPin = foot + Vec3(-0.1f, 0f, 0f), rightFootPin = foot + Vec3(0.1f, 0f, 0f))
        }
    }

    private fun pelvisAt(pivot: Pivot, pitch: Float) =
        pivot.point + Vec3(0f, cos(pitch.rad()), sin(pitch.rad())) * pivot.toHip

    private fun shoulderAt(pivot: Pivot, pitch: Float) =
        pelvisAt(pivot, pitch) + Vec3(0f, cos(pitch.rad()), sin(pitch.rad())) * (Proportions.TORSO - SHOULDER_DROP)

    /**
     * The body pitch at which the shoulders are [reach] from the hand, searched from upright to
     * [maxPitch].
     */
    private fun pitchFor(pivot: Pivot, hand: Vec3, reach: Float, maxPitch: Float): Float {
        var best = 5f
        var bestError = Float.MAX_VALUE
        var pitch = 5f
        while (pitch <= maxPitch) {
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

    /**
     * A push-up variation. [handAt] places the right hand for a given top shoulder position (the
     * left mirrors it); [bottomReach] is how close the shoulders come to the hands.
     */
    private fun plank(
        pivot: Pivot,
        topPitch: Float,
        handAt: (shoulder: Vec3) -> Vec3,
        bottomReach: Float,
        camera: FigureCamera = FigureCamera(yaw = 55f, pitch = 14f),
        durationMillis: Int = 2800,
        props: List<Prop> = emptyList(),
        // Only a decline push-up, with the feet raised, tips past flat.
        maxPitch: Float = 88f,
    ): FigureMotion {
        val hand = handAt(shoulderAt(pivot, topPitch))
        val top = pitchFor(pivot, hand, ARM * 0.97f, maxPitch)
        val bottom = pitchFor(pivot, hand, bottomReach, maxPitch)
        fun pose(pitch: Float) = pivot.feet(
            FigurePose(
                pelvis = pelvisAt(pivot, pitch),
                bodyPitch = pitch,
                neckFlex = -15f,
                leftHandPin = Vec3(-hand.x, hand.y, hand.z),
                rightHandPin = hand,
            )
        )
        return FigureMotion(listOf(Keyframe(0f, pose(top)), Keyframe(0.5f, pose(bottom)), Keyframe(1f, pose(top))), durationMillis, camera, props = props)
    }

    private val shoulderWidth = Proportions.SHOULDER_HALF_WIDTH + 0.03f

    val wall = run {
        // The wall stands an arm's length ahead of the shoulders when leaning a little.
        val pivot = toes(0f, z = 0f)
        val lean = shoulderAt(pivot, 18f)
        plank(
            pivot, topPitch = 18f,
            handAt = { Vec3(shoulderWidth, lean.y - 0.05f, lean.z + ARM * 0.95f) },
            bottomReach = 0.3f,
            // Seen from the side and a little behind: the wall stands in front of the figure.
            camera = FigureCamera(yaw = 115f, pitch = 12f),
            props = listOf(Prop(Vec3(-0.7f, 0f, lean.z + ARM * 0.95f), Vec3(0.7f, 1.9f, lean.z + ARM * 0.95f + 0.1f))),
        )
    }

    val incline = run {
        val shoulder = shoulderAt(toes(0f, z = -0.6f), 50f)
        plank(
            toes(0f, z = -0.6f), topPitch = 50f, handAt = { Vec3(shoulderWidth, 0.45f, it.z + 0.02f) }, bottomReach = 0.22f,
            props = listOf(Prop(Vec3(-0.45f, 0f, shoulder.z - 0.15f), Vec3(0.45f, 0.45f, shoulder.z + 0.25f))),
        )
    }

    val knee = plank(knees(), topPitch = 60f, handAt = { Vec3(shoulderWidth, 0f, it.z + 0.02f) }, bottomReach = 0.3f)

    val full = plank(toes(0f), topPitch = 72f, handAt = { Vec3(shoulderWidth, 0f, it.z + 0.02f) }, bottomReach = 0.22f)

    // Hands together under the chest, a little lower down the body.
    val diamond = plank(toes(0f), topPitch = 72f, handAt = { Vec3(0.06f, 0f, it.z - 0.06f) }, bottomReach = 0.2f)

    val decline = plank(toes(0.45f, z = -0.9f), topPitch = 80f, handAt = { Vec3(shoulderWidth, 0f, it.z + 0.04f) }, bottomReach = 0.3f, maxPitch = 115f,
        props = listOf(Prop(Vec3(-0.45f, 0f, -1.15f), Vec3(0.45f, 0.45f, -0.75f))))

    /** Wide hands; the body lowers towards one hand, then the other, keeping the far arm straight. */
    val archer: FigureMotion = run {
        val base = plank(toes(0f), topPitch = 72f, handAt = { Vec3(0.46f, 0f, it.z + 0.02f) }, bottomReach = 0.3f)
        val top = base.keyframes[0].pose
        val bottom = base.keyframes[1].pose
        fun toward(sign: Float) = bottom.copy(pelvis = bottom.pelvis + Vec3(sign * 0.16f, 0f, 0f), bodyRoll = -sign * 6f)
        FigureMotion(
            listOf(Keyframe(0f, top), Keyframe(0.25f, toward(1f)), Keyframe(0.5f, top), Keyframe(0.75f, toward(-1f)), Keyframe(1f, top)),
            durationMillis = 5200,
            camera = base.camera,
            stillAt = 0.25f,
        )
    }

    /** One hand under the chest, the other behind the back, feet wide for balance. */
    val oneArm: FigureMotion = run {
        val base = plank(toes(0f, feetHalfWidth = 0.32f), topPitch = 72f, handAt = { Vec3(0.08f, 0f, it.z) }, bottomReach = 0.26f)
        fun oneHanded(pose: FigurePose) = pose.copy(
            leftHandPin = null,
            leftArm = LimbAngles(flex = -35f, abduct = 10f, bend = 100f),
            bodyRoll = 8f,
        )
        FigureMotion(base.keyframes.map { Keyframe(it.at, oneHanded(it.pose)) }, durationMillis = 3400, camera = base.camera)
    }

    val byStep: Map<String, FigureMotion> = mapOf(
        "push.wall" to wall,
        "push.incline" to incline,
        "push.knee" to knee,
        "push.full" to full,
        "push.diamond" to diamond,
        "push.decline" to decline,
        "push.archer" to archer,
        "push.one_arm" to oneArm,
    )
}

/** The demonstration for each catalogue step that has one so far (ADR 0008, decision 3). */
object StepMotions {
    private val all: Map<String, FigureMotion> = PushMotions.byStep + SquatMotions.byStep

    fun forStep(stepId: String): FigureMotion? = all[stepId]

    val stepIds: Set<String> get() = all.keys
}
