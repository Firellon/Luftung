package dev.macfi.luftung.data

import dev.macfi.luftung.domain.OutdoorConditions
import dev.macfi.luftung.domain.OutdoorConditionsSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ObservedWeatherProviderTest {
    @Test
    fun fallsBackToOpenMeteoWhenDwdCurrentWeatherFails() = runBlocking {
        val fallback = OutdoorConditions(
            temperatureC = 28.4,
            relativeHumidityPercent = 25.0,
            dewPointC = 7.0,
            source = OutdoorConditionsSource.CurrentLocation(52.52, 13.41),
            locationLabel = "Open-Meteo fallback",
            updatedAtMillis = 1L,
        )
        val provider = ObservedWeatherProvider(
            dwdWeatherProvider = object : DwdWeatherProvider() {
                override suspend fun getCurrentWeather(latitude: Double, longitude: Double): OutdoorConditions {
                    error("DWD unavailable")
                }
            },
            openMeteoWeatherProvider = object : OpenMeteoWeatherProvider() {
                override suspend fun getCurrentWeather(latitude: Double, longitude: Double): OutdoorConditions {
                    return fallback
                }
            },
        )

        assertEquals(fallback, provider.getCurrentWeather(52.52, 13.41))
    }
}
