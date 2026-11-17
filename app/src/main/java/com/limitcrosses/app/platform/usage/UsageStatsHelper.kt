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

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun getOverlaySettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Determines which package is currently in the foreground by querying the most recent
     * ACTIVITY_RESUMED event in the past 10 seconds.
     */
    fun getForegroundPackage(): String? {
        if (!hasUsagePermission() || usageStatsManager == null) return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000L
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime) ?: return null
            val event = UsageEvents.Event()
            var lastForegroundPkg: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastForegroundPkg = event.packageName
                }
            }
            return lastForegroundPkg
        } catch (_: Exception) {
            return null
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

        // Maximum physical ceiling: cannot exceed elapsed seconds since midnight today
        val maxPossibleSecondsToday = ((endTime - startTime) / 1000L).coerceAtLeast(0L)

        try {
            // Primary & Authoritative: Granular UsageEvents bounded strictly by today's midnight timestamp
            val events = usageStatsManager.queryEvents(startTime, endTime)
            if (events != null) {
                val event = UsageEvents.Event()
                // Track active session start timestamps strictly from ACTIVITY_RESUMED
                val activeSessionStarts = mutableMapOf<String, Long>()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val pkg = event.packageName ?: continue
                    if (!MONITORED_PACKAGES.containsKey(pkg)) continue

                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                            // User brought this app to the foreground
                            activeSessionStarts[pkg] = event.timeStamp
                            if (event.timeStamp > (lastTimeMap[pkg] ?: 0L)) {
                                lastTimeMap[pkg] = event.timeStamp
                            }
                        }
                        UsageEvents.Event.ACTIVITY_PAUSED -> {
                            // User left this app: strictly pair with its preceding ACTIVITY_RESUMED event
                            val sessionStart = activeSessionStarts.remove(pkg)
                            if (sessionStart != null && event.timeStamp > sessionStart) {
                                val sessionDurationSec = (event.timeStamp - sessionStart) / 1000L
                                val safeDurationSec = sessionDurationSec.coerceIn(0L, maxPossibleSecondsToday)
                                usageMap[pkg] = (usageMap[pkg] ?: 0L) + safeDurationSec
                            }
                            if (event.timeStamp > (lastTimeMap[pkg] ?: 0L)) {
                                lastTimeMap[pkg] = event.timeStamp
                            }
                        }
                        // Intentionally do NOT process ACTIVITY_STOPPED.
                        // Android fires ACTIVITY_STOPPED alongside ACTIVITY_PAUSED for normal activity exits,
                        // and also sends it in the background when processes are trimmed.
                        // Treating ACTIVITY_STOPPED as a session end caused un-paired phantom sessions
                        // to accumulate thousands of bogus minutes from midnight.
                    }
                }

                // If an app is *currently* open in the foreground right now, credit its active session
                val currentForegroundPkg = getForegroundPackage()
                if (currentForegroundPkg != null && MONITORED_PACKAGES.containsKey(currentForegroundPkg)) {
                    val sessionStart = activeSessionStarts[currentForegroundPkg]
                    if (sessionStart != null && endTime > sessionStart) {
                        val activeDurationSec = ((endTime - sessionStart) / 1000L).coerceIn(0L, maxPossibleSecondsToday)
                        usageMap[currentForegroundPkg] = (usageMap[currentForegroundPkg] ?: 0L) + activeDurationSec
                        lastTimeMap[currentForegroundPkg] = endTime
                    }
                }
            }
        } catch (_: Exception) {}

        // Defense-in-depth: clamp each monitored package to physical elapsed time today
        MONITORED_PACKAGES.keys.forEach { pkg ->
            val raw = usageMap[pkg] ?: 0L
            usageMap[pkg] = raw.coerceIn(0L, maxPossibleSecondsToday)
        }

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

