package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import androidx.core.content.edit

/** Whether the welcome guide was seen; it shows once, on the first launch. */
class OnboardingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isWelcomeSeen(): Boolean = prefs.getBoolean(KEY_WELCOME_SEEN, false)

    fun markWelcomeSeen() = prefs.edit { putBoolean(KEY_WELCOME_SEEN, true) }

    private companion object {
        const val PREFS_NAME = "onboarding_preferences"
        const val KEY_WELCOME_SEEN = "welcome_seen"
    }
}
