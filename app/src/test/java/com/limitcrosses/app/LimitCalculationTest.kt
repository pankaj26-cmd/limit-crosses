package com.limitcrosses.app

import com.limitcrosses.app.core.model.AppUsageInfo
import org.junit.Assert.*
import org.junit.Test

class LimitCalculationTest {

    @Test
    fun testCombinedUsageCalculation() {
        val fbUsage = AppUsageInfo("com.facebook.katana", "Facebook", 600L, 0L, "facebook.com") // 10 min
        val igUsage = AppUsageInfo("com.instagram.android", "Instagram", 900L, 0L, "instagram.com") // 15 min
        val snapUsage = AppUsageInfo("com.snapchat.android", "Snapchat", 300L, 0L, "snapchat.com") // 5 min

        val apps = listOf(fbUsage, igUsage, snapUsage)
        val totalUsedSeconds = apps.sumOf { it.usageSeconds }
        val totalUsedMinutes = (totalUsedSeconds / 60L).toInt()

        assertEquals(30, totalUsedMinutes)

        val dailyLimitMinutes = 35
        val limitSeconds = dailyLimitMinutes * 60L
        val remainingSeconds = (limitSeconds - totalUsedSeconds).coerceAtLeast(0L)
        val remainingMinutes = (remainingSeconds / 60L).toInt()

        assertEquals(5, remainingMinutes)
        assertFalse(remainingSeconds <= 0L)
    }

    @Test
    fun testLimitExhaustionAtZeroRemaining() {
        val totalUsedSeconds = 45 * 60L // 45 min used
        val dailyLimitMinutes = 45 // 45 min max limit

        val limitSeconds = dailyLimitMinutes * 60L
        val remainingSeconds = (limitSeconds - totalUsedSeconds).coerceAtLeast(0L)

        assertEquals(0L, remainingSeconds)
        val isLimitReached = remainingSeconds <= 0L
        assertTrue(isLimitReached)
    }

    @Test
    fun testCombinedLimitClampedTo45MinutesMax() {
        val attemptedLimit1 = 60
        val clamped1 = attemptedLimit1.coerceIn(0, 45)
        assertEquals(45, clamped1)

        val attemptedLimit2 = -5
        val clamped2 = attemptedLimit2.coerceIn(0, 45)
        assertEquals(0, clamped2)
    }

    @Test
    fun testUnpairedBackgroundStopEventsDoNotCreditTime() {
        val todayMidnight = 1700000000000L
        val now = todayMidnight + (11 * 3600 + 46 * 60) * 1000L // 11:46 AM today

        val activeActivities = mutableMapOf<String, Long>()
        var usageSeconds = 0L

        // Simulate 5 background STOP events firing for Snapchat without an ACTIVITY_RESUMED event
        val stopTimestamps = listOf(
            todayMidnight + 1800000L,
            todayMidnight + 7200000L,
            todayMidnight + 14400000L,
            todayMidnight + 28800000L,
            now
        )

        for (stopTs in stopTimestamps) {
            val sessionStart = activeActivities.remove("com.snapchat.android/default")
            if (sessionStart != null && stopTs > sessionStart) {
                usageSeconds += (stopTs - sessionStart) / 1000L
            }
        }

        // Must remain strictly 0 seconds because Snapchat was never actually resumed by the user
        assertEquals(0L, usageSeconds)
    }

    @Test
    fun testPairedResumePauseCreditsExactDuration() {
        val todayMidnight = 1700000000000L
        val activeActivities = mutableMapOf<String, Long>()
        var usageSeconds = 0L

        // User opens Instagram at 10:00:00 AM
        val resumeTs = todayMidnight + (10 * 3600 * 1000L)
        activeActivities["com.instagram.android/MainActivity"] = resumeTs

        // User leaves Instagram at 10:05:30 AM (5m 30s = 330s)
        val pauseTs = resumeTs + (330 * 1000L)
        val sessionStart = activeActivities.remove("com.instagram.android/MainActivity")
        assertNotNull(sessionStart)
        if (sessionStart != null && pauseTs > sessionStart) {
            usageSeconds += (pauseTs - sessionStart) / 1000L
        }

        assertEquals(330L, usageSeconds)
    }

    @Test
    fun testIntraAppActivityNavigationPreservesSessionDuration() {
        val todayMidnight = 1700000000000L
        val startTime = todayMidnight
        val endTime = todayMidnight + (12 * 3600 * 1000L) // 12:00 PM
        val activeActivities = mutableMapOf<String, Long>()
        var usageSeconds = 0L

        // 1. User opens Instagram Feed at 10:00:00 AM
        val t1 = todayMidnight + (10 * 3600 * 1000L)
        activeActivities["com.instagram.android/MainActivity"] = t1

        // 2. User taps a Reel at 10:00:05 AM -> ReelViewerActivity resumes BEFORE MainActivity pauses
        val t2 = t1 + 5000L
        activeActivities["com.instagram.android/ReelViewerActivity"] = t2

        // 3. MainActivity pauses at 10:00:05.050 AM
        val t3 = t2 + 50L
        val startFeed = activeActivities.remove("com.instagram.android/MainActivity")
        assertNotNull(startFeed)
        if (startFeed != null && t3 > startFeed) {
            usageSeconds += (t3 - startFeed) / 1000L // 5 seconds
        }

        // 4. User stays on Reels for 20 minutes (until 10:20:05 AM)
        val t4 = t2 + (20 * 60 * 1000L)
        val startReels = activeActivities.remove("com.instagram.android/ReelViewerActivity")
        assertNotNull(startReels)
        if (startReels != null && t4 > startReels) {
            usageSeconds += (t4 - startReels) / 1000L // 1200 seconds
        }

        // Total credited duration must be exactly 5s + 1200s = 1205s (20m 5s), not 0s
        assertEquals(1205L, usageSeconds)
        assertTrue(activeActivities.isEmpty())
    }

    @Test
    fun testOngoingActiveSessionAtEndTimeIsCredited() {
        val todayMidnight = 1700000000000L
        val startTime = todayMidnight
        val activeActivities = mutableMapOf<String, Long>()
        var usageSeconds = 0L

        // User opens Facebook at 11:30:00 AM
        val tStart = todayMidnight + (11 * 3600 + 30 * 60) * 1000L
        activeActivities["com.facebook.katana/FbMainTabActivity"] = tStart

        // Current time is 11:45:00 AM (15 minutes in, user is still inside the app)
        val tNow = tStart + (15 * 60 * 1000L)

        // The query engine evaluates active sessions at endTime
        for ((key, start) in activeActivities) {
            val effStart = start.coerceAtLeast(startTime)
            if (tNow > effStart) {
                usageSeconds += (tNow - effStart) / 1000L
            }
        }

        // Must credit the full active 15 minutes (900 seconds) in real time
        assertEquals(900L, usageSeconds)
    }

    @Test
    fun testAppSwitchingConcludesMonitoredSession() {
        val todayMidnight = 1700000000000L
        val startTime = todayMidnight
        val activeActivities = mutableMapOf<String, Long>()
        var usageSeconds = 0L

        // 1. User opens Snapchat at 10:00:00 AM
        val tStart = todayMidnight + (10 * 3600 * 1000L)
        activeActivities["com.snapchat.android/LandingPageActivity"] = tStart

        // 2. User switches directly to WhatsApp at 10:03:00 AM (3 min later)
        val tSwitch = tStart + (3 * 60 * 1000L)
        // Displace any running monitored activities
        val keys = activeActivities.keys.filter { it.startsWith("com.snapchat.android/") }
        for (k in keys) {
            val s = activeActivities.remove(k)
            if (s != null && tSwitch > s) {
                usageSeconds += (tSwitch - s) / 1000L
            }
        }

        assertEquals(180L, usageSeconds) // exactly 3 minutes
        assertTrue(activeActivities.isEmpty())
    }

    @Test
    fun testMidnightCrossoverClampsStartTimeToMidnight() {
        val todayMidnight = 1700000000000L
        val startTime = todayMidnight

        // App opened at 11:55:00 PM yesterday (5 minutes before midnight)
        val tOpen = todayMidnight - (5 * 60 * 1000L)
        // App closed at 12:10:00 AM today (10 minutes after midnight)
        val tClose = todayMidnight + (10 * 60 * 1000L)

        val effStart = tOpen.coerceAtLeast(startTime) // Clamped to 00:00:00 today
        val effEnd = tClose
        val durationToday = (effEnd - effStart) / 1000L

        // Only the 10 minutes (600 seconds) belonging to today are credited
        assertEquals(600L, durationToday)
    }

    @Test
    fun testPhysicalElapsedCeilingClamp() {
        val todayMidnight = 1700000000000L
        // 11:46 AM elapsed time = 706 minutes = 42,360 seconds
        val now = todayMidnight + (11 * 3600 + 46 * 60) * 1000L
        val maxPossibleSecondsToday = ((now - todayMidnight) / 1000L).coerceAtLeast(0L)

        // Corrupted or bogus 3712 minutes = 222,720 seconds
        val bogusSeconds = 3712 * 60L
        val clampedUsageSeconds = bogusSeconds.coerceIn(0L, maxPossibleSecondsToday)

        // Clamped to 42,360 seconds (706 min), physically impossible to reach 3712 min
        assertEquals(maxPossibleSecondsToday, clampedUsageSeconds)
        assertTrue(clampedUsageSeconds <= maxPossibleSecondsToday)
    }
}
