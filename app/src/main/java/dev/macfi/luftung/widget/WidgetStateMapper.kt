package dev.macfi.luftung.widget

import dev.macfi.luftung.domain.IndoorConditions
import dev.macfi.luftung.domain.OutdoorConditions
import dev.macfi.luftung.domain.Recommendation
import dev.macfi.luftung.domain.VentilationAdvice
import dev.macfi.luftung.domain.oneDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object WidgetStateMapper {
    fun fromAdvice(
        indoor: IndoorConditions,
        outdoor: OutdoorConditions,
        advice: VentilationAdvice,
        stale: Boolean,
        warning: String?,
    ): WidgetDisplayState {
        return WidgetDisplayState(
            status = advice.status(),
            recommendation = advice.recommendation,
            title = advice.recommendation.label,
            indoorLine = "Indoor: ${indoor.temperatureC.oneDecimal()} C / ${indoor.relativeHumidityPercent.oneDecimal()}% / DP ${advice.currentIndoorDewPoint.oneDecimal()} C",
            outdoorLine = "Outdoor: ${outdoor.temperatureC.oneDecimal()} C / ${outdoor.relativeHumidityPercent.oneDecimal()}% / DP ${outdoor.dewPointC.oneDecimal()} C${outdoor.locationSuffix()}",
            reason = when {
                stale -> "Outdoor data stale - tap to refresh."
                warning != null -> "$warning ${advice.shortReason()}"
                else -> advice.shortReason()
            },
            lastUpdated = formatTime(outdoor.updatedAtMillis),
            stale = stale,
        )
    }

    fun error(title: String, reason: String): WidgetDisplayState {
        return WidgetDisplayState(
            status = WidgetStatus.ERROR,
            recommendation = null,
            title = title,
            indoorLine = "Indoor: unavailable",
            outdoorLine = "Outdoor: unavailable",
            reason = reason,
            lastUpdated = "Never",
            stale = false,
        )
    }

    private fun VentilationAdvice.status(): WidgetStatus {
        return when (recommendation) {
            Recommendation.STRONGLY_VENTILATE,
            Recommendation.VENTILATE -> WidgetStatus.HELPFUL
            Recommendation.VENTILATE_BRIEFLY -> {
                if (predictedDewPoint < 5.0 || predictedTemp < 18.0) WidgetStatus.COLD_OR_DRY else WidgetStatus.MIXED
            }
            Recommendation.KEEP_CLOSED -> WidgetStatus.NOT_HELPFUL
        }
    }

    private fun VentilationAdvice.shortReason(): String {
        val improvement = currentScore - predictedScore
        return "Comfort score ${if (improvement >= 0) "improves" else "worsens"} by ${kotlin.math.abs(improvement).oneDecimal()}."
    }

    private fun formatTime(millis: Long): String {
        return DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(millis))
    }

    private fun OutdoorConditions.locationSuffix(): String {
        return locationLabel?.let { " - $it" }.orEmpty()
    }
}
