package com.tejaswi.airpodsbattery.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.tejaswi.airpodsbattery.model.AirPodsBattery
import com.tejaswi.airpodsbattery.model.BluetoothPacket

class BluetoothAirPodsScanner(context: Context) {
    private val appContext = context.applicationContext
    private val manager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = manager.adapter

    private var callback: ScanCallback? = null

    fun hasBluetooth(): Boolean = adapter?.isEnabled == true

    fun hasPermissions(): Boolean {
        val scan = if (android.os.Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        val connect = if (android.os.Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return scan && connect
    }

    @SuppressLint("MissingPermission")
    fun startScan(
        onPacket: (BluetoothPacket) -> Unit,
        onBattery: (AirPodsBattery) -> Unit,
        onError: (String) -> Unit
    ) {
        stopScan()
        val bleScanner = adapter?.bluetoothLeScanner
        if (bleScanner == null) {
            onError("Bluetooth LE scanner unavailable.")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord ?: return
                val apple = record.getManufacturerSpecificData(AirPodsParser.APPLE_COMPANY_ID)
                    ?: return

                val battery = AirPodsParser.parse(apple, result.rssi)

                onPacket(
                    BluetoothPacket(
                        name = result.device.name ?: record.deviceName ?: "Unknown",
                        address = result.device.address,
                        rssi = result.rssi,
                        appleData = AirPodsParser.hex(apple),
                        manufacturerLength = apple.size,
                        isAirPods3 = AirPodsParser.isAirPods3(apple)
                    )
                )

                if (battery != null) onBattery(battery)
            }

            override fun onScanFailed(errorCode: Int) {
                onError("Bluetooth scan failed: $errorCode")
            }
        }

        try {
            bleScanner.startScan(null, settings, callback)
        } catch (e: SecurityException) {
            onError("Bluetooth permission was not granted.")
        } catch (e: Exception) {
            onError(e.message ?: "Unable to start Bluetooth scan.")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val current = callback ?: return
        try {
            adapter?.bluetoothLeScanner?.stopScan(current)
        } catch (_: Exception) {
        }
        callback = null
    }
}
