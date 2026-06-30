package dev.macfi.luftung.data

import dev.macfi.luftung.domain.CitySearchResult
import dev.macfi.luftung.domain.OutdoorConditions
import dev.macfi.luftung.domain.OutdoorConditionsSource

class ObservedWeatherProvider(
    private val dwdWeatherProvider: DwdWeatherProvider = DwdWeatherProvider(),
    private val openMeteoWeatherProvider: OpenMeteoWeatherProvider = OpenMeteoWeatherProvider(),
) : WeatherProvider {
    override suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
    ): OutdoorConditions {
        return runCatching {
            dwdWeatherProvider.getCurrentWeather(latitude, longitude)
        }.getOrElse {
            openMeteoWeatherProvider.getCurrentWeather(latitude, longitude)
        }
    }

    suspend fun getCurrentWeatherForCity(city: OutdoorConditionsSource.City): OutdoorConditions {
        return runCatching {
            val observed = dwdWeatherProvider.getCurrentWeather(city.latitude, city.longitude)
            observed.copy(
                source = city,
                locationLabel = "${observed.locationLabel} near ${listOfNotNull(city.name, city.countryCode).joinToString(", ")}",
            )
        }.getOrElse {
            openMeteoWeatherProvider.getCurrentWeatherForCity(city)
        }
    }

    override suspend fun searchCities(query: String): List<CitySearchResult> {
        return openMeteoWeatherProvider.searchCities(query)
    }

}
