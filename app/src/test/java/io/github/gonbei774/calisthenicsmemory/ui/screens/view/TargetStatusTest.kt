package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

/** Progressions › Targets judges a target as the chain cards do. */
class TargetStatusTest {
    private val pushUp = Exercise(id = 1, name = "Push-up", type = "Dynamic", targetSets = 3, targetValue = 10)

    private fun set(n: Int, value: Int, date: String = "2026-03-02", left: Int? = null) =
        HistorySet(HistorySource.V2, null, emptyList(), 1, date, "07:00", n, value, left, "", null, null, null)

    private fun status(sets: List<HistorySet>, exercise: Exercise = pushUp) = calculateChallengeStatus(exercise, sets, null, DayOfWeek.MONDAY)

    @Test fun `one big set does not clear a multi-set target`() {
        // The old rule summed sets uncapped: 30 reps "cleared" 3 x 10.
        val result = status(listOf(set(1, 30)))
        assertEquals(ChallengeResult.NeedWork, result.status)
        assertEquals(33, result.achievementRate)
    }

    @Test fun `every set reaching the target clears it, dated by the latest session that did`() {
        val result = status(listOf(set(1, 10, "2026-03-02"), set(2, 10, "2026-03-02"), set(3, 11, "2026-03-02")))
        assertEquals(ChallengeResult.Perfect, result.status)
        assertEquals("2026-03-02", result.lastAchievedDate)
    }

    @Test fun `a one-sided set counts its weaker side`() {
        val oneSided = pushUp.copy(laterality = "Unilateral", targetSets = 1)
        assertEquals(ChallengeResult.NearlyThere, status(listOf(set(1, value = 10, left = 6)), oneSided).status)
    }

    @Test fun `the day strip clears a day by the same rule`() {
        val today = io.github.gonbei774.calisthenicsmemory.util.AppClock.today().toString()
        val oneBigSet = calculateClearDays(pushUp, listOf(set(1, 30, today)), Period.OneWeek, DayOfWeek.MONDAY)
        assertEquals(0, oneBigSet.clearCount)
        val threeSets = calculateClearDays(pushUp, (1..3).map { set(it, 10, today) }, Period.OneWeek, DayOfWeek.MONDAY)
        assertEquals(1, threeSets.clearCount)
    }
}
