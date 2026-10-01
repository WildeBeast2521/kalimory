package io.github.gonbei774.calisthenicsmemory

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Catalogue
import io.github.gonbei774.calisthenicsmemory.data.catalogue.Standard
import io.github.gonbei774.calisthenicsmemory.data.progression.ChainProgress
import io.github.gonbei774.calisthenicsmemory.data.progression.StepSession
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.progressionsSection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Progressions tab shows each followed chain and opens it; with none, it points to the catalogue. */
@RunWith(AndroidJUnit4::class)
class ProgressionsViewTest {
    @get:Rule
    val rule = createComposeRule()

    private val pull = Catalogue.chain("pull")!!
    private val chinUp = Catalogue.step("pull.chin")!!

    private fun show(progressions: List<ChainProgress>, onOpenChain: (String) -> Unit = {}, onOpenCatalogue: () -> Unit = {}) {
        rule.setContent {
            LazyColumn { progressionsSection(progressions, onOpenChain, onOpenCatalogue) }
        }
    }

    @Test
    fun aMetStandardOffersTheNextStepAndTheCardOpensItsChain() {
        var opened: String? = null
        show(
            listOf(
                ChainProgress(
                    chain = pull,
                    step = chinUp,
                    exercise = Exercise(id = 1, name = "My chin-ups", type = "Dynamic", catalogId = chinUp.id),
                    moveOn = Standard(3, 8),
                    lastSession = StepSession("2026-09-29", "08:00", listOf(8, 8, 8)),
                    percent = 100,
                    mastered = true,
                    next = Catalogue.step("pull.full"),
                )
            ),
            onOpenChain = { opened = it },
        )

        rule.onNodeWithText("My chin-ups").assertIsDisplayed()
        rule.onNodeWithText("Step 5 of 9").assertIsDisplayed()
        rule.onNodeWithText("Last session: 8, 8, 8").assertIsDisplayed()
        rule.onNodeWithText("Ready for the next step").assertIsDisplayed()
        rule.onNodeWithText("Next: Pull-up").assertIsDisplayed()

        rule.onNodeWithText("My chin-ups").performClick()
        assertEquals("pull", opened)
    }

    @Test
    fun withNoChainItPointsToTheCatalogue() {
        var browsed = false
        show(emptyList(), onOpenCatalogue = { browsed = true })

        rule.onNodeWithText("Browse the catalogue").performClick()
        assertEquals(true, browsed)
    }

    @Test
    fun withEveryChainUnfollowedItSaysSo() {
        rule.setContent {
            LazyColumn { progressionsSection(emptyList(), {}, {}, allUnfollowed = true) }
        }
        rule.onNodeWithText("You are not following any progression. Open one in the catalogue to follow it again.").assertIsDisplayed()
    }
}
