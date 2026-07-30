package com.ppnam.station4aa.data.mqtt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MqttTopicsTest {

    @Test
    fun `waste collection topic matches the contract exactly`() {
        // Case-sensitive exact match per Station4_Wastage_MQTT_Contract.md's transport table.
        assertEquals("station4/waste/collection", MqttTopics.WASTE_COLLECTION)
    }

    @Test
    fun `request topic uses the req segment`() {
        assertEquals(
            "PPNAM/station4_handheld_1/req/scram_start_requested",
            MqttTopics.request("station4_handheld_1", "scram_start_requested")
        )
    }

    @Test
    fun `responseWildcard subscribes to the res segment only`() {
        assertEquals("PPNAM/station4_handheld_1/res/+", MqttTopics.responseWildcard("station4_handheld_1"))
    }

    @Test
    fun `blank deviceId is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MqttTopics.request("", "scram_start_requested")
        }
    }

    @Test
    fun `deviceId containing a wildcard is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MqttTopics.responseWildcard("device+1")
        }
    }
}
