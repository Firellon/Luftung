package dev.macfi.luftung.domain

object VentilationExplanationBuilder {
    fun build(
        candidate: VentilationCandidate,
        indoor: IndoorConditions,
        outdoor: OutdoorConditions,
        indoorDewPoint: Double,
        outdoorDewPoint: Double,
        predictedRelativeHumidityPercent: Double,
        currentScore: Double,
        staleAir: Boolean,
    ): VentilationExplanation {
        val temperatureDelta = candidate.prediction.temperatureC - indoor.temperatureC
        val dewPointDelta = candidate.prediction.dewPointC - indoorDewPoint
        val scoreDelta = currentScore - candidate.score
        val reason = reasonFor(
            recommendation = candidate.recommendation,
            outdoor = outdoor,
            indoor = indoor,
            outdoorDewPoint = outdoorDewPoint,
            indoorDewPoint = indoorDewPoint,
            scoreDelta = scoreDelta,
            staleAir = staleAir,
        )
        val expected = "Expected after: ${candidate.prediction.temperatureC.oneDecimal()} C / " +
            "${predictedRelativeHumidityPercent.oneDecimal()}% RH / " +
            "dew point ${candidate.prediction.dewPointC.oneDecimal()} C."
        return VentilationExplanation(
            temperatureDelta = temperatureDelta,
            dewPointDelta = dewPointDelta,
            scoreDelta = scoreDelta,
            shortReason = shortReasonFor(reason, temperatureDelta, dewPointDelta, scoreDelta),
            detailedReason = "$reason $expected",
        )
    }

    private fun reasonFor(
        recommendation: Recommendation,
        outdoor: OutdoorConditions,
        indoor: IndoorConditions,
        outdoorDewPoint: Double,
        indoorDewPoint: Double,
        scoreDelta: Double,
        staleAir: Boolean,
    ): String {
        if (staleAir) {
            return "Short airing is useful for fresh air after several closed hours."
        }
        if (recommendation == Recommendation.KEEP_CLOSED || recommendation == Recommendation.CLOSE_WINDOWS_NOW) {
            return "Opening would make comfort score worse."
        }

        val cooler = outdoor.temperatureC < indoor.temperatureC
        val drier = outdoorDewPoint < indoorDewPoint
        return when {
            cooler && drier -> "Outdoor air improves both temperature and dew point."
            cooler && !drier && scoreDelta > 0.0 -> "Cooling benefit outweighs the dew point increase."
            !cooler && drier && scoreDelta > 0.0 -> "Drier air helps, but warming limits the benefit."
            else -> "Predicted comfort improves after ventilation."
        }
    }

    private fun shortReasonFor(
        reason: String,
        temperatureDelta: Double,
        dewPointDelta: Double,
        scoreDelta: Double,
    ): String {
        if (reason.startsWith("Opening would")) {
            return when {
                dewPointDelta > 0.5 -> "Would get stickier."
                temperatureDelta > 0.5 -> "Would get warmer."
                else -> "Would not improve comfort."
            }
        }
        if (reason.startsWith("Short airing")) {
            return "Fresh air after closed hours."
        }
        return when {
            temperatureDelta < -0.5 && dewPointDelta <= 0.5 -> "Cooler, humidity OK."
            temperatureDelta < -0.5 && dewPointDelta > 0.5 -> "Cooler, slightly more humid."
            temperatureDelta > 0.5 && dewPointDelta < -0.5 -> "Drier, but warmer."
            scoreDelta > 0.0 -> "Comfort improves."
            else -> "Comfort unchanged."
        }
    }
}

data class VentilationExplanation(
    val temperatureDelta: Double,
    val dewPointDelta: Double,
    val scoreDelta: Double,
    val shortReason: String,
    val detailedReason: String,
)
