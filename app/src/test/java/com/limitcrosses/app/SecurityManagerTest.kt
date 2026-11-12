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
        assertEquals(12, code.length) // LC-XXXX-XXXX: 3 + 4 + 1 + 4 = 12
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
