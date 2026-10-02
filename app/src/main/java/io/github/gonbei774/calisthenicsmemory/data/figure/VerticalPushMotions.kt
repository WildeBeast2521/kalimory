package io.github.gonbei774.calisthenicsmemory.data.figure

/**
 * The dip and handstand push-up chains (ADR 0008, decision 3): pressing down from the shoulders, on
 * parallel bars or a bench, or upside down on the hands with the feet on a wall.
 */
internal object VerticalPushMotions {

    private fun loop(top: FigurePose, bottom: FigurePose, durationMillis: Int, camera: FigureCamera = FigureCamera(), props: List<Prop>) =
        FigureMotion(listOf(Keyframe(0f, top), Keyframe(0.5f, bottom), Keyframe(1f, top)), durationMillis, camera, props = props)

    /** From the top, a slow lowering, then a quick return. */
    private fun slowLowering(top: FigurePose, bottom: FigurePose, camera: FigureCamera = FigureCamera(), props: List<Prop>) = FigureMotion(
        listOf(Keyframe(0f, top), Keyframe(0.8f, bottom), Keyframe(1f, top)),
        durationMillis = 4500,
        camera = camera,
        props = props,
        stillAt = 0.4f,
    )

    // Parallel bars: two rails running front to back, a little wider than the shoulders.
    private const val BARS = 1.2f
    private const val RAIL_HALF_WIDTH = 0.25f
    private val bars = listOf(-1f, 1f).map { side ->
        Prop(Vec3(side * RAIL_HALF_WIDTH - 0.02f, BARS, -0.5f), Vec3(side * RAIL_HALF_WIDTH + 0.02f, BARS + 0.035f, 0.5f))
    }

    /** Knees bent so the feet clear the floor, hanging behind. */
    private val tuckedLegs = LimbAngles(flex = 5f, bend = 75f)

    private fun onBars(pelvisY: Float, lean: Float, pelvisZ: Float = 0f) = FigurePose(
        pelvis = Vec3(0f, pelvisY, pelvisZ),
        bodyPitch = lean,
        neckFlex = -5f,
        leftHandPin = Vec3(-RAIL_HALF_WIDTH, BARS, 0f),
        rightHandPin = Vec3(RAIL_HALF_WIDTH, BARS, 0f),
        leftLeg = tuckedLegs,
        rightLeg = tuckedLegs,
    )

    // Arms straight: shoulders an arm's length above the bars.
    private val supportTop = onBars(BARS + 0.56f - (Proportions.TORSO - 0.03f), lean = 0f)
    // Shoulders just below the elbows, leaning a little forward.
    private val dipBottom = onBars(BARS + 0.1f - (Proportions.TORSO - 0.03f), lean = 18f, pelvisZ = -0.12f)

    val support = FigureMotion(
        listOf(Keyframe(0f, supportTop), Keyframe(0.5f, supportTop.copy(pelvis = supportTop.pelvis + Vec3(0f, -0.01f, 0f))), Keyframe(1f, supportTop)),
        durationMillis = 3600,
        props = bars,
    )

    val dipNegative = slowLowering(supportTop, dipBottom, props = bars)

    val dip = loop(supportTop, dipBottom, durationMillis = 3000, props = bars)

    /** Hands on a bench behind, heels on the floor ahead; the hips sink in front of the bench. */
    val benchDip = run {
        val bench = Prop(Vec3(-0.4f, 0f, -0.55f), Vec3(0.4f, 0.45f, -0.2f))
        fun pose(pelvisY: Float) = FigurePose(
            pelvis = Vec3(0f, pelvisY, -0.08f),
            neckFlex = -5f,
            leftHandPin = Vec3(-0.2f, 0.45f, -0.24f),
            rightHandPin = Vec3(0.2f, 0.45f, -0.24f),
            leftFootPin = Vec3(-0.12f, 0f, 0.55f),
            rightFootPin = Vec3(0.12f, 0f, 0.55f),
        )
        loop(pose(0.5f), pose(0.22f), durationMillis = 2800, camera = FigureCamera(yaw = 60f, pitch = 14f), props = listOf(bench))
    }

    /**
     * Pike push-ups: feet on the floor (or a bench), hips high, the torso pointing down to the hands,
     * the head lowering between them.
     */
    private fun pike(feet: Vec3, pelvis: Vec3, spine: Float, pelvisBottom: Vec3, spineBottom: Float, handZ: Float, props: List<Prop>): FigureMotion {
        fun pose(p: Vec3, s: Float, neck: Float) = FigurePose(
            pelvis = p,
            spineFlex = s,
            neckFlex = neck,
            leftFootPin = feet + Vec3(-0.1f, 0f, 0f),
            rightFootPin = feet + Vec3(0.1f, 0f, 0f),
            leftHandPin = Vec3(-0.22f, 0f, handZ),
            rightHandPin = Vec3(0.22f, 0f, handZ),
        )
        return loop(
            pose(pelvis, spine, -20f), pose(pelvisBottom, spineBottom, -10f),
            durationMillis = 3000,
            // From the side: from the front or behind, the raised hips hide the head and arms.
            camera = FigureCamera(yaw = 95f, pitch = 12f),
            props = props,
        )
    }

    val pikePushUp = pike(
        feet = Vec3(0f, 0f, -0.35f),
        pelvis = Vec3(0f, 0.86f, 0f), spine = 140f,
        pelvisBottom = Vec3(0f, 0.74f, 0f), spineBottom = 160f,
        handZ = 0.32f,
        props = emptyList(),
    )

    val elevatedPike = pike(
        feet = Vec3(0f, 0.45f, -0.7f),
        pelvis = Vec3(0f, 1.0f, 0f), spine = 160f,
        pelvisBottom = Vec3(0f, 0.84f, 0f), spineBottom = 165f,
        handZ = 0.2f,
        props = listOf(Prop(Vec3(-0.35f, 0f, -0.9f), Vec3(0.35f, 0.45f, -0.5f))),
    )

    // Upside down, chest to the wall: the wall stands behind the hands (towards -z).
    private val wall = Prop(Vec3(-0.7f, 0f, -0.32f), Vec3(0.7f, 2.3f, -0.22f))
    private val cushion = Prop(Vec3(-0.15f, 0f, -0.12f), Vec3(0.15f, 0.05f, 0.14f))

    private fun handstand(pelvisY: Float) = FigurePose(
        pelvis = Vec3(0f, pelvisY, 0f),
        bodyPitch = 175f,
        neckFlex = 15f,
        leftHandPin = Vec3(-0.2f, 0f, -0.1f),
        rightHandPin = Vec3(0.2f, 0f, -0.1f),
    )

    // Arms straight: shoulders an arm's length above the floor.
    private val handstandTop = handstand(0.56f + (Proportions.TORSO - 0.03f))
    // The head down on the cushion.
    private val handstandBottom = handstand(0.78f)

    val wallHold = FigureMotion(
        listOf(Keyframe(0f, handstandTop), Keyframe(0.5f, handstandTop.copy(pelvis = handstandTop.pelvis + Vec3(0f, -0.01f, 0f))), Keyframe(1f, handstandTop)),
        durationMillis = 3600,
        camera = FigureCamera(yaw = 60f, pitch = 12f),
        props = listOf(wall),
    )

    val wallNegative = slowLowering(handstandTop, handstandBottom, camera = FigureCamera(yaw = 60f, pitch = 12f), props = listOf(wall, cushion))

    val wallHandstandPushUp = loop(handstandTop, handstandBottom, durationMillis = 3400, camera = FigureCamera(yaw = 60f, pitch = 12f), props = listOf(wall, cushion))

    val byStep: Map<String, FigureMotion> = mapOf(
        "dip.bench" to benchDip,
        "dip.support" to support,
        "dip.negative" to dipNegative,
        "dip.full" to dip,
        "vpush.pike" to pikePushUp,
        "vpush.elevated_pike" to elevatedPike,
        "vpush.wall_hold" to wallHold,
        "vpush.wall_negative" to wallNegative,
        "vpush.wall_hspu" to wallHandstandPushUp,
    )
}
