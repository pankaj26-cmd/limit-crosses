package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.domain.repository.DailyHistoryItem
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.UsageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageDetailsScreen(
    viewModel: UsageViewModel,
    onBack: () -> Unit
) {
    val report by viewModel.currentReport.collectAsState()
    val history by viewModel.historyItems.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.usage_details_title), color = CosmicTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = CosmicTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Diagnostics Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CosmicCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.usage_diagnostics_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.usage_last_polled, diagnostics.lastPolledString),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.usage_timezone, diagnostics.timezoneString),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.usage_cloud_sync_status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmicSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7-Day History Table
            Text(
                text = stringResource(R.string.usage_history_7days),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            history.forEach { item ->
                HistoryRow(item = item)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryRow(item: DailyHistoryItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CosmicSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.dateString,
                    fontWeight = FontWeight.SemiBold,
                    color = CosmicTextPrimary
                )
                Text(
                    text = "Cap: ${item.limitMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextMuted
                )
            }

            Text(
                text = "${item.usedMinutes} min",
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary,
                modifier = Modifier.padding(end = 12.dp)
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (item.reachedCap) CosmicDanger.copy(alpha = 0.2f) else CosmicSuccess.copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (item.reachedCap) stringResource(R.string.usage_status_reached) else stringResource(R.string.usage_status_within),
                    color = if (item.reachedCap) CosmicDanger else CosmicSuccess,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

