package net.tomascichero.birthdayremainder.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { System, Light, Dark }

data class LanguageOption(val code: String, val label: String)

object AppPreferences {
    private const val PREFS_NAME = "app_preferences"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_LANGUAGE = "language"

    val availableLanguages = listOf(
        LanguageOption("system", "System"),
        LanguageOption("en", "English"),
        LanguageOption("es", "Español"),
        LanguageOption("fr", "Français"),
        LanguageOption("pt", "Português"),
        LanguageOption("de", "Deutsch"),
        LanguageOption("it", "Italiano"),
    )

    private val _themeMode = MutableStateFlow(ThemeMode.System)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _language = MutableStateFlow("system")
    val language: StateFlow<String> = _language

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun init(context: Context) {
        val p = prefs(context)
        _themeMode.value = when (p.getString(KEY_THEME, "system")) {
            "light" -> ThemeMode.Light
            "dark" -> ThemeMode.Dark
            else -> ThemeMode.System
        }
        _language.value = p.getString(KEY_LANGUAGE, "system") ?: "system"
    }

    fun setTheme(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        prefs(context).edit().putString(KEY_THEME, when (mode) {
            ThemeMode.System -> "system"
            ThemeMode.Light -> "light"
            ThemeMode.Dark -> "dark"
        }).apply()
    }

    fun setLanguage(context: Context, code: String) {
        _language.value = code
        prefs(context).edit().putString(KEY_LANGUAGE, code).apply()

        val locales = if (code == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(code)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun getLanguageLabel(code: String): String =
        availableLanguages.find { it.code == code }?.label ?: "System"
}
