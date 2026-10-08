package com.tejaswi.airpodsbattery.bluetooth

import com.tejaswi.airpodsbattery.model.AirPodsBattery

/**
 * Parser for the Apple BLE proximity-pairing advertisement used by AirPods.
 *
 * Apple company ID: 0x004C
 * AirPods 3 model identifier: 0x1320
 *
 * For the 27-byte paired-mode payload:
 *   byte 0: 0x07 (proximity pairing)
 *   byte 1: 0x19 (payload length)
 *   byte 2: mode/prefix
 *   byte 3..4: model identifier
 *   byte 5: status / left-right flip bit
 *   byte 6: left/right battery nibbles
 *   byte 7: case battery low nibble + charging flags high nibble
 *
 * Battery values are encoded as:
 *   0..9 -> 5..95%
 *   10   -> 100%
 *   15   -> unavailable
 */
object AirPodsParser {

    const val APPLE_COMPANY_ID = 0x004C
    private const val AIRPODS_3_MODEL = 0x1320
    private const val PROXIMITY_TYPE = 0x07

    fun parse(manufacturerData: ByteArray): AirPodsBattery? {
        if (manufacturerData.size < 8) return null

        val data = manufacturerData.map { it.toInt() and 0xFF }

        if (data[0] != PROXIMITY_TYPE) return null

        val model = data[3] or (data[4] shl 8)
        if (model != AIRPODS_3_MODEL) return null

        val status = data[5]
        val podBattery = data[6]
        val caseAndFlags = data[7]

        val firstPod = decodeNibble((podBattery ushr 4) and 0x0F)
        val secondPod = decodeNibble(podBattery and 0x0F)
        val caseBattery = decodeNibble(caseAndFlags and 0x0F)

        // Bit 5 identifies which pod is primary. The battery nibbles can
        // therefore need to be mapped in the opposite order.
        val flip = (status and 0x20) != 0

        val left = if (flip) secondPod else firstPod
        val right = if (flip) firstPod else secondPod

        // High nibble charging flags:
        // bit 4 -> left, bit 5 -> right, bit 6 -> case.
        val flags = (caseAndFlags ushr 4) and 0x0F
        val leftCharging = (flags and 0x01) != 0
        val rightCharging = (flags and 0x02) != 0
        val caseCharging = (flags and 0x04) != 0

        return AirPodsBattery(
            left = left,
            right = right,
            case = caseBattery,
            leftCharging = leftCharging,
            rightCharging = rightCharging,
            caseCharging = caseCharging
        )
    }

    private fun decodeNibble(value: Int): Int? {
        return when {
            value in 0..9 -> (value * 10) + 5
            value == 10 -> 100
            else -> null
        }
    }
}
