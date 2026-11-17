package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.AccountDeletionRequest
import com.limitcrosses.app.core.model.UserAccount
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val currentLanguage: String = "en",
    val currentThemeMode: String = "SYSTEM",
    val isCloudSyncEnabled: Boolean = false,
    val cloudEndpoint: String = "",
    val currentUser: UserAccount? = null,
    val pendingDeletionRequest: AccountDeletionRequest? = null,
    val isDeleteConfirmationVisible: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val localDataStorage: LocalDataStorage,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            currentLanguage = localDataStorage.getAppLanguage(),
            currentThemeMode = localDataStorage.getAppThemeMode(),
            isCloudSyncEnabled = localDataStorage.isCloudSyncEnabled(),
            cloudEndpoint = localDataStorage.getCloudEndpoint()
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getCurrentUserFlow().collect { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
                refreshDeletionStatus()
            }
        }
        viewModelScope.launch {
            localDataStorage.themeModeFlow.collect { theme ->
                _uiState.value = _uiState.value.copy(currentThemeMode = theme)
            }
        }
    }

    fun setThemeMode(mode: String) {
        localDataStorage.setAppThemeMode(mode)
        _uiState.value = _uiState.value.copy(currentThemeMode = mode)
    }

    fun refreshDeletionStatus() {
        viewModelScope.launch {
            val res = authRepository.getPendingDeletionRequest()
            _uiState.value = _uiState.value.copy(pendingDeletionRequest = res.getOrNull())
        }
    }

    fun setLanguage(lang: String) {
        localDataStorage.setAppLanguage(lang)
        _uiState.value = _uiState.value.copy(currentLanguage = lang)
    }

    fun toggleCloudSync(enabled: Boolean) {
        localDataStorage.setCloudSyncEnabled(enabled)
        _uiState.value = _uiState.value.copy(isCloudSyncEnabled = enabled)
    }

    fun setCloudEndpoint(endpoint: String) {
        localDataStorage.setCloudEndpoint(endpoint)
        _uiState.value = _uiState.value.copy(cloudEndpoint = endpoint)
    }

    fun showDeleteConfirmation(show: Boolean) {
        _uiState.value = _uiState.value.copy(isDeleteConfirmationVisible = show)
    }

    fun submitAccountDeletionRequest(reason: String) {
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please state a reason for requesting account deletion.")
            return
        }
        viewModelScope.launch {
            val res = authRepository.requestAccountDeletion(reason.trim())
            res.onSuccess { req ->
                _uiState.value = _uiState.value.copy(
                    isDeleteConfirmationVisible = false,
                    pendingDeletionRequest = req,
                    statusMessage = "Account deletion request sent to your Administrator for review.",
                    errorMessage = null
                )
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Failed to submit deletion request.")
            }
        }
    }

    fun cancelDeletionRequest() {
        viewModelScope.launch {
            val res = authRepository.cancelAccountDeletion()
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    pendingDeletionRequest = null,
                    statusMessage = "Account deletion request cancelled.",
                    errorMessage = null
                )
            }
        }
    }
}
