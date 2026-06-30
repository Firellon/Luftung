package dev.macfi.luftung.data

import dev.macfi.luftung.domain.OutdoorConditions
import dev.macfi.luftung.domain.OutdoorConditionsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutdoorConditionsRepositoryTest {
    @Test
    fun cacheIsFreshForThirtyMinutesAndStaleAfterwards() {
        val conditions = OutdoorConditions(
            temperatureC = 20.0,
            relativeHumidityPercent = 50.0,
            dewPointC = 9.3,
            source = OutdoorConditionsSource.Manual,
            locationLabel = null,
            updatedAtMillis = 1_000L,
        )

        assertTrue(OutdoorConditionsCache.isFresh(conditions, nowMillis = 1_000L + 29 * 60_000L))
        assertFalse(OutdoorConditionsCache.isFresh(conditions, nowMillis = 1_000L + 31 * 60_000L))
    }

    @Test
    fun failedFetchReturnsCachedOutdoorConditionsWithWarning() {
        val cached = OutdoorConditions(
            temperatureC = 18.0,
            relativeHumidityPercent = 60.0,
            dewPointC = 10.1,
            source = OutdoorConditionsSource.City("Berlin", "DE", 52.52, 13.41),
            locationLabel = "Berlin, DE",
            updatedAtMillis = 123L,
        )
        val result = OutdoorConditionsCache.fallback(cached)

        assertEquals(cached, result.conditions)
        assertEquals("Using cached outdoor weather.", result.warning)
    }
}
