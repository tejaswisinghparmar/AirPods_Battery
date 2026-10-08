# AirPods Battery V2

Android AirPods 3 battery detector with a BLE diagnostic scanner.

## V2 changes

V1 used a restrictive BLE manufacturer filter and silently ignored packets that did not match.

V2 scans all BLE advertisements and displays every Apple manufacturer packet received by Android. This lets us inspect the exact AirPods advertisement before changing the parser.

The app identifies:

- Apple manufacturer ID `0x004C`
- AirPods 3 model identifier `0x1320`
- raw manufacturer bytes
- RSSI
- packet length
- decoded left/right/case battery when the expected format is present

## Test

1. Pair AirPods 3 with Android normally.
2. Open the AirPods case / take the earbuds out.
3. Open this app.
4. Grant Bluetooth permission.
5. Tap **Scan for AirPods 3**.
6. Wait 10–20 seconds.
7. If an Apple packet appears, send a screenshot showing the packet data.

The raw packet is intentionally shown because it is the evidence needed to adjust the parser for the exact AirPods advertisement format received by your phone.

## Build

```bash
gradle assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions automatically builds the debug APK on pushes to `main`.
