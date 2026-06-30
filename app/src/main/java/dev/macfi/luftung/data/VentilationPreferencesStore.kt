package dev.macfi.luftung.data

import android.content.Context
import dev.macfi.luftung.domain.VentilationMode

class VentilationPreferencesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "ventilation_preferences",
        Context.MODE_PRIVATE,
    )

    fun readMode(): VentilationMode {
        return prefs.getString(KEY_MODE, VentilationMode.ONE_WINDOW.name)
            ?.let { VentilationMode.valueOf(it) }
            ?: VentilationMode.ONE_WINDOW
    }

    fun saveMode(mode: VentilationMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun readLastVentilatedAtMillis(): Long? {
        return prefs.getLong(KEY_LAST_VENTILATED_AT, Long.MIN_VALUE)
            .takeUnless { it == Long.MIN_VALUE }
    }

    fun markVentilated(nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_VENTILATED_AT, nowMillis).apply()
    }

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_LAST_VENTILATED_AT = "last_ventilated_at"
    }
}
