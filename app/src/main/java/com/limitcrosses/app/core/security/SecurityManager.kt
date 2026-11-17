package com.limitcrosses.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class UniversalInvitePayload(
    val limitMinutes: Int,
    val validDays: Int,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val nonce: Int
)

object SecurityManager {
    // The master bootstrap token required to create the first admin account.
    // Public registration as admin is strictly blocked unless this secret is presented.
    const val MASTER_BOOTSTRAP_SECRET = "LIMIT-CROSS-ADMIN-BOOTSTRAP-2026"

    private const val BASE_EPOCH_MS = 1704067200000L // 2024-01-01 UTC
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // 32 human-friendly chars (no 0, O, 1, I)

    private val secureRandom = SecureRandom()

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(plain: String, hash: String): Boolean {
        val computedHash = hashPassword(plain).toByteArray(Charsets.UTF_8)
        val targetHash = hash.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(computedHash, targetHash)
    }

    fun generateUuid(): String = UUID.randomUUID().toString()

    fun validateAdminBootstrap(providedKey: String): Boolean {
        val providedBytes = providedKey.trim().toByteArray(Charsets.UTF_8)
        val secretBytes = MASTER_BOOTSTRAP_SECRET.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(providedBytes, secretBytes)
    }

    /**
     * Normalizes an invite code by trimming, uppercasing, and standardizing group hyphens.
     */
    fun normalizeInviteCode(rawInput: String): String {
        val cleaned = rawInput.trim().uppercase().replace(" ", "").replace("-", "")
        val core = if (cleaned.startsWith("LC")) cleaned.substring(2) else cleaned
        return when (core.length) {
            12 -> "LC-${core.substring(0, 4)}-${core.substring(4, 8)}-${core.substring(8, 12)}"
            8 -> "LC-${core.substring(0, 4)}-${core.substring(4, 8)}"
            else -> rawInput.trim().uppercase()
        }
    }

    /**
     * Generates a self-verifying, cryptographic Universal Multi-Device Invitation Code.
     * Contains encoded limit allowance, validity duration, epoch timestamp, random nonce,
     * and a 20-bit HMAC-SHA256 signature that can be verified offline on any device.
     */
    fun generateUniversalInviteCode(
        limitMinutes: Int = 30,
        validDays: Int = 14,
        nowMs: Long = System.currentTimeMillis()
    ): String {
        val clampedLimit = limitMinutes.coerceIn(0, 45)
        val clampedDays = validDays.coerceIn(1, 90)
        val epochDays = ((nowMs - BASE_EPOCH_MS) / (86400 * 1000L)).toInt().coerceIn(0, 4095)
        val nonce = secureRandom.nextInt(32768)

        val limitBits = clampedLimit.toLong() and 0x3FL
        val daysBits = clampedDays.toLong() and 0x7FL
        val epochBits = epochDays.toLong() and 0xFFFL
        val nonceBits = nonce.toLong() and 0x7FFFL

        val payload40 = (limitBits shl 34) or (daysBits shl 27) or (epochBits shl 15) or nonceBits

        val payloadBytes = ByteArray(5) { i ->
            ((payload40 ushr ((4 - i) * 8)) and 0xFFL).toByte()
        }

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(MASTER_BOOTSTRAP_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hmac = mac.doFinal(payloadBytes)
        val sig20 = (((hmac[0].toInt() and 0xFF) shl 12) or
                ((hmac[1].toInt() and 0xFF) shl 4) or
                ((hmac[2].toInt() and 0xF0) ushr 4)) and 0xFFFFF

        var packed60 = (payload40 shl 20) or sig20.toLong()

        val chars = CharArray(12)
        for (i in 11 downTo 0) {
            chars[i] = ALPHABET[(packed60 and 0x1FL).toInt()]
            packed60 = packed60 ushr 5
        }
        val raw = String(chars)
        return "LC-${raw.substring(0, 4)}-${raw.substring(4, 8)}-${raw.substring(8, 12)}"
    }

    /**
     * Cryptographically verifies and extracts payload from a Universal Invite Code.
     * Returns null if signature is invalid, corrupted, or format is malformed.
     */
    fun decodeAndVerifyUniversalInvite(code: String): UniversalInvitePayload? {
        val cleaned = code.trim().uppercase().replace(" ", "").replace("-", "")
        val raw12 = if (cleaned.startsWith("LC")) cleaned.substring(2) else cleaned
        if (raw12.length != 12) return null

        var packed60 = 0L
        for (c in raw12) {
            val idx = ALPHABET.indexOf(c)
            if (idx < 0) return null
            packed60 = (packed60 shl 5) or (idx.toLong() and 0x1FL)
        }

        val sig20 = (packed60 and 0xFFFFFL).toInt()
        val payload40 = (packed60 ushr 20) and 0xFFFFFFFFFFL

        val payloadBytes = ByteArray(5) { i ->
            ((payload40 ushr ((4 - i) * 8)) and 0xFFL).toByte()
        }

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(MASTER_BOOTSTRAP_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hmac = mac.doFinal(payloadBytes)
        val expectedSig20 = (((hmac[0].toInt() and 0xFF) shl 12) or
                ((hmac[1].toInt() and 0xFF) shl 4) or
                ((hmac[2].toInt() and 0xF0) ushr 4)) and 0xFFFFF

        if (sig20 != expectedSig20) {
            return null // Invalid HMAC signature or tampered code
        }

        val nonce = (payload40 and 0x7FFFL).toInt()
        val epochDays = ((payload40 ushr 15) and 0xFFFL).toInt()
        val validDays = ((payload40 ushr 27) and 0x7FL).toInt()
        val limitMinutes = ((payload40 ushr 34) and 0x3FL).toInt()

        val createdAtEpochMs = BASE_EPOCH_MS + (epochDays.toLong() * 86400 * 1000L)
        val expiresAtEpochMs = createdAtEpochMs + (validDays.toLong() * 86400 * 1000L)

        return UniversalInvitePayload(
            limitMinutes = limitMinutes.coerceIn(0, 45),
            validDays = validDays.coerceIn(1, 90),
            createdAtEpochMs = createdAtEpochMs,
            expiresAtEpochMs = expiresAtEpochMs,
            nonce = nonce
        )
    }

    /**
     * Backward-compatible helper for legacy callers.
     */
    fun generateInviteCode(): String = generateUniversalInviteCode(limitMinutes = 30, validDays = 14)
}

