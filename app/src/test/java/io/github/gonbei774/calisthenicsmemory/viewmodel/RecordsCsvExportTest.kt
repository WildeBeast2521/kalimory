package io.github.gonbei774.calisthenicsmemory.viewmodel

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import org.junit.Assert.assertEquals
import org.junit.Test

/** The records CSV the app writes carries warm-up marks and effort, and reads them back. */
class RecordsCsvExportTest {
    private val push = Exercise(id = 1, name = "Push-up", type = "Dynamic")

    private fun set(n: Int, warmUp: Boolean, effort: Int?) = HistorySet(
        source = HistorySource.V2, legacyRecordId = null, setEntryIds = listOf(n.toLong()), exerciseId = 1, date = "2026-03-09", time = "07:30",
        setNumber = n, valueRight = 10 + n, valueLeft = null, comment = "", distanceCm = null, weightG = null, assistanceG = null,
        warmUp = warmUp, effortLeft = effort,
    )

    @Test fun `the header has thirteen columns, the last two warmUp and effortLeft`() {
        val header = recordsCsv(emptyList(), listOf(push)).lines().first().split(",")
        assertEquals(13, header.size)
        assertEquals(listOf("warmUp", "effortLeft"), header.takeLast(2))
    }

    @Test fun `marks are written and read back`() {
        val csv = recordsCsv(listOf(set(1, warmUp = true, effort = 3), set(2, warmUp = false, effort = null)), listOf(push))
        val rows = parseCsvRecords(csv).drop(1).map { parseRecordCsvRow(it).getOrThrow() }
        assertEquals(listOf(true, false), rows.map { it.warmUp })
        assertEquals(listOf(3, null), rows.map { it.effortLeft })
    }
}
