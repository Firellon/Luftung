package dev.macfi.luftung.domain

data class ComfortProfile(
    val preset: ComfortProfilePreset,
    val comfortStrictness: Double,
    val temperaturePriority: Double,
    val dewPointPriority: Double,
) {
    companion object {
        fun balanced(
            comfortStrictness: Double = 1.0,
            temperaturePriority: Double = 1.0,
            dewPointPriority: Double = 1.0,
        ): ComfortProfile {
            return ComfortProfile(
                preset = ComfortProfilePreset.BALANCED,
                comfortStrictness = comfortStrictness,
                temperaturePriority = temperaturePriority,
                dewPointPriority = dewPointPriority,
            )
        }

        fun humiditySensitive(
            comfortStrictness: Double = 1.0,
            temperaturePriority: Double = 0.9,
            dewPointPriority: Double = 1.35,
        ): ComfortProfile {
            return ComfortProfile(
                preset = ComfortProfilePreset.HUMIDITY_SENSITIVE,
                comfortStrictness = comfortStrictness,
                temperaturePriority = temperaturePriority,
                dewPointPriority = dewPointPriority,
            )
        }

        fun coolingFocused(
            comfortStrictness: Double = 1.0,
            temperaturePriority: Double = 1.35,
            dewPointPriority: Double = 0.85,
        ): ComfortProfile {
            return ComfortProfile(
                preset = ComfortProfilePreset.COOLING_FOCUSED,
                comfortStrictness = comfortStrictness,
                temperaturePriority = temperaturePriority,
                dewPointPriority = dewPointPriority,
            )
        }
    }
}

enum class ComfortProfilePreset(val label: String) {
    HUMIDITY_SENSITIVE("Humidity Sensitive"),
    BALANCED("Balanced"),
    COOLING_FOCUSED("Cooling Focused"),
}
