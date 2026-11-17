package com.limitcrosses.app

import com.limitcrosses.app.core.security.SecurityManager
import org.junit.Assert.*
import org.junit.Test

class SecurityManagerTest {

    @Test
    fun testPasswordHashingAndVerification() {
        val plain = "StrongPassword@2026"
        val hash = SecurityManager.hashPassword(plain)
        assertNotNull(hash)
        assertTrue(hash.length == 64) // SHA-256 hex string

        assertTrue(SecurityManager.verifyPassword(plain, hash))
        assertFalse(SecurityManager.verifyPassword("WrongPassword", hash))
    }

    @Test
    fun testInviteCodeGenerationFormat() {
        val code = SecurityManager.generateInviteCode()
        assertNotNull(code)
        assertTrue(code.startsWith("LC-"))
        assertEquals(17, code.length) // LC-XXXX-YYYY-ZZZZ: 3 + 4 + 1 + 4 + 1 + 4 = 17
    }

    @Test
    fun testUniversalMultiDeviceInviteRoundtrip() {
        val limits = listOf(0, 15, 30, 45)
        val validDays = listOf(1, 7, 14, 30, 60, 90)

        for (limit in limits) {
            for (days in validDays) {
                val code = SecurityManager.generateUniversalInviteCode(limitMinutes = limit, validDays = days)
                assertTrue("Code should start with LC-", code.startsWith("LC-"))
                assertEquals(17, code.length)

                val payload = SecurityManager.decodeAndVerifyUniversalInvite(code)
                assertNotNull("Payload must be valid for generated code: $code", payload)
                assertEquals(limit, payload!!.limitMinutes)
                assertEquals(days, payload.validDays)
                assertTrue(payload.expiresAtEpochMs > payload.createdAtEpochMs)

                // Tamper resistance test
                val tamperedChar = if (code.last() != 'A') 'A' else 'B'
                val tamperedCode = code.dropLast(1) + tamperedChar
                assertNull("Tampered code must fail cryptographic HMAC verification", SecurityManager.decodeAndVerifyUniversalInvite(tamperedCode))
            }
        }
    }

    @Test
    fun testUniversalInviteNormalization() {
        val code = SecurityManager.generateUniversalInviteCode(limitMinutes = 30, validDays = 14)
        val payloadOriginal = SecurityManager.decodeAndVerifyUniversalInvite(code)
        assertNotNull(payloadOriginal)

        // Lowercase
        val lowerPayload = SecurityManager.decodeAndVerifyUniversalInvite(code.lowercase())
        assertNotNull(lowerPayload)
        assertEquals(payloadOriginal!!.limitMinutes, lowerPayload!!.limitMinutes)

        // Without LC- prefix
        val rawCode = code.removePrefix("LC-").replace("-", "")
        val rawPayload = SecurityManager.decodeAndVerifyUniversalInvite(rawCode)
        assertNotNull(rawPayload)
        assertEquals(payloadOriginal.limitMinutes, rawPayload!!.limitMinutes)

        // With spaces
        val spacedCode = code.replace("-", " ")
        val spacedPayload = SecurityManager.decodeAndVerifyUniversalInvite(spacedCode)
        assertNotNull(spacedPayload)
        assertEquals(payloadOriginal.limitMinutes, spacedPayload!!.limitMinutes)
    }

    @Test
    fun testAdminBootstrapSecurity() {
        // Valid Master Key
        assertTrue(SecurityManager.validateAdminBootstrap("LIMIT-CROSS-ADMIN-BOOTSTRAP-2026"))
        assertTrue(SecurityManager.validateAdminBootstrap(" LIMIT-CROSS-ADMIN-BOOTSTRAP-2026 "))

        // Unauthorized attempts
        assertFalse(SecurityManager.validateAdminBootstrap("admin123"))
        assertFalse(SecurityManager.validateAdminBootstrap("secret"))
        assertFalse(SecurityManager.validateAdminBootstrap(""))
    }
}
