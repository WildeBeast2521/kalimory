package io.github.gonbei774.calisthenicsmemory.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutNotificationContent
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutNotificationLabels
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutPhase
import io.github.gonbei774.calisthenicsmemory.workout.WorkoutProgress

/** The notification's words, from the app's strings in the current language. */
class AndroidWorkoutNotificationLabels(private val context: Context) : WorkoutNotificationLabels {
    override fun phase(phase: WorkoutPhase): String = context.getString(
        when (phase) {
            WorkoutPhase.GET_READY -> R.string.interval_get_ready
            WorkoutPhase.REST -> R.string.interval_rest_label
            WorkoutPhase.WORK -> R.string.interval_work_label
            WorkoutPhase.HOLD -> R.string.notification_hold
            WorkoutPhase.REPS -> R.string.notification_workout_title
        }
    )

    override fun reps(done: Int, target: Int): String =
        context.resources.getQuantityString(R.plurals.notification_reps_progress, target, done, target)

    override val paused: String get() = context.getString(R.string.notification_paused)

    override fun pausedWithTimeLeft(time: String): String = context.getString(R.string.notification_paused_time_left, time)

    override fun time(millis: Long): String = DateUtils.formatElapsedTime((millis.coerceAtLeast(0) + 999) / 1_000)
}

/**
 * The timer service's one notification. It is silent and alerts only the first time, so updating it
 * as the workout moves on changes the notification in place instead of adding new ones.
 */
fun buildWorkoutNotification(
    context: Context,
    channelId: String,
    contentIntent: PendingIntent,
    progress: WorkoutProgress?,
): Notification {
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentIntent(contentIntent)
        .setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    if (progress == null) {
        return builder
            .setContentTitle(context.getString(R.string.notification_workout_title))
            .setContentText(context.getString(R.string.notification_workout_text))
            .build()
    }
    val content = WorkoutNotificationContent.of(progress, AndroidWorkoutNotificationLabels(context))
    builder
        .setContentTitle(content.title)
        .setContentText(content.text)
        .setSubText(content.subText)
    if (content.countdownEndsAtMillis != null) {
        // The system ticks the countdown itself, so the app does not post every second.
        builder.setWhen(content.countdownEndsAtMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
    } else {
        builder.setShowWhen(false).setUsesChronometer(false)
    }
    if (content.progressTotal > 0) {
        builder.setProgress(content.progressTotal, content.progressDone, false)
    }
    return builder.build()
}
