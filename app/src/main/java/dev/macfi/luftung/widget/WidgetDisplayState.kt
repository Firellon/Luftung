package dev.macfi.luftung.widget

import dev.macfi.luftung.domain.Recommendation

data class WidgetDisplayState(
    val status: WidgetStatus,
    val recommendation: Recommendation?,
    val title: String,
    val indoorLine: String,
    val outdoorLine: String,
    val reason: String,
    val detailedReason: String,
    val lastUpdated: String,
    val stale: Boolean,
) {
    companion object
}

enum class WidgetStatus {
    HELPFUL,
    MIXED,
    NOT_HELPFUL,
    COLD_OR_DRY,
    NEEDS_INPUT,
    ERROR,
}

fun WidgetDisplayState.Companion.initial(): WidgetDisplayState {
    return WidgetDisplayState(
        status = WidgetStatus.NEEDS_INPUT,
        recommendation = null,
        title = "Indoor and outdoor data needed",
        indoorLine = "Indoor: missing",
        outdoorLine = "Outdoor: missing",
        reason = "Open Luftung and enter the room conditions.",
        detailedReason = "Open Luftung and enter the room conditions.",
        lastUpdated = "Never",
        stale = false,
    )
}
