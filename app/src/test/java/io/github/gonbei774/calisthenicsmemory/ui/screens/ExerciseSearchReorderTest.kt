package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Search results in Exercises and groups show only some of a group's exercises, so they offer no
 * reordering and no group menu: a drag there used to reorder the whole group by filtered positions.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class ExerciseSearchReorderTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    private val dragHandle = hasContentDescription("Drag to reorder")
    private val groupMenu = hasContentDescription("Menu")

    @Test fun `search results offer no reordering and no group menu`() {
        val library = hasText("Library") and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
        waitFor(library)
        compose.onNode(library).performClick()
        waitFor(hasText("Exercises and groups") and hasClickAction())
        compose.onNode(hasText("Exercises and groups") and hasClickAction()).performClick()

        // Outside search, an open group can be reordered and has its menu.
        waitFor(hasText("Push") and hasClickAction())
        val menusBefore = count(groupMenu)
        compose.onNode(hasText("Push") and hasClickAction()).performClick()
        waitFor(dragHandle)

        // "pike" leaves one of Push's two exercises.
        compose.onNode(hasSetTextAction()).performTextInput("pike")
        waitFor(hasText("Pike push-up"))
        assertEquals(0, count(dragHandle))
        assertTrue("group menus still shown while searching", count(groupMenu) < menusBefore)
        assertEquals(0, compose.onAllNodes(groupMenu and hasAnyAncestor(hasText("Push")), useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    // Icons are counted in the unmerged tree, where each keeps its own description.
    private fun count(matcher: SemanticsMatcher) = compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(MainScreenFixture.STARTUP_TIMEOUT_MS) {
            compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() || count(matcher) > 0
        }
        compose.waitForIdle()
    }
}
