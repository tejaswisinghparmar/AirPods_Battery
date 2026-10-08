package com.tejaswi.airpodsbattery.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tejaswi.airpodsbattery.AppPrefs
import com.tejaswi.airpodsbattery.AirPodsStateBus
import com.tejaswi.airpodsbattery.bluetooth.BluetoothAirPodsScanner
import com.tejaswi.airpodsbattery.media.MediaControlNotificationListenerService
import com.tejaswi.airpodsbattery.model.AirPodsBattery
import com.tejaswi.airpodsbattery.widget.AirPodsWidget

class AirPodsMonitorService : Service() {
    private lateinit var scanner: BluetoothAirPodsScanner
    private var previous: AirPodsBattery? = null

    override fun onCreate() {
        super.onCreate()
        scanner = BluetoothAirPodsScanner(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!scanner.hasBluetooth() || !scanner.hasPermissions()) {
            stopSelf()
            return START_NOT_STICKY
        }

        scanner.startScan(
            onPacket = { packet ->
                // The service only needs decoded AirPods packets for state changes.
            },
            onBattery = { battery ->
                val old = previous
                previous = battery
                AppPrefs.saveBattery(this, battery)
                AirPodsStateBus.publish(battery)
                AirPodsWidget.refresh(this)

                if (AppPrefs.autoPause(this) && shouldPause(old, battery)) {
                    MediaControlNotificationListenerService.pausePlayback()
                }
            },
            onError = {
                stopSelf()
            }
        )

        return START_STICKY
    }

    private fun shouldPause(old: AirPodsBattery?, current: AirPodsBattery): Boolean {
        if (old == null) return false
        val oldLeft = old.leftInEar ?: return false
        val oldRight = old.rightInEar ?: return false
        val newLeft = current.leftInEar ?: return false
        val newRight = current.rightInEar ?: return false

        // Pause when at least one earbud was known to be in-ear and a later
        // confirmed packet reports that an earbud has been removed. This also
        // supports people who listen with only one AirPod.
        val wasAnyInEar = oldLeft || oldRight
        val nowMissingOne = !newLeft || !newRight
        return wasAnyInEar && nowMissingOne
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.tejaswi.airpodsbattery.R.drawable.ic_stat_airpods)
            .setContentTitle("AirPods Battery")
            .setContentText(if (AppPrefs.autoPause(this)) "Monitoring • Auto-pause enabled" else "Monitoring AirPods")
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "AirPods monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background AirPods battery and ear-detection monitoring"
            }
        )
    }

    override fun onDestroy() {
        scanner.stopScan()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "airpods_monitoring"
        private const val NOTIFICATION_ID = 1001
    }
}
