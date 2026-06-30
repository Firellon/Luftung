package dev.macfi.luftung.data

import dev.macfi.luftung.domain.CitySearchResult
import dev.macfi.luftung.domain.OutdoorConditions

interface WeatherProvider {
    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
    ): OutdoorConditions

    suspend fun searchCities(query: String): List<CitySearchResult>
}
