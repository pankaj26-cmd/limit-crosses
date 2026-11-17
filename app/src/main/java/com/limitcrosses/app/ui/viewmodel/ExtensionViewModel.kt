package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.ExtensionRequest
import com.limitcrosses.app.domain.repository.AuthRepository
import com.limitcrosses.app.domain.repository.ExtensionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExtensionUiState(
    val isEligible: Boolean = false,
    val isLoading: Boolean = false,
    val requests: List<ExtensionRequest> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ExtensionViewModel(
    private val extensionRepository: ExtensionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtensionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val user = authRepository.getActiveUser() ?: return@launch
            val eligible = extensionRepository.isEligibleToRequest(user.id)
            _uiState.value = _uiState.value.copy(isEligible = eligible)

            extensionRepository.getExtensionRequestsFlow(user.id).collect { list ->
                _uiState.value = _uiState.value.copy(requests = list)
            }
        }
    }

    fun submitRequest(minutes: Int, reason: String, onSuccess: () -> Unit) {
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please explain a constructive reason.")
            return
        }

        val clamped = minutes.coerceIn(1, 20)
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val user = authRepository.getActiveUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Session expired.")
                return@launch
            }

            val result = extensionRepository.submitRequest(user.id, clamped, reason.trim())
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isEligible = false,
                    successMessage = "Extension request submitted to your Administrator."
                )
                onSuccess()
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Submission failed.")
            }
        }
    }
}

