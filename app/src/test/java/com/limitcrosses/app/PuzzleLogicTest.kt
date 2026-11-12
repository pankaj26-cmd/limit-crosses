package com.limitcrosses.app

import org.junit.Assert.*
import org.junit.Test

class PuzzleLogicTest {

    @Test
    fun testLevel1SequenceLogic() {
        // Sequence: 3, 7, 15, 31, 63, ?
        // Rule: x * 2 + 1
        val lastTerm = 63
        val nextTerm = (lastTerm * 2) + 1
        assertEquals(127, nextTerm)
    }

    @Test
    fun testLevel2DeductionOrder() {
        // Alpha > Beta
        // Beta > Gamma > Delta
        // Order: Alpha > Beta > Gamma > Delta
        val itemsOrdered = listOf("Alpha", "Beta", "Gamma", "Delta")
        val secondHeaviest = itemsOrdered[1]
        assertEquals("Beta", secondHeaviest)
    }

    @Test
    fun testLevel3TransformationFormula() {
        // (X, Y) -> (2X + Y, X - Y)
        // For (3, 2):
        val x = 3
        val y = 2
        val newX = 2 * x + y
        val newY = x - y
        assertEquals(8, newX)
        assertEquals(1, newY)
    }

    @Test
    fun testLevel4CryptarithmeticSystem() {
        // S + T = 14
        // S - T = 4
        // 2S = 18 -> S = 9, T = 5
        // S * T = 9 * 5 = 45
        val square = (14 + 4) / 2
        val triangle = 14 - square
        assertEquals(9, square)
        assertEquals(5, triangle)
        assertEquals(45, square * triangle)
    }

    @Test
    fun testLevel5ClockDriftDifference() {
        // Master loses 2 min/hr
        // Slave gains 1 min/hr
        // Relative drift = 3 min/hr
        // Elapsed = 6 hours (12 PM to 6 PM)
        val elapsedHours = 6
        val driftRate = 2 + 1
        val totalDiffMinutes = elapsedHours * driftRate
        assertEquals(18, totalDiffMinutes)
    }

    @Test
    fun testFailureReturnsUserToLevel1() {
        var currentLevel = 4
        val submittedAnswerIsCorrect = false

        if (!submittedAnswerIsCorrect) {
            currentLevel = 1
        }

        assertEquals(1, currentLevel)
    }
}

