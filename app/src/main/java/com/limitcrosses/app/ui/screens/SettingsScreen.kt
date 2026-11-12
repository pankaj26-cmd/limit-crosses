package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.AuthViewModel
import com.limitcrosses.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onLoggedOut: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val uiState by settingsViewModel.uiState.collectAsState()

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), color = CosmicTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = CosmicTextPrimary)
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
            // Profile Card
            Text(
                text = stringResource(R.string.settings_section_profile),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CosmicCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = uiState.currentUser?.fullName ?: "User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Role: ${uiState.currentUser?.role?.name ?: "USER"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicSecondary
                    )

                    val handles = uiState.currentUser?.socialHandles
                    if (handles?.facebook != null || handles?.instagram != null || handles?.snapchat != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = CosmicCardBorder)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Linked Display Handles:", style = MaterialTheme.typography.labelSmall, color = CosmicTextMuted)
                        handles.facebook?.let { Text(text = "FB: $it", style = MaterialTheme.typography.bodyMedium, color = CosmicTextSecondary) }
                        handles.instagram?.let { Text(text = "IG: $it", style = MaterialTheme.typography.bodyMedium, color = CosmicTextSecondary) }
                        handles.snapchat?.let { Text(text = "SC: $it", style = MaterialTheme.typography.bodyMedium, color = CosmicTextSecondary) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Language Selection
            Text(
                text = stringResource(R.string.settings_section_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CosmicCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = uiState.currentLanguage == "en",
                        onClick = { settingsViewModel.setLanguage("en") },
                        label = { Text(stringResource(R.string.settings_lang_en)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CosmicPrimary,
                            selectedLabelColor = CosmicTextPrimary,
                            containerColor = CosmicSurface,
                            labelColor = CosmicTextSecondary
                        )
                    )
                    FilterChip(
                        selected = uiState.currentLanguage == "hi",
                        onClick = { settingsViewModel.setLanguage("hi") },
                        label = { Text(stringResource(R.string.settings_lang_hi)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CosmicPrimary,
                            selectedLabelColor = CosmicTextPrimary,
                            containerColor = CosmicSurface,
                            labelColor = CosmicTextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Backend Connectivity (Demo/Local Mode vs Cloud Mode)
            Text(
                text = stringResource(R.string.settings_section_backend),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CosmicCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isCloudSyncEnabled) stringResource(R.string.status_cloud_mode) else stringResource(R.string.status_demo_mode),
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isCloudSyncEnabled) CosmicSecondary else CosmicSuccess
                            )
                            Text(
                                text = if (uiState.isCloudSyncEnabled) "Synchronizes state with external cloud endpoint." else "All accounts, limits, & audit records stored securely locally.",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicTextMuted
                            )
                        }
                        Switch(
                            checked = uiState.isCloudSyncEnabled,
                            onCheckedChange = { settingsViewModel.toggleCloudSync(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CosmicSecondary,
                                checkedTrackColor = CosmicPrimary
                            )
                        )
                    }

                    if (uiState.isCloudSyncEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = uiState.cloudEndpoint,
                            onValueChange = { settingsViewModel.setCloudEndpoint(it) },
                            label = { Text(stringResource(R.string.settings_cloud_endpoint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicSecondary,
                                unfocusedBorderColor = CosmicCardBorder,
                                focusedTextColor = CosmicTextPrimary,
                                unfocusedTextColor = CosmicTextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy Policy Link
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CosmicSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.nav_privacy),
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary
                        )
                        Text(
                            text = "Review zero-leakage data retention and visibility policies.",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToPrivacy) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CosmicSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Out
            OutlinedButton(
                onClick = { authViewModel.logout { onLoggedOut() } },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = CosmicTextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.action_logout), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Delete Account Button
            Button(
                onClick = { settingsViewModel.showDeleteConfirmation(true) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CosmicDanger.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicDanger)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CosmicDanger)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.action_delete_account), color = CosmicDanger, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Account Deletion Confirmation Dialog
    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.showDeleteConfirmation(false) },
            title = { Text(stringResource(R.string.settings_confirm_delete_title)) },
            text = { Text(stringResource(R.string.settings_confirm_delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.confirmDeleteAccount {
                            onAccountDeleted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicDanger)
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { settingsViewModel.showDeleteConfirmation(false) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

