package com.limitcrosses.app.ui.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object InviteAcceptance : Screen("invite_acceptance")
    object AccountCreation : Screen("account_creation/{inviteCode}") {
        fun createRoute(inviteCode: String) = "account_creation/$inviteCode"
    }
    object AdminBootstrap : Screen("admin_bootstrap")
    object Login : Screen("login")
    object PasswordReset : Screen("password_reset")
    object PermissionsChecklist : Screen("permissions_checklist")
    object LimitSetup : Screen("limit_setup")
    object Dashboard : Screen("dashboard")
    object UsageDetails : Screen("usage_details")
    object LimitReached : Screen("limit_reached")
    object PuzzleLevels : Screen("puzzle_levels")
    object ExtensionRequests : Screen("extension_requests")
    object AdminDashboard : Screen("admin_dashboard")
    object LearnCosmos : Screen("learn_cosmos")
    object Settings : Screen("settings")
    object Privacy : Screen("privacy")
}

