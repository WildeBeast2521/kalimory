package io.github.gonbei774.calisthenicsmemory.data.figure

import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The figure carries every catalogue muscle, each facing the right way (ADR 0008, decision 2). */
class FigureMusclesTest {
    private val standing = SkeletonSolver.solve(FigurePose())
    private val placed = FigureMuscles.place(standing) { _, _ -> 0.06f to 0.05f }

    private fun normalOf(muscle: Muscle, side: String? = null) =
        placed.first { it.shape.muscle == muscle && (side == null || it.shape.from.name.startsWith(side)) }.normal

    @Test fun `every catalogue muscle is drawn on the figure`() {
        val drawn = FigureMuscles.shapes.map { it.muscle }.toSet()
        Muscle.entries.forEach { assertTrue(it.name, it in drawn) }
    }

    @Test fun `standing, front muscles face forward and back muscles face back`() {
        assertTrue(normalOf(Muscle.ABDOMINALS).z > 0.9f)
        assertTrue(normalOf(Muscle.QUADRICEPS, "LEFT").z > 0.9f)
        assertTrue(normalOf(Muscle.CALVES, "RIGHT").z < -0.9f)
        assertTrue(normalOf(Muscle.TRICEPS, "LEFT").z < -0.9f)
    }

    @Test fun `shoulders face outwards on each side`() {
        assertTrue(normalOf(Muscle.SHOULDERS, "LEFT").x < -0.9f)
        assertTrue(normalOf(Muscle.SHOULDERS, "RIGHT").x > 0.9f)
    }

    @Test fun `normals are unit length in every sampled pose`() {
        SampleMotions.all.forEach { sample ->
            for (i in 0..10) {
                FigureMuscles.place(SkeletonSolver.solve(sample.motion.poseAt(i / 10f))) { _, _ -> 0.06f to 0.05f }.forEach {
                    assertEquals("${sample.name} ${it.shape.muscle}", 1f, it.normal.length(), 0.001f)
                }
            }
        }
    }

    @Test fun `the camera sees the front of a standing figure`() {
        val camera = FigureCamera()
        assertTrue(normalOf(Muscle.ABDOMINALS).dot(camera.towardViewer) > 0.5f)
        assertTrue(normalOf(Muscle.LOWER_BACK).dot(camera.towardViewer) < 0f)
    }
}
