package dev.macfi.luftung.widget

import dev.macfi.luftung.domain.DewPointCalculator
import dev.macfi.luftung.domain.IndoorConditions
import dev.macfi.luftung.domain.OutdoorConditions
import dev.macfi.luftung.domain.OutdoorConditionsSource
import dev.macfi.luftung.domain.Recommendation
import dev.macfi.luftung.domain.VentilationAdvisor
import dev.macfi.luftung.domain.ComfortProfile
import dev.macfi.luftung.domain.WindowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateMapperTest {
    @Test
    fun stalePersistedRecommendationDoesNotCrashParser() {
        assertEquals(null, WidgetStateStore.parseRecommendationOrNull("VENTILATE_BRIEFLY"))
        assertEquals(Recommendation.OPEN_WINDOWS, WidgetStateStore.parseRecommendationOrNull("OPEN_WINDOWS"))
        assertEquals(null, WidgetStateStore.parseStatusOrNull("OLD_STATUS"))
        assertEquals(WidgetStatus.HELPFUL, WidgetStateStore.parseStatusOrNull("HELPFUL"))
    }

    @Test
    fun widgetMapperIncludesActionExpectedResultAndShortReason() {
        val indoor = IndoorConditions(temperatureC = 29.0, relativeHumidityPercent = 60.0)
        val outdoor = outdoor(temperatureC = 21.0, relativeHumidityPercent = 45.0)
        val advice = VentilationAdvisor().assess(
            indoor = indoor,
            outdoor = outdoor,
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        val state = WidgetStateMapper.fromAdvice(
            indoor = indoor,
            outdoor = outdoor,
            advice = advice,
            stale = false,
            warning = null,
        )

        assertTrue(state.title.startsWith("🪟 Open windows"))
        assertTrue(state.indoorLine.contains("🏠"))
        assertTrue(state.indoorLine.contains("->"))
        assertEquals(advice.shortReason, state.reason)
    }

    @Test
    fun keepClosedWidgetReasonExplainsWorseningFactor() {
        val indoor = IndoorConditions(temperatureC = 22.0, relativeHumidityPercent = 45.0)
        val outdoor = outdoor(temperatureC = 31.0, relativeHumidityPercent = 70.0)
        val advice = VentilationAdvisor().assess(
            indoor = indoor,
            outdoor = outdoor,
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        val state = WidgetStateMapper.fromAdvice(
            indoor = indoor,
            outdoor = outdoor,
            advice = advice,
            stale = false,
            warning = null,
        )

        assertTrue(state.title.startsWith("🔒 Keep closed"))
        assertTrue(state.reason.contains("Would get"))
    }

    private fun outdoor(
        temperatureC: Double,
        relativeHumidityPercent: Double,
    ): OutdoorConditions {
        return OutdoorConditions(
            temperatureC = temperatureC,
            relativeHumidityPercent = relativeHumidityPercent,
            dewPointC = DewPointCalculator.calculateDewPoint(temperatureC, relativeHumidityPercent),
            source = OutdoorConditionsSource.Manual,
            locationLabel = null,
            updatedAtMillis = 0L,
        )
    }
}
