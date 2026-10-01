package io.github.gonbei774.calisthenicsmemory.data.progression

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Standard
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySet
import io.github.gonbei774.calisthenicsmemory.data.v2.HistorySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Where the user stands in each chain, from the library and history alone (ADR 0005). */
class ProgressionsTest {
    private val pull = Catalogue.chain("pull")!!
    private val negative = Catalogue.step("pull.negative")!!
    private val chinUp = Catalogue.step("pull.chin")!!
    private val oneArm = pull.steps.last()

    private fun exercise(id: Long, catalogId: String?, targetSets: Int? = null, targetValue: Int? = null, laterality: String = "Bilateral") =
        Exercise(id = id, name = "Exercise $id", type = "Dynamic", laterality = laterality, targetSets = targetSets, targetValue = targetValue, catalogId = catalogId)

    private fun set(exerciseId: Long, date: String, value: Int, left: Int? = null, time: String = "08:00", source: HistorySource = HistorySource.V2) =
        HistorySet(source, null, emptyList(), exerciseId, date, time, 1, value, left, "", null, null, null)

    @Test fun `chains without a step in the library are not followed`() {
        assertTrue(Progressions.chains(listOf(exercise(1, null)), emptyList()).isEmpty())
    }

    @Test fun `an untrained chain starts at its easiest step in the library`() {
        val result = Progressions.chains(listOf(exercise(1, chinUp.id), exercise(2, negative.id)), emptyList()).single()

        assertEquals(negative, result.step)
        assertNull(result.lastSession)
        assertEquals(0, result.percent)
        assertFalse(result.mastered)
        assertEquals(pull.steps[pull.steps.indexOf(negative) + 1], result.next)
    }

    @Test fun `the current step is the hardest one trained`() {
        val exercises = listOf(exercise(1, negative.id), exercise(2, chinUp.id), exercise(3, oneArm.id))
        val history = listOf(set(1, "2026-09-01", 5), set(2, "2026-09-03", 4))

        val result = Progressions.chains(exercises, history).single()

        assertEquals(chinUp, result.step)
        assertEquals(listOf(4), result.lastSession!!.values)
    }

    @Test fun `progress counts the best sets up to the standard, from the latest session`() {
        val exercises = listOf(exercise(1, chinUp.id, targetSets = 3, targetValue = 8))
        val history = listOf(
            set(1, "2026-09-01", 8), set(1, "2026-09-01", 8), set(1, "2026-09-01", 8),
            set(1, "2026-09-05", 12), set(1, "2026-09-05", 6), set(1, "2026-09-05", 4), set(1, "2026-09-05", 2),
        )

        val result = Progressions.chains(exercises, history).single()

        // 8 (12 capped) + 6 + 4 of 24.
        assertEquals(75, result.percent)
        assertTrue("met on 1 September", result.mastered)
        assertEquals(listOf(12, 6, 4, 2), result.lastSession!!.values)
    }

    @Test fun `a big first set does not make up for short ones`() {
        val session = StepSession("2026-09-01", "08:00", listOf(16, 8, 0))
        assertFalse(Progressions.meets(session, Standard(3, 8)))
        assertEquals(66, Progressions.percentOf(session, Standard(3, 8)))
    }

    @Test fun `the user's own target is the move-on standard`() {
        assertEquals(Standard(5, 5), Progressions.moveOnFor(exercise(1, chinUp.id, 5, 5), chinUp))
        assertEquals(chinUp.moveOn, Progressions.moveOnFor(exercise(1, chinUp.id), chinUp))
    }

    @Test fun `unilateral sets count the weaker side`() {
        val sessions = Progressions.sessions(listOf(set(1, "2026-09-01", 6, left = 4)), unilateral = true)
        assertEquals(listOf(4), sessions.single().values)
    }

    @Test fun `sets at the same time from different stores are separate sessions`() {
        val sessions = Progressions.sessions(
            listOf(set(1, "2026-09-01", 5, source = HistorySource.LEGACY), set(1, "2026-09-01", 6)),
            unilateral = false,
        )
        assertEquals(2, sessions.size)
    }

    @Test fun `the top step has no next step`() {
        val result = Progressions.chains(listOf(exercise(1, oneArm.id)), emptyList()).single()
        assertNull(result.next)
    }

    @Test fun `a chain the user stopped following is left out`() {
        val exercises = listOf(exercise(1, chinUp.id), exercise(2, "push.full"))
        assertEquals(listOf("push"), Progressions.chains(exercises, emptyList(), unfollowed = setOf("pull")).map { it.chain.id })
    }
}
