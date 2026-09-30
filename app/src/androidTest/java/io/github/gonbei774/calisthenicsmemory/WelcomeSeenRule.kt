package io.github.gonbei774.calisthenicsmemory

import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.OnboardingPreferences
import org.junit.rules.ExternalResource

/** Marks the welcome guide as seen, so tests that launch the app start on Today. */
class WelcomeSeenRule : ExternalResource() {
    override fun before() {
        OnboardingPreferences(InstrumentationRegistry.getInstrumentation().targetContext).markWelcomeSeen()
    }
}
