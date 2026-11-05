package io.github.gonbei774.calisthenicsmemory.ui.components.muscles

import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MovementPattern
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Muscle
import io.github.gonbei774.calisthenicsmemory.data.review.VolumeTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** How the body map's shades land on the drawn figure (ADR 0011). */
class VolumeRegionsTest {
    @Test fun `every muscle a training step names can be shaded`() {
        Catalogue.chains.filter { it.pattern != MovementPattern.MOBILITY }.flatMap { it.steps }
            .flatMap { it.primaryMuscles + it.secondaryMuscles }.toSet()
            .forEach { assertTrue(it.name, muscleRegions(it).isNotEmpty()) }
    }

    @Test fun `lats and middle back share a region, which takes the higher shade`() {
        val regions = regionTiers(mapOf(Muscle.LATS to VolumeTier.LOW, Muscle.MIDDLE_BACK to VolumeTier.HIGH))
        assertEquals(VolumeTier.HIGH, regions[BodyRegion.UPPER_BACK])
        assertEquals(VolumeTier.HIGH, regionTiers(mapOf(Muscle.MIDDLE_BACK to VolumeTier.HIGH, Muscle.LATS to VolumeTier.LOW))[BodyRegion.UPPER_BACK])
    }
}
