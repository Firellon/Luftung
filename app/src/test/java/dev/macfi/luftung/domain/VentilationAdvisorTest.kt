package dev.macfi.luftung.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VentilationAdvisorTest {
    @Test
    fun magnusDewPointMatchesKnownReferenceWithinTolerance() {
        val dewPoint = DewPointCalculator.calculateDewPoint(
            temperatureC = 20.0,
            relativeHumidityPercent = 50.0,
        )

        assertEquals(9.3, dewPoint, 0.2)
    }

    @Test
    fun comfortScorerKeepsIdealTemperatureAndDewPointAtZero() {
        val score = ComfortScorer.score(temperatureC = 22.0, dewPointC = 10.0)

        assertEquals(0.0, score, 0.001)
    }

    @Test
    fun comfortScorerAppliesColdDryWarmHumidAndHotBands() {
        assertEquals(3.3, ComfortScorer.score(temperatureC = 16.0, dewPointC = 3.0), 0.001)
        assertEquals(7.32, ComfortScorer.score(temperatureC = 27.0, dewPointC = 18.0), 0.001)
        assertEquals(44.88, ComfortScorer.score(temperatureC = 34.0, dewPointC = 22.0), 0.001)
    }

    @Test
    fun simplifiedActionsMapToDistinctMixFactors() {
        assertEquals(0.15, VentilationMode.BRIEF_AIRING.mixFactor, 0.001)
        assertEquals(0.80, VentilationMode.FULL_AIRING.mixFactor, 0.001)
    }

    @Test
    fun predictorInterpolatesTemperatureAndDewPointByVentilationMode() {
        val prediction = VentilationPredictor.predict(
            indoorTempC = 28.0,
            indoorDewPointC = 18.0,
            outdoorTempC = 20.0,
            outdoorDewPointC = 10.0,
            mode = VentilationMode.FULL_AIRING,
        )

        assertEquals(21.6, prediction.temperatureC, 0.001)
        assertEquals(11.6, prediction.dewPointC, 0.001)
    }

    @Test
    fun recommendationThresholdsUsePredictedComfortImprovement() {
        val advisor = VentilationAdvisor()

        assertEquals(
            Recommendation.STRONGLY_VENTILATE,
            advisor.recommendForScores(currentScore = 10.0, predictedScore = 6.9),
        )
        assertEquals(
            Recommendation.VENTILATE,
            advisor.recommendForScores(currentScore = 10.0, predictedScore = 9.0),
        )
        assertEquals(
            Recommendation.VENTILATE_BRIEFLY,
            advisor.recommendForScores(currentScore = 10.0, predictedScore = 9.8),
        )
        assertEquals(
            Recommendation.KEEP_CLOSED,
            advisor.recommendForScores(currentScore = 10.0, predictedScore = 10.0),
        )
        assertEquals(
            Recommendation.KEEP_CLOSED,
            advisor.recommendForScores(currentScore = 10.0, predictedScore = 10.1),
        )
    }

    @Test
    fun staleAirOverrideRecommendsBriefVentilationWhenWindowsWereClosedForHours() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 22.0, relativeHumidityPercent = 45.0),
            outdoor = OutdoorConditions(
                temperatureC = 23.0,
                relativeHumidityPercent = 45.0,
                dewPointC = DewPointCalculator.calculateDewPoint(23.0, 45.0),
                source = OutdoorConditionsSource.Manual,
                locationLabel = null,
                updatedAtMillis = 3_600_000L,
            ),
            mode = VentilationMode.BRIEF_AIRING,
            lastVentilatedAtMillis = 0L,
            nowMillis = 4 * 3_600_000L,
        )

        assertEquals(Recommendation.VENTILATE_BRIEFLY, advice.recommendation)
        assertTrue(advice.explanation.contains("closed for more than 3 hours"))
    }

    @Test
    fun staleAirOverrideDoesNotApplyWhenOutdoorAirIsMuchHotterAndMoreHumid() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 22.0, relativeHumidityPercent = 45.0),
            outdoor = OutdoorConditions(
                temperatureC = 28.0,
                relativeHumidityPercent = 70.0,
                dewPointC = DewPointCalculator.calculateDewPoint(28.0, 70.0),
                source = OutdoorConditionsSource.Manual,
                locationLabel = null,
                updatedAtMillis = 3_600_000L,
            ),
            mode = VentilationMode.FULL_AIRING,
            lastVentilatedAtMillis = 0L,
            nowMillis = 4 * 3_600_000L,
        )

        assertEquals(
            "recommendation for hot humid outdoor full airing",
            Recommendation.KEEP_CLOSED,
            advice.recommendation,
        )
    }

    @Test
    fun advisorReturnsPredictedStateAndExplanation() {
        val advice = VentilationAdvisor().assess(
            indoor = IndoorConditions(temperatureC = 28.0, relativeHumidityPercent = 55.0),
            outdoor = OutdoorConditions(
                temperatureC = 22.0,
                relativeHumidityPercent = 45.0,
                dewPointC = DewPointCalculator.calculateDewPoint(22.0, 45.0),
                source = OutdoorConditionsSource.Manual,
                locationLabel = null,
                updatedAtMillis = 0L,
            ),
            mode = VentilationMode.FULL_AIRING,
            lastVentilatedAtMillis = null,
            nowMillis = 0L,
        )

        assertEquals(23.2, advice.predictedTemp, 0.1)
        assertEquals(Recommendation.STRONGLY_VENTILATE, advice.recommendation)
        assertTrue(advice.currentScore > advice.predictedScore)
        assertTrue(advice.explanation.startsWith("Strongly ventilate."))
    }
}
