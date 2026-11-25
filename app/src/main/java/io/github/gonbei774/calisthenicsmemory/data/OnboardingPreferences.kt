package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import androidx.core.content.edit
import io.github.gonbei774.calisthenicsmemory.BuildConfig

/** One-time first-run moments: the welcome guide, the notification permission request, and the "What's new" sheet after an update. */
class OnboardingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isWelcomeSeen(): Boolean = prefs.getBoolean(KEY_WELCOME_SEEN, false)

    /** The welcome guide covers a fresh install, so it also counts as having seen this version's changes. */
    fun markWelcomeSeen() = prefs.edit {
        putBoolean(KEY_WELCOME_SEEN, true)
        putInt(KEY_LAST_SEEN_CODE, BuildConfig.VERSION_CODE)
        putString(KEY_LAST_SEEN_NAME, BuildConfig.VERSION_NAME)
    }

    /** The version whose changes were last shown; null for an install that predates the sheet. */
    fun lastSeenVersionCode(): Int? = if (prefs.contains(KEY_LAST_SEEN_CODE)) prefs.getInt(KEY_LAST_SEEN_CODE, 0) else null

    fun lastSeenVersionName(): String? = prefs.getString(KEY_LAST_SEEN_NAME, null)

    /** True on the first launch after an update. A downgrade shows nothing and forgets the newer version. */
    fun isWhatsNewDue(currentCode: Int = BuildConfig.VERSION_CODE): Boolean {
        val seen = lastSeenVersionCode()
        if (seen != null && seen > currentCode) { markWhatsNewSeen(); return false }
        return whatsNewDue(isWelcomeSeen(), seen, currentCode)
    }

    fun markWhatsNewSeen() = prefs.edit {
        putInt(KEY_LAST_SEEN_CODE, BuildConfig.VERSION_CODE)
        putString(KEY_LAST_SEEN_NAME, BuildConfig.VERSION_NAME)
    }

    /** The notification permission is asked for once, when the first workout opens. */
    fun isNotificationAsked(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_ASKED, false)

    fun markNotificationAsked() = prefs.edit { putBoolean(KEY_NOTIFICATION_ASKED, true) }

    private companion object {
        const val PREFS_NAME = "onboarding_preferences"
        const val KEY_WELCOME_SEEN = "welcome_seen"
        const val KEY_NOTIFICATION_ASKED = "notification_asked"
        const val KEY_LAST_SEEN_CODE = "last_seen_version_code"
        const val KEY_LAST_SEEN_NAME = "last_seen_version_name"
    }
}
