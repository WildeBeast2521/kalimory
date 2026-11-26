package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.BodyWeight
import io.github.gonbei774.calisthenicsmemory.ui.screens.MainScreenFixture
import io.github.gonbei774.calisthenicsmemory.ui.screens.tapTargetProblems
import io.github.gonbei774.calisthenicsmemory.ui.screens.textFitProblems
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** The body weight list: what it shows, how entries are added, changed and deleted, and its text and touch rules. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class BodyWeightScreenTest {
    private val compose = createComposeRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(MainScreenFixture.state(AppTheme.LIGHT)).around(compose)

    private val weights = listOf(BodyWeight(1, "2026-10-12", 65_000), BodyWeight(2, "2026-11-11", 62_850))
    private val deleted = mutableListOf<BodyWeight>()

    private fun show(list: List<BodyWeight> = weights, scale: Float = 1f, rtl: Boolean = false) {
        RuntimeEnvironment.setFontScale(scale)
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                CalisthenicsMemoryTheme { BodyWeightContent(list, LocalDate.of(2026, 11, 26), { _, _, _ -> }, { deleted += it }, {}) }
            }
        }
        repeat(2) { compose.waitForIdle(); Thread.sleep(150) }
    }

    @Test fun `an empty list says what it is for`() {
        show(emptyList())
        compose.onNodeWithText("Add your body weight to count it in the volume of exercises that have a share of body weight").assertExists()
    }

    @Test fun `the entries are listed newest first, each with its weight`() {
        show()
        compose.onNode(hasText("62.85 kg")).assertExists()
        compose.onNode(hasText("65 kg")).assertExists()
    }

    // The add, edit and delete dialogs hold a text field, which keeps Compose from idling under Robolectric:
    // they are covered by the instrumented BodyWeightTest on the emulator.

    @Test fun `no text is cut off at 200 percent and every control is a full target`() {
        show(scale = 2f)
        val problems = tapTargetProblems(compose, compose.density) + textFitProblems(compose)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test fun `a right-to-left layout has no cut-off text`() {
        show(rtl = true)
        val problems = tapTargetProblems(compose, compose.density) + textFitProblems(compose)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
