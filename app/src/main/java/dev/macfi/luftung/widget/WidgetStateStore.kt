package dev.macfi.luftung.widget

import android.content.Context
import dev.macfi.luftung.domain.Recommendation

class WidgetStateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "widget_state",
        Context.MODE_PRIVATE,
    )

    fun read(): WidgetDisplayState {
        val title = prefs.getString(KEY_TITLE, null) ?: return WidgetDisplayState.initial()
        val recommendation = parseRecommendationOrNull(prefs.getString(KEY_RECOMMENDATION, null))
        val status = parseStatusOrNull(prefs.getString(KEY_STATUS, WidgetStatus.ERROR.name))
            ?: return WidgetDisplayState.initial()
        return WidgetDisplayState(
            status = status,
            recommendation = recommendation,
            title = title,
            indoorLine = prefs.getString(KEY_INDOOR_LINE, "") ?: "",
            outdoorLine = prefs.getString(KEY_OUTDOOR_LINE, "") ?: "",
            reason = prefs.getString(KEY_REASON, "") ?: "",
            detailedReason = prefs.getString(KEY_DETAILED_REASON, null)
                ?: prefs.getString(KEY_REASON, "") ?: "",
            lastUpdated = prefs.getString(KEY_LAST_UPDATED, "Never") ?: "Never",
            stale = prefs.getBoolean(KEY_STALE, false),
        )
    }

    fun save(state: WidgetDisplayState) {
        prefs.edit()
            .putString(KEY_STATUS, state.status.name)
            .putString(KEY_RECOMMENDATION, state.recommendation?.name)
            .putString(KEY_TITLE, state.title)
            .putString(KEY_INDOOR_LINE, state.indoorLine)
            .putString(KEY_OUTDOOR_LINE, state.outdoorLine)
            .putString(KEY_REASON, state.reason)
            .putString(KEY_DETAILED_REASON, state.detailedReason)
            .putString(KEY_LAST_UPDATED, state.lastUpdated)
            .putBoolean(KEY_STALE, state.stale)
            .apply()
    }

    companion object {
        private const val KEY_STATUS = "status"
        private const val KEY_RECOMMENDATION = "recommendation"
        private const val KEY_TITLE = "title"
        private const val KEY_INDOOR_LINE = "indoor_line"
        private const val KEY_OUTDOOR_LINE = "outdoor_line"
        private const val KEY_REASON = "reason"
        private const val KEY_DETAILED_REASON = "detailed_reason"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val KEY_STALE = "stale"

        fun parseRecommendationOrNull(stored: String?): Recommendation? {
            return stored?.let { value ->
                runCatching { Recommendation.valueOf(value) }.getOrNull()
            }
        }

        fun parseStatusOrNull(stored: String?): WidgetStatus? {
            return stored?.let { value ->
                runCatching { WidgetStatus.valueOf(value) }.getOrNull()
            }
        }
    }
}
