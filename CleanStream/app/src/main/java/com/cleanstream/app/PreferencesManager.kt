package com.cleanstream.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Centralized manager for all user-configurable feature toggles.
 * Uses SharedPreferences for lightweight, persistent key-value storage.
 */
object PreferencesManager {

    private const val PREFS_FILE_NAME = "cleanstream_prefs"
    private const val KEY_BLOCK_ADS = "pref_block_ads"
    private const val KEY_BLOCK_SHORTS = "pref_block_shorts"
    private const val KEY_BACKGROUND_PLAY = "pref_background_play"
    private const val KEY_BLOCK_SYSTEM_ADS = "pref_block_system_ads"

    private lateinit var sharedPreferences: SharedPreferences

    /** Must be called once from Application or MainActivity before using other methods. */
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    // --- Getters (all default to true for the best out-of-box experience) ---

    fun isAdBlockEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_BLOCK_ADS, true)

    fun isShortsBlockEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_BLOCK_SHORTS, true)

    fun isBackgroundPlayEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_BACKGROUND_PLAY, true)

    fun isSystemAdBlockEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_BLOCK_SYSTEM_ADS, false)

    // --- Setters ---

    fun setAdBlockEnabled(enabled: Boolean) =
        sharedPreferences.edit().putBoolean(KEY_BLOCK_ADS, enabled).apply()

    fun setShortsBlockEnabled(enabled: Boolean) =
        sharedPreferences.edit().putBoolean(KEY_BLOCK_SHORTS, enabled).apply()

    fun setBackgroundPlayEnabled(enabled: Boolean) =
        sharedPreferences.edit().putBoolean(KEY_BACKGROUND_PLAY, enabled).apply()

    fun setSystemAdBlockEnabled(enabled: Boolean) =
        sharedPreferences.edit().putBoolean(KEY_BLOCK_SYSTEM_ADS, enabled).apply()
}
