package com.limitcrosses.app.data.remote

import com.limitcrosses.app.core.model.*
import com.limitcrosses.app.core.security.SecurityManager
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.platform.service.AppBlockerOverlayManager
import com.limitcrosses.app.platform.usage.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMockBackendEngine(
    private val localDataStorage: LocalDataStorage,
    private val usageStatsHelper: UsageStatsHelper
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
        val normalizedCode = SecurityManager.normalizeInviteCode(code)
        var invite = localDataStorage.getInviteByCode(normalizedCode)
            ?: localDataStorage.getInviteByCode(code.trim().uppercase())

        if (invite == null) {
            // Check if this is an authentic, self-verifying Universal Multi-Device Invite Code
            val payload = SecurityManager.decodeAndVerifyUniversalInvite(code)
            if (payload != null) {
                val now = System.currentTimeMillis()
                if (now > payload.expiresAtEpochMs) {
                    return@withContext Result.failure(IllegalStateException("Invite code has expired."))
                }

                // Synthesize and register the verified invitation into local storage on this device
                val universalInvite = InviteCode(
                    code = normalizedCode,
                    createdByAdminId = "admin-universal",
                    assignedDefaultLimitMinutes = payload.limitMinutes,
                    createdAtEpochMs = payload.createdAtEpochMs,
                    expiresAtEpochMs = payload.expiresAtEpochMs,
                    isRedeemed = false,
                    notes = "Universal Multi-Device Invite (${payload.limitMinutes}m allowance)"
                )
                localDataStorage.saveInvite(universalInvite)
                invite = universalInvite
            }
        }

        if (invite == null) {
            return@withContext Result.failure(IllegalArgumentException("Invite code not found or invalid."))
        }

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
            registeredViaInviteCode = invite.code,
            createdAtEpochMs = System.currentTimeMillis()
        )

        // Mark invite redeemed with registrant details
        val updatedInvite = invite.copy(
            isRedeemed = true,
            redeemedByUserId = userId,
            redeemedByUserName = user.fullName,
            redeemedByUserEmail = user.email,
            redeemedAtEpochMs = System.currentTimeMillis()
        )
        localDataStorage.saveInvite(updatedInvite)
        localDataStorage.saveUser(user, passwordPlain)

        // Broadcast registration across cloud relay so the Admin device discovers it immediately
        try {
            CloudSyncRelayHelper.publishRegistration(
                inviteCode = invite.code,
                user = user,
                limitMinutes = invite.assignedDefaultLimitMinutes
            )
        } catch (_: Exception) {}

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
        if (newMinutes <= 0 || newMinutes > 45) {
            return@withContext Result.failure(
                IllegalArgumentException("Daily limit must be a positive number between 1 and 45 minutes.")
            )
        }

        val clamped = newMinutes.coerceIn(1, 45)
        val updated = DailyLimitConfig(
            userId = targetUserId,
            dailyLimitMinutes = clamped,
            isLocked = true,
            lockedAtEpochMs = System.currentTimeMillis(),
            lastModifiedBy = adminId,
            lastModifiedReason = auditReason
        )
        localDataStorage.saveLimitConfig(updated)

        // Broadcast limit change to user device
        val targetUser = localDataStorage.getUserById(targetUserId)
        val inviteCode = targetUser?.registeredViaInviteCode
        if (!inviteCode.isNullOrBlank()) {
            try {
                CloudSyncRelayHelper.publishLimitUpdate(inviteCode, targetUserId, clamped, auditReason)
            } catch (_: Exception) {}
        }

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

        val currentMonitoredUsageSeconds = try {
            usageStatsHelper.queryMonitoredAppUsage().sumOf { it.usageSeconds }
        } catch (_: Exception) {
            0L
        }
        val inviteCode = user.registeredViaInviteCode

        val clampedReq = requestedMinutes.coerceIn(1, 45)
        val req = ExtensionRequest(
            id = "ext-" + SecurityManager.generateUuid().take(8),
            userId = userId,
            userName = user.fullName,
            requestedMinutes = clampedReq,
            reason = reason.trim(),
            status = ExtensionStatus.PENDING,
            requestedAtEpochMs = System.currentTimeMillis(),
            usageSecondsAtRequest = currentMonitoredUsageSeconds,
            inviteCode = inviteCode
        )
        localDataStorage.saveExtensionRequest(req)

        // Broadcast extension request across Cloud Relay so Admin receives it immediately on any device
        if (!inviteCode.isNullOrBlank()) {
            try {
                CloudSyncRelayHelper.publishExtensionRequest(inviteCode, req)
            } catch (_: Exception) {}
        }

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
            grantedMinutes.coerceIn(1, 45)
        } else {
            0
        }

        val currentMonitoredUsageSeconds = try {
            usageStatsHelper.queryMonitoredAppUsage().sumOf { it.usageSeconds }
        } catch (_: Exception) {
            0L
        }

        // Anchor baseline to user's usage when requested or approved
        val effectiveBaseline = maxOf(existing.usageSecondsAtRequest, currentMonitoredUsageSeconds)

        val updated = existing.copy(
            status = newStatus,
            grantedMinutes = effectiveGranted,
            adminNotes = adminNotes,
            respondedAtEpochMs = System.currentTimeMillis(),
            expiresAtEpochMs = usageStatsHelper.getNextMidnightEpochMs(),
            baselineUsageSecondsAtApproval = effectiveBaseline
        )
        localDataStorage.saveExtensionRequest(updated)

        // Broadcast decision across Cloud Relay to the user's mobile device
        val inviteCode = existing.inviteCode ?: localDataStorage.getUserById(existing.userId)?.registeredViaInviteCode
        if (!inviteCode.isNullOrBlank()) {
            try {
                CloudSyncRelayHelper.publishExtensionDecision(
                    inviteCode = inviteCode,
                    requestId = requestId,
                    status = newStatus,
                    grantedMinutes = effectiveGranted,
                    adminNotes = adminNotes
                )
            } catch (_: Exception) {}
        }

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

        val clampedLimit = defaultLimitMinutes.coerceIn(0, 45)
        val clampedDays = validDays.coerceIn(1, 90)
        val code = SecurityManager.generateUniversalInviteCode(
            limitMinutes = clampedLimit,
            validDays = clampedDays
        )
        val now = System.currentTimeMillis()
        val invite = InviteCode(
            code = code,
            createdByAdminId = adminId,
            assignedDefaultLimitMinutes = clampedLimit,
            createdAtEpochMs = now,
            expiresAtEpochMs = now + (clampedDays * 24 * 60 * 60 * 1000L),
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

    override suspend fun listAllExtensions(): Result<List<ExtensionRequest>> = withContext(Dispatchers.IO) {
        Result.success(localDataStorage.getAllExtensionRequests())
    }

    override suspend fun listAllInvites(): Result<List<InviteCode>> = withContext(Dispatchers.IO) {
        val invites = localDataStorage.getAllInvites().values.map { invite ->
            if (invite.isRedeemed && (invite.redeemedByUserName.isNullOrBlank() || invite.redeemedByUserEmail.isNullOrBlank())) {
                val user = invite.redeemedByUserId?.let { localDataStorage.getUserById(it) }
                if (user != null) {
                    val fixed = invite.copy(
                        redeemedByUserName = user.fullName,
                        redeemedByUserEmail = user.email
                    )
                    localDataStorage.saveInvite(fixed)
                    fixed
                } else {
                    invite
                }
            } else {
                invite
            }
        }.sortedByDescending { it.createdAtEpochMs }
        Result.success(invites)
    }

    override suspend fun syncRemoteRegistrations(): Result<Int> = withContext(Dispatchers.IO) {
        var syncedCount = 0
        val allInvites = localDataStorage.getAllInvites().values.toList()
        for (invite in allInvites) {
            val events = try {
                CloudSyncRelayHelper.pollAllEventsForInvite(invite.code)
            } catch (_: Exception) {
                InviteTopicEvents()
            }

            // 1. Discover user registrations
            val remote = events.registrations.lastOrNull()
            if (remote != null) {
                var existingUser = localDataStorage.getUserById(remote.userId)
                    ?: localDataStorage.getUserByEmail(remote.email)
                if (existingUser == null) {
                    val newUser = UserAccount(
                        id = remote.userId,
                        email = remote.email,
                        fullName = remote.fullName,
                        role = UserRole.USER,
                        status = UserStatus.ACTIVE,
                        socialHandles = SocialHandles(
                            facebook = remote.facebook,
                            instagram = remote.instagram,
                            snapchat = remote.snapchat
                        ),
                        registeredViaInviteCode = invite.code,
                        createdAtEpochMs = remote.createdAtEpochMs,
                        lastActiveEpochMs = System.currentTimeMillis()
                    )
                    localDataStorage.saveUser(newUser)
                    localDataStorage.saveLimitConfig(
                        DailyLimitConfig(
                            userId = remote.userId,
                            dailyLimitMinutes = remote.limitMinutes.coerceIn(0, 45),
                            isLocked = true,
                            lockedAtEpochMs = remote.createdAtEpochMs,
                            lastModifiedBy = "system",
                            lastModifiedReason = "Synchronized from remote registration via invite ${invite.code}"
                        )
                    )
                    syncedCount++
                }

                if (!invite.isRedeemed || invite.redeemedByUserName.isNullOrBlank()) {
                    val updatedInvite = invite.copy(
                        isRedeemed = true,
                        redeemedByUserId = remote.userId,
                        redeemedByUserName = remote.fullName,
                        redeemedByUserEmail = remote.email,
                        redeemedAtEpochMs = remote.createdAtEpochMs
                    )
                    localDataStorage.saveInvite(updatedInvite)
                    syncedCount++
                }
            } else if (invite.isRedeemed && (invite.redeemedByUserName.isNullOrBlank() || invite.redeemedByUserEmail.isNullOrBlank())) {
                val user = invite.redeemedByUserId?.let { localDataStorage.getUserById(it) }
                if (user != null) {
                    val updatedInvite = invite.copy(
                        redeemedByUserName = user.fullName,
                        redeemedByUserEmail = user.email
                    )
                    localDataStorage.saveInvite(updatedInvite)
                }
            }

            // 2. Discover Extension Requests submitted by registered users
            for (rReq in events.extensionRequests) {
                val localReq = localDataStorage.getAllExtensionRequests().find { it.id == rReq.id }
                if (localReq == null) {
                    localDataStorage.saveExtensionRequest(rReq)
                    syncedCount++
                }
            }

            // 3. Discover Account Deletion Requests submitted by registered users
            for (rDel in events.deletionRequests) {
                val localDel = localDataStorage.getAllDeletionRequests().find { it.id == rDel.id }
                if (localDel == null) {
                    localDataStorage.saveDeletionRequest(rDel)
                    syncedCount++
                }
            }

            // 4. Discover Usage Telemetry reported by registered users
            for (tel in events.usageTelemetries) {
                val matchingUser = localDataStorage.getUserById(tel.userId)
                    ?: (tel.userEmail?.let { localDataStorage.getUserByEmail(it) })
                // Strictly accept telemetry only for registered remote users (never overwrite or echo admin)
                if (matchingUser != null && matchingUser.role != UserRole.ADMIN && !matchingUser.registeredViaInviteCode.isNullOrBlank()) {
                    val existing = localDataStorage.getUserUsageTelemetry(matchingUser.id)
                        ?: localDataStorage.getUserUsageTelemetryByEmail(matchingUser.email)
                    if (existing == null || tel.timestampEpochMs >= existing.timestampEpochMs) {
                        localDataStorage.saveUserUsageTelemetry(tel.copy(userId = matchingUser.id, userEmail = matchingUser.email), invite.code, matchingUser.email)
                        syncedCount++
                        if (tel.timestampEpochMs > matchingUser.lastActiveEpochMs) {
                            localDataStorage.saveUser(matchingUser.copy(lastActiveEpochMs = tel.timestampEpochMs))
                        }
                    }
                }
            }
        }
        Result.success(syncedCount)
    }

    override suspend fun syncUserUpdates(userId: String): Result<Int> = withContext(Dispatchers.IO) {
        val user = localDataStorage.getUserById(userId)
            ?: return@withContext Result.failure(IllegalArgumentException("User not found."))
        val inviteCode = user.registeredViaInviteCode
            ?: return@withContext Result.success(0)

        var updatesCount = 0

        val events = try {
            CloudSyncRelayHelper.pollAllEventsForInvite(inviteCode)
        } catch (_: Exception) {
            InviteTopicEvents()
        }

        // 1. Check for Extension Decisions from Admin
        for (dec in events.extensionDecisions) {
            val localReq = localDataStorage.getAllExtensionRequests().find { it.id == dec.requestId }
            if (localReq != null && localReq.status != dec.status) {
                val currentMonitoredUsageSeconds = try {
                    usageStatsHelper.queryMonitoredAppUsage().sumOf { it.usageSeconds }
                } catch (_: Exception) {
                    0L
                }
                val baseline = maxOf(localReq.usageSecondsAtRequest, currentMonitoredUsageSeconds)
                val updated = localReq.copy(
                    status = dec.status,
                    grantedMinutes = dec.grantedMinutes,
                    adminNotes = dec.adminNotes,
                    respondedAtEpochMs = dec.respondedAtEpochMs,
                    expiresAtEpochMs = usageStatsHelper.getNextMidnightEpochMs(),
                    baselineUsageSecondsAtApproval = baseline
                )
                localDataStorage.saveExtensionRequest(updated)
                updatesCount++

                if (dec.status == ExtensionStatus.APPROVED || dec.status == ExtensionStatus.MODIFIED) {
                    try {
                        AppBlockerOverlayManager.hideOverlay()
                    } catch (_: Exception) {}
                }
            }
        }

        // 2. Check for Limit Updates from Admin
        for (upd in events.limitUpdates) {
            if (upd.userId == userId || upd.userId == user.email) {
                val currentCfg = localDataStorage.getLimitConfig(userId)
                if (currentCfg == null || currentCfg.dailyLimitMinutes != upd.dailyLimitMinutes) {
                    localDataStorage.saveLimitConfig(
                        DailyLimitConfig(
                            userId = userId,
                            dailyLimitMinutes = upd.dailyLimitMinutes.coerceIn(1, 45),
                            isLocked = true,
                            lockedAtEpochMs = upd.updatedAtEpochMs,
                            lastModifiedBy = "admin",
                            lastModifiedReason = upd.reason
                        )
                    )
                    updatesCount++
                }
            }
        }

        // 3. Check for Deletion Decisions from Admin
        for (del in events.deletionDecisions) {
            val localDel = localDataStorage.getAllDeletionRequests().find { it.id == del.requestId }
            if (localDel != null && localDel.status == DeletionRequestStatus.PENDING) {
                if (del.approved) {
                    localDataStorage.deleteUserAccount(userId)
                    updatesCount++
                } else {
                    localDataStorage.saveDeletionRequest(
                        localDel.copy(status = DeletionRequestStatus.REJECTED)
                    )
                    updatesCount++
                }
            }
        }

        if (updatesCount > 0) {
            localDataStorage.notifyDataChanged()
        }

        Result.success(updatesCount)
    }

    override suspend fun publishUsageTelemetry(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = localDataStorage.getUserById(userId)
            ?: return@withContext Result.failure(IllegalArgumentException("User not found."))
        // Never publish telemetry for Admin accounts or accounts without an invite binding
        if (user.role == UserRole.ADMIN || user.registeredViaInviteCode.isNullOrBlank()) {
            return@withContext Result.success(false)
        }
        val inviteCode = user.registeredViaInviteCode
            ?: return@withContext Result.success(false)

        val monitored = try {
            usageStatsHelper.queryMonitoredAppUsage()
        } catch (_: Exception) {
            emptyList()
        }

        val fbSec = monitored.find { it.packageName.contains("facebook") }?.usageSeconds ?: 0L
        val igSec = monitored.find { it.packageName.contains("instagram") }?.usageSeconds ?: 0L
        val scSec = monitored.find { it.packageName.contains("snapchat") }?.usageSeconds ?: 0L
        val totalSec = fbSec + igSec + scSec
        val now = System.currentTimeMillis()

        val telemetry = RemoteUsageTelemetry(
            userId = userId,
            userEmail = user.email,
            inviteCode = inviteCode,
            facebookSeconds = fbSec,
            instagramSeconds = igSec,
            snapchatSeconds = scSec,
            totalUsedSeconds = totalSec,
            timestampEpochMs = now
        )

        localDataStorage.saveUserUsageTelemetry(telemetry, inviteCode, user.email)
        val published = CloudSyncRelayHelper.publishUsageTelemetry(inviteCode, telemetry)
        Result.success(published)
    }

    override suspend fun importOfflineRegistration(syncCode: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val payload = CloudSyncRelayHelper.parseOfflineSyncCode(syncCode)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid offline sync string format."))

        val newUser = UserAccount(
            id = payload.userId,
            email = payload.email,
            fullName = payload.fullName,
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            socialHandles = SocialHandles(
                facebook = payload.facebook,
                instagram = payload.instagram,
                snapchat = payload.snapchat
            ),
            registeredViaInviteCode = payload.inviteCode,
            createdAtEpochMs = payload.createdAtEpochMs,
            lastActiveEpochMs = System.currentTimeMillis()
        )
        localDataStorage.saveUser(newUser)
        localDataStorage.saveLimitConfig(
            DailyLimitConfig(
                userId = payload.userId,
                dailyLimitMinutes = payload.limitMinutes.coerceIn(0, 45),
                isLocked = true,
                lockedAtEpochMs = payload.createdAtEpochMs,
                lastModifiedBy = "system",
                lastModifiedReason = "Imported offline registration"
            )
        )
        if (payload.usageTelemetry != null) {
            localDataStorage.saveUserUsageTelemetry(payload.usageTelemetry)
        }

        // Mark corresponding invite as redeemed if present
        val matchingInvite = localDataStorage.getInviteByCode(payload.inviteCode)
        if (matchingInvite != null) {
            localDataStorage.saveInvite(
                matchingInvite.copy(
                    isRedeemed = true,
                    redeemedByUserId = payload.userId,
                    redeemedByUserName = payload.fullName,
                    redeemedByUserEmail = payload.email,
                    redeemedAtEpochMs = payload.createdAtEpochMs
                )
            )
        }

        Result.success(newUser)
    }

    override suspend fun listAuditLogs(): Result<List<AuditLogEntry>> = withContext(Dispatchers.IO) {
        Result.success(localDataStorage.getAllAuditLogs())
    }

    override suspend fun requestAccountDeletion(userId: String, reason: String): Result<AccountDeletionRequest> = withContext(Dispatchers.IO) {
        val user = localDataStorage.getUserById(userId)
            ?: return@withContext Result.failure(IllegalArgumentException("User not found."))
        if (reason.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please explain a reason for requesting account deletion."))
        }

        val req = AccountDeletionRequest(
            id = "del-" + SecurityManager.generateUuid().take(8),
            userId = userId,
            userName = user.fullName,
            userEmail = user.email,
            reason = reason.trim(),
            status = DeletionRequestStatus.PENDING,
            requestedAtEpochMs = System.currentTimeMillis()
        )
        localDataStorage.saveDeletionRequest(req)

        // Broadcast deletion request across Cloud Relay to Admin
        val inviteCode = user.registeredViaInviteCode
        if (!inviteCode.isNullOrBlank()) {
            try {
                CloudSyncRelayHelper.publishDeletionRequest(inviteCode, req)
            } catch (_: Exception) {}
        }

        localDataStorage.appendAuditLog(
            AuditLogEntry(
                id = SecurityManager.generateUuid(),
                actorId = userId,
                actorRole = user.role,
                actionType = AuditActionType.ACCOUNT_DELETION_REQUESTED,
                description = "User requested account deletion: '$reason'. Awaiting Administrator review.",
                timestampEpochMs = System.currentTimeMillis()
            )
        )

        Result.success(req)
    }

    override suspend fun cancelAccountDeletion(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val pending = localDataStorage.getDeletionRequestByUserId(userId)
        if (pending != null) {
            localDataStorage.deleteDeletionRequest(pending.id)
            localDataStorage.appendAuditLog(
                AuditLogEntry(
                    id = SecurityManager.generateUuid(),
                    actorId = userId,
                    actorRole = UserRole.USER,
                    actionType = AuditActionType.ACCOUNT_DELETION_REJECTED,
                    description = "User cancelled their pending account deletion request.",
                    timestampEpochMs = System.currentTimeMillis()
                )
            )
        }
        Result.success(Unit)
    }

    override suspend fun adminReviewDeletionRequest(
        adminId: String,
        requestId: String,
        approved: Boolean,
        notes: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val admin = localDataStorage.getUserById(adminId)
        if (admin?.role != UserRole.ADMIN) {
            return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required."))
        }

        val all = localDataStorage.getAllDeletionRequests()
        val existing = all.find { it.id == requestId }
            ?: return@withContext Result.failure(IllegalArgumentException("Deletion request not found."))

        if (approved) {
            // Delete the user account and associated configurations
            localDataStorage.deleteUserAccount(existing.userId)
            localDataStorage.saveDeletionRequest(
                existing.copy(
                    status = DeletionRequestStatus.APPROVED,
                    processedAtEpochMs = System.currentTimeMillis(),
                    adminNotes = notes
                )
            )
            localDataStorage.appendAuditLog(
                AuditLogEntry(
                    id = SecurityManager.generateUuid(),
                    actorId = adminId,
                    actorRole = UserRole.ADMIN,
                    actionType = AuditActionType.ACCOUNT_DELETION_APPROVED,
                    description = "Administrator approved deletion of account ${existing.userName} (${existing.userEmail}).",
                    timestampEpochMs = System.currentTimeMillis()
                )
            )
        } else {
            localDataStorage.saveDeletionRequest(
                existing.copy(
                    status = DeletionRequestStatus.REJECTED,
                    processedAtEpochMs = System.currentTimeMillis(),
                    adminNotes = notes
                )
            )
            localDataStorage.appendAuditLog(
                AuditLogEntry(
                    id = SecurityManager.generateUuid(),
                    actorId = adminId,
                    actorRole = UserRole.ADMIN,
                    actionType = AuditActionType.ACCOUNT_DELETION_REJECTED,
                    description = "Administrator rejected deletion request for ${existing.userName}: ${notes ?: "No notes"}",
                    timestampEpochMs = System.currentTimeMillis()
                )
            )
        }

        // Broadcast deletion decision across Cloud Relay to User
        val targetUser = localDataStorage.getUserById(existing.userId)
        val inviteCode = targetUser?.registeredViaInviteCode
        if (!inviteCode.isNullOrBlank()) {
            try {
                CloudSyncRelayHelper.publishDeletionDecision(inviteCode, requestId, approved)
            } catch (_: Exception) {}
        }

        Result.success(Unit)
    }

    override suspend fun listPendingDeletionRequests(): Result<List<AccountDeletionRequest>> = withContext(Dispatchers.IO) {
        val pending = localDataStorage.getAllDeletionRequests().filter { it.status == DeletionRequestStatus.PENDING }
        Result.success(pending)
    }

    override suspend fun getPendingDeletionRequest(userId: String): Result<AccountDeletionRequest?> = withContext(Dispatchers.IO) {
        val req = localDataStorage.getDeletionRequestByUserId(userId)
        Result.success(req)
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

