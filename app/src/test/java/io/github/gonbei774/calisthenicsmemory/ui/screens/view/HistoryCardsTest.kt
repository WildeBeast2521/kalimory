package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.data.v2.BodySide
import io.github.gonbei774.calisthenicsmemory.data.v2.CompatibilityHistory
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.V2HistoryRow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class HistoryCardsTest {
    private val start = ZonedDateTime.of(2026, 3, 2, 7, 30, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun row(id: Long, session: Long, reps: Int) = V2HistoryRow(
        setEntryId = id, sessionExerciseId = session * 10, exerciseId = 1, exerciseKindSnapshot = ExerciseKind.DYNAMIC,
        setNumber = 1, orderIndex = 0, side = BodySide.BILATERAL, repetitions = reps, durationMillis = null,
        distanceCm = null, addedWeightGrams = null, assistanceGrams = null, completedAtEpochMillis = null,
        sessionStartedAtEpochMillis = start, sessionComment = null, workoutSessionId = session,
    )

    @Test
    fun twoV2WorkoutsInTheSameMinuteHaveTheirOwnCards() {
        val history = CompatibilityHistory.merge(emptyList(), listOf(row(1, session = 5, reps = 10), row(2, session = 6, reps = 8)), ZoneOffset.UTC)
        val cards = historyCards(history)
        assertEquals(2, cards.size)
        assertEquals(setOf(listOf(1L), listOf(2L)), cards.map { card -> card.records.flatMap { it.setEntryIds } }.toSet())
    }

    @Test
    fun aV2WorkoutsSetsShareOneCard() {
        val history = CompatibilityHistory.merge(
            emptyList(), listOf(row(1, session = 5, reps = 10), row(2, session = 5, reps = 8).copy(setNumber = 2, orderIndex = 1)), ZoneOffset.UTC,
        )
        assertEquals(listOf(listOf(10, 8)), historyCards(history).map { card -> card.records.map { it.valueRight } })
    }

    @Test
    fun legacyCardsStayOnePerExerciseAndMinute() {
        val records = listOf(
            TrainingRecord(1, 1, 10, null, 1, "2026-03-02", "07:30", "a"),
            TrainingRecord(2, 1, 8, null, 1, "2026-03-02", "07:30", "b"),
            TrainingRecord(3, 1, 6, null, 1, "2026-03-01", "07:30", ""),
        )
        val cards = historyCards(CompatibilityHistory.merge(records, emptyList(), ZoneOffset.UTC))
        assertEquals(listOf(listOf(1L, 2L), listOf(3L)), cards.map { card -> card.records.mapNotNull { it.legacyRecordId }.sorted() })
    }
}
