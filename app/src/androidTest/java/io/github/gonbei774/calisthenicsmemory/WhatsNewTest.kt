package io.github.gonbei774.calisthenicsmemory

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The "What's new" sheet appears once after an update, and not again once it has been closed. */
@RunWith(AndroidJUnit4::class)
class WhatsNewTest {
    @get:Rule(order = 0) val welcomeSeen = WelcomeSeenRule()
    @get:Rule(order = 1) val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<MainActivity>? = null

    @After fun close() { scenario?.close() }

    private fun launch() { scenario = ActivityScenario.launch(MainActivity::class.java) }

    private fun waitForHome() = rule.waitUntil(10_000) {
        rule.onAllNodes(hasAnyAncestor(hasTestTag(PRIMARY_NAVIGATION_BAR_TAG))).fetchSemanticsNodes().isNotEmpty()
    }

    private fun sheetShown() = rule.onAllNodes(hasText(context.getString(R.string.whats_new_close))).fetchSemanticsNodes().isNotEmpty()

    @Test fun theSheetShowsAfterAnUpdateAndStaysClosedOnceDismissed() {
        // An install from before this version: the guide is behind it and the stored version is older.
        context.getSharedPreferences("onboarding_preferences", 0).edit().putInt("last_seen_version_code", BuildConfig.VERSION_CODE - 1).commit()
        launch()
        rule.waitUntil(10_000) { sheetShown() }
        rule.onNodeWithText(context.getString(R.string.whats_new_title)).assertExists()

        rule.onNodeWithText(context.getString(R.string.whats_new_close)).performClick()
        rule.waitUntil(10_000) { !sheetShown() }

        scenario?.close()
        launch()
        waitForHome()
        rule.waitForIdle()
        check(!sheetShown()) { "the sheet came back after it was closed" }
        check(rule.onAllNodesWithText(context.getString(R.string.whats_new_title)).fetchSemanticsNodes().isEmpty())
    }

    @Test fun noSheetOnTheSameVersion() {
        launch()
        waitForHome()
        rule.waitForIdle()
        check(!sheetShown()) { "the sheet showed without an update" }
    }
}
