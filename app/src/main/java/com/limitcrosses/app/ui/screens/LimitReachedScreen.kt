package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.limitcrosses.app.R
import com.limitcrosses.app.core.model.AppUsageInfo
import com.limitcrosses.app.platform.service.AppBlockerOverlayManager
import com.limitcrosses.app.ui.components.PankajYadavFooter
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.AuthViewModel
import com.limitcrosses.app.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitReachedScreen(
    dashboardViewModel: DashboardViewModel,
    authViewModel: AuthViewModel,
    onNavigateToPuzzles: () -> Unit,
    onNavigateToCosmos: () -> Unit,
    onNavigateToUsage: () -> Unit = {},
    onBackToDashboard: () -> Unit = {},
    onLoggedOut: () -> Unit = {}
) {
    val usageReport by dashboardViewModel.usageState.collectAsState()
    val resetTimerStr by dashboardViewModel.resetTimeFormatted.collectAsState()
    val activeBonusMinutes by dashboardViewModel.activeBonusMinutes.collectAsState()
    val pendingRequestMinutes by dashboardViewModel.pendingRequestMinutes.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    val limitMin = usageReport?.dailyLimitMinutes ?: 30
    val totalUsedSeconds = usageReport?.totalUsedSeconds ?: 0L
    val totalUsedMin = (totalUsedSeconds / 60L).toInt()
    val remainingSeconds = usageReport?.remainingSeconds ?: 0L
    val remainingMin = if (remainingSeconds > 0L) ((remainingSeconds + 59L) / 60L).toInt() else 0
    val isLimitCurrentlyReached = usageReport?.isLimitReached ?: true
    val isExhausted = isLimitCurrentlyReached || remainingSeconds <= 0L
    // Only show "Extended" if the user exhausted their base limit and is genuinely in the admin-granted bonus window
    val baseLimitMin = (limitMin - activeBonusMinutes).coerceAtLeast(0)
    val hasActiveAdminExtension = !isExhausted && activeBonusMinutes > 0 && totalUsedSeconds >= (baseLimitMin * 60L)
    val isWithinRegularAllowance = !isExhausted && !hasActiveAdminExtension

    // Continuous responsive polling while this screen is active so user sees granted time immediately
    LaunchedEffect(Unit) {
        while (isActive) {
            dashboardViewModel.manualRefresh()
            delay(3000L)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dashboardViewModel.manualRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(!isExhausted) {
        if (!isExhausted) {
            try {
                AppBlockerOverlayManager.hideOverlay()
            } catch (_: Exception) {}
        }
    }

    val defaultMonitoredApps = listOf(
        AppUsageInfo("com.facebook.katana", "Facebook", 0L, 0L, "facebook.com"),
        AppUsageInfo("com.instagram.android", "Instagram", 0L, 0L, "instagram.com"),
        AppUsageInfo("com.snapchat.android", "Snapchat", 0L, 0L, "snapchat.com")
    )
    val displayApps = if (!usageReport?.apps.isNullOrEmpty()) usageReport!!.apps else defaultMonitoredApps

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            hasActiveAdminExtension -> "Time Extended"
                            isWithinRegularAllowance -> "Screen Time Active"
                            else -> "Limit Enforced"
                        },
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return to Dashboard",
                            tint = CosmicTextPrimary
                        )
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Glowing Icon
            Surface(
                modifier = Modifier.size(86.dp),
                shape = RoundedCornerShape(26.dp),
                color = when {
                    hasActiveAdminExtension -> CosmicSuccess.copy(alpha = 0.15f)
                    isWithinRegularAllowance -> CosmicPrimary.copy(alpha = 0.15f)
                    else -> CosmicDanger.copy(alpha = 0.15f)
                },
                border = BorderStroke(2.dp, when {
                    hasActiveAdminExtension -> CosmicSuccess
                    isWithinRegularAllowance -> CosmicPrimary
                    else -> CosmicDanger
                })
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            hasActiveAdminExtension -> Icons.Default.LockOpen
                            isWithinRegularAllowance -> Icons.Default.Timer
                            else -> Icons.Default.HourglassBottom
                        },
                        contentDescription = null,
                        tint = when {
                            hasActiveAdminExtension -> CosmicSuccess
                            isWithinRegularAllowance -> CosmicPrimary
                            else -> CosmicDanger
                        },
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    hasActiveAdminExtension -> "Daily Limit Extended!"
                    isWithinRegularAllowance -> "Daily Allowance Active"
                    else -> stringResource(R.string.limit_reached_title)
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (!isExhausted) "Remaining Time: $remainingMin Minutes" else stringResource(R.string.limit_reached_remaining_zero),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    hasActiveAdminExtension -> CosmicSuccess
                    isWithinRegularAllowance -> CosmicPrimary
                    else -> CosmicDanger
                },
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when {
                    hasActiveAdminExtension -> "Your Administrator has approved +$activeBonusMinutes min extra allowance today. You can now use Facebook, Instagram, and Snapchat."
                    isWithinRegularAllowance -> "You have $remainingMin minutes of daily screen time remaining today for Facebook, Instagram, and Snapchat."
                    else -> stringResource(R.string.limit_reached_desc)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Reset time info badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CosmicSurface,
                border = BorderStroke(1.dp, CosmicSecondary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = CosmicSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (resetTimerStr.isNotBlank()) "Resets $resetTimerStr (12:00 AM midnight)" else stringResource(R.string.limit_reached_reset_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // TODAY'S SOCIAL MEDIA APP USAGE BREAKDOWN
            // (Facebook, Instagram, Snapchat)
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = CosmicSurface,
                border = BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's App Usage",
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Combined Social Cap: $limitMin min",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicTextMuted
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CosmicDanger.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CosmicDanger.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "$totalUsedMin / $limitMin min",
                                fontWeight = FontWeight.ExtraBold,
                                color = CosmicDanger,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    displayApps.forEach { appInfo ->
                        LimitReachedAppCard(appInfo = appInfo, limitMin = limitMin)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Administrator Granted Time Banner (Requirement 10)
            if (activeBonusMinutes > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = CosmicSuccess.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CosmicSuccess)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = CosmicSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "+$activeBonusMinutes min granted by your Administrator",
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Extra time was added to your daily allowance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicTextSecondary
                            )
                        }
                    }
                }
            }

            // Pending Extension Banner (Requirement 1)
            if (pendingRequestMinutes != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = CosmicWarning.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CosmicWarning)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = CosmicWarning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Extension Request Pending: +$pendingRequestMinutes min",
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Your request was submitted and is waiting for Administrator review.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicTextSecondary
                            )
                        }
                    }
                }
            }

            // Action 1: Resume Using Apps (if allowance or extension available) OR Solve Puzzles to Request Time
            if (!isExhausted) {
                Button(
                    onClick = onBackToDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasActiveAdminExtension) CosmicSuccess else CosmicPrimary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resume Using Social Apps ($remainingMin min)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = onNavigateToPuzzles,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.limit_reached_btn_request_time),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action 2: Explore Cosmos While Waiting
            OutlinedButton(
                onClick = onNavigateToCosmos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary),
                border = BorderStroke(1.5.dp, CosmicSecondary)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = CosmicSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.limit_reached_btn_learn_cosmos),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = CosmicSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action 3: View Full 7-Day Usage Details
            OutlinedButton(
                onClick = onNavigateToUsage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CosmicCardBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextSecondary)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "View 7-Day Usage History",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action 4: Return to Dashboard
            TextButton(
                onClick = onBackToDashboard,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp), tint = CosmicTextSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Return to Dashboard", color = CosmicTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 5: Sign Out / Log Out Button
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CosmicDanger.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicDanger)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_logout),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CosmicDanger
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Best effort notice
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CosmicSurface,
                border = BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Text(
                    text = stringResource(R.string.limit_reached_best_effort_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PankajYadavFooter()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Sign Out Confirmation Dialog
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
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = CosmicTextSecondary
                    )
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun LimitReachedAppCard(appInfo: AppUsageInfo, limitMin: Int) {
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
        shape = RoundedCornerShape(12.dp),
        color = CosmicCard,
        border = BorderStroke(1.dp, CosmicCardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brandColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
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
                            fontSize = 14.sp
                        )
                        Text(
                            text = appInfo.targetDomain,
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CosmicSurface
                ) {
                    Text(
                        text = if (appInfo.usageSeconds in 1..59) "< 1 min" else "$usedMin min",
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App progress bar
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
