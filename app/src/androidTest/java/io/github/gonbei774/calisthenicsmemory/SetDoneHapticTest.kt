package io.github.gonbei774.calisthenicsmemory

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.SetDoneBadge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A finished set confirms with one haptic, unless the user turned it off in Settings. */
@RunWith(AndroidJUnit4::class)
class SetDoneHapticTest {
    @get:Rule
    val rule = createComposeRule()

    private val prefs = WorkoutPreferences(InstrumentationRegistry.getInstrumentation().targetContext)

    private class RecordingHaptics : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    private fun showBadge(): RecordingHaptics {
        val haptics = RecordingHaptics()
        rule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                SetDoneBadge(label = "Set 1 done")
            }
        }
        rule.waitForIdle()
        return haptics
    }

    @After
    fun restoreDefault() = prefs.setSetDoneVibrationEnabled(true)

    @Test
    fun vibratesOnceByDefault() {
        prefs.setSetDoneVibrationEnabled(true)
        assertEquals(listOf(HapticFeedbackType.Confirm), showBadge().performed)
    }

    @Test
    fun staysStillWhenTurnedOff() {
        prefs.setSetDoneVibrationEnabled(false)
        assertEquals(emptyList<HapticFeedbackType>(), showBadge().performed)
    }
}
