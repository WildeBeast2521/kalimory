package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupData
import io.github.gonbei774.calisthenicsmemory.viewmodel.ExportRecord
import io.github.gonbei774.calisthenicsmemory.viewmodel.ExportSetEntry
import io.github.gonbei774.calisthenicsmemory.viewmodel.setCount
import org.junit.Assert.assertEquals
import org.junit.Test

/** The set count a restore preview and the backup messages show: every set the user did, once. */
class BackupSetCountTest {
    private fun entry(id: Long, sessionExercise: Long, set: Int, side: String = "BOTH", status: String = "COMPLETED", legacy: Long? = null) =
        ExportSetEntry(id, sessionExercise, set, set, status = status, side = side, repetitions = 5, timePrecision = "EXACT", legacyTrainingRecordId = legacy)

    private fun backup(records: Int, entries: List<ExportSetEntry>) = BackupData(
        version = 12, exportDate = "2026-01-02T03:04:05", app = "Kalimory",
        groups = emptyList(), exercises = emptyList(),
        records = (1..records).map { ExportRecord(it.toLong(), 1, 5, null, it, "2026-01-02", "03:04", "") },
        setEntries = entries,
    )

    @Test fun `Kalimory workouts count, not only original records`() {
        // The bug: a backup of Kalimory workouts said "Records: 0" right before replacing all data.
        assertEquals(3, backup(0, listOf(entry(1, 1, 1), entry(2, 1, 2), entry(3, 2, 1))).setCount())
    }

    @Test fun `both stores add up`() {
        assertEquals(4, backup(2, listOf(entry(1, 1, 1), entry(2, 1, 2))).setCount())
    }

    @Test fun `a one-sided set counts once, skipped sets and copied records not at all`() {
        val entries = listOf(
            entry(1, 1, 1, side = "RIGHT"), entry(2, 1, 1, side = "LEFT"),
            entry(3, 1, 2, status = "SKIPPED"),
            entry(4, 1, 3, legacy = 1),
        )
        assertEquals(1 + 1, backup(1, entries).setCount())
    }
}
