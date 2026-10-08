package com.tejaswi.airpodsbattery.bluetooth

import com.tejaswi.airpodsbattery.model.AirPodsBattery

object AirPodsParser {
    const val APPLE_COMPANY_ID = 0x004C
    const val AIRPODS_3_MODEL = 0x1320

    /**
     * Parses the 27-byte AirPods proximity/battery advertisement.
     *
     * Battery fields are well-established for AirPods 3. Ear detection is
     * reverse-engineered and therefore exposed as nullable/experimental.
     */
    fun parse(data: ByteArray, rssi: Int? = null): AirPodsBattery? {
        if (data.size < 8) return null

        val b = data.map { it.toInt() and 0xFF }
        if (b[0] != 0x07) return null

        val model = (b[3] shl 8) or b[4]
        if (model != AIRPODS_3_MODEL) return null

        val status = b[5]
        val podBattery = b[6]
        val caseAndFlags = b[7]

        val first = decode(podBattery ushr 4)
        val second = decode(podBattery and 0x0F)
        val caseBattery = decode(caseAndFlags and 0x0F)

        val flipped = (status and 0x20) != 0
        val left = if (flipped) second else first
        val right = if (flipped) first else second

        val flags = caseAndFlags ushr 4

        // Charging flags: bit 0 = right, bit 1 = left, bit 2 = case.
        val rightCharging = (flags and 0x01) != 0
        val leftCharging = (flags and 0x02) != 0
        val caseCharging = (flags and 0x04) != 0

        // Reverse-engineered AirPods proximity status bits.
        // Bit 5 selects the primary pod. Bit 6 identifies whether this
        // advertisement's pod is in the case. Bits 1 and 3 encode ear state
        // with XOR orientation handling.
        val primaryIsLeft = (status and 0x20) != 0
        val thisPodInCase = (status and 0x40) != 0
        val xorFactor = primaryIsLeft.xor(thisPodInCase)
        // The raw orientation is opposite to the physical side for the
        // AirPods 3 advertisements observed in the field. Swap the two
        // decoded ear bits after applying the orientation factor.
        val decodedA = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val decodedB = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0
        val leftInEar = decodedB
        val rightInEar = decodedA

        return AirPodsBattery(
            left = left,
            right = right,
            case = caseBattery,
            rightCharging = rightCharging,
            leftCharging = leftCharging,
            caseCharging = caseCharging,
            leftInEar = leftInEar,
            rightInEar = rightInEar,
            rssi = rssi
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
        val model = (b[3] shl 8) or b[4]
        return model == AIRPODS_3_MODEL
    }

    fun hex(data: ByteArray): String =
        data.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
