package dev.macfi.luftung.domain

class VentilationAdvisor {
    fun assess(
        indoor: IndoorConditions,
        outdoor: OutdoorConditions,
        mode: VentilationMode,
        lastVentilatedAtMillis: Long?,
        nowMillis: Long,
    ): VentilationAdvice {
        DewPointCalculator.validate(indoor.temperatureC, indoor.relativeHumidityPercent)
        DewPointCalculator.validate(outdoor.temperatureC, outdoor.relativeHumidityPercent)

        val indoorDewPoint = DewPointCalculator.calculateDewPoint(
            temperatureC = indoor.temperatureC,
            relativeHumidityPercent = indoor.relativeHumidityPercent,
        )
        val outdoorDewPoint = outdoor.dewPointC
        val predicted = VentilationPredictor.predict(
            indoorTempC = indoor.temperatureC,
            indoorDewPointC = indoorDewPoint,
            outdoorTempC = outdoor.temperatureC,
            outdoorDewPointC = outdoorDewPoint,
            mode = mode,
        )
        val currentScore = ComfortScorer.score(indoor.temperatureC, indoorDewPoint)
        val predictedScore = ComfortScorer.score(predicted.temperatureC, predicted.dewPointC)
        val recommendation = staleAirRecommendation(
            indoor = indoor,
            outdoor = outdoor,
            indoorDewPoint = indoorDewPoint,
            outdoorDewPoint = outdoorDewPoint,
            lastVentilatedAtMillis = lastVentilatedAtMillis,
            nowMillis = nowMillis,
        ) ?: recommendForScores(currentScore, predictedScore)

        return VentilationAdvice(
            recommendation = recommendation,
            currentIndoorTemp = indoor.temperatureC,
            currentIndoorDewPoint = indoorDewPoint,
            outdoorTemp = outdoor.temperatureC,
            outdoorDewPoint = outdoorDewPoint,
            predictedTemp = predicted.temperatureC,
            predictedDewPoint = predicted.dewPointC,
            currentScore = currentScore,
            predictedScore = predictedScore,
            explanation = buildExplanation(
                recommendation = recommendation,
                indoor = indoor,
                outdoor = outdoor,
                predicted = predicted,
                indoorDewPoint = indoorDewPoint,
                outdoorDewPoint = outdoorDewPoint,
                staleOverride = lastVentilatedAtMillis != null &&
                    isStaleAir(lastVentilatedAtMillis, nowMillis) &&
                    !isOutdoorHotAndHumidException(indoor, outdoor, indoorDewPoint, outdoorDewPoint),
            ),
        )
    }

    fun recommendForScores(
        currentScore: Double,
        predictedScore: Double,
    ): Recommendation {
        val improvement = currentScore - predictedScore
        return when {
            improvement >= 3.0 -> Recommendation.STRONGLY_VENTILATE
            improvement >= 1.0 -> Recommendation.VENTILATE
            improvement > -1.0 -> Recommendation.VENTILATE_BRIEFLY
            else -> Recommendation.KEEP_CLOSED
        }
    }

    private fun staleAirRecommendation(
        indoor: IndoorConditions,
        outdoor: OutdoorConditions,
        indoorDewPoint: Double,
        outdoorDewPoint: Double,
        lastVentilatedAtMillis: Long?,
        nowMillis: Long,
    ): Recommendation? {
        if (lastVentilatedAtMillis == null || !isStaleAir(lastVentilatedAtMillis, nowMillis)) {
            return null
        }
        if (isOutdoorHotAndHumidException(indoor, outdoor, indoorDewPoint, outdoorDewPoint)) {
            return null
        }
        return Recommendation.VENTILATE_BRIEFLY
    }

    private fun isStaleAir(
        lastVentilatedAtMillis: Long,
        nowMillis: Long,
    ): Boolean {
        return nowMillis - lastVentilatedAtMillis >= STALE_AIR_MILLIS
    }

    private fun isOutdoorHotAndHumidException(
        indoor: IndoorConditions,
        outdoor: OutdoorConditions,
        indoorDewPoint: Double,
        outdoorDewPoint: Double,
    ): Boolean {
        return outdoor.temperatureC > indoor.temperatureC + 5.0 &&
            outdoorDewPoint > indoorDewPoint + 3.0
    }

    private fun buildExplanation(
        recommendation: Recommendation,
        indoor: IndoorConditions,
        outdoor: OutdoorConditions,
        predicted: PredictedConditions,
        indoorDewPoint: Double,
        outdoorDewPoint: Double,
        staleOverride: Boolean,
    ): String {
        if (staleOverride) {
            return "Ventilate briefly. Windows have been closed for more than 3 hours; 5-10 minutes of airing is useful unless outdoor air is much hotter and more humid."
        }

        val warmerCooler = if (outdoor.temperatureC >= indoor.temperatureC) "warmer" else "cooler"
        val drierWetter = if (outdoorDewPoint <= indoorDewPoint) "drier" else "more humid"
        return "${recommendation.label}. Outdoor air is ${kotlin.math.abs(outdoor.temperatureC - indoor.temperatureC).oneDecimal()} C $warmerCooler and ${kotlin.math.abs(outdoorDewPoint - indoorDewPoint).oneDecimal()} C $drierWetter by dew point. Predicted result after ventilation: ${predicted.temperatureC.oneDecimal()} C, dew point ${predicted.dewPointC.oneDecimal()} C."
    }

    private companion object {
        const val STALE_AIR_MILLIS = 3 * 60 * 60 * 1000L
    }
}

data class VentilationAdvice(
    val recommendation: Recommendation,
    val currentIndoorTemp: Double,
    val currentIndoorDewPoint: Double,
    val outdoorTemp: Double,
    val outdoorDewPoint: Double,
    val predictedTemp: Double,
    val predictedDewPoint: Double,
    val currentScore: Double,
    val predictedScore: Double,
    val explanation: String,
)

enum class Recommendation(val label: String) {
    STRONGLY_VENTILATE("Strongly ventilate"),
    VENTILATE("Ventilate"),
    VENTILATE_BRIEFLY("Ventilate briefly"),
    KEEP_CLOSED("Keep closed"),
}

fun Double.oneDecimal(): String {
    return String.format(java.util.Locale.US, "%.1f", this)
}
