package dev.macfi.luftung.domain

data class IndoorConditions(
    val temperatureC: Double,
    val relativeHumidityPercent: Double,
)

data class OutdoorConditions(
    val temperatureC: Double,
    val relativeHumidityPercent: Double,
    val dewPointC: Double,
    val source: OutdoorConditionsSource,
    val locationLabel: String?,
    val updatedAtMillis: Long,
)

sealed class OutdoorConditionsSource {
    data object Manual : OutdoorConditionsSource()

    data class CurrentLocation(
        val latitude: Double,
        val longitude: Double,
    ) : OutdoorConditionsSource()

    data class City(
        val name: String,
        val countryCode: String?,
        val latitude: Double,
        val longitude: Double,
    ) : OutdoorConditionsSource()
}

data class CitySearchResult(
    val name: String,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
)
