package io.github.gonbei774.calisthenicsmemory

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.CrashContext
import io.github.gonbei774.calisthenicsmemory.data.CrashReports
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A crash kept on the phone is offered once on the next launch; answering it deletes the report. */
@RunWith(AndroidJUnit4::class)
class CrashReportOfferTest {
    // Also clears reports left by earlier tests.
    @get:Rule(order = 0)
    val welcomeSeen = WelcomeSeenRule()

    @get:Rule(order = 1)
    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val reports = CrashReports.forApp(context)
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun text(id: Int) = context.getString(id)

    @After
    fun cleanUp() {
        if (::scenario.isInitialized) scenario.close()
        reports.clear()
    }

    private fun crashEarlier() {
        reports.record("main", IllegalStateException("synthetic crash for the test"), CrashContext.current(), System.currentTimeMillis())
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntil(STARTUP_TIMEOUT_MS) { rule.onAllNodesWithText(text(R.string.nav_today)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun offerShown() = rule.onAllNodesWithText(text(R.string.crash_report_title)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun aKeptCrashIsOfferedAndDeleteRemovesIt() {
        crashEarlier()
        launch()
        rule.waitUntil(STARTUP_TIMEOUT_MS) { offerShown() }
        rule.onNodeWithText(text(R.string.delete)).performClick()
        rule.waitForIdle()
        rule.waitUntil(STARTUP_TIMEOUT_MS) { reports.pending().isEmpty() }
        assertEquals(false, offerShown())
    }

    @Test
    fun closingTheOfferWithBackKeepsTheReportForNextTime() {
        crashEarlier()
        launch()
        rule.waitUntil(STARTUP_TIMEOUT_MS) { offerShown() }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        rule.waitUntil(STARTUP_TIMEOUT_MS) { !offerShown() }
        assertEquals(1, reports.pending().size)
    }

    @Test
    fun withoutACrashNothingIsOffered() {
        launch()
        rule.waitForIdle()
        assertEquals(false, offerShown())
    }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 10_000L
    }
}
