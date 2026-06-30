package dev.macfi.luftung.data

import android.content.Context
import dev.macfi.luftung.domain.IndoorConditions

class IndoorClimateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "indoor_climate",
        Context.MODE_PRIVATE,
    )

    fun read(): IndoorConditions? {
        if (!prefs.contains(KEY_TEMPERATURE_C) || !prefs.contains(KEY_RH_PERCENT)) {
            return null
        }
        return IndoorConditions(
            temperatureC = Double.fromBits(prefs.getLong(KEY_TEMPERATURE_C, 0L)),
            relativeHumidityPercent = Double.fromBits(prefs.getLong(KEY_RH_PERCENT, 0L)),
        )
    }

    fun save(sample: IndoorConditions) {
        prefs.edit()
            .putLong(KEY_TEMPERATURE_C, sample.temperatureC.toBits())
            .putLong(KEY_RH_PERCENT, sample.relativeHumidityPercent.toBits())
            .apply()
    }

    private companion object {
        const val KEY_TEMPERATURE_C = "temperature_c"
        const val KEY_RH_PERCENT = "relative_humidity_percent"
    }
}
