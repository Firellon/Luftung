package dev.macfi.luftung.domain

object ComfortScorer {
    fun score(
        temperatureC: Double,
        dewPointC: Double,
    ): Double {
        val tempPenalty = temperaturePenalty(temperatureC)
        val dewPointPenalty = dewPointPenalty(dewPointC)
        return when {
            temperatureC < 26.0 -> tempPenalty + 1.3 * dewPointPenalty
            temperatureC < 32.0 -> 1.2 * tempPenalty + 1.2 * dewPointPenalty
            else -> 1.8 * tempPenalty + 1.5 * dewPointPenalty
        }
    }

    fun temperaturePenalty(temperatureC: Double): Double {
        return when {
            temperatureC < 18.0 -> 18.0 - temperatureC
            temperatureC <= 24.0 -> 0.0
            temperatureC <= 28.0 -> (temperatureC - 24.0) * 0.7
            temperatureC <= 32.0 -> (28.0 - 24.0) * 0.7 + (temperatureC - 28.0) * 1.2
            else -> (28.0 - 24.0) * 0.7 + (32.0 - 28.0) * 1.2 + (temperatureC - 32.0) * 2.0
        }
    }

    fun dewPointPenalty(dewPointC: Double): Double {
        return when {
            dewPointC < 5.0 -> (5.0 - dewPointC) * 0.5
            dewPointC <= 15.0 -> 0.0
            dewPointC <= 17.0 -> dewPointC - 15.0
            dewPointC <= 20.0 -> (17.0 - 15.0) + (dewPointC - 17.0) * 2.0
            else -> (17.0 - 15.0) + (20.0 - 17.0) * 2.0 + (dewPointC - 20.0) * 4.0
        }
    }
}
