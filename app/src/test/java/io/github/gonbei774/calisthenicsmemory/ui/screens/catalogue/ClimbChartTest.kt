package io.github.gonbei774.calisthenicsmemory.ui.screens.catalogue

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.github.gonbei774.calisthenicsmemory.data.progression.ChainClimb
import io.github.gonbei774.calisthenicsmemory.data.progression.ClimbMark
import io.github.gonbei774.calisthenicsmemory.ui.screens.textFitProblems
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** What TalkBack reads of the climb chart, and its fit at large text. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class ClimbChartTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun `TalkBack reads the climb as one sentence under a heading`() {
        compose.setContent { CalisthenicsMemoryTheme { ClimbChart(ClimbChartSamples.climb) } }
        compose.onNodeWithContentDescription("From step 1 on Jan 5, 2026 to step 4 on Jun 1, 2026").assertExists()
        compose.onNode(hasText("Your climb") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
    }

    @Test fun `one step only reads as being on it since the first day`() {
        val day = LocalDate.of(2026, 3, 1)
        val one = ChainClimb(day, LocalDate.of(2026, 10, 8), listOf(day to 2), listOf(ClimbMark(day, 2, false)))
        compose.setContent { CalisthenicsMemoryTheme { ClimbChart(one) } }
        compose.onNodeWithContentDescription("On step 2 since Mar 1, 2026").assertExists()
    }

    @Test fun `at 200 percent font no text is cut off`() {
        RuntimeEnvironment.setFontScale(2f)
        try {
            compose.setContent { CalisthenicsMemoryTheme { ClimbChart(ClimbChartSamples.climb) } }
            compose.waitForIdle()
            val problems = textFitProblems(compose)
            assertTrue(problems.joinToString("\n"), problems.isEmpty())
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }
}
