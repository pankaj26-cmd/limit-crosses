package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.*
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.domain.repository.AdminRepository
import com.limitcrosses.app.domain.repository.AuthRepository
import com.limitcrosses.app.domain.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val selectedTab: Int = 0, // 0: Invites, 1: Users & Usage, 2: Requests & Deletions, 3: Audit Log
    val managedUsers: List<UserAccount> = emptyList(),
    val userLimits: Map<String, DailyLimitConfig> = emptyMap(),
    val userExtraMinutesGranted: Map<String, Int> = emptyMap(), // How much extra time admin has given to each user today
    val userUsageReports: Map<String, CombinedUsageReport> = emptyMap(), // Usage on all monitored apps
    val userInactiveAlerts: List<String> = emptyList(), // Alerts if user uninstalled or disconnected
    val pendingExtensions: List<ExtensionRequest> = emptyList(),
    val allExtensions: List<ExtensionRequest> = emptyList(),
    val pendingDeletions: List<AccountDeletionRequest> = emptyList(),
    val activeInvites: List<InviteCode> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val generatedCode: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository,
    private val localDataStorage: LocalDataStorage,
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
        refreshAll()
    }

    fun refreshAll() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            // Automatically discover and sync user registrations from other devices via Cloud Relay
            try {
                adminRepository.syncRemoteRegistrations()
            } catch (_: Exception) {}

            val usersRes = adminRepository.listManagedUsers()
            val allExtRes = adminRepository.listAllExtensions()
            val pendingDelRes = adminRepository.listPendingDeletionRequests()
            val invRes = adminRepository.listAllInvites()
            val auditRes = adminRepository.listAuditLogs()

            val users = usersRes.getOrDefault(emptyList())
            val allExts = allExtRes.getOrDefault(emptyList())
            val pendingExts = allExts.filter { it.status == ExtensionStatus.PENDING }
            val pendingDeletions = pendingDelRes.getOrDefault(emptyList())

            // Map user limits
            val limitsMap = users.associate { u ->
                u.id to (localDataStorage.getLimitConfig(u.id) ?: DailyLimitConfig(u.id, 30, true))
            }

            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val todayMidnight = cal.timeInMillis
            val now = System.currentTimeMillis()

            // Map total extra time granted to each user today by Admin (daily only: reset at midnight)
            val extraMinutesMap = users.associate { u ->
                val approvedToday = allExts.filter {
                    it.userId == u.id &&
                    (it.status == ExtensionStatus.APPROVED || it.status == ExtensionStatus.MODIFIED) &&
                    (it.expiresAtEpochMs == null || it.expiresAtEpochMs > now) &&
                    ((it.respondedAtEpochMs ?: 0L) >= todayMidnight || it.requestedAtEpochMs >= todayMidnight)
                }
                u.id to approvedToday.sumOf { it.grantedMinutes }
            }

            val currentAdmin = authRepository.refreshCurrentUser() ?: authRepository.getActiveUser()
            val currentUserId = localDataStorage.getCurrentUserId()
            // Query active usage report on all apps
            val localDeviceReport = usageRepository.refreshUsage()
            val reportsMap = users.associate { u ->
                val isLocalDeviceAccount = (currentAdmin != null && u.id == currentAdmin.id) ||
                    (currentUserId != null && u.id == currentUserId) ||
                    (!currentAdmin?.email.isNullOrBlank() && u.email.equals(currentAdmin?.email, ignoreCase = true)) ||
                    u.role == UserRole.ADMIN

                val reportForUser = if (isLocalDeviceAccount) {
                    // Local admin account uses physical device screen stats
                    localDeviceReport
                } else {
                    // Remote managed user: retrieve telemetry explicitly belonging to this user
                    val rawCandidate = localDataStorage.getUserUsageTelemetry(u.id)
                        ?: localDataStorage.getUserUsageTelemetryByEmail(u.email)
                        ?: u.registeredViaInviteCode?.let { inv ->
                            val invTel = localDataStorage.getInviteUsageTelemetry(inv)
                            if (invTel != null && (invTel.userId == u.id || invTel.userEmail.equals(u.email, ignoreCase = true))) {
                                invTel
                            } else null
                        }

                    // Strict attribution: reject telemetry that belongs to the admin or mirrors local admin stats
                    val candidate = rawCandidate?.takeIf { tel ->
                        val isNotAdmin = tel.userId != currentAdmin?.id &&
                            tel.userId != currentUserId &&
                            (currentAdmin?.email.isNullOrBlank() || !tel.userEmail.equals(currentAdmin?.email, ignoreCase = true))
                        val isMatchingUser = tel.userId == u.id ||
                            (!tel.userEmail.isNullOrBlank() && tel.userEmail.equals(u.email, ignoreCase = true))
                        isNotAdmin && isMatchingUser
                    }

                    // Strictly verify telemetry was reported today (since today's midnight)
                    val isTelemetryFromToday = candidate != null && candidate.timestampEpochMs >= todayMidnight
                    val telemetry = if (isTelemetryFromToday) candidate else null

                    val baseLimit = limitsMap[u.id]?.dailyLimitMinutes ?: 30
                    val extraGranted = extraMinutesMap[u.id] ?: 0
                    val totalCap = baseLimit + extraGranted
                    if (telemetry != null) {
                        val fbSec = telemetry.facebookSeconds
                        val igSec = telemetry.instagramSeconds
                        val scSec = telemetry.snapchatSeconds
                        val totalSec = telemetry.totalUsedSeconds.coerceAtLeast(fbSec + igSec + scSec)
                        val remSec = (totalCap * 60L - totalSec).coerceAtLeast(0L)
                        CombinedUsageReport(
                            dailyLimitMinutes = totalCap,
                            totalUsedSeconds = totalSec,
                            remainingSeconds = remSec,
                            isLimitReached = remSec <= 0L,
                            resetTimeEpochMs = localDeviceReport.resetTimeEpochMs,
                            apps = listOf(
                                AppUsageInfo("com.facebook.katana", "Facebook", fbSec, 0L, "facebook.com"),
                                AppUsageInfo("com.instagram.android", "Instagram", igSec, 0L, "instagram.com"),
                                AppUsageInfo("com.snapchat.android", "Snapchat", scSec, 0L, "snapchat.com")
                            ),
                            trackingActive = true,
                            lastPolledEpochMs = telemetry.timestampEpochMs
                        )
                    } else {
                        // Brand-new remote user who has not reported usage yet -> strictly 0m across all apps
                        CombinedUsageReport(
                            dailyLimitMinutes = totalCap,
                            totalUsedSeconds = 0L,
                            remainingSeconds = totalCap * 60L,
                            isLimitReached = false,
                            resetTimeEpochMs = localDeviceReport.resetTimeEpochMs,
                            apps = listOf(
                                AppUsageInfo("com.facebook.katana", "Facebook", 0L, 0L, "facebook.com"),
                                AppUsageInfo("com.instagram.android", "Instagram", 0L, 0L, "instagram.com"),
                                AppUsageInfo("com.snapchat.android", "Snapchat", 0L, 0L, "snapchat.com")
                            ),
                            trackingActive = true,
                            lastPolledEpochMs = System.currentTimeMillis()
                        )
                    }
                }
                u.id to reportForUser
            }

            // Check for potential uninstallation / inactivity
            val inactiveAlerts = mutableListOf<String>()
            users.forEach { u ->
                val diffHours = (now - u.lastActiveEpochMs) / (1000 * 60 * 60)
                if (diffHours >= 48) {
                    inactiveAlerts.add("⚠️ Inactive Warning: ${u.fullName} (${u.email}) has no heartbeat recorded for $diffHours hours. The app may be uninstalled or disabled.")
                }
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                managedUsers = users,
                userLimits = limitsMap,
                userExtraMinutesGranted = extraMinutesMap,
                userUsageReports = reportsMap,
                userInactiveAlerts = inactiveAlerts,
                pendingExtensions = pendingExts,
                allExtensions = allExts,
                pendingDeletions = pendingDeletions,
                activeInvites = invRes.getOrDefault(emptyList()),
                auditLogs = auditRes.getOrDefault(emptyList())
            )
        }
    }

    fun createInvite(limitMinutes: Int, validDays: Int, notes: String) {
        viewModelScope.launch {
            val admin = authRepository.getActiveUser() ?: return@launch
            val res = adminRepository.generateInvite(admin.id, limitMinutes, validDays, notes)
            res.onSuccess { invite ->
                _uiState.value = _uiState.value.copy(
                    generatedCode = invite.code,
                    successMessage = "Invite ${invite.code} created successfully."
                )
                refreshAll()
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Failed to generate invite.")
            }
        }
    }

    fun modifyUserLimit(targetUserId: String, newMinutes: Int, reason: String) {
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Audit reason is required.")
            return
        }
        if (newMinutes <= 0 || newMinutes > 45) {
            _uiState.value = _uiState.value.copy(errorMessage = "Daily limit must be a positive number between 1 and 45 minutes.")
            return
        }

        viewModelScope.launch {
            val admin = authRepository.getActiveUser() ?: return@launch
            val res = adminRepository.modifyUserLimit(admin.id, targetUserId, newMinutes, reason)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "User limit updated to $newMinutes min.")
                refreshAll()
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Failed to modify limit.")
            }
        }
    }

    fun reviewExtension(
        requestId: String,
        newStatus: ExtensionStatus,
        grantedMinutes: Int,
        notes: String?
    ) {
        viewModelScope.launch {
            val admin = authRepository.getActiveUser() ?: return@launch
            val res = adminRepository.reviewExtension(admin.id, requestId, newStatus, grantedMinutes, notes)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Extension request decided ($newStatus).")
                refreshAll()
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Review failed.")
            }
        }
    }

    fun reviewDeletionRequest(requestId: String, approved: Boolean, notes: String? = null) {
        viewModelScope.launch {
            val admin = authRepository.getActiveUser() ?: return@launch
            val res = adminRepository.reviewDeletionRequest(admin.id, requestId, approved, notes)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = if (approved) "Account deleted upon user request." else "Deletion request rejected."
                )
                refreshAll()
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Deletion review failed.")
            }
        }
    }

    fun importOfflineRegistration(syncCode: String, onResult: (Boolean, String?) -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val res = adminRepository.importOfflineRegistration(syncCode)
            _uiState.value = _uiState.value.copy(isLoading = false)
            res.onSuccess { user ->
                _uiState.value = _uiState.value.copy(successMessage = "Successfully imported and linked user ${user.fullName} (${user.email})!")
                refreshAll()
                onResult(true, null)
            }.onFailure { err ->
                val msg = err.message ?: "Failed to import registration code."
                _uiState.value = _uiState.value.copy(errorMessage = msg)
                onResult(false, msg)
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
