package com.limitcrosses.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.limitcrosses.app.core.model.UserRole
import com.limitcrosses.app.core.model.UserStatus
import com.limitcrosses.app.ui.screens.*
import com.limitcrosses.app.ui.viewmodel.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun LimitCrossesNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Welcome.route
) {
    val authViewModel: AuthViewModel = koinViewModel()
    val dashboardViewModel: DashboardViewModel = koinViewModel()
    val usageViewModel: UsageViewModel = koinViewModel()
    val permissionsViewModel: PermissionsViewModel = koinViewModel()
    val limitSetupViewModel: LimitSetupViewModel = koinViewModel()
    val puzzleViewModel: PuzzleViewModel = koinViewModel()
    val extensionViewModel: ExtensionViewModel = koinViewModel()
    val adminViewModel: AdminViewModel = koinViewModel()
    val cosmosViewModel: CosmosViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToInvite = { navController.navigate(Screen.InviteAcceptance.route) },
                onNavigateToBootstrap = { navController.navigate(Screen.AdminBootstrap.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) }
            )
        }

        composable(Screen.InviteAcceptance.route) {
            InviteAcceptanceScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onInviteValidated = { validCode ->
                    navController.navigate(Screen.AccountCreation.createRoute(validCode))
                }
            )
        }

        composable(
            route = Screen.AccountCreation.route,
            arguments = listOf(navArgument("inviteCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val inviteCode = backStackEntry.arguments?.getString("inviteCode") ?: ""
            AccountCreationScreen(
                inviteCode = inviteCode,
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate(Screen.PermissionsChecklist.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminBootstrap.route) {
            AdminBootstrapScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onBootstrapped = {
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onForgotPassword = { navController.navigate(Screen.PasswordReset.route) },
                onLoginSuccess = { user ->
                    when {
                        user.role == UserRole.ADMIN -> {
                            navController.navigate(Screen.AdminDashboard.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                        user.status == UserStatus.PENDING_LIMIT_SETUP -> {
                            navController.navigate(Screen.PermissionsChecklist.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                        else -> {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(Screen.PasswordReset.route) {
            PasswordResetScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PermissionsChecklist.route) {
            PermissionsChecklistScreen(
                viewModel = permissionsViewModel,
                onContinue = {
                    navController.navigate(Screen.LimitSetup.route)
                }
            )
        }

        composable(Screen.LimitSetup.route) {
            LimitSetupScreen(
                viewModel = limitSetupViewModel,
                onConfirmed = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.LimitSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                dashboardViewModel = dashboardViewModel,
                authViewModel = authViewModel,
                onNavigateToUsage = { navController.navigate(Screen.UsageDetails.route) },
                onNavigateToCosmos = { navController.navigate(Screen.LearnCosmos.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAdmin = { navController.navigate(Screen.AdminDashboard.route) },
                onNavigateToPuzzles = { navController.navigate(Screen.PuzzleLevels.route) },
                onNavigateToLimitReached = { navController.navigate(Screen.LimitReached.route) },
                onNavigateToPermissions = { navController.navigate(Screen.PermissionsChecklist.route) },
                onLoggedOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.UsageDetails.route) {
            UsageDetailsScreen(
                viewModel = usageViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LimitReached.route) {
            LimitReachedScreen(
                onNavigateToPuzzles = { navController.navigate(Screen.PuzzleLevels.route) },
                onNavigateToCosmos = { navController.navigate(Screen.LearnCosmos.route) }
            )
        }

        composable(Screen.PuzzleLevels.route) {
            PuzzleLevelsScreen(
                viewModel = puzzleViewModel,
                onBack = { navController.popBackStack() },
                onProceedToRequest = {
                    navController.navigate(Screen.ExtensionRequests.route) {
                        popUpTo(Screen.PuzzleLevels.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ExtensionRequests.route) {
            ExtensionRequestsScreen(
                viewModel = extensionViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPuzzles = { navController.navigate(Screen.PuzzleLevels.route) }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                viewModel = adminViewModel,
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Welcome.route)
                    }
                },
                onLoggedOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LearnCosmos.route) {
            LearnCosmosScreen(
                viewModel = cosmosViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onLoggedOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAccountDeleted = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

