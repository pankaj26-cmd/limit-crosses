package com.limitcrosses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.limitcrosses.app.core.model.*
import com.limitcrosses.app.domain.repository.AdminRepository
import com.limitcrosses.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val selectedTab: Int = 0, // 0: Invites, 1: Users, 2: Extensions, 3: Audit
    val managedUsers: List<UserAccount> = emptyList(),
    val pendingExtensions: List<ExtensionRequest> = emptyList(),
    val activeInvites: List<InviteCode> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val generatedCode: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun refreshAll() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val usersRes = adminRepository.listManagedUsers()
            val extRes = adminRepository.listPendingExtensions()
            val invRes = adminRepository.listAllInvites()
            val auditRes = adminRepository.listAuditLogs()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                managedUsers = usersRes.getOrDefault(emptyList()),
                pendingExtensions = extRes.getOrDefault(emptyList()),
                activeInvites = invRes.getOrDefault(emptyList()),
                auditLogs = auditRes.getOrDefault(emptyList())
            )
        }
    }

    fun createInvite(limitMinutes: Int, validDays: Int, notes: String) {
        viewModelScope.launch {
            val admin = authRepository.getActiveUser() ?: return@launch
            val res = adminRepository.generateInvite(admin.id, limitMinutes, validDays, notes)
            res.onSuccess { invite ->
                _uiState.value = _uiState.value.copy(
                    generatedCode = invite.code,
                    successMessage = "Invite ${invite.code} created successfully."
                )
                refreshAll()
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Failed to generate invite.")
            }
        }
    }

    fun modifyUserLimit(targetUserId: String, newMinutes: Int, reason: String) {
        if (reason.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Audit reason is required.")
            return
        }
        viewModelScope.launch {
            val admin = authRepository.getActiveUser() ?: return@launch
            val res = adminRepository.modifyUserLimit(admin.id, targetUserId, newMinutes, reason)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "User limit updated to $newMinutes min.")
                refreshAll()
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Failed to modify limit.")
            }
        }
    }

    fun reviewExtension(
        requestId: String,
        newStatus: ExtensionStatus,
        grantedMinutes: Int,
        notes: String?
    ) {
        viewModelScope.launch {
            val admin = authRepository.getActiveUser() ?: return@launch
            val res = adminRepository.reviewExtension(admin.id, requestId, newStatus, grantedMinutes, notes)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(successMessage = "Extension request $newStatus.")
                refreshAll()
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(errorMessage = ex.message ?: "Review failed.")
            }
        }
    }
}

