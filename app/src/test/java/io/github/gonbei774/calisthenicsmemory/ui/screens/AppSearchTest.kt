package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Search finds what a section shows (#278): group names in the library and in an exercise picker. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class AppSearchTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    private fun open(tab: String) {
        val item = hasText(tab) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
        waitFor(item)
        compose.onNode(item).performClick()
    }

    private fun search(text: String) {
        waitFor(hasSetTextAction())
        compose.onNode(hasSetTextAction()).performTextInput(text)
        compose.waitForIdle()
    }

    private fun count(matcher: SemanticsMatcher) = compose.onAllNodes(matcher).fetchSemanticsNodes().size

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(MainScreenFixture.STARTUP_TIMEOUT_MS) { count(matcher) > 0 }
        compose.waitForIdle()
    }

    @Test fun `the library finds a group by its name, with all its exercises`() {
        open("Library")
        waitFor(hasText("Exercises and groups") and hasClickAction())
        compose.onNode(hasText("Exercises and groups") and hasClickAction()).performClick()
        // "Australian row" is in the Pull group; its own name has no "pull".
        search("pull")
        waitFor(hasText("Australian row"))
        assertEquals(0, count(hasText("Push-up")))
    }

    @Test fun `a picker finds an exercise by its group and shows the group`() {
        open("Train")
        // The row is at the end of Train's list, below the fold.
        val logPast = hasText("Log a past workout") and hasClickAction()
        waitFor(hasScrollAction())
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(logPast)
        compose.onNode(logPast).performClick()
        search("pull")
        waitFor(hasText("Australian row", substring = true))
        // The result's card carries the group's badge beside the name.
        compose.onNode(hasText("Australian row") and hasText("Pull")).assertExists()
        assertEquals(0, count(hasText("Pike push-up", substring = true)))
    }
}
