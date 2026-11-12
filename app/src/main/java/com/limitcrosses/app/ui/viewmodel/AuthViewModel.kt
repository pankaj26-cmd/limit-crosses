package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.SocialHandles
import com.limitcrosses.app.core.model.UserAccount
import com.limitcrosses.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: UserAccount? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val validatedInviteCode: String? = null,
    val assignedDefaultLimit: Int = 30
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getCurrentUserFlow().collect { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun login(email: String, passwordPlain: String, onSuccess: (UserAccount) -> Unit) {
        if (email.isBlank() || passwordPlain.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter both email and password.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.login(email.trim(), passwordPlain)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { user ->
                onSuccess(user)
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Authentication failed")
            }
        }
    }

    fun validateInviteCode(code: String, onSuccess: (String) -> Unit) {
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter an invitation code.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.validateInvite(code.trim().uppercase())
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { invite ->
                _uiState.value = _uiState.value.copy(
                    validatedInviteCode = invite.code,
                    assignedDefaultLimit = invite.assignedDefaultLimitMinutes
                )
                onSuccess(invite.code)
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Invalid invitation code.")
            }
        }
    }

    fun register(
        fullName: String,
        email: String,
        passwordPlain: String,
        inviteCode: String,
        socialHandles: SocialHandles,
        onSuccess: (UserAccount) -> Unit
    ) {
        if (fullName.isBlank() || email.isBlank() || passwordPlain.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "All registration fields are required.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.register(
                fullName = fullName.trim(),
                email = email.trim(),
                passwordPlain = passwordPlain,
                inviteCode = inviteCode.trim().uppercase(),
                socialHandles = socialHandles
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { user ->
                onSuccess(user)
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Registration failed.")
            }
        }
    }

    fun bootstrapAdmin(
        fullName: String,
        email: String,
        passwordPlain: String,
        secretKey: String,
        onSuccess: (UserAccount) -> Unit
    ) {
        if (fullName.isBlank() || email.isBlank() || passwordPlain.isBlank() || secretKey.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "All bootstrap fields including Master Key are required.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.bootstrapAdmin(
                fullName = fullName.trim(),
                email = email.trim(),
                passwordPlain = passwordPlain,
                secretKey = secretKey.trim()
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { user ->
                onSuccess(user)
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Admin bootstrap failed.")
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your email.")
            return
        }
        viewModelScope.launch {
            authRepository.resetPassword(email.trim())
            _uiState.value = _uiState.value.copy(
                successMessage = "If this email is registered, recovery instructions have been dispatched."
            )
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onSuccess()
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }
}

