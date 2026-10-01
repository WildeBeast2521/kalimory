package io.github.gonbei774.calisthenicsmemory.data.progression

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Standard
import io.github.gonbei774.calisthenicsmemory.data.v2.ExerciseKind
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Today's suggestions (ADR 0005, decisions 4 and 5), from the library and history alone. */
class SuggestionsTest {
    private val today = LocalDate.of(2026, 10, 1)

    private fun exercise(id: Long, catalogId: String, targetSets: Int? = null, targetValue: Int? = null) =
        Exercise(id = id, name = "Exercise $id", type = "Dynamic", targetSets = targetSets, targetValue = targetValue, catalogId = catalogId)

    private fun session(exerciseId: Long, date: String, vararg values: Int, time: String = "08:00") =
        values.mapIndexed { index, value ->
            HistorySet(HistorySource.V2, null, emptyList(), exerciseId, date, time, index + 1, value, null, "", null, null, null)
        }

    @Test fun `an untrained step starts at its working standard`() {
        val step = Catalogue.step("push.incline")!!
        val suggestion = Suggestions.forDay(today, listOf(exercise(1, step.id)), emptyList()).single()

        assertEquals(SuggestionReason.FIRST_SESSION, suggestion.reason)
        assertEquals(step.working, suggestion.target)
    }

    @Test fun `the target adds one rep to the weakest set that counts`() {
        val exercises = listOf(exercise(1, "pull.chin", 3, 8))
        val suggestion = Suggestions.forDay(today, exercises, session(1, "2026-09-28", 7, 6, 6, 3)).single()

        assertEquals(SuggestionReason.CONTINUE, suggestion.reason)
        assertEquals(Standard(3, 7), suggestion.target)
        assertEquals(listOf(7, 6, 6, 3), suggestion.lastSession!!.values)
    }

    @Test fun `the target never passes the move-on standard and holds add seconds`() {
        assertEquals(Standard(3, 8), Suggestions.nextTarget(StepSession("d", "t", listOf(9, 8, 8)), Standard(3, 8), ExerciseKind.DYNAMIC))
        assertEquals(Standard(3, 25), Suggestions.nextTarget(StepSession("d", "t", listOf(30, 20, 20)), Standard(3, 30), ExerciseKind.ISOMETRIC))
    }

    @Test fun `missing sets are completed before the reps go up`() {
        assertEquals(Standard(3, 5), Suggestions.nextTarget(StepSession("d", "t", listOf(6, 5)), Standard(3, 8), ExerciseKind.DYNAMIC))
    }

    @Test fun `a met standard suggests the next step at its working standard`() {
        val next = Catalogue.step("pull.full")!!
        val exercises = listOf(exercise(1, "pull.chin", 3, 8), exercise(2, next.id))
        val suggestion = Suggestions.forDay(today, exercises, session(1, "2026-09-28", 8, 8, 8)).single()

        assertEquals(SuggestionReason.NEXT_STEP, suggestion.reason)
        assertEquals(next, suggestion.step)
        assertEquals(2L, suggestion.exercise!!.id)
        assertEquals(next.working, suggestion.target)
    }

    @Test fun `a met standard names the next step even before it is added`() {
        val suggestion = Suggestions.forDay(today, listOf(exercise(1, "pull.chin", 3, 8)), session(1, "2026-09-28", 8, 8, 8)).single()

        assertEquals(SuggestionReason.NEXT_STEP_NOT_ADDED, suggestion.reason)
        assertEquals("pull.full", suggestion.step.id)
        assertNull(suggestion.exercise)
    }

    @Test fun `a movement trained today or yesterday rests, and so does its pattern`() {
        // Dip and handstand push-up are both vertical pushes.
        val exercises = listOf(exercise(1, "dip.bench"), exercise(2, "vpush.pike"), exercise(3, "row.incline"))
        val history = session(1, "2026-09-30", 10) + session(3, "2026-09-29", 10)

        val chains = Suggestions.forDay(today, exercises, history).map { it.chain.id }

        assertEquals(listOf("row"), chains)
    }

    @Test fun `the least recently trained chain comes first, untrained before all`() {
        val exercises = listOf(exercise(1, "pull.chin"), exercise(2, "push.full"), exercise(3, "squat.full"))
        val history = session(1, "2026-09-20", 5) + session(2, "2026-09-25", 5)

        assertEquals(listOf("squat", "pull", "push"), Suggestions.forDay(today, exercises, history).map { it.chain.id })
    }

    @Test fun `a dismissed chain is left out for the day`() {
        val exercises = listOf(exercise(1, "pull.chin"), exercise(2, "push.full"))
        assertEquals(listOf("push"), Suggestions.forDay(today, exercises, emptyList(), dismissedChains = setOf("pull")).map { it.chain.id })
    }

    @Test fun `two sessions short of the working standard offer the easier step`() {
        val chin = Catalogue.step("pull.chin")!!
        val history = session(1, "2026-09-20", 2, 2, 1) + session(1, "2026-09-24", 3, 2, 2)

        val suggestion = Suggestions.forDay(today, listOf(exercise(1, chin.id)), history).single()

        assertTrue(Progressions.meets(StepSession("d", "t", listOf(chin.working.value, chin.working.value, chin.working.value)), chin.working))
        assertEquals(Catalogue.step("pull.band"), suggestion.easier)
        // Still a suggestion for the current step: the easier one is only offered.
        assertEquals(chin, suggestion.step)
    }

    @Test fun `an unfollowed chain is not suggested, but its training still rests the pattern`() {
        // Dip is no longer followed, yet a dip yesterday still rests the handstand push-up (both vertical pushes).
        val exercises = listOf(exercise(1, "dip.bench"), exercise(2, "vpush.pike"), exercise(3, "row.incline"))
        val history = session(1, "2026-09-30", 10)

        val chains = Suggestions.forDay(today, exercises, history, unfollowedChains = setOf("dip")).map { it.chain.id }

        assertEquals(listOf("row"), chains)
    }
}
