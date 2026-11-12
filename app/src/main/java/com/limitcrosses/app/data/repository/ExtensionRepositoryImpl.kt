package com.limitcrosses.app.data.repository

import com.limitcrosses.app.core.model.ExtensionRequest
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.data.remote.BackendClient
import com.limitcrosses.app.domain.repository.ExtensionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExtensionRepositoryImpl(
    private val localDataStorage: LocalDataStorage,
    private val backendClient: BackendClient
) : ExtensionRepository {

    private val extensionsFlow = MutableStateFlow<List<ExtensionRequest>>(emptyList())

    override fun getExtensionRequestsFlow(userId: String): Flow<List<ExtensionRequest>> {
        refreshForUser(userId)
        return extensionsFlow.asStateFlow()
    }

    private fun refreshForUser(userId: String) {
        val userReqs = localDataStorage.getAllExtensionRequests().filter { it.userId == userId }
        extensionsFlow.value = userReqs
    }

    override suspend fun submitRequest(
        userId: String,
        minutes: Int,
        reason: String
    ): Result<ExtensionRequest> {
        val result = backendClient.submitExtensionRequest(userId, minutes, reason)
        if (result.isSuccess) {
            refreshForUser(userId)
            // Reset puzzle eligibility once request has been submitted
            localDataStorage.setPuzzleEligible(userId, false)
        }
        return result
    }

    override suspend fun verifyPuzzleCompletion(
        userId: String,
        passedAllFive: Boolean
    ): Result<Unit> {
        localDataStorage.setPuzzleEligible(userId, passedAllFive)
        return Result.success(Unit)
    }

    override suspend fun isEligibleToRequest(userId: String): Boolean {
        return localDataStorage.isPuzzleEligible(userId)
    }
}

