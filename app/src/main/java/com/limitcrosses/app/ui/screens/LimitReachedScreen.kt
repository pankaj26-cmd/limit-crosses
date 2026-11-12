package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.ui.theme.*

@Composable
fun LimitReachedScreen(
    onNavigateToPuzzles: () -> Unit,
    onNavigateToCosmos: () -> Unit
) {
    Scaffold(
        containerColor = CosmicBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.size(90.dp),
                shape = RoundedCornerShape(26.dp),
                color = CosmicDanger.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(2.dp, CosmicDanger)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = CosmicDanger,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.limit_reached_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.limit_reached_remaining_zero),
                style = MaterialTheme.typography.titleLarge,
                color = CosmicDanger,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.limit_reached_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = CosmicTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.limit_reached_reset_info),
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Action: Solve Puzzles to Request Time
            Button(
                onClick = onNavigateToPuzzles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = CosmicTextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.limit_reached_btn_request_time),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CosmicTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Explore Cosmos
            OutlinedButton(
                onClick = onNavigateToCosmos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicSecondary),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CosmicSecondary)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = CosmicSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.limit_reached_btn_learn_cosmos),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = CosmicSecondary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Best effort transparent label
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CosmicSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Text(
                    text = stringResource(R.string.limit_reached_best_effort_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            com.limitcrosses.app.ui.components.PankajYadavFooter()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

