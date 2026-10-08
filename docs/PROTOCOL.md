# AirPods 3 battery protocol

This project uses the Apple BLE proximity-pairing advertisement.

- Apple company ID: `0x004C`
- Proximity message type: `0x07`
- AirPods 3 model: `0x1320`
- Battery information is carried in the compact nibble fields of the advertisement.
- Battery values are quantized to 5% steps up to 95%, plus 100%.
- The left/right assignment uses the protocol status/flip bit.

The parser is intentionally isolated in `AirPodsParser.kt` so protocol changes can be handled without changing the UI or Bluetooth scanning code.

This is a community reverse-engineering effort. Apple does not publish this AirPods battery advertisement as a public Android API.
