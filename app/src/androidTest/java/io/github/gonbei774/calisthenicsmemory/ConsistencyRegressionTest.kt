package io.github.gonbei774.calisthenicsmemory

import androidx.annotation.StringRes
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PrimaryDestination
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression tests for the round 2 phone test findings that need the real app: insets, where
 * To Dos are reached, which filters Progressions shows, the theme and language choices, and the
 * interval editor's name.
 */
@RunWith(AndroidJUnit4::class)
class ConsistencyRegressionTest {
    @get:Rule(order = 0)
    val welcomeSeen = WelcomeSeenRule()

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun waitForShell() {
        rule.waitUntil(STARTUP_TIMEOUT_MS) {
            rule.onAllNodesWithTag(PRIMARY_NAVIGATION_BAR_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun text(@StringRes id: Int) = rule.activity.getString(id)

    private fun tab(destination: PrimaryDestination) = rule.onNode(
        hasText(text(destination.label)) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG)),
    )

    private fun libraryRow(@StringRes id: Int) =
        rule.onNode(hasText(text(id)) and hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG)).not())

    /** The navigation bar's labels sit above the system navigation bar, not under its handle. */
    @Test
    fun navigationBarLabelsClearTheSystemBar() {
        val insets = ViewCompat.getRootWindowInsets(rule.activity.window.decorView)!!
            .getInsets(WindowInsetsCompat.Type.navigationBars())
        val windowBottom = rule.activity.window.decorView.height
        val label = tab(PrimaryDestination.TODAY).fetchSemanticsNode().boundsInWindow
        assertTrue(
            "label bottom ${label.bottom} must clear the system bar starting at ${windowBottom - insets.bottom}",
            label.bottom <= windowBottom - insets.bottom,
        )
    }

    /** To Dos can be found and edited from the Library, not only from Today's suggestions. */
    @Test
    fun libraryOpensTheToDoList() {
        tab(PrimaryDestination.LIBRARY).performClick()
        libraryRow(R.string.todo_title).performClick()
        rule.onNode(hasText(text(R.string.todo_title)) and isHeading()).assertExists()
    }

    /** The exercise and period filters belong to the record tabs; Progressions does not show them. */
    @Test
    fun progressionsTabHidesTheRecordFilters() {
        tab(PrimaryDestination.PROGRESS).performClick()
        rule.onNodeWithText(text(R.string.select_exercise_filter)).assertExists()
        rule.onNodeWithText(text(R.string.tab_progressions)).performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText(text(R.string.select_exercise_filter)).fetchSemanticsNodes().isEmpty()
        }
    }

    /** Theme offers Follow system, Light, Dark and AMOLED; languages show their own names and Cancel. */
    @Test
    fun settingsOfferAmoledAndEveryLanguageByItsOwnName() {
        tab(PrimaryDestination.LIBRARY).performClick()
        libraryRow(R.string.settings).performClick()
        rule.onNodeWithText(text(R.string.theme_amoled)).performScrollTo().assertExists()

        rule.onNodeWithText(text(R.string.language_setting)).performScrollTo().performClick()
        listOf("Русский", "العربية", "日本語", "English").forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithText(text(R.string.cancel)).performClick()
    }

    /** The interval list's + and the editor it opens say "interval", not "program". */
    @Test
    fun newIntervalIsCalledAnInterval() {
        tab(PrimaryDestination.LIBRARY).performClick()
        libraryRow(R.string.interval_list_title).performClick()
        rule.onNode(hasContentDescription(text(R.string.new_interval_program))).performClick()
        rule.onNode(hasText(text(R.string.new_interval_program)) and isHeading()).assertExists()
    }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 10_000L
    }
}
