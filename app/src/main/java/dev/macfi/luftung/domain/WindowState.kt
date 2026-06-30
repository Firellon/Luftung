package dev.macfi.luftung.domain

enum class WindowState(
    val label: String,
    val airChangesPerHour: Double,
) {
    CLOSED("Closed", 0.0),
    TILTED("Tilted", 0.35),
    OPEN("Open", 2.0),
    CROSS_VENTILATION("Cross ventilation", 5.0),
}
