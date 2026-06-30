package dev.macfi.luftung.data

import dev.macfi.luftung.domain.OutdoorConditionsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DwdWeatherProviderTest {
    @Test
    fun mapsBrightSkyCurrentWeatherIntoOutdoorConditions() {
        val json = """
            {
              "weather": {
                "timestamp": "2026-06-30T12:30:00+00:00",
                "source_id": 123,
                "temperature": 30.0,
                "relative_humidity": 41,
                "dew_point": 15.4
              },
              "sources": [
                {
                  "id": 123,
                  "station_name": "Berlin-Tempelhof",
                  "distance": 2300
                }
              ]
            }
        """.trimIndent()

        val conditions = DwdWeatherProvider.parseCurrentWeather(
            json = json,
            latitude = 52.52,
            longitude = 13.41,
        )

        assertEquals(30.0, conditions.temperatureC, 0.001)
        assertEquals(41.0, conditions.relativeHumidityPercent, 0.001)
        assertEquals(15.4, conditions.dewPointC, 0.001)
        assertEquals("DWD station Berlin-Tempelhof", conditions.locationLabel)
        assertTrue(conditions.updatedAtMillis > 0L)
        assertTrue(conditions.source is OutdoorConditionsSource.CurrentLocation)
    }
}
