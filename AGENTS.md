# AGENTS.md

## Collaboration

- Default to coaching before solving when the user is learning Android/Kotlin.
- Take over implementation when the user explicitly says "do it", "go ahead", or "PLEASE IMPLEMENT THIS PLAN".
- Keep changes small and grounded in the current Android/Kotlin/Compose structure.

## Android Migration Safety

- Treat anything stored in `SharedPreferences`, widget state, or app caches as public persisted data.
- Do not assume persisted enum names are safe to rename or remove. Android upgrades keep old values on the device.
- Avoid raw `Enum.valueOf(stored)` for persisted data unless it is wrapped with `runCatching` and a safe fallback.
- When replacing enum values, either:
  - keep legacy aliases and map them to the new model, or
  - add a migration/clear path for stale preferences and widget state.
- Widget state is especially sensitive because it may be read on app startup or by Glance before the user refreshes anything.
- Add regression coverage for preference/widget parsing when changing persisted model names.

## Verification

- For app behavior changes, run:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

- If a device build crashes, capture `adb logcat` and read the `FATAL EXCEPTION` / `Caused by` section before proposing fixes.
