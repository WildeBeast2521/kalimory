package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.R
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordCsvRowTest {
    private fun row(vararg columns: String) = parseRecordCsvRow(columns.toList())
    private fun error(vararg columns: String) = (row(*columns).exceptionOrNull() as? ImportIssueException)?.issue

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

    @Test fun `a negative distance reads back, as the app saves one`() {
        assertEquals(-40, row("Sled", "Dynamic", "2026-10-08", "07:30", "1", "8", "", "", "-40", "", "").getOrThrow().distanceCm)
    }

    @Test fun `other negative numbers are still refused`() {
        assertEquals(ImportIssue(R.string.csv_issue_negative), error("A", "Dynamic", "2026-10-08", "07:30", "1", "-5", "", ""))
        assertEquals(ImportIssue(R.string.csv_issue_negative), error("A", "Dynamic", "2026-10-08", "07:30", "1", "5", "", "", "", "-1000", ""))
    }

    @Test fun `dates and times must be in the form the app writes`() {
        assertEquals(ImportIssue(R.string.csv_issue_date, listOf("2026-2-30")), error("A", "Dynamic", "2026-2-30", "07:30", "1", "5", "", ""))
        assertEquals(ImportIssue(R.string.csv_issue_date, listOf("2026-02-30")), error("A", "Dynamic", "2026-02-30", "07:30", "1", "5", "", ""))
        assertEquals(ImportIssue(R.string.csv_issue_time, listOf("7:30")), error("A", "Dynamic", "2026-10-08", "7:30", "1", "5", "", ""))
        assertEquals(ImportIssue(R.string.csv_issue_time, listOf("07:30:00")), error("A", "Dynamic", "2026-10-08", "07:30:00", "1", "5", "", ""))
    }

    @Test fun `numbers must be whole and not negative, optional ones included`() {
        assertEquals(ImportIssue(R.string.csv_issue_number), error("A", "Dynamic", "2026-10-08", "07:30", "1", "five", "", ""))
        assertEquals(ImportIssue(R.string.csv_issue_number), error("A", "Dynamic", "2026-10-08", "07:30", "1", "5", "", "", "1.5km"))
        assertEquals(ImportIssue(R.string.csv_issue_negative), error("A", "Dynamic", "2026-10-08", "07:30", "1", "-5", "", ""))
        assertEquals(ImportIssue(R.string.csv_issue_negative), error("A", "Dynamic", "2026-10-08", "07:30", "1", "5", "-1", ""))
    }

    @Test fun `short or incomplete rows are refused`() {
        assertEquals(ImportIssue(R.string.csv_issue_record_columns), error("A", "Dynamic", "2026-10-08"))
        assertEquals(ImportIssue(R.string.csv_issue_record_required), error("A", "", "2026-10-08", "07:30", "1", "5", "", ""))
    }

    @Test fun `warm-up and effort read when present`() {
        val r = row("A", "Dynamic", "2026-10-08", "07:30", "1", "12", "", "", "", "", "", "true", "3").getOrThrow()
        assertEquals(true, r.warmUp)
        assertEquals(3, r.effortLeft)
    }

    @Test fun `an old eleven-column row has no marks`() {
        val r = row("A", "Dynamic", "2026-10-08", "07:30", "1", "12", "", "", "", "", "").getOrThrow()
        assertEquals(false, r.warmUp)
        assertEquals(null, r.effortLeft)
        // Empty cells mean the same as missing ones.
        val empty = row("A", "Dynamic", "2026-10-08", "07:30", "1", "12", "", "", "", "", "", "", "").getOrThrow()
        assertEquals(false, empty.warmUp)
        assertEquals(null, empty.effortLeft)
    }

    @Test fun `a bad warm-up or effort value is refused, not read as empty`() {
        assertEquals(ImportIssue(R.string.csv_issue_warmup, listOf("yes")), error("A", "Dynamic", "2026-10-08", "07:30", "1", "12", "", "", "", "", "", "yes"))
        assertEquals(ImportIssue(R.string.csv_issue_effort, listOf("5", 4)), error("A", "Dynamic", "2026-10-08", "07:30", "1", "12", "", "", "", "", "", "false", "5"))
        assertEquals(ImportIssue(R.string.csv_issue_effort, listOf("x", 4)), error("A", "Dynamic", "2026-10-08", "07:30", "1", "12", "", "", "", "", "", "", "x"))
    }
}
