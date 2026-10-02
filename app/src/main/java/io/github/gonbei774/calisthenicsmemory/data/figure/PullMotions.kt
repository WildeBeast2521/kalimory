package io.github.gonbei774.calisthenicsmemory.data.figure

/**
 * The pull-up chain's motions (ADR 0008, decision 3). The hands are pinned to a bar; the body hangs
 * below it, from a straight-arm hang to the chin over the bar, slightly behind it so the head clears.
 */
internal object PullMotions {

    private const val BAR = 2.2f
    private val bar = Prop(Vec3(-0.75f, BAR, -0.02f), Vec3(0.75f, BAR + 0.035f, 0.02f))

    /** Shoulder to pelvis along a straight torso, with the shoulder's small drop below the neck. */
    private const val SHOULDER_ABOVE_PELVIS = Proportions.TORSO - 0.03f
    private const val ARM = Proportions.UPPER_ARM + Proportions.FOREARM

    /** Arms straight: the pelvis as high as straight arms allow, the grip [halfGrip] from the middle. */
    private fun hangHeight(halfGrip: Float): Float {
        val dx = halfGrip - Proportions.SHOULDER_HALF_WIDTH
        return BAR - kotlin.math.sqrt(ARM * ARM * 0.995f - dx * dx) - SHOULDER_ABOVE_PELVIS
    }

    /** The chin over the bar: the neck just above it, the body a little behind. */
    private const val TOP_PELVIS = BAR + 0.02f - Proportions.TORSO

    private val hangingLegs = LimbAngles(flex = 8f, bend = 18f)

    private fun hang(halfGrip: Float, pelvisY: Float, pelvisZ: Float = -0.03f, legs: LimbAngles = hangingLegs, x: Float = 0f) = FigurePose(
        pelvis = Vec3(x, pelvisY, pelvisZ),
        neckFlex = -10f,
        leftHandPin = Vec3(-halfGrip, BAR, 0f),
        rightHandPin = Vec3(halfGrip, BAR, 0f),
        leftLeg = legs,
        rightLeg = legs,
    )

    private fun upAndDown(halfGrip: Float, legs: LimbAngles = hangingLegs, durationMillis: Int = 3000) = FigureMotion(
        listOf(
            Keyframe(0f, hang(halfGrip, hangHeight(halfGrip), legs = legs)),
            Keyframe(0.5f, hang(halfGrip, TOP_PELVIS, pelvisZ = -0.13f, legs = legs)),
            Keyframe(1f, hang(halfGrip, hangHeight(halfGrip), legs = legs)),
        ),
        durationMillis,
        props = listOf(bar),
    )

    private const val SHOULDER_GRIP = 0.21f
    private const val WIDE_GRIP = 0.3f

    /** A still hang, breathing: the body settles a centimetre and back. */
    val deadHang = FigureMotion(
        listOf(
            Keyframe(0f, hang(WIDE_GRIP, hangHeight(WIDE_GRIP))),
            Keyframe(0.5f, hang(WIDE_GRIP, hangHeight(WIDE_GRIP) - 0.012f)),
            Keyframe(1f, hang(WIDE_GRIP, hangHeight(WIDE_GRIP))),
        ),
        durationMillis = 3600,
        props = listOf(bar),
    )

    /**
     * Pulling the shoulder blades down lifts the body a few centimetres. The skeleton has no shrug,
     * so the lift shows as the body rising with nearly straight arms.
     */
    val scapular = FigureMotion(
        listOf(
            Keyframe(0f, hang(WIDE_GRIP, hangHeight(WIDE_GRIP))),
            Keyframe(0.5f, hang(WIDE_GRIP, hangHeight(WIDE_GRIP) + 0.05f, legs = LimbAngles(flex = 5f, bend = 10f)).copy(neckFlex = -18f)),
            Keyframe(1f, hang(WIDE_GRIP, hangHeight(WIDE_GRIP))),
        ),
        durationMillis = 2600,
        props = listOf(bar),
    )

    /** From the top, a slow lowering, then a quick return to the top. */
    private fun slowLowering(top: FigurePose, bottom: FigurePose) = FigureMotion(
        listOf(Keyframe(0f, top), Keyframe(0.8f, bottom), Keyframe(1f, top)),
        durationMillis = 4500,
        props = listOf(bar),
        stillAt = 0.4f,
    )

    val negative = slowLowering(
        top = hang(SHOULDER_GRIP, TOP_PELVIS, pelvisZ = -0.13f),
        bottom = hang(SHOULDER_GRIP, hangHeight(SHOULDER_GRIP)),
    )

    /** Knees bent, as if standing in a band looped over the bar. */
    val band = upAndDown(SHOULDER_GRIP, legs = LimbAngles(flex = 25f, bend = 80f))

    val chin = upAndDown(SHOULDER_GRIP)

    val full = upAndDown(WIDE_GRIP)

    /** A wide grip, pulling towards one hand, then the other. */
    val archer = run {
        val grip = 0.55f
        // The far arm stays nearly straight, so the body moves only partway towards the working hand.
        fun toward(sign: Float) = hang(grip, TOP_PELVIS - 0.15f, pelvisZ = -0.12f, x = sign * 0.16f)
        FigureMotion(
            listOf(
                Keyframe(0f, hang(grip, hangHeight(grip))),
                Keyframe(0.25f, toward(1f)),
                Keyframe(0.5f, hang(grip, hangHeight(grip))),
                Keyframe(0.75f, toward(-1f)),
                Keyframe(1f, hang(grip, hangHeight(grip))),
            ),
            durationMillis = 5600,
            props = listOf(bar),
            stillAt = 0.25f,
        )
    }

    /** One hand on the bar, the other arm down by the side. */
    private fun oneHand(pose: FigurePose) = pose.copy(
        pelvis = pose.pelvis + Vec3(0.08f, 0f, 0f),
        leftHandPin = null,
        leftArm = LimbAngles(flex = 20f, abduct = 15f, bend = 40f),
        rightHandPin = Vec3(0.08f, BAR, 0f),
        bodyYaw = 12f,
    )

    val oneArmNegative = slowLowering(
        top = oneHand(hang(0.1f, TOP_PELVIS, pelvisZ = -0.13f)),
        bottom = oneHand(hang(0.1f, hangHeight(0.1f) + 0.02f)),
    )

    val oneArm = FigureMotion(
        upAndDown(0.1f, durationMillis = 3400).keyframes.map { Keyframe(it.at, oneHand(it.pose)) },
        durationMillis = 3400,
        props = listOf(bar),
    )

    val byStep: Map<String, FigureMotion> = mapOf(
        "pull.dead_hang" to deadHang,
        "pull.scapular" to scapular,
        "pull.negative" to negative,
        "pull.band" to band,
        "pull.chin" to chin,
        "pull.full" to full,
        "pull.archer" to archer,
        "pull.one_arm_negative" to oneArmNegative,
        "pull.one_arm" to oneArm,
    )
}
