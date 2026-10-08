package com.tejaswi.airpodsbattery

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tejaswi.airpodsbattery.bluetooth.AirPodsParser
import com.tejaswi.airpodsbattery.bluetooth.BluetoothAirPodsScanner
import com.tejaswi.airpodsbattery.model.AirPodsBattery
import com.tejaswi.airpodsbattery.model.BluetoothPacket

class MainActivity : ComponentActivity() {

    private lateinit var scanner: BluetoothAirPodsScanner

    private var battery by mutableStateOf<AirPodsBattery?>(null)
    private var packets by mutableStateOf(listOf<BluetoothPacket>())
    private var status by mutableStateOf("Ready to scan")
    private var scanning by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (scanner.hasPermissions()) startScan()
            else status = "Bluetooth permission is required."
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanner = BluetoothAirPodsScanner(this)

        setContent {
            MaterialTheme {
                BatteryScreen(
                    battery = battery,
                    packets = packets,
                    status = status,
                    scanning = scanning,
                    onScan = ::requestPermissionsAndScan,
                    onStop = {
                        scanner.stopScan()
                        scanning = false
                        status = "Scan stopped"
                    }
                )
            }
        }
    }

    private fun requestPermissionsAndScan() {
        val permissions = if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) startScan()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startScan() {
        if (!scanner.hasBluetooth()) {
            status = "Turn Bluetooth on first."
            return
        }

        if (!scanner.hasPermissions()) {
            status = "Bluetooth permission is required."
            return
        }

        packets = emptyList()
        battery = null
        scanning = true
        status = "Scanning all BLE advertisements..."

        scanner.startScan(
            onPacket = { packet ->
                runOnUiThread {
                    val existing = packets
                        .filterNot { it.address == packet.address && it.appleData == packet.appleData }
                        .take(19)

                    packets = listOf(packet) + existing

                    status = if (packet.isAirPods3) {
                        "AirPods 3 packet detected"
                    } else {
                        "Apple BLE packet detected"
                    }
                }
            },
            onBattery = {
                runOnUiThread {
                    battery = it
                    status = "AirPods 3 battery detected"
                }
            },
            onError = {
                runOnUiThread {
                    scanning = false
                    status = it
                }
            }
        )
    }

    override fun onDestroy() {
        scanner.stopScan()
        super.onDestroy()
    }
}

@Composable
private fun BatteryScreen(
    battery: AirPodsBattery?,
    packets: List<BluetoothPacket>,
    status: String,
    scanning: Boolean,
    onScan: () -> Unit,
    onStop: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "AirPods Battery",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "V3 • AirPods 3 BLE battery",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(status, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))

                    if (battery == null) {
                        Text("Open the case or remove the earbuds, then scan.")
                    } else {
                        BatteryRow("Left", battery.left, battery.leftCharging)
                        BatteryRow("Right", battery.right, battery.rightCharging)
                        BatteryRow("Case", battery.case, battery.caseCharging)
                    }
                }
            }
        }

        item {
            Button(
                onClick = if (scanning) onStop else onScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (scanning) "Stop scan" else "Scan for AirPods 3")
            }
        }

        item {
            Text(
                "Apple BLE packets seen: ${packets.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(packets) { packet ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        if (packet.isAirPods3) "✓ AirPods 3 candidate" else "Apple BLE packet",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Name: ${packet.name}")
                    Text("RSSI: ${packet.rssi} dBm")
                    Text("Length: ${packet.manufacturerLength} bytes")
                    Text("Data: ${packet.appleData ?: "—"}")
                }
            }
        }

        item {
            Text(
                "V3 keeps BLE diagnostics visible so packet parsing can be verified on different Android phones and ROMs.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BatteryRow(label: String, value: Int?, charging: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontWeight = FontWeight.Medium)
            if (charging) Text("Charging", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            value?.let { "$it%" } ?: "—",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
