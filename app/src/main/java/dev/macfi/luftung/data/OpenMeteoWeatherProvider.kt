package dev.macfi.luftung.data

import dev.macfi.luftung.domain.CitySearchResult
import dev.macfi.luftung.domain.DewPointCalculator
import dev.macfi.luftung.domain.OutdoorConditions
import dev.macfi.luftung.domain.OutdoorConditionsSource
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

open class OpenMeteoWeatherProvider : WeatherProvider {
    override suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
    ): OutdoorConditions = withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude" +
                "&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,dew_point_2m" +
                "&timezone=auto",
        )
        val current = JSONObject(url.readTextFromHttp()).getJSONObject("current")
        val temp = current.getDouble("temperature_2m")
        val rh = current.getDouble("relative_humidity_2m")
        val dewPoint = current.optDouble("dew_point_2m", Double.NaN)
            .takeUnless { it.isNaN() }
            ?: DewPointCalculator.calculateDewPoint(temp, rh)

        OutdoorConditions(
            temperatureC = temp,
            relativeHumidityPercent = rh,
            dewPointC = dewPoint,
            source = OutdoorConditionsSource.CurrentLocation(latitude, longitude),
            locationLabel = "Current location",
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    open suspend fun getCurrentWeatherForCity(city: OutdoorConditionsSource.City): OutdoorConditions {
        val conditions = getCurrentWeather(city.latitude, city.longitude)
        return conditions.copy(
            source = city,
            locationLabel = listOfNotNull(city.name, city.countryCode).joinToString(", "),
        )
    }

    override suspend fun searchCities(query: String): List<CitySearchResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 3) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
        val url = URL(
            "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=$encoded" +
                "&count=10" +
                "&language=en" +
                "&format=json",
        )
        val root = JSONObject(url.readTextFromHttp())
        val results = root.optJSONArray("results") ?: return@withContext emptyList()
        List(results.length()) { index ->
            val item = results.getJSONObject(index)
            val name = item.getString("name")
            val countryCode = item.optString("country_code").ifBlank { null }
            val admin = item.optString("admin1").ifBlank { null }
            CitySearchResult(
                name = name,
                countryCode = countryCode,
                latitude = item.getDouble("latitude"),
                longitude = item.getDouble("longitude"),
                displayName = listOfNotNull(name, admin, countryCode).joinToString(", "),
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
