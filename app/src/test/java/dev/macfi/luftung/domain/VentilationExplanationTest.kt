package dev.macfi.luftung.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VentilationExplanationTest {
    @Test
    fun inverseMagnusReturnsRelativeHumidityFromTemperatureAndDewPoint() {
        val dewPoint = DewPointCalculator.calculateDewPoint(
            temperatureC = 24.0,
            relativeHumidityPercent = 50.0,
        )

        val relativeHumidity = DewPointCalculator.calculateRelativeHumidity(
            temperatureC = 24.0,
            dewPointC = dewPoint,
        )

        assertEquals(50.0, relativeHumidity, 0.2)
    }

    @Test
    fun adviceIncludesPredictedRelativeHumidityAndDeltas() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 29.0, relativeHumidityPercent = 60.0),
            outdoor = outdoor(temperatureC = 21.0, relativeHumidityPercent = 45.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        assertTrue(advice.predictedRelativeHumidityPercent in 1.0..100.0)
        assertTrue(advice.temperatureDelta < 0.0)
        assertTrue(advice.dewPointDelta < 0.0)
        assertTrue(advice.scoreDelta > 0.0)
        assertTrue(advice.shortReason.length <= 80)
        assertTrue(advice.detailedReason.contains("Expected after"))
    }

    @Test
    fun explanationCoversCoolerAndDrierOutdoorAir() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 29.0, relativeHumidityPercent = 60.0),
            outdoor = outdoor(temperatureC = 21.0, relativeHumidityPercent = 45.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        assertTrue(advice.detailedReason.contains("improves both temperature and dew point"))
    }

    @Test
    fun explanationCoversCoolerButMoreHumidOutdoorAir() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 29.0, relativeHumidityPercent = 45.0),
            outdoor = outdoor(temperatureC = 24.0, relativeHumidityPercent = 70.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.coolingFocused(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        assertTrue(advice.detailedReason.contains("Cooling benefit outweighs the dew point increase"))
    }

    @Test
    fun explanationCoversWarmerButDrierOutdoorAir() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 20.0, relativeHumidityPercent = 80.0),
            outdoor = outdoor(temperatureC = 24.0, relativeHumidityPercent = 35.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.humiditySensitive(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        assertTrue(advice.detailedReason.contains("Drier air helps, but warming limits the benefit"))
    }

    @Test
    fun explanationCoversWorseOverallRecommendation() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 22.0, relativeHumidityPercent = 45.0),
            outdoor = outdoor(temperatureC = 31.0, relativeHumidityPercent = 70.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        assertEquals(Recommendation.KEEP_CLOSED, advice.recommendation)
        assertTrue(advice.detailedReason.contains("Opening would make comfort score worse"))
    }

    @Test
    fun explanationCoversStaleAir() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 22.0, relativeHumidityPercent = 45.0),
            outdoor = outdoor(temperatureC = 23.0, relativeHumidityPercent = 45.0),
            windowState = WindowState.CLOSED,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = 0L,
            nowMillis = 4 * 3_600_000L,
        )

        assertTrue(advice.detailedReason.contains("Short airing is useful for fresh air"))
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
