package com.limitcrosses.app.data.remote

import com.limitcrosses.app.core.model.AccountDeletionRequest
import com.limitcrosses.app.core.model.DeletionRequestStatus
import com.limitcrosses.app.core.model.ExtensionRequest
import com.limitcrosses.app.core.model.ExtensionStatus
import com.limitcrosses.app.core.model.UserAccount
import com.limitcrosses.app.core.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

data class RemoteRegistrationPayload(
    val inviteCode: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val limitMinutes: Int,
    val createdAtEpochMs: Long,
    val facebook: String? = null,
    val instagram: String? = null,
    val snapchat: String? = null,
    val usageTelemetry: RemoteUsageTelemetry? = null
)

data class RemoteUsageTelemetry(
    val userId: String,
    val userEmail: String? = null,
    val inviteCode: String? = null,
    val facebookSeconds: Long,
    val instagramSeconds: Long,
    val snapchatSeconds: Long,
    val totalUsedSeconds: Long,
    val timestampEpochMs: Long
)

data class InviteTopicEvents(
    val registrations: List<RemoteRegistrationPayload> = emptyList(),
    val extensionRequests: List<ExtensionRequest> = emptyList(),
    val extensionDecisions: List<RemoteExtensionDecision> = emptyList(),
    val limitUpdates: List<RemoteLimitUpdate> = emptyList(),
    val deletionRequests: List<AccountDeletionRequest> = emptyList(),
    val deletionDecisions: List<RemoteDeletionDecision> = emptyList(),
    val usageTelemetries: List<RemoteUsageTelemetry> = emptyList()
)

data class RemoteExtensionDecision(
    val requestId: String,
    val status: ExtensionStatus,
    val grantedMinutes: Int,
    val adminNotes: String?,
    val respondedAtEpochMs: Long
)

data class RemoteLimitUpdate(
    val userId: String,
    val dailyLimitMinutes: Int,
    val reason: String,
    val updatedAtEpochMs: Long
)

data class RemoteDeletionDecision(
    val requestId: String,
    val approved: Boolean,
    val decidedAtEpochMs: Long
)

object CloudSyncRelayHelper {
    private const val RELAY_BASE = "https://ntfy.sh"
    private const val TOPIC_SALT = "LIMIT_CROSSES_SALT_V1_"

    /**
     * Derives a deterministic, private, unguessable relay topic for an invitation code.
     * Uses normalizeInviteCode to guarantee identical topic whether raw input has
     * hyphens, spaces, lowercase characters, or missing LC prefix.
     */
    fun getTopicForInvite(inviteCode: String): String {
        val clean = SecurityManager.normalizeInviteCode(inviteCode)
            .uppercase()
            .replace(" ", "")
            .replace("-", "")
        val hash = SecurityManager.hashPassword(TOPIC_SALT + clean)
        return "lc_inv_" + hash.take(20)
    }

    private fun postEvent(topic: String, payloadJson: String): Boolean {
        return try {
            val url = URL("$RELAY_BASE/$topic")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            conn.setRequestProperty("Title", "LimitCrosses Cloud Relay")
            conn.outputStream.use { os ->
                os.write(payloadJson.toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    private fun pollEvents(topic: String): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        try {
            val url = URL("$RELAY_BASE/$topic/json?poll=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                for (line in text.trim().split("\n")) {
                    if (line.isBlank()) continue
                    try {
                        val eventObj = JSONObject(line)
                        if (eventObj.optString("event") == "message") {
                            val msgStr = eventObj.optString("message")
                            val msgJson = JSONObject(msgStr)
                            results.add(msgJson)
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
        return results
    }

    // ==========================================
    // 1. REGISTRATION SYNC
    // ==========================================

    suspend fun publishRegistration(
        inviteCode: String,
        user: UserAccount,
        limitMinutes: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val payload = JSONObject().apply {
            put("type", "REGISTRATION")
            put("inviteCode", inviteCode)
            put("userId", user.id)
            put("fullName", user.fullName)
            put("email", user.email)
            put("limitMinutes", limitMinutes)
            put("createdAtEpochMs", user.createdAtEpochMs)
            put("facebook", user.socialHandles.facebook ?: "")
            put("instagram", user.socialHandles.instagram ?: "")
            put("snapchat", user.socialHandles.snapchat ?: "")
        }
        postEvent(topic, payload.toString())
    }

    suspend fun pollRegistrationForInvite(inviteCode: String): RemoteRegistrationPayload? = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val events = pollEvents(topic)
        for (msgJson in events.reversed()) {
            if (msgJson.optString("type") == "REGISTRATION") {
                return@withContext RemoteRegistrationPayload(
                    inviteCode = msgJson.getString("inviteCode"),
                    userId = msgJson.getString("userId"),
                    fullName = msgJson.getString("fullName"),
                    email = msgJson.getString("email"),
                    limitMinutes = msgJson.getInt("limitMinutes"),
                    createdAtEpochMs = msgJson.optLong("createdAtEpochMs", System.currentTimeMillis()),
                    facebook = msgJson.optString("facebook").takeIf { it.isNotEmpty() },
                    instagram = msgJson.optString("instagram").takeIf { it.isNotEmpty() },
                    snapchat = msgJson.optString("snapchat").takeIf { it.isNotEmpty() }
                )
            }
        }
        null
    }

    // ==========================================
    // 2. EXTENSION REQUESTS SYNC (USER -> ADMIN)
    // ==========================================

    suspend fun publishExtensionRequest(
        inviteCode: String,
        req: ExtensionRequest
    ): Boolean = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val payload = JSONObject().apply {
            put("type", "EXTENSION_REQUEST")
            put("id", req.id)
            put("userId", req.userId)
            put("userName", req.userName)
            put("requestedMinutes", req.requestedMinutes)
            put("reason", req.reason)
            put("status", req.status.name)
            put("requestedAtEpochMs", req.requestedAtEpochMs)
            put("usageSecondsAtRequest", req.usageSecondsAtRequest)
            put("inviteCode", inviteCode)
        }
        postEvent(topic, payload.toString())
    }

    suspend fun pollExtensionRequests(inviteCode: String): List<ExtensionRequest> = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val events = pollEvents(topic)
        val requestsMap = mutableMapOf<String, ExtensionRequest>()
        for (msg in events) {
            if (msg.optString("type") == "EXTENSION_REQUEST") {
                val req = ExtensionRequest(
                    id = msg.getString("id"),
                    userId = msg.getString("userId"),
                    userName = msg.getString("userName"),
                    requestedMinutes = msg.getInt("requestedMinutes"),
                    reason = msg.getString("reason"),
                    status = ExtensionStatus.valueOf(msg.optString("status", ExtensionStatus.PENDING.name)),
                    requestedAtEpochMs = msg.getLong("requestedAtEpochMs"),
                    usageSecondsAtRequest = msg.optLong("usageSecondsAtRequest", 0L),
                    inviteCode = inviteCode
                )
                requestsMap[req.id] = req
            }
        }
        requestsMap.values.toList()
    }

    // ==========================================
    // 3. EXTENSION DECISION SYNC (ADMIN -> USER)
    // ==========================================

    suspend fun publishExtensionDecision(
        inviteCode: String,
        requestId: String,
        status: ExtensionStatus,
        grantedMinutes: Int,
        adminNotes: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val payload = JSONObject().apply {
            put("type", "EXTENSION_DECISION")
            put("requestId", requestId)
            put("status", status.name)
            put("grantedMinutes", grantedMinutes)
            put("adminNotes", adminNotes ?: "")
            put("respondedAtEpochMs", System.currentTimeMillis())
        }
        postEvent(topic, payload.toString())
    }

    suspend fun pollExtensionDecisions(inviteCode: String): List<RemoteExtensionDecision> = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val events = pollEvents(topic)
        val decisionsMap = mutableMapOf<String, RemoteExtensionDecision>()
        for (msg in events) {
            if (msg.optString("type") == "EXTENSION_DECISION") {
                val dec = RemoteExtensionDecision(
                    requestId = msg.getString("requestId"),
                    status = ExtensionStatus.valueOf(msg.getString("status")),
                    grantedMinutes = msg.getInt("grantedMinutes"),
                    adminNotes = msg.optString("adminNotes").takeIf { it.isNotEmpty() },
                    respondedAtEpochMs = msg.optLong("respondedAtEpochMs", System.currentTimeMillis())
                )
                decisionsMap[dec.requestId] = dec
            }
        }
        decisionsMap.values.toList()
    }

    // ==========================================
    // 4. ADMIN LIMIT MODIFICATION SYNC (ADMIN -> USER)
    // ==========================================

    suspend fun publishLimitUpdate(
        inviteCode: String,
        userId: String,
        newMinutes: Int,
        reason: String
    ): Boolean = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val payload = JSONObject().apply {
            put("type", "LIMIT_UPDATE")
            put("userId", userId)
            put("dailyLimitMinutes", newMinutes)
            put("reason", reason)
            put("updatedAtEpochMs", System.currentTimeMillis())
        }
        postEvent(topic, payload.toString())
    }

    suspend fun pollLimitUpdates(inviteCode: String): List<RemoteLimitUpdate> = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val events = pollEvents(topic)
        val updates = mutableListOf<RemoteLimitUpdate>()
        for (msg in events) {
            if (msg.optString("type") == "LIMIT_UPDATE") {
                updates.add(
                    RemoteLimitUpdate(
                        userId = msg.getString("userId"),
                        dailyLimitMinutes = msg.getInt("dailyLimitMinutes"),
                        reason = msg.optString("reason", "Admin policy update"),
                        updatedAtEpochMs = msg.optLong("updatedAtEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }
        updates
    }

    // ==========================================
    // 5. ACCOUNT DELETION SYNC
    // ==========================================

    suspend fun publishDeletionRequest(
        inviteCode: String,
        req: AccountDeletionRequest
    ): Boolean = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val payload = JSONObject().apply {
            put("type", "DELETION_REQUEST")
            put("id", req.id)
            put("userId", req.userId)
            put("userEmail", req.userEmail)
            put("userName", req.userName)
            put("reason", req.reason)
            put("status", req.status.name)
            put("requestedAtEpochMs", req.requestedAtEpochMs)
        }
        postEvent(topic, payload.toString())
    }

    suspend fun pollDeletionRequests(inviteCode: String): List<AccountDeletionRequest> = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val events = pollEvents(topic)
        val requests = mutableListOf<AccountDeletionRequest>()
        for (msg in events) {
            if (msg.optString("type") == "DELETION_REQUEST") {
                requests.add(
                    AccountDeletionRequest(
                        id = msg.getString("id"),
                        userId = msg.getString("userId"),
                        userEmail = msg.getString("userEmail"),
                        userName = msg.getString("userName"),
                        reason = msg.getString("reason"),
                        status = DeletionRequestStatus.valueOf(msg.optString("status", DeletionRequestStatus.PENDING.name)),
                        requestedAtEpochMs = msg.getLong("requestedAtEpochMs")
                    )
                )
            }
        }
        requests
    }

    suspend fun publishDeletionDecision(
        inviteCode: String,
        requestId: String,
        approved: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val payload = JSONObject().apply {
            put("type", "DELETION_DECISION")
            put("requestId", requestId)
            put("approved", approved)
            put("decidedAtEpochMs", System.currentTimeMillis())
        }
        postEvent(topic, payload.toString())
    }

    suspend fun pollDeletionDecisions(inviteCode: String): List<RemoteDeletionDecision> = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val events = pollEvents(topic)
        val decisions = mutableListOf<RemoteDeletionDecision>()
        for (msg in events) {
            if (msg.optString("type") == "DELETION_DECISION") {
                decisions.add(
                    RemoteDeletionDecision(
                        requestId = msg.getString("requestId"),
                        approved = msg.getBoolean("approved"),
                        decidedAtEpochMs = msg.optLong("decidedAtEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }
        decisions
    }

    // ==========================================
    // 6. USAGE TELEMETRY SYNC (USER -> ADMIN)
    // ==========================================

    suspend fun publishUsageTelemetry(
        inviteCode: String,
        telemetry: RemoteUsageTelemetry
    ): Boolean = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val payload = JSONObject().apply {
            put("type", "USAGE_TELEMETRY")
            put("userId", telemetry.userId)
            put("userEmail", telemetry.userEmail ?: "")
            put("inviteCode", telemetry.inviteCode ?: inviteCode)
            put("fbSec", telemetry.facebookSeconds)
            put("igSec", telemetry.instagramSeconds)
            put("scSec", telemetry.snapchatSeconds)
            put("totalSec", telemetry.totalUsedSeconds)
            put("timestampEpochMs", telemetry.timestampEpochMs)
        }
        postEvent(topic, payload.toString())
    }

    suspend fun pollAllEventsForInvite(inviteCode: String): InviteTopicEvents = withContext(Dispatchers.IO) {
        val topic = getTopicForInvite(inviteCode)
        val rawList = pollEvents(topic)
        val regs = mutableListOf<RemoteRegistrationPayload>()
        val extReqs = mutableMapOf<String, ExtensionRequest>()
        val extDecs = mutableMapOf<String, RemoteExtensionDecision>()
        val limitUpds = mutableListOf<RemoteLimitUpdate>()
        val delReqs = mutableListOf<AccountDeletionRequest>()
        val delDecs = mutableListOf<RemoteDeletionDecision>()
        val telemetries = mutableMapOf<String, RemoteUsageTelemetry>()

        for (msg in rawList) {
            when (msg.optString("type")) {
                "REGISTRATION" -> {
                    try {
                        regs.add(
                            RemoteRegistrationPayload(
                                inviteCode = msg.getString("inviteCode"),
                                userId = msg.getString("userId"),
                                fullName = msg.getString("fullName"),
                                email = msg.getString("email"),
                                limitMinutes = msg.getInt("limitMinutes"),
                                createdAtEpochMs = msg.optLong("createdAtEpochMs", System.currentTimeMillis()),
                                facebook = msg.optString("facebook").takeIf { it.isNotEmpty() },
                                instagram = msg.optString("instagram").takeIf { it.isNotEmpty() },
                                snapchat = msg.optString("snapchat").takeIf { it.isNotEmpty() }
                            )
                        )
                    } catch (_: Exception) {}
                }
                "EXTENSION_REQUEST" -> {
                    try {
                        val req = ExtensionRequest(
                            id = msg.getString("id"),
                            userId = msg.getString("userId"),
                            userName = msg.getString("userName"),
                            requestedMinutes = msg.getInt("requestedMinutes"),
                            reason = msg.getString("reason"),
                            status = ExtensionStatus.valueOf(msg.optString("status", ExtensionStatus.PENDING.name)),
                            requestedAtEpochMs = msg.getLong("requestedAtEpochMs"),
                            usageSecondsAtRequest = msg.optLong("usageSecondsAtRequest", 0L),
                            inviteCode = inviteCode
                        )
                        extReqs[req.id] = req
                    } catch (_: Exception) {}
                }
                "EXTENSION_DECISION" -> {
                    try {
                        val dec = RemoteExtensionDecision(
                            requestId = msg.getString("requestId"),
                            status = ExtensionStatus.valueOf(msg.getString("status")),
                            grantedMinutes = msg.getInt("grantedMinutes"),
                            adminNotes = msg.optString("adminNotes").takeIf { it.isNotEmpty() },
                            respondedAtEpochMs = msg.optLong("respondedAtEpochMs", System.currentTimeMillis())
                        )
                        extDecs[dec.requestId] = dec
                    } catch (_: Exception) {}
                }
                "LIMIT_UPDATE" -> {
                    try {
                        limitUpds.add(
                            RemoteLimitUpdate(
                                userId = msg.getString("userId"),
                                dailyLimitMinutes = msg.getInt("dailyLimitMinutes"),
                                reason = msg.optString("reason", "Admin policy update"),
                                updatedAtEpochMs = msg.optLong("updatedAtEpochMs", System.currentTimeMillis())
                            )
                        )
                    } catch (_: Exception) {}
                }
                "DELETION_REQUEST" -> {
                    try {
                        delReqs.add(
                            AccountDeletionRequest(
                                id = msg.getString("id"),
                                userId = msg.getString("userId"),
                                userEmail = msg.getString("userEmail"),
                                userName = msg.getString("userName"),
                                reason = msg.getString("reason"),
                                status = DeletionRequestStatus.valueOf(msg.optString("status", DeletionRequestStatus.PENDING.name)),
                                requestedAtEpochMs = msg.getLong("requestedAtEpochMs")
                            )
                        )
                    } catch (_: Exception) {}
                }
                "DELETION_DECISION" -> {
                    try {
                        delDecs.add(
                            RemoteDeletionDecision(
                                requestId = msg.getString("requestId"),
                                approved = msg.getBoolean("approved"),
                                decidedAtEpochMs = msg.optLong("decidedAtEpochMs", System.currentTimeMillis())
                            )
                        )
                    } catch (_: Exception) {}
                }
                "USAGE_TELEMETRY" -> {
                    try {
                        val uid = msg.getString("userId")
                        val email = msg.optString("userEmail").takeIf { it.isNotEmpty() }
                        val inv = msg.optString("inviteCode").takeIf { it.isNotEmpty() } ?: inviteCode
                        val tel = RemoteUsageTelemetry(
                            userId = uid,
                            userEmail = email,
                            inviteCode = inv,
                            facebookSeconds = msg.optLong("fbSec", 0L),
                            instagramSeconds = msg.optLong("igSec", 0L),
                            snapchatSeconds = msg.optLong("scSec", 0L),
                            totalUsedSeconds = msg.optLong("totalSec", 0L),
                            timestampEpochMs = msg.optLong("timestampEpochMs", System.currentTimeMillis())
                        )
                        val existing = telemetries[uid]
                        if (existing == null || tel.timestampEpochMs >= existing.timestampEpochMs) {
                            telemetries[uid] = tel
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        InviteTopicEvents(
            registrations = regs,
            extensionRequests = extReqs.values.toList(),
            extensionDecisions = extDecs.values.toList(),
            limitUpdates = limitUpds,
            deletionRequests = delReqs,
            deletionDecisions = delDecs,
            usageTelemetries = telemetries.values.toList()
        )
    }

    suspend fun pollUsageTelemetry(inviteCode: String): List<RemoteUsageTelemetry> = withContext(Dispatchers.IO) {
        pollAllEventsForInvite(inviteCode).usageTelemetries
    }

    // ==========================================
    // 7. OFFLINE AIR-GAPPED SYNC CODE (Cross-Platform Base64)
    // ==========================================

    fun generateOfflineSyncCode(
        user: UserAccount,
        inviteCode: String,
        limitMinutes: Int,
        telemetry: RemoteUsageTelemetry? = null
    ): String {
        val json = JSONObject().apply {
            put("inv", inviteCode)
            put("id", user.id)
            put("n", user.fullName)
            put("e", user.email)
            put("l", limitMinutes)
            put("t", user.createdAtEpochMs)
            if (telemetry != null) {
                put("fb", telemetry.facebookSeconds)
                put("ig", telemetry.instagramSeconds)
                put("sc", telemetry.snapchatSeconds)
                put("tot", telemetry.totalUsedSeconds)
                put("telT", telemetry.timestampEpochMs)
            }
        }
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
            json.toString().toByteArray(Charsets.UTF_8)
        )
        return "LCR-$encoded"
    }

    fun parseOfflineSyncCode(syncCode: String): RemoteRegistrationPayload? {
        return try {
            val raw = syncCode.trim().removePrefix("LCR-").trim()
            val decoded = Base64.getUrlDecoder().decode(raw)
            val json = JSONObject(String(decoded, Charsets.UTF_8))
            val userId = json.getString("id")
            val tel = if (json.has("tot")) {
                RemoteUsageTelemetry(
                    userId = userId,
                    userEmail = json.getString("e"),
                    inviteCode = json.getString("inv"),
                    facebookSeconds = json.optLong("fb", 0L),
                    instagramSeconds = json.optLong("ig", 0L),
                    snapchatSeconds = json.optLong("sc", 0L),
                    totalUsedSeconds = json.optLong("tot", 0L),
                    timestampEpochMs = json.optLong("telT", System.currentTimeMillis())
                )
            } else null

            RemoteRegistrationPayload(
                inviteCode = json.getString("inv"),
                userId = userId,
                fullName = json.getString("n"),
                email = json.getString("e"),
                limitMinutes = json.getInt("l"),
                createdAtEpochMs = json.optLong("t", System.currentTimeMillis()),
                usageTelemetry = tel
            )
        } catch (_: Exception) {
            null
        }
    }
}
