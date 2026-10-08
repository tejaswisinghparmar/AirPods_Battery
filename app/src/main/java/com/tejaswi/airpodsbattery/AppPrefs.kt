package com.tejaswi.airpodsbattery

import android.content.Context

object AppPrefs {
    private const val NAME = "airpods_preferences"
    private const val AUTO_PAUSE = "auto_pause"
    private const val BACKGROUND_MONITORING = "background_monitoring"
    private const val LAST_LEFT = "last_left"
    private const val LAST_RIGHT = "last_right"
    private const val LAST_CASE = "last_case"
    private const val LAST_LEFT_CHARGING = "last_left_charging"
    private const val LAST_RIGHT_CHARGING = "last_right_charging"
    private const val LAST_CASE_CHARGING = "last_case_charging"
    private const val LAST_LEFT_EAR = "last_left_ear"
    private const val LAST_RIGHT_EAR = "last_right_ear"
    private const val LAST_RSSI = "last_rssi"
    private const val LAST_UPDATED = "last_updated"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun autoPause(context: Context): Boolean = prefs(context).getBoolean(AUTO_PAUSE, false)

    fun setAutoPause(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(AUTO_PAUSE, enabled).apply()
    }

    fun backgroundMonitoring(context: Context): Boolean =
        prefs(context).getBoolean(BACKGROUND_MONITORING, false)

    fun setBackgroundMonitoring(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(BACKGROUND_MONITORING, enabled).apply()
    }

    fun saveBattery(context: Context, battery: com.tejaswi.airpodsbattery.model.AirPodsBattery) {
        prefs(context).edit()
            .putIntOrRemove(LAST_LEFT, battery.left)
            .putIntOrRemove(LAST_RIGHT, battery.right)
            .putIntOrRemove(LAST_CASE, battery.case)
            .putBoolean(LAST_LEFT_CHARGING, battery.leftCharging)
            .putBoolean(LAST_RIGHT_CHARGING, battery.rightCharging)
            .putBoolean(LAST_CASE_CHARGING, battery.caseCharging)
            .putBooleanOrRemove(LAST_LEFT_EAR, battery.leftInEar)
            .putBooleanOrRemove(LAST_RIGHT_EAR, battery.rightInEar)
            .putIntOrRemove(LAST_RSSI, battery.rssi)
            .putLong(LAST_UPDATED, System.currentTimeMillis())
            .apply()
    }

    fun lastBattery(context: Context): com.tejaswi.airpodsbattery.model.AirPodsBattery? {
        val p = prefs(context)
        val updated = p.getLong(LAST_UPDATED, 0L)
        if (updated == 0L) return null
        return com.tejaswi.airpodsbattery.model.AirPodsBattery(
            left = p.intOrNull(LAST_LEFT),
            right = p.intOrNull(LAST_RIGHT),
            case = p.intOrNull(LAST_CASE),
            leftCharging = p.getBoolean(LAST_LEFT_CHARGING, false),
            rightCharging = p.getBoolean(LAST_RIGHT_CHARGING, false),
            caseCharging = p.getBoolean(LAST_CASE_CHARGING, false),
            leftInEar = p.booleanOrNull(LAST_LEFT_EAR),
            rightInEar = p.booleanOrNull(LAST_RIGHT_EAR),
            rssi = p.intOrNull(LAST_RSSI)
        )
    }

    fun lastUpdated(context: Context): Long = prefs(context).getLong(LAST_UPDATED, 0L)

    private fun android.content.SharedPreferences.Editor.putIntOrRemove(key: String, value: Int?) = apply {
        if (value == null) remove(key) else putInt(key, value)
    }

    private fun android.content.SharedPreferences.Editor.putBooleanOrRemove(key: String, value: Boolean?) = apply {
        if (value == null) remove(key) else putBoolean(key, value)
    }

    private fun android.content.SharedPreferences.intOrNull(key: String): Int? =
        if (contains(key)) getInt(key, 0) else null

    private fun android.content.SharedPreferences.booleanOrNull(key: String): Boolean? =
        if (contains(key)) getBoolean(key, false) else null
}
