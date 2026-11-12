package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.PuzzleQuestion
import com.limitcrosses.app.domain.repository.AuthRepository
import com.limitcrosses.app.domain.repository.ExtensionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PuzzleUiState(
    val currentLevel: Int = 1, // 1..5
    val currentQuestion: PuzzleQuestion,
    val selectedOptionIndex: Int? = null,
    val isAccessibilityMode: Boolean = false,
    val isAllCompleted: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

class PuzzleViewModel(
    private val extensionRepository: ExtensionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // 5 Deterministic Logic Puzzles:
    // Level 1: Number Sequence (Fibonacci / Difference patterns)
    // Level 2: Deductive Logic Grid (Deterministic relation)
    // Level 3: Dual-step Geometric Pattern
    // Level 4: Symbolic Math Equation System
    // Level 5: Constraint Deduction Challenge
    private val puzzles = listOf(
        PuzzleQuestion(
            level = 1,
            prompt = "Determine the next number in the deterministic sequence: 3, 7, 15, 31, 63, ?",
            promptHi = "शृंखला में अगली संख्या ज्ञात करें: 3, 7, 15, 31, 63, ?",
            options = listOf("125", "127", "129", "131"),
            correctIndex = 1, // 63 * 2 + 1 = 127
            explanation = "Rule: Each term is obtained by doubling the preceding number and adding 1: (63 × 2) + 1 = 127.",
            explanationHi = "नियम: प्रत्येक पद पिछले पद को दोगुना करके 1 जोड़ने पर प्राप्त होता है: (63 × 2) + 1 = 127.",
            accessibilityHint = "Accessibility: Double 63 and add 1.",
            accessibilityHintHi = "सुलभता संकेत: 63 को दोगुना करें और 1 जोड़ें।"
        ),
        PuzzleQuestion(
            level = 2,
            prompt = "Four items (Alpha, Beta, Gamma, Delta) are ordered by weight. Alpha is heavier than Beta. Gamma is heavier than Delta but lighter than Beta. Which item is the second heaviest?",
            promptHi = "चार वस्तुएं (अल्फा, बीटा, गामा, डेल्टा) भार के क्रम में हैं। अल्फा, बीटा से भारी है। गामा, डेल्टा से भारी है लेकिन बीटा से हल्का है। दूसरी सबसे भारी वस्तु कौन सी है?",
            options = listOf("Alpha", "Beta", "Gamma", "Delta"),
            correctIndex = 1, // Alpha > Beta > Gamma > Delta. Second heaviest is Beta.
            explanation = "From the constraints: Alpha > Beta, and Beta > Gamma > Delta. The strict order is Alpha > Beta > Gamma > Delta. Beta is second heaviest.",
            explanationHi = "शर्तों के अनुसार: अल्फा > बीटा > गामा > डेल्टा। अतः दूसरी सबसे भारी वस्तु बीटा है।",
            accessibilityHint = "Accessibility: The chain is Alpha > Beta > Gamma > Delta.",
            accessibilityHintHi = "सुलभता संकेत: क्रम अल्फा > बीटा > गामा > डेल्टा है।"
        ),
        PuzzleQuestion(
            level = 3,
            prompt = "Consider the transformation rule: (X, Y) -> (2X + Y, X - Y). Applying this rule to (4, 1) yields (9, 3). Applying it to (9, 3) yields (21, 6). What does applying it to (3, 2) yield?",
            promptHi = "रूपांतरण नियम: (X, Y) -> (2X + Y, X - Y)। यदि (3, 2) पर यह नियम लागू करें, तो क्या परिणाम मिलेगा?",
            options = listOf("(7, 1)", "(8, 1)", "(7, 2)", "(8, 5)"),
            correctIndex = 1, // 2*(3) + 2 = 8, 3 - 2 = 1 -> (8, 1)
            explanation = "Calculation: 2(3) + 2 = 8, and 3 - 2 = 1. Therefore the coordinate pair is (8, 1).",
            explanationHi = "गणना: 2(3) + 2 = 8, और 3 - 2 = 1। अतः निर्देशांक (8, 1) है।",
            accessibilityHint = "Accessibility: First value is (2*3)+2=8; second value is 3-2=1.",
            accessibilityHintHi = "सुलभता संकेत: पहला मान (2*3)+2=8; दूसरा मान 3-2=1 है।"
        ),
        PuzzleQuestion(
            level = 4,
            prompt = "In a base-10 cryptarithmetic balance: [Square] + [Triangle] = 14, and [Square] - [Triangle] = 4. What is the value of [Square] × [Triangle]?",
            promptHi = "यदि [वर्ग] + [त्रिभुज] = 14, तथा [वर्ग] - [त्रिभुज] = 4 है, तो [वर्ग] × [त्रिभुज] का मान क्या होगा?",
            options = listOf("40", "45", "48", "54"),
            correctIndex = 1, // 2*Square = 18 -> Square = 9, Triangle = 5. Product = 9*5 = 45.
            explanation = "Solving simultaneously: Square = (14 + 4) / 2 = 9. Triangle = 14 - 9 = 5. Product: 9 × 5 = 45.",
            explanationHi = "समीकरण हल करने पर: वर्ग = 9, त्रिभुज = 5। गुणनफल: 9 × 5 = 45।",
            accessibilityHint = "Accessibility: Square is 9, Triangle is 5. Multiply 9 by 5.",
            accessibilityHintHi = "सुलभता संकेत: वर्ग 9 है, त्रिभुज 5 है। 9 को 5 से गुणा करें।"
        ),
        PuzzleQuestion(
            level = 5,
            prompt = "A master clock loses 2 minutes every hour. A slave clock gains 1 minute every hour. If both are synchronized precisely at 12:00 PM, how many minutes apart will they be at 6:00 PM on the same day?",
            promptHi = "एक घड़ी प्रति घंटे 2 मिनट धीमी होती है। दूसरी घड़ी प्रति घंटे 1 मिनट तेज होती है। यदि दोनों को दोपहर 12:00 बजे मिलाया जाए, तो शाम 6:00 बजे उनके बीच कितने मिनट का अंतर होगा?",
            options = listOf("12 minutes", "15 minutes", "18 minutes", "24 minutes"),
            correctIndex = 2, // 6 hours elapsed. Master loses 12 min. Slave gains 6 min. Difference = 12 + 6 = 18 min.
            explanation = "Between 12:00 PM and 6:00 PM, 6 hours elapse. The relative drift rate is (2 + 1) = 3 minutes per hour. 6 × 3 = 18 minutes difference.",
            explanationHi = "12:00 से 6:00 तक 6 घंटे होते हैं। दोनों के बीच प्रति घंटे 3 मिनट का अंतर बनता है। 6 × 3 = 18 मिनट।",
            accessibilityHint = "Accessibility: 6 hours elapsed multiplied by (2+1)=3 min per hour gives 18 minutes.",
            accessibilityHintHi = "सुलभता संकेत: 6 घंटे × 3 मिनट = 18 मिनट अंतर।"
        )
    )

    private val _uiState = MutableStateFlow(
        PuzzleUiState(currentQuestion = puzzles[0])
    )
    val uiState = _uiState.asStateFlow()

    fun toggleAccessibilityMode() {
        _uiState.value = _uiState.value.copy(
            isAccessibilityMode = !_uiState.value.isAccessibilityMode
        )
    }

    fun selectOption(index: Int) {
        _uiState.value = _uiState.value.copy(selectedOptionIndex = index)
    }

    fun submitAnswer() {
        val selected = _uiState.value.selectedOptionIndex ?: return
        val currentQ = _uiState.value.currentQuestion

        if (selected == currentQ.correctIndex) {
            // Correct answer
            if (_uiState.value.currentLevel >= 5) {
                // Completed all 5 consecutive levels!
                viewModelScope.launch {
                    val user = authRepository.getActiveUser()
                    if (user != null) {
                        extensionRepository.verifyPuzzleCompletion(user.id, true)
                    }
                    _uiState.value = _uiState.value.copy(
                        isAllCompleted = true,
                        statusMessage = "All 5 levels passed! You are now eligible to request an extension.",
                        isError = false
                    )
                }
            } else {
                val nextLevel = _uiState.value.currentLevel + 1
                _uiState.value = _uiState.value.copy(
                    currentLevel = nextLevel,
                    currentQuestion = puzzles[nextLevel - 1],
                    selectedOptionIndex = null,
                    statusMessage = "Level ${_uiState.value.currentLevel} passed! Proceeding to Level $nextLevel.",
                    isError = false
                )
            }
        } else {
            // Failed: Strict rule - Return user immediately to Level 1
            viewModelScope.launch {
                val user = authRepository.getActiveUser()
                if (user != null) {
                    extensionRepository.verifyPuzzleCompletion(user.id, false)
                }
                _uiState.value = _uiState.value.copy(
                    currentLevel = 1,
                    currentQuestion = puzzles[0],
                    selectedOptionIndex = null,
                    statusMessage = "Incorrect answer! True discipline requires patience. Resetting to Level 1.",
                    isError = true
                )
            }
        }
    }
}

