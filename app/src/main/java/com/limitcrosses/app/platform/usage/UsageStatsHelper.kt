package com.limitcrosses.app.platform.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import com.limitcrosses.app.core.model.AppUsageInfo
import java.util.Calendar
import java.util.TimeZone

class UsageStatsHelper(private val context: Context) {

    companion object {
        const val PKG_FACEBOOK = "com.facebook.katana"
        const val PKG_INSTAGRAM = "com.instagram.android"
        const val PKG_SNAPCHAT = "com.snapchat.android"

        val MONITORED_PACKAGES = mapOf(
            PKG_FACEBOOK to Pair("Facebook", "facebook.com"),
            PKG_INSTAGRAM to Pair("Instagram", "instagram.com"),
            PKG_SNAPCHAT to Pair("Snapchat", "snapchat.com")
        )
    }

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Calculates today's local midnight timestamp in the current timezone,
     * robustly handling DST transitions and timezone changes.
     */
    fun getTodayMidnightEpochMs(): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Calculates the next local midnight timestamp.
     */
    fun getNextMidnightEpochMs(): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }

    /**
     * Queries foreground events or usage stats for the 3 monitored packages since today's midnight.
     */
    fun queryMonitoredAppUsage(): List<AppUsageInfo> {
        val startTime = getTodayMidnightEpochMs()
        val endTime = System.currentTimeMillis()

        if (!hasUsagePermission() || usageStatsManager == null) {
            // Return empty baseline if permission is not granted
            return MONITORED_PACKAGES.map { (pkg, meta) ->
                AppUsageInfo(
                    packageName = pkg,
                    displayName = meta.first,
                    usageSeconds = 0L,
                    lastTimeUsedEpochMs = 0L,
                    targetDomain = meta.second
                )
            }
        }

        val usageMap = mutableMapOf<String, Long>()
        val lastTimeMap = mutableMapOf<String, Long>()
        MONITORED_PACKAGES.keys.forEach {
            usageMap[it] = 0L
            lastTimeMap[it] = 0L
        }

        try {
            // Aggregate from usage stats query
            val statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            if (!statsList.isNullOrEmpty()) {
                for (stat in statsList) {
                    if (MONITORED_PACKAGES.containsKey(stat.packageName)) {
                        val currentTotal = usageMap[stat.packageName] ?: 0L
                        usageMap[stat.packageName] = currentTotal + (stat.totalTimeInForeground / 1000L)
                        if (stat.lastTimeUsed > (lastTimeMap[stat.packageName] ?: 0L)) {
                            lastTimeMap[stat.packageName] = stat.lastTimeUsed
                        }
                    }
                }
            } else {
                // Fallback to event stream if daily interval list is empty
                val events = usageStatsManager.queryEvents(startTime, endTime)
                val event = UsageEvents.Event()
                val startTimes = mutableMapOf<String, Long>()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val pkg = event.packageName ?: continue
                    if (!MONITORED_PACKAGES.containsKey(pkg)) continue

                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                            startTimes[pkg] = event.timeStamp
                            lastTimeMap[pkg] = event.timeStamp
                        }
                        UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                            val start = startTimes.remove(pkg)
                            if (start != null && event.timeStamp > start) {
                                val durationSec = (event.timeStamp - start) / 1000L
                                usageMap[pkg] = (usageMap[pkg] ?: 0L) + durationSec
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return MONITORED_PACKAGES.map { (pkg, meta) ->
            AppUsageInfo(
                packageName = pkg,
                displayName = meta.first,
                usageSeconds = usageMap[pkg] ?: 0L,
                lastTimeUsedEpochMs = lastTimeMap[pkg] ?: 0L,
                targetDomain = meta.second
            )
        }
    }
}

