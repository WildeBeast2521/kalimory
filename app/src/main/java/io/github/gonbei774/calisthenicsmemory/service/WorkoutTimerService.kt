package io.github.gonbei774.calisthenicsmemory.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import io.github.gonbei774.calisthenicsmemory.MainActivity
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutProgressBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import io.github.gonbei774.calisthenicsmemory.util.withAppLanguage

/**
 * Foreground Service for workout timer.
 * Keeps the timer running accurately even when the screen is off
 * by maintaining a WakeLock and showing a foreground notification.
 *
 * The actual timer logic remains in the workout screens - this service
 * only provides the infrastructure (WakeLock + Foreground notification)
 * to keep the app alive during workouts. The notification shows the progress
 * the screens publish to [WorkoutProgressBus], updated in place.
 */
class WorkoutTimerService : Service() {

    companion object {
        private const val TAG = "WorkoutTimerService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "workout_timer_channel"

        // Actions
        const val ACTION_START = "io.github.gonbei774.calisthenicsmemory.action.START"
        const val ACTION_STOP = "io.github.gonbei774.calisthenicsmemory.action.STOP"

        /**
         * Starts or renews the foreground service. Android refuses a start while the app is in the
         * background and its service is not already in the foreground; the workout's timers do not
         * depend on the service (they read the monotonic clock), so a refusal is logged, never a crash.
         */
        fun startService(context: Context) {
            val intent = Intent(context, WorkoutTimerService::class.java).apply {
                action = ACTION_START
            }
            try {
                context.startForegroundService(intent)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "The workout service could not start", e)
            }
        }

        /**
         * Stops the service unless a newer start is waiting (see [onStartCommand]). The stop travels
         * as a start command, which Android refuses while the app is in the background without a
         * foreground service, for example when a screen closes long after its workout ended. Then
         * no start can be waiting either, so the service is stopped directly.
         */
        fun stopService(context: Context) {
            val intent = Intent(context, WorkoutTimerService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: IllegalStateException) {
                context.stopService(Intent(context, WorkoutTimerService::class.java))
            }
        }
    }

    // The notification is built with this service's context: it needs the app's language too.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(withAppLanguage(newBase))
    }

    // Binder for local binding
    inner class LocalBinder : Binder() {
        fun getService(): WorkoutTimerService = this@WorkoutTimerService
    }

    private val binder = LocalBinder()

    // WakeLock
    private var wakeLock: PowerManager.WakeLock? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foreground = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Each change of progress replaces the same notification; it never adds another one.
        scope.launch {
            WorkoutProgressBus.progress.drop(1).collect {
                if (foreground) {
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, createNotification())
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                foreground = true
                acquireWakeLock()
            }
            ACTION_STOP -> {
                // Stop only if no start came after this stop. A plain stopSelf() would destroy the
                // service while a newer start waits, and Android ends the app when that start never
                // reaches startForeground().
                if (stopSelfResult(startId)) {
                    foreground = false
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        foreground = false
        scope.cancel()
        super.onDestroy()
        releaseWakeLock()
    }

    // Swiping the app away ends the workout screen; the checkpoint lets it resume later.
    override fun onTaskRemoved(rootIntent: Intent?) {
        foreground = false
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_workout),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_workout_description)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return buildWorkoutNotification(this, CHANNEL_ID, pendingIntent, WorkoutProgressBus.progress.value)
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "CalisthenicsMemory::WorkoutTimerService"
            ).apply {
                // Screens send ACTION_START on every step change. A reference-counted lock
                // would need one release per start and outlive the workout; this one is
                // held once and released by a single release().
                setReferenceCounted(false)
            }
        }
        wakeLock?.acquire(60 * 60 * 1000L) // Max 1 hour; renewed by each start
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}