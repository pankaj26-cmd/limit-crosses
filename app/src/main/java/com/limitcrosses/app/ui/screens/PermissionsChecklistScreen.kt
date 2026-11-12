package com.limitcrosses.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.PermissionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsChecklistScreen(
    viewModel: PermissionsViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val permState by viewModel.permState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

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
        // If approved, start VPN service
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
                title = { Text(stringResource(R.string.perm_title), color = CosmicTextPrimary) },
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

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Usage Stats Permission (MANDATORY)
            PermissionCard(
                title = stringResource(R.string.perm_usage_title),
                description = stringResource(R.string.perm_usage_desc),
                icon = Icons.Default.QueryStats,
                isGranted = permState.hasUsageStats,
                buttonText = stringResource(R.string.perm_usage_btn),
                onAction = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            // 3. VPN Local Inspection (Optional / Best Effort)
            PermissionCard(
                title = stringResource(R.string.perm_vpn_title),
                description = stringResource(R.string.perm_vpn_desc),
                icon = Icons.Default.VpnLock,
                isGranted = permState.hasVpn,
                buttonText = stringResource(R.string.perm_vpn_btn),
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

            Spacer(modifier = Modifier.height(28.dp))

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
                    color = CosmicTextPrimary
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
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    buttonText: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CosmicCard,
        border = androidx.compose.foundation.BorderStroke(
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicSecondary)
                ) {
                    Text(text = buttonText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

