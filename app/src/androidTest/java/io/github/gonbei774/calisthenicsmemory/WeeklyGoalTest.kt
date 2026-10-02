package io.github.gonbei774.calisthenicsmemory

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.ProgressionPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The weekly goal is set in Settings and Today counts the week against it (ADR 0005, decision 6). */
@RunWith(AndroidJUnit4::class)
class WeeklyGoalTest {
    @get:Rule(order = 0)
    val welcomeSeen = WelcomeSeenRule()

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = ProgressionPreferences(context)
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    @Before
    fun clearGoal() = prefs.setWeeklyGoal(null)

    @After
    fun cleanUp() {
        if (::scenario.isInitialized) scenario.close()
        prefs.setWeeklyGoal(null)
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntil(TIMEOUT_MS) { rule.onAllNodesWithText(text(R.string.nav_train)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun onlyTwoToSixDaysAreKept() {
        prefs.setWeeklyGoal(7)
        assertNull(prefs.weeklyGoal())
        prefs.setWeeklyGoal(6)
        assertEquals(6, prefs.weeklyGoal())
    }

    @Test
    fun aGoalChosenInSettingsCountsOnToday() {
        launch()
        rule.onNode(hasContentDescription(text(R.string.settings))).performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(text(R.string.weekly_goal)))
        // The goal is a connected button group in place: "4" reads aloud as "4 of 7 days".
        rule.onNodeWithContentDescription(text(R.string.weekly_goal_days, 4)).performClick()
        assertEquals(4, prefs.weeklyGoal())

        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        // Days trained this week depend on other tests' data; the goal is what this test controls.
        rule.onNode(hasText(" 4", substring = true) and hasText(text(R.string.today_week_summary, 0, 4).substringBefore(" 0"), substring = true))
            .assertExists()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
