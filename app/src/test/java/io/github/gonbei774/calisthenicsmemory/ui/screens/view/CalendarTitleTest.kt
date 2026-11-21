package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.YearMonth
import java.util.Locale

/** The calendar's month title follows each language's own order and script, not only Japanese. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CalendarTitleTest {
    private val november = YearMonth.of(2026, 11)

    @Test fun `English puts the month first`() = assertEquals("November 2026", formatYearMonth(november, Locale.ENGLISH))

    @Test fun `Japanese and Chinese put the year first with the month sign`() {
        assertEquals("2026年11月", formatYearMonth(november, Locale.JAPANESE))
        assertEquals("2026年11月", formatYearMonth(november, Locale.SIMPLIFIED_CHINESE))
    }

    @Test fun `other languages use their own month name`() {
        assertEquals("novembre 2026", formatYearMonth(november, Locale.FRENCH))
        assertEquals("November 2026", formatYearMonth(november, Locale.GERMAN))
    }
}
