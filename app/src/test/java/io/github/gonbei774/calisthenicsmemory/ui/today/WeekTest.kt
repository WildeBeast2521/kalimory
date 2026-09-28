package io.github.gonbei774.calisthenicsmemory.ui.today

import io.github.gonbei774.calisthenicsmemory.ui.screens.today.weekOf
import io.github.gonbei774.calisthenicsmemory.ui.theme.weekDaysFrom
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WeekTest {
    @Test fun `week days start on the chosen day and wrap`() {
        assertEquals(
            listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            weekDaysFrom(DayOfWeek.SATURDAY),
        )
    }

    @Test fun `the week containing a day starts on the chosen first day`() {
        val wednesday = LocalDate.of(2026, 9, 30)
        assertEquals(LocalDate.of(2026, 9, 28), weekOf(wednesday, DayOfWeek.MONDAY).first())
        assertEquals(LocalDate.of(2026, 9, 27), weekOf(wednesday, DayOfWeek.SUNDAY).first())
        assertEquals(LocalDate.of(2026, 9, 26), weekOf(wednesday, DayOfWeek.SATURDAY).first())
        // A week that starts today begins today.
        assertEquals(wednesday, weekOf(wednesday, DayOfWeek.WEDNESDAY).first())
        assertEquals(7, weekOf(wednesday, DayOfWeek.MONDAY).size)
    }
}
