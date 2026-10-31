package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
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
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
/**
 * The four main screens at Android's largest font size (200 %) on a small phone: no text is cut
 * off, shortened with "…" or split inside a word (textFitProblems).
 */
class LargeTextTest {
    private val fontScale = object : ExternalResource() {
        override fun before() = RuntimeEnvironment.setFontScale(2f)
        override fun after() = RuntimeEnvironment.setFontScale(1f)
    }
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(fontScale).around(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    private fun show(tab: String?, waitFor: String, label: String = tab ?: "Today") {
        if (tab != null) {
            val item = hasText(tab) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))
            compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(item).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(item).performClick()
        }
        compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(hasText(waitFor)).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        val problems = textFitProblems(compose).map { "$label: $it" }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test fun settings() {
        compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(hasContentDescription("Settings")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasContentDescription("Settings")).performClick()
        show(null, "Settings", label = "Settings")
    }

    @Test fun weeklyReview() {
        val button = hasText("Weekly review") and hasClickAction()
        compose.waitUntil(STARTUP_TIMEOUT_MS) { compose.onAllNodes(button).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(button).performClick()
        show(null, "This week", label = "Weekly review")
    }

    @Test fun today() = show(null, MainScreenFixture.PROGRAM)
    @Test fun train() = show("Train", MainScreenFixture.PROGRAM)
    @Test fun progress() = show("Progress", "Progress")
    @Test fun library() = show("Library", "4")
}
