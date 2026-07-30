package com.ppnam.station4aa.domain.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WasteCollectionEventTest {

    private val fixedInstant: Instant = Instant.parse("2026-07-30T10:15:30.000Z")

    @Test
    fun `create trims fields and stamps schema version 2 on the wire message`() {
        val event = WasteCollectionEvent.create(
            machineCode = "  EXT-04  ",
            machineName = " Extruder 4 ",
            wasteTypeCode = " WT-01 ",
            collectedBy = " WO-00112 ",
            machineOperatorUserId = " MO-00427 ",
            now = fixedInstant,
        )

        assertEquals("EXT-04", event.machineCode)
        assertEquals("Extruder 4", event.machineName)
        assertEquals("WT-01", event.wasteTypeCode)
        assertEquals("WO-00112", event.collectedBy)
        assertEquals("MO-00427", event.machineOperatorUserId)
        assertEquals("2026-07-30T10:15:30.000Z", event.collectedAtUtc)
        assertEquals(2, event.toWireMessage().schemaVersion)
    }

    @Test
    fun `collectionId follows the contract's WC-yyyyMMdd- shape`() {
        val event = WasteCollectionEvent.create(
            machineCode = "EXT-04",
            machineName = "Extruder 4",
            wasteTypeCode = "WT-01",
            collectedBy = "WO-00112",
            machineOperatorUserId = "MO-00427",
            now = fixedInstant,
        )
        assertTrue(event.collectionId.matches(Regex("WC-20260730-\\d{6}")))
    }

    @Test
    fun `wire JSON uses the exact camelCase property names the contract requires`() {
        val event = WasteCollectionEvent.create(
            machineCode = "EXT-04",
            machineName = "Extruder 4",
            wasteTypeCode = "WT-01",
            collectedBy = "WO-00112",
            machineOperatorUserId = "MO-00427",
            now = fixedInstant,
        )
        val json = Gson().toJson(event.toWireMessage())

        listOf(
            "\"schemaVersion\":2",
            "\"messageId\"",
            "\"collectionId\"",
            "\"machineCode\":\"EXT-04\"",
            "\"machineName\":\"Extruder 4\"",
            "\"wasteTypeCode\":\"WT-01\"",
            "\"collectedBy\":\"WO-00112\"",
            "\"machineOperatorUserId\":\"MO-00427\"",
            "\"collectedAtUtc\":\"2026-07-30T10:15:30.000Z\"",
        ).forEach { expectedFragment ->
            assertTrue("Expected JSON to contain $expectedFragment but was $json", json.contains(expectedFragment))
        }
    }

    @Test
    fun `two events created back to back get different messageIds`() {
        val first = WasteCollectionEvent.create("EXT-04", "Extruder 4", "WT-01", "WO-00112", "MO-00427", fixedInstant)
        val second = WasteCollectionEvent.create("EXT-04", "Extruder 4", "WT-01", "WO-00112", "MO-00427", fixedInstant)
        assertTrue(first.messageId != second.messageId)
    }
}
