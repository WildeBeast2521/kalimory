package io.github.gonbei774.calisthenicsmemory

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.ui.components.workout.rememberSetDoneHaptic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A finished set confirms with one haptic, unless the user turned it off in Settings; the last set too. */
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

    // Finishing a set calls this where the set is recorded; no rest screen has to follow.
    private fun finishSet(): RecordingHaptics {
        val haptics = RecordingHaptics()
        lateinit var onSetDone: () -> Unit
        rule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                onSetDone = rememberSetDoneHaptic()
            }
        }
        rule.runOnIdle { onSetDone() }
        return haptics
    }

    @After
    fun restoreDefault() = prefs.setSetDoneVibrationEnabled(true)

    @Test
    fun vibratesOnceByDefault() {
        prefs.setSetDoneVibrationEnabled(true)
        assertEquals(listOf(HapticFeedbackType.Confirm), finishSet().performed)
    }

    @Test
    fun staysStillWhenTurnedOff() {
        prefs.setSetDoneVibrationEnabled(false)
        assertEquals(emptyList<HapticFeedbackType>(), finishSet().performed)
    }
}
