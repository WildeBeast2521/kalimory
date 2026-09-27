package io.github.gonbei774.calisthenicsmemory.data.v2

import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CompatibilityHistoryTest {
    private val zone = ZoneId.of("America/New_York")
    private val start = ZonedDateTime.of(2025, 2, 1, 9, 5, 30, 0, zone).toInstant().toEpochMilli()

    private fun row(id: Long, exercise: Long = 1, set: Int = 1, side: BodySide = BodySide.BILATERAL, reps: Int? = null,
                    durationMillis: Long? = null, kind: ExerciseKind = ExerciseKind.DYNAMIC, completedAt: Long? = null) =
        V2HistoryRow(id, sessionExerciseId = exercise * 10, exerciseId = exercise, exerciseKindSnapshot = kind, setNumber = set,
            orderIndex = id.toInt(), side = side, repetitions = reps, durationMillis = durationMillis, distanceCm = null,
            addedWeightGrams = 2_000, assistanceGrams = null, completedAtEpochMillis = completedAt,
            sessionStartedAtEpochMillis = start, sessionComment = "v2 workout")

    @Test fun `legacy records pass through unchanged`() {
        val legacy = TrainingRecord(7, 1, 12, 11, 2, "2024-12-31", "18:00", "old", 100, 200, 300)
        val merged = CompatibilityHistory.merge(listOf(legacy), emptyList(), zone).single()
        assertEquals(
            HistorySet(HistorySource.LEGACY, 7, emptyList(), 1, "2024-12-31", "18:00", 2, 12, 11, "old", 100, 200, 300),
            merged,
        )
    }

    @Test fun `v2 sides pair into one row and values use legacy units`() {
        val rows = listOf(
            row(1, set = 1, side = BodySide.RIGHT, reps = 8),
            row(2, set = 1, side = BodySide.LEFT, reps = 7),
            row(3, exercise = 2, kind = ExerciseKind.ISOMETRIC, durationMillis = 45_900),
        )
        val merged = CompatibilityHistory.merge(emptyList(), rows, zone)
        val lunge = merged.single { it.exerciseId == 1L }
        assertEquals(listOf(1L, 2L), lunge.setEntryIds)
        assertEquals(8, lunge.valueRight)
        assertEquals(7, lunge.valueLeft)
        assertEquals(2_000, lunge.weightG)
        val plank = merged.single { it.exerciseId == 2L }
        assertEquals(45, plank.valueRight)
        assertEquals(null, plank.valueLeft)
    }

    @Test fun `v2 sets take their session's start time in the given zone, like one saved legacy workout`() {
        val completed = ZonedDateTime.of(2025, 2, 1, 23, 59, 59, 0, zone).toInstant().toEpochMilli()
        val merged = CompatibilityHistory.merge(emptyList(), listOf(row(1, reps = 5), row(2, exercise = 2, reps = 5, completedAt = completed)), zone)
        assertEquals(listOf("2025-02-01" to "09:05", "2025-02-01" to "09:05"), merged.map { it.date to it.time })
        assertEquals(setOf(HistorySource.V2), merged.map { it.source }.toSet())
    }

    @Test fun `newest first, legacy before v2 at the same minute`() {
        val legacy = listOf(
            TrainingRecord(1, 1, 1, null, 1, "2025-02-01", "09:05", ""),
            TrainingRecord(2, 1, 1, null, 1, "2025-01-01", "09:05", ""),
        )
        val merged = CompatibilityHistory.merge(legacy, listOf(row(9, reps = 3)), zone)
        assertEquals(listOf(HistorySource.LEGACY, HistorySource.V2, HistorySource.LEGACY), merged.map { it.source })
    }

    @Test fun `legacy rows convert back to the exact record for edits`() {
        val legacy = TrainingRecord(7, 1, 12, 11, 2, "2024-12-31", "18:00", "old", 100, 200, 300)
        assertEquals(legacy, CompatibilityHistory.merge(listOf(legacy), emptyList(), zone).single().toLegacyRecord())
    }

    @Test fun `v2 rows have no legacy record to edit`() {
        assertEquals(null, CompatibilityHistory.merge(emptyList(), listOf(row(1, reps = 5)), zone).single().toLegacyRecord())
    }
}
