package com.limitcrosses.app.data.remote

import com.limitcrosses.app.core.model.*
import kotlinx.coroutines.flow.Flow

interface BackendClient {
    val isCloudMode: Boolean

    suspend fun bootstrapAdmin(fullName: String, email: String, passwordPlain: String, secretKey: String): Result<UserAccount>
    suspend fun login(email: String, passwordPlain: String): Result<UserAccount>
    suspend fun validateInvite(code: String): Result<InviteCode>
    suspend fun registerUser(
        fullName: String,
        email: String,
        passwordPlain: String,
        inviteCode: String,
        socialHandles: SocialHandles
    ): Result<UserAccount>

    suspend fun getDailyLimit(userId: String): Result<DailyLimitConfig>
    suspend fun setInitialLimit(userId: String, minutes: Int): Result<DailyLimitConfig>
    suspend fun adminModifyLimit(
        adminId: String,
        targetUserId: String,
        newMinutes: Int,
        auditReason: String
    ): Result<DailyLimitConfig>

    suspend fun submitExtensionRequest(
        userId: String,
        requestedMinutes: Int,
        reason: String
    ): Result<ExtensionRequest>

    suspend fun adminReviewExtension(
        adminId: String,
        requestId: String,
        newStatus: ExtensionStatus,
        grantedMinutes: Int,
        adminNotes: String?
    ): Result<ExtensionRequest>

    suspend fun adminGenerateInvite(
        adminId: String,
        defaultLimitMinutes: Int,
        validDays: Int,
        notes: String
    ): Result<InviteCode>

    suspend fun listManagedUsers(): Result<List<UserAccount>>
    suspend fun listPendingExtensions(): Result<List<ExtensionRequest>>
    suspend fun listAllInvites(): Result<List<InviteCode>>
    suspend fun listAuditLogs(): Result<List<AuditLogEntry>>

    suspend fun deleteAccount(userId: String): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
}

