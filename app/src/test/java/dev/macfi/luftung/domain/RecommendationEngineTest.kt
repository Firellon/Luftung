package dev.macfi.luftung.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    @Test
    fun closedWindowsRecommendOpeningWhenCandidateImprovesComfort() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 29.0, relativeHumidityPercent = 60.0),
            outdoor = outdoor(temperatureC = 21.0, relativeHumidityPercent = 45.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        assertEquals(Recommendation.OPEN_WINDOWS, advice.recommendation)
        assertTrue(advice.recommendedMinutes in listOf(5, 10, 15, 30, 60))
        assertTrue(advice.currentScore > advice.predictedScore)
    }

    @Test
    fun closedWindowsRecommendKeepingClosedWhenNoCandidateImprovesComfort() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 22.0, relativeHumidityPercent = 45.0),
            outdoor = outdoor(temperatureC = 31.0, relativeHumidityPercent = 70.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        assertEquals(Recommendation.KEEP_CLOSED, advice.recommendation)
        assertEquals(0, advice.recommendedMinutes)
    }

    @Test
    fun staleClosedRoomCanRecommendShortAiringEvenWithSmallComfortCost() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 22.0, relativeHumidityPercent = 45.0),
            outdoor = outdoor(temperatureC = 23.0, relativeHumidityPercent = 45.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = 0L,
            nowMillis = 4 * 3_600_000L,
        )

        assertEquals(Recommendation.OPEN_WINDOWS, advice.recommendation)
        assertEquals(5, advice.recommendedMinutes)
        assertTrue(advice.explanation.contains("fresh air"))
    }

    @Test
    fun alreadyOpenWindowsRecommendClosingWhenFurtherVentilationDoesNotHelp() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 22.0, relativeHumidityPercent = 45.0),
            outdoor = outdoor(temperatureC = 31.0, relativeHumidityPercent = 70.0),
            windowState = WindowState.OPEN,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        assertEquals(Recommendation.CLOSE_WINDOWS_NOW, advice.recommendation)
        assertEquals(0, advice.recommendedMinutes)
    }

    @Test
    fun profileChangesSelectedRecommendationScore() {
        val balanced = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 29.0, relativeHumidityPercent = 45.0),
            outdoor = outdoor(temperatureC = 24.0, relativeHumidityPercent = 70.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )
        val humiditySensitive = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 29.0, relativeHumidityPercent = 45.0),
            outdoor = outdoor(temperatureC = 24.0, relativeHumidityPercent = 70.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.humiditySensitive(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        assertTrue(humiditySensitive.predictedScore > balanced.predictedScore)
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
