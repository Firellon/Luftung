package dev.macfi.luftung.domain

class VentilationAdvisor {
    fun assess(
        indoor: IndoorConditions,
        outdoor: OutdoorConditions,
        windowState: WindowState,
        comfortProfile: ComfortProfile,
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
        val currentScore = ComfortScorer.score(indoor.temperatureC, indoorDewPoint, comfortProfile)
        val staleAir = windowState == WindowState.CLOSED &&
            lastVentilatedAtMillis != null &&
            isStaleAir(lastVentilatedAtMillis, nowMillis) &&
            !isOutdoorHotAndHumidException(indoor, outdoor, indoorDewPoint, outdoorDewPoint)

        val candidates = candidatesFor(windowState, indoor, outdoor, indoorDewPoint, comfortProfile)
        val best = candidates.minBy { it.score }
        val currentCandidate = VentilationCandidate(
            recommendation = if (windowState == WindowState.CLOSED) {
                Recommendation.KEEP_CLOSED
            } else {
                Recommendation.CLOSE_WINDOWS_NOW
            },
            minutes = 0,
            prediction = PredictedConditions(indoor.temperatureC, indoorDewPoint),
            score = currentScore,
        )

        val selected = when {
            staleAir && best.score >= currentScore -> candidates.first { it.minutes == 5 }
            best.score < currentScore -> best
            else -> currentCandidate
        }

        return VentilationAdvice(
            recommendation = selected.recommendation,
            recommendedMinutes = selected.minutes,
            currentIndoorTemp = indoor.temperatureC,
            currentIndoorDewPoint = indoorDewPoint,
            outdoorTemp = outdoor.temperatureC,
            outdoorDewPoint = outdoorDewPoint,
            predictedTemp = selected.prediction.temperatureC,
            predictedDewPoint = selected.prediction.dewPointC,
            currentScore = currentScore,
            predictedScore = selected.score,
            explanation = buildExplanation(
                candidate = selected,
                indoor = indoor,
                outdoor = outdoor,
                indoorDewPoint = indoorDewPoint,
                outdoorDewPoint = outdoorDewPoint,
                staleAir = staleAir && best.score >= currentScore,
            ),
        )
    }

    fun assess(
        indoor: IndoorConditions,
        outdoor: OutdoorConditions,
        mode: VentilationMode,
        lastVentilatedAtMillis: Long?,
        nowMillis: Long,
    ): VentilationAdvice {
        val state = when (mode) {
            VentilationMode.BRIEF_AIRING -> WindowState.TILTED
            VentilationMode.FULL_AIRING -> WindowState.OPEN
        }
        return assess(
            indoor = indoor,
            outdoor = outdoor,
            windowState = state,
            comfortProfile = ComfortProfile.balanced(),
            lastVentilatedAtMillis = lastVentilatedAtMillis,
            nowMillis = nowMillis,
        )
    }

    fun recommendForScores(
        currentScore: Double,
        predictedScore: Double,
    ): Recommendation {
        return if (predictedScore < currentScore) {
            Recommendation.OPEN_WINDOWS
        } else {
            Recommendation.KEEP_CLOSED
        }
    }

    private fun candidatesFor(
        windowState: WindowState,
        indoor: IndoorConditions,
        outdoor: OutdoorConditions,
        indoorDewPoint: Double,
        comfortProfile: ComfortProfile,
    ): List<VentilationCandidate> {
        val candidateWindowState = if (windowState == WindowState.CLOSED) WindowState.OPEN else windowState
        val recommendation = if (windowState == WindowState.CLOSED) {
            Recommendation.OPEN_WINDOWS
        } else {
            Recommendation.KEEP_WINDOWS_OPEN
        }
        return CANDIDATE_MINUTES.map { minutes ->
            val prediction = VentilationPredictor.predictAfterMinutes(
                indoorTempC = indoor.temperatureC,
                indoorDewPointC = indoorDewPoint,
                outdoorTempC = outdoor.temperatureC,
                outdoorDewPointC = outdoor.dewPointC,
                windowState = candidateWindowState,
                minutes = minutes,
            )
            VentilationCandidate(
                recommendation = recommendation,
                minutes = minutes,
                prediction = prediction,
                score = ComfortScorer.score(prediction.temperatureC, prediction.dewPointC, comfortProfile),
            )
        }
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
        candidate: VentilationCandidate,
        indoor: IndoorConditions,
        outdoor: OutdoorConditions,
        indoorDewPoint: Double,
        outdoorDewPoint: Double,
        staleAir: Boolean,
    ): String {
        if (staleAir) {
            return "Open windows for ${candidate.minutes} minutes. Some fresh air is useful after several closed hours, and outdoor conditions are not extremely unfavorable."
        }

        val warmerCooler = if (outdoor.temperatureC >= indoor.temperatureC) "warmer" else "cooler"
        val drierWetter = if (outdoorDewPoint <= indoorDewPoint) "drier" else "more humid"
        val duration = if (candidate.minutes > 0) " for ${candidate.minutes} minutes" else ""
        return "${candidate.recommendation.label}$duration. Outdoor air is ${kotlin.math.abs(outdoor.temperatureC - indoor.temperatureC).oneDecimal()} C $warmerCooler and ${kotlin.math.abs(outdoorDewPoint - indoorDewPoint).oneDecimal()} C $drierWetter by dew point. Predicted result: ${candidate.prediction.temperatureC.oneDecimal()} C, dew point ${candidate.prediction.dewPointC.oneDecimal()} C."
    }

    private companion object {
        val CANDIDATE_MINUTES = listOf(5, 10, 15, 30, 60)
        const val STALE_AIR_MILLIS = 3 * 60 * 60 * 1000L
    }
}

data class VentilationAdvice(
    val recommendation: Recommendation,
    val recommendedMinutes: Int,
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

data class VentilationCandidate(
    val recommendation: Recommendation,
    val minutes: Int,
    val prediction: PredictedConditions,
    val score: Double,
)

enum class Recommendation(val label: String) {
    OPEN_WINDOWS("Open windows"),
    KEEP_WINDOWS_OPEN("Keep windows open"),
    CLOSE_WINDOWS_NOW("Close windows now"),
    KEEP_CLOSED("Keep closed"),
}

fun Double.oneDecimal(): String {
    return String.format(java.util.Locale.US, "%.1f", this)
}
