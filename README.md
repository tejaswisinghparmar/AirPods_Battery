# AirPods Battery — V5

A dark-mode Android companion for AirPods 3. V5 turns the proven BLE battery MVP into a real app surface with a home-screen widget, persistent settings, background monitoring, experimental in-ear detection, and optional automatic media pause.

## V5 features

- AirPods 3 BLE battery detection
- Left / right / case battery
- Charging state
- Dark-first UI
- AirPods-style launcher icon
- Experimental left/right in-ear state
- Home-screen widget using Jetpack Glance
- Background BLE monitoring through a foreground service
- **Auto-pause toggle in Settings**
- Auto-pause only when the app has confirmed an ear was previously in-ear and then detects removal
- Notification Access integration for media-session pause commands
- Raw BLE diagnostics retained for troubleshooting

## Auto-pause

Go to:

**Settings → Playback → Auto-pause when removed**

When enabled, the app needs Android Notification Access so it can control the active media session. It then monitors AirPods state in the background and requests pause when an AirPod is removed after both earbuds were previously detected in-ear.

The feature is intentionally conservative and experimental because AirPods ear-detection information is reverse-engineered rather than provided by a public Apple Android API.

## Background monitoring

**Settings → Background monitoring** keeps the BLE monitor running while the app is not on screen. Android shows an ongoing low-priority monitoring notification while the service is active.

## Widget

Add **AirPods Battery** from your Android launcher's widget picker. The widget shows the last decoded left, right, and case battery state and opens the app when tapped.

Widget updates are pushed when the app/service receives a fresh AirPods battery packet. Android may also refresh widgets periodically according to its normal widget scheduling rules.

## V5 architecture

```text
AirPods BLE advertisements
          ↓
     AirPodsParser
          ↓
      AirPodsState
          ↓
   ┌──────┼────────┐
   ↓      ↓        ↓
  App   Widget   Background
                    ↓
              Ear detection
                    ↓
              Auto-pause
                    ↓
             Media session
```

## Important limitation

Battery decoding is the stable part of the project. Ear detection and automatic media control are experimental and can vary by AirPods firmware, Android version, OEM Bluetooth stack, background restrictions, and the media app's media-session implementation.
