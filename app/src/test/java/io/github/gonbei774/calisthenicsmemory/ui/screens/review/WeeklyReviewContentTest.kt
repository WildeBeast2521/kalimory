package io.github.gonbei774.calisthenicsmemory.ui.screens.review

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import io.github.gonbei774.calisthenicsmemory.data.review.WeekReview
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

/** The weekly review's layout and what TalkBack reads, with a made-up week. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class WeeklyReviewContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        review: WeekReview = WeeklyReviewSamples.week,
        heading: String? = "This week",
        goal: Int? = 4,
        canGoBack: Boolean = true,
        canGoForward: Boolean = false,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) = compose.setContent {
        CalisthenicsMemoryTheme {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                WeeklyReviewContent(review, heading, WeeklyReviewSamples.RANGE, goal, canGoBack, canGoForward, onPrevious = {}, onNext = {})
            }
        }
    }

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test fun `at 200 percent font every control is a full target and no text is cut off`() {
        RuntimeEnvironment.setFontScale(2f)
        try {
            show(canGoForward = true)
            compose.waitForIdle()
            val problems = tapTargetProblems(compose, compose.density) + textFitProblems(compose)
            assertTrue(problems.joinToString("\n"), problems.isEmpty())
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }

    @Test fun `the week and the movement section are headings`() {
        show()
        compose.onNode(hasText("This week") and isHeading).assertExists()
        compose.onNode(hasText("Sets by movement") and isHeading).assertExists()
    }

    @Test fun `a past week without a name uses its range as the heading`() {
        show(heading = null)
        compose.onNode(hasText(WeeklyReviewSamples.RANGE) and isHeading).assertExists()
    }

    @Test fun `each row reads as one item`() {
        show()
        compose.onNode(hasText("Push") and hasText("12 sets")).assertExists()
        compose.onNode(hasText("Other exercises") and hasText("3 sets") and hasText("Not linked to the catalogue")).assertExists()
        compose.onNode(hasText("Intervals") and hasText("8 sets")).assertExists()
    }

    @Test fun `the totals count against the weekly goal`() {
        show()
        compose.onNodeWithContentDescription("44 sets").assertExists()
        compose.onNodeWithText("Trained on 3 of 4 days").assertExists()
    }

    @Test fun `without a goal the days stand alone`() {
        show(goal = null)
        compose.onNodeWithText("Trained on 3 days").assertExists()
    }

    @Test fun `next is off on the current week and previous with no earlier history`() {
        show(canGoBack = false, canGoForward = false)
        compose.onNodeWithContentDescription("Next week").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Previous week").assertIsNotEnabled()
    }

    @Test fun `both ways are open in the middle of the history`() {
        show(canGoBack = true, canGoForward = true)
        compose.onNodeWithContentDescription("Next week").assertIsEnabled()
        compose.onNodeWithContentDescription("Previous week").assertIsEnabled()
    }

    @Test fun `the reference line shows with a strength group`() {
        show()
        compose.onNodeWithText("The mark shows 10 sets a week", substring = true).assertExists()
    }

    @Test fun `with only other exercises there is no reference line`() {
        show(review = WeeklyReviewSamples.week.copy(families = emptyList()))
        compose.onNodeWithText("The mark shows 10 sets a week", substring = true).assertDoesNotExist()
    }

    @Test fun `an empty week shows only the header and one line`() {
        show(review = WeeklyReviewSamples.empty)
        compose.onNodeWithText("No sets logged in this week").assertIsDisplayed()
        compose.onNodeWithText("Sets by movement").assertDoesNotExist()
        compose.onNodeWithText("Trained on", substring = true).assertDoesNotExist()
    }

    @Test fun `in right-to-left layout previous sits on the right`() {
        show(direction = LayoutDirection.Rtl, canGoForward = true)
        val previous = compose.onNodeWithContentDescription("Previous week").getBoundsInRoot()
        val next = compose.onNodeWithContentDescription("Next week").getBoundsInRoot()
        assertTrue("previous ${previous.left} should be right of next ${next.left}", previous.left > next.left)
        assertEquals(previous.top, next.top)
    }
}
