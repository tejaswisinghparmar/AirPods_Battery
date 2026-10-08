package com.tejaswi.airpodsbattery.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tejaswi.airpodsbattery.AppPrefs
import com.tejaswi.airpodsbattery.AirPodsStateBus
import com.tejaswi.airpodsbattery.bluetooth.BluetoothAirPodsScanner
import com.tejaswi.airpodsbattery.media.MediaControlNotificationListenerService
import com.tejaswi.airpodsbattery.model.AirPodsBattery
import com.tejaswi.airpodsbattery.widget.AirPodsWidget

class AirPodsMonitorService : Service() {
    private lateinit var scanner: BluetoothAirPodsScanner
    private var previous: AirPodsBattery? = null
    private var pausedByApp = false

    override fun onCreate() {
        super.onCreate()
        scanner = BluetoothAirPodsScanner(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!scanner.hasBluetooth() || !scanner.hasPermissions()) { stopSelf(); return START_NOT_STICKY }
        scanner.startScan(
            onPacket = {},
            onBattery = { incoming ->
                val stable = AirPodsStateBus.publish(incoming)
                val old = previous
                previous = stable
                AppPrefs.saveBattery(this, stable)
                AirPodsWidget.refresh(this)

                if (AppPrefs.autoPause(this) && shouldPause(old, stable)) {
                    pausedByApp = MediaControlNotificationListenerService.pausePlayback()
                } else if (AppPrefs.autoPlay(this) && pausedByApp && shouldResume(old, stable)) {
                    if (MediaControlNotificationListenerService.resumePlayback()) pausedByApp = false
                }
            },
            onError = { stopSelf() }
        )
        return START_STICKY
    }

    private fun shouldPause(old: AirPodsBattery?, current: AirPodsBattery): Boolean {
        if (old == null) return false
        val wasAnyInEar = old.leftInEar == true || old.rightInEar == true
        val nowAnyRemoved = current.leftInEar == false || current.rightInEar == false
        return wasAnyInEar && nowAnyRemoved
    }

    private fun shouldResume(old: AirPodsBattery?, current: AirPodsBattery): Boolean {
        if (old == null) return false
        val wasAnyOut = old.leftInEar == false || old.rightInEar == false
        val nowAnyIn = current.leftInEar == true || current.rightInEar == true
        return wasAnyOut && nowAnyIn
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(com.tejaswi.airpodsbattery.R.drawable.ic_stat_airpods)
        .setContentTitle("AirPods Battery")
        .setContentText("Monitoring AirPods")
        .setOngoing(true).setSilent(true).setCategory(NotificationCompat.CATEGORY_SERVICE).build()

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "AirPods monitoring", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onDestroy() { scanner.stopScan(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { private const val CHANNEL_ID = "airpods_monitoring"; private const val NOTIFICATION_ID = 1001 }
}
