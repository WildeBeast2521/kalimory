package io.github.gonbei774.calisthenicsmemory.service

import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The service holds its wake lock only while a workout runs, however often screens start it. */
@RunWith(AndroidJUnit4::class)
class WorkoutTimerServiceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun wakeLockHeld(): Boolean {
        val output = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("dumpsys power")
        ).bufferedReader().use { it.readText() }
        // Only the list of held wake locks counts. Newer Android versions also print a log of
        // past acquire and release events, where the tag stays after it is released.
        val held = output.lineSequence()
            .dropWhile { !it.trimStart().startsWith("Wake Locks: size=") }
            .drop(1)
            .takeWhile { it.isNotBlank() }
        return held.any { it.contains("CalisthenicsMemory::WorkoutTimerService") }
    }

    private fun waitUntil(condition: () -> Boolean): Boolean {
        repeat(50) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }

    @After
    fun stop() {
        WorkoutTimerService.stopService(context)
    }

    @Test
    fun oneStopReleasesTheWakeLockAfterRepeatedStarts() {
        // Execution screens start the service again on every step change.
        repeat(5) {
            WorkoutTimerService.startService(context)
            Thread.sleep(200)
        }
        assertTrue("wake lock held while running", waitUntil { wakeLockHeld() })

        WorkoutTimerService.stopService(context)

        assertTrue("wake lock released after one stop", waitUntil { !wakeLockHeld() })
        Thread.sleep(1_000)
        assertFalse("wake lock stays released", wakeLockHeld())
    }
}
