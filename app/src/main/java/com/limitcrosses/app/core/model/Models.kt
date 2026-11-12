package com.limitcrosses.app.core.model

enum class UserRole {
    ADMIN,
    USER
}

enum class UserStatus {
    ACTIVE,
    PENDING_LIMIT_SETUP,
    SUSPENDED,
    DELETED
}

data class SocialHandles(
    val facebook: String? = null,
    val instagram: String? = null,
    val snapchat: String? = null
)

data class UserAccount(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val status: UserStatus,
    val socialHandles: SocialHandles = SocialHandles(),
    val createdAtEpochMs: Long,
    val lastActiveEpochMs: Long = createdAtEpochMs
)

data class InviteCode(
    val code: String,
    val createdByAdminId: String,
    val assignedDefaultLimitMinutes: Int = 30,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val isRedeemed: Boolean = false,
    val redeemedByUserId: String? = null,
    val notes: String = ""
)

data class DailyLimitConfig(
    val userId: String,
    val dailyLimitMinutes: Int, // 0..45
    val isLocked: Boolean = false,
    val lockedAtEpochMs: Long? = null,
    val lastModifiedBy: String = userId,
    val lastModifiedReason: String? = null
)

data class AppUsageInfo(
    val packageName: String,
    val displayName: String,
    val usageSeconds: Long,
    val lastTimeUsedEpochMs: Long,
    val targetDomain: String
)

data class CombinedUsageReport(
    val dailyLimitMinutes: Int,
    val totalUsedSeconds: Long,
    val remainingSeconds: Long,
    val isLimitReached: Boolean,
    val resetTimeEpochMs: Long,
    val apps: List<AppUsageInfo>,
    val trackingActive: Boolean,
    val lastPolledEpochMs: Long
)

enum class ExtensionStatus {
    PENDING,
    APPROVED,
    DENIED,
    MODIFIED
}

data class ExtensionRequest(
    val id: String,
    val userId: String,
    val userName: String,
    val requestedMinutes: Int,
    val reason: String,
    val status: ExtensionStatus = ExtensionStatus.PENDING,
    val grantedMinutes: Int = 0,
    val adminNotes: String? = null,
    val requestedAtEpochMs: Long,
    val respondedAtEpochMs: Long? = null,
    val expiresAtEpochMs: Long? = null
)

enum class AuditActionType {
    ADMIN_BOOTSTRAP,
    INVITE_GENERATED,
    USER_REGISTERED,
    USER_LOGIN,
    LIMIT_INITIALIZED,
    LIMIT_LOCKED,
    LIMIT_MODIFIED_BY_ADMIN,
    PUZZLES_COMPLETED,
    EXTENSION_REQUESTED,
    EXTENSION_DECIDED,
    ACCOUNT_DELETED,
    PERMISSION_CHANGED
}

data class AuditLogEntry(
    val id: String,
    val actorId: String,
    val actorRole: UserRole,
    val actionType: AuditActionType,
    val description: String,
    val timestampEpochMs: Long,
    val metadata: Map<String, String> = emptyMap()
)

data class PuzzleQuestion(
    val level: Int,
    val prompt: String,
    val promptHi: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val explanationHi: String,
    val accessibilityHint: String,
    val accessibilityHintHi: String
)

enum class CosmosCategory {
    ALL,
    UNIVERSE,
    GALAXIES,
    STARS,
    BLACK_HOLES,
    PLANETS,
    MISSIONS
}

data class CosmosTopic(
    val id: String,
    val title: String,
    val titleHi: String,
    val category: CosmosCategory,
    val summary: String,
    val summaryHi: String,
    val fullDescription: String,
    val fullDescriptionHi: String,
    val sourceName: String, // NASA or ESA
    val sourceUrl: String,
    val lastVerifiedDate: String
)

data class PermissionState(
    val hasUsageStats: Boolean = false,
    val hasNotifications: Boolean = false,
    val hasVpn: Boolean = false
)

