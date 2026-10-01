package io.github.gonbei774.calisthenicsmemory.ui.components.muscles

import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import org.junit.Assert.assertTrue
import org.junit.Test

/** The muscle map can show every muscle the catalogue names (ADR 0008, decision 1). */
class MuscleMapTest {
    private val drawn = (BodyMapData.front + BodyMapData.back).map { it.region }.toSet()

    @Test fun `every muscle has a drawn region`() {
        Muscle.entries.forEach { muscle ->
            val regions = muscleRegions(muscle)
            assertTrue("$muscle has regions", regions.isNotEmpty())
            assertTrue("$muscle regions are drawn", regions.all { it in drawn })
        }
    }

    @Test fun `every outline is a closed polygon inside the box`() {
        (BodyMapData.front + BodyMapData.back).forEach { shape ->
            assertTrue(shape.region.name, shape.points.size >= 6 && shape.points.size % 2 == 0)
            shape.points.toList().chunked(2).forEach { (x, y) ->
                assertTrue("${shape.region} x $x", x in 0f..BodyMapData.WIDTH)
                assertTrue("${shape.region} y $y", y in 0f..BodyMapData.HEIGHT)
            }
        }
    }

    @Test fun `every catalogue step shows its main muscles`() {
        Catalogue.steps.forEach { step ->
            assertTrue(step.id, step.primaryMuscles.flatMap(::muscleRegions).isNotEmpty())
        }
    }
}
