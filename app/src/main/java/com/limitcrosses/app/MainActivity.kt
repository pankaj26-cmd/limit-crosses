package com.limitcrosses.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.limitcrosses.app.core.model.UserRole
import com.limitcrosses.app.core.model.UserStatus
import com.limitcrosses.app.domain.repository.AuthRepository
import com.limitcrosses.app.ui.navigation.LimitCrossesNavGraph
import com.limitcrosses.app.ui.navigation.Screen
import com.limitcrosses.app.ui.theme.CosmicBackground
import com.limitcrosses.app.ui.theme.LimitCrossesTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LimitCrossesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CosmicBackground
                ) {
                    val user by authRepository.getCurrentUserFlow().collectAsState(initial = null)
                    val navController = rememberNavController()

                    val startDestination = when {
                        user == null -> Screen.Welcome.route
                        user?.role == UserRole.ADMIN -> Screen.AdminDashboard.route
                        user?.status == UserStatus.PENDING_LIMIT_SETUP -> Screen.PermissionsChecklist.route
                        else -> Screen.Dashboard.route
                    }

                    LimitCrossesNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}

