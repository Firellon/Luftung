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
        val recommendation = prefs.getString(KEY_RECOMMENDATION, null)?.let { Recommendation.valueOf(it) }
        return WidgetDisplayState(
            status = WidgetStatus.valueOf(prefs.getString(KEY_STATUS, WidgetStatus.ERROR.name)!!),
            recommendation = recommendation,
            title = title,
            indoorLine = prefs.getString(KEY_INDOOR_LINE, "") ?: "",
            outdoorLine = prefs.getString(KEY_OUTDOOR_LINE, "") ?: "",
            reason = prefs.getString(KEY_REASON, "") ?: "",
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
            .putString(KEY_LAST_UPDATED, state.lastUpdated)
            .putBoolean(KEY_STALE, state.stale)
            .apply()
    }

    private companion object {
        const val KEY_STATUS = "status"
        const val KEY_RECOMMENDATION = "recommendation"
        const val KEY_TITLE = "title"
        const val KEY_INDOOR_LINE = "indoor_line"
        const val KEY_OUTDOOR_LINE = "outdoor_line"
        const val KEY_REASON = "reason"
        const val KEY_LAST_UPDATED = "last_updated"
        const val KEY_STALE = "stale"
    }
}
