package com.limitcrosses.app.data.remote

import com.limitcrosses.app.core.model.*
import com.limitcrosses.app.core.security.SecurityManager
import com.limitcrosses.app.data.local.LocalDataStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMockBackendEngine(
    private val localDataStorage: LocalDataStorage
) : BackendClient {

    override val isCloudMode: Boolean
        get() = localDataStorage.isCloudSyncEnabled()

    override suspend fun bootstrapAdmin(
        fullName: String,
        email: String,
        passwordPlain: String,
        secretKey: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        if (!SecurityManager.validateAdminBootstrap(secretKey)) {
            return@withContext Result.failure(
                SecurityException("Invalid Master Bootstrap Secret. Administrator self-registration blocked.")
            )
        }

        val existing = localDataStorage.getUserByEmail(email)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("An account with this email already exists."))
        }

        val adminUser = UserAccount(
            id = "admin-" + SecurityManager.generateUuid().take(8),
            email = email.trim(),
            fullName = fullName.trim(),
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE,
            createdAtEpochMs = System.currentTimeMillis()
        )

        localDataStorage.saveUser(adminUser, passwordPlain)

        localDataStorage.appendAuditLog(
            AuditLogEntry(
                id = SecurityManager.generateUuid(),
                actorId = adminUser.id,
                actorRole = UserRole.ADMIN,
                actionType = AuditActionType.ADMIN_BOOTSTRAP,
                description = "Primary administrator account bootstrapped with master key.",
                timestampEpochMs = System.currentTimeMillis(),
                metadata = mapOf("email" to adminUser.email)
            )
        )

        Result.success(adminUser)
    }

    override suspend fun login(email: String, passwordPlain: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val user = localDataStorage.verifyCredentials(email, passwordPlain)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid email or password."))

        if (user.status == UserStatus.DELETED) {
            return@withContext Result.failure(IllegalStateException("This account has been deleted."))
        }

        localDataStorage.appendAuditLog(
            AuditLogEntry(
                id = SecurityManager.generateUuid(),
                actorId = user.id,
                actorRole = user.role,
                actionType = AuditActionType.USER_LOGIN,
                description = "User authenticated successfully.",
                timestampEpochMs = System.currentTimeMillis()
            )
        )

        Result.success(user)
    }

    override suspend fun validateInvite(code: String): Result<InviteCode> = withContext(Dispatchers.IO) {
        val invite = localDataStorage.getInviteByCode(code)
            ?: return@withContext Result.failure(IllegalArgumentException("Invite code not found."))

        if (invite.isRedeemed) {
            return@withContext Result.failure(IllegalStateException("Invite code has already been redeemed."))
        }

        if (System.currentTimeMillis() > invite.expiresAtEpochMs) {
            return@withContext Result.failure(IllegalStateException("Invite code has expired."))
        }

        Result.success(invite)
    }

    override suspend fun registerUser(
        fullName: String,
        email: String,
        passwordPlain: String,
        inviteCode: String,
        socialHandles: SocialHandles
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        val inviteResult = validateInvite(inviteCode)
        if (inviteResult.isFailure) {
            return@withContext Result.failure(inviteResult.exceptionOrNull() ?: Exception("Invalid invite"))
        }
        val invite = inviteResult.getOrThrow()

        val existing = localDataStorage.getUserByEmail(email)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("Email is already registered."))
        }

        val userId = "usr-" + SecurityManager.generateUuid().take(8)
        val user = UserAccount(
            id = userId,
            email = email.trim(),
            fullName = fullName.trim(),
            role = UserRole.USER,
            status = UserStatus.PENDING_LIMIT_SETUP,
            socialHandles = socialHandles,
            createdAtEpochMs = System.currentTimeMillis()
        )

        // Mark invite redeemed
        val updatedInvite = invite.copy(
            isRedeemed = true,
            redeemedByUserId = userId
        )
        localDataStorage.saveInvite(updatedInvite)
        localDataStorage.saveUser(user, passwordPlain)

        // Set default limit from invite
        val limitConfig = DailyLimitConfig(
            userId = userId,
            dailyLimitMinutes = invite.assignedDefaultLimitMinutes.coerceIn(0, 45),
            isLocked = false
        )
        localDataStorage.saveLimitConfig(limitConfig)

        localDataStorage.appendAuditLog(
            AuditLogEntry(
                id = SecurityManager.generateUuid(),
                actorId = userId,
                actorRole = UserRole.USER,
                actionType = AuditActionType.USER_REGISTERED,
                description = "User registered via invite ${invite.code}.",
                timestampEpochMs = System.currentTimeMillis(),
                metadata = mapOf("inviteCode" to invite.code)
            )
        )

        Result.success(user)
    }

    override suspend fun getDailyLimit(userId: String): Result<DailyLimitConfig> = withContext(Dispatchers.IO) {
        val config = localDataStorage.getLimitConfig(userId)
            ?: DailyLimitConfig(userId = userId, dailyLimitMinutes = 30, isLocked = false)
        Result.success(config)
    }

    override suspend fun setInitialLimit(userId: String, minutes: Int): Result<DailyLimitConfig> = withContext(Dispatchers.IO) {
        val current = localDataStorage.getLimitConfig(userId)
        if (current != null && current.isLocked) {
            return@withContext Result.failure(
                IllegalStateException("Daily limit is locked. Modifications require Administrator approval.")
            )
        }

        val clamped = minutes.coerceIn(0, 45)
        val lockedConfig = DailyLimitConfig(
            userId = userId,
            dailyLimitMinutes = clamped,
            isLocked = true,
            lockedAtEpochMs = System.currentTimeMillis(),
            lastModifiedBy = userId,
            lastModifiedReason = "Initial user commitment confirmation"
        )
        localDataStorage.saveLimitConfig(lockedConfig)

        // Update user status from PENDING_LIMIT_SETUP to ACTIVE
        val user = localDataStorage.getUserById(userId)
        if (user != null && user.status == UserStatus.PENDING_LIMIT_SETUP) {
            localDataStorage.saveUser(user.copy(status = UserStatus.ACTIVE))
        }

        localDataStorage.appendAuditLog(
            AuditLogEntry(
                id = SecurityManager.generateUuid(),
                actorId = userId,
                actorRole = UserRole.USER,
                actionType = AuditActionType.LIMIT_LOCKED,
                description = "User set and permanently locked daily combined limit at $clamped minutes.",
                timestampEpochMs = System.currentTimeMillis()
            )
        )

        Result.success(lockedConfig)
    }

    override suspend fun adminModifyLimit(
        adminId: String,
        targetUserId: String,
        newMinutes: Int,
        auditReason: String
    ): Result<DailyLimitConfig> = withContext(Dispatchers.IO) {
        val admin = localDataStorage.getUserById(adminId)
        if (admin?.role != UserRole.ADMIN) {
            return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required."))
        }
        if (auditReason.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("An audit reason is required for policy changes."))
        }

        val clamped = newMinutes.coerceIn(0, 45)
        val updated = DailyLimitConfig(
            userId = targetUserId,
            dailyLimitMinutes = clamped,
            isLocked = true,
            lockedAtEpochMs = System.currentTimeMillis(),
            lastModifiedBy = adminId,
            lastModifiedReason = auditReason
        )
        localDataStorage.saveLimitConfig(updated)

        localDataStorage.appendAuditLog(
            AuditLogEntry(
                id = SecurityManager.generateUuid(),
                actorId = adminId,
                actorRole = UserRole.ADMIN,
                actionType = AuditActionType.LIMIT_MODIFIED_BY_ADMIN,
                description = "Administrator modified user limit to $clamped minutes.",
                timestampEpochMs = System.currentTimeMillis(),
                metadata = mapOf("targetUserId" to targetUserId, "reason" to auditReason)
            )
        )

        Result.success(updated)
    }

    override suspend fun submitExtensionRequest(
        userId: String,
        requestedMinutes: Int,
        reason: String
    ): Result<ExtensionRequest> = withContext(Dispatchers.IO) {
        val user = localDataStorage.getUserById(userId)
            ?: return@withContext Result.failure(IllegalArgumentException("User not found."))

        if (!localDataStorage.isPuzzleEligible(userId)) {
            return@withContext Result.failure(
                IllegalStateException("Puzzle eligibility requirement not met. Must pass 5 consecutive puzzles.")
            )
        }

        val clampedReq = requestedMinutes.coerceIn(1, 30)
        val req = ExtensionRequest(
            id = "ext-" + SecurityManager.generateUuid().take(8),
            userId = userId,
            userName = user.fullName,
            requestedMinutes = clampedReq,
            reason = reason.trim(),
            status = ExtensionStatus.PENDING,
            requestedAtEpochMs = System.currentTimeMillis()
        )
        localDataStorage.saveExtensionRequest(req)

        localDataStorage.appendAuditLog(
            AuditLogEntry(
                id = SecurityManager.generateUuid(),
                actorId = userId,
                actorRole = user.role,
                actionType = AuditActionType.EXTENSION_REQUESTED,
                description = "Submitted request for $clampedReq extra minutes: '$reason'",
                timestampEpochMs = System.currentTimeMillis()
            )
        )

        Result.success(req)
    }

    override suspend fun adminReviewExtension(
        adminId: String,
        requestId: String,
        newStatus: ExtensionStatus,
        grantedMinutes: Int,
        adminNotes: String?
    ): Result<ExtensionRequest> = withContext(Dispatchers.IO) {
        val admin = localDataStorage.getUserById(adminId)
        if (admin?.role != UserRole.ADMIN) {
            return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required."))
        }

        val all = localDataStorage.getAllExtensionRequests()
        val existing = all.find { it.id == requestId }
            ?: return@withContext Result.failure(IllegalArgumentException("Extension request not found."))

        val effectiveGranted = if (newStatus == ExtensionStatus.APPROVED || newStatus == ExtensionStatus.MODIFIED) {
            grantedMinutes.coerceIn(1, 30)
        } else {
            0
        }

        val updated = existing.copy(
            status = newStatus,
            grantedMinutes = effectiveGranted,
            adminNotes = adminNotes,
            respondedAtEpochMs = System.currentTimeMillis(),
            expiresAtEpochMs = System.currentTimeMillis() + (effectiveGranted * 60 * 1000L)
        )
        localDataStorage.saveExtensionRequest(updated)

        localDataStorage.appendAuditLog(
            AuditLogEntry(
                id = SecurityManager.generateUuid(),
                actorId = adminId,
                actorRole = UserRole.ADMIN,
                actionType = AuditActionType.EXTENSION_DECIDED,
                description = "Admin decided request ${existing.id} as $newStatus ($effectiveGranted min granted).",
                timestampEpochMs = System.currentTimeMillis(),
                metadata = mapOf("requestId" to requestId, "notes" to (adminNotes ?: ""))
            )
        )

        Result.success(updated)
    }

    override suspend fun adminGenerateInvite(
        adminId: String,
        defaultLimitMinutes: Int,
        validDays: Int,
        notes: String
    ): Result<InviteCode> = withContext(Dispatchers.IO) {
        val admin = localDataStorage.getUserById(adminId)
        if (admin?.role != UserRole.ADMIN) {
            return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required."))
        }

        val code = SecurityManager.generateInviteCode()
        val now = System.currentTimeMillis()
        val invite = InviteCode(
            code = code,
            createdByAdminId = adminId,
            assignedDefaultLimitMinutes = defaultLimitMinutes.coerceIn(0, 45),
            createdAtEpochMs = now,
            expiresAtEpochMs = now + (validDays.coerceIn(1, 90) * 24 * 60 * 60 * 1000L),
            isRedeemed = false,
            notes = notes
        )
        localDataStorage.saveInvite(invite)

        localDataStorage.appendAuditLog(
            AuditLogEntry(
                id = SecurityManager.generateUuid(),
                actorId = adminId,
                actorRole = UserRole.ADMIN,
                actionType = AuditActionType.INVITE_GENERATED,
                description = "Admin generated invitation code $code (Limit: ${invite.assignedDefaultLimitMinutes}m).",
                timestampEpochMs = now
            )
        )

        Result.success(invite)
    }

    override suspend fun listManagedUsers(): Result<List<UserAccount>> = withContext(Dispatchers.IO) {
        val users = localDataStorage.getAllUsers().values
            .filter { it.role == UserRole.USER && it.status != UserStatus.DELETED }
            .toList()
        Result.success(users)
    }

    override suspend fun listPendingExtensions(): Result<List<ExtensionRequest>> = withContext(Dispatchers.IO) {
        val pending = localDataStorage.getAllExtensionRequests().filter { it.status == ExtensionStatus.PENDING }
        Result.success(pending)
    }

    override suspend fun listAllInvites(): Result<List<InviteCode>> = withContext(Dispatchers.IO) {
        Result.success(localDataStorage.getAllInvites().values.toList())
    }

    override suspend fun listAuditLogs(): Result<List<AuditLogEntry>> = withContext(Dispatchers.IO) {
        Result.success(localDataStorage.getAllAuditLogs())
    }

    override suspend fun deleteAccount(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = localDataStorage.getUserById(userId)
        localDataStorage.deleteUserAccount(userId)

        localDataStorage.appendAuditLog(
            AuditLogEntry(
                id = SecurityManager.generateUuid(),
                actorId = userId,
                actorRole = user?.role ?: UserRole.USER,
                actionType = AuditActionType.ACCOUNT_DELETED,
                description = "Account $userId permanently deleted upon user request.",
                timestampEpochMs = System.currentTimeMillis()
            )
        )

        Result.success(Unit)
    }

    override suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        // Simulates dispatching recovery token / email without leaking account existence
        Result.success(Unit)
    }
}

