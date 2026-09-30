package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.OnboardingPreferences
import io.github.gonbei774.calisthenicsmemory.ui.navigation.PRIMARY_NAVIGATION_BAR_TAG
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The welcome guide opens on first launch, can be skipped, stays away after, and reopens from Settings. */
@RunWith(AndroidJUnit4::class)
class WelcomeGuideTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun text(id: Int) = context.getString(id)

    private fun forgetWelcome() =
        context.getSharedPreferences("onboarding_preferences", Context.MODE_PRIVATE).edit().clear().commit()

    private fun waitForText(value: String) = rule.waitUntil(TIMEOUT_MS) {
        rule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
    }

    @Before
    fun firstLaunch() {
        forgetWelcome()
    }

    @After
    fun cleanUp() {
        if (::scenario.isInitialized) scenario.close()
        OnboardingPreferences(context).markWelcomeSeen()
    }

    @Test
    fun showsOnceAndCanBeSkipped() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText(text(R.string.guide_welcome_title))
        rule.onNode(hasText(text(R.string.guide_welcome_title)) and isHeading()).assertExists()

        rule.onNodeWithText(text(R.string.guide_skip)).performClick()
        rule.onAllNodesWithTag(PRIMARY_NAVIGATION_BAR_TAG).fetchSemanticsNodes().isNotEmpty().let(::assertTrue)
        assertTrue(OnboardingPreferences(context).isWelcomeSeen())

        // The next launch goes straight to Today.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText(text(R.string.nav_train))
        rule.onNodeWithText(text(R.string.guide_welcome_title)).assertDoesNotExist()
    }

    @Test
    fun walksToTheEndAndReopensFromSettings() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText(text(R.string.guide_welcome_title))
        repeat(3) { rule.onNodeWithText(text(R.string.guide_next)).performClick() }
        rule.onNodeWithText(text(R.string.guide_go_to_today)).performClick()
        waitForText(text(R.string.nav_train))

        rule.onNode(hasContentDescription(text(R.string.settings))).performClick()
        // Settings is a lazy list; the row exists once scrolled to.
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(text(R.string.settings_welcome_guide)))
        rule.onNodeWithText(text(R.string.settings_welcome_guide)).performClick()
        rule.onNode(hasText(text(R.string.guide_welcome_title)) and isHeading()).assertExists()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
