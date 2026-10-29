package com.creator.tiktokstudio.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(
    val code: String,
    val title: String,
    val flag: String,
    val isRtl: Boolean
) {
    ARABIC("ar", "العربية", "🇸🇦", true),
    ENGLISH("en", "English", "🇺🇸", false),
    FRENCH("fr", "Français", "🇫🇷", false);

    companion object {
        fun fromCode(code: String): AppLanguage =
            values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ARABIC
    }
}

object ThemeAndLocaleManager {
    private const val PREFS_NAME = "tiktok_studio_prefs"
    private const val KEY_LANGUAGE = "key_app_language"
    private const val KEY_DARK_MODE = "key_dark_mode"

    private val _currentLanguage = MutableStateFlow(AppLanguage.ARABIC)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val langCode = prefs.getString(KEY_LANGUAGE, AppLanguage.ARABIC.code) ?: AppLanguage.ARABIC.code
        _currentLanguage.value = AppLanguage.fromCode(langCode)
        _isDarkMode.value = prefs.getBoolean(KEY_DARK_MODE, true)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        _currentLanguage.value = language
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
    }

    fun toggleTheme(context: Context) {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, newMode)
            .apply()
    }

    fun setTheme(context: Context, isDark: Boolean) {
        _isDarkMode.value = isDark
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, isDark)
            .apply()
    }
}
