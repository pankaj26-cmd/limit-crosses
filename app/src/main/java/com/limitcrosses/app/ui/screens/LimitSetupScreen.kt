package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.ui.components.PankajYadavFooter
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.AuthViewModel
import com.limitcrosses.app.ui.viewmodel.LimitSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitSetupScreen(
    viewModel: LimitSetupViewModel,
    authViewModel: AuthViewModel? = null,
    onConfirmed: () -> Unit,
    onLoggedOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.limit_setup_title), color = CosmicTextPrimary) },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.size(68.dp),
                shape = RoundedCornerShape(20.dp),
                color = CosmicPrimary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CosmicSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.limit_setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Crucial prompt requirement: 45 min COMBINED, NOT PER APP
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CosmicSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicSecondary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CosmicSecondary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.limit_combined_rule_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = CosmicTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Limit Locking Warning
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CosmicCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicWarning.copy(alpha = 0.4f))
            ) {
                Text(
                    text = stringResource(R.string.limit_locking_warning),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicWarning,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Gauge display of chosen minutes
            Surface(
                modifier = Modifier.size(160.dp),
                shape = RoundedCornerShape(80.dp),
                color = CosmicCard,
                border = androidx.compose.foundation.BorderStroke(3.dp, CosmicPrimary)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${uiState.currentSelectedMinutes}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CosmicSecondary
                    )
                    Text(
                        text = "MINUTES / DAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 0..45 Minutes Slider (1-minute increments)
            Slider(
                value = uiState.currentSelectedMinutes.toFloat(),
                onValueChange = { viewModel.onSliderChanged(it.toInt()) },
                valueRange = 0f..45f,
                steps = 44, // 1-minute steps between 0 and 45
                enabled = !uiState.isLocked && !uiState.isLoading,
                colors = SliderDefaults.colors(
                    thumbColor = CosmicSecondary,
                    activeTrackColor = CosmicPrimary,
                    inactiveTrackColor = CosmicCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "0 min", style = MaterialTheme.typography.labelSmall, color = CosmicTextMuted)
                Text(text = "20 min", style = MaterialTheme.typography.labelSmall, color = CosmicTextMuted)
                Text(text = "45 min (Max)", style = MaterialTheme.typography.labelSmall, color = CosmicTextMuted)
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = uiState.errorMessage ?: "",
                    color = CosmicDanger,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isLocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CosmicSurface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.limit_locked_status),
                            fontWeight = FontWeight.Bold,
                            color = CosmicWarning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.limit_locked_desc, uiState.currentSelectedMinutes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onConfirmed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text(
                        text = stringResource(R.string.action_continue),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = {
                        viewModel.confirmAndLock {
                            onConfirmed()
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            text = stringResource(R.string.limit_btn_confirm_lock),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            PankajYadavFooter()
            Spacer(modifier = Modifier.height(16.dp))
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
}

