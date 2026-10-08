package com.tejaswi.airpodsbattery.model

data class BluetoothPacket(
    val name: String,
    val address: String,
    val rssi: Int,
    val appleData: String?,
    val manufacturerLength: Int,
    val isAirPods3: Boolean
)
