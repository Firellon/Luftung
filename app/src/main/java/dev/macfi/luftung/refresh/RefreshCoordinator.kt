package dev.macfi.luftung.refresh

import android.content.Context
import dev.macfi.luftung.data.IndoorClimateStore
import dev.macfi.luftung.data.ObservedWeatherProvider
import dev.macfi.luftung.data.OutdoorConditionsCache
import dev.macfi.luftung.data.OutdoorConditionsResult
import dev.macfi.luftung.data.OutdoorConditionsStore
import dev.macfi.luftung.data.OutdoorInputMode
import dev.macfi.luftung.data.VentilationPreferencesStore
import dev.macfi.luftung.domain.OutdoorConditions
import dev.macfi.luftung.domain.VentilationAdvisor
import dev.macfi.luftung.location.AndroidLocationProvider
import dev.macfi.luftung.widget.VentilationWidget
import dev.macfi.luftung.widget.WidgetDisplayState
import dev.macfi.luftung.widget.WidgetStateMapper
import dev.macfi.luftung.widget.WidgetStateStore
import dev.macfi.luftung.widget.initial
import androidx.glance.appwidget.updateAll

class RefreshCoordinator(
    private val context: Context,
    private val indoorStore: IndoorClimateStore = IndoorClimateStore(context),
    private val outdoorStore: OutdoorConditionsStore = OutdoorConditionsStore(context),
    private val preferencesStore: VentilationPreferencesStore = VentilationPreferencesStore(context),
    private val locationProvider: AndroidLocationProvider = AndroidLocationProvider(context),
    private val weatherProvider: ObservedWeatherProvider = ObservedWeatherProvider(),
    private val advisor: VentilationAdvisor = VentilationAdvisor(),
    private val widgetStateStore: WidgetStateStore = WidgetStateStore(context),
) {
    suspend fun refresh(force: Boolean = false): WidgetDisplayState {
        val indoor = indoorStore.read() ?: return saveAndUpdate(WidgetDisplayState.initial())
        val outdoorResult = resolveOutdoorConditions(force)
            ?: return saveAndUpdate(
                WidgetStateMapper.error(
                    title = "Outdoor data needed",
                    reason = "Choose manual, current location, or city weather.",
                ),
            )

        val mode = preferencesStore.readMode()
        val advice = advisor.assess(
            indoor = indoor,
            outdoor = outdoorResult.conditions,
            mode = mode,
            lastVentilatedAtMillis = preferencesStore.readLastVentilatedAtMillis(),
            nowMillis = System.currentTimeMillis(),
        )
        val stale = !OutdoorConditionsCache.isFresh(outdoorResult.conditions, System.currentTimeMillis())
        return saveAndUpdate(
            WidgetStateMapper.fromAdvice(
                indoor = indoor,
                outdoor = outdoorResult.conditions,
                advice = advice,
                stale = stale,
                warning = outdoorResult.warning,
            ),
        )
    }

    private suspend fun resolveOutdoorConditions(force: Boolean): OutdoorConditionsResult? {
        return when (outdoorStore.readMode()) {
            OutdoorInputMode.MANUAL -> outdoorStore.readManual()?.let {
                OutdoorConditionsResult(it, warning = null)
            }
            OutdoorInputMode.CURRENT_LOCATION -> resolveCurrentLocation(force)
            OutdoorInputMode.CITY_SEARCH -> resolveCity(force)
        }
    }

    private suspend fun resolveCurrentLocation(force: Boolean): OutdoorConditionsResult? {
        val cached = outdoorStore.readCached()
        if (!force && cached != null && OutdoorConditionsCache.isFresh(cached, System.currentTimeMillis())) {
            return OutdoorConditionsResult(cached, warning = null)
        }
        val coordinates = locationProvider.currentCoordinates() ?: return cached?.let(OutdoorConditionsCache::fallback)
        return fetchAndCache(cached) {
            weatherProvider.getCurrentWeather(coordinates.latitude, coordinates.longitude)
        }
    }

    private suspend fun resolveCity(force: Boolean): OutdoorConditionsResult? {
        val city = outdoorStore.readSelectedCity() ?: return null
        val cached = outdoorStore.readCached()
        if (!force && cached != null && OutdoorConditionsCache.isFresh(cached, System.currentTimeMillis())) {
            return OutdoorConditionsResult(cached, warning = null)
        }
        return fetchAndCache(cached) {
            weatherProvider.getCurrentWeatherForCity(city)
        }
    }

    private suspend fun fetchAndCache(
        cached: OutdoorConditions?,
        fetch: suspend () -> OutdoorConditions,
    ): OutdoorConditionsResult? {
        return runCatching {
            fetch().also(outdoorStore::saveCached)
        }.fold(
            onSuccess = { OutdoorConditionsResult(it, warning = null) },
            onFailure = { cached?.let(OutdoorConditionsCache::fallback) },
        )
    }

    private suspend fun saveAndUpdate(state: WidgetDisplayState): WidgetDisplayState {
        widgetStateStore.save(state)
        VentilationWidget().updateAll(context)
        return state
    }
}
