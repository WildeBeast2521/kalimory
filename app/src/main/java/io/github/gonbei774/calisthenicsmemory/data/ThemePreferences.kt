package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * テーマ設定の保存・読み込みを管理するクラス
 */
class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * テーマ設定を取得
     * @return AppTheme (SYSTEM, LIGHT, DARK)
     */
    fun getTheme(): AppTheme {
        val themeCode = prefs.getString(KEY_THEME, AppTheme.SYSTEM.code)
        return AppTheme.fromCode(themeCode ?: AppTheme.SYSTEM.code)
    }

    /**
     * テーマ設定を保存
     * @param theme 保存するテーマ
     */
    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.code).apply()
    }

    /** Whether colours follow the wallpaper (Android 12+); off by default so the app keeps its own palette. */
    fun isDynamicColor(): Boolean = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, enabled) }
    }

    /** The chosen first day of the week, or null to follow the language and region. */
    fun getFirstDayOfWeek(): java.time.DayOfWeek? =
        prefs.getInt(KEY_FIRST_DAY_OF_WEEK, 0).takeIf { it in 1..7 }?.let { java.time.DayOfWeek.of(it) }

    fun setFirstDayOfWeek(day: java.time.DayOfWeek?) {
        prefs.edit { if (day == null) remove(KEY_FIRST_DAY_OF_WEEK) else putInt(KEY_FIRST_DAY_OF_WEEK, day.value) }
    }

    companion object {
        private const val KEY_FIRST_DAY_OF_WEEK = "first_day_of_week"
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_THEME = "app_theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}

/**
 * アプリで選択可能なテーマ
 */
enum class AppTheme(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromCode(code: String): AppTheme {
            return entries.find { it.code == code } ?: SYSTEM
        }
    }
}