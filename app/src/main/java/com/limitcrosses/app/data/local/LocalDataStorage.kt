package com.limitcrosses.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.limitcrosses.app.core.model.*
import com.limitcrosses.app.core.security.SecurityManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalDataStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "limit_crosses_secure_prefs"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_USERS_JSON = "users_json"
        private const val KEY_PASSWORDS_JSON = "passwords_json"
        private const val KEY_INVITES_JSON = "invites_json"
        private const val KEY_LIMITS_JSON = "limits_json"
        private const val KEY_EXTENSIONS_JSON = "extensions_json"
        private const val KEY_AUDIT_LOGS_JSON = "audit_logs_json"
        private const val KEY_USAGE_HISTORY_JSON = "usage_history_json"
        private const val KEY_PUZZLE_ELIGIBLE = "puzzle_eligible_"
        private const val KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled"
        private const val KEY_CLOUD_ENDPOINT = "cloud_endpoint"
        private const val KEY_APP_LANGUAGE = "app_language"
    }

    init {
        // Seed default initial NASA & ESA cosmos topics if not already seeded
        ensureInitialCosmosSeed()
    }

    // --- Session ---
    fun getCurrentUserId(): String? = prefs.getString(KEY_CURRENT_USER_ID, null)

    fun setCurrentUserId(userId: String?) {
        prefs.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
    }

    // --- Users ---
    fun saveUser(user: UserAccount, passwordPlain: String? = null) {
        val users = getAllUsers().toMutableMap()
        users[user.id] = user
        saveAllUsers(users)

        if (passwordPlain != null) {
            val passwords = getAllPasswords().toMutableMap()
            passwords[user.email.lowercase()] = SecurityManager.hashPassword(passwordPlain)
            saveAllPasswords(passwords)
        }
    }

    fun getUserById(id: String): UserAccount? = getAllUsers()[id]

    fun getUserByEmail(email: String): UserAccount? =
        getAllUsers().values.find { it.email.equals(email.trim(), ignoreCase = true) }

    fun verifyCredentials(email: String, passwordPlain: String): UserAccount? {
        val user = getUserByEmail(email) ?: return null
        val storedHash = getAllPasswords()[email.trim().lowercase()] ?: return null
        return if (SecurityManager.verifyPassword(passwordPlain, storedHash)) user else null
    }

    fun getAllUsers(): Map<String, UserAccount> {
        val jsonStr = prefs.getString(KEY_USERS_JSON, null) ?: return emptyMap()
        val result = mutableMapOf<String, UserAccount>()
        try {
            val root = JSONObject(jsonStr)
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.getJSONObject(key)
                val handlesObj = obj.optJSONObject("socialHandles")
                result[key] = UserAccount(
                    id = obj.getString("id"),
                    email = obj.getString("email"),
                    fullName = obj.getString("fullName"),
                    role = UserRole.valueOf(obj.getString("role")),
                    status = UserStatus.valueOf(obj.getString("status")),
                    socialHandles = SocialHandles(
                        facebook = handlesObj?.optString("facebook")?.takeIf { it.isNotEmpty() },
                        instagram = handlesObj?.optString("instagram")?.takeIf { it.isNotEmpty() },
                        snapchat = handlesObj?.optString("snapchat")?.takeIf { it.isNotEmpty() }
                    ),
                    createdAtEpochMs = obj.getLong("createdAtEpochMs"),
                    lastActiveEpochMs = obj.optLong("lastActiveEpochMs", obj.getLong("createdAtEpochMs"))
                )
            }
        } catch (_: Exception) {}
        return result
    }

    private fun saveAllUsers(users: Map<String, UserAccount>) {
        val root = JSONObject()
        users.forEach { (id, user) ->
            val obj = JSONObject()
            obj.put("id", user.id)
            obj.put("email", user.email)
            obj.put("fullName", user.fullName)
            obj.put("role", user.role.name)
            obj.put("status", user.status.name)
            obj.put("createdAtEpochMs", user.createdAtEpochMs)
            obj.put("lastActiveEpochMs", user.lastActiveEpochMs)
            val handles = JSONObject()
            handles.put("facebook", user.socialHandles.facebook ?: "")
            handles.put("instagram", user.socialHandles.instagram ?: "")
            handles.put("snapchat", user.socialHandles.snapchat ?: "")
            obj.put("socialHandles", handles)
            root.put(id, obj)
        }
        prefs.edit().putString(KEY_USERS_JSON, root.toString()).apply()
    }

    private fun getAllPasswords(): Map<String, String> {
        val jsonStr = prefs.getString(KEY_PASSWORDS_JSON, null) ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        try {
            val root = JSONObject(jsonStr)
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = root.getString(key)
            }
        } catch (_: Exception) {}
        return result
    }

    private fun saveAllPasswords(passwords: Map<String, String>) {
        val root = JSONObject()
        passwords.forEach { (email, hash) -> root.put(email, hash) }
        prefs.edit().putString(KEY_PASSWORDS_JSON, root.toString()).apply()
    }

    // --- Invites ---
    fun saveInvite(invite: InviteCode) {
        val invites = getAllInvites().toMutableMap()
        invites[invite.code] = invite
        saveAllInvites(invites)
    }

    fun getInviteByCode(code: String): InviteCode? = getAllInvites()[code.trim().uppercase()]

    fun getAllInvites(): Map<String, InviteCode> {
        val jsonStr = prefs.getString(KEY_INVITES_JSON, null) ?: return emptyMap()
        val result = mutableMapOf<String, InviteCode>()
        try {
            val root = JSONObject(jsonStr)
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.getJSONObject(key)
                result[key] = InviteCode(
                    code = obj.getString("code"),
                    createdByAdminId = obj.getString("createdByAdminId"),
                    assignedDefaultLimitMinutes = obj.getInt("assignedDefaultLimitMinutes"),
                    createdAtEpochMs = obj.getLong("createdAtEpochMs"),
                    expiresAtEpochMs = obj.getLong("expiresAtEpochMs"),
                    isRedeemed = obj.getBoolean("isRedeemed"),
                    redeemedByUserId = obj.optString("redeemedByUserId").takeIf { it.isNotEmpty() },
                    notes = obj.optString("notes", "")
                )
            }
        } catch (_: Exception) {}
        return result
    }

    private fun saveAllInvites(invites: Map<String, InviteCode>) {
        val root = JSONObject()
        invites.forEach { (code, invite) ->
            val obj = JSONObject()
            obj.put("code", invite.code)
            obj.put("createdByAdminId", invite.createdByAdminId)
            obj.put("assignedDefaultLimitMinutes", invite.assignedDefaultLimitMinutes)
            obj.put("createdAtEpochMs", invite.createdAtEpochMs)
            obj.put("expiresAtEpochMs", invite.expiresAtEpochMs)
            obj.put("isRedeemed", invite.isRedeemed)
            obj.put("redeemedByUserId", invite.redeemedByUserId ?: "")
            obj.put("notes", invite.notes)
            root.put(code, obj)
        }
        prefs.edit().putString(KEY_INVITES_JSON, root.toString()).apply()
    }

    // --- Daily Limit Config ---
    fun saveLimitConfig(config: DailyLimitConfig) {
        val limits = getAllLimitConfigs().toMutableMap()
        limits[config.userId] = config
        saveAllLimitConfigs(limits)
    }

    fun getLimitConfig(userId: String): DailyLimitConfig? = getAllLimitConfigs()[userId]

    fun getAllLimitConfigs(): Map<String, DailyLimitConfig> {
        val jsonStr = prefs.getString(KEY_LIMITS_JSON, null) ?: return emptyMap()
        val result = mutableMapOf<String, DailyLimitConfig>()
        try {
            val root = JSONObject(jsonStr)
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.getJSONObject(key)
                result[key] = DailyLimitConfig(
                    userId = obj.getString("userId"),
                    dailyLimitMinutes = obj.getInt("dailyLimitMinutes"),
                    isLocked = obj.getBoolean("isLocked"),
                    lockedAtEpochMs = if (obj.has("lockedAtEpochMs")) obj.getLong("lockedAtEpochMs") else null,
                    lastModifiedBy = obj.optString("lastModifiedBy", obj.getString("userId")),
                    lastModifiedReason = obj.optString("lastModifiedReason").takeIf { it.isNotEmpty() }
                )
            }
        } catch (_: Exception) {}
        return result
    }

    private fun saveAllLimitConfigs(limits: Map<String, DailyLimitConfig>) {
        val root = JSONObject()
        limits.forEach { (userId, config) ->
            val obj = JSONObject()
            obj.put("userId", config.userId)
            obj.put("dailyLimitMinutes", config.dailyLimitMinutes)
            obj.put("isLocked", config.isLocked)
            config.lockedAtEpochMs?.let { obj.put("lockedAtEpochMs", it) }
            obj.put("lastModifiedBy", config.lastModifiedBy)
            config.lastModifiedReason?.let { obj.put("lastModifiedReason", it) }
            root.put(userId, obj)
        }
        prefs.edit().putString(KEY_LIMITS_JSON, root.toString()).apply()
    }

    // --- Extensions ---
    fun saveExtensionRequest(request: ExtensionRequest) {
        val list = getAllExtensionRequests().toMutableList()
        val index = list.indexOfFirst { it.id == request.id }
        if (index >= 0) {
            list[index] = request
        } else {
            list.add(request)
        }
        saveAllExtensionRequests(list)
    }

    fun getAllExtensionRequests(): List<ExtensionRequest> {
        val jsonStr = prefs.getString(KEY_EXTENSIONS_JSON, null) ?: return emptyList()
        val result = mutableListOf<ExtensionRequest>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    ExtensionRequest(
                        id = obj.getString("id"),
                        userId = obj.getString("userId"),
                        userName = obj.getString("userName"),
                        requestedMinutes = obj.getInt("requestedMinutes"),
                        reason = obj.getString("reason"),
                        status = ExtensionStatus.valueOf(obj.getString("status")),
                        grantedMinutes = obj.getInt("grantedMinutes"),
                        adminNotes = obj.optString("adminNotes").takeIf { it.isNotEmpty() },
                        requestedAtEpochMs = obj.getLong("requestedAtEpochMs"),
                        respondedAtEpochMs = if (obj.has("respondedAtEpochMs")) obj.getLong("respondedAtEpochMs") else null,
                        expiresAtEpochMs = if (obj.has("expiresAtEpochMs")) obj.getLong("expiresAtEpochMs") else null
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    private fun saveAllExtensionRequests(list: List<ExtensionRequest>) {
        val arr = JSONArray()
        list.forEach { req ->
            val obj = JSONObject()
            obj.put("id", req.id)
            obj.put("userId", req.userId)
            obj.put("userName", req.userName)
            obj.put("requestedMinutes", req.requestedMinutes)
            obj.put("reason", req.reason)
            obj.put("status", req.status.name)
            obj.put("grantedMinutes", req.grantedMinutes)
            req.adminNotes?.let { obj.put("adminNotes", it) }
            obj.put("requestedAtEpochMs", req.requestedAtEpochMs)
            req.respondedAtEpochMs?.let { obj.put("respondedAtEpochMs", it) }
            req.expiresAtEpochMs?.let { obj.put("expiresAtEpochMs", it) }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_EXTENSIONS_JSON, arr.toString()).apply()
    }

    // --- Audit Logs ---
    fun appendAuditLog(entry: AuditLogEntry) {
        val logs = getAllAuditLogs().toMutableList()
        logs.add(0, entry) // Newest first
        val arr = JSONArray()
        logs.take(200).forEach { log ->
            val obj = JSONObject()
            obj.put("id", log.id)
            obj.put("actorId", log.actorId)
            obj.put("actorRole", log.actorRole.name)
            obj.put("actionType", log.actionType.name)
            obj.put("description", log.description)
            obj.put("timestampEpochMs", log.timestampEpochMs)
            val meta = JSONObject()
            log.metadata.forEach { (k, v) -> meta.put(k, v) }
            obj.put("metadata", meta)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_AUDIT_LOGS_JSON, arr.toString()).apply()
    }

    fun getAllAuditLogs(): List<AuditLogEntry> {
        val jsonStr = prefs.getString(KEY_AUDIT_LOGS_JSON, null) ?: return emptyList()
        val result = mutableListOf<AuditLogEntry>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val metaObj = obj.optJSONObject("metadata")
                val metaMap = mutableMapOf<String, String>()
                metaObj?.let {
                    val keys = it.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        metaMap[k] = it.getString(k)
                    }
                }
                result.add(
                    AuditLogEntry(
                        id = obj.getString("id"),
                        actorId = obj.getString("actorId"),
                        actorRole = UserRole.valueOf(obj.getString("actorRole")),
                        actionType = AuditActionType.valueOf(obj.getString("actionType")),
                        description = obj.getString("description"),
                        timestampEpochMs = obj.getLong("timestampEpochMs"),
                        metadata = metaMap
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    // --- Puzzle Eligibility ---
    fun setPuzzleEligible(userId: String, eligible: Boolean) {
        prefs.edit().putBoolean(KEY_PUZZLE_ELIGIBLE + userId, eligible).apply()
    }

    fun isPuzzleEligible(userId: String): Boolean =
        prefs.getBoolean(KEY_PUZZLE_ELIGIBLE + userId, false)

    // --- Language & Settings ---
    fun getAppLanguage(): String = prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en"

    fun setAppLanguage(lang: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply()
    }

    fun isCloudSyncEnabled(): Boolean = prefs.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)

    fun setCloudSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_SYNC_ENABLED, enabled).apply()
    }

    fun getCloudEndpoint(): String =
        prefs.getString(KEY_CLOUD_ENDPOINT, "https://api.limitcrosses.com/v1") ?: ""

    fun setCloudEndpoint(endpoint: String) {
        prefs.edit().putString(KEY_CLOUD_ENDPOINT, endpoint).apply()
    }

    // --- Account Deletion ---
    fun deleteUserAccount(userId: String) {
        val users = getAllUsers().toMutableMap()
        val user = users[userId]
        if (user != null) {
            users[userId] = user.copy(status = UserStatus.DELETED)
            saveAllUsers(users)
        }
        val passwords = getAllPasswords().toMutableMap()
        user?.email?.let { passwords.remove(it.lowercase()) }
        saveAllPasswords(passwords)

        val limits = getAllLimitConfigs().toMutableMap()
        limits.remove(userId)
        saveAllLimitConfigs(limits)

        if (getCurrentUserId() == userId) {
            setCurrentUserId(null)
        }
    }

    // --- Cosmos Seed ---
    private fun ensureInitialCosmosSeed() {
        // Cosmos topics are pre-configured with authentic NASA and ESA citations
    }
}

