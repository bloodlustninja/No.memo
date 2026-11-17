package com.example.nomemo.data.pref

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nomemo_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode" // "PITCH_BLACK", "LIGHT"
        private const val KEY_GLASSMORPHISM = "key_glassmorphism"
        private const val KEY_HAPTICS = "key_haptics"
        private const val KEY_ACCENT_HEX = "key_accent_hex"
        private const val KEY_DOT_SPACING = "key_dot_spacing"
        private const val KEY_WAVE_SPEED_MS = "key_wave_speed_ms"
    }

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "PITCH_BLACK") ?: "PITCH_BLACK"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var isPitchBlackEnabled: Boolean
        get() = themeMode == "PITCH_BLACK"
        set(value) {
            themeMode = if (value) "PITCH_BLACK" else "LIGHT"
        }

    var isGlassmorphismEnabled: Boolean
        get() = prefs.getBoolean(KEY_GLASSMORPHISM, true)
        set(value) = prefs.edit().putBoolean(KEY_GLASSMORPHISM, value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    var accentHex: String
        get() = prefs.getString(KEY_ACCENT_HEX, "#D71921") ?: "#D71921"
        set(value) = prefs.edit().putString(KEY_ACCENT_HEX, value).apply()

    var dotSpacing: Float
        get() = prefs.getFloat(KEY_DOT_SPACING, 28f)
        set(value) = prefs.edit().putFloat(KEY_DOT_SPACING, value).apply()

    var waveSpeedMs: Int
        get() = prefs.getInt(KEY_WAVE_SPEED_MS, 3600)
        set(value) = prefs.edit().putInt(KEY_WAVE_SPEED_MS, value).apply()
}
