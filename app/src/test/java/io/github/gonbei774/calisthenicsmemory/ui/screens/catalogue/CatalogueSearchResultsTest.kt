package io.github.gonbei774.calisthenicsmemory.ui.screens.catalogue

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.CatalogueSearch
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Equipment
import io.github.gonbei774.calisthenicsmemory.ui.screens.tapTargetProblems
import io.github.gonbei774.calisthenicsmemory.ui.screens.textFitProblems
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The catalogue search on the real catalogue text (#258): what it finds in each language and how results read. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class CatalogueSearchResultsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun entries(): List<CatalogueSearch.Entry> {
        var list = emptyList<CatalogueSearch.Entry>()
        compose.setContent { list = searchEntries() }
        compose.waitForIdle()
        return list
    }

    private fun first(query: String) = CatalogueSearch.search(entries(), query).firstOrNull()?.step?.id

    @Test fun `every step is found by its own name, first`() {
        val all = entries()
        assertEquals(Catalogue.chains.sumOf { chain -> chain.steps.count { !it.retired } }, all.size)
        all.forEach { entry ->
            // Two steps may share a name; either way the exact name comes first.
            val found = CatalogueSearch.search(all, entry.name)
            assertEquals(entry.step.id, entry.name, found.first().name)
            assertTrue(entry.step.id, found.any { it.step.id == entry.step.id })
        }
    }

    @Test fun `muscles and equipment find steps in English`() {
        val all = entries()
        val ringSteps = all.filter { Equipment.RINGS in it.step.equipment }.map { it.step.id }.toSet()
        assertTrue(ringSteps.isNotEmpty())
        assertTrue(CatalogueSearch.search(all, "rings").map { it.step.id }.containsAll(ringSteps))
        assertTrue(CatalogueSearch.search(all, "triceps").any { it.step.id == "push.diamond" })
    }

    @Test @Config(qualifiers = "fr-w360dp-h740dp-xhdpi")
    fun `French matches without accents`() = assertEquals("push.decline", first("pompe declinee"))

    @Test @Config(qualifiers = "ar-w360dp-h740dp-xhdpi")
    fun `Arabic matches without harakat or hamza`() {
        val all = entries()
        assertEquals("pull.dead_hang", CatalogueSearch.search(all, "التعلق").first().step.id)
        assertEquals("row.horizontal", CatalogueSearch.search(all, "التجديف الافقي").first().step.id)
    }

    @Test @Config(qualifiers = "ja-w360dp-h740dp-xhdpi")
    fun `Japanese matches part of a name`() = assertEquals("push.diamond", first("ダイヤモンド"))

    @Test @Config(qualifiers = "zh-rCN-w360dp-h740dp-xhdpi")
    fun `Chinese matches part of a name`() = assertEquals("push.diamond", first("钻石"))

    @Test fun `a result names its chain and place, and opens on tap`() {
        var opened: String? = null
        lateinit var results: List<CatalogueSearch.Entry>
        compose.setContent {
            CalisthenicsMemoryTheme {
                results = CatalogueSearch.search(searchEntries(), "diamond")
                CatalogueSearchResults(results, linked = setOf("push.diamond")) { opened = it.step.id }
            }
        }
        val diamond = results.first { it.step.id == "push.diamond" }
        compose.onNodeWithText("Push-up · Step ${diamond.number} of ${diamond.chain.steps.size}").assertExists()
        compose.onNodeWithContentDescription("In your library").assertExists()
        compose.onNodeWithText("Diamond push-up").performClick()
        assertEquals("push.diamond", opened)
    }

    @Test fun `no match says so`() {
        compose.setContent { CalisthenicsMemoryTheme { CatalogueSearchResults(emptyList(), emptySet()) {} } }
        compose.onNodeWithText("No results found").assertExists()
    }

    // Three rows, so all of them are inside the window; the screen scrolls the full list.
    @Test fun `at 200 percent font results fit and every row is a big enough target`() {
        RuntimeEnvironment.setFontScale(2f)
        try {
            compose.setContent {
                CalisthenicsMemoryTheme { CatalogueSearchResults(CatalogueSearch.search(searchEntries(), "push").take(3), setOf("push.wall")) {} }
            }
            compose.waitForIdle()
            val text = textFitProblems(compose)
            assertTrue(text.joinToString("\n"), text.isEmpty())
            val taps = tapTargetProblems(compose, compose.density)
            assertTrue(taps.joinToString("\n"), taps.isEmpty())
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }
}
