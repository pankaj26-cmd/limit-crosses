package com.limitcrosses.app.data.repository

import com.limitcrosses.app.core.model.DailyLimitConfig
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.data.remote.BackendClient
import com.limitcrosses.app.domain.repository.LimitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LimitRepositoryImpl(
    private val localDataStorage: LocalDataStorage,
    private val backendClient: BackendClient
) : LimitRepository {

    private val limitFlow = MutableStateFlow<DailyLimitConfig?>(null)

    override fun getLimitConfigFlow(userId: String): Flow<DailyLimitConfig?> {
        val current = localDataStorage.getLimitConfig(userId)
        limitFlow.value = current
        return limitFlow.asStateFlow()
    }

    override suspend fun fetchLimitConfig(userId: String): DailyLimitConfig {
        val result = backendClient.getDailyLimit(userId)
        val config = result.getOrElse {
            localDataStorage.getLimitConfig(userId) ?: DailyLimitConfig(userId = userId, dailyLimitMinutes = 30)
        }
        limitFlow.value = config
        return config
    }

    override suspend fun confirmAndLockLimit(userId: String, minutes: Int): Result<DailyLimitConfig> {
        val result = backendClient.setInitialLimit(userId, minutes)
        if (result.isSuccess) {
            limitFlow.value = result.getOrThrow()
        }
        return result
    }
}

