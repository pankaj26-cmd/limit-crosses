package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.core.model.AppUsageInfo
import com.limitcrosses.app.core.model.UserRole
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.AuthViewModel
import com.limitcrosses.app.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    authViewModel: AuthViewModel,
    onNavigateToUsage: () -> Unit,
    onNavigateToCosmos: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToPuzzles: () -> Unit,
    onNavigateToLimitReached: () -> Unit
) {
    val usageReport by dashboardViewModel.usageState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val resetTimerStr by dashboardViewModel.resetTimeFormatted.collectAsState()

    // Trigger LimitReachedScreen if limit is exhausted
    LaunchedEffect(usageReport?.isLimitReached) {
        if (usageReport?.isLimitReached == true) {
            onNavigateToLimitReached()
        }
    }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CosmicPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = stringResource(R.string.best_effort_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { dashboardViewModel.manualRefresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = CosmicTextPrimary)
                    }
                    if (authState.currentUser?.role == UserRole.ADMIN) {
                        IconButton(onClick = onNavigateToAdmin) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = stringResource(R.string.nav_admin), tint = CosmicSecondary)
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings), tint = CosmicTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Reliability Warning if permissions disabled
            if (usageReport?.trackingActive != true) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CosmicDanger.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicDanger)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CosmicDanger)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.dash_warning_permissions_disabled),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicDanger
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CosmicSuccess.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CosmicSuccess)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.dash_tracking_active_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Main Remaining Gauge Card
            val report = usageReport
            val remainingMin = if (report != null) (report.remainingSeconds / 60L).toInt() else 30
            val totalUsedMin = if (report != null) (report.totalUsedSeconds / 60L).toInt() else 0
            val limitMin = report?.dailyLimitMinutes ?: 30

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToUsage() },
                shape = RoundedCornerShape(20.dp),
                color = CosmicCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.dash_remaining_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = CosmicTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$remainingMin",
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (remainingMin > 5) CosmicSecondary else CosmicDanger
                    )

                    Text(
                        text = "MINUTES REMAINING TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar
                    val progressFraction = if (limitMin > 0) {
                        (totalUsedMin.toFloat() / limitMin.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (remainingMin > 5) CosmicPrimary else CosmicDanger,
                        trackColor = CosmicSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stringResource(R.string.dash_total_used_label)}: $totalUsedMin m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary
                        )
                        Text(
                            text = "${stringResource(R.string.dash_daily_limit_label)}: $limitMin m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.dash_reset_timer_prefix, resetTimerStr),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Per-App Usage Breakdown
            Text(
                text = stringResource(R.string.dash_per_app_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            val apps = report?.apps ?: emptyList()
            apps.forEach { appInfo ->
                AppUsageRow(appInfo = appInfo)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Request Extra Time (Triggers 5-level deterministic puzzles)
            Button(
                onClick = onNavigateToPuzzles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
            ) {
                Icon(Icons.Default.Extension, contentDescription = null, tint = CosmicTextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dash_btn_puzzle_extension),
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Learn Cosmos
            OutlinedButton(
                onClick = onNavigateToCosmos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicSecondary)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = CosmicSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.nav_cosmos),
                    fontWeight = FontWeight.SemiBold,
                    color = CosmicSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AppUsageRow(appInfo: AppUsageInfo) {
    val usedMin = (appInfo.usageSeconds / 60L).toInt()
    val brandColor = when (appInfo.packageName) {
        "com.facebook.katana" -> BrandFacebook
        "com.instagram.android" -> BrandInstagram
        "com.snapchat.android" -> BrandSnapchat
        else -> CosmicPrimary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CosmicSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brandColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.displayName,
                    fontWeight = FontWeight.SemiBold,
                    color = CosmicTextPrimary
                )
                Text(
                    text = appInfo.targetDomain,
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextMuted
                )
            }
            Text(
                text = "$usedMin min",
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary,
                fontSize = 15.sp
            )
        }
    }
}

