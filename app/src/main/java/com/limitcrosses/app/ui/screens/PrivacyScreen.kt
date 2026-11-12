package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title), color = CosmicTextPrimary) },
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
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = CosmicPrimary.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CosmicSecondary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Data Dignity & Transparency",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = "Last updated: November 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. What We Collect
            PrivacySectionCard(
                title = stringResource(R.string.privacy_section_collected_title),
                body = stringResource(R.string.privacy_section_collected_body),
                icon = Icons.Default.CheckCircle,
                iconTint = CosmicSuccess
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. What We Never Collect
            PrivacySectionCard(
                title = stringResource(R.string.privacy_section_not_collected_title),
                body = stringResource(R.string.privacy_section_not_collected_body),
                icon = Icons.Default.Block,
                iconTint = CosmicDanger
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. What Admins Can See
            PrivacySectionCard(
                title = stringResource(R.string.privacy_section_admin_visibility_title),
                body = stringResource(R.string.privacy_section_admin_visibility_body),
                icon = Icons.Default.Visibility,
                iconTint = CosmicSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Data Retention & Deletion
            PrivacySectionCard(
                title = stringResource(R.string.privacy_section_retention_title),
                body = stringResource(R.string.privacy_section_retention_body),
                icon = Icons.Default.Delete,
                iconTint = CosmicPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrivacySectionCard(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CosmicCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicTextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

