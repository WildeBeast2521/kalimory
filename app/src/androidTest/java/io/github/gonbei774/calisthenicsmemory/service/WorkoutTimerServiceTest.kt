package io.github.gonbei774.calisthenicsmemory.service

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutPhase
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutProgress
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutProgressBus
import org.junit.Assert.assertEquals
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
        WorkoutProgressBus.clear()
        WorkoutTimerService.stopService(context)
    }

    private val notifications = context.getSystemService(NotificationManager::class.java)

    private fun shownText(): String? = notifications.activeNotifications.singleOrNull()
        ?.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    /** A context in the background without a foreground service: Android refuses start commands. */
    private inner class RefusingContext : ContextWrapper(context) {
        override fun startService(service: Intent?): ComponentName? =
            throw IllegalStateException("Not allowed to start service: app is in background")

        override fun startForegroundService(service: Intent?): ComponentName? =
            throw IllegalStateException("startForegroundService() not allowed")
    }

    @Test
    fun aRefusedStopStillStopsTheService() {
        WorkoutTimerService.startService(context)
        assertTrue(waitUntil { wakeLockHeld() })

        WorkoutTimerService.stopService(RefusingContext())

        assertTrue(waitUntil { !wakeLockHeld() })
    }

    @Test
    fun aRefusedStartIsNotACrash() {
        WorkoutTimerService.startService(RefusingContext())
        assertFalse(wakeLockHeld())
    }

    @Test
    fun aStartRightAfterAStopKeepsTheServiceInTheForeground() {
        // Screens stop and start the service as a workout moves between steps. A stop must not
        // destroy the service while a newer start is waiting, or Android ends the app because
        // that start never reached startForeground().
        repeat(20) {
            WorkoutTimerService.stopService(context)
            WorkoutTimerService.startService(context)
        }
        assertTrue("still in the foreground", waitUntil { notifications.activeNotifications.isNotEmpty() })
        Thread.sleep(6_000) // past the time Android allows a start to reach startForeground()
        assertTrue("still in the foreground", notifications.activeNotifications.isNotEmpty())
    }

    @Test
    fun progressUpdatesTheOneNotificationInPlace() {
        WorkoutTimerService.startService(context)
        assertTrue("notification shown", waitUntil { notifications.activeNotifications.isNotEmpty() })

        fun reps(done: Int) = WorkoutProgress("Push-up", "Set 1 of 3", WorkoutPhase.REPS, repsDone = done, repsTarget = 10, stepsTotal = 3)
        WorkoutProgressBus.publish(reps(1))
        assertTrue("first count shown", waitUntil { shownText()?.startsWith("1 / 10") == true })
        WorkoutProgressBus.publish(reps(2))
        assertTrue("count updated", waitUntil { shownText()?.startsWith("2 / 10") == true })

        // Still one notification, updated in place, and it stays silent.
        val shown = notifications.activeNotifications
        assertEquals(1, shown.size)
        assertEquals("Push-up", shown.single().notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
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
