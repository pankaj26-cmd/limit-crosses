package com.limitcrosses.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.core.model.AppUsageInfo
import com.limitcrosses.app.core.model.UserRole
import com.limitcrosses.app.ui.components.PankajYadavFooter
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
    onNavigateToLimitReached: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val usageReport by dashboardViewModel.usageState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val resetTimerStr by dashboardViewModel.resetTimeFormatted.collectAsState()
    val activeBonusMinutes by dashboardViewModel.activeBonusMinutes.collectAsState()
    val pendingRequestMinutes by dashboardViewModel.pendingRequestMinutes.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRestrictedSettingsGuide by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                dashboardViewModel.manualRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                            fontSize = 20.sp,
                            color = CosmicTextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CosmicPrimary.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = stringResource(R.string.best_effort_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { dashboardViewModel.manualRefresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            tint = CosmicTextPrimary
                        )
                    }
                    if (authState.currentUser?.role == UserRole.ADMIN) {
                        IconButton(onClick = onNavigateToAdmin) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = stringResource(R.string.nav_admin),
                                tint = CosmicSecondary
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                            tint = CosmicTextPrimary
                        )
                    }
                    // Prominent Sign Out Button in Top Bar
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.action_logout),
                            tint = CosmicDanger
                        )
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Logged in User Bar
            val user = authState.currentUser
            if (user != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CosmicSurface,
                    border = BorderStroke(1.dp, CosmicCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(CosmicPrimary, CosmicSecondary)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.fullName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = user.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicTextPrimary
                                )
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CosmicTextSecondary
                                )
                            }
                        }

                        // Logout Pill
                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, CosmicDanger.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicDanger)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_logout),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Administrator Granted Time Banner (Requirement 10)
            if (activeBonusMinutes > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = CosmicSuccess.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CosmicSuccess)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CosmicSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "+$activeBonusMinutes minutes granted by your Administrator",
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "This extra time has been added to your daily allowance by your Administrator.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicTextSecondary
                            )
                        }
                    }
                }
            }

            // Pending Extension Banner
            if (pendingRequestMinutes != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = CosmicWarning.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CosmicWarning)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = CosmicWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Extension Request Pending: +$pendingRequestMinutes min",
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Awaiting review by your Administrator. You will be credited once approved.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicTextSecondary
                            )
                        }
                    }
                }
            }
            if (usageReport?.trackingActive != true) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CosmicDanger.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, CosmicDanger.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = CosmicDanger.copy(alpha = 0.25f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = CosmicDanger,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tracking Inactive — Usage Access Needed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicTextPrimary
                                )
                                Text(
                                    text = "Android requires Usage Access to enforce daily social limits.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Explanatory note for Android 13+ restricted settings
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CosmicSurface.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⚠️ Is the toggle disabled or greyed out in Phone Settings?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = CosmicWarning
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "On Android 13+, sideloaded apps require one-time approval. Tap 'Unlock in App Info' below, tap the 3 dots (⋮) in the top right, and choose 'Allow restricted settings'.",
                                    fontSize = 11.sp,
                                    color = CosmicTextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    openUsageSettings(context)
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                            ) {
                                Text(text = "Open Usage Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    openAppDetailsSettings(context)
                                    showRestrictedSettingsGuide = true
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CosmicSecondary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary)
                            ) {
                                Text(text = "Unlock in App Info", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showRestrictedSettingsGuide = true }) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = CosmicSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Step-by-step unlock guide", fontSize = 11.sp, color = CosmicSecondary)
                            }
                            TextButton(onClick = onNavigateToPermissions) {
                                Text("All Permissions Checklist →", fontSize = 11.sp, color = CosmicTextSecondary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                // Tracking Active Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CosmicSuccess.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, CosmicSuccess.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
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

            // GORGEOUS MAIN REMAINING GAUGE CARD (Bug 3 Fix)
            val report = usageReport
            val remainingMin = if (report != null) (report.remainingSeconds / 60L).toInt() else 30
            val totalUsedMin = if (report != null) (report.totalUsedSeconds / 60L).toInt() else 0
            val limitMin = report?.dailyLimitMinutes ?: 30

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToUsage() },
                shape = RoundedCornerShape(24.dp),
                color = CosmicCard,
                border = BorderStroke(1.dp, CosmicCardBorder),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CosmicCard,
                                    CosmicSurface.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.dash_remaining_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CosmicTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Glowing Circular Remaining Minutes Indicator
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            val progressFraction = if (limitMin > 0) {
                                (totalUsedMin.toFloat() / limitMin.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            val animatedProgress by animateFloatAsState(
                                targetValue = progressFraction,
                                animationSpec = tween(durationMillis = 800),
                                label = "gaugeProgress"
                            )

                            // Track Ring
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 10.dp,
                                color = CosmicSurface,
                                trackColor = Color.Transparent
                            )

                            // Progress Ring
                            CircularProgressIndicator(
                                progress = { (1f - animatedProgress).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 10.dp,
                                color = if (remainingMin > 5) CosmicSecondary else CosmicDanger,
                                trackColor = Color.Transparent
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$remainingMin",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (remainingMin > 5) CosmicTextPrimary else CosmicDanger
                                )
                                Text(
                                    text = "MINUTES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = CosmicTextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CosmicSurface,
                                border = BorderStroke(1.dp, CosmicCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.dash_daily_limit_label)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmicTextMuted
                                    )
                                    Text(
                                        text = "$limitMin m",
                                        fontWeight = FontWeight.Bold,
                                        color = CosmicTextPrimary,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CosmicSurface,
                                border = BorderStroke(1.dp, CosmicCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.dash_total_used_label)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmicTextMuted
                                    )
                                    Text(
                                        text = "$totalUsedMin m",
                                        fontWeight = FontWeight.Bold,
                                        color = CosmicPrimary,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = CosmicTextMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.dash_reset_timer_prefix, resetTimerStr),
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicTextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Per-App Usage Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dash_per_app_breakdown),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
                Text(
                    text = "Combined 0–45m Cap",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val apps = report?.apps ?: emptyList()
            apps.forEach { appInfo ->
                AppUsageCard(appInfo = appInfo, limitMin = limitMin)
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Request Extra Time (Triggers 5-level deterministic puzzles)
            Button(
                onClick = onNavigateToPuzzles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Extension, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dash_btn_puzzle_extension),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Learn Cosmos (Independent Educational Section)
            OutlinedButton(
                onClick = onNavigateToCosmos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary),
                border = BorderStroke(1.dp, CosmicSecondary)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = CosmicSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.nav_cosmos),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = CosmicSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Diagnostic / Permissions Health Button
            OutlinedButton(
                onClick = onNavigateToPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextSecondary),
                border = BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = CosmicTextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permission Health & Settings",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = CosmicTextSecondary
                )
            }

            // CREATOR FOOTER: Pankaj Yadav (Bug 4 Fix)
            Spacer(modifier = Modifier.height(16.dp))
            PankajYadavFooter(detailed = true)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // SIGN OUT CONFIRMATION DIALOG (Bug 2 Fix)
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_sign_out_title),
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_sign_out_message),
                    color = CosmicTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout {
                            onLoggedOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicDanger)
                ) {
                    Text(
                        text = stringResource(R.string.action_logout),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel), color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // RESTRICTED SETTINGS GUIDE DIALOG (Bug 1 Fix)
    if (showRestrictedSettingsGuide) {
        AlertDialog(
            onDismissRequest = { showRestrictedSettingsGuide = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = CosmicSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock Restricted Settings",
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "On Android 13+ (Pixel, Samsung, Xiaomi, etc.), Android disables sensitive permissions for sideloaded APKs until unlocked:",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CosmicSurface,
                        border = BorderStroke(1.dp, CosmicCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "1. Tap 'Open App Info' below.", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CosmicTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "2. Tap the 3 dots (⋮) in the top-right corner.", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CosmicTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "3. Select 'Allow restricted settings' & confirm PIN.", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CosmicTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "4. Return to Limit Crosses & turn ON Usage Access.", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CosmicSuccess)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedSettingsGuide = false
                        openAppDetailsSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSecondary)
                ) {
                    Text(text = "Open App Info", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedSettingsGuide = false }) {
                    Text(text = "Close", color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun AppUsageCard(appInfo: AppUsageInfo, limitMin: Int) {
    val usedMin = (appInfo.usageSeconds / 60L).toInt()
    val brandColor = when (appInfo.packageName) {
        "com.facebook.katana" -> BrandFacebook
        "com.instagram.android" -> BrandInstagram
        "com.snapchat.android" -> BrandSnapchat
        else -> CosmicPrimary
    }

    val fraction = if (limitMin > 0) (usedMin.toFloat() / limitMin.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CosmicCard,
        border = BorderStroke(1.dp, CosmicCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brandColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(brandColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = appInfo.displayName,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = appInfo.targetDomain,
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CosmicSurface
                ) {
                    Text(
                        text = "$usedMin min",
                        fontWeight = FontWeight.ExtraBold,
                        color = CosmicTextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Individual micro progress bar
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = brandColor,
                trackColor = CosmicSurface
            )
        }
    }
}

private fun openUsageSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:" + context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallback)
    }
}

private fun openAppDetailsSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:" + context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallback)
    }
}
