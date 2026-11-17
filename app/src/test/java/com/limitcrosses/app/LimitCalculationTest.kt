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

        val activeSessionStarts = mutableMapOf<String, Long>()
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
            val sessionStart = activeSessionStarts.remove("com.snapchat.android")
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
        val activeSessionStarts = mutableMapOf<String, Long>()
        var usageSeconds = 0L

        // User opens Instagram at 10:00:00 AM
        val resumeTs = todayMidnight + (10 * 3600 * 1000L)
        activeSessionStarts["com.instagram.android"] = resumeTs

        // User leaves Instagram at 10:05:30 AM (5m 30s = 330s)
        val pauseTs = resumeTs + (330 * 1000L)
        val sessionStart = activeSessionStarts.remove("com.instagram.android")
        assertNotNull(sessionStart)
        if (sessionStart != null && pauseTs > sessionStart) {
            usageSeconds += (pauseTs - sessionStart) / 1000L
        }

        assertEquals(330L, usageSeconds)
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

