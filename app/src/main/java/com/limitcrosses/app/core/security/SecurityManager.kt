package com.limitcrosses.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object SecurityManager {
    // The master bootstrap token required to create the first admin account.
    // Public registration as admin is strictly blocked unless this secret is presented.
    const val MASTER_BOOTSTRAP_SECRET = "LIMIT-CROSS-ADMIN-BOOTSTRAP-2026"

    private val secureRandom = SecureRandom()
    private val allowedInviteChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(plain: String, hash: String): Boolean {
        return hashPassword(plain) == hash
    }

    fun generateInviteCode(): String {
        val sb = java.lang.StringBuilder("LC-")
        for (i in 0 until 4) {
            sb.append(allowedInviteChars[secureRandom.nextInt(allowedInviteChars.length)])
        }
        sb.append("-")
        for (i in 0 until 4) {
            sb.append(allowedInviteChars[secureRandom.nextInt(allowedInviteChars.length)])
        }
        return sb.toString()
    }

    fun generateUuid(): String = UUID.randomUUID().toString()

    fun validateAdminBootstrap(providedKey: String): Boolean {
        return providedKey.trim() == MASTER_BOOTSTRAP_SECRET
    }
}

