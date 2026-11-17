package com.limitcrosses.app.data.repository

import com.limitcrosses.app.core.model.*
import com.limitcrosses.app.data.remote.BackendClient
import com.limitcrosses.app.domain.repository.AdminRepository

class AdminRepositoryImpl(
    private val backendClient: BackendClient
) : AdminRepository {

    override suspend fun generateInvite(
        adminId: String,
        limitMinutes: Int,
        validDays: Int,
        notes: String
    ): Result<InviteCode> {
        return backendClient.adminGenerateInvite(adminId, limitMinutes, validDays, notes)
    }

    override suspend fun listManagedUsers(): Result<List<UserAccount>> {
        return backendClient.listManagedUsers()
    }

    override suspend fun listPendingExtensions(): Result<List<ExtensionRequest>> {
        return backendClient.listPendingExtensions()
    }

    override suspend fun listAllExtensions(): Result<List<ExtensionRequest>> {
        return backendClient.listAllExtensions()
    }

    override suspend fun listPendingDeletionRequests(): Result<List<AccountDeletionRequest>> {
        return backendClient.listPendingDeletionRequests()
    }

    override suspend fun reviewDeletionRequest(
        adminId: String,
        requestId: String,
        approved: Boolean,
        notes: String?
    ): Result<Unit> {
        return backendClient.adminReviewDeletionRequest(adminId, requestId, approved, notes)
    }

    override suspend fun reviewExtension(
        adminId: String,
        requestId: String,
        newStatus: ExtensionStatus,
        grantedMinutes: Int,
        adminNotes: String?
    ): Result<ExtensionRequest> {
        return backendClient.adminReviewExtension(adminId, requestId, newStatus, grantedMinutes, adminNotes)
    }

    override suspend fun modifyUserLimit(
        adminId: String,
        targetUserId: String,
        newMinutes: Int,
        reason: String
    ): Result<DailyLimitConfig> {
        return backendClient.adminModifyLimit(adminId, targetUserId, newMinutes, reason)
    }

    override suspend fun listAuditLogs(): Result<List<AuditLogEntry>> {
        return backendClient.listAuditLogs()
    }

    override suspend fun listAllInvites(): Result<List<InviteCode>> {
        return backendClient.listAllInvites()
    }

    override suspend fun syncRemoteRegistrations(): Result<Int> {
        return backendClient.syncRemoteRegistrations()
    }

    override suspend fun importOfflineRegistration(syncCode: String): Result<UserAccount> {
        return backendClient.importOfflineRegistration(syncCode)
    }
}

