package dev.macfi.luftung.domain

import kotlin.math.ln

object DewPointCalculator {
    private const val A = 17.27
    private const val B = 237.7

    fun calculateDewPoint(
        temperatureC: Double,
        relativeHumidityPercent: Double,
    ): Double {
        validate(temperatureC, relativeHumidityPercent)
        val alpha = (A * temperatureC) / (B + temperatureC) +
            ln(relativeHumidityPercent / 100.0)
        return (B * alpha) / (A - alpha)
    }

    fun validate(
        temperatureC: Double,
        relativeHumidityPercent: Double,
    ) {
        require(temperatureC in -50.0..80.0) {
            "Temperature must be between -50 and 80 C."
        }
        require(relativeHumidityPercent in 0.0..100.0) {
            "Relative humidity must be between 0 and 100%."
        }
        require(relativeHumidityPercent > 0.0) {
            "Relative humidity must be greater than 0% for dew point."
        }
    }
}
