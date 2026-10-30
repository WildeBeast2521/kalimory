package io.github.gonbei774.calisthenicsmemory.service

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.AppLanguage
import io.github.gonbei774.calisthenicsmemory.data.LanguagePreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The workout notification speaks the language chosen in Settings, not the phone's. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class WorkoutServiceLanguageTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @After
    fun reset() = LanguagePreferences(application).setLanguage(AppLanguage.SYSTEM)

    @Test
    fun theServiceUsesTheAppLanguage() {
        LanguagePreferences(application).setLanguage(AppLanguage.GERMAN)
        val german = application.createConfigurationContext(
            android.content.res.Configuration(application.resources.configuration).apply { setLocale(java.util.Locale.GERMAN) }
        ).getString(R.string.notification_workout_title)

        val service = Robolectric.buildService(WorkoutTimerService::class.java).get()

        assertEquals(german, service.getString(R.string.notification_workout_title))
    }
}
