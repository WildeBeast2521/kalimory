package io.github.gonbei774.calisthenicsmemory

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The four primary destinations: switching, back behavior, secondary screens, and recreation. */
@RunWith(AndroidJUnit4::class)
class PrimaryNavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    /** The shell appears only after the start-up database check, which runs off the main thread. */
    @Before
    fun waitForShell() {
        rule.waitUntil(STARTUP_TIMEOUT_MS) {
            rule.onAllNodesWithTag(PRIMARY_NAVIGATION_BAR_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun text(@StringRes id: Int) = rule.activity.getString(id)

    private fun tab(destination: PrimaryDestination) = rule.onNode(
        hasText(text(destination.label)) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG)),
        useUnmergedTree = false,
    )

    private fun assertShowing(destination: PrimaryDestination) {
        rule.onNode(hasText(text(destination.label)) and isHeading()).assertExists()
        PrimaryDestination.entries.forEach {
            if (it == destination) tab(it).assertIsSelected() else tab(it).assertIsNotSelected()
        }
    }

    private fun pressBack() {
        // Let the click's recomposition register the destination's back handler first.
        rule.waitForIdle()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun startsOnTodayAndSwitchesBetweenAllDestinations() {
        assertShowing(PrimaryDestination.TODAY)
        PrimaryDestination.entries.forEach { destination ->
            tab(destination).performClick()
            assertShowing(destination)
        }
    }

    @Test
    fun backFromAnotherDestinationReturnsToToday() {
        tab(PrimaryDestination.PROGRESS).performClick()
        pressBack()
        assertShowing(PrimaryDestination.TODAY)
    }

    @Test
    fun secondaryScreenHidesTheBarAndBackReturnsToTheSameDestination() {
        tab(PrimaryDestination.LIBRARY).performClick()
        rule.onNode(hasText(text(R.string.program_list_title)) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG)).not())
            .performClick()
        rule.onNodeWithTag(PRIMARY_NAVIGATION_BAR_TAG).assertDoesNotExist()

        pressBack()
        assertShowing(PrimaryDestination.LIBRARY)
    }

    @Test
    fun selectedDestinationSurvivesActivityRecreation() {
        tab(PrimaryDestination.TRAIN).performClick()
        rule.activityRule.scenario.recreate()
        waitForShell()
        assertShowing(PrimaryDestination.TRAIN)
    }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 10_000L
    }
}
