package com.tejaswi.airpodsbattery

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tejaswi.airpodsbattery.bluetooth.BluetoothAirPodsScanner
import com.tejaswi.airpodsbattery.model.AirPodsBattery

class MainActivity : ComponentActivity() {

    private lateinit var scanner: BluetoothAirPodsScanner

    private var battery by mutableStateOf<AirPodsBattery?>(null)
    private var status by mutableStateOf("Ready to scan")
    private var scanning by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.all { it }
            if (granted) {
                startAirPodsScan()
            } else {
                status = "Bluetooth permission is required."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scanner = BluetoothAirPodsScanner(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BatteryScreen(
                        battery = battery,
                        status = status,
                        scanning = scanning,
                        onScan = { requestPermissionsAndScan() },
                        onStop = {
                            scanner.stopScan()
                            scanning = false
                            status = "Scan stopped"
                        }
                    )
                }
            }
        }
    }

    private fun requestPermissionsAndScan() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startAirPodsScan()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startAirPodsScan() {
        if (!scanner.hasBluetooth()) {
            status = "Turn Bluetooth on first."
            return
        }

        if (!scanner.hasPermissions()) {
            status = "Bluetooth permission is required."
            return
        }

        scanning = true
        status = "Scanning for AirPods 3..."

        scanner.startScan(
            onBattery = {
                runOnUiThread {
                    battery = it
                    status = "AirPods 3 detected"
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

@androidx.compose.runtime.Composable
private fun BatteryScreen(
    battery: AirPodsBattery?,
    status: String,
    scanning: Boolean,
    onScan: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AirPods Battery",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "AirPods 3",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = if (battery != null) "Connected / detected" else status,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(18.dp))

                BatteryRow("Left", battery?.left, battery?.leftCharging == true)
                Spacer(Modifier.height(16.dp))
                BatteryRow("Right", battery?.right, battery?.rightCharging == true)
                Spacer(Modifier.height(16.dp))
                BatteryRow("Case", battery?.case, battery?.caseCharging == true)
            }
        }

        Spacer(Modifier.height(24.dp))

        if (scanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop scanning")
            }
        } else {
            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan for AirPods 3")
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Keep the AirPods out of the case or open the lid while scanning. Battery advertisements are sent over Bluetooth LE.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@androidx.compose.runtime.Composable
private fun BatteryRow(
    label: String,
    value: Int?,
    charging: Boolean
) {
    val display = value?.let { "$it%" } ?: "—"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (charging) {
                Text(
                    text = "Charging",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(
            text = display,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
