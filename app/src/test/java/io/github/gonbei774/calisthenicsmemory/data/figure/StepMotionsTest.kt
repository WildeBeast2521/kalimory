package io.github.gonbei774.calisthenicsmemory.data.figure

import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every catalogue motion is physically sound: pins reached, nothing through the floor, loops closed. */
class StepMotionsTest {
    private fun distance(a: Vec3, b: Vec3) = (a - b).length()

    /** Chains whose motions are done; each later chain joins this list. */
    private val covered = listOf("push", "squat", "pull", "row", "dip", "vpush", "hinge", "core", "leg_raise")

    @Test fun `every step of a covered chain has a motion`() {
        covered.forEach { chain -> Catalogue.chain(chain)!!.steps.forEach { assertNotNull(it.id, StepMotions.forStep(it.id)) } }
    }

    @Test fun `pull-ups bring the chin over the bar`() {
        listOf("pull.band", "pull.chin", "pull.full", "pull.negative", "pull.one_arm", "pull.one_arm_negative").forEach { id ->
            val motion = StepMotions.forStep(id)!!
            val heads = (0..20).map { SkeletonSolver.solve(motion.poseAt(it / 20f))[Joint.HEAD].y }
            val bar = motion.props.single().min.y
            assertTrue("$id reaches the bar", heads.max() > bar + 0.1f)
            assertTrue("$id hangs below it", heads.min() < bar)
        }
    }

    @Test fun `rows pull the chest up to the bar`() {
        StepMotions.stepIds.filter { it.startsWith("row.") }.forEach { id ->
            val motion = StepMotions.forStep(id)!!
            val hand = motion.poseAt(0f).rightHandPin!!
            val start = SkeletonSolver.solve(motion.poseAt(0f))[Joint.CHEST]
            val pulled = SkeletonSolver.solve(motion.stillPose)[Joint.CHEST]
            assertTrue("$id chest rises towards the bar", (pulled - hand).length() < (start - hand).length() - 0.15f)
        }
    }

    @Test fun `dips and handstand push-ups lower the shoulders towards the hands`() {
        listOf("dip.bench", "dip.full", "dip.negative", "vpush.pike", "vpush.elevated_pike", "vpush.wall_negative", "vpush.wall_hspu").forEach { id ->
            val motion = StepMotions.forStep(id)!!
            val hand = motion.poseAt(0f).rightHandPin!!
            val start = SkeletonSolver.solve(motion.poseAt(0f))[Joint.RIGHT_SHOULDER]
            // The deepest point of the loop; a negative's still pose is only halfway down.
            val closest = (0..40).minOf { (SkeletonSolver.solve(motion.poseAt(it / 40f))[Joint.RIGHT_SHOULDER] - hand).length() }
            assertTrue("$id lowers", closest < (start - hand).length() - 0.15f)
        }
    }

    @Test fun `the wall handstand holds its head above the floor and its feet near the wall`() {
        val top = SkeletonSolver.solve(StepMotions.forStep("vpush.wall_hold")!!.poseAt(0f))
        assertTrue(top[Joint.HEAD].y > 0.2f)
        assertTrue("feet up", top[Joint.LEFT_ANKLE].y > 1.6f)
    }

    @Test fun `every catalogue step has a motion`() {
        Catalogue.steps.forEach { assertNotNull(it.id, StepMotions.forStep(it.id)) }
    }

    @Test fun `bridges lift the hips and leg raises lift the feet`() {
        listOf("hinge.bridge", "hinge.single_bridge", "hinge.hip_thrust").forEach { id ->
            val motion = StepMotions.forStep(id)!!
            assertTrue(id, motion.stillPose.pelvis.y > motion.poseAt(0f).pelvis.y + 0.2f)
        }
        listOf("leg_raise.lying_leg", "leg_raise.hanging_leg", "leg_raise.toes_to_bar").forEach { id ->
            val motion = StepMotions.forStep(id)!!
            val start = SkeletonSolver.solve(motion.poseAt(0f))[Joint.RIGHT_ANKLE].y
            val raised = SkeletonSolver.solve(motion.stillPose)[Joint.RIGHT_ANKLE].y
            assertTrue(id, raised > start + 0.6f)
        }
    }

    @Test fun `squats lower the hips and stand back up`() {
        StepMotions.stepIds.filter { it.startsWith("squat.") }.forEach { id ->
            val motion = StepMotions.forStep(id)!!
            val top = motion.poseAt(0f).pelvis.y
            val bottom = motion.stillPose.pelvis.y
            assertTrue("$id goes down", bottom < top - 0.25f)
        }
    }

    @Test fun `motions belong to real catalogue steps`() {
        StepMotions.stepIds.forEach { assertNotNull(it, Catalogue.step(it)) }
    }

    @Test fun `pinned hands and feet stay put and nothing sinks into the floor`() {
        StepMotions.stepIds.forEach { id ->
            val motion = StepMotions.forStep(id)!!
            for (i in 0..24) {
                val pose = motion.poseAt(i / 24f)
                val s = SkeletonSolver.solve(pose)
                pose.leftHandPin?.let { assertEquals("$id left hand", 0f, distance(s[Joint.LEFT_WRIST], it), 0.03f) }
                pose.rightHandPin?.let { assertEquals("$id right hand", 0f, distance(s[Joint.RIGHT_WRIST], it), 0.03f) }
                pose.leftFootPin?.let { assertEquals("$id left foot", 0f, distance(s[Joint.LEFT_ANKLE], it + Vec3.UP * Proportions.ANKLE_HEIGHT), 0.03f) }
                pose.rightFootPin?.let { assertEquals("$id right foot", 0f, distance(s[Joint.RIGHT_ANKLE], it + Vec3.UP * Proportions.ANKLE_HEIGHT), 0.03f) }
                s.all.forEach { (joint, p) -> assertTrue("$id $joint at ${p.y}", p.y >= -0.02f) }
            }
            assertEquals(id, motion.poseAt(0f), motion.poseAt(1f))
        }
    }

    @Test fun `push-ups go down and come back up`() {
        StepMotions.stepIds.filter { it.startsWith("push.") }.forEach { id ->
            val motion = StepMotions.forStep(id)!!
            val top = SkeletonSolver.solve(motion.poseAt(0f))[Joint.NECK]
            val bottom = SkeletonSolver.solve(motion.stillPose)[Joint.NECK]
            val hands = motion.poseAt(0f).rightHandPin!!
            assertTrue("$id comes closer to the hands", distance(bottom, hands) < distance(top, hands) - 0.1f)
        }
    }
}
