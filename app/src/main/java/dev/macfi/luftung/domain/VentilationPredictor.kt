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

    fun predictAfterMinutes(
        indoorTempC: Double,
        indoorDewPointC: Double,
        outdoorTempC: Double,
        outdoorDewPointC: Double,
        windowState: WindowState,
        minutes: Int,
    ): PredictedConditions {
        val mixFactor = exponentialMixFactor(windowState, minutes)
        return PredictedConditions(
            temperatureC = lerp(indoorTempC, outdoorTempC, mixFactor),
            dewPointC = lerp(indoorDewPointC, outdoorDewPointC, mixFactor),
        )
    }

    fun exponentialMixFactor(
        windowState: WindowState,
        minutes: Int,
    ): Double {
        if (windowState == WindowState.CLOSED || minutes <= 0) return 0.0
        val hours = minutes / 60.0
        return 1.0 - kotlin.math.exp(-windowState.airChangesPerHour * hours)
    }
}

data class PredictedConditions(
    val temperatureC: Double,
    val dewPointC: Double,
)
