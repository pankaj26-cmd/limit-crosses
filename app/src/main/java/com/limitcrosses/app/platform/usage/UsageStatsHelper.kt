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
     * Determines which package is currently in the foreground by examining the latest
     * lifecycle state transitions over the recent window (looking back to today's midnight or up to 2 hours).
     * This avoids returning null when a user has been inside an app for more than a few seconds.
     */
    fun getForegroundPackage(): String? {
        if (!hasUsagePermission() || usageStatsManager == null) return null
        val endTime = System.currentTimeMillis()
        val midnight = getTodayMidnightEpochMs()
        val startTime = (endTime - (2 * 60 * 60 * 1000L)).coerceAtLeast(midnight)

        try {
            val events = usageStatsManager.queryEvents(startTime, endTime) ?: return null
            val event = UsageEvents.Event()
            var topPackage: String? = null
            var isInteractive = true

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    1 -> { // MOVE_TO_FOREGROUND
                        topPackage = pkg
                        isInteractive = true
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    2 -> { // MOVE_TO_BACKGROUND
                        if (pkg == topPackage) {
                            topPackage = null
                        }
                    }
                    16, // SCREEN_NON_INTERACTIVE
                    17  // KEYGUARD_SHOWN
                    -> {
                        isInteractive = false
                        topPackage = null
                    }
                    15, // SCREEN_INTERACTIVE
                    18  // KEYGUARD_HIDDEN
                    -> {
                        isInteractive = true
                    }
                }
            }
            return if (isInteractive) topPackage else null
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
     * Uses a multi-activity tracking state machine with boundary clamping to ensure that:
     * 1. Intra-app navigation (Feed -> Reel -> Comments) does not prematurely clear the session.
     * 2. Switching to other apps (WhatsApp, Launcher) correctly closes active monitored sessions.
     * 3. Screen off / device lock cleanly concludes active sessions.
     * 4. Currently running foreground apps receive active time down to the exact second.
     * 5. Fallback to aggregated system stats ensures non-zero metrics on restrictive OEM firmware.
     */
    fun queryMonitoredAppUsage(): List<AppUsageInfo> {
        val startTime = getTodayMidnightEpochMs()
        val endTime = System.currentTimeMillis()

        if (!hasUsagePermission() || usageStatsManager == null) {
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

        // Maximum physical ceiling: cannot exceed elapsed physical seconds since midnight today
        val maxPossibleSecondsToday = ((endTime - startTime) / 1000L).coerceAtLeast(0L)

        try {
            // Query events starting slightly before midnight to capture ongoing midnight crossovers
            val queryStart = startTime - (2 * 60 * 60 * 1000L)
            val events = usageStatsManager.queryEvents(queryStart, endTime)

            if (events != null) {
                val event = UsageEvents.Event()
                // Map of active activity sessions: "$packageName/$className" -> start timestamp
                val activeActivities = mutableMapOf<String, Long>()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val pkg = event.packageName ?: continue
                    val time = event.timeStamp
                    val className = event.className ?: "default"
                    val actKey = "$pkg/$className"

                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED,
                        1 -> { // MOVE_TO_FOREGROUND
                            if (MONITORED_PACKAGES.containsKey(pkg)) {
                                // If this activity was already active, credit elapsed duration
                                val prevStart = activeActivities.remove(actKey)
                                if (prevStart != null && time > prevStart) {
                                    val effStart = prevStart.coerceAtLeast(startTime)
                                    val effEnd = time.coerceAtMost(endTime)
                                    if (effEnd > effStart) {
                                        val sec = (effEnd - effStart) / 1000L
                                        usageMap[pkg] = (usageMap[pkg] ?: 0L) + sec
                                    }
                                }
                                activeActivities[actKey] = time
                                if (time > (lastTimeMap[pkg] ?: 0L)) {
                                    lastTimeMap[pkg] = time
                                }
                            } else {
                                // User switched to a non-monitored package (Launcher, WhatsApp, Settings, etc.)
                                // Conclude all monitored activities currently marked as active
                                val keysToClose = activeActivities.keys.filter { k ->
                                    MONITORED_PACKAGES.keys.any { k.startsWith("$it/") }
                                }
                                for (k in keysToClose) {
                                    val start = activeActivities.remove(k)
                                    if (start != null && time > start) {
                                        val mPkg = k.substringBefore("/")
                                        val effStart = start.coerceAtLeast(startTime)
                                        val effEnd = time.coerceAtMost(endTime)
                                        if (effEnd > effStart) {
                                            val sec = (effEnd - effStart) / 1000L
                                            usageMap[mPkg] = (usageMap[mPkg] ?: 0L) + sec
                                        }
                                        if (time > (lastTimeMap[mPkg] ?: 0L)) {
                                            lastTimeMap[mPkg] = time
                                        }
                                    }
                                }
                            }
                        }

                        UsageEvents.Event.ACTIVITY_PAUSED,
                        2 -> { // MOVE_TO_BACKGROUND
                            if (MONITORED_PACKAGES.containsKey(pkg)) {
                                val start = activeActivities.remove(actKey)
                                if (start != null && time > start) {
                                    val effStart = start.coerceAtLeast(startTime)
                                    val effEnd = time.coerceAtMost(endTime)
                                    if (effEnd > effStart) {
                                        val sec = (effEnd - effStart) / 1000L
                                        usageMap[pkg] = (usageMap[pkg] ?: 0L) + sec
                                    }
                                }
                                if (time > (lastTimeMap[pkg] ?: 0L)) {
                                    lastTimeMap[pkg] = time
                                }
                            }
                        }

                        16, // SCREEN_NON_INTERACTIVE
                        17  // KEYGUARD_SHOWN
                        -> {
                            // Screen turned off / locked: conclude all active monitored activities
                            val keysToClose = activeActivities.keys.filter { k ->
                                MONITORED_PACKAGES.keys.any { k.startsWith("$it/") }
                            }
                            for (k in keysToClose) {
                                val start = activeActivities.remove(k)
                                if (start != null && time > start) {
                                    val mPkg = k.substringBefore("/")
                                    val effStart = start.coerceAtLeast(startTime)
                                    val effEnd = time.coerceAtMost(endTime)
                                    if (effEnd > effStart) {
                                        val sec = (effEnd - effStart) / 1000L
                                        usageMap[mPkg] = (usageMap[mPkg] ?: 0L) + sec
                                    }
                                    if (time > (lastTimeMap[mPkg] ?: 0L)) {
                                        lastTimeMap[mPkg] = time
                                    }
                                }
                            }
                        }
                    }
                }

                // Credit any monitored activities currently still active in the foreground at endTime
                for ((k, start) in activeActivities) {
                    val mPkg = k.substringBefore("/")
                    if (MONITORED_PACKAGES.containsKey(mPkg) && endTime > start) {
                        val effStart = start.coerceAtLeast(startTime)
                        val effEnd = endTime
                        if (effEnd > effStart) {
                            val activeSec = (effEnd - effStart) / 1000L
                            usageMap[mPkg] = (usageMap[mPkg] ?: 0L) + activeSec
                            lastTimeMap[mPkg] = endTime
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: If queryEvents produced 0s across all monitored apps (e.g. system usage cache flushed or delayed),
        // check queryAndAggregateUsageStats directly
        if (usageMap.values.all { it == 0L }) {
            try {
                val aggregated = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
                if (!aggregated.isNullOrEmpty()) {
                    MONITORED_PACKAGES.keys.forEach { pkg ->
                        val stat = aggregated[pkg]
                        if (stat != null && stat.totalTimeInForeground > 0L) {
                            val sec = (stat.totalTimeInForeground / 1000L).coerceIn(0L, maxPossibleSecondsToday)
                            usageMap[pkg] = sec
                            if (stat.lastTimeUsed > (lastTimeMap[pkg] ?: 0L)) {
                                lastTimeMap[pkg] = stat.lastTimeUsed
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Clamp each monitored package to physical elapsed time today
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
