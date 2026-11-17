package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.CombinedUsageReport
import com.limitcrosses.app.core.model.ExtensionStatus
import com.limitcrosses.app.core.model.UserRole
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.data.remote.BackendClient
import com.limitcrosses.app.domain.repository.UsageRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    private val usageRepository: UsageRepository,
    private val localDataStorage: LocalDataStorage,
    private val backendClient: BackendClient? = null
) : ViewModel() {

    private val _usageState = MutableStateFlow<CombinedUsageReport?>(null)
    val usageState = _usageState.asStateFlow()

    private val _resetTimeFormatted = MutableStateFlow("")
    val resetTimeFormatted = _resetTimeFormatted.asStateFlow()

    private val _activeBonusMinutes = MutableStateFlow(0)
    val activeBonusMinutes = _activeBonusMinutes.asStateFlow()

    private val _pendingRequestMinutes = MutableStateFlow<Int?>(null)
    val pendingRequestMinutes = _pendingRequestMinutes.asStateFlow()

    init {
        viewModelScope.launch {
            usageRepository.getCombinedUsageFlow().collect { report ->
                _usageState.value = report
                formatResetTime(report.resetTimeEpochMs)
                checkExtensions()
            }
        }

        // Real-time reactive updates from local storage events (immediate sync when Admin grants time)
        viewModelScope.launch {
            localDataStorage.dataUpdatesFlow.collect {
                usageRepository.refreshUsage()
                checkExtensions()
            }
        }

        // Immediate telemetry sync upon opening the Dashboard
        viewModelScope.launch {
            syncRemoteUpdates(forceTelemetry = true)
        }

        // Periodic polling loop (adaptive: 2s when pending/active, 5s normal) for seamless screen refreshes
        viewModelScope.launch {
            while (isActive) {
                syncRemoteUpdates()
                usageRepository.refreshUsage()
                checkExtensions()
                val interval = if (_pendingRequestMinutes.value != null || _activeBonusMinutes.value > 0) 2_000L else 5_000L
                delay(interval)
            }
        }
    }

    private var lastTelemetryPublishMs = 0L
    private var isSyncInProgress = false

    private suspend fun syncRemoteUpdates(forceTelemetry: Boolean = false) {
        if (isSyncInProgress) return
        isSyncInProgress = true
        try {
            val currentUserId = localDataStorage.getCurrentUserId() ?: return
            val user = localDataStorage.getUserById(currentUserId)

            // Strictly only client devices with an active invite code sync decisions and push telemetry
            if (user != null && user.role != UserRole.ADMIN && !user.registeredViaInviteCode.isNullOrBlank()) {
                // 1. Sync remote decisions from admin
                try {
                    backendClient?.syncUserUpdates(currentUserId)
                } catch (_: Exception) {}

                // 2. Publish device screen time telemetry to Admin
                val now = System.currentTimeMillis()
                if (forceTelemetry || (now - lastTelemetryPublishMs >= 10_000L)) {
                    lastTelemetryPublishMs = now
                    try {
                        backendClient?.publishUsageTelemetry(currentUserId)
                    } catch (_: Exception) {}
                }
            }
        } finally {
            isSyncInProgress = false
        }
    }

    fun manualRefresh() {
        viewModelScope.launch {
            syncRemoteUpdates(forceTelemetry = true)
            usageRepository.refreshUsage()
            checkExtensions()
        }
    }

    fun checkExtensions() {
        val currentUserId = localDataStorage.getCurrentUserId() ?: return
        val allRequests = localDataStorage.getAllExtensionRequests().filter { it.userId == currentUserId }
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayMidnight = cal.timeInMillis

        val activeApproved = allRequests.filter {
            (it.status == ExtensionStatus.APPROVED || it.status == ExtensionStatus.MODIFIED) &&
            (it.expiresAtEpochMs == null || it.expiresAtEpochMs > now) &&
            ((it.respondedAtEpochMs ?: 0L) >= todayMidnight || it.requestedAtEpochMs >= todayMidnight)
        }

        val report = _usageState.value
        val totalUsedSeconds = report?.totalUsedSeconds ?: 0L
        var remainingBonusSec = 0L

        activeApproved.forEach { ext ->
            val grantedSeconds = ext.grantedMinutes * 60L
            val baseline = if (ext.baselineUsageSecondsAtApproval > 0L) ext.baselineUsageSecondsAtApproval else 0L
            val usedSinceGrant = (totalUsedSeconds - baseline).coerceAtLeast(0L)
            val remSec = (grantedSeconds - usedSinceGrant).coerceAtLeast(0L)
            if (remSec > 0L) {
                remainingBonusSec += remSec
            }
        }

        val remainingBonusMin = ((remainingBonusSec + 59L) / 60L).toInt()
        _activeBonusMinutes.value = if (activeApproved.isNotEmpty() && remainingBonusMin > 0) {
            remainingBonusMin
        } else {
            0
        }

        val pending = allRequests.firstOrNull { it.status == ExtensionStatus.PENDING }
        _pendingRequestMinutes.value = pending?.requestedMinutes
    }

    fun simulateLimitExhaustion(exhausted: Boolean) {
        usageRepository.setSimulatedLimitReached(exhausted)
        manualRefresh()
    }

    private fun formatResetTime(epochMs: Long) {
        if (epochMs <= 0L) {
            _resetTimeFormatted.value = "12:00 AM"
            return
        }
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        _resetTimeFormatted.value = formatter.format(Date(epochMs))
    }
}
