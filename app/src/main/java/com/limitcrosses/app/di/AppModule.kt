package com.limitcrosses.app.di

import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.data.remote.BackendClient
import com.limitcrosses.app.data.remote.LocalMockBackendEngine
import com.limitcrosses.app.data.repository.*
import com.limitcrosses.app.domain.repository.*
import com.limitcrosses.app.platform.usage.UsageStatsHelper
import com.limitcrosses.app.ui.viewmodel.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Local storage & platform helpers
    single { LocalDataStorage(androidContext()) }
    single { UsageStatsHelper(androidContext()) }

    // Backend Client (defaults to LocalMockBackendEngine which handles both offline & simulated backend rules)
    single<BackendClient> { LocalMockBackendEngine(get(), get()) }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<UsageRepository> { UsageRepositoryImpl(get(), get()) }
    single<LimitRepository> { LimitRepositoryImpl(get(), get()) }
    single<ExtensionRepository> { ExtensionRepositoryImpl(get(), get()) }
    single<AdminRepository> { AdminRepositoryImpl(get()) }
    single<CosmosRepository> { CosmosRepositoryImpl() }

    // ViewModels
    viewModel { AuthViewModel(get()) }
    viewModel { DashboardViewModel(get(), get(), get()) }
    viewModel { UsageViewModel(get()) }
    viewModel { PermissionsViewModel(androidContext(), get()) }
    viewModel { LimitSetupViewModel(get(), get()) }
    viewModel { PuzzleViewModel(get(), get(), get()) }
    viewModel { ExtensionViewModel(get(), get()) }
    viewModel { AdminViewModel(get(), get(), get(), get()) }
    viewModel { CosmosViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
}

