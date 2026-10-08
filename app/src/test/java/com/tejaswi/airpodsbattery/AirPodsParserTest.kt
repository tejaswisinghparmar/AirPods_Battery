package com.tejaswi.airpodsbattery

import com.tejaswi.airpodsbattery.bluetooth.AirPodsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AirPodsParserTest {

    @Test
    fun parsesAirPods3Advertisement() {
        // Example structure:
        // 07 19 01 13 20 00 A8 55 ...
        // model = 0x1320
        // byte 6 = A8 -> 100%, 85%
        // byte 7 = 55 -> case 55%, charging flags 0
        val packet = byteArrayOf(
            0x07, 0x19, 0x01, 0x13, 0x20,
            0x00, 0xA8.toByte(), 0x05
        )

        val result = AirPodsParser.parse(packet)

        assertNotNull(result)
        assertEquals(100, result!!.left)
        assertEquals(85, result.right)
        assertEquals(55, result.case)
    }

    @Test
    fun rejectsNonAirPods3Model() {
        val packet = byteArrayOf(
            0x07, 0x19, 0x01, 0x0F, 0x20,
            0x00, 0xA8.toByte(), 0x05
        )

        assertNull(AirPodsParser.parse(packet))
    }

    @Test
    fun rejectsNonProximityPacket() {
        val packet = byteArrayOf(
            0x08, 0x19, 0x01, 0x13, 0x20,
            0x00, 0xA8.toByte(), 0x05
        )

        assertNull(AirPodsParser.parse(packet))
    }
}
