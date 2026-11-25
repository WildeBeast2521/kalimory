package io.github.gonbei774.calisthenicsmemory.ui.screens.whatsnew

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.LayoutDirection
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.ChangeKind
import io.github.gonbei774.calisthenicsmemory.data.ChangeSection
import io.github.gonbei774.calisthenicsmemory.data.Release
import io.github.gonbei774.calisthenicsmemory.ui.screens.MainScreenFixture
import io.github.gonbei774.calisthenicsmemory.ui.screens.tapTargetProblems
import io.github.gonbei774.calisthenicsmemory.ui.screens.textFitProblems
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The "What's new" list with fixed releases: what it shows, how versions open, and its text and touch rules. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class WhatsNewSheetTest {
    private val compose = createComposeRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    private val releases = listOf(
        Release("1.2.0", "2026-12-01", false, null, listOf(
            ChangeSection(ChangeKind.ADDED, listOf("A brand new thing", "Another new thing")),
            ChangeSection(ChangeKind.FIXED, listOf("Something that was broken")),
        )),
        Release("1.1.1", "2026-11-21", true, null, listOf(ChangeSection(ChangeKind.CHANGED, listOf("A change from 1.1.1")))),
        Release("1.1.0", "2026-10-31", false, null, listOf(ChangeSection(ChangeKind.ADDED, listOf("A feature of 1.1.0")))),
        Release("Calisthenics Memory 1.26.0", "2026-07-20", false, "Intro of the original.", listOf(ChangeSection(ChangeKind.FIXED, listOf("An original fix")))),
    )

    private fun show(scale: Float = 1f, rtl: Boolean = false, lastSeen: String? = "1.1.1", celebrate: Boolean = true) {
        RuntimeEnvironment.setFontScale(scale)
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                CalisthenicsMemoryTheme {
                    Box(Modifier.fillMaxSize()) { WhatsNewList(releases, "The overview of this version.", lastSeen, celebrate, visible = true, installed = "1.2.0") }
                }
            }
        }
        repeat(3) { compose.waitForIdle(); Thread.sleep(200) }
    }

    private fun scrollTo(text: String) = compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))

    @Test fun `the installed version opens the list with its overview and its changes in full`() {
        show()
        compose.onNodeWithText("What's new").assertExists()
        compose.onNodeWithText("The overview of this version.").assertExists()
        compose.onNodeWithText("A brand new thing").assertExists()
        compose.onNodeWithText("Something that was broken").assertExists()
    }

    @Test fun `earlier versions are collapsed rows that open in place, one at a time`() {
        show()
        scrollTo("1.1.1")
        compose.onNodeWithText("A change from 1.1.1").assertDoesNotExist()
        compose.onNodeWithText("1.1.1").performClick()
        compose.onNodeWithText("A change from 1.1.1").assertExists()
        scrollTo("1.1.0")
        compose.onNodeWithText("1.1.0").performClick()
        compose.onNodeWithText("A feature of 1.1.0").assertExists()
        compose.onNodeWithText("A change from 1.1.1").assertDoesNotExist()
    }

    @Test fun `a withdrawn version says so and the original app's history sits under its own heading`() {
        show()
        scrollTo("Withdrawn")
        compose.onNode(hasText("Withdrawn")).assertExists()
        scrollTo("Before Kalimory")
        compose.onNode(hasText("Before Kalimory")).assertExists()
        scrollTo("Calisthenics Memory 1.26.0")
        compose.onNodeWithText("Calisthenics Memory 1.26.0").performClick()
        compose.onNodeWithText("Intro of the original.").assertExists()
        compose.onNodeWithText("An original fix").assertExists()
    }

    @Test fun `opened again from Settings nothing is marked new`() {
        show(lastSeen = null, celebrate = false)
        compose.onAllNodesWithText("New").assertCountEquals(0)
    }

    @Test fun `no text is cut off at 200 percent`() {
        show(scale = 2f)
        val problems = textFitProblems(compose)
        assertEquals(problems.joinToString("\n"), emptyList<String>(), problems)
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test fun `no text is cut off in a right-to-left layout`() {
        show(rtl = true)
        val problems = textFitProblems(compose)
        assertEquals(problems.joinToString("\n"), emptyList<String>(), problems)
    }

    @Test fun `every row meets the touch rules`() {
        show()
        scrollTo("1.1.1")
        val problems = tapTargetProblems(compose, compose.density)
        assertEquals(problems.joinToString("\n"), emptyList<String>(), problems)
    }

    @Test fun `a long section shows its first entries and a button for all of them`() {
        val many = (1..12).map { "Entry number $it" }
        compose.setContent {
            CalisthenicsMemoryTheme {
                Box(Modifier.fillMaxSize()) {
                    WhatsNewList(listOf(Release("1.2.0", "2026-12-01", false, null, listOf(ChangeSection(ChangeKind.ADDED, many)))), null, "1.1.1", true, true, "1.2.0")
                }
            }
        }
        repeat(3) { compose.waitForIdle(); Thread.sleep(200) }
        compose.onNodeWithText("Entry number 1").assertExists()
        compose.onNodeWithText("Entry number 7").assertDoesNotExist()
        compose.onNodeWithText("Show all 12").performClick()
        compose.onNodeWithText("Entry number 12").assertExists()
        compose.onNodeWithText("Show all 12").assertDoesNotExist()
    }
}
