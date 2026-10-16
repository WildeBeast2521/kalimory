package io.github.gonbei774.calisthenicsmemory.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class AppClockTest {

    @After
    fun reset() {
        AppClock.fixed = null
    }

    @Test
    fun `a fixed clock sets today and the time of day`() {
        AppClock.fixed = Clock.fixed(LocalDate.of(2026, 3, 12).atTime(18, 5).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        assertEquals(LocalDate.of(2026, 3, 12), AppClock.today())
        assertEquals(LocalTime.of(18, 5), AppClock.timeNow())
    }

    @Test
    fun `without a fixed clock it follows the system clock`() {
        val before = LocalDate.now()
        val today = AppClock.today()
        val after = LocalDate.now()
        assertTrue("$today not between $before and $after", today == before || today == after)
    }
}
