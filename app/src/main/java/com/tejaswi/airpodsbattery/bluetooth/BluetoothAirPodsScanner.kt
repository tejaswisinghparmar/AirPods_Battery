package com.tejaswi.airpodsbattery.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.tejaswi.airpodsbattery.model.AirPodsBattery

class BluetoothAirPodsScanner(context: Context) {

    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private var callback: ScanCallback? = null

    fun hasBluetooth(): Boolean =
        adapter?.isEnabled == true

    fun hasPermissions(): Boolean {
        val scanGranted = if (android.os.Build.VERSION.SDK_INT >= 31) {
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

        val connectGranted = if (android.os.Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return scanGranted && connectGranted
    }

    @SuppressLint("MissingPermission")
    fun startScan(onBattery: (AirPodsBattery) -> Unit, onError: (String) -> Unit) {
        stopScan()

        val bleScanner = adapter?.bluetoothLeScanner
        if (bleScanner == null) {
            onError("Bluetooth LE scanner is unavailable.")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Filter by Apple's manufacturer ID. Model filtering is performed
        // in AirPodsParser because manufacturer data is variable in length.
        val filter = ScanFilter.Builder()
            .setManufacturerData(
                AirPodsParser.APPLE_COMPANY_ID,
                byteArrayOf(0x07),
                byteArrayOf(0xFF.toByte())
            )
            .build()

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val manufacturerData = result.scanRecord
                    ?.getManufacturerSpecificData(AirPodsParser.APPLE_COMPANY_ID)
                    ?: return

                val parsed = AirPodsParser.parse(manufacturerData) ?: return
                onBattery(parsed)
            }

            override fun onScanFailed(errorCode: Int) {
                onError("Bluetooth scan failed: $errorCode")
            }
        }

        try {
            bleScanner.startScan(listOf(filter), settings, callback)
        } catch (e: SecurityException) {
            onError("Bluetooth permission was not granted.")
        } catch (e: Exception) {
            onError(e.message ?: "Unable to start Bluetooth scan.")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val currentCallback = callback ?: return
        try {
            adapter?.bluetoothLeScanner?.stopScan(currentCallback)
        } catch (_: Exception) {
            // Scanner may already have been stopped.
        }
        callback = null
    }
}
