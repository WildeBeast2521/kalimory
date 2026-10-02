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
    private val covered = listOf("push", "squat")

    @Test fun `every step of a covered chain has a motion`() {
        covered.forEach { chain -> Catalogue.chain(chain)!!.steps.forEach { assertNotNull(it.id, StepMotions.forStep(it.id)) } }
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
