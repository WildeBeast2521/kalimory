package io.github.gonbei774.calisthenicsmemory.data.figure

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The demonstration figure's skeleton (ADR 0008, decision 2): a pose in 3D, solved from a few joint
 * angles, with hands or feet pinned by a two-bone solver. Pure Kotlin, so poses can be unit-tested.
 *
 * World axes, in metres: x to the figure's left-right, y up, z forward. The floor is y = 0.
 */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(dot(this))
    fun normalized(): Vec3 = length().let { if (it < 1e-6f) this else this * (1f / it) }

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val UP = Vec3(0f, 1f, 0f)
        val DOWN = Vec3(0f, -1f, 0f)
        val FORWARD = Vec3(0f, 0f, 1f)
        val BACK = Vec3(0f, 0f, -1f)
        val RIGHT = Vec3(1f, 0f, 0f)
    }
}

/** One rigid rotation, kept as its three axes (columns). */
class Frame(val right: Vec3, val up: Vec3, val forward: Vec3) {
    fun apply(v: Vec3) = right * v.x + up * v.y + forward * v.z

    /** Turns this frame about its own right axis by [degrees]; positive tips the up axis forward. */
    fun pitched(degrees: Float): Frame {
        val a = degrees.rad()
        val c = cos(a)
        val s = sin(a)
        return Frame(right, up * c + forward * s, forward * c - up * s)
    }

    /** Turns this frame about its own up axis by [degrees]; positive turns forward to the right. */
    fun yawed(degrees: Float): Frame {
        val a = degrees.rad()
        val c = cos(a)
        val s = sin(a)
        return Frame(right * c - forward * s, up, forward * c + right * s)
    }

    /** Turns this frame about its own forward axis by [degrees]; positive leans up to the right. */
    fun rolled(degrees: Float): Frame {
        val a = degrees.rad()
        val c = cos(a)
        val s = sin(a)
        return Frame(right * c - up * s, up * c + right * s, forward)
    }

    companion object {
        val IDENTITY = Frame(Vec3.RIGHT, Vec3.UP, Vec3.FORWARD)
    }
}

internal fun Float.rad() = (this * PI / 180.0).toFloat()

/** Segment lengths and widths of an adult of about 1.75 m, in metres. */
object Proportions {
    const val PELVIS_HALF_WIDTH = 0.09f
    const val SHOULDER_HALF_WIDTH = 0.19f
    const val TORSO = 0.50f
    const val NECK = 0.10f
    const val HEAD_RADIUS = 0.11f
    const val UPPER_ARM = 0.30f
    const val FOREARM = 0.27f
    const val HAND = 0.08f
    const val THIGH = 0.43f
    const val SHIN = 0.43f
    const val FOOT = 0.20f
    const val ANKLE_HEIGHT = 0.08f

    /** Hip joint height when standing straight. */
    const val STANDING_HIP_HEIGHT = THIGH + SHIN + ANKLE_HEIGHT
}

enum class Side(val sign: Float) { LEFT(-1f), RIGHT(1f) }

/** One limb's angles in degrees, relative to the body part it hangs from. */
data class LimbAngles(
    /** Swings the limb forward: 0 hangs straight down, 90 points straight ahead, 180 overhead. */
    val flex: Float = 0f,
    /** Swings the limb out to the side: 0 at the body, 90 straight out. */
    val abduct: Float = 0f,
    /** Bends the middle joint (elbow, knee): 0 is straight. */
    val bend: Float = 0f,
)

/**
 * A pose: where the pelvis is and how the body is turned, the joint angles, and optional pins. A
 * pinned hand or foot is placed at its world point, and the limb is solved to reach it.
 */
data class FigurePose(
    val pelvis: Vec3 = Vec3(0f, Proportions.STANDING_HIP_HEIGHT, 0f),
    /** Tips the whole body forward about the hips: 0 upright, 90 lying face down. */
    val bodyPitch: Float = 0f,
    val bodyYaw: Float = 0f,
    val bodyRoll: Float = 0f,
    /** Bends the torso forward from the pelvis. */
    val spineFlex: Float = 0f,
    val neckFlex: Float = 0f,
    val leftArm: LimbAngles = LimbAngles(),
    val rightArm: LimbAngles = LimbAngles(),
    val leftLeg: LimbAngles = LimbAngles(),
    val rightLeg: LimbAngles = LimbAngles(),
    val leftHandPin: Vec3? = null,
    val rightHandPin: Vec3? = null,
    val leftFootPin: Vec3? = null,
    val rightFootPin: Vec3? = null,
)

enum class Joint {
    PELVIS, CHEST, NECK, HEAD,
    LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST, LEFT_HAND,
    RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST, RIGHT_HAND,
    LEFT_HIP, LEFT_KNEE, LEFT_ANKLE, LEFT_TOE,
    RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE, RIGHT_TOE,
}

/** Every joint's world position for one pose. */
class SolvedSkeleton(private val joints: Map<Joint, Vec3>) {
    operator fun get(joint: Joint): Vec3 = joints.getValue(joint)
    val all: Map<Joint, Vec3> get() = joints
}

object SkeletonSolver {

    fun solve(pose: FigurePose): SolvedSkeleton {
        val joints = mutableMapOf<Joint, Vec3>()
        val body = Frame.IDENTITY.yawed(pose.bodyYaw).pitched(pose.bodyPitch).rolled(pose.bodyRoll)
        val torso = body.pitched(pose.spineFlex)

        val pelvis = pose.pelvis
        val neck = pelvis + torso.up * Proportions.TORSO
        val chest = pelvis + torso.up * (Proportions.TORSO * 0.62f)
        val head = neck + torso.pitched(pose.neckFlex).up * (Proportions.NECK + Proportions.HEAD_RADIUS)
        joints[Joint.PELVIS] = pelvis
        joints[Joint.CHEST] = chest
        joints[Joint.NECK] = neck
        joints[Joint.HEAD] = head

        for (side in Side.entries) {
            val left = side == Side.LEFT
            val shoulder = neck + torso.right * (side.sign * Proportions.SHOULDER_HALF_WIDTH) - torso.up * 0.03f
            val arm = limb(
                root = shoulder,
                frame = torso,
                side = side,
                angles = if (left) pose.leftArm else pose.rightArm,
                upper = Proportions.UPPER_ARM,
                lower = Proportions.FOREARM,
                pin = if (left) pose.leftHandPin else pose.rightHandPin,
                // Elbows bend backwards and a little out; reaching overhead (a bar), they point down
                // and out instead, as in a pull-up.
                bendHint = if (((if (left) pose.leftHandPin else pose.rightHandPin)?.y ?: 0f) > shoulder.y) {
                    torso.up * -1f + torso.right * (side.sign * 0.6f) + torso.forward * 0.2f
                } else {
                    torso.forward * -1f + torso.right * (side.sign * 0.4f)
                },
                bendSign = 1f,
            )
            val wrist = arm.second
            val handPin = if (left) pose.leftHandPin else pose.rightHandPin
            val forearm = (wrist - arm.first).normalized()
            val handDirection = if (handPin == null) {
                forearm
            } else {
                // A pinned hand lies flat on its support, fingers pointing along the body: forward on
                // the floor, up on a wall.
                var flat = (torso.up - forearm * torso.up.dot(forearm)).normalized()
                if (handPin.y < 0.05f && flat.y < 0f) flat = Vec3(flat.x, 0f, flat.z).normalized()
                flat
            }
            val hand = wrist + handDirection * Proportions.HAND
            joints[if (left) Joint.LEFT_SHOULDER else Joint.RIGHT_SHOULDER] = shoulder
            joints[if (left) Joint.LEFT_ELBOW else Joint.RIGHT_ELBOW] = arm.first
            joints[if (left) Joint.LEFT_WRIST else Joint.RIGHT_WRIST] = wrist
            joints[if (left) Joint.LEFT_HAND else Joint.RIGHT_HAND] = hand

            val hip = pelvis + body.right * (side.sign * Proportions.PELVIS_HALF_WIDTH)
            val legPin = (if (left) pose.leftFootPin else pose.rightFootPin)?.let { it + Vec3.UP * Proportions.ANKLE_HEIGHT }
            val leg = limb(
                root = hip,
                frame = body,
                side = side,
                angles = if (left) pose.leftLeg else pose.rightLeg,
                upper = Proportions.THIGH,
                lower = Proportions.SHIN,
                pin = legPin,
                // Knees bend forwards.
                bendHint = body.forward,
                bendSign = -1f,
            )
            val ankle = leg.second
            // The foot points forward along the body, flattened onto the floor when pinned.
            val footDirection = if (legPin != null) Vec3(body.forward.x, 0f, body.forward.z).normalized() else (body.forward - body.up * 0.2f).normalized()
            val toe = ankle + footDirection * Proportions.FOOT - Vec3.UP * (if (legPin != null) Proportions.ANKLE_HEIGHT else 0f)
            joints[if (left) Joint.LEFT_HIP else Joint.RIGHT_HIP] = hip
            joints[if (left) Joint.LEFT_KNEE else Joint.RIGHT_KNEE] = leg.first
            joints[if (left) Joint.LEFT_ANKLE else Joint.RIGHT_ANKLE] = ankle
            joints[if (left) Joint.LEFT_TOE else Joint.RIGHT_TOE] = toe
        }
        return SolvedSkeleton(joints)
    }

    /**
     * A two-segment limb from [root]. Without a pin it follows the angles; with one, the end is
     * placed at the pin (or as close as the limb reaches) and the middle joint bends towards
     * [bendHint]. Returns the middle and end joints.
     */
    private fun limb(
        root: Vec3,
        frame: Frame,
        side: Side,
        angles: LimbAngles,
        upper: Float,
        lower: Float,
        pin: Vec3?,
        bendHint: Vec3,
        bendSign: Float,
    ): Pair<Vec3, Vec3> {
        if (pin != null) return twoBone(root, pin, upper, lower, bendHint)
        val limbFrame = frame.rolled(-side.sign * angles.abduct).pitched(-angles.flex)
        val upperDir = limbFrame.up * -1f
        val middle = root + upperDir * upper
        val lowerDir = (limbFrame.pitched(bendSign * -angles.bend)).up * -1f
        return middle to (middle + lowerDir * lower)
    }

    /** Places the middle joint so both segments keep their lengths, bending towards [hint]. */
    fun twoBone(root: Vec3, target: Vec3, upper: Float, lower: Float, hint: Vec3): Pair<Vec3, Vec3> {
        val toTarget = target - root
        val reach = (upper + lower) * 0.9999f
        val distance = toTarget.length().coerceIn(abs(upper - lower) + 1e-4f, reach)
        val direction = toTarget.normalized()
        val end = root + direction * distance
        // Law of cosines: the angle at the root between the target line and the upper segment.
        val cosRoot = ((upper * upper + distance * distance - lower * lower) / (2 * upper * distance)).coerceIn(-1f, 1f)
        val rootAngle = acos(cosRoot)
        // The bend plane: the hint direction, without its component along the target line.
        var bend = (hint - direction * hint.dot(direction)).normalized()
        if (bend.length() < 0.5f) bend = direction.cross(Vec3.RIGHT).normalized()
        val middle = root + (direction * cos(rootAngle) + bend * sin(rootAngle)) * upper
        return middle to end
    }

    private fun abs(v: Float) = if (v < 0) -v else v
}
