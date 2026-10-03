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

    @Test fun `unreadable exercise lists count only the part round`() {
        assertEquals(1, IntervalHistoryItem(record(2, 1, "not json")).completedSets)
    }
}
