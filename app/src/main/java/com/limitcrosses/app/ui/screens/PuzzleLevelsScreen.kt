package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.limitcrosses.app.ui.components.PankajYadavFooter
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.AuthViewModel
import com.limitcrosses.app.ui.viewmodel.PuzzleViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleLevelsScreen(
    viewModel: PuzzleViewModel,
    authViewModel: AuthViewModel? = null,
    onBack: () -> Unit,
    onProceedToRequest: () -> Unit,
    onLoggedOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Prepare fresh run if not eligible or if restarting
    LaunchedEffect(Unit) {
        viewModel.checkAndPrepareRun()
    }

    // Requirement: If user backgrounds the app, reset puzzles with new questions
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.resetOnBackground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.puzzle_screen_title), color = CosmicTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = CosmicTextPrimary)
                    }
                },
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
            if (uiState.isAllCompleted) {
                // ==========================================
                // ALL 5 PUZZLES SOLVED CARD (Image 1 State)
                // ==========================================
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CosmicCard,
                    border = BorderStroke(2.dp, CosmicSuccess)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CosmicSuccess.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CosmicSuccess,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.puzzle_all_completed_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(R.string.puzzle_all_completed_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Trigger "Are you sure?" Questioning Dialog before extension request
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                        ) {
                            Text(
                                text = stringResource(R.string.puzzle_btn_proceed_request),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // 5-LEVEL PUZZLE CHALLENGE INTERFACE
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.puzzle_level_indicator, uiState.currentLevel),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary
                    )
                    Text(
                        text = "5 Consecutive Required",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5-step progress indicator dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..5) {
                        val isFinished = i < uiState.currentLevel
                        val isCurrent = i == uiState.currentLevel
                        val dotColor = when {
                            isFinished -> CosmicSuccess
                            isCurrent -> CosmicPrimary
                            else -> CosmicSurface
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(dotColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Single-Use Hint Card / Button (Constraint: Only 1 hint across whole 5-puzzle run)
                if (uiState.isHintRevealed) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CosmicSecondary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, CosmicSecondary)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = CosmicSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Single-Use Hint (Revealed for Level ${uiState.currentLevel})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.currentQuestion.accessibilityHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicTextPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (uiState.isHintUsedInSession) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CosmicSurface,
                                border = BorderStroke(1.dp, CosmicCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = CosmicTextMuted, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Hint Used (0 remaining for run)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmicTextMuted
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.useHint() },
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CosmicSecondary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary)
                            ) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Use Single Hint (1 Available)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Question Prompt Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CosmicCard,
                    border = BorderStroke(1.dp, CosmicCardBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = CosmicSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Deterministic Logic Challenge",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.currentQuestion.prompt,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = CosmicTextPrimary,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Options List
                uiState.currentQuestion.options.forEachIndexed { index, optionText ->
                    val isSelected = uiState.selectedOptionIndex == index
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectOption(index) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CosmicPrimary.copy(alpha = 0.2f) else CosmicSurface,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) CosmicPrimary else CosmicCardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectOption(index) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CosmicSecondary,
                                    unselectedColor = CosmicTextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = CosmicTextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (uiState.statusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (uiState.isError) CosmicDanger.copy(alpha = 0.15f) else CosmicSuccess.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (uiState.isError) CosmicDanger else CosmicSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.statusMessage ?: "",
                                color = if (uiState.isError) CosmicDanger else CosmicSuccess,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.submitAnswer() },
                    enabled = uiState.selectedOptionIndex != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text(
                        text = stringResource(R.string.puzzle_btn_submit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Rules: Any mistake or backgrounding the app resets progress to Level 1 with new questions. Only 1 hint allowed per session.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            PankajYadavFooter()
        }
    }

    // ==========================================
    // "ARE YOU SURE YOU WANT TO EXTEND?" DIALOG (Point 5)
    // ==========================================
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Are you sure you want to extend?",
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "You have proven strong intentional focus by passing 5 consecutive cognitive challenges.\n\nBefore asking your Administrator for extra time, reflect: do you truly need to cross your daily limit today, or is this a mindful moment to step away?",
                    color = CosmicTextSecondary,
                    lineHeight = 20.sp,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        coroutineScope.launch {
                            viewModel.markEligibleInRepository()
                            onProceedToRequest()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text("Yes, Request Time", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onBack()
                }) {
                    Text("No, Keep My Limit", color = CosmicSecondary, fontWeight = FontWeight.SemiBold)
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
