package dev.macfi.luftung.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComfortProfileTest {
    @Test
    fun balancedProfilePreservesCurrentComfortScore() {
        val score = ComfortScorer.score(
            temperatureC = 27.0,
            dewPointC = 18.0,
            profile = ComfortProfile.balanced(),
        )

        assertEquals(7.32, score, 0.001)
    }

    @Test
    fun humiditySensitiveProfilePenalizesHighDewPointMoreThanBalanced() {
        val balanced = ComfortScorer.score(27.0, 18.0, ComfortProfile.balanced())
        val humiditySensitive = ComfortScorer.score(27.0, 18.0, ComfortProfile.humiditySensitive())

        assertTrue(humiditySensitive > balanced)
    }

    @Test
    fun coolingFocusedProfilePenalizesHighTemperatureMoreThanBalanced() {
        val balanced = ComfortScorer.score(31.0, 12.0, ComfortProfile.balanced())
        val coolingFocused = ComfortScorer.score(31.0, 12.0, ComfortProfile.coolingFocused())

        assertTrue(coolingFocused > balanced)
    }
}
