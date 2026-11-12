package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.core.model.*
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var inviteLimitMinutes by remember { mutableStateOf("30") }
    var inviteDays by remember { mutableStateOf("14") }
    var inviteNotes by remember { mutableStateOf("") }

    var showModifyLimitDialog by remember { mutableStateOf(false) }
    var targetUserToModify by remember { mutableStateOf<UserAccount?>(null) }
    var newLimitInput by remember { mutableStateOf("30") }
    var auditReasonInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_title), color = CosmicTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = CosmicTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = CosmicTextPrimary)
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
            // Tabs: Invites, Users & Limits, Extensions, Audit Log
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = CosmicSurface,
                contentColor = CosmicSecondary
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text(stringResource(R.string.admin_tab_invites)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text(stringResource(R.string.admin_tab_users)) }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text(stringResource(R.string.admin_tab_extensions)) }
                )
                Tab(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    text = { Text(stringResource(R.string.admin_tab_audit)) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (uiState.selectedTab) {
                    0 -> {
                        // Generate Invites Tab
                        Text(
                            text = stringResource(R.string.admin_generate_invite_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = CosmicCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = inviteLimitMinutes,
                                    onValueChange = { inviteLimitMinutes = it.filter { c -> c.isDigit() } },
                                    label = { Text(stringResource(R.string.admin_invite_default_limit)) },
                                    placeholder = { Text("0 to 45 min") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CosmicPrimary,
                                        unfocusedBorderColor = CosmicCardBorder,
                                        focusedTextColor = CosmicTextPrimary,
                                        unfocusedTextColor = CosmicTextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = inviteDays,
                                    onValueChange = { inviteDays = it.filter { c -> c.isDigit() } },
                                    label = { Text(stringResource(R.string.admin_invite_expiry_days)) },
                                    placeholder = { Text("1 to 90 days") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CosmicPrimary,
                                        unfocusedBorderColor = CosmicCardBorder,
                                        focusedTextColor = CosmicTextPrimary,
                                        unfocusedTextColor = CosmicTextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = inviteNotes,
                                    onValueChange = { inviteNotes = it },
                                    label = { Text("Designated Recipient / Notes") },
                                    placeholder = { Text("e.g. For Alex's smartphone") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CosmicPrimary,
                                        unfocusedBorderColor = CosmicCardBorder,
                                        focusedTextColor = CosmicTextPrimary,
                                        unfocusedTextColor = CosmicTextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val lim = inviteLimitMinutes.toIntOrNull() ?: 30
                                        val d = inviteDays.toIntOrNull() ?: 14
                                        viewModel.createInvite(lim, d, inviteNotes)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = CosmicTextPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.admin_btn_create_invite), fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                                }
                            }
                        }

                        if (uiState.generatedCode != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = CosmicPrimary.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, CosmicSecondary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "SHAREABLE INVITATION CODE", style = MaterialTheme.typography.labelSmall, color = CosmicSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = uiState.generatedCode ?: "",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CosmicTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(text = "Active & Past Invites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        uiState.activeInvites.forEach { invite ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = CosmicSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = invite.code, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                                        Text(
                                            text = "Cap: ${invite.assignedDefaultLimitMinutes}m | ${if (invite.notes.isNotEmpty()) invite.notes else "No notes"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CosmicTextSecondary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (invite.isRedeemed) CosmicTextMuted.copy(alpha = 0.3f) else CosmicSuccess.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (invite.isRedeemed) "Redeemed" else "Available",
                                            color = if (invite.isRedeemed) CosmicTextMuted else CosmicSuccess,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    1 -> {
                        // Managed Users & Limits Tab
                        Text(text = stringResource(R.string.admin_user_list_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.managedUsers.isEmpty()) {
                            Text(text = "No users registered yet.", color = CosmicTextMuted)
                        } else {
                            uiState.managedUsers.forEach { user ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = CosmicSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = user.fullName, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = CosmicSecondary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = user.status.name,
                                                    color = CosmicSecondary,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = user.email, style = MaterialTheme.typography.bodyMedium, color = CosmicTextSecondary)

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedButton(
                                            onClick = {
                                                targetUserToModify = user
                                                newLimitInput = "30"
                                                auditReasonInput = ""
                                                showModifyLimitDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicSecondary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.admin_btn_change_limit), fontSize = 13.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }

                    2 -> {
                        // Pending Extension Requests Tab
                        Text(text = stringResource(R.string.admin_pending_extensions_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.pendingExtensions.isEmpty()) {
                            Text(text = "No pending extension requests.", color = CosmicTextMuted)
                        } else {
                            uiState.pendingExtensions.forEach { req ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = CosmicCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicWarning)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = "${req.userName} requests +${req.requestedMinutes} min", fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Reason: ${req.reason}", style = MaterialTheme.typography.bodyMedium, color = CosmicTextSecondary)

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.reviewExtension(req.id, ExtensionStatus.APPROVED, req.requestedMinutes, "Approved by Admin")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CosmicSuccess),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(stringResource(R.string.admin_btn_approve), color = CosmicBackground, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.reviewExtension(req.id, ExtensionStatus.DENIED, 0, "Denied by Admin")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CosmicDanger),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(stringResource(R.string.admin_btn_deny), color = CosmicTextPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }

                    3 -> {
                        // Audit Log Tab
                        Text(text = stringResource(R.string.admin_audit_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                        uiState.auditLogs.forEach { log ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = CosmicSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
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
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Modify User Limit Dialog (with required audit reason)
    if (showModifyLimitDialog && targetUserToModify != null) {
        AlertDialog(
            onDismissRequest = { showModifyLimitDialog = false },
            title = { Text(stringResource(R.string.admin_dialog_modify_limit_title)) },
            text = {
                Column {
                    Text(text = "Target: ${targetUserToModify?.fullName} (${targetUserToModify?.email})")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newLimitInput,
                        onValueChange = { newLimitInput = it.filter { c -> c.isDigit() } },
                        label = { Text("New Daily Cap (0-45 min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = auditReasonInput,
                        onValueChange = { auditReasonInput = it },
                        label = { Text(stringResource(R.string.admin_dialog_modify_reason)) },
                        placeholder = { Text("Justification for limit change") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val minutes = newLimitInput.toIntOrNull() ?: 30
                        viewModel.modifyUserLimit(targetUserToModify!!.id, minutes, auditReasonInput)
                        showModifyLimitDialog = false
                    },
                    enabled = auditReasonInput.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showModifyLimitDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

