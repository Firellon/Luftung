package dev.macfi.luftung.data

import android.content.Context
import dev.macfi.luftung.domain.DewPointCalculator
import dev.macfi.luftung.domain.OutdoorConditions
import dev.macfi.luftung.domain.OutdoorConditionsSource

class OutdoorConditionsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "outdoor_conditions",
        Context.MODE_PRIVATE,
    )

    fun readMode(): OutdoorInputMode {
        return prefs.getString(KEY_MODE, OutdoorInputMode.MANUAL.name)
            ?.let { OutdoorInputMode.valueOf(it) }
            ?: OutdoorInputMode.MANUAL
    }

    fun saveMode(mode: OutdoorInputMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun readManual(): OutdoorConditions? {
        if (!prefs.contains(KEY_MANUAL_TEMP_C) || !prefs.contains(KEY_MANUAL_RH)) return null
        val temp = Double.fromBits(prefs.getLong(KEY_MANUAL_TEMP_C, 0L))
        val rh = Double.fromBits(prefs.getLong(KEY_MANUAL_RH, 0L))
        return OutdoorConditions(
            temperatureC = temp,
            relativeHumidityPercent = rh,
            dewPointC = DewPointCalculator.calculateDewPoint(temp, rh),
            source = OutdoorConditionsSource.Manual,
            locationLabel = "Manual",
            updatedAtMillis = prefs.getLong(KEY_MANUAL_UPDATED_AT, System.currentTimeMillis()),
        )
    }

    fun saveManual(
        temperatureC: Double,
        relativeHumidity: Double,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        prefs.edit()
            .putLong(KEY_MANUAL_TEMP_C, temperatureC.toBits())
            .putLong(KEY_MANUAL_RH, relativeHumidity.toBits())
            .putLong(KEY_MANUAL_UPDATED_AT, nowMillis)
            .apply()
    }

    fun readCached(): OutdoorConditions? {
        if (!prefs.contains(KEY_CACHE_TEMP_C) || !prefs.contains(KEY_CACHE_RH)) return null
        val source = readCachedSource() ?: return null
        return OutdoorConditions(
            temperatureC = Double.fromBits(prefs.getLong(KEY_CACHE_TEMP_C, 0L)),
            relativeHumidityPercent = Double.fromBits(prefs.getLong(KEY_CACHE_RH, 0L)),
            dewPointC = Double.fromBits(prefs.getLong(KEY_CACHE_DEW_POINT_C, 0L)),
            source = source,
            locationLabel = prefs.getString(KEY_CACHE_LABEL, null),
            updatedAtMillis = prefs.getLong(KEY_CACHE_UPDATED_AT, 0L),
        )
    }

    fun saveCached(conditions: OutdoorConditions) {
        val editor = prefs.edit()
            .putLong(KEY_CACHE_TEMP_C, conditions.temperatureC.toBits())
            .putLong(KEY_CACHE_RH, conditions.relativeHumidityPercent.toBits())
            .putLong(KEY_CACHE_DEW_POINT_C, conditions.dewPointC.toBits())
            .putString(KEY_CACHE_LABEL, conditions.locationLabel)
            .putLong(KEY_CACHE_UPDATED_AT, conditions.updatedAtMillis)

        when (val source = conditions.source) {
            OutdoorConditionsSource.Manual -> editor.putString(KEY_CACHE_SOURCE, "manual")
            is OutdoorConditionsSource.CurrentLocation -> editor
                .putString(KEY_CACHE_SOURCE, "current_location")
                .putLong(KEY_CACHE_LAT, source.latitude.toBits())
                .putLong(KEY_CACHE_LON, source.longitude.toBits())
            is OutdoorConditionsSource.City -> editor
                .putString(KEY_CACHE_SOURCE, "city")
                .putString(KEY_CACHE_CITY_NAME, source.name)
                .putString(KEY_CACHE_COUNTRY_CODE, source.countryCode)
                .putLong(KEY_CACHE_LAT, source.latitude.toBits())
                .putLong(KEY_CACHE_LON, source.longitude.toBits())
        }
        editor.apply()
    }

    fun saveSelectedCity(
        name: String,
        countryCode: String?,
        latitude: Double,
        longitude: Double,
    ) {
        prefs.edit()
            .putString(KEY_CITY_NAME, name)
            .putString(KEY_COUNTRY_CODE, countryCode)
            .putLong(KEY_CITY_LAT, latitude.toBits())
            .putLong(KEY_CITY_LON, longitude.toBits())
            .apply()
    }

    fun readSelectedCity(): OutdoorConditionsSource.City? {
        val name = prefs.getString(KEY_CITY_NAME, null) ?: return null
        return OutdoorConditionsSource.City(
            name = name,
            countryCode = prefs.getString(KEY_COUNTRY_CODE, null),
            latitude = Double.fromBits(prefs.getLong(KEY_CITY_LAT, 0L)),
            longitude = Double.fromBits(prefs.getLong(KEY_CITY_LON, 0L)),
        )
    }

    private fun readCachedSource(): OutdoorConditionsSource? {
        return when (prefs.getString(KEY_CACHE_SOURCE, null)) {
            "manual" -> OutdoorConditionsSource.Manual
            "current_location" -> OutdoorConditionsSource.CurrentLocation(
                latitude = Double.fromBits(prefs.getLong(KEY_CACHE_LAT, 0L)),
                longitude = Double.fromBits(prefs.getLong(KEY_CACHE_LON, 0L)),
            )
            "city" -> OutdoorConditionsSource.City(
                name = prefs.getString(KEY_CACHE_CITY_NAME, null) ?: return null,
                countryCode = prefs.getString(KEY_CACHE_COUNTRY_CODE, null),
                latitude = Double.fromBits(prefs.getLong(KEY_CACHE_LAT, 0L)),
                longitude = Double.fromBits(prefs.getLong(KEY_CACHE_LON, 0L)),
            )
            else -> null
        }
    }

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_MANUAL_TEMP_C = "manual_temp_c"
        const val KEY_MANUAL_RH = "manual_rh"
        const val KEY_MANUAL_UPDATED_AT = "manual_updated_at"
        const val KEY_CACHE_TEMP_C = "cache_temp_c"
        const val KEY_CACHE_RH = "cache_rh"
        const val KEY_CACHE_DEW_POINT_C = "cache_dew_point_c"
        const val KEY_CACHE_LABEL = "cache_label"
        const val KEY_CACHE_UPDATED_AT = "cache_updated_at"
        const val KEY_CACHE_SOURCE = "cache_source"
        const val KEY_CACHE_CITY_NAME = "cache_city_name"
        const val KEY_CACHE_COUNTRY_CODE = "cache_country_code"
        const val KEY_CACHE_LAT = "cache_lat"
        const val KEY_CACHE_LON = "cache_lon"
        const val KEY_CITY_NAME = "city_name"
        const val KEY_COUNTRY_CODE = "country_code"
        const val KEY_CITY_LAT = "city_lat"
        const val KEY_CITY_LON = "city_lon"
    }
}
