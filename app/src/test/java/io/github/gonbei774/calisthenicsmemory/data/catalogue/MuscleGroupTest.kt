package io.github.gonbei774.calisthenicsmemory.data.catalogue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The groups people think in, derived from muscles (#274). */
class MuscleGroupTest {
    @Test fun `every muscle is in exactly one group and no group is empty`() {
        val byGroup = Muscle.entries.groupBy { it.group }
        assertEquals(Muscle.entries.size, byGroup.values.sumOf { it.size })
        MuscleGroup.entries.forEach { assertTrue("$it has no muscle", byGroup[it].orEmpty().isNotEmpty()) }
    }

    @Test fun `the groups are the ones people name`() {
        assertEquals(MuscleGroup.BACK, Muscle.LATS.group)
        assertEquals(MuscleGroup.BACK, Muscle.LOWER_BACK.group)
        assertEquals(MuscleGroup.ARMS, Muscle.BICEPS.group)
        assertEquals(MuscleGroup.ARMS, Muscle.FOREARMS.group)
        assertEquals(MuscleGroup.CORE, Muscle.ABDOMINALS.group)
        assertEquals(MuscleGroup.LEGS, Muscle.GLUTES.group)
        assertEquals(MuscleGroup.CALVES, Muscle.CALVES.group)
        assertEquals(MuscleGroup.SHOULDERS, Muscle.SHOULDERS.group)
        assertEquals(MuscleGroup.CHEST, Muscle.CHEST.group)
    }
}
