package io.github.gonbei774.calisthenicsmemory.data.figure

/**
 * The squat chain's motions (ADR 0008, decision 3). Feet are pinned; the pelvis travels between a
 * standing and a bottom position while the torso leans to keep balance over the feet. Single-leg
 * steps hold the free leg by its angles.
 */
internal object SquatMotions {

    private fun loop(top: FigurePose, bottom: FigurePose, durationMillis: Int = 3200, camera: FigureCamera = FigureCamera(), props: List<Prop> = emptyList()) =
        FigureMotion(listOf(Keyframe(0f, top), Keyframe(0.5f, bottom), Keyframe(1f, top)), durationMillis, camera, props = props)

    private val bothFeet = FigurePose(
        leftFootPin = Vec3(-0.13f, 0f, 0f),
        rightFootPin = Vec3(0.13f, 0f, 0f),
    )

    /** A pole in front, held at chest height, for the assisted steps. */
    private val pole = Prop(Vec3(-0.03f, 0f, 0.38f), Vec3(0.03f, 2.0f, 0.44f))
    private val leftOnPole = Vec3(-0.04f, 1.0f, 0.35f)
    private val rightOnPole = Vec3(0.04f, 1.0f, 0.35f)

    val full = loop(
        top = bothFeet.copy(pelvis = Vec3(0f, 0.93f, 0f), leftArm = LimbAngles(flex = 10f, bend = 10f), rightArm = LimbAngles(flex = 10f, bend = 10f)),
        bottom = bothFeet.copy(
            pelvis = Vec3(0f, 0.5f, -0.2f),
            spineFlex = 38f,
            neckFlex = -25f,
            leftArm = LimbAngles(flex = 95f, abduct = 5f),
            rightArm = LimbAngles(flex = 95f, abduct = 5f),
        ),
    )

    val assisted = loop(
        top = bothFeet.copy(pelvis = Vec3(0f, 0.93f, 0f), leftHandPin = leftOnPole, rightHandPin = rightOnPole),
        bottom = bothFeet.copy(pelvis = Vec3(0f, 0.5f, -0.24f), spineFlex = 18f, neckFlex = -10f, leftHandPin = leftOnPole, rightHandPin = rightOnPole),
        props = listOf(pole),
    )

    /** A long stride: the back knee drops towards the floor, the torso stays upright. */
    private val stride = FigurePose(
        leftFootPin = Vec3(-0.12f, 0f, -0.48f),
        rightFootPin = Vec3(0.12f, 0f, 0.36f),
        leftArm = LimbAngles(abduct = 8f, bend = 10f),
        rightArm = LimbAngles(abduct = 8f, bend = 10f),
    )

    val split = loop(
        top = stride.copy(pelvis = Vec3(0f, 0.8f, -0.03f)),
        bottom = stride.copy(pelvis = Vec3(0f, 0.5f, -0.06f), spineFlex = 5f),
        camera = FigureCamera(yaw = 60f, pitch = 12f),
    )

    val bulgarian = run {
        val bench = Prop(Vec3(-0.35f, 0f, -0.85f), Vec3(0.35f, 0.45f, -0.55f))
        val stance = FigurePose(
            leftFootPin = Vec3(-0.1f, 0.45f, -0.66f),
            rightFootPin = Vec3(0.12f, 0f, 0.3f),
            leftArm = LimbAngles(abduct = 8f, bend = 10f),
            rightArm = LimbAngles(abduct = 8f, bend = 10f),
        )
        loop(
            top = stance.copy(pelvis = Vec3(0f, 0.9f, -0.04f), spineFlex = 5f),
            bottom = stance.copy(pelvis = Vec3(0f, 0.5f, -0.08f), spineFlex = 15f),
            camera = FigureCamera(yaw = 60f, pitch = 12f),
            props = listOf(bench),
        )
    }

    /** On one foot under the body; the free leg reaches forward, lifted higher as the hips sink. */
    private val oneFoot = FigurePose(rightFootPin = Vec3(0.09f, 0f, 0f))

    val assistedPistol = loop(
        top = oneFoot.copy(pelvis = Vec3(0f, 0.9f, -0.02f), leftLeg = LimbAngles(flex = 30f), leftHandPin = leftOnPole, rightHandPin = rightOnPole),
        bottom = oneFoot.copy(
            pelvis = Vec3(0f, 0.42f, -0.18f),
            spineFlex = 25f,
            leftLeg = LimbAngles(flex = 80f),
            leftHandPin = leftOnPole,
            rightHandPin = rightOnPole,
        ),
        props = listOf(pole),
    )

    /** The rear foot is held behind, and the back knee touches down softly. */
    val shrimp = loop(
        top = oneFoot.copy(
            pelvis = Vec3(0f, 0.9f, -0.02f),
            leftLeg = LimbAngles(flex = -10f, bend = 100f),
            leftArm = LimbAngles(flex = -25f, bend = 70f),
            rightArm = LimbAngles(flex = 40f, bend = 10f),
        ),
        bottom = oneFoot.copy(
            pelvis = Vec3(0f, 0.45f, -0.12f),
            spineFlex = 35f,
            neckFlex = -20f,
            leftLeg = LimbAngles(flex = 20f, bend = 110f),
            leftArm = LimbAngles(flex = -10f, bend = 80f),
            rightArm = LimbAngles(flex = 90f),
        ),
        camera = FigureCamera(yaw = 60f, pitch = 12f),
    )

    val pistol = loop(
        top = oneFoot.copy(pelvis = Vec3(0f, 0.9f, -0.02f), leftLeg = LimbAngles(flex = 35f), leftArm = LimbAngles(flex = 80f), rightArm = LimbAngles(flex = 80f)),
        bottom = oneFoot.copy(
            pelvis = Vec3(0f, 0.38f, -0.2f),
            spineFlex = 42f,
            neckFlex = -25f,
            leftLeg = LimbAngles(flex = 85f),
            leftArm = LimbAngles(flex = 95f),
            rightArm = LimbAngles(flex = 95f),
        ),
        durationMillis = 3800,
    )

    val byStep: Map<String, FigureMotion> = mapOf(
        "squat.assisted" to assisted,
        "squat.full" to full,
        "squat.split" to split,
        "squat.bulgarian" to bulgarian,
        "squat.assisted_pistol" to assistedPistol,
        "squat.shrimp" to shrimp,
        "squat.pistol" to pistol,
    )
}
