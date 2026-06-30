package dev.macfi.luftung.data

import dev.macfi.luftung.domain.DewPointCalculator
import dev.macfi.luftung.domain.OutdoorConditions
import dev.macfi.luftung.domain.OutdoorConditionsSource
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

open class DwdWeatherProvider {
    open suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
    ): OutdoorConditions = withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.brightsky.dev/current_weather" +
                "?lat=$latitude" +
                "&lon=$longitude",
        )
        parseCurrentWeather(
            json = url.readTextFromHttp(),
            latitude = latitude,
            longitude = longitude,
        )
    }

    companion object {
        fun parseCurrentWeather(
            json: String,
            latitude: Double,
            longitude: Double,
        ): OutdoorConditions {
            val root = JSONObject(json)
            val weather = root.getJSONObject("weather")
            val temp = weather.getDouble("temperature")
            val rh = weather.getDouble("relative_humidity")
            val dewPoint = weather.optDouble("dew_point", Double.NaN)
                .takeUnless { it.isNaN() }
                ?: DewPointCalculator.calculateDewPoint(temp, rh)
            val sourceId = weather.optInt("source_id", -1)
            val stationName = root.optJSONArray("sources")
                ?.let { sources ->
                    (0 until sources.length())
                        .map { sources.getJSONObject(it) }
                        .firstOrNull { it.optInt("id", -2) == sourceId }
                        ?: sources.optJSONObject(0)
                }
                ?.optString("station_name")
                ?.ifBlank { null }

            return OutdoorConditions(
                temperatureC = temp,
                relativeHumidityPercent = rh,
                dewPointC = dewPoint,
                source = OutdoorConditionsSource.CurrentLocation(latitude, longitude),
                locationLabel = stationName?.let { "DWD station $it" } ?: "DWD observation",
                updatedAtMillis = Instant.parse(weather.getString("timestamp")).toEpochMilli(),
            )
        }
    }
}

private fun URL.readTextFromHttp(): String {
    val connection = (openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 10_000
        requestMethod = "GET"
    }
    return try {
        require(connection.responseCode in 200..299) {
            "HTTP ${connection.responseCode}: ${connection.errorStream?.bufferedReader()?.readText().orEmpty()}"
        }
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
