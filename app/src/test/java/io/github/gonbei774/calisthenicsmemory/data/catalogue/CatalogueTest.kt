package io.github.gonbei774.calisthenicsmemory.data.catalogue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The rules every catalogue entry follows (ADR 0006). */
class CatalogueTest {
    private val steps = Catalogue.chains.flatMap { it.steps }
    private val idPattern = Regex("^[a-z]+(_[a-z]+)*\\.[a-z0-9]+(_[a-z0-9]+)*$")

    @Test fun `ids are unique and well formed`() {
        assertEquals(steps.size, steps.map { it.id }.toSet().size)
        assertEquals(Catalogue.chains.size, Catalogue.chains.map { it.id }.toSet().size)
        steps.forEach { assertTrue(it.id, idPattern.matches(it.id)) }
    }

    @Test fun `every step belongs to its chain`() {
        Catalogue.chains.forEach { chain ->
            assertTrue("${chain.id} has steps", chain.steps.isNotEmpty())
            chain.steps.forEach { step ->
                assertEquals(step.id, chain.id, step.chainId)
                assertTrue(step.id, step.id.startsWith(chain.id + "."))
            }
        }
    }

    @Test fun `difficulty stays on the scale and never drops along a chain`() {
        Catalogue.chains.forEach { chain ->
            chain.steps.forEach { assertTrue(it.id, it.difficulty in 1..10) }
            chain.steps.zipWithNext().forEach { (easier, harder) ->
                assertTrue("${harder.id} is easier than ${easier.id}", harder.difficulty >= easier.difficulty)
            }
        }
    }

    @Test fun `every working standard is below its move-on standard`() {
        steps.forEach { step ->
            val (working, moveOn) = step.working to step.moveOn
            assertTrue(step.id, working.sets in 1..moveOn.sets && working.value in 1 until moveOn.value)
        }
    }

    @Test fun `every step works some muscle`() {
        steps.forEach { step ->
            assertTrue(step.id, step.primaryMuscles.isNotEmpty())
            assertTrue(step.id, (step.primaryMuscles intersect step.secondaryMuscles).isEmpty())
        }
    }

    @Test fun `prerequisites point at other existing steps without cycles`() {
        steps.forEach { step ->
            step.prerequisites.forEach { assertTrue("${step.id} needs $it", Catalogue.step(it) != null && it != step.id) }
        }
        fun reaches(from: String, target: String, seen: MutableSet<String> = mutableSetOf()): Boolean =
            Catalogue.step(from)!!.prerequisites.any { it == target || (seen.add(it) && reaches(it, target, seen)) }
        steps.forEach { assertTrue("${it.id} is part of a cycle", !reaches(it.id, it.id)) }
    }

    @Test fun `every step has a name, a description and cues`() {
        // Unit tests run from the module directory, so the source strings file can be read directly.
        val xml = File("src/main/res/values/catalogue_strings.xml").readText()
        val strings = Regex("<string name=\"([a-z0-9_]+)\">([^<]*)</string>").findAll(xml).associate { it.groupValues[1] to it.groupValues[2] }
        Catalogue.chains.forEach { assertTrue(it.id, !strings["cat_chain_${it.id}"].isNullOrBlank()) }
        steps.forEach { step ->
            val key = "cat_" + step.id.replace('.', '_')
            assertTrue("$key name", !strings[key].isNullOrBlank())
            assertTrue("$key description", !strings["${key}_desc"].isNullOrBlank())
            val cues = strings["${key}_cues"].orEmpty().split("\\n")
            assertTrue("$key needs at least two cues", cues.size >= 2 && cues.all { it.isNotBlank() })
        }
    }

    @Test fun `lookups find every step and chain`() {
        steps.forEach { assertEquals(it, Catalogue.step(it.id)) }
        Catalogue.chains.forEach { assertEquals(it, Catalogue.chain(it.id)) }
        assertEquals(null, Catalogue.step("push.does_not_exist"))
    }
}
