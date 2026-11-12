package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.PuzzleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleLevelsScreen(
    viewModel: PuzzleViewModel,
    onBack: () -> Unit,
    onProceedToRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.puzzle_screen_title), color = CosmicTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = CosmicTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleAccessibilityMode() }) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = stringResource(R.string.puzzle_accessibility_toggle),
                            tint = if (uiState.isAccessibilityMode) CosmicSecondary else CosmicTextMuted
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
                // Congratulations / Eligible Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CosmicCard,
                    border = androidx.compose.foundation.BorderStroke(2.dp, CosmicSuccess)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CosmicSuccess,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.puzzle_all_completed_title),
                            style = MaterialTheme.typography.headlineMedium,
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
                        Button(
                            onClick = onProceedToRequest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                        ) {
                            Text(
                                text = stringResource(R.string.puzzle_btn_proceed_request),
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary
                            )
                        }
                    }
                }
            } else {
                // 5-Level Progress Header
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

                // 5 step progress dots
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

                Spacer(modifier = Modifier.height(20.dp))

                // Accessibility Banner (if active)
                if (uiState.isAccessibilityMode) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = CosmicSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicSecondary)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.puzzle_accessibility_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.currentQuestion.accessibilityHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CosmicTextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Question Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CosmicCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
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

                Spacer(modifier = Modifier.height(20.dp))

                // Options List
                uiState.currentQuestion.options.forEachIndexed { index, optionText ->
                    val isSelected = uiState.selectedOptionIndex == index
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectOption(index) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CosmicPrimary.copy(alpha = 0.2f) else CosmicSurface,
                        border = androidx.compose.foundation.BorderStroke(
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

                Spacer(modifier = Modifier.height(24.dp))

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
                        color = CosmicTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Rule: Any mistake resets your progress immediately back to Level 1.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

