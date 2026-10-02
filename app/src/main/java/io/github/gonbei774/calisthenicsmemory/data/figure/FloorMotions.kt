package io.github.gonbei774.calisthenicsmemory.data.figure

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * The hip hinge, core and leg raise chains (ADR 0008, decision 3). Most lie face up: a body pitch
 * of -90 puts the head towards -z and the chest to the ceiling. Nordic curls kneel, and the hanging
 * leg raises reuse the pull-up bar.
 */
internal object FloorMotions {

    private fun loop(a: FigurePose, b: FigurePose, durationMillis: Int = 3000, camera: FigureCamera = SIDE, props: List<Prop> = emptyList(), stillAt: Float = 0.5f) =
        FigureMotion(listOf(Keyframe(0f, a), Keyframe(0.5f, b), Keyframe(1f, a)), durationMillis, camera, stillAt, props)

    /** A hold that only breathes: the pelvis settles half a centimetre and back. */
    private fun hold(pose: FigurePose, camera: FigureCamera = SIDE, props: List<Prop> = emptyList()) =
        loop(pose, pose.copy(pelvis = pose.pelvis + Vec3(0f, -0.005f, 0f)), durationMillis = 3600, camera = camera, props = props)

    /** Floor work reads best from the side and a little above. */
    private val SIDE = FigureCamera(yaw = 70f, pitch = 18f)

    private const val TORSO_LENGTH = Proportions.TORSO - 0.03f
    private const val LYING_HEIGHT = 0.12f

    /** Lying face up, the pelvis at the origin's height, arms resting by the sides. */
    private val lying = FigurePose(
        pelvis = Vec3(0f, LYING_HEIGHT, 0f),
        bodyPitch = -90f,
        leftArm = LimbAngles(abduct = 12f),
        rightArm = LimbAngles(abduct = 12f),
    )

    // ---- Hip hinge ----

    /**
     * A bridge: shoulders resting at [shoulders], the straight torso turning about them as the
     * pelvis rises to [pelvisHeight].
     */
    private fun bridge(shoulders: Vec3, pelvisHeight: Float, feet: Vec3?, extendedLeg: LimbAngles? = null): FigurePose {
        val cosine = ((shoulders.y - pelvisHeight) / TORSO_LENGTH).coerceIn(-1f, 1f)
        val pitch = -Math.toDegrees(acos(cosine).toDouble()).toFloat()
        val pelvis = Vec3(0f, pelvisHeight, shoulders.z - sin(pitch.rad()) * TORSO_LENGTH)
        return lying.copy(
            pelvis = pelvis,
            bodyPitch = pitch,
            // The head and arms stay resting: they turn back as far as the torso tilts.
            neckFlex = -(pitch + 90f),
            leftArm = LimbAngles(flex = pitch + 90f, abduct = 12f),
            rightArm = LimbAngles(flex = pitch + 90f, abduct = 12f),
            rightFootPin = feet?.let { it + Vec3(0.12f, 0f, 0f) },
            leftFootPin = if (extendedLeg == null) feet?.let { it + Vec3(-0.12f, 0f, 0f) } else null,
            leftLeg = extendedLeg ?: LimbAngles(),
        )
    }

    private val floorShoulders = Vec3(0f, 0.1f, -0.55f)
    private val bentKneeFeet = Vec3(0f, 0f, 0.32f)

    val gluteBridge = loop(bridge(floorShoulders, 0.14f, bentKneeFeet), bridge(floorShoulders, 0.38f, bentKneeFeet))

    val singleBridge = loop(
        bridge(floorShoulders, 0.14f, bentKneeFeet, extendedLeg = LimbAngles(flex = 25f)),
        bridge(floorShoulders, 0.36f, bentKneeFeet, extendedLeg = LimbAngles(flex = 25f)),
    )

    val hipThrust = run {
        val benchShoulders = Vec3(0f, 0.45f, -0.6f)
        val bench = Prop(Vec3(-0.45f, 0f, -0.85f), Vec3(0.45f, 0.38f, -0.55f))
        val feet = Vec3(0f, 0f, 0.3f)
        loop(
            bridge(benchShoulders, 0.2f, feet, extendedLeg = LimbAngles(flex = 60f, bend = 60f)),
            bridge(benchShoulders, 0.45f, feet, extendedLeg = LimbAngles(flex = 60f, bend = 60f)),
            props = listOf(bench),
        )
    }

    /** Holding the bridge, the heels slide out and back in. */
    val sliderCurl = loop(
        bridge(floorShoulders, 0.36f, bentKneeFeet),
        bridge(floorShoulders, 0.26f, Vec3(0f, 0f, 0.62f)),
        durationMillis = 3400,
    )

    /** Kneeling with the ankles anchored; the straight body leans forward about the knees. */
    private fun kneelingLean(degrees: Float, arms: LimbAngles): FigurePose {
        // Shins flat on the floor: the knee at ankle height, a shin's length ahead of the anchored ankles.
        val knee = Vec3(0f, Proportions.ANKLE_HEIGHT, 0f)
        val pelvis = knee + Vec3(0f, cos(degrees.rad()), sin(degrees.rad())) * Proportions.THIGH
        return FigurePose(
            pelvis = pelvis,
            bodyPitch = degrees,
            neckFlex = -10f,
            leftFootPin = Vec3(-0.1f, 0f, -Proportions.SHIN * 0.995f),
            rightFootPin = Vec3(0.1f, 0f, -Proportions.SHIN * 0.995f),
            leftArm = arms,
            rightArm = arms,
        )
    }

    private val anchor = Prop(Vec3(-0.35f, 0.12f, -0.5f), Vec3(0.35f, 0.18f, -0.4f))
    private val ready = LimbAngles(flex = 40f, bend = 80f)

    val nordicNegative = FigureMotion(
        listOf(
            Keyframe(0f, kneelingLean(0f, ready)),
            // Arms bent, hands just above the floor, ready to catch.
            Keyframe(0.75f, kneelingLean(66f, LimbAngles(flex = 45f, bend = 100f))),
            Keyframe(1f, kneelingLean(0f, ready)),
        ),
        durationMillis = 5000,
        camera = SIDE,
        stillAt = 0.45f,
        props = listOf(anchor),
    )

    val nordic = loop(kneelingLean(0f, ready), kneelingLean(55f, ready), durationMillis = 4200, props = listOf(anchor))

    // ---- Core ----

    private val tableTop = LimbAngles(flex = 90f, bend = 90f)
    private val reachingUp = LimbAngles(flex = 90f)

    /** Opposite arm and leg lower towards the floor, one side, then the other. */
    val deadBug = run {
        val start = lying.copy(leftArm = reachingUp, rightArm = reachingUp, leftLeg = tableTop, rightLeg = tableTop)
        FigureMotion(
            listOf(
                Keyframe(0f, start),
                Keyframe(0.25f, start.copy(rightArm = LimbAngles(flex = 170f), leftLeg = LimbAngles(flex = 12f))),
                Keyframe(0.5f, start),
                Keyframe(0.75f, start.copy(leftArm = LimbAngles(flex = 170f), rightLeg = LimbAngles(flex = 12f))),
                Keyframe(1f, start),
            ),
            durationMillis = 5200,
            camera = SIDE,
            stillAt = 0.25f,
        )
    }

    /** On the forearms and toes: upper arms straight down, forearms forward on the floor. */
    val plank = run {
        val pitch = 79f
        val toes = Vec3(0f, 0f, -0.9f)
        val pelvis = toes + Vec3.UP * Proportions.ANKLE_HEIGHT + Vec3(0f, cos(pitch.rad()), sin(pitch.rad())) * (Proportions.THIGH + Proportions.SHIN)
        val forearms = LimbAngles(flex = pitch, bend = 90f)
        hold(
            FigurePose(
                pelvis = pelvis,
                bodyPitch = pitch,
                neckFlex = -10f,
                leftFootPin = toes + Vec3(-0.1f, 0f, 0f),
                rightFootPin = toes + Vec3(0.1f, 0f, 0f),
                leftArm = forearms,
                rightArm = forearms,
            ),
            camera = FigureCamera(yaw = 60f, pitch = 16f),
        )
    }

    val hollowTuck = hold(
        lying.copy(
            spineFlex = 22f,
            neckFlex = 15f,
            leftLeg = LimbAngles(flex = 100f, bend = 110f),
            rightLeg = LimbAngles(flex = 100f, bend = 110f),
            leftArm = LimbAngles(flex = 25f),
            rightArm = LimbAngles(flex = 25f),
        )
    )

    val hollow = hold(
        lying.copy(
            spineFlex = 20f,
            neckFlex = 15f,
            leftLeg = LimbAngles(flex = 20f),
            rightLeg = LimbAngles(flex = 20f),
            leftArm = LimbAngles(flex = 160f),
            rightArm = LimbAngles(flex = 160f),
        )
    )

    // ---- Leg raises ----

    private fun lyingLegs(legs: LimbAngles) = lying.copy(leftLeg = legs, rightLeg = legs)

    val lyingKneeRaise = loop(lyingLegs(LimbAngles(flex = 8f, bend = 10f)), lyingLegs(LimbAngles(flex = 105f, bend = 110f)).copy(spineFlex = -5f))

    val lyingLegRaise = loop(lyingLegs(LimbAngles(flex = 8f)), lyingLegs(LimbAngles(flex = 90f)), durationMillis = 3400)

    private val grip = 0.3f
    /** Hanging with the shoulders fixed under the bar; a lean swings the pelvis about them. */
    private fun hanging(legs: LimbAngles, lean: Float = 0f) = FigurePose(
        pelvis = Vec3(0f, PullMotions.hangHeight(grip) + TORSO_LENGTH, -0.03f) - Vec3(0f, cos(lean.rad()), sin(lean.rad())) * TORSO_LENGTH,
        bodyPitch = lean,
        neckFlex = -10f,
        leftHandPin = Vec3(-grip, PullMotions.BAR, 0f),
        rightHandPin = Vec3(grip, PullMotions.BAR, 0f),
        leftLeg = legs,
        rightLeg = legs,
    )

    private val HANGING_CAMERA = FigureCamera(yaw = 60f, pitch = 8f)

    val hangingKneeRaise = loop(hanging(LimbAngles(bend = 10f)), hanging(LimbAngles(flex = 100f, bend = 110f)), camera = HANGING_CAMERA, props = listOf(PullMotions.bar))

    val hangingLegRaise = loop(hanging(LimbAngles()), hanging(LimbAngles(flex = 92f)), durationMillis = 3400, camera = HANGING_CAMERA, props = listOf(PullMotions.bar))

    /** The legs rise all the way to the bar, the body leaning back under it. */
    val toesToBar = loop(hanging(LimbAngles()), hanging(LimbAngles(flex = 150f), lean = -20f), durationMillis = 3400, camera = HANGING_CAMERA, props = listOf(PullMotions.bar))

    val byStep: Map<String, FigureMotion> = mapOf(
        "hinge.bridge" to gluteBridge,
        "hinge.single_bridge" to singleBridge,
        "hinge.hip_thrust" to hipThrust,
        "hinge.slider_curl" to sliderCurl,
        "hinge.nordic_negative" to nordicNegative,
        "hinge.nordic" to nordic,
        "core.dead_bug" to deadBug,
        "core.plank" to plank,
        "core.hollow_tuck" to hollowTuck,
        "core.hollow" to hollow,
        "leg_raise.lying_knee" to lyingKneeRaise,
        "leg_raise.lying_leg" to lyingLegRaise,
        "leg_raise.hanging_knee" to hangingKneeRaise,
        "leg_raise.hanging_leg" to hangingLegRaise,
        "leg_raise.toes_to_bar" to toesToBar,
    )
}
