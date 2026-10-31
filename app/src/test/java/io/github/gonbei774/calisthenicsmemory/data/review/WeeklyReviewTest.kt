package io.github.gonbei774.calisthenicsmemory.data.review

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.MovementPattern
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import io.github.gonbei774.calisthenicsmemory.data.v2.IntervalHistoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyReviewTest {
    // Monday 9 to Sunday 15 March 2026.
    private val monday = LocalDate.of(2026, 3, 9)

    private fun set(date: String, exerciseId: Long, source: HistorySource = HistorySource.V2, left: Int? = null) = HistorySet(
        source = source, legacyRecordId = if (source == HistorySource.LEGACY) 1 else null, setEntryIds = emptyList(),
        exerciseId = exerciseId, date = date, time = "07:30", setNumber = 1, valueRight = 10, valueLeft = left,
        comment = "", distanceCm = null, weightG = null, assistanceG = null,
    )

    private fun interval(date: String, rounds: Int, done: Int, inLast: Int = 0) = IntervalHistoryItem(
        IntervalRecord(
            programName = "Tabata", date = date, time = "18:00", workSeconds = 20, restSeconds = 10, rounds = rounds,
            roundRestSeconds = 0, completedRounds = done, completedExercisesInLastRound = inLast, exercisesJson = """["Burpee","Squat"]""",
        )
    )

    private fun exercise(id: Long, catalogId: String?) = Exercise(id = id, name = "E$id", type = "Dynamic", catalogId = catalogId)

    /** A real step of the first chain with [pattern], so renamed ids do not break the test. */
    private fun stepOf(pattern: MovementPattern) = Catalogue.chains.first { it.pattern == pattern }.steps.first().id

    private val push = exercise(1, stepOf(MovementPattern.HORIZONTAL_PUSH))
    private val unlinked = exercise(2, null)

    @Test fun `sets on the first and last day count, the days around them do not`() {
        val history = listOf("2026-03-08", "2026-03-09", "2026-03-15", "2026-03-16").map { set(it, push.id) }
        assertEquals(2, WeeklyReview.build(monday, history, emptyList(), listOf(push)).totalSets)
    }

    @Test fun `the total equals Today's count for the same week`() {
        val history = listOf(set("2026-03-09", push.id), set("2026-03-10", unlinked.id), set("2026-03-20", push.id))
        val intervals = listOf(interval("2026-03-11", rounds = 3, done = 3), interval("2026-03-02", rounds = 3, done = 1))
        val weekDates = (0L..6L).map { monday.plusDays(it).toString() }.toSet()
        // Today's formula (TodayScreen): history sets plus work intervals in the week.
        val today = history.count { it.date in weekDates } + intervals.filter { it.record.date in weekDates }.sumOf { it.completedSets }
        assertEquals(today, WeeklyReview.build(monday, history, intervals, listOf(push, unlinked)).totalSets)
    }

    @Test fun `a unilateral set with both sides is one set`() {
        val review = WeeklyReview.build(monday, listOf(set("2026-03-10", push.id, left = 9)), emptyList(), listOf(push))
        assertEquals(1, review.totalSets)
    }

    @Test fun `legacy and v2 sets both count`() {
        val history = listOf(set("2026-03-10", push.id, HistorySource.LEGACY), set("2026-03-10", push.id, HistorySource.V2))
        assertEquals(listOf(FamilySets(MovementFamily.PUSH, 2)), WeeklyReview.build(monday, history, emptyList(), listOf(push)).families)
    }

    @Test fun `linked exercises count under their chain's group`() {
        val cases = mapOf(
            MovementPattern.HORIZONTAL_PUSH to MovementFamily.PUSH, MovementPattern.VERTICAL_PULL to MovementFamily.PULL,
            MovementPattern.HINGE to MovementFamily.LEGS, MovementPattern.CORE_FLEXION to MovementFamily.CORE,
            MovementPattern.MOBILITY to MovementFamily.MOBILITY,
        )
        cases.entries.forEachIndexed { index, (pattern, family) ->
            assertEquals(pattern.name, family, WeeklyReview.familyOf(exercise(index + 10L, stepOf(pattern))))
        }
    }

    @Test fun `every pattern maps to its group as the table says`() {
        val expected = mapOf(
            MovementPattern.HORIZONTAL_PUSH to MovementFamily.PUSH, MovementPattern.VERTICAL_PUSH to MovementFamily.PUSH,
            MovementPattern.HORIZONTAL_PULL to MovementFamily.PULL, MovementPattern.VERTICAL_PULL to MovementFamily.PULL,
            MovementPattern.SQUAT to MovementFamily.LEGS, MovementPattern.HINGE to MovementFamily.LEGS,
            MovementPattern.CORE_ANTI_EXTENSION to MovementFamily.CORE, MovementPattern.CORE_FLEXION to MovementFamily.CORE,
            MovementPattern.CORE_LATERAL to MovementFamily.CORE, MovementPattern.CONDITIONING to MovementFamily.CONDITIONING,
            MovementPattern.MOBILITY to MovementFamily.MOBILITY,
        )
        assertEquals(MovementPattern.entries.toSet(), expected.keys)
        expected.forEach { (pattern, family) -> assertEquals(pattern.name, family, pattern.family()) }
        // Only the strength groups carry the 10-set mark.
        assertEquals(setOf(MovementFamily.PUSH, MovementFamily.PULL, MovementFamily.LEGS), MovementFamily.entries.filter { it.hasReference }.toSet())
    }

    @Test fun `every catalogue step has a group`() {
        Catalogue.chains.flatMap { it.steps }.forEach { step ->
            assertTrue(step.id, WeeklyReview.familyOf(exercise(1, step.id)) != null)
        }
    }

    @Test fun `an exercise without a catalogue link counts as other exercises`() {
        val review = WeeklyReview.build(monday, listOf(set("2026-03-10", unlinked.id)), emptyList(), listOf(unlinked))
        assertEquals(1, review.otherSets)
        assertTrue(review.families.isEmpty())
    }

    @Test fun `an unknown catalogue id counts as other exercises`() {
        val newer = exercise(3, "push.not_yet")
        assertEquals(1, WeeklyReview.build(monday, listOf(set("2026-03-10", newer.id)), emptyList(), listOf(newer)).otherSets)
    }

    @Test fun `sets of an exercise missing from the library count as other exercises`() {
        assertEquals(1, WeeklyReview.build(monday, listOf(set("2026-03-10", 99)), emptyList(), emptyList()).otherSets)
    }

    @Test fun `interval work counts its work intervals`() {
        // A full run of 3 rounds of 2 exercises is 6 intervals; a run stopped after 1 round and 1 exercise is 3.
        val intervals = listOf(interval("2026-03-10", rounds = 3, done = 3, inLast = 2), interval("2026-03-12", rounds = 3, done = 1, inLast = 1))
        val review = WeeklyReview.build(monday, emptyList(), intervals, emptyList())
        assertEquals(9, review.intervalSets)
        assertEquals(9, review.totalSets)
    }

    @Test fun `days trained counts distinct days, interval-only days included`() {
        val history = listOf(set("2026-03-09", push.id), set("2026-03-09", push.id), set("2026-03-11", push.id))
        val review = WeeklyReview.build(monday, history, listOf(interval("2026-03-13", 2, 2)), listOf(push))
        assertEquals(3, review.daysTrained)
    }

    @Test fun `unreadable dates are skipped`() {
        val history = listOf(set("2026-3-9", push.id), set("", push.id), set("2026-03-09", push.id))
        assertEquals(1, WeeklyReview.build(monday, history, emptyList(), listOf(push)).totalSets)
    }

    @Test fun `groups come in a fixed order and only when trained`() {
        val legs = exercise(4, stepOf(MovementPattern.SQUAT))
        val core = exercise(5, stepOf(MovementPattern.CORE_ANTI_EXTENSION))
        val history = listOf(set("2026-03-10", core.id), set("2026-03-10", legs.id), set("2026-03-11", push.id), set("2026-03-11", push.id))
        val review = WeeklyReview.build(monday, history, emptyList(), listOf(push, legs, core))
        assertEquals(
            listOf(FamilySets(MovementFamily.PUSH, 2), FamilySets(MovementFamily.LEGS, 1), FamilySets(MovementFamily.CORE, 1)),
            review.families,
        )
    }

    @Test fun `an empty week is empty`() {
        val review = WeeklyReview.build(monday, listOf(set("2026-03-02", push.id)), emptyList(), listOf(push))
        assertTrue(review.isEmpty)
        assertEquals(0, review.daysTrained)
    }

    @Test fun `the first day of the week changes which sets are in it`() {
        val history = listOf(set("2026-03-08", push.id), set("2026-03-15", push.id))
        // Sunday 8 to Saturday 14 holds the 8th; Monday 9 to Sunday 15 holds the 15th.
        assertEquals(1, WeeklyReview.build(LocalDate.of(2026, 3, 8), history, emptyList(), listOf(push)).totalSets)
        assertEquals(1, WeeklyReview.build(monday, history, emptyList(), listOf(push)).totalSets)
        assertEquals(2, WeeklyReview.build(LocalDate.of(2026, 3, 8), history + set("2026-03-09", push.id), emptyList(), listOf(push)).totalSets)
    }

    @Test fun `the first trained day is the earliest across sets and intervals`() {
        assertNull(WeeklyReview.firstTrainedDay(emptyList(), emptyList()))
        val first = WeeklyReview.firstTrainedDay(listOf(set("2026-03-10", push.id), set("bad", push.id)), listOf(interval("2026-02-27", 2, 2)))
        assertEquals(LocalDate.of(2026, 2, 27), first)
    }

    @Test fun `moving between weeks stops at the first trained week and at the current week`() {
        val first = LocalDate.of(2026, 3, 4)
        assertTrue(WeeklyReview.canGoBack(monday, first))
        assertFalse(WeeklyReview.canGoBack(LocalDate.of(2026, 3, 2), first))
        assertFalse(WeeklyReview.canGoBack(monday, null))
        assertTrue(WeeklyReview.canGoForward(LocalDate.of(2026, 3, 2), monday))
        assertFalse(WeeklyReview.canGoForward(monday, monday))
    }
}
