package dev.macfi.luftung.data

import dev.macfi.luftung.domain.OutdoorConditions

object OutdoorConditionsCache {
    const val FRESH_MILLIS: Long = 30 * 60 * 1000L

    fun isFresh(
        conditions: OutdoorConditions,
        nowMillis: Long,
    ): Boolean {
        return nowMillis - conditions.updatedAtMillis <= FRESH_MILLIS
    }

    fun fallback(cached: OutdoorConditions): OutdoorConditionsResult {
        return OutdoorConditionsResult(
            conditions = cached,
            warning = "Using cached outdoor weather.",
        )
    }
}

data class OutdoorConditionsResult(
    val conditions: OutdoorConditions,
    val warning: String?,
)
