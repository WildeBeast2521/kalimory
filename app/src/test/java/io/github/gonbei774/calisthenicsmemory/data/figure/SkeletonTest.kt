package io.github.gonbei774.calisthenicsmemory.data.figure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The figure's skeleton keeps its proportions, reaches its pins and loops smoothly (ADR 0008). */
class SkeletonTest {
    private fun distance(a: Vec3, b: Vec3) = (a - b).length()

    @Test fun `standing puts the feet on the floor and the head on top`() {
        val skeleton = SkeletonSolver.solve(FigurePose())
        assertEquals(Proportions.ANKLE_HEIGHT, skeleton[Joint.LEFT_ANKLE].y, 0.001f)
        assertTrue(skeleton[Joint.HEAD].y > skeleton[Joint.NECK].y)
        assertTrue(skeleton[Joint.RIGHT_SHOULDER].x > 0 && skeleton[Joint.LEFT_SHOULDER].x < 0)
    }

    @Test fun `limb angles follow their meaning`() {
        val skeleton = SkeletonSolver.solve(FigurePose(rightArm = LimbAngles(flex = 90f), leftArm = LimbAngles(abduct = 90f)))
        val forward = skeleton[Joint.RIGHT_ELBOW] - skeleton[Joint.RIGHT_SHOULDER]
        assertEquals(Proportions.UPPER_ARM, forward.z, 0.001f)
        val out = skeleton[Joint.LEFT_ELBOW] - skeleton[Joint.LEFT_SHOULDER]
        assertEquals(-Proportions.UPPER_ARM, out.x, 0.001f)
    }

    @Test fun `a bent knee folds backwards and a bent elbow forwards`() {
        val skeleton = SkeletonSolver.solve(FigurePose(rightLeg = LimbAngles(flex = 90f, bend = 90f), rightArm = LimbAngles(bend = 90f)))
        assertTrue("shin hangs down", skeleton[Joint.RIGHT_ANKLE].y < skeleton[Joint.RIGHT_KNEE].y - 0.3f)
        assertTrue("forearm points ahead", skeleton[Joint.RIGHT_WRIST].z > skeleton[Joint.RIGHT_ELBOW].z + 0.2f)
    }

    @Test fun `pinned limbs reach their pins and keep their lengths`() {
        SampleMotions.all.forEach { (name, motion) ->
            for (i in 0..20) {
                val pose = motion.poseAt(i / 20f)
                val s = SkeletonSolver.solve(pose)
                pose.leftHandPin?.let { assertEquals("$name hand", 0f, distance(s[Joint.LEFT_WRIST], it), 0.02f) }
                pose.rightFootPin?.let { assertEquals("$name foot", 0f, distance(s[Joint.RIGHT_ANKLE], it + Vec3.UP * Proportions.ANKLE_HEIGHT), 0.02f) }
                assertEquals("$name thigh", Proportions.THIGH, distance(s[Joint.LEFT_HIP], s[Joint.LEFT_KNEE]), 0.001f)
                assertEquals("$name shin", Proportions.SHIN, distance(s[Joint.LEFT_KNEE], s[Joint.LEFT_ANKLE]), 0.001f)
                assertEquals("$name upper arm", Proportions.UPPER_ARM, distance(s[Joint.RIGHT_SHOULDER], s[Joint.RIGHT_ELBOW]), 0.001f)
                assertEquals("$name forearm", Proportions.FOREARM, distance(s[Joint.RIGHT_ELBOW], s[Joint.RIGHT_WRIST]), 0.001f)
            }
        }
    }

    @Test fun `the squat keeps its knees forward and stays above the floor`() {
        val bottom = SkeletonSolver.solve(SampleMotions.squat.stillPose)
        assertTrue(bottom[Joint.LEFT_KNEE].z > bottom[Joint.LEFT_ANKLE].z)
        assertTrue(bottom.all.values.all { it.y >= -0.001f })
    }

    @Test fun `motions loop back to where they started`() {
        SampleMotions.all.forEach { (name, motion) ->
            assertEquals(name, motion.poseAt(0f), motion.poseAt(1f))
        }
    }

    @Test fun `the camera shows the figure's right on the viewer's left`() {
        val front = FigureCamera(yaw = 0f, pitch = 0f)
        assertTrue(front.project(Vec3(1f, 0f, 0f)).x < 0)
        assertTrue("nearer is deeper", front.project(Vec3(0f, 0f, 1f)).depth > front.project(Vec3.ZERO).depth)
    }
}
