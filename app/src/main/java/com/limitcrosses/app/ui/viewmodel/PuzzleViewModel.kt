package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.PuzzleQuestion
import com.limitcrosses.app.domain.repository.AuthRepository
import com.limitcrosses.app.domain.repository.ExtensionRepository
import com.limitcrosses.app.core.puzzle.PuzzleQuestionBank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class PuzzleUiState(
    val currentLevel: Int = 1, // 1..5
    val currentQuestion: PuzzleQuestion,
    val selectedOptionIndex: Int? = null,
    val isHintRevealed: Boolean = false,
    val isHintUsedInSession: Boolean = false, // Max 1 hint allowed across entire 5-puzzle run
    val isAllCompleted: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

class PuzzleViewModel(
    private val extensionRepository: ExtensionRepository,
    private val authRepository: AuthRepository,
    private val localDataStorage: com.limitcrosses.app.data.local.LocalDataStorage? = null
) : ViewModel() {

    // =========================================================================
    // PROCEDURAL DETERMINISTIC QUESTION BANK (1,000+ UNIQUE COGNITIVE CHALLENGES)
    // Tracks seen questions across runs and persists history so users never receive
    // duplicate questions even after process recreation.
    // =========================================================================

    private val seenQuestions = mutableMapOf<Int, MutableSet<String>>()

    private fun getNextQuestionForLevel(level: Int): PuzzleQuestion {
        val pool = PuzzleQuestionBank.getQuestionsForLevel(level)
        val persistedSeen = localDataStorage?.getSeenPuzzlePrompts(level) ?: emptySet()
        val seenSet = seenQuestions.getOrPut(level) { persistedSeen.toMutableSet() }
        seenSet.addAll(persistedSeen)

        val available = pool.filter { it.prompt !in seenSet }
        val chosen = if (available.isNotEmpty()) {
            available.random()
        } else {
            // When all questions in this level (200+) have been seen, reset history and choose anew
            seenSet.clear()
            localDataStorage?.clearSeenPuzzlePrompts(level)
            pool.random()
        }
        seenSet.add(chosen.prompt)
        localDataStorage?.saveSeenPuzzlePrompt(level, chosen.prompt)
        return chosen
    }

    private fun generateFreshQuestions(): List<PuzzleQuestion> {
        return listOf(
            getNextQuestionForLevel(1),
            getNextQuestionForLevel(2),
            getNextQuestionForLevel(3),
            getNextQuestionForLevel(4),
            getNextQuestionForLevel(5)
        )
    }

    // Current session's active 5 questions (drawn from 1,000+ question bank)
    private var activeSessionQuestions: List<PuzzleQuestion> = generateFreshQuestions()

    private val _uiState = MutableStateFlow(
        PuzzleUiState(
            currentLevel = 1,
            currentQuestion = activeSessionQuestions[0],
            isHintRevealed = false,
            isHintUsedInSession = false
        )
    )
    val uiState = _uiState.asStateFlow()

    fun checkAndPrepareRun() {
        viewModelScope.launch {
            val user = authRepository.getActiveUser() ?: return@launch
            val isEligible = extensionRepository.isEligibleToRequest(user.id)
            if (!isEligible) {
                // If not currently eligible, prepare fresh challenge
                startFreshChallenge()
            }
        }
    }

    fun startFreshChallenge() {
        activeSessionQuestions = generateFreshQuestions()
        viewModelScope.launch {
            val user = authRepository.getActiveUser()
            if (user != null) {
                extensionRepository.verifyPuzzleCompletion(user.id, false)
            }
            _uiState.value = PuzzleUiState(
                currentLevel = 1,
                currentQuestion = activeSessionQuestions[0],
                selectedOptionIndex = null,
                isHintRevealed = false,
                isHintUsedInSession = false,
                isAllCompleted = false,
                statusMessage = "Fresh cognitive challenges loaded for this session.",
                isError = false
            )
        }
    }

    /**
     * Reveals the single allowed hint for the current question.
     * Enforces strict constraint: only 1 hint can be used per 5-question session!
     */
    fun useHint() {
        if (_uiState.value.isHintUsedInSession && !_uiState.value.isHintRevealed) {
            // Already used single hint for another question
            _uiState.value = _uiState.value.copy(
                statusMessage = "Hint limit reached: Only 1 hint permitted per 5-puzzle session."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isHintRevealed = true,
            isHintUsedInSession = true,
            statusMessage = "Single-use hint revealed for this challenge."
        )
    }

    /**
     * Mandatory requirement: If user backgrounds the app,
     * the puzzle resets immediately to Level 1 with fresh questions!
     */
    fun resetOnBackground() {
        if (_uiState.value.isAllCompleted) return // Don't reset if already completed

        activeSessionQuestions = generateFreshQuestions()
        viewModelScope.launch {
            val user = authRepository.getActiveUser()
            if (user != null) {
                extensionRepository.verifyPuzzleCompletion(user.id, false)
            }
            _uiState.value = PuzzleUiState(
                currentLevel = 1,
                currentQuestion = activeSessionQuestions[0],
                selectedOptionIndex = null,
                isHintRevealed = false,
                isHintUsedInSession = false,
                isAllCompleted = false,
                statusMessage = "App was backgrounded. Challenges have been reset with new questions.",
                isError = true
            )
        }
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
                    currentQuestion = activeSessionQuestions[nextLevel - 1],
                    selectedOptionIndex = null,
                    isHintRevealed = false, // Hint is hidden for the next question
                    statusMessage = "Level ${_uiState.value.currentLevel} passed! Proceeding to Level $nextLevel.",
                    isError = false
                )
            }
        } else {
            // Failed: Strict rule - Return user immediately to Level 1 with NEW questions
            activeSessionQuestions = generateFreshQuestions()
            viewModelScope.launch {
                val user = authRepository.getActiveUser()
                if (user != null) {
                    extensionRepository.verifyPuzzleCompletion(user.id, false)
                }
                _uiState.value = PuzzleUiState(
                    currentLevel = 1,
                    currentQuestion = activeSessionQuestions[0],
                    selectedOptionIndex = null,
                    isHintRevealed = false,
                    isHintUsedInSession = false,
                    statusMessage = "Incorrect! True discipline requires patience. Resetting to Level 1 with new challenges.",
                    isError = true
                )
            }
        }
    }

    suspend fun markEligibleInRepository() {
        val user = authRepository.getActiveUser()
        if (user != null) {
            extensionRepository.verifyPuzzleCompletion(user.id, true)
        }
    }
}
