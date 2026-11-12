package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.UserAccount
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val currentLanguage: String = "en",
    val isCloudSyncEnabled: Boolean = false,
    val cloudEndpoint: String = "",
    val currentUser: UserAccount? = null,
    val isDeleteConfirmationVisible: Boolean = false,
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val localDataStorage: LocalDataStorage,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            currentLanguage = localDataStorage.getAppLanguage(),
            isCloudSyncEnabled = localDataStorage.isCloudSyncEnabled(),
            cloudEndpoint = localDataStorage.getCloudEndpoint()
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getCurrentUserFlow().collect { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
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

    fun confirmDeleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = authRepository.deleteAccount()
            if (res.isSuccess) {
                onSuccess()
            }
        }
    }
}

