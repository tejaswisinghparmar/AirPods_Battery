package com.tejaswi.airpodsbattery.model

data class AirPodsBattery(
    val left: Int?,
    val right: Int?,
    val case: Int?,
    val leftCharging: Boolean = false,
    val rightCharging: Boolean = false,
    val caseCharging: Boolean = false,
    val leftInEar: Boolean? = null,
    val rightInEar: Boolean? = null,
    val rssi: Int? = null
)
