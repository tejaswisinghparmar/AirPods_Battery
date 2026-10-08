# AirPods Battery

A small Android app that reads **AirPods 3** left, right, and case battery levels from Apple's Bluetooth LE proximity advertisements.

## Features

- AirPods 3 detection
- Left battery percentage
- Right battery percentage
- Case battery percentage
- Charging flags
- No internet permission
- Jetpack Compose UI
- GitHub Actions APK build

## How it works

AirPods periodically broadcast Apple manufacturer-specific BLE advertisements.

The app:

1. Scans Bluetooth LE advertisements.
2. Filters Apple manufacturer data (`0x004C`).
3. Checks the AirPods 3 model identifier (`0x1320`).
4. Decodes the battery nibbles.
5. Maps the two pod values to left/right using the protocol's flip bit.
6. Displays the result.

Battery values in the BLE advertisement are quantized. The protocol represents 5%, 15%, 25%, ..., 95%, and 100% rather than arbitrary 1% values.

## Android permissions

Android 12+:
- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`

Android 11 and below:
- `ACCESS_FINE_LOCATION` for BLE scanning.

The Android 12+ Bluetooth scan permission is declared with `neverForLocation`.

## Run locally in VS Code

Install:

- JDK 17
- Android SDK
- Android SDK Platform 36
- Android build tools 36.0.0
- Gradle 8.13

Then:

```bash
gradle assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install with ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

Push this repository to GitHub.

The workflow in:

```text
.github/workflows/build-apk.yml
```

automatically builds the debug APK.

After the workflow finishes:

**GitHub → Actions → Build APK → Artifacts → AirPodsBattery-debug**

Download the artifact and install the APK on your Android phone.

## Important testing note

For the first test, pair the AirPods normally through Android Bluetooth settings.

Then open the case / take the AirPods out and press **Scan for AirPods 3**.

BLE advertisements can take a few seconds to appear. The case battery may not be available in every AirPods state.

## Current scope

This project intentionally does NOT implement:

- Spatial audio
- ANC controls
- Automatic device switching
- Siri
- Find My
- AirPods configuration

Those can be separate projects/features later.
