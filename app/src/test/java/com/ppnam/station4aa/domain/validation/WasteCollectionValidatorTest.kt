package com.ppnam.station4aa.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WasteCollectionValidatorTest {

    @Test
    fun `blank machine operator id is rejected`() {
        assertEquals("Required.", WasteCollectionValidator.validateMachineOperatorUserId(""))
        assertEquals("Required.", WasteCollectionValidator.validateMachineOperatorUserId("   "))
    }

    @Test
    fun `placeholder machine operator id is rejected case-insensitively`() {
        assertNotNull(WasteCollectionValidator.validateMachineOperatorUserId("UNKNOWN"))
        assertNotNull(WasteCollectionValidator.validateMachineOperatorUserId("unknown"))
        assertNotNull(WasteCollectionValidator.validateMachineOperatorUserId("N/A"))
        assertNotNull(WasteCollectionValidator.validateMachineOperatorUserId("n/a"))
    }

    @Test
    fun `control characters in machine operator id are rejected`() {
        val bell = 7.toChar()
        val withControlChar = "MO-001$bell"
        assertNotNull(WasteCollectionValidator.validateMachineOperatorUserId(withControlChar))
    }

    @Test
    fun `machine operator id over 100 characters is rejected`() {
        val tooLong = "A".repeat(101)
        assertNotNull(WasteCollectionValidator.validateMachineOperatorUserId(tooLong))
    }

    @Test
    fun `valid machine operator id is accepted`() {
        assertNull(WasteCollectionValidator.validateMachineOperatorUserId("MO-00427"))
    }

    @Test
    fun `leading and trailing whitespace does not itself fail validation`() {
        assertNull(WasteCollectionValidator.validateMachineOperatorUserId("  MO-00427  "))
    }

    @Test
    fun `blank collected by is rejected`() {
        assertEquals("Required.", WasteCollectionValidator.validateCollectedBy(""))
    }

    @Test
    fun `collected by is not placeholder-checked`() {
        // Unlike machineOperatorUserId, collectedBy is an existing handheld value, not freshly
        // typed per transaction — see WasteCollectionValidator's class doc.
        assertNull(WasteCollectionValidator.validateCollectedBy("UNKNOWN"))
    }

    @Test
    fun `collected by over 200 characters is rejected`() {
        val tooLong = "A".repeat(201)
        assertNotNull(WasteCollectionValidator.validateCollectedBy(tooLong))
    }
}
