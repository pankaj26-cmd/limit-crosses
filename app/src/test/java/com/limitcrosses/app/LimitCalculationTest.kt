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
}

