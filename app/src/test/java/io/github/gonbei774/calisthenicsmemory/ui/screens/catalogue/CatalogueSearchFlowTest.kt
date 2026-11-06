package io.github.gonbei774.calisthenicsmemory.ui.screens.catalogue

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.SemanticsMatcher
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.screens.MainScreenFixture
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Searching the catalogue in the app (#258): a result opens the step's sheet, which adds it to the library. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class CatalogueSearchFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    @Test fun `a search result opens the step sheet and adds the step`() {
        val library = hasText("Library") and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
        waitFor(library)
        compose.onNode(library).performClick()
        waitFor(hasText("Exercise catalogue") and hasClickAction())
        compose.onNode(hasText("Exercise catalogue") and hasClickAction()).performClick()
        waitFor(hasSetTextAction())
        compose.onNode(hasSetTextAction()).performTextInput("diamond")

        val result = hasText("Diamond push-up") and hasClickAction()
        waitFor(result)
        compose.onNode(hasText("Push-up · Step 5 of 9")).assertExists()
        compose.onNode(result).performClick()
        waitFor(hasText("How to do it"))
        compose.onNode(hasText("Add to my exercises") and hasClickAction()).performScrollTo().performClick()

        // The sheet closes and the result now shows the step is in the library.
        waitFor(result and hasContentDescription("In your library"))
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(MainScreenFixture.STARTUP_TIMEOUT_MS) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
}
