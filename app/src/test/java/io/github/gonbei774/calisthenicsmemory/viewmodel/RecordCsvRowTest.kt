package io.github.gonbei774.calisthenicsmemory.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordCsvRowTest {
    private fun row(vararg columns: String) = parseRecordCsvRow(columns.toList())
    private fun error(vararg columns: String) = row(*columns).exceptionOrNull()?.message

    @Test fun `a full row reads every column`() {
        assertEquals(
            RecordCsvRow("Pull-up", "Dynamic", "2026-10-08", "07:30", 2, 8, 7, "Band", 100, 5000, 22000),
            row("Pull-up", "Dynamic", "2026-10-08", "07:30", "2", "8", "7", "Band", "100", "5000", "22000").getOrThrow(),
        )
    }

    @Test fun `an old eight-column row leaves the newer fields empty`() {
        assertEquals(
            RecordCsvRow("Plank", "Isometric", "2026-10-08", "07:30", 1, 60, null, "", null, null, null),
            row("Plank", "Isometric", "2026-10-08", "07:30", "1", "60", "", "").getOrThrow(),
        )
    }

    @Test fun `dates and times must be in the form the app writes`() {
        assertEquals("Invalid date (expected YYYY-MM-DD): 2026-2-30", error("A", "Dynamic", "2026-2-30", "07:30", "1", "5", "", ""))
        assertEquals("Invalid date (expected YYYY-MM-DD): 2026-02-30", error("A", "Dynamic", "2026-02-30", "07:30", "1", "5", "", ""))
        assertEquals("Invalid time (expected HH:MM): 7:30", error("A", "Dynamic", "2026-10-08", "7:30", "1", "5", "", ""))
        assertEquals("Invalid time (expected HH:MM): 07:30:00", error("A", "Dynamic", "2026-10-08", "07:30:00", "1", "5", "", ""))
    }

    @Test fun `numbers must be whole and not negative, optional ones included`() {
        assertEquals("Invalid number format", error("A", "Dynamic", "2026-10-08", "07:30", "1", "five", "", ""))
        assertEquals("Invalid number format", error("A", "Dynamic", "2026-10-08", "07:30", "1", "5", "", "", "1.5km"))
        assertEquals("Negative value", error("A", "Dynamic", "2026-10-08", "07:30", "1", "-5", "", ""))
        assertEquals("Negative value", error("A", "Dynamic", "2026-10-08", "07:30", "1", "5", "-1", ""))
    }

    @Test fun `short or incomplete rows are refused`() {
        assertEquals("Invalid format (not enough columns)", error("A", "Dynamic", "2026-10-08"))
        assertEquals("Missing required fields", error("A", "", "2026-10-08", "07:30", "1", "5", "", ""))
    }
}
