package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PeriodStartTest {
    private val wednesday = LocalDate.of(2026, 9, 30)

    @Test fun `one week starts on the chosen first day of this week`() {
        assertEquals(LocalDate.of(2026, 9, 27), Period.OneWeek.startDate(wednesday, DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 9, 28), Period.OneWeek.startDate(wednesday, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 9, 26), Period.OneWeek.startDate(wednesday, DayOfWeek.SATURDAY))
    }

    @Test fun `on the first day itself the week is just today`() {
        assertEquals(wednesday, Period.OneWeek.startDate(wednesday, DayOfWeek.WEDNESDAY))
    }

    @Test fun `longer periods still count back from today`() {
        assertEquals(LocalDate.of(2026, 9, 1), Period.OneMonth.startDate(wednesday, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 7, 3), Period.ThreeMonths.startDate(wednesday, DayOfWeek.MONDAY))
    }
}
