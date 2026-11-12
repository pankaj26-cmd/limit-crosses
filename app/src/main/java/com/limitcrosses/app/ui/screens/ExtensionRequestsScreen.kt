package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.core.model.ExtensionRequest
import com.limitcrosses.app.core.model.ExtensionStatus
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.ExtensionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionRequestsScreen(
    viewModel: ExtensionViewModel,
    onBack: () -> Unit,
    onNavigateToPuzzles: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var minutesStr by remember { mutableStateOf("10") }
    var reasonStr by remember { mutableStateOf("") }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ext_title), color = CosmicTextPrimary) },
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
                .padding(20.dp)
        ) {
            if (!uiState.isEligible) {
                // Not eligible warning
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CosmicCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicWarning)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = CosmicWarning)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Verification Required",
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To submit an extension request, you must first demonstrate intentional focus by solving 5 consecutive logic puzzles.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToPuzzles,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                        ) {
                            Text(
                                text = stringResource(R.string.limit_reached_btn_request_time),
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary
                            )
                        }
                    }
                }
            } else {
                // Submission form
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CosmicCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicPrimary)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.ext_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = minutesStr,
                            onValueChange = { minutesStr = it.filter { char -> char.isDigit() } },
                            label = { Text(stringResource(R.string.ext_minutes_label)) },
                            placeholder = { Text(stringResource(R.string.ext_minutes_hint)) },
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

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = reasonStr,
                            onValueChange = { reasonStr = it },
                            label = { Text(stringResource(R.string.ext_reason_label)) },
                            placeholder = { Text(stringResource(R.string.ext_reason_hint)) },
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

                        Button(
                            onClick = {
                                val min = minutesStr.toIntOrNull() ?: 10
                                viewModel.submitRequest(min, reasonStr) {
                                    reasonStr = ""
                                }
                            },
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
                                color = CosmicTextPrimary
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RequestHistoryCard(req: ExtensionRequest) {
    val statusColor = when (req.status) {
        ExtensionStatus.PENDING -> CosmicWarning
        ExtensionStatus.APPROVED -> CosmicSuccess
        ExtensionStatus.MODIFIED -> CosmicSecondary
        ExtensionStatus.DENIED -> CosmicDanger
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CosmicSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Requested: ${req.requestedMinutes} min",
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = req.status.name,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
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

            if (!req.adminNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ext_admin_notes, req.adminNotes ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicSecondary
                )
            }
        }
    }
}

