package io.github.gonbei774.calisthenicsmemory.data.figure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The lofted body follows its bones and keeps a muscular shape (owner, 2026-10-02). */
class FigureBodyTest {
    private val standing = SkeletonSolver.solve(FigurePose())
    private val parts = FigureBody.parts(standing)

    @Test fun `limbs start and end at their joints`() {
        val limbs = parts.filter { part ->
            part.segments.size == 1 && part.segments[0].first.name.substringAfter('_') in setOf("SHOULDER", "ELBOW", "HIP", "KNEE")
        }
        assertEquals(8, limbs.size)
        limbs.forEach { part ->
            val (from, to) = part.segments.single()
            assertEquals(0f, (part.rings.first().center - standing[from]).length(), 1e-4f)
            assertEquals(0f, (part.rings.last().center - standing[to]).length(), 1e-4f)
        }
    }

    @Test fun `the torso is a V, wider at the chest than at the waist`() {
        val torso = parts.first { Joint.PELVIS to Joint.CHEST in it.segments }
        val widths = torso.rings.map { it.a.length() }
        val waist = widths.subList(0, widths.size / 2).min()
        val chest = widths.subList(widths.size / 2, widths.size).max()
        assertTrue("chest $chest, waist $waist", chest > waist * 1.4f)
    }

    @Test fun `rings sit at right angles to their limb`() {
        val thigh = parts.first { Joint.LEFT_HIP to Joint.LEFT_KNEE in it.segments }
        val axis = (standing[Joint.LEFT_KNEE] - standing[Joint.LEFT_HIP]).normalized()
        thigh.rings.forEach {
            assertEquals(0f, it.a.normalized().dot(axis), 1e-3f)
            assertEquals(0f, it.b.normalized().dot(axis), 1e-3f)
        }
    }

    @Test fun `the calf and biceps bulge`() {
        assertTrue(FigureBody.radiusAt(Joint.LEFT_KNEE, Joint.LEFT_ANKLE, 0.22f) > FigureBody.radiusAt(Joint.LEFT_KNEE, Joint.LEFT_ANKLE, 0f))
        assertTrue(FigureBody.radiusAt(Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW, 0.52f) > FigureBody.radiusAt(Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW, 0.3f))
    }
}
