package com.example.flowmind.domain.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyShieldTest {

    @Test
    fun testSsnRedaction() {
        val input = "My SSN is 123-45-6789 please keep it safe."
        val expected = "My SSN is [REDACTED_SSN] please keep it safe."
        
        val result = PrivacyShield.redactSensitiveInfo(input)
        assertEquals(expected, result)
        assertFalse(PrivacyShield.isSafeForCloud(input))
    }

    @Test
    fun testEmailRedaction() {
        val input = "Contact me at user@example.com for more info."
        val expected = "Contact me at [REDACTED_EMAIL] for more info."
        
        val result = PrivacyShield.redactSensitiveInfo(input)
        assertEquals(expected, result)
        // Emails are redacted but generally safe for cloud parsing without hard-rejecting the whole payload.
        // Our isSafeForCloud only checks SSN and CC right now, so it should return true here.
        assertTrue(PrivacyShield.isSafeForCloud(input))
    }

    @Test
    fun testSafeData() {
        val input = "The plant has yellow leaves and looks wilted."
        val result = PrivacyShield.redactSensitiveInfo(input)
        
        assertEquals(input, result)
        assertTrue(PrivacyShield.isSafeForCloud(input))
    }
}
