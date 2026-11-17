package com.limitcrosses.app.data.repository

import com.limitcrosses.app.core.model.AppUsageInfo
import com.limitcrosses.app.core.model.CombinedUsageReport
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.domain.repository.DailyHistoryItem
import com.limitcrosses.app.domain.repository.UsageRepository
import com.limitcrosses.app.platform.service.AppBlockerOverlayManager
import com.limitcrosses.app.platform.usage.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UsageRepositoryImpl(
    private val localDataStorage: LocalDataStorage,
    private val usageStatsHelper: UsageStatsHelper
) : UsageRepository {

    private val usageFlow = MutableStateFlow(createEmptyReport())
    private var simulatedLimitReached = false
    private val repoScope = CoroutineScope(Dispatchers.Default)

    init {
        refreshUsageSync()
        repoScope.launch {
            localDataStorage.dataUpdatesFlow.collect {
                refreshUsageSync()
            }
        }
    }

    override fun getCombinedUsageFlow(): Flow<CombinedUsageReport> = usageFlow.asStateFlow()

    override suspend fun refreshUsage(): CombinedUsageReport {
        return refreshUsageSync()
    }

    override fun setSimulatedLimitReached(reached: Boolean) {
        simulatedLimitReached = reached
        refreshUsageSync()
    }

    private fun refreshUsageSync(): CombinedUsageReport {
        val isLoggedOut = localDataStorage.getCurrentUserId() == null
        val currentUserId = localDataStorage.getCurrentUserId() ?: "guest"
        val limitConfig = localDataStorage.getLimitConfig(currentUserId)
        val limitMinutes = limitConfig?.dailyLimitMinutes ?: 30

        val appUsages = usageStatsHelper.queryMonitoredAppUsage()
        val totalUsedSeconds = appUsages.sumOf { it.usageSeconds }
        val now = System.currentTimeMillis()

        val todayMidnight = usageStatsHelper.getTodayMidnightEpochMs()
        // Include any active approved extensions granted by the administrator today (resets at midnight)
        val activeExtensions = localDataStorage.getAllExtensionRequests().filter {
            it.userId == currentUserId &&
            (it.status == com.limitcrosses.app.core.model.ExtensionStatus.APPROVED || it.status == com.limitcrosses.app.core.model.ExtensionStatus.MODIFIED) &&
            (it.expiresAtEpochMs == null || it.expiresAtEpochMs > now) &&
            ((it.respondedAtEpochMs ?: 0L) >= todayMidnight || it.requestedAtEpochMs >= todayMidnight)
        }

        var totalBonusGrantedMinutes = 0
        var totalExtensionRemainingSeconds = 0L

        activeExtensions.forEach { ext ->
            val grantedSeconds = ext.grantedMinutes * 60L
            // Baseline is anchored to usage at approval/request time, or current usage when activated,
            // so prior usage never eats away newly granted screen time
            val baseline = when {
                ext.baselineUsageSecondsAtApproval > 0L -> ext.baselineUsageSecondsAtApproval
                ext.usageSecondsAtRequest > 0L -> ext.usageSecondsAtRequest
                totalUsedSeconds > (limitMinutes * 60L) -> totalUsedSeconds
                else -> (limitMinutes * 60L)
            }

            // Persist baseline so future checks anchor to this exact moment
            if (ext.baselineUsageSecondsAtApproval <= 0L && baseline > 0L) {
                try {
                    localDataStorage.saveExtensionRequest(
                        ext.copy(baselineUsageSecondsAtApproval = baseline)
                    )
                } catch (_: Exception) {}
            }

            val usedSinceApproval = (totalUsedSeconds - baseline).coerceAtLeast(0L)
            val remainingFromThisGrant = (grantedSeconds - usedSinceApproval).coerceAtLeast(0L)

            if (remainingFromThisGrant > 0L) {
                totalBonusGrantedMinutes += ext.grantedMinutes
                totalExtensionRemainingSeconds += remainingFromThisGrant
            }
        }

        val baseLimitSeconds = limitMinutes * 60L
        val baseRemainingSeconds = (baseLimitSeconds - totalUsedSeconds).coerceAtLeast(0L)

        // Real remaining time includes base allowance plus any active approved extension seconds
        val realRemainingSeconds = if (totalUsedSeconds < baseLimitSeconds) {
            baseRemainingSeconds + (totalBonusGrantedMinutes * 60L)
        } else {
            totalExtensionRemainingSeconds
        }

        val effectiveRemainingSeconds = if (isLoggedOut || simulatedLimitReached) 0L else realRemainingSeconds
        val effectiveUsedSeconds = if (isLoggedOut || simulatedLimitReached) (limitMinutes * 60L) else totalUsedSeconds
        val isLimitReached = isLoggedOut || simulatedLimitReached || (effectiveRemainingSeconds <= 0L)

        val effectiveLimitMinutes = limitMinutes + totalBonusGrantedMinutes

        val resetTimeEpochMs = usageStatsHelper.getNextMidnightEpochMs()
        val hasPermission = usageStatsHelper.hasUsagePermission()

        // If limit is not reached anymore, immediately dismiss any active blocker overlay window
        if (!isLimitReached) {
            try {
                AppBlockerOverlayManager.hideOverlay()
            } catch (_: Exception) {}
        }

        val report = CombinedUsageReport(
            dailyLimitMinutes = effectiveLimitMinutes,
            totalUsedSeconds = effectiveUsedSeconds,
            remainingSeconds = effectiveRemainingSeconds,
            isLimitReached = isLimitReached,
            resetTimeEpochMs = resetTimeEpochMs,
            apps = appUsages,
            trackingActive = hasPermission,
            lastPolledEpochMs = System.currentTimeMillis()
        )

        usageFlow.value = report
        return report
    }

    override suspend fun isTrackingActive(): Boolean {
        return usageStatsHelper.hasUsagePermission()
    }

    override suspend fun getResetTimeMillis(): Long {
        return usageStatsHelper.getNextMidnightEpochMs()
    }

    override fun getDailyHistory(): List<DailyHistoryItem> {
        val currentUserId = localDataStorage.getCurrentUserId() ?: "guest"
        val limitConfig = localDataStorage.getLimitConfig(currentUserId)
        val limitMinutes = limitConfig?.dailyLimitMinutes ?: 30

        val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val list = mutableListOf<DailyHistoryItem>()

        // Historical trend (current day + previous 6 days)
        for (i in 0 until 7) {
            val dateStr = if (i == 0) "Today" else dateFormat.format(cal.time)
            // Today uses real tracked value; past days use simulated historical data
            val usedMin = if (i == 0) {
                (usageFlow.value.totalUsedSeconds / 60L).toInt()
            } else {
                (18 + (i * 4) % 25)
            }
            list.add(
                DailyHistoryItem(
                    dateString = dateStr,
                    usedMinutes = usedMin,
                    limitMinutes = limitMinutes,
                    reachedCap = usedMin >= limitMinutes
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return list
    }

    private fun createEmptyReport(): CombinedUsageReport {
        return CombinedUsageReport(
            dailyLimitMinutes = 30,
            totalUsedSeconds = 0L,
            remainingSeconds = 30 * 60L,
            isLimitReached = false,
            resetTimeEpochMs = usageStatsHelper.getNextMidnightEpochMs(),
            apps = UsageStatsHelper.MONITORED_PACKAGES.map { (pkg, meta) ->
                AppUsageInfo(
                    packageName = pkg,
                    displayName = meta.first,
                    usageSeconds = 0L,
                    lastTimeUsedEpochMs = 0L,
                    targetDomain = meta.second
                )
            },
            trackingActive = false,
            lastPolledEpochMs = System.currentTimeMillis()
        )
    }
}

