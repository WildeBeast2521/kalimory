package io.github.gonbei774.calisthenicsmemory.data.progression

import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** A chain's climb over time (#161), from the journey's stored dates. */
class ChainClimbTest {
    private val chain = Catalogue.chain("pull")!!
    private val today = LocalDate.of(2026, 10, 8)
    private fun step(n: Int) = chain.steps[n - 1]
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun `no trained step means no climb`() {
        assertNull(Climb.of(chain, emptyList(), today))
    }

    @Test fun `the line holds the highest step started, and rises only when it goes up`() {
        val journey = listOf(
            StepJourney(step(1), "2026-01-05", "2026-02-02"),
            StepJourney(step(2), "2026-02-04", "2026-04-01"),
            StepJourney(step(3), "2026-04-03", null),
        )
        val climb = Climb.of(chain, journey, today)!!
        assertEquals(listOf(d("2026-01-05") to 1, d("2026-02-04") to 2, d("2026-04-03") to 3), climb.levels)
        assertEquals(1 to 3, climb.firstStep to climb.highestStep)
        assertEquals(d("2026-04-03"), climb.highestOn)
        assertEquals(d("2026-01-05") to today, climb.start to climb.end)
    }

    @Test fun `going back to an easier step does not read as a fall`() {
        // Step 4 started first, then step 2 later: the line stays at 4.
        val journey = listOf(StepJourney(step(2), "2026-05-01", null), StepJourney(step(4), "2026-03-01", null))
        val climb = Climb.of(chain, journey, today)!!
        assertEquals(listOf(d("2026-03-01") to 4), climb.levels)
        assertEquals(2, climb.marks.size)
    }

    @Test fun `steps started on the first day start the climb at the highest of them`() {
        val journey = listOf(StepJourney(step(1), "2026-03-01", null), StepJourney(step(3), "2026-03-01", null))
        assertEquals(listOf(d("2026-03-01") to 3), Climb.of(chain, journey, today)!!.levels)
    }

    @Test fun `every start and every first meeting is a mark, in date order`() {
        val journey = listOf(StepJourney(step(1), "2026-01-05", "2026-02-02"), StepJourney(step(2), "2026-02-04", null))
        assertEquals(
            listOf(ClimbMark(d("2026-01-05"), 1, false), ClimbMark(d("2026-02-02"), 1, true), ClimbMark(d("2026-02-04"), 2, false)),
            Climb.of(chain, journey, today)!!.marks,
        )
    }

    @Test fun `unreadable dates are skipped, never guessed`() {
        val journey = listOf(StepJourney(step(1), "2026-1-5", null), StepJourney(step(2), "2026-02-04", "bad"))
        val climb = Climb.of(chain, journey, today)!!
        assertEquals(listOf(ClimbMark(d("2026-02-04"), 2, false)), climb.marks)
    }

    @Test fun `a date after today stretches the end to it`() {
        val climb = Climb.of(chain, listOf(StepJourney(step(1), "2026-12-01", null)), today)!!
        assertEquals(d("2026-12-01"), climb.end)
    }
}
