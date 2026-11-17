package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.limitcrosses.app.R
import com.limitcrosses.app.core.model.ExtensionRequest
import com.limitcrosses.app.core.model.ExtensionStatus
import com.limitcrosses.app.ui.components.PankajYadavFooter
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.AuthViewModel
import com.limitcrosses.app.ui.viewmodel.ExtensionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionRequestsScreen(
    viewModel: ExtensionViewModel,
    authViewModel: AuthViewModel? = null,
    onBack: () -> Unit,
    onNavigateToPuzzles: () -> Unit,
    onLoggedOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var minutesStr by remember { mutableStateOf("10") }
    var reasonStr by remember { mutableStateOf("") }
    var showConfirmSubmitDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Critical Fix: Always refresh eligibility and history whenever screen enters or resumes
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ext_title), color = CosmicTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = CosmicTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = CosmicTextPrimary)
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
                .padding(20.dp)
        ) {
            if (!uiState.isEligible) {
                // Not eligible warning card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CosmicCard,
                    border = BorderStroke(1.dp, CosmicWarning)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = CosmicWarning)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Verification Required",
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To submit an extension request, you must first demonstrate intentional focus by solving 5 consecutive logic puzzles.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToPuzzles,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.limit_reached_btn_request_time),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Submission form
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CosmicCard,
                    border = BorderStroke(1.5.dp, CosmicSuccess.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CosmicSuccess, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Puzzle Verification Passed",
                                fontWeight = FontWeight.Bold,
                                color = CosmicSuccess,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "You are eligible to request extra screen time (up to 20 minutes) from your Administrator.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Select Requested Minutes (1–20 min):",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextMuted,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick selection chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("5", "10", "15", "20").forEach { preset ->
                                val isSelected = minutesStr == preset
                                OutlinedButton(
                                    onClick = { minutesStr = preset },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) CosmicSecondary else CosmicCardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) CosmicSecondary.copy(alpha = 0.2f) else CosmicSurface,
                                        contentColor = if (isSelected) CosmicSecondary else CosmicTextSecondary
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("$preset m", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = minutesStr,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }
                                val num = filtered.toIntOrNull()
                                if (num == null) {
                                    minutesStr = filtered
                                } else {
                                    minutesStr = num.coerceIn(1, 20).toString()
                                }
                            },
                            label = { Text("Requested Minutes (Max 20)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = reasonStr,
                            onValueChange = { reasonStr = it },
                            label = { Text(stringResource(R.string.ext_reason_label)) },
                            placeholder = { Text("Constructive justification for your administrator") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicPrimary,
                                unfocusedBorderColor = CosmicCardBorder,
                                focusedTextColor = CosmicTextPrimary,
                                unfocusedTextColor = CosmicTextPrimary
                            )
                        )

                        if (uiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = uiState.errorMessage ?: "", color = CosmicDanger, style = MaterialTheme.typography.bodyMedium)
                        }

                        if (uiState.successMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = uiState.successMessage ?: "", color = CosmicSuccess, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Submit Button triggers confirmation questioning
                        Button(
                            onClick = { showConfirmSubmitDialog = true },
                            enabled = reasonStr.isNotBlank() && !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                        ) {
                            Text(
                                text = stringResource(R.string.ext_btn_submit),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // History of requests
            Text(
                text = stringResource(R.string.ext_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.requests.isEmpty()) {
                Text(
                    text = "No previous extension requests submitted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicTextMuted
                )
            } else {
                uiState.requests.forEach { req ->
                    RequestHistoryCard(req = req)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            PankajYadavFooter()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation Questioning Dialog (Point 5)
    if (showConfirmSubmitDialog) {
        val chosenMin = (minutesStr.toIntOrNull() ?: 10).coerceIn(1, 20)
        AlertDialog(
            onDismissRequest = { showConfirmSubmitDialog = false },
            title = {
                Text("Confirm Extension Request", fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to request +$chosenMin minutes from your Administrator?\n\nReason: \"${reasonStr.trim()}\"",
                    color = CosmicTextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSubmitDialog = false
                        viewModel.submitRequest(chosenMin, reasonStr) {
                            reasonStr = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text("Confirm & Submit", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSubmitDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Log Out Dialog
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

@Composable
private fun RequestHistoryCard(req: ExtensionRequest) {
    val statusColor = when (req.status) {
        ExtensionStatus.PENDING -> CosmicWarning
        ExtensionStatus.APPROVED -> CosmicSuccess
        ExtensionStatus.DENIED -> CosmicDanger
        ExtensionStatus.MODIFIED -> CosmicSecondary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CosmicSurface,
        border = BorderStroke(1.dp, CosmicCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Requested: +${req.requestedMinutes} min",
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary,
                    fontSize = 15.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = req.status.name,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Reason: ${req.reason}",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicTextSecondary
            )

            if (req.grantedMinutes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Granted by Admin: +${req.grantedMinutes} min",
                    color = CosmicSuccess,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (!req.adminNotes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Admin Note: ${req.adminNotes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextMuted
                )
            }
        }
    }
}
