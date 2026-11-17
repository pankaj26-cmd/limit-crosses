package com.limitcrosses.app.data.repository

import com.limitcrosses.app.core.model.*
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.data.remote.BackendClient
import com.limitcrosses.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val localDataStorage: LocalDataStorage,
    private val backendClient: BackendClient
) : AuthRepository {

    private val currentUserFlow = MutableStateFlow<UserAccount?>(null)

    init {
        // Restore session if available
        val savedId = localDataStorage.getCurrentUserId()
        if (savedId != null) {
            val user = localDataStorage.getUserById(savedId)
            if (user != null && user.status != UserStatus.DELETED) {
                currentUserFlow.value = user
            }
        }
    }

    override fun getCurrentUserFlow(): Flow<UserAccount?> = currentUserFlow.asStateFlow()

    override suspend fun getActiveUser(): UserAccount? = currentUserFlow.value

    override suspend fun refreshCurrentUser(): UserAccount? {
        val savedId = localDataStorage.getCurrentUserId() ?: return null
        val user = localDataStorage.getUserById(savedId)
        if (user != null && user.status != UserStatus.DELETED) {
            currentUserFlow.value = user
        }
        return user
    }

    override suspend fun login(email: String, passwordPlain: String): Result<UserAccount> {
        val result = backendClient.login(email, passwordPlain)
        if (result.isSuccess) {
            val user = result.getOrThrow()
            localDataStorage.setCurrentUserId(user.id)
            currentUserFlow.value = user
        }
        return result
    }

    override suspend fun bootstrapAdmin(
        fullName: String,
        email: String,
        passwordPlain: String,
        secretKey: String
    ): Result<UserAccount> {
        val result = backendClient.bootstrapAdmin(fullName, email, passwordPlain, secretKey)
        if (result.isSuccess) {
            val user = result.getOrThrow()
            localDataStorage.setCurrentUserId(user.id)
            currentUserFlow.value = user
        }
        return result
    }

    override suspend fun validateInvite(code: String): Result<InviteCode> {
        return backendClient.validateInvite(code)
    }

    override suspend fun register(
        fullName: String,
        email: String,
        passwordPlain: String,
        inviteCode: String,
        socialHandles: SocialHandles
    ): Result<UserAccount> {
        val result = backendClient.registerUser(fullName, email, passwordPlain, inviteCode, socialHandles)
        if (result.isSuccess) {
            val user = result.getOrThrow()
            localDataStorage.setCurrentUserId(user.id)
            currentUserFlow.value = user
        }
        return result
    }

    override suspend fun logout(): Result<Unit> {
        localDataStorage.setCurrentUserId(null)
        currentUserFlow.value = null
        return Result.success(Unit)
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return backendClient.resetPassword(email)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val current = currentUserFlow.value ?: return Result.failure(IllegalStateException("No logged in user"))
        val result = backendClient.deleteAccount(current.id)
        if (result.isSuccess) {
            localDataStorage.setCurrentUserId(null)
            currentUserFlow.value = null
        }
        return result
    }

    override suspend fun requestAccountDeletion(reason: String): Result<AccountDeletionRequest> {
        val current = currentUserFlow.value ?: return Result.failure(IllegalStateException("No logged in user"))
        return backendClient.requestAccountDeletion(current.id, reason)
    }

    override suspend fun cancelAccountDeletion(): Result<Unit> {
        val current = currentUserFlow.value ?: return Result.failure(IllegalStateException("No logged in user"))
        return backendClient.cancelAccountDeletion(current.id)
    }

    override suspend fun getPendingDeletionRequest(): Result<AccountDeletionRequest?> {
        val current = currentUserFlow.value ?: return Result.success(null)
        return backendClient.getPendingDeletionRequest(current.id)
    }
}

