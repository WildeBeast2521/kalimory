package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 言語設定の保存・読み込みを管理するクラス
 */
class LanguagePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * 言語設定を取得
     * @return AppLanguage (SYSTEM, JAPANESE, ENGLISH)
     */
    fun getLanguage(): AppLanguage {
        val languageCode = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.code)
        return AppLanguage.fromCode(languageCode ?: AppLanguage.SYSTEM.code)
    }

    /**
     * 言語設定を保存
     * @param language 保存する言語
     */
    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    companion object {
        private const val PREFS_NAME = "language_preferences"
        private const val KEY_LANGUAGE = "app_language"
    }
}

/**
 * アプリで選択可能な言語
 */
enum class AppLanguage(val code: String, val nativeName: String) {
    // Each language is listed in its own name, so a reader finds theirs whatever is showing.
    SYSTEM("system", ""),
    JAPANESE("ja", "日本語"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    GERMAN("de", "Deutsch"),
    CHINESE("zh", "简体中文"),
    FRENCH("fr", "Français"),
    ITALIAN("it", "Italiano"),
    UKRAINIAN("uk", "Українська"),
    RUSSIAN("ru", "Русский"),
    ARABIC("ar", "العربية");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: SYSTEM
        }
    }
}
