package com.tejaswi.airpodsbattery

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tejaswi.airpodsbattery.bluetooth.AirPodsParser
import com.tejaswi.airpodsbattery.bluetooth.BluetoothAirPodsScanner
import com.tejaswi.airpodsbattery.media.MediaControlNotificationListenerService
import com.tejaswi.airpodsbattery.model.AirPodsBattery
import com.tejaswi.airpodsbattery.model.BluetoothPacket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AppBackground = Color(0xFF0B0B0F)
private val AppSurface = Color(0xFF15151B)
private val AppSurface2 = Color(0xFF1D1D24)
private val AppText = Color(0xFFF5F5F7)
private val AppSecondary = Color(0xFFA7A7B2)
private val AppAccent = Color(0xFFA78BFA)
private val AppSuccess = Color(0xFF72D98B)

class MainActivity : ComponentActivity() {
    private lateinit var scanner: BluetoothAirPodsScanner
    private var packets by mutableStateOf(listOf<BluetoothPacket>())
    private var status by mutableStateOf("Ready")
    private var scanning by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (scanner.hasPermissions()) startForegroundScan()
            else status = "Bluetooth permission is required."
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanner = BluetoothAirPodsScanner(this)

        setContent {
            AirPodsTheme {
                AirPodsApp(
                    activity = this,
                    packets = packets,
                    status = status,
                    scanning = scanning,
                    onStartScan = ::requestPermissionsAndScan,
                    onStopScan = ::stopForegroundScan
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::scanner.isInitialized && AppPrefs.autoPause(this) && !MediaControlNotificationListenerService.isConnected()) {
            // The settings screen explains how to enable access; do not force a prompt here.
        }
    }

    private fun requestPermissionsAndScan() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= 33) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startForegroundScan()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startForegroundScan() {
        if (!scanner.hasBluetooth()) {
            status = "Turn Bluetooth on first."
            return
        }
        if (!scanner.hasPermissions()) {
            status = "Bluetooth permission is required."
            return
        }

        packets = emptyList()
        scanning = true
        status = "Scanning for AirPods 3..."
        scanner.startScan(
            onPacket = { packet ->
                runOnUiThread {
                    packets = listOf(packet) + packets
                        .filterNot { it.address == packet.address && it.appleData == packet.appleData }
                        .take(19)
                    if (packet.isAirPods3) status = "AirPods 3 detected"
                }
            },
            onBattery = { battery ->
                runOnUiThread {
                    val stable = AirPodsStateBus.publish(battery)
                    AppPrefs.saveBattery(this, stable)
                    com.tejaswi.airpodsbattery.widget.AirPodsWidget.refresh(this)
                    status = "AirPods 3 detected"
                }
            },
            onError = { error ->
                runOnUiThread {
                    scanning = false
                    status = error
                }
            }
        )
    }

    private fun stopForegroundScan() {
        scanner.stopScan()
        scanning = false
        status = "Monitoring paused"
    }

    override fun onDestroy() {
        scanner.stopScan()
        super.onDestroy()
    }
}

@Composable
private fun AirPodsApp(
    activity: MainActivity,
    packets: List<BluetoothPacket>,
    status: String,
    scanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    var settings by remember { mutableStateOf(false) }
    val battery by AirPodsStateBus.battery.collectAsState()
    val savedBattery = remember { mutableStateOf(AppPrefs.lastBattery(activity)) }

    LaunchedEffect(battery) {
        if (battery != null) savedBattery.value = battery
    }

    if (settings) {
        SettingsScreen(
            activity = activity,
            packets = packets,
            onBack = { settings = false }
        )
    } else {
        HomeScreen(
            activity = activity,
            battery = battery ?: savedBattery.value,
            status = status,
            scanning = scanning,
            onSettings = { settings = true },
            onScan = onStartScan,
            onStop = onStopScan
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    activity: MainActivity,
    battery: AirPodsBattery?,
    status: String,
    scanning: Boolean,
    onSettings: () -> Unit,
    onScan: () -> Unit,
    onStop: () -> Unit
) {
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AirPods", color = AppText, fontWeight = FontWeight.Bold)
                        Text("AirPods 3", color = AppSecondary, fontSize = 12.sp)
                    }
                },
                actions = {
                    Text(
                        "⚙",
                        color = AppText,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .padding(end = 18.dp)
                            .clickable(onClick = onSettings)
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                ConnectionPill(status = status, battery = battery, scanning = scanning)
            }

            item {
                BatteryHero(battery)
            }

            item {
                EarStateCard(battery)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BatteryTile("LEFT", battery?.left, battery?.leftCharging, Modifier.weight(1f))
                    BatteryTile("RIGHT", battery?.right, battery?.rightCharging, Modifier.weight(1f))
                }
            }

            item {
                BatteryTile("CASE", battery?.case, battery?.caseCharging, Modifier.fillMaxWidth())
            }

            item {
                Surface(
                    color = AppSurface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Live monitoring", color = AppText, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (scanning) "BLE scan active" else "Tap to scan for AirPods",
                                    color = AppSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                if (scanning) "STOP" else "SCAN",
                                color = AppAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { if (scanning) onStop() else onScan() }
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun ConnectionPill(status: String, battery: AirPodsBattery?, scanning: Boolean) {
    val connected = battery != null
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AppSurface)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) AppSuccess else AppAccent))
        Spacer(Modifier.width(8.dp))
        Text(
            when {
                connected -> "Connected"
                scanning -> "Searching..."
                else -> status
            },
            color = AppText,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun BatteryHero(battery: AirPodsBattery?) {
    val average = if (battery?.left != null && battery.right != null) {
        (battery.left + battery.right) / 2
    } else battery?.left ?: battery?.right

    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AIRPODS", color = AppSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                average?.let { "$it%" } ?: "—",
                color = AppText,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold
            )
            Text("earbuds", color = AppSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BatteryTile(label: String, value: Int?, charging: Boolean?, modifier: Modifier) {
    Surface(color = AppSurface2, shape = RoundedCornerShape(22.dp), modifier = modifier) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = AppSecondary, fontSize = 11.sp, letterSpacing = 1.5.sp)
                if (charging == true) Text("⚡", color = AppAccent, fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(value?.let { "$it%" } ?: "—", color = AppText, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(
                when {
                    charging == true -> "Charging"
                    value == null -> "Unavailable"
                    else -> "Battery"
                },
                color = AppSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EarStateCard(battery: AirPodsBattery?) {
    Surface(color = AppSurface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ear detection", color = AppText, fontWeight = FontWeight.SemiBold)
                Text("EXPERIMENTAL", color = AppAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                EarState("Left", battery?.leftInEar)
                EarState("Right", battery?.rightInEar)
            }
        }
    }
}

@Composable
private fun EarState(label: String, inEar: Boolean?) {
    Column {
        Text(label, color = AppSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            when (inEar) {
                true -> "● In ear"
                false -> "○ Removed"
                null -> "— Unknown"
            },
            color = if (inEar == true) AppSuccess else AppText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DiagnosticPacket(packet: BluetoothPacket) {
    Surface(color = AppSurface, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                if (packet.isAirPods3) "✓ AirPods 3 packet" else "Apple BLE packet",
                color = AppText,
                fontWeight = FontWeight.SemiBold
            )
            Text("RSSI ${packet.rssi} dBm  •  ${packet.manufacturerLength} bytes", color = AppSecondary, fontSize = 12.sp)
            Text(packet.appleData ?: "—", color = AppSecondary, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(activity: MainActivity, packets: List<BluetoothPacket>, onBack: () -> Unit) {
    var autoPause by remember { mutableStateOf(AppPrefs.autoPause(activity)) }
    var autoPlay by remember { mutableStateOf(AppPrefs.autoPlay(activity)) }
    var backgroundMonitoring by remember { mutableStateOf(AppPrefs.backgroundMonitoring(activity)) }
    var showAccessDialog by remember { mutableStateOf(false) }


    if (showAccessDialog) {
        AlertDialog(
            onDismissRequest = { showAccessDialog = false },
            title = { Text("Allow media control?") },
            text = {
                Text("Playback controls need Notification Access so the app can control the active media session when an AirPod is removed or inserted.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showAccessDialog = false
                    activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }) { Text("Open settings") }
            },
            dismissButton = { TextButton(onClick = { showAccessDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = AppText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Text("‹", color = AppText, fontSize = 36.sp, modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .clickable(onClick = onBack))
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsHeader("PLAYBACK")
                SettingSwitchRow(
                    title = "Auto-pause when removed",
                    subtitle = "Pause active media when an AirPod leaves your ear.",
                    checked = autoPause,
                    onCheckedChange = { enabled ->
                        if (enabled && !notificationAccessEnabled(activity)) {
                            showAccessDialog = true
                        } else {
                            autoPause = enabled
                            AppPrefs.setAutoPause(activity, enabled)
                            if (enabled) MonitorController.start(activity) else if (!backgroundMonitoring) MonitorController.stop(activity)
                        }
                    }
                )
                if (autoPause && !notificationAccessEnabled(activity)) {
                    TextButton(onClick = { activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                        Text("Enable media access")
                    }
                }
            }

            item {
                SettingSwitchRow(
                    title = "Auto-play when inserted",
                    subtitle = "Resume media when an AirPod is placed back in your ear.",
                    checked = autoPlay,
                    onCheckedChange = { enabled ->
                        if (enabled && !notificationAccessEnabled(activity)) {
                            showAccessDialog = true
                        } else {
                            autoPlay = enabled
                            AppPrefs.setAutoPlay(activity, enabled)
                            if (enabled || autoPause) MonitorController.start(activity) else if (!backgroundMonitoring) MonitorController.stop(activity)
                        }
                    }
                )
            }

            item {
                SettingsHeader("BACKGROUND")
                SettingSwitchRow(
                    title = "Background monitoring",
                    subtitle = "Keep scanning for battery and ear state while the app is closed.",
                    checked = backgroundMonitoring,
                    onCheckedChange = { enabled ->
                        backgroundMonitoring = enabled
                        AppPrefs.setBackgroundMonitoring(activity, enabled)
                        if (enabled) MonitorController.start(activity) else if (!autoPause && !autoPlay) MonitorController.stop(activity)
                    }
                )
            }

            item {
                SettingsHeader("WIDGET")
                Surface(color = AppSurface, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Home-screen widget", color = AppText, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))
                        Text("Add the AirPods widget from your launcher's widget picker.", color = AppSecondary, fontSize = 13.sp)
                    }
                }
            }

            item {
                SettingsHeader("DIAGNOSTICS")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (packets.isEmpty()) {
                        Text("No BLE packets captured in this session.", color = AppSecondary, fontSize = 12.sp)
                    } else {
                        packets.forEach { DiagnosticPacket(it) }
                    }
                }
            }

            item {
                SettingsHeader("ABOUT")
                Surface(color = AppSurface, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("AirPods Battery", color = AppText, fontWeight = FontWeight.SemiBold)
                        Text("V5.0.0 • AirPods 3 BLE companion", color = AppSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Divider(color = AppSurface2)
                        Spacer(Modifier.height(10.dp))
                        Text("Ear detection is reverse-engineered and should be treated as experimental.", color = AppSecondary, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun SettingsHeader(text: String) {
    Text(
        text,
        color = AppSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(color = AppSurface, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = AppText, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = AppSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun notificationAccessEnabled(activity: MainActivity): Boolean {
    val packageName = activity.packageName
    return NotificationManagerCompat.getEnabledListenerPackages(activity).contains(packageName)
}


@Composable
private fun AirPodsTheme(content: @Composable () -> Unit) {
    val colors = androidx.compose.material3.darkColorScheme(
        primary = AppAccent,
        onPrimary = Color(0xFF17131F),
        background = AppBackground,
        onBackground = AppText,
        surface = AppSurface,
        onSurface = AppText,
        surfaceVariant = AppSurface2,
        onSurfaceVariant = AppSecondary
    )
    MaterialTheme(colorScheme = colors, content = content)
}
