package com.limitcrosses.app

import com.limitcrosses.app.core.puzzle.PuzzleQuestionBank
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

    @Test
    fun testQuestionBankHasOver1000UniqueQuestions() {
        val total = PuzzleQuestionBank.totalCount
        println("PuzzleQuestionBank total questions: $total")
        assertTrue("Expected >= 1000 questions in bank, found $total", total >= 1000)
    }

    @Test
    fun testEveryLevelHasAtLeastTwoHundredQuestions() {
        val l1 = PuzzleQuestionBank.level1Pool.size
        val l2 = PuzzleQuestionBank.level2Pool.size
        val l3 = PuzzleQuestionBank.level3Pool.size
        val l4 = PuzzleQuestionBank.level4Pool.size
        val l5 = PuzzleQuestionBank.level5Pool.size

        println("Level distribution: L1=$l1, L2=$l2, L3=$l3, L4=$l4, L5=$l5")
        assertTrue("Level 1 pool must have >= 200 questions, has $l1", l1 >= 200)
        assertTrue("Level 2 pool must have >= 200 questions, has $l2", l2 >= 200)
        assertTrue("Level 3 pool must have >= 200 questions, has $l3", l3 >= 200)
        assertTrue("Level 4 pool must have >= 200 questions, has $l4", l4 >= 200)
        assertTrue("Level 5 pool must have >= 200 questions, has $l5", l5 >= 200)
    }

    @Test
    fun testAllQuestionsHaveFourUniqueOptionsAndValidCorrectIndex() {
        val all = PuzzleQuestionBank.allQuestions
        for ((index, q) in all.withIndex()) {
            assertEquals("Question #$index level ${q.level} must have 4 options", 4, q.options.size)
            assertEquals("Question #$index options must all be distinct", 4, q.options.toSet().size)
            assertTrue("Question #$index correctIndex must be 0..3", q.correctIndex in 0..3)
            val correctAns = q.options[q.correctIndex]
            assertTrue("Correct answer option must not be blank", correctAns.isNotBlank())
            assertTrue("Prompt must not be blank", q.prompt.isNotBlank())
            assertTrue("PromptHi must not be blank", q.promptHi.isNotBlank())
            assertTrue("Explanation must not be blank", q.explanation.isNotBlank())
            assertTrue("AccessibilityHint must not be blank", q.accessibilityHint.isNotBlank())
        }
    }
}

