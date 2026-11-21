package io.github.gonbei774.calisthenicsmemory.ui.components.workout

import android.app.Application
import android.content.Context
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The buzzer asks the phone's vibrator for a pulse: a short one for a tick, a longer one to confirm. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VibratorBuzzerTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val shadow get() = shadowOf(context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)

    @Test fun `a tick and a confirm both reach the vibrator, the confirm longer`() {
        val buzzer = VibratorBuzzer(context)
        buzzer.tick()
        val tick = shadow.milliseconds
        assertTrue("the tick should vibrate", tick > 0)
        buzzer.confirm()
        assertEquals(true, shadow.milliseconds > tick)
    }
}
