package io.github.gonbei774.calisthenicsmemory.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import io.github.gonbei774.calisthenicsmemory.data.AppLanguage
import io.github.gonbei774.calisthenicsmemory.data.LanguagePreferences
import java.util.Locale

/**
 * [context] in the language chosen in Settings, or unchanged when the app follows the system.
 * Every component with its own base context applies it in attachBaseContext: the application, the
 * activity, and the workout service, whose notification otherwise spoke the phone's language.
 */
// The app ships as one APK with every language inside; there are no language splits to download.
@SuppressLint("AppBundleLocaleChanges")
fun withAppLanguage(context: Context): Context {
    val languagePrefs = LanguagePreferences(context)
    val selectedLanguage = languagePrefs.getLanguage()

    android.util.Log.d("AppLocale", "Selected language: ${selectedLanguage.code}")

    // システム設定に従う場合は何もしない
    if (selectedLanguage == AppLanguage.SYSTEM) {
        android.util.Log.d("AppLocale", "Using system language")
        return context
    }

    val locale = when (selectedLanguage) {
        AppLanguage.JAPANESE -> Locale("ja")
        AppLanguage.ENGLISH -> Locale("en")
        AppLanguage.SPANISH -> Locale("es")
        AppLanguage.GERMAN -> Locale("de")
        AppLanguage.CHINESE -> Locale("zh", "CN")
        AppLanguage.FRENCH -> Locale("fr")
        AppLanguage.ITALIAN -> Locale("it")
        AppLanguage.UKRAINIAN -> Locale("uk")
        AppLanguage.RUSSIAN -> Locale("ru")
        AppLanguage.ARABIC -> Locale("ar")
        AppLanguage.SYSTEM -> return context
    }

    android.util.Log.d("AppLocale", "Setting locale to: ${locale.language}")
    Locale.setDefault(locale)

    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)

    return context.createConfigurationContext(config)
}
