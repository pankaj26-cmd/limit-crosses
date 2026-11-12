package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.DailyLimitConfig
import com.limitcrosses.app.domain.repository.AuthRepository
import com.limitcrosses.app.domain.repository.LimitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LimitSetupUiState(
    val currentSelectedMinutes: Int = 30, // 0..45
    val isLocked: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LimitSetupViewModel(
    private val limitRepository: LimitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LimitSetupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadExisting()
    }

    private fun loadExisting() {
        viewModelScope.launch {
            val user = authRepository.getActiveUser() ?: return@launch
            val config = limitRepository.fetchLimitConfig(user.id)
            _uiState.value = _uiState.value.copy(
                currentSelectedMinutes = config.dailyLimitMinutes,
                isLocked = config.isLocked
            )
        }
    }

    fun onSliderChanged(newMinutes: Int) {
        if (!_uiState.value.isLocked) {
            _uiState.value = _uiState.value.copy(
                currentSelectedMinutes = newMinutes.coerceIn(0, 45)
            )
        }
    }

    fun confirmAndLock(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = authRepository.getActiveUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Active user session required.")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = limitRepository.confirmAndLockLimit(
                userId = user.id,
                minutes = _uiState.value.currentSelectedMinutes
            )
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess { config ->
                _uiState.value = _uiState.value.copy(isLocked = config.isLocked)
                onSuccess()
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Failed to lock limit.")
            }
        }
    }
}

