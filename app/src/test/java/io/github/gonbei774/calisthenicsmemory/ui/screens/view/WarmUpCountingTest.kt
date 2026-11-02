package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import io.github.gonbei774.calisthenicsmemory.ui.screens.formatRecordsForClipboard
import io.github.gonbei774.calisthenicsmemory.util.AppClock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

/** Progress and the clipboard summary leave warm-ups out of every count and target (ADR 0012). */
class WarmUpCountingTest {
    private val today = LocalDate.of(2026, 3, 12)
    private val pushUp = Exercise(id = 1, name = "Push-up", type = "Dynamic", targetSets = 1, targetValue = 10)

    @Before fun fixToday() { AppClock.fixed = Clock.fixed(today.atTime(18, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC) }
    @After fun freeToday() { AppClock.fixed = null }

    private fun set(date: LocalDate, value: Int, warmUp: Boolean = false, time: String = "07:00") =
        HistorySet(HistorySource.V2, null, emptyList(), pushUp.id, date.toString(), time, 1, value, null, "", null, null, null, warmUp = warmUp)

    @Test fun `a day of warm-ups only is trained but never clears the target`() {
        val records = listOf(set(today.minusDays(1), 12, warmUp = true), set(today, 12))
        val days = calculateClearDays(pushUp, records, Period.OneWeek, DayOfWeek.MONDAY)
        assertEquals(listOf(DayClearState.Trained, DayClearState.Clear), days.states.takeLast(2))
        assertEquals(1, days.clearCount)
    }

    @Test fun `statistics count only sets of work`() {
        val records = listOf(set(today, 30, warmUp = true), set(today, 12), set(today, 10))
        val stats = calculateStatistics(pushUp, records, Period.OneWeek, DayOfWeek.MONDAY)
        assertEquals(2, stats.totalSets)
        assertEquals(12, stats.max)
    }

    @Test fun `the clipboard summary lists the work and is empty for warm-ups only`() {
        assertEquals("", formatRecordsForClipboard(listOf(set(today, 9, warmUp = true)), listOf(pushUp)))
        val text = formatRecordsForClipboard(listOf(set(today, 9, warmUp = true), set(today, 12)), listOf(pushUp))
        assertTrue(text, text.contains("12"))
        assertFalse(text, text.contains("9"))
    }
}
