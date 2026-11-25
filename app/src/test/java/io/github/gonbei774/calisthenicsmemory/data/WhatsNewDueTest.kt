package io.github.gonbei774.calisthenicsmemory.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** When the "What's new" sheet is due, with the real preferences file. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WhatsNewDueTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val prefs get() = context.getSharedPreferences("onboarding_preferences", 0)

    @Test fun `a fresh install is not shown the sheet, even after its welcome guide`() {
        val onboarding = OnboardingPreferences(context)
        assertFalse(onboarding.isWhatsNewDue())
        onboarding.markWelcomeSeen()
        assertFalse(onboarding.isWhatsNewDue())
        assertEquals(BuildConfig.VERSION_CODE, onboarding.lastSeenVersionCode())
        assertEquals(BuildConfig.VERSION_NAME, onboarding.lastSeenVersionName())
    }

    @Test fun `a person who saw the guide before the sheet existed gets it once after updating`() {
        prefs.edit().putBoolean("welcome_seen", true).commit()
        val onboarding = OnboardingPreferences(context)
        assertNull(onboarding.lastSeenVersionCode())
        assertTrue(onboarding.isWhatsNewDue())
        onboarding.markWhatsNewSeen()
        assertFalse(onboarding.isWhatsNewDue())
    }

    @Test fun `an older stored version is due until the sheet is dismissed`() {
        prefs.edit().putBoolean("welcome_seen", true).putInt("last_seen_version_code", BuildConfig.VERSION_CODE - 1).commit()
        val onboarding = OnboardingPreferences(context)
        assertTrue(onboarding.isWhatsNewDue())
        assertTrue("still due until it is marked seen", onboarding.isWhatsNewDue())
        onboarding.markWhatsNewSeen()
        assertFalse(onboarding.isWhatsNewDue())
    }

    @Test fun `a downgrade shows nothing and forgets the newer version`() {
        prefs.edit().putBoolean("welcome_seen", true).putInt("last_seen_version_code", BuildConfig.VERSION_CODE + 3).commit()
        val onboarding = OnboardingPreferences(context)
        assertFalse(onboarding.isWhatsNewDue())
        assertEquals(BuildConfig.VERSION_CODE, onboarding.lastSeenVersionCode())
    }
}
