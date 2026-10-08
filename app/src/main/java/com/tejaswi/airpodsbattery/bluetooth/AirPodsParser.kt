package com.tejaswi.airpodsbattery.bluetooth

import com.tejaswi.airpodsbattery.model.AirPodsBattery

object AirPodsParser {

    const val APPLE_COMPANY_ID = 0x004C
    const val AIRPODS_3_MODEL = 0x1320

    /**
     * Parses the compact AirPods proximity advertisement when the expected
     * battery fields are present.
     *
     * This parser is deliberately conservative. Unknown packet layouts are
     * returned as null rather than guessing battery values.
     */
    fun parse(data: ByteArray): AirPodsBattery? {
        if (data.size < 8) return null

        val b = data.map { it.toInt() and 0xFF }

        if (b[0] != 0x07) return null

        val model = b[3] or (b[4] shl 8)
        if (model != AIRPODS_3_MODEL) return null

        val status = b[5]
        val podBattery = b[6]
        val caseAndFlags = b[7]

        val first = decode(b[6] ushr 4)
        val second = decode(b[6] and 0x0F)
        val caseBattery = decode(caseAndFlags and 0x0F)

        val flip = (status and 0x20) != 0

        val left = if (flip) second else first
        val right = if (flip) first else second

        val flags = caseAndFlags ushr 4

        return AirPodsBattery(
            left = left,
            right = right,
            case = caseBattery,
            leftCharging = (flags and 0x01) != 0,
            rightCharging = (flags and 0x02) != 0,
            caseCharging = (flags and 0x04) != 0
        )
    }

    private fun decode(value: Int): Int? {
        return when {
            value in 0..9 -> value * 10 + 5
            value == 10 -> 100
            else -> null
        }
    }

    fun isAirPods3(data: ByteArray): Boolean {
        if (data.size < 5) return false
        val b = data.map { it.toInt() and 0xFF }
        if (b[0] != 0x07) return false
        val model = b[3] or (b[4] shl 8)
        return model == AIRPODS_3_MODEL
    }

    fun hex(data: ByteArray): String =
        data.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
