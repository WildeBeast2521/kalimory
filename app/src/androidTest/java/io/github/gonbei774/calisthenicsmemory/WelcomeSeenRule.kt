package io.github.gonbei774.calisthenicsmemory

import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.CrashReports
import io.github.gonbei774.calisthenicsmemory.data.OnboardingPreferences
import org.junit.rules.ExternalResource

/**
 * Marks the welcome guide as seen, so tests that launch the app start on Today. Clears crash
 * reports too: one left by an earlier crash would put its offer over the screen under test.
 */
class WelcomeSeenRule : ExternalResource() {
    override fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        OnboardingPreferences(context).markWelcomeSeen()
        CrashReports.forApp(context).clear()
    }
}
