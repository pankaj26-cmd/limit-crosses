package com.limitcrosses.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.limitcrosses.app.R
import com.limitcrosses.app.core.model.*
import com.limitcrosses.app.ui.components.PankajYadavFooter
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.AdminViewModel
import com.limitcrosses.app.ui.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    authViewModel: AuthViewModel? = null,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh when entering and on resume
    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var inviteLimitMinutes by remember { mutableStateOf("30") }
    var inviteDays by remember { mutableStateOf("14") }
    var inviteNotes by remember { mutableStateOf("") }

    var showModifyLimitDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importCodeInput by remember { mutableStateOf("") }
    var targetUserToModify by remember { mutableStateOf<UserAccount?>(null) }
    var newLimitInput by remember { mutableStateOf("30") }
    var auditReasonInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_title), color = CosmicTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = CosmicTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = CosmicTextPrimary)
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.action_logout), tint = CosmicDanger)
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
        ) {
            // Tamper / Disconnected / Uninstalled Inactivity Alerts Banner (Requirement 12)
            if (uiState.userInactiveAlerts.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    uiState.userInactiveAlerts.forEach { alertMsg ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = CosmicDanger.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, CosmicDanger)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CosmicDanger, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = alertMsg, style = MaterialTheme.typography.bodySmall, color = CosmicTextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Tabs: Invites, Users & Usage, Requests & Deletions, Audit Log
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = CosmicSurface,
                contentColor = CosmicSecondary
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    selectedContentColor = CosmicPrimary,
                    unselectedContentColor = CosmicTextSecondary,
                    text = { Text(stringResource(R.string.admin_tab_invites), fontSize = 12.sp) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    selectedContentColor = CosmicPrimary,
                    unselectedContentColor = CosmicTextSecondary,
                    text = { Text("Users & Usage", fontSize = 12.sp) }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    selectedContentColor = CosmicPrimary,
                    unselectedContentColor = CosmicTextSecondary,
                    text = {
                        val totalReqs = uiState.pendingExtensions.size + uiState.pendingDeletions.size
                        Text(if (totalReqs > 0) "Requests ($totalReqs)" else "Requests", fontSize = 12.sp)
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    selectedContentColor = CosmicPrimary,
                    unselectedContentColor = CosmicTextSecondary,
                    text = { Text(stringResource(R.string.admin_tab_audit), fontSize = 12.sp) }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (uiState.selectedTab) {
                        0 -> {
                            // --- TAB 0: INVITES ---
                            Text(text = stringResource(R.string.admin_generate_invite_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = CosmicCard,
                                border = BorderStroke(1.dp, CosmicCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    OutlinedTextField(
                                        value = inviteLimitMinutes,
                                        onValueChange = { inviteLimitMinutes = it.filter { c -> c.isDigit() } },
                                        label = { Text(stringResource(R.string.admin_invite_default_limit)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = inviteDays,
                                        onValueChange = { inviteDays = it.filter { c -> c.isDigit() } },
                                        label = { Text(stringResource(R.string.admin_invite_expiry_days)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = inviteNotes,
                                        onValueChange = { inviteNotes = it },
                                        label = { Text("Designated Recipient / Notes") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            val limit = inviteLimitMinutes.toIntOrNull() ?: 30
                                            val days = inviteDays.toIntOrNull() ?: 14
                                            viewModel.createInvite(limit, days, inviteNotes)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.admin_btn_create_invite), fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            // Generated Code Banner
                            uiState.generatedCode?.let { code ->
                                var isCopied by remember(code) { mutableStateOf(false) }
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = CosmicCard,
                                    border = BorderStroke(2.dp, CosmicSecondary)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "SHAREABLE INVITATION CODE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CosmicSecondary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = code, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = CosmicTextPrimary, letterSpacing = 2.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(code))
                                                    isCopied = true
                                                    Toast.makeText(context, "Copied invite code: $code", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isCopied) CosmicSuccess else CosmicSecondary,
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color.Black
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isCopied) "Code Copied!" else "Copy Invite Code",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active & Past Invites",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicTextPrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.refreshAll()
                                            Toast.makeText(context, "Checking cloud relay for user registrations...", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                        border = BorderStroke(1.dp, CosmicSecondary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sync Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showImportDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                        border = BorderStroke(1.dp, CosmicCardBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextSecondary)
                                    ) {
                                        Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Import Code", fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            uiState.activeInvites.forEach { invite ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(invite.code))
                                            Toast.makeText(context, "Copied: ${invite.code}", Toast.LENGTH_SHORT).show()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = CosmicSurface,
                                    border = BorderStroke(1.dp, if (invite.isRedeemed) CosmicPrimary.copy(alpha = 0.5f) else CosmicCardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = invite.code, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CosmicTextPrimary)
                                                Text(text = "Cap: ${invite.assignedDefaultLimitMinutes}m | ${if (invite.notes.isNotEmpty()) invite.notes else "No notes"}", style = MaterialTheme.typography.labelSmall, color = CosmicTextSecondary)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (invite.isRedeemed) CosmicPrimary.copy(alpha = 0.2f) else CosmicSuccess.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = if (invite.isRedeemed) "Redeemed" else "Available",
                                                    color = if (invite.isRedeemed) CosmicSecondary else CosmicSuccess,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        // Registered User Information
                                        if (invite.isRedeemed) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = CosmicCard,
                                                border = BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.35f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = CosmicSecondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "Registered User: ${invite.redeemedByUserName ?: "Account Linked"}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = CosmicTextPrimary
                                                        )
                                                        if (!invite.redeemedByUserEmail.isNullOrBlank()) {
                                                            Text(
                                                                text = invite.redeemedByUserEmail,
                                                                fontSize = 11.sp,
                                                                color = CosmicTextSecondary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // --- TAB 1: MANAGED USERS, LIMITS & ALL APPS USAGE (Requirements 10, 11) ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "User Accounts, Allocations & App Usage",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicTextPrimary
                                )
                                OutlinedButton(
                                    onClick = {
                                        viewModel.refreshAll()
                                        Toast.makeText(context, "Syncing users...", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp),
                                    border = BorderStroke(1.dp, CosmicSecondary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.managedUsers.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CosmicSurface,
                                    border = BorderStroke(1.dp, CosmicCardBorder),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "No users registered yet.", color = CosmicTextMuted, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "When a user joins with an invite code on another device, tap 'Sync Cloud' or 'Sync' to update.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CosmicTextSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                uiState.managedUsers.forEach { user ->
                                    val config = uiState.userLimits[user.id]
                                    val baseLimit = config?.dailyLimitMinutes ?: 30
                                    val extraGranted = uiState.userExtraMinutesGranted[user.id] ?: 0
                                    val totalCap = baseLimit + extraGranted
                                    val report = uiState.userUsageReports[user.id]

                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = CosmicSurface,
                                        border = BorderStroke(1.dp, CosmicCardBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            // Header: Name & Status
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = user.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CosmicTextPrimary)
                                                    Text(text = user.email, fontSize = 12.sp, color = CosmicTextSecondary)
                                                    if (!user.registeredViaInviteCode.isNullOrBlank()) {
                                                        Text(
                                                            text = "Joined via: ${user.registeredViaInviteCode}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = CosmicSecondary
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = CosmicSecondary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = user.status.name,
                                                        color = CosmicSecondary,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = CosmicCardBorder.copy(alpha = 0.6f))
                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Time Allocation (Base + Admin Grants)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(text = "Base Limit", style = MaterialTheme.typography.labelSmall, color = CosmicTextMuted)
                                                    Text(text = "${baseLimit}m", fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                                                }
                                                Column {
                                                    Text(text = "Extra Given by Admin", style = MaterialTheme.typography.labelSmall, color = CosmicSecondary)
                                                    Text(text = "+${extraGranted}m", fontWeight = FontWeight.Bold, color = CosmicSecondary)
                                                }
                                                Column {
                                                    Text(text = "Total Cap Today", style = MaterialTheme.typography.labelSmall, color = CosmicTextPrimary)
                                                    Text(text = "${totalCap}m", fontWeight = FontWeight.Bold, color = CosmicSuccess)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Detailed Social Media Usage Breakdown on All Apps (Requirement 11)
                                            Text(
                                                text = "Active Social App Usage Breakdown:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = CosmicTextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            val apps = report?.apps ?: emptyList()
                                            val fbSec = apps.find { it.packageName.contains("facebook") }?.usageSeconds ?: 0L
                                            val igSec = apps.find { it.packageName.contains("instagram") }?.usageSeconds ?: 0L
                                            val scSec = apps.find { it.packageName.contains("snapchat") }?.usageSeconds ?: 0L
                                            val totalUsedSec = fbSec + igSec + scSec
                                            val totalUsedMin = (totalUsedSec / 60).toInt()

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Facebook Card
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = CosmicCard
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Facebook", fontSize = 11.sp, color = BrandFacebook, fontWeight = FontWeight.Bold)
                                                        Text("${fbSec / 60}m", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                                                    }
                                                }
                                                // Instagram Card
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = CosmicCard
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Instagram", fontSize = 11.sp, color = BrandInstagram, fontWeight = FontWeight.Bold)
                                                        Text("${igSec / 60}m", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                                                    }
                                                }
                                                // Snapchat Card
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = CosmicCard
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Snapchat", fontSize = 11.sp, color = BrandSnapchat, fontWeight = FontWeight.Bold)
                                                        Text("${scSec / 60}m", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            // Progress Bar
                                            val progress = if (totalCap > 0) (totalUsedMin.toFloat() / totalCap.toFloat()).coerceIn(0f, 1f) else 0f
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                color = if (progress >= 1f) CosmicDanger else CosmicPrimary,
                                                trackColor = CosmicCardBorder
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Combined: ${totalUsedMin}m used of ${totalCap}m total (${totalCap - totalUsedMin}m remaining)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CosmicTextSecondary
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))

                                            OutlinedButton(
                                                onClick = {
                                                    targetUserToModify = user
                                                    newLimitInput = baseLimit.toString()
                                                    auditReasonInput = ""
                                                    showModifyLimitDialog = true
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary),
                                                border = BorderStroke(1.dp, CosmicSecondary),
                                                modifier = Modifier.fillMaxWidth().height(38.dp)
                                            ) {
                                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Modify Daily Base Cap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // --- TAB 2: REQUESTS & DELETIONS (Requirements 1, 2, 3, 4) ---
                            // Section 1: Pending Extension Requests
                            Text(text = "Pending Extension Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                            Spacer(modifier = Modifier.height(10.dp))

                            if (uiState.pendingExtensions.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = CosmicSurface
                                ) {
                                    Text(
                                        text = "No pending extension requests at this time.",
                                        color = CosmicTextMuted,
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                uiState.pendingExtensions.forEach { req ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = CosmicCard,
                                        border = BorderStroke(1.dp, CosmicWarning)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = req.userName, fontWeight = FontWeight.Bold, color = CosmicTextPrimary, fontSize = 15.sp)
                                                Surface(shape = RoundedCornerShape(6.dp), color = CosmicWarning.copy(alpha = 0.2f)) {
                                                    Text(text = "+${req.requestedMinutes} min requested", color = CosmicWarning, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "Reason: ${req.reason}", style = MaterialTheme.typography.bodyMedium, color = CosmicTextSecondary)

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Text(text = "Admin Quick Grant (up to 20m):", style = MaterialTheme.typography.labelSmall, color = CosmicSecondary, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(5, 10, 15, 20).forEach { mins ->
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.reviewExtension(req.id, ExtensionStatus.MODIFIED, mins, "Admin granted +${mins}m")
                                                        },
                                                        modifier = Modifier.weight(1f).height(34.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, CosmicSecondary),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text("+$mins m", fontSize = 11.sp, color = CosmicSecondary, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val granted = req.requestedMinutes.coerceIn(1, 45)
                                                        viewModel.reviewExtension(req.id, ExtensionStatus.APPROVED, granted, "Approved by Admin")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSuccess),
                                                    modifier = Modifier.weight(1f).height(40.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Approve (+${req.requestedMinutes}m)", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        viewModel.reviewExtension(req.id, ExtensionStatus.DENIED, 0, "Denied by Admin")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicDanger),
                                                    modifier = Modifier.weight(1f).height(40.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(stringResource(R.string.admin_btn_deny), color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Section 2: Account Deletion Requests (Requirement 3: Admin Approval Required)
                            Text(text = "Account Deletion Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                            Spacer(modifier = Modifier.height(10.dp))

                            if (uiState.pendingDeletions.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = CosmicSurface
                                ) {
                                    Text(
                                        text = "No pending account deletion requests.",
                                        color = CosmicTextMuted,
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else {
                                uiState.pendingDeletions.forEach { delReq ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = CosmicCard,
                                        border = BorderStroke(1.dp, CosmicDanger)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = delReq.userName, fontWeight = FontWeight.Bold, color = CosmicTextPrimary, fontSize = 15.sp)
                                                Surface(shape = RoundedCornerShape(4.dp), color = CosmicDanger.copy(alpha = 0.2f)) {
                                                    Text("Deletion Requested", color = CosmicDanger, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = delReq.userEmail, style = MaterialTheme.typography.bodySmall, color = CosmicTextSecondary)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "User Justification: \"${delReq.reason}\"", style = MaterialTheme.typography.bodyMedium, color = CosmicTextPrimary)

                                            Spacer(modifier = Modifier.height(14.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        viewModel.reviewDeletionRequest(delReq.id, approved = true, notes = "Approved by Admin")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicDanger),
                                                    modifier = Modifier.weight(1f).height(40.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Approve & Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.reviewDeletionRequest(delReq.id, approved = false, notes = "Rejected by Admin")
                                                    },
                                                    border = BorderStroke(1.dp, CosmicSecondary),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary),
                                                    modifier = Modifier.weight(1f).height(40.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Reject Request", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Section 3: All Extension Requests History (Requirement 1 & 2)
                            Text(text = "All Extension History (Past & Decided)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                            Spacer(modifier = Modifier.height(10.dp))

                            val nonPending = uiState.allExtensions.filter { it.status != ExtensionStatus.PENDING }
                            if (nonPending.isEmpty()) {
                                Text(text = "No past decided requests.", color = CosmicTextMuted, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                nonPending.forEach { req ->
                                    val statusColor = when (req.status) {
                                        ExtensionStatus.APPROVED -> CosmicSuccess
                                        ExtensionStatus.DENIED -> CosmicDanger
                                        ExtensionStatus.MODIFIED -> CosmicSecondary
                                        else -> CosmicWarning
                                    }
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = CosmicSurface,
                                        border = BorderStroke(1.dp, CosmicCardBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(text = "${req.userName} (+${req.requestedMinutes}m)", fontWeight = FontWeight.Bold, color = CosmicTextPrimary, fontSize = 14.sp)
                                                Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(alpha = 0.2f)) {
                                                    Text(text = req.status.name, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "Reason: ${req.reason}", style = MaterialTheme.typography.bodySmall, color = CosmicTextSecondary)
                                            if (req.grantedMinutes > 0) {
                                                Text(text = "Granted: +${req.grantedMinutes} min", style = MaterialTheme.typography.labelSmall, color = CosmicSuccess, fontWeight = FontWeight.Bold)
                                            }
                                            Text(text = sdf.format(Date(req.requestedAtEpochMs)), style = MaterialTheme.typography.labelSmall, color = CosmicTextMuted)
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // --- TAB 3: AUDIT LOG ---
                            Text(text = stringResource(R.string.admin_audit_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                            Spacer(modifier = Modifier.height(12.dp))

                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                            uiState.auditLogs.forEach { log ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = CosmicSurface,
                                    border = BorderStroke(1.dp, CosmicCardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = log.actionType.name, fontWeight = FontWeight.Bold, color = CosmicSecondary, fontSize = 13.sp)
                                            Text(text = sdf.format(Date(log.timestampEpochMs)), style = MaterialTheme.typography.labelSmall, color = CosmicTextMuted)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = log.description, style = MaterialTheme.typography.bodyMedium, color = CosmicTextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    PankajYadavFooter(detailed = true)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Modify User Limit Dialog (Strict Positive Number & Max Cap Constraints - Requirement 8)
    if (showModifyLimitDialog && targetUserToModify != null) {
        val limitNum = newLimitInput.toIntOrNull()
        val isValidPositiveLimit = limitNum != null && limitNum in 1..45

        AlertDialog(
            onDismissRequest = { showModifyLimitDialog = false },
            title = { Text(stringResource(R.string.admin_dialog_modify_limit_title), fontWeight = FontWeight.Bold, color = CosmicTextPrimary) },
            text = {
                Column {
                    Text(text = "Target: ${targetUserToModify?.fullName} (${targetUserToModify?.email})", color = CosmicTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newLimitInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            newLimitInput = digits
                        },
                        label = { Text("New Daily Cap (1-45 min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = !isValidPositiveLimit && newLimitInput.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (!isValidPositiveLimit && newLimitInput.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Daily limit must be a positive integer between 1 and 45 minutes.",
                            color = CosmicDanger,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = auditReasonInput,
                        onValueChange = { auditReasonInput = it },
                        label = { Text(stringResource(R.string.admin_dialog_modify_reason)) },
                        placeholder = { Text("Justification for limit change") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minutes = limitNum ?: 30
                        viewModel.modifyUserLimit(targetUserToModify!!.id, minutes, auditReasonInput)
                        showModifyLimitDialog = false
                    },
                    enabled = isValidPositiveLimit && auditReasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text(stringResource(R.string.action_confirm), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showModifyLimitDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(16.dp)
        )
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
                        viewModel.logout {
                            onLoggedOut()
                        }
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

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text("Import User Registration", color = CosmicTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Paste the offline sync code from the user device (starts with LCR-) to manually link the user:",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importCodeInput,
                        onValueChange = { importCodeInput = it },
                        placeholder = { Text("Paste LCR-... code here") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importOfflineRegistration(importCodeInput) { success, _ ->
                            if (success) {
                                showImportDialog = false
                                importCodeInput = ""
                            }
                        }
                    },
                    enabled = importCodeInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text("Import & Link User", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
