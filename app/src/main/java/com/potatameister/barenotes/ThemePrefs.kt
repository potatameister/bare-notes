package com.potatameister.barenotes

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object ThemePrefs {

    private const val FILE = "settings"
    private const val KEY = "dark_mode"
    private const val LIGHT = "light"
    private const val DARK = "dark"

    fun isDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    fun applyStored(context: Context) {
        when (prefs(context).getString(KEY, null)) {
            DARK -> apply(AppCompatDelegate.MODE_NIGHT_YES)
            LIGHT -> apply(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun save(context: Context, dark: Boolean) {
        prefs(context).edit()
            .putString(KEY, if (dark) DARK else LIGHT)
            .apply()
    }

    private fun apply(mode: Int) {
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
