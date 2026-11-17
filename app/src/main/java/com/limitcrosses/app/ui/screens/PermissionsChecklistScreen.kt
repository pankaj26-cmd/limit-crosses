package com.limitcrosses.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.limitcrosses.app.R
import com.limitcrosses.app.platform.vpn.LimitCrossesVpnService
import com.limitcrosses.app.ui.components.DeviceSetupGuideDialog
import com.limitcrosses.app.ui.components.PankajYadavFooter
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.AuthViewModel
import com.limitcrosses.app.ui.viewmodel.PermissionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsChecklistScreen(
    viewModel: PermissionsViewModel,
    authViewModel: AuthViewModel? = null,
    onContinue: () -> Unit,
    onLoggedOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val permState by viewModel.permState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var showGuideDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Refresh permissions whenever returning to this screen from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkPermissions()
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkPermissions()
        if (LimitCrossesVpnService.isPrepared(context)) {
            val intent = Intent(context, LimitCrossesVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.perm_title), color = CosmicTextPrimary, fontWeight = FontWeight.Bold) },
                actions = {
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
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.perm_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // RESTRICTED SETTINGS UNLOCKER CARD (Android 13+ / MIUI / ColorOS / OneUI)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CosmicSurface,
                border = BorderStroke(1.dp, CosmicWarning.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CosmicWarning, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.perm_restricted_settings_title),
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.perm_restricted_settings_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                openAppDetailsSettings(context)
                                showGuideDialog = true
                            },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicWarning.copy(alpha = 0.85f),
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.perm_restricted_settings_btn),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showGuideDialog = true },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CosmicCardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextSecondary)
                        ) {
                            Text("Guide", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Usage Stats Permission (MANDATORY)
            PermissionCard(
                title = stringResource(R.string.perm_usage_title),
                description = stringResource(R.string.perm_usage_desc),
                icon = Icons.Default.QueryStats,
                isGranted = permState.hasUsageStats,
                buttonText = stringResource(R.string.perm_usage_btn),
                onAction = {
                    openUsageSettings(context)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Notification Permission (Android 13+)
            PermissionCard(
                title = stringResource(R.string.perm_notifications_title),
                description = stringResource(R.string.perm_notifications_desc),
                icon = Icons.Default.Notifications,
                isGranted = permState.hasNotifications,
                buttonText = stringResource(R.string.perm_notifications_btn),
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.checkPermissions()
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Display Over Other Apps (Overlay Blocker)
            PermissionCard(
                title = "Display Over Other Apps (Overlay Lock)",
                description = "Draws an impassable full-screen lock card directly over Instagram and Snapchat when remaining allowance hits 0 min.",
                icon = Icons.Default.Layers,
                isGranted = permState.hasOverlay,
                buttonText = "Allow Overlay",
                isOptional = true,
                onAction = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. VPN Local Inspection (Optional / Best Effort)
            PermissionCard(
                title = stringResource(R.string.perm_vpn_title),
                description = stringResource(R.string.perm_vpn_desc),
                icon = Icons.Default.VpnLock,
                isGranted = permState.hasVpn,
                buttonText = stringResource(R.string.perm_vpn_btn),
                isOptional = true,
                onAction = {
                    val vpnIntent = VpnService.prepare(context)
                    if (vpnIntent != null) {
                        vpnLauncher.launch(vpnIntent)
                    } else {
                        val intent = Intent(context, LimitCrossesVpnService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        viewModel.checkPermissions()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                enabled = viewModel.canProceedToLimit(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
            ) {
                Text(
                    text = stringResource(R.string.perm_btn_continue_to_limit),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            // Fallback for Local Demo/Testing Mode so developer/tester is never trapped
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CosmicCardBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextSecondary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.perm_bypass_demo_btn),
                    fontSize = 13.sp,
                    color = CosmicTextSecondary
                )
            }

            if (!viewModel.canProceedToLimit()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.dash_warning_tracking_inactive),
                    color = CosmicWarning,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CosmicDanger.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicDanger)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_logout),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicDanger
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            PankajYadavFooter()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.dialog_sign_out_title), fontWeight = FontWeight.Bold, color = CosmicTextPrimary) },
            text = { Text(stringResource(R.string.dialog_sign_out_message), color = CosmicTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel?.logout { onLoggedOut() } ?: onLoggedOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicDanger)
                ) {
                    Text(stringResource(R.string.action_logout), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showGuideDialog) {
        DeviceSetupGuideDialog(
            onDismiss = { showGuideDialog = false },
            onOpenAppInfo = {
                showGuideDialog = false
                openAppDetailsSettings(context)
            },
            onOpenUsageSettings = {
                showGuideDialog = false
                openUsageSettings(context)
            }
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    buttonText: String,
    isOptional: Boolean = false,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CosmicCard,
        border = BorderStroke(
            1.dp,
            if (isGranted) CosmicSuccess.copy(alpha = 0.4f) else CosmicCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isGranted) CosmicSuccess.copy(alpha = 0.15f) else CosmicPrimary.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) CosmicSuccess else CosmicSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isGranted) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CosmicSuccess, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.perm_granted),
                                color = CosmicSuccess,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (isOptional) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CosmicSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.perm_optional_inactive),
                                color = CosmicSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = CosmicWarning, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.perm_missing),
                                color = CosmicWarning,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicTextSecondary,
                lineHeight = 18.sp
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary),
                    border = BorderStroke(1.dp, CosmicSecondary)
                ) {
                    Text(text = buttonText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
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
