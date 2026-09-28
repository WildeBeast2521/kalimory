package io.github.gonbei774.calisthenicsmemory.ui.screens

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class BackupTimeFormatTest {
    private lateinit var saved: Locale

    @Before fun useUsLocale() {
        saved = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After fun restoreLocale() = Locale.setDefault(saved)

    @Test fun `shows the date and minute in the given time zone`() {
        // 2026-09-28T16:57:30Z is 21:57 in Karachi (UTC+5).
        val text = formatBackupTime(1_790_614_650_000L, ZoneId.of("Asia/Karachi"))
        assertTrue(text, text.startsWith("Sep 28, 2026"))
        assertTrue(text, text.contains("9:57"))
        assertTrue(text, text.contains("PM"))
    }
}
