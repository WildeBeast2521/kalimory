package io.github.gonbei774.calisthenicsmemory.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings › Start countdown: turning it off means no countdown, in workouts, programs and time estimates. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StartCountdownSwitchTest {
    private val prefs = WorkoutPreferences(ApplicationProvider.getApplicationContext<Application>())

    @Test fun `on, the countdown is the chosen seconds`() {
        prefs.setStartCountdownEnabled(true)
        prefs.setStartCountdown(7)
        assertEquals(7, prefs.effectiveStartCountdown())
    }

    @Test fun `off, there is no countdown whatever seconds are stored`() {
        prefs.setStartCountdown(7)
        prefs.setStartCountdownEnabled(false)
        assertEquals(0, prefs.effectiveStartCountdown())
    }
}
