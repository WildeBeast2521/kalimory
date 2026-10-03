package io.github.gonbei774.calisthenicsmemory.data.catalogue

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Each step suggests its own rest, so not every exercise rests 240 s (owner report, 2026-10-03). */
class RecommendedRestTest {
    @Test fun `mobility and conditioning rest briefly`() {
        assertEquals(30, Catalogue.step("flex_lower.deep_squat")!!.recommendedRestSeconds())
        assertEquals(60, Catalogue.step("burpee.full")!!.recommendedRestSeconds())
    }

    @Test fun `strength rest grows with difficulty`() {
        assertEquals(60, Catalogue.step("push.wall")!!.recommendedRestSeconds())
        assertEquals(90, Catalogue.step("push.full")!!.recommendedRestSeconds())
        assertEquals(180, Catalogue.step("planche.full")!!.recommendedRestSeconds())
    }

    @Test fun `every step has a rest between 30 seconds and 3 minutes`() {
        Catalogue.chains.flatMap { it.steps }.forEach { step ->
            assertTrue(step.id, step.recommendedRestSeconds() in 30..180)
        }
    }

    @Test fun `an exercise linked to the catalogue uses its step's rest, a custom one has none`() {
        val linked = Exercise(name = "Push-up", type = "Dynamic", catalogId = "push.full")
        val custom = Exercise(name = "My move", type = "Dynamic")
        assertEquals(90, linked.recommendedRestSeconds())
        assertNull(custom.recommendedRestSeconds())
    }
}
