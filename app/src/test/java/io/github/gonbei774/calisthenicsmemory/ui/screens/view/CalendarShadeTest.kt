package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import org.junit.Assert.assertEquals
import org.junit.Test

/** A calendar day is shaded by how many sets it holds, not by a hidden weighting of reps and difficulty. */
class CalendarShadeTest {
    @Test fun `no sets is no shade`() {
        assertEquals(0, shadeLevel(0))
    }

    @Test fun `the shade steps at 1, 4, 10 and 20 sets`() {
        assertEquals(listOf(1, 1, 1), listOf(1, 2, 3).map(::shadeLevel))
        assertEquals(listOf(2, 2), listOf(4, 9).map(::shadeLevel))
        assertEquals(listOf(3, 3), listOf(10, 19).map(::shadeLevel))
        assertEquals(listOf(4, 4), listOf(20, 60).map(::shadeLevel))
    }

    @Test fun `an empty day has no sets`() {
        assertEquals(0, daySets(emptyList()))
    }
}
