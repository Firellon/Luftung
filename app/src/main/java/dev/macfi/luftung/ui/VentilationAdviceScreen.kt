package dev.macfi.luftung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.macfi.luftung.data.IndoorClimateStore
import dev.macfi.luftung.data.ObservedWeatherProvider
import dev.macfi.luftung.data.OutdoorConditionsStore
import dev.macfi.luftung.data.OutdoorInputMode
import dev.macfi.luftung.data.VentilationPreferencesStore
import dev.macfi.luftung.domain.CitySearchResult
import dev.macfi.luftung.domain.ComfortProfile
import dev.macfi.luftung.domain.ComfortProfilePreset
import dev.macfi.luftung.domain.DewPointCalculator
import dev.macfi.luftung.domain.IndoorConditions
import dev.macfi.luftung.domain.WindowState
import dev.macfi.luftung.domain.oneDecimal
import dev.macfi.luftung.refresh.RefreshCoordinator
import dev.macfi.luftung.widget.WidgetDisplayState
import dev.macfi.luftung.widget.WidgetStateStore
import dev.macfi.luftung.widget.WidgetStatus
import kotlinx.coroutines.launch

@Composable
fun VentilationAdviceScreen(
    requestLocationPermission: () -> Unit,
) {
    val context = LocalContext.current
    val indoorStore = remember { IndoorClimateStore(context) }
    val outdoorStore = remember { OutdoorConditionsStore(context) }
    val preferencesStore = remember { VentilationPreferencesStore(context) }
    val weatherProvider = remember { ObservedWeatherProvider() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var indoorTempText by remember { mutableStateOf("") }
    var indoorHumidityText by remember { mutableStateOf("") }
    var outdoorTempText by remember { mutableStateOf("") }
    var outdoorHumidityText by remember { mutableStateOf("") }
    var outdoorMode by remember { mutableStateOf(outdoorStore.readMode()) }
    var windowState by remember { mutableStateOf(preferencesStore.readWindowState()) }
    var comfortProfile by remember { mutableStateOf(preferencesStore.readComfortProfile()) }
    var cityQuery by remember { mutableStateOf("") }
    var cityResults by remember { mutableStateOf(emptyList<CitySearchResult>()) }
    var message by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf(WidgetStateStore(context).read()) }

    LaunchedEffect(Unit) {
        indoorStore.read()?.let {
            indoorTempText = it.temperatureC.cleanNumber()
            indoorHumidityText = it.relativeHumidityPercent.cleanNumber()
        }
        outdoorStore.readManual()?.let {
            outdoorTempText = it.temperatureC.cleanNumber()
            outdoorHumidityText = it.relativeHumidityPercent.cleanNumber()
        }
        outdoorStore.readSelectedCity()?.let {
            cityQuery = listOfNotNull(it.name, it.countryCode).joinToString(", ")
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7FBF8),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Luftung",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF12372A),
            )
            Text(
                text = "Comfort-based ventilation advice from temperature and dew point.",
                color = Color(0xFF274D40),
            )

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Advice") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Settings") },
                )
            }

            if (selectedTab == 0) {
                AdviceTab(
                    indoorTempText = indoorTempText,
                    indoorHumidityText = indoorHumidityText,
                    onIndoorTempChange = { indoorTempText = it },
                    onIndoorHumidityChange = { indoorHumidityText = it },
                    windowState = windowState,
                    onWindowStateChange = {
                        windowState = it
                        preferencesStore.saveWindowState(it)
                    },
                    outdoorMode = outdoorMode,
                    onOutdoorModeChange = {
                        outdoorMode = it
                        outdoorStore.saveMode(it)
                    },
                    outdoorTempText = outdoorTempText,
                    outdoorHumidityText = outdoorHumidityText,
                    onOutdoorTempChange = { outdoorTempText = it },
                    onOutdoorHumidityChange = { outdoorHumidityText = it },
                    cityQuery = cityQuery,
                    onCityQueryChange = { cityQuery = it },
                    cityResults = cityResults,
                    requestLocationPermission = requestLocationPermission,
                    onSearchCity = {
                        scope.launch {
                            cityResults = weatherProvider.searchCities(cityQuery)
                            message = if (cityResults.isEmpty()) "No city suggestions found." else null
                        }
                    },
                    onSelectCity = { city ->
                        outdoorStore.saveSelectedCity(
                            name = city.name,
                            countryCode = city.countryCode,
                            latitude = city.latitude,
                            longitude = city.longitude,
                        )
                        outdoorStore.saveMode(OutdoorInputMode.CITY_SEARCH)
                        outdoorMode = OutdoorInputMode.CITY_SEARCH
                        cityQuery = city.displayName
                        cityResults = emptyList()
                    },
                    onSaveAndRefresh = {
                        val indoor = parseIndoor(indoorTempText, indoorHumidityText)
                        if (indoor == null) {
                            message = "Enter valid indoor temperature and humidity."
                            return@AdviceTab
                        }
                        indoorStore.save(indoor)

                        if (outdoorMode == OutdoorInputMode.MANUAL) {
                            val manualOutdoor = parseOutdoorManual(outdoorTempText, outdoorHumidityText)
                            if (manualOutdoor == null) {
                                message = "Enter valid manual outdoor temperature and humidity."
                                return@AdviceTab
                            }
                            outdoorStore.saveManual(
                                temperatureC = manualOutdoor.first,
                                relativeHumidity = manualOutdoor.second,
                            )
                        }

                        preferencesStore.saveWindowState(windowState)
                        preferencesStore.saveComfortProfile(comfortProfile)
                        scope.launch {
                            state = RefreshCoordinator(context).refresh(force = true)
                            message = "Updated recommendation."
                        }
                    },
                    onOpenedNow = {
                        preferencesStore.markVentilated()
                        windowState = WindowState.OPEN
                        message = "Marked windows as opened now."
                    },
                    message = message,
                    state = state,
                )
            } else {
                SettingsTab(
                    comfortProfile = comfortProfile,
                    onPresetSelected = { preset ->
                        comfortProfile = preset.defaultProfile()
                        preferencesStore.saveComfortProfile(comfortProfile)
                    },
                    onProfileChanged = {
                        comfortProfile = it
                        preferencesStore.saveComfortProfile(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun AdviceTab(
    indoorTempText: String,
    indoorHumidityText: String,
    onIndoorTempChange: (String) -> Unit,
    onIndoorHumidityChange: (String) -> Unit,
    windowState: WindowState,
    onWindowStateChange: (WindowState) -> Unit,
    outdoorMode: OutdoorInputMode,
    onOutdoorModeChange: (OutdoorInputMode) -> Unit,
    outdoorTempText: String,
    outdoorHumidityText: String,
    onOutdoorTempChange: (String) -> Unit,
    onOutdoorHumidityChange: (String) -> Unit,
    cityQuery: String,
    onCityQueryChange: (String) -> Unit,
    cityResults: List<CitySearchResult>,
    requestLocationPermission: () -> Unit,
    onSearchCity: () -> Unit,
    onSelectCity: (CitySearchResult) -> Unit,
    onSaveAndRefresh: () -> Unit,
    onOpenedNow: () -> Unit,
    message: String?,
    state: WidgetDisplayState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IndoorConditionsCard(
            temperatureText = indoorTempText,
            humidityText = indoorHumidityText,
            onTemperatureChange = onIndoorTempChange,
            onHumidityChange = onIndoorHumidityChange,
        )

        WindowStateCard(
            selected = windowState,
            onSelected = onWindowStateChange,
        )

        OutdoorConditionsCard(
            mode = outdoorMode,
            onModeChange = onOutdoorModeChange,
            outdoorTempText = outdoorTempText,
            outdoorHumidityText = outdoorHumidityText,
            onOutdoorTempChange = onOutdoorTempChange,
            onOutdoorHumidityChange = onOutdoorHumidityChange,
            cityQuery = cityQuery,
            onCityQueryChange = onCityQueryChange,
            cityResults = cityResults,
            requestLocationPermission = requestLocationPermission,
            onSearchCity = onSearchCity,
            onSelectCity = onSelectCity,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSaveAndRefresh) {
                Text("Save and refresh")
            }

            OutlinedButton(onClick = onOpenedNow) {
                Text("Opened now")
            }
        }

        message?.let {
            Text(text = it, color = Color(0xFF274D40))
        }

        RecommendationCard(state)
    }
}

@Composable
private fun SettingsTab(
    comfortProfile: ComfortProfile,
    onPresetSelected: (ComfortProfilePreset) -> Unit,
    onProfileChanged: (ComfortProfile) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Comfort Priorities", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Relative humidity is shown because it is familiar, but the comfort model scores humidity through dew point.",
            color = Color(0xFF48685C),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComfortProfilePreset.entries.forEach { preset ->
                ModeButton(
                    label = preset.label,
                    selected = comfortProfile.preset == preset,
                    onClick = { onPresetSelected(preset) },
                )
            }
        }

        PrioritySlider(
            label = "Comfort strictness",
            value = comfortProfile.comfortStrictness,
            onValueChange = {
                onProfileChanged(comfortProfile.copy(comfortStrictness = it))
            },
        )
        PrioritySlider(
            label = "Temperature priority",
            value = comfortProfile.temperaturePriority,
            onValueChange = {
                onProfileChanged(comfortProfile.copy(temperaturePriority = it))
            },
        )
        PrioritySlider(
            label = "Dew point priority",
            value = comfortProfile.dewPointPriority,
            onValueChange = {
                onProfileChanged(comfortProfile.copy(dewPointPriority = it))
            },
        )
    }
}

@Composable
private fun PrioritySlider(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: ${value.oneDecimal()}x")
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = 0.5f..1.8f,
        )
    }
}

@Composable
private fun IndoorConditionsCard(
    temperatureText: String,
    humidityText: String,
    onTemperatureChange: (String) -> Unit,
    onHumidityChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Indoor Conditions", style = MaterialTheme.typography.titleMedium)
        DecimalField("Indoor temperature (C)", temperatureText, onTemperatureChange)
        DecimalField("Indoor humidity (%)", humidityText, onHumidityChange)
    }
}

@Composable
private fun WindowStateCard(
    selected: WindowState,
    onSelected: (WindowState) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Window State", style = MaterialTheme.typography.titleMedium)
        WindowState.entries.chunked(2).forEach { rowStates ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowStates.forEach { state ->
                    ModeButton(
                        label = state.label,
                        selected = selected == state,
                        onClick = { onSelected(state) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OutdoorConditionsCard(
    mode: OutdoorInputMode,
    onModeChange: (OutdoorInputMode) -> Unit,
    outdoorTempText: String,
    outdoorHumidityText: String,
    onOutdoorTempChange: (String) -> Unit,
    onOutdoorHumidityChange: (String) -> Unit,
    cityQuery: String,
    onCityQueryChange: (String) -> Unit,
    cityResults: List<CitySearchResult>,
    requestLocationPermission: () -> Unit,
    onSearchCity: () -> Unit,
    onSelectCity: (CitySearchResult) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Outdoor Conditions", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeButton("Manual", mode == OutdoorInputMode.MANUAL) { onModeChange(OutdoorInputMode.MANUAL) }
            ModeButton("Current", mode == OutdoorInputMode.CURRENT_LOCATION) {
                onModeChange(OutdoorInputMode.CURRENT_LOCATION)
                requestLocationPermission()
            }
            ModeButton("City", mode == OutdoorInputMode.CITY_SEARCH) { onModeChange(OutdoorInputMode.CITY_SEARCH) }
        }

        when (mode) {
            OutdoorInputMode.MANUAL -> {
                DecimalField("Outdoor temperature (C)", outdoorTempText, onOutdoorTempChange)
                DecimalField("Outdoor humidity (%)", outdoorHumidityText, onOutdoorHumidityChange)
            }
            OutdoorInputMode.CURRENT_LOCATION -> {
                Text("Uses approximate location only. Tap Save and refresh after granting permission.")
            }
            OutdoorInputMode.CITY_SEARCH -> {
                OutlinedTextField(
                    value = cityQuery,
                    onValueChange = onCityQueryChange,
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = onSearchCity) {
                    Text("Search city")
                }
                cityResults.forEach { city ->
                    OutlinedButton(
                        onClick = { onSelectCity(city) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(city.displayName)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(state: WidgetDisplayState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(state.panelColor())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF12372A),
        )
        Text(text = state.indoorLine, color = Color(0xFF274D40))
        Text(text = state.outdoorLine, color = Color(0xFF274D40))
        HorizontalDivider()
        Text(text = state.reason, color = Color(0xFF274D40))
        Text(text = "Updated: ${state.lastUpdated}", color = Color(0xFF48685C))
    }
}

@Composable
private fun DecimalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(label)
        }
    }
}

private fun parseIndoor(
    temperatureText: String,
    humidityText: String,
): IndoorConditions? {
    val indoor = IndoorConditions(
        temperatureC = temperatureText.toDoubleInput() ?: return null,
        relativeHumidityPercent = humidityText.toDoubleInput() ?: return null,
    )
    return runCatching {
        DewPointCalculator.validate(indoor.temperatureC, indoor.relativeHumidityPercent)
        indoor
    }.getOrNull()
}

private fun parseOutdoorManual(
    temperatureText: String,
    humidityText: String,
): Pair<Double, Double>? {
    val temp = temperatureText.toDoubleInput() ?: return null
    val rh = humidityText.toDoubleInput() ?: return null
    return runCatching {
        DewPointCalculator.validate(temp, rh)
        temp to rh
    }.getOrNull()
}

private fun WidgetDisplayState.panelColor(): Color {
    return when (status) {
        WidgetStatus.HELPFUL -> Color(0xFFE8F5E9)
        WidgetStatus.MIXED -> Color(0xFFFFF8E1)
        WidgetStatus.NOT_HELPFUL -> Color(0xFFFFEBEE)
        WidgetStatus.COLD_OR_DRY -> Color(0xFFE3F2FD)
        WidgetStatus.NEEDS_INPUT -> Color(0xFFE3F2FD)
        WidgetStatus.ERROR -> Color(0xFFF2F2F2)
    }
}

private fun String.toDoubleInput(): Double? {
    return replace(',', '.').toDoubleOrNull()
}

private fun Double.cleanNumber(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        oneDecimal()
    }
}

private fun ComfortProfilePreset.defaultProfile(): ComfortProfile {
    return when (this) {
        ComfortProfilePreset.HUMIDITY_SENSITIVE -> ComfortProfile.humiditySensitive()
        ComfortProfilePreset.BALANCED -> ComfortProfile.balanced()
        ComfortProfilePreset.COOLING_FOCUSED -> ComfortProfile.coolingFocused()
    }
}
