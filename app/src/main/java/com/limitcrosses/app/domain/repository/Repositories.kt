package com.limitcrosses.app.domain.repository

import com.limitcrosses.app.core.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUserFlow(): Flow<UserAccount?>
    suspend fun getActiveUser(): UserAccount?
    suspend fun refreshCurrentUser(): UserAccount?
    suspend fun login(email: String, passwordPlain: String): Result<UserAccount>
    suspend fun bootstrapAdmin(fullName: String, email: String, passwordPlain: String, secretKey: String): Result<UserAccount>
    suspend fun validateInvite(code: String): Result<InviteCode>
    suspend fun register(
        fullName: String,
        email: String,
        passwordPlain: String,
        inviteCode: String,
        socialHandles: SocialHandles
    ): Result<UserAccount>
    suspend fun logout(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun requestAccountDeletion(reason: String): Result<AccountDeletionRequest>
    suspend fun cancelAccountDeletion(): Result<Unit>
    suspend fun getPendingDeletionRequest(): Result<AccountDeletionRequest?>
}

interface UsageRepository {
    fun getCombinedUsageFlow(): Flow<CombinedUsageReport>
    suspend fun refreshUsage(): CombinedUsageReport
    suspend fun isTrackingActive(): Boolean
    suspend fun getResetTimeMillis(): Long
    fun getDailyHistory(): List<DailyHistoryItem>
    fun setSimulatedLimitReached(reached: Boolean)
}

data class DailyHistoryItem(
    val dateString: String,
    val usedMinutes: Int,
    val limitMinutes: Int,
    val reachedCap: Boolean
)

interface LimitRepository {
    fun getLimitConfigFlow(userId: String): Flow<DailyLimitConfig?>
    suspend fun fetchLimitConfig(userId: String): DailyLimitConfig
    suspend fun confirmAndLockLimit(userId: String, minutes: Int): Result<DailyLimitConfig>
}

interface ExtensionRepository {
    fun getExtensionRequestsFlow(userId: String): Flow<List<ExtensionRequest>>
    suspend fun submitRequest(userId: String, minutes: Int, reason: String): Result<ExtensionRequest>
    suspend fun verifyPuzzleCompletion(userId: String, passedAllFive: Boolean): Result<Unit>
    suspend fun isEligibleToRequest(userId: String): Boolean
}

interface AdminRepository {
    suspend fun generateInvite(adminId: String, limitMinutes: Int, validDays: Int, notes: String): Result<InviteCode>
    suspend fun listManagedUsers(): Result<List<UserAccount>>
    suspend fun listPendingExtensions(): Result<List<ExtensionRequest>>
    suspend fun listAllExtensions(): Result<List<ExtensionRequest>>
    suspend fun reviewExtension(
        adminId: String,
        requestId: String,
        newStatus: ExtensionStatus,
        grantedMinutes: Int,
        adminNotes: String?
    ): Result<ExtensionRequest>
    suspend fun modifyUserLimit(adminId: String, targetUserId: String, newMinutes: Int, reason: String): Result<DailyLimitConfig>
    suspend fun listAuditLogs(): Result<List<AuditLogEntry>>
    suspend fun listAllInvites(): Result<List<InviteCode>>
    suspend fun syncRemoteRegistrations(): Result<Int>
    suspend fun importOfflineRegistration(syncCode: String): Result<UserAccount>
    suspend fun listPendingDeletionRequests(): Result<List<AccountDeletionRequest>>
    suspend fun reviewDeletionRequest(adminId: String, requestId: String, approved: Boolean, notes: String?): Result<Unit>
}

interface CosmosRepository {
    fun getTopics(category: CosmosCategory): List<CosmosTopic>
    fun getTopicById(id: String): CosmosTopic?
    fun getAllCategories(): List<CosmosCategory>
}

