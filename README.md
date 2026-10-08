# AirPods Battery V3

Android AirPods 3 battery detector using Bluetooth Low Energy advertisements.

## V3 fixes

V3 fixes the AirPods 3 packet parsing bug found in V2. The observed 27-byte Apple packet was:

```text
07 19 01 13 20 2B 99 8F 02 00 04 76 25 DF 99 75 55 37 C9 18 76 21 FD 65 81 07 2D
```

For this format:

- `07` = AirPods advertisement type
- `13 20` = AirPods 3 model identifier `0x1320` (big-endian)
- `99` = both earbud battery nibbles are 9 → 95%
- `8F` = case battery nibble `F` → unavailable in this advertisement
- status byte `2B` contains the flip flag used to map the two earbud nibbles to left/right
- charging flags use bit 0 = right, bit 1 = left, bit 2 = case

### Important V2 bug

V2 interpreted `13 20` as little-endian and calculated `0x2013`, so it rejected a valid AirPods 3 packet. V3 reads the model as `(b[3] shl 8) or b[4]`.

V3 also corrects the left/right charging flag mapping.

## Test

1. Pair AirPods 3 with Android normally.
2. Open the AirPods case / take the earbuds out.
3. Open this app.
4. Grant Bluetooth permission.
5. Tap **Scan for AirPods 3**.
6. Wait a few seconds.

When the AirPods advertisement is received, the decoded battery should appear at the top of the screen.

The diagnostic packet list remains available so parser behavior can be verified if a phone/ROM sends a different advertisement layout.

## Build

```bash
gradle assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions builds the debug APK automatically on pushes to `main`.
