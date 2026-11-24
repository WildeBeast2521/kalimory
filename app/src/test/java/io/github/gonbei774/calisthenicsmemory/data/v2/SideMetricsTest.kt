package io.github.gonbei774.calisthenicsmemory.data.v2

import org.junit.Assert.assertEquals
import org.junit.Test

class SideMetricsTest {
    @Test fun `the left follows the right while the two are equal`() {
        assertEquals("6", linkedLeft(oldRight = "5", newRight = "6", left = "5"))
        assertEquals("5", linkedLeft(oldRight = "", newRight = "5", left = ""))
    }

    @Test fun `a left that was changed stays as it is`() {
        assertEquals("9", linkedLeft(oldRight = "5", newRight = "6", left = "9"))
        // A saved left of none beside a right of 5 is not "equal", so it is not overwritten.
        assertEquals("", linkedLeft(oldRight = "5", newRight = "6", left = ""))
    }
}
