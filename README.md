# Luftung

Luftung is an Android app and widget for one practical question:

> Should I open my windows right now, and for how long?

The app is not meant to be a weather display. Weather is an input. The goal is to compare indoor and outdoor conditions, predict how ventilation changes the room, and recommend one clear action.

## Current Product Direction

Luftung uses:

- indoor temperature and relative humidity entered manually
- outdoor temperature, humidity, and dew point from manual input, current location, or city search
- observed DWD/Bright Sky weather where available, with Open-Meteo as fallback and geocoding provider
- current window state: closed, tilted, open, or cross ventilation
- comfort priorities selected from presets or adjusted in Settings

The app should hide meteorological complexity from the user. It should show the important numbers, explain the recommendation, and avoid asking the user to choose implementation details such as "brief" or "full" ventilation.

## Scientific Model

Luftung calculates dew point from temperature and relative humidity using the Magnus formula. Relative humidity is still displayed because people expect to see it, but humidity comfort is scored through dew point.

The comfort score combines:

- temperature penalty
- dew point penalty
- a user comfort profile

Lower scores are better. The recommendation engine predicts future indoor temperature and dew point, scores candidate actions, and chooses the lowest-score result.

Candidate actions include:

- keep windows closed
- open windows for a short duration
- keep already-open windows open longer
- close windows now

Fresh air has value on its own. If windows have stayed closed for several hours, Luftung may recommend a short airing even when the comfort score changes only slightly, unless outdoor air is much hotter and more humid.

## Comfort Priorities

Settings include three presets:

- `Balanced`: the default scientific baseline
- `Humidity Sensitive`: increases the importance of dew point and sticky air
- `Cooling Focused`: increases the importance of temperature reduction

Each preset can be adjusted with sliders:

- comfort strictness
- temperature priority
- dew point priority

The first settings version intentionally uses sliders instead of full threshold editing. Fine-grained threshold editing can come later if the presets and sliders are not expressive enough.

## App UI

The app has two tabs:

- `Advice`: indoor inputs, outdoor source, window state, refresh controls, and the primary recommendation
- `Settings`: comfort profile preset and priority sliders

The recommendation card should display exactly one primary action, such as:

- Open windows 10 min
- Keep windows open 15 min
- Close windows now
- Keep closed

The explanation should mention the relevant temperature and dew point trade-off.

## Widget

The widget summarizes the same recommendation as the app:

- recommendation and duration
- indoor temperature and dew point
- outdoor temperature and dew point
- one short reason

If outdoor data is stale, the widget tells the user to tap to refresh.

## Roadmap

1. Improve the candidate-action model with more realistic ventilation duration tuning.
2. Refine window state tracking and elapsed-open behavior.
3. Add richer settings for comfort profiles if the sliders are not enough.
4. Improve widget density and status colors after the recommendation model settles.
5. Add indoor sensor support, such as Bluetooth hygrometers.

## Build

Open this folder in Android Studio. The project expects:

- JDK 17
- Android SDK with API 37
- Android Gradle Plugin 9.2.0
- Gradle 9.4.1 or the checked-in Gradle wrapper

Useful commands:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

To run on a device, enable Developer Options and USB debugging, connect the phone, then use Android Studio or:

```powershell
.\gradlew.bat installDebug
```
