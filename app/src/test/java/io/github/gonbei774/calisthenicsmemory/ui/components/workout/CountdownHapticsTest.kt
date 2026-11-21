package io.github.gonbei774.calisthenicsmemory.ui.components.workout

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

/** A vibration goes with each countdown beep and the go cue, only when the person turned it on. */
class CountdownHapticsTest {
    private class Recording : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    @Test fun `each beep ticks and the go cue confirms when on`() {
        val haptics = Recording()
        val cues = CountdownHaptics({ true }, haptics)
        repeat(3) { cues.tick() }
        cues.go()
        assertEquals(List(3) { HapticFeedbackType.ContextClick } + HapticFeedbackType.Confirm, haptics.performed)
    }

    @Test fun `nothing vibrates when it is off`() {
        val haptics = Recording()
        val cues = CountdownHaptics({ false }, haptics)
        cues.tick()
        cues.go()
        assertEquals(emptyList<HapticFeedbackType>(), haptics.performed)
    }

    @Test fun `the switch is read each time, so changing it in Settings takes effect at once`() {
        val haptics = Recording()
        var on = false
        val cues = CountdownHaptics({ on }, haptics)
        cues.tick()
        on = true
        cues.tick()
        assertEquals(1, haptics.performed.size)
    }
}
