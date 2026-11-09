package com.minetallest.minefiy.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple SharedPreferences wrapper for user-level preferences.
 *
 * Stored per-device (survives logout). Source preference is not
 * cleared on logout — a user who logged out and back in on the same
 * phone keeps their choice.
 */
class UserPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getDataSource(): DataSource? {
        val stored = prefs.getString(KEY_DATA_SOURCE, null) ?: return null
        return try {
            DataSource.valueOf(stored)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun setDataSource(source: DataSource) {
        prefs.edit().putString(KEY_DATA_SOURCE, source.name).apply()
    }

    fun hasDataSource(): Boolean = getDataSource() != null

    companion object {
        private const val PREFS_NAME = "minefiy_prefs"
        private const val KEY_DATA_SOURCE = "data_source"
    }
}
