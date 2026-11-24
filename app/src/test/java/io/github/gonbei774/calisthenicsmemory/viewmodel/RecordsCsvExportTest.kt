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

    @Test fun `the header has sixteen columns, the last five optional`() {
        val header = recordsCsv(emptyList(), listOf(push)).lines().first().split(",")
        assertEquals(16, header.size)
        assertEquals(listOf("warmUp", "effortLeft", "weightLeftG", "assistanceLeftG", "distanceLeftCm"), header.takeLast(5))
    }

    private fun oneSided(weight: Int?, weightLeft: Int?, assistance: Int? = null, assistanceLeft: Int? = null, left: Int? = 5) = HistorySet(
        source = HistorySource.V2, legacyRecordId = null, setEntryIds = listOf(1, 2), exerciseId = 1, date = "2026-03-09", time = "07:30",
        setNumber = 1, valueRight = 5, valueLeft = left, comment = "", distanceCm = null, weightG = weight, assistanceG = assistance,
        weightLeftG = weightLeft, assistanceLeftG = assistanceLeft,
    )

    @Test fun `a left side that differs is written and read back, an equal one stays empty`() {
        val csv = recordsCsv(listOf(oneSided(2_000, 4_000, assistance = 5_000, assistanceLeft = 9_000), oneSided(3_000, 3_000)), listOf(push))
        val rows = parseCsvRecords(csv).drop(1).map { parseRecordCsvRow(it).getOrThrow() }
        assertEquals(listOf<Int?>(4_000, null), rows.map { it.weightLeftG })
        assertEquals(listOf<Int?>(9_000, null), rows.map { it.assistanceLeftG })
    }

    @Test fun `a two-sided set writes no left cells`() {
        val csv = recordsCsv(listOf(oneSided(2_000, null, left = null)), listOf(push))
        val row = parseRecordCsvRow(parseCsvRecords(csv).drop(1).single()).getOrThrow()
        assertEquals(null, row.weightLeftG)
    }

    @Test fun `marks are written and read back`() {
        val csv = recordsCsv(listOf(set(1, warmUp = true, effort = 3), set(2, warmUp = false, effort = null)), listOf(push))
        val rows = parseCsvRecords(csv).drop(1).map { parseRecordCsvRow(it).getOrThrow() }
        assertEquals(listOf(true, false), rows.map { it.warmUp })
        assertEquals(listOf(3, null), rows.map { it.effortLeft })
    }
}
