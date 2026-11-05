package io.github.gonbei774.calisthenicsmemory.ui.screens.review

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import io.github.gonbei774.calisthenicsmemory.data.review.MuscleVolumeSummary
import io.github.gonbei774.calisthenicsmemory.ui.screens.tapTargetProblems
import io.github.gonbei774.calisthenicsmemory.ui.screens.textFitProblems
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.Period
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

/** The body map's layout and what TalkBack reads, with a made-up week. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h740dp-xhdpi")
class BodyMapContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        summary: MuscleVolumeSummary = BodyMapSamples.week,
        period: Period = Period.OneWeek,
        onPeriod: (Period) -> Unit = {},
        direction: LayoutDirection = LayoutDirection.Ltr,
        view: BodyMapView = BodyMapView.SETS,
        onView: (BodyMapView) -> Unit = {},
    ) = compose.setContent {
        CalisthenicsMemoryTheme {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                BodyMapContent(summary, weeks = period.days / 7.0, period = period, onPeriod = onPeriod, view = view, onView = onView)
            }
        }
    }

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test fun `at 200 percent font every control is a full target and no text is cut off`() {
        RuntimeEnvironment.setFontScale(2f)
        try {
            show()
            compose.waitForIdle()
            val problems = tapTargetProblems(compose, compose.density) + textFitProblems(compose)
            assertTrue(problems.joinToString("\n"), problems.isEmpty())
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }

    @Test fun `each muscle reads as one item with the sets it helped in`() {
        show()
        compose.onNode(hasText("Chest") and hasText("12 sets")).assertExists()
        compose.onNode(hasText("Triceps") and hasText("3 sets") and hasText("Also helped in 8 sets")).assertExists()
    }

    @Test fun `sections are headings`() {
        show()
        compose.onNode(hasText("Sets by muscle") and isHeading).assertExists()
        compose.onNode(hasText("Not on the map") and isHeading).assertExists()
    }

    @Test fun `the chosen period is selected and another can be chosen`() {
        val chosen = mutableListOf<Period>()
        show(onPeriod = { chosen += it })
        compose.onNodeWithText("1 week").assertIsSelected()
        compose.onNodeWithText("1 month").performClick()
        assertEquals(listOf(Period.OneMonth), chosen)
    }

    @Test fun `sets not on the figure are listed, never dropped`() {
        show()
        compose.onNode(hasText("Mobility") and hasText("4 sets")).assertExists()
        compose.onNode(hasText("Other exercises") and hasText("3 sets")).assertExists()
        compose.onNode(hasText("Intervals") and hasText("8 sets")).assertExists()
    }

    @Test fun `an empty period shows the periods and one line`() {
        show(summary = BodyMapSamples.empty)
        compose.onNodeWithText("No sets logged in this period").assertExists()
        compose.onNodeWithText("Sets by muscle").assertDoesNotExist()
    }

    @Test fun `in right-to-left layout a muscle's name still reads before its count`() {
        show(direction = LayoutDirection.Rtl)
        val name = compose.onNodeWithText("Chest", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val count = compose.onNodeWithText("12 sets", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("name ${name.left} should be right of the count ${count.left}", name.left > count.left)
    }

    @Test fun `the view can be switched, and Sets is chosen at first`() {
        val chosen = mutableListOf<BodyMapView>()
        show(onView = { chosen += it })
        compose.onNodeWithText("Sets").assertIsSelected()
        compose.onNodeWithText("Not worked").performClick()
        assertEquals(listOf(BodyMapView.NOT_WORKED), chosen)
    }

    @Test fun `not worked lists the muscles with no set and leaves the sets list out`() {
        show(view = BodyMapView.NOT_WORKED)
        compose.onNode(hasText("Not worked in this period") and isHeading).assertExists()
        // The sample worked the chest; the hamstrings got nothing.
        compose.onNodeWithText("Hamstrings").assertExists()
        compose.onNodeWithText("Chest").assertDoesNotExist()
        compose.onNodeWithText("Sets by muscle").assertDoesNotExist()
    }

    @Test fun `at 200 percent font the not worked view fits and every control is a full target`() {
        RuntimeEnvironment.setFontScale(2f)
        try {
            show(view = BodyMapView.NOT_WORKED)
            compose.waitForIdle()
            val problems = tapTargetProblems(compose, compose.density) + textFitProblems(compose)
            assertTrue(problems.joinToString("\n"), problems.isEmpty())
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }
}
