package com.limitcrosses.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.limitcrosses.app.core.model.UserRole
import com.limitcrosses.app.core.model.UserStatus
import com.limitcrosses.app.data.local.LocalDataStorage
import com.limitcrosses.app.domain.repository.AuthRepository
import com.limitcrosses.app.platform.service.AppMonitorBlockerService
import com.limitcrosses.app.ui.navigation.LimitCrossesNavGraph
import com.limitcrosses.app.ui.navigation.Screen
import com.limitcrosses.app.ui.theme.AppThemeMode
import com.limitcrosses.app.ui.theme.LimitCrossesTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()
    private val localDataStorage: LocalDataStorage by inject()
    private var navController: NavHostController? = null

    private var pendingLimitReachedRoute by mutableStateOf(false)
    private var pendingInviteCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if opened due to limit reached event
        if (intent?.action == AppMonitorBlockerService.ACTION_LIMIT_REACHED) {
            pendingLimitReachedRoute = true
        }

        // Check for incoming invite deep links (e.g. limitcrosses://invite?code=XYZ)
        val deepLinkCode = intent?.data?.getQueryParameter("code")
            ?: intent?.getStringExtra("invite_code")
        if (!deepLinkCode.isNullOrBlank()) {
            pendingInviteCode = deepLinkCode
        }

        setContent {
            val themeModeString by localDataStorage.themeModeFlow.collectAsState()
            val currentThemeMode = AppThemeMode.fromId(themeModeString)

            LimitCrossesTheme(
                themeMode = currentThemeMode,
                onThemeChange = { newMode ->
                    localDataStorage.setAppThemeMode(newMode.id)
                }
            ) {
                com.limitcrosses.app.ui.components.CosmicBackgroundContainer {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.ui.graphics.Color.Transparent
                    ) {
                        val currentUserId = localDataStorage.getCurrentUserId()
                        val initialUser = currentUserId?.let { localDataStorage.getUserById(it) }
                        val user by authRepository.getCurrentUserFlow().collectAsState(initial = initialUser)
                        val hostNavController = rememberNavController().also { navController = it }

                        // Check if current user has already configured and locked their limit
                        val limitConfig = user?.let { localDataStorage.getLimitConfig(it.id) }
                        val isLimitLocked = limitConfig?.isLocked == true

                        val startDestination = when {
                            pendingLimitReachedRoute -> Screen.LimitReached.route
                            user == null -> Screen.Welcome.route
                            user?.role == UserRole.ADMIN -> Screen.AdminDashboard.route
                            (user?.status == UserStatus.PENDING_LIMIT_SETUP && !isLimitLocked) -> Screen.PermissionsChecklist.route
                            else -> Screen.Dashboard.route
                        }

                        LaunchedEffect(user, isLimitLocked) {
                            if (user != null && (user?.status != UserStatus.PENDING_LIMIT_SETUP || isLimitLocked)) {
                                try {
                                    AppMonitorBlockerService.start(applicationContext)
                                } catch (_: Exception) {}
                            }
                        }

                        LaunchedEffect(pendingLimitReachedRoute) {
                            if (pendingLimitReachedRoute) {
                                hostNavController.navigate(Screen.LimitReached.route) {
                                    launchSingleTop = true
                                }
                                pendingLimitReachedRoute = false
                            }
                        }

                        LaunchedEffect(pendingInviteCode) {
                            val code = pendingInviteCode
                            if (!code.isNullOrBlank() && user == null) {
                                hostNavController.navigate(Screen.AccountCreation.createRoute(code)) {
                                    launchSingleTop = true
                                }
                                pendingInviteCode = null
                            }
                        }

                        LimitCrossesNavGraph(
                            navController = hostNavController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == AppMonitorBlockerService.ACTION_LIMIT_REACHED) {
            pendingLimitReachedRoute = true
            navController?.navigate(Screen.LimitReached.route) {
                launchSingleTop = true
            }
        }
        val deepLinkCode = intent.data?.getQueryParameter("code")
            ?: intent.getStringExtra("invite_code")
        if (!deepLinkCode.isNullOrBlank()) {
            pendingInviteCode = deepLinkCode
            navController?.navigate(Screen.AccountCreation.createRoute(deepLinkCode)) {
                launchSingleTop = true
            }
        }
    }
}
