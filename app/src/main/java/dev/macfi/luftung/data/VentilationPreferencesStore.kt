package dev.macfi.luftung.data

import android.content.Context
import dev.macfi.luftung.domain.ComfortProfile
import dev.macfi.luftung.domain.ComfortProfilePreset
import dev.macfi.luftung.domain.VentilationMode
import dev.macfi.luftung.domain.WindowState

class VentilationPreferencesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "ventilation_preferences",
        Context.MODE_PRIVATE,
    )

    fun readMode(): VentilationMode {
        val stored = prefs.getString(KEY_MODE, VentilationMode.FULL_AIRING.name)
        return runCatching { stored?.let { VentilationMode.valueOf(it) } }
            .getOrNull()
            ?: VentilationMode.FULL_AIRING
    }

    fun saveMode(mode: VentilationMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun readComfortProfile(): ComfortProfile {
        val preset = prefs.getString(KEY_PROFILE_PRESET, ComfortProfilePreset.BALANCED.name)
            ?.let { stored -> runCatching { ComfortProfilePreset.valueOf(stored) }.getOrNull() }
            ?: ComfortProfilePreset.BALANCED
        val default = preset.defaultProfile()
        return ComfortProfile(
            preset = preset,
            comfortStrictness = prefs.getFloat(KEY_COMFORT_STRICTNESS, default.comfortStrictness.toFloat()).toDouble(),
            temperaturePriority = prefs.getFloat(KEY_TEMPERATURE_PRIORITY, default.temperaturePriority.toFloat()).toDouble(),
            dewPointPriority = prefs.getFloat(KEY_DEW_POINT_PRIORITY, default.dewPointPriority.toFloat()).toDouble(),
        )
    }

    fun saveComfortProfile(profile: ComfortProfile) {
        prefs.edit()
            .putString(KEY_PROFILE_PRESET, profile.preset.name)
            .putFloat(KEY_COMFORT_STRICTNESS, profile.comfortStrictness.toFloat())
            .putFloat(KEY_TEMPERATURE_PRIORITY, profile.temperaturePriority.toFloat())
            .putFloat(KEY_DEW_POINT_PRIORITY, profile.dewPointPriority.toFloat())
            .apply()
    }

    fun saveComfortPreset(preset: ComfortProfilePreset) {
        saveComfortProfile(preset.defaultProfile())
    }

    fun readWindowState(): WindowState {
        val stored = prefs.getString(KEY_WINDOW_STATE, WindowState.CLOSED.name)
        return runCatching { stored?.let { WindowState.valueOf(it) } }
            .getOrNull()
            ?: WindowState.CLOSED
    }

    fun saveWindowState(windowState: WindowState) {
        val editor = prefs.edit().putString(KEY_WINDOW_STATE, windowState.name)
        if (windowState == WindowState.CLOSED) {
            editor.remove(KEY_OPENED_AT)
        } else if (!prefs.contains(KEY_OPENED_AT)) {
            editor.putLong(KEY_OPENED_AT, System.currentTimeMillis())
        }
        editor.apply()
    }

    fun readLastVentilatedAtMillis(): Long? {
        return prefs.getLong(KEY_LAST_VENTILATED_AT, Long.MIN_VALUE)
            .takeUnless { it == Long.MIN_VALUE }
    }

    fun markVentilated(nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong(KEY_LAST_VENTILATED_AT, nowMillis)
            .putLong(KEY_OPENED_AT, nowMillis)
            .putString(KEY_WINDOW_STATE, WindowState.OPEN.name)
            .apply()
    }

    fun readOpenedAtMillis(): Long? {
        return prefs.getLong(KEY_OPENED_AT, Long.MIN_VALUE)
            .takeUnless { it == Long.MIN_VALUE }
    }

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_LAST_VENTILATED_AT = "last_ventilated_at"
        const val KEY_PROFILE_PRESET = "profile_preset"
        const val KEY_COMFORT_STRICTNESS = "comfort_strictness"
        const val KEY_TEMPERATURE_PRIORITY = "temperature_priority"
        const val KEY_DEW_POINT_PRIORITY = "dew_point_priority"
        const val KEY_WINDOW_STATE = "window_state"
        const val KEY_OPENED_AT = "opened_at"
    }
}

private fun ComfortProfilePreset.defaultProfile(): ComfortProfile {
    return when (this) {
        ComfortProfilePreset.HUMIDITY_SENSITIVE -> ComfortProfile.humiditySensitive()
        ComfortProfilePreset.BALANCED -> ComfortProfile.balanced()
        ComfortProfilePreset.COOLING_FOCUSED -> ComfortProfile.coolingFocused()
    }
}
