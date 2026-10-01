package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import androidx.core.content.edit

/** Progression settings (ADR 0007, decision 4): small choices, kept like the other settings. */
class ProgressionPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Training days a week the user aims for (ADR 0005, decision 6), or null for none. */
    fun weeklyGoal(): Int? = prefs.getInt(KEY_WEEKLY_GOAL, 0).takeIf { it in WEEKLY_GOAL_RANGE }

    fun setWeeklyGoal(days: Int?) = prefs.edit {
        if (days != null && days in WEEKLY_GOAL_RANGE) putInt(KEY_WEEKLY_GOAL, days) else remove(KEY_WEEKLY_GOAL)
    }

    /** Whether Today suggests what to train (ADR 0005, decision 1: a setting hides them entirely). */
    fun showSuggestions(): Boolean = prefs.getBoolean(KEY_SHOW_SUGGESTIONS, true)

    fun setShowSuggestions(show: Boolean) = prefs.edit { putBoolean(KEY_SHOW_SUGGESTIONS, show) }

    /** Chains whose suggestion was dismissed on [date]; a new day starts with none. */
    fun dismissedChains(date: String): Set<String> =
        if (prefs.getString(KEY_DISMISSED_DATE, null) == date) prefs.getStringSet(KEY_DISMISSED_CHAINS, null).orEmpty() else emptySet()

    fun dismissChain(date: String, chainId: String) {
        val chains = dismissedChains(date) + chainId
        prefs.edit {
            putString(KEY_DISMISSED_DATE, date)
            putStringSet(KEY_DISMISSED_CHAINS, chains)
        }
    }

    companion object {
        val WEEKLY_GOAL_RANGE = 2..6
        private const val PREFS_NAME = "progression_preferences"
        private const val KEY_WEEKLY_GOAL = "weekly_goal"
        private const val KEY_SHOW_SUGGESTIONS = "show_suggestions"
        private const val KEY_DISMISSED_DATE = "dismissed_date"
        private const val KEY_DISMISSED_CHAINS = "dismissed_chains"
    }
}
