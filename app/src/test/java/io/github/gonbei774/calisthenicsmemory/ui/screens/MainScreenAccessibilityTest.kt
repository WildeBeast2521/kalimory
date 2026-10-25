package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.screens.MainScreenFixture.STARTUP_TIMEOUT_MS
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
 * What TalkBack and touch need on the four main screens: every element that can be tapped says what
 * it is (a text, a description or a state) and is at least 48 × 48 dp, from the real app with the
 * screenshot tests' made-up history.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class MainScreenAccessibilityTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    @Test fun today() = audit("Today", waitFor = MainScreenFixture.PROGRAM)

    @Test fun train() {
        open("Train")
        audit("Train", waitFor = MainScreenFixture.PROGRAM)
    }

    @Test fun progress() {
        open("Progress")
        audit("Progress", waitFor = "Australian row")
    }

    @Test fun library() {
        open("Library")
        audit("Library", waitFor = "4")
    }

    private fun open(tab: String) {
        val item = hasText(tab) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
        compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(item).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(item).performClick()
    }

    private fun audit(screen: String, waitFor: String) {
        compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(hasText(waitFor)).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        val problems = tapTargetProblems(compose, compose.density).map { "$screen: $it" }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
