package io.github.gonbei774.calisthenicsmemory.data.v2

import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalHistoryItemTest {

    private fun record(completedRounds: Int, inLastRound: Int, exercisesJson: String) = IntervalRecord(
        programName = "Tabata", date = "2026-10-03", time = "15:25", workSeconds = 40, restSeconds = 20, rounds = 3,
        roundRestSeconds = 60, completedRounds = completedRounds, completedExercisesInLastRound = inLastRound,
        exercisesJson = exercisesJson,
    )

    @Test fun `each work interval done counts as one set for the weekly total`() {
        assertEquals(5, IntervalHistoryItem(record(2, 1, """["Burpee","Squat"]""")).completedSets)
        assertEquals(2, IntervalHistoryItem(record(2, 0, """["Burpee"]""")).completedSets)
    }

    @Test fun `a full run counts each round once`() {
        // A finished run is stored as (rounds, all exercises): 3 rounds of 2 exercises is 6 sets, not 8.
        assertEquals(6, IntervalHistoryItem(record(3, 2, """["Burpee","Squat"]""")).completedSets)
        assertEquals(3, IntervalHistoryItem(record(3, 1, """["Burpee"]""")).completedSets)
    }

    @Test fun `a v2 full run read back through the history counts each round once`() {
        val rows = (1..3).flatMap { round ->
            listOf("Burpee", "Squat").mapIndexed { index, name ->
                IntervalSessionRow(7, "Tabata", 0, null, 40, 20, 3, 60, index, name, round, SetEntryStatus.COMPLETED)
            }
        }
        val item = IntervalHistory.merge(emptyList(), rows, java.time.ZoneOffset.UTC).single()
        assertEquals(6, item.completedSets)
    }

    @Test fun `unreadable exercise lists count only the part round`() {
        assertEquals(1, IntervalHistoryItem(record(2, 1, "not json")).completedSets)
    }
}
