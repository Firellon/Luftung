# Luftung

Luftung is a small Android widget that compares indoor and outdoor moisture conditions and recommends whether ventilating helps dry a room.

The first version uses manual indoor temperature/humidity input, phone location for outdoor weather, and Open-Meteo current weather data. Recommendations are based on absolute humidity differences, with dew point shown as supporting context.

## Build

Open this folder in Android Studio. The project expects:

- JDK 17
- Android SDK with API 37
- Android Gradle Plugin 9.2.0
- Gradle 9.4.1 or a compatible Android Studio-managed Gradle runtime

This repository intentionally does not include a Gradle wrapper jar yet. Generate one from Android Studio or a local Gradle install when the Android toolchain is available.
