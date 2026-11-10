package com.potatameister.barenotes

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

object ThemePrefs {

    private const val FILE = "settings"
    private const val KEY = "theme_mode"

    fun mode(context: Context): ThemeMode =
        when (prefs(context).getString(KEY, null)) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }

    fun save(context: Context, mode: ThemeMode) {
        prefs(context).edit {
            putString(
                KEY,
                when (mode) {
                    ThemeMode.SYSTEM -> "system"
                    ThemeMode.LIGHT -> "light"
                    ThemeMode.DARK -> "dark"
                }
            )
        }
    }

    fun resolveDark(context: Context): Boolean = when (mode(context)) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM ->
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
