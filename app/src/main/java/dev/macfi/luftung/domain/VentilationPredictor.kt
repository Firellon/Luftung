package dev.macfi.luftung.domain

enum class VentilationMode(val mixFactor: Double) {
    BRIEF_AIRING(0.15),
    FULL_AIRING(0.80),
}

object VentilationPredictor {
    fun predict(
        indoorTempC: Double,
        indoorDewPointC: Double,
        outdoorTempC: Double,
        outdoorDewPointC: Double,
        mode: VentilationMode,
    ): PredictedConditions {
        return PredictedConditions(
            temperatureC = lerp(indoorTempC, outdoorTempC, mode.mixFactor),
            dewPointC = lerp(indoorDewPointC, outdoorDewPointC, mode.mixFactor),
        )
    }

    fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * t
    }
}

data class PredictedConditions(
    val temperatureC: Double,
    val dewPointC: Double,
)
