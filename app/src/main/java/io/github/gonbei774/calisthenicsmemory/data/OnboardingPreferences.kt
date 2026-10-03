package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import androidx.core.content.edit

/** One-time first-run moments: the welcome guide, and the notification permission request. */
class OnboardingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isWelcomeSeen(): Boolean = prefs.getBoolean(KEY_WELCOME_SEEN, false)

    fun markWelcomeSeen() = prefs.edit { putBoolean(KEY_WELCOME_SEEN, true) }

    /** The notification permission is asked for once, when the first workout opens. */
    fun isNotificationAsked(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_ASKED, false)

    fun markNotificationAsked() = prefs.edit { putBoolean(KEY_NOTIFICATION_ASKED, true) }

    private companion object {
        const val PREFS_NAME = "onboarding_preferences"
        const val KEY_WELCOME_SEEN = "welcome_seen"
        const val KEY_NOTIFICATION_ASKED = "notification_asked"
    }
}
