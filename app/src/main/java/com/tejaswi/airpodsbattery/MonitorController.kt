package com.tejaswi.airpodsbattery

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.tejaswi.airpodsbattery.service.AirPodsMonitorService

object MonitorController {
    fun start(context: Context) {
        val intent = Intent(context, AirPodsMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, AirPodsMonitorService::class.java))
    }
}
