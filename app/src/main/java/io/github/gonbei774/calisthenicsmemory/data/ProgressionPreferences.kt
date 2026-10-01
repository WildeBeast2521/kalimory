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

    companion object {
        val WEEKLY_GOAL_RANGE = 2..6
        private const val PREFS_NAME = "progression_preferences"
        private const val KEY_WEEKLY_GOAL = "weekly_goal"
    }
}
