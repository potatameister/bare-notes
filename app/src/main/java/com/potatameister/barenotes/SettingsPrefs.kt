package com.potatameister.barenotes

import android.content.Context

object SettingsPrefs {

    private const val FILE = "settings"
    private const val KEY_TEXT_SIZE = "text_size"
    private const val KEY_SORT = "sort_order"

    const val SORT_MODIFIED = "modified"
    const val SORT_CREATED = "created"

    fun textSizeSp(context: Context): Int =
        prefs(context).getInt(KEY_TEXT_SIZE, 16).coerceIn(14, 20)

    fun setTextSizeSp(context: Context, sp: Int) {
        prefs(context).edit().putInt(KEY_TEXT_SIZE, sp.coerceIn(14, 20)).apply()
    }

    fun sortOrder(context: Context): String =
        prefs(context).getString(KEY_SORT, SORT_MODIFIED) ?: SORT_MODIFIED

    fun setSortOrder(context: Context, order: String) {
        prefs(context).edit().putString(KEY_SORT, order).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
