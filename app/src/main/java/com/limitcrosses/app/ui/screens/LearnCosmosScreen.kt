package com.limitcrosses.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.core.model.CosmosCategory
import com.limitcrosses.app.core.model.CosmosTopic
import com.limitcrosses.app.ui.theme.*
import com.limitcrosses.app.ui.viewmodel.CosmosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnCosmosScreen(
    viewModel: CosmosViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val topics by viewModel.topics.collectAsState()

    Scaffold(
        containerColor = CosmicBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cosmos_title), color = CosmicTextPrimary) },
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
        ) {
            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CosmosCategory.values().forEach { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(cat) },
                        label = {
                            Text(
                                text = when (cat) {
                                    CosmosCategory.ALL -> stringResource(R.string.cosmos_cat_all)
                                    CosmosCategory.UNIVERSE -> stringResource(R.string.cosmos_cat_universe)
                                    CosmosCategory.GALAXIES -> stringResource(R.string.cosmos_cat_galaxies)
                                    CosmosCategory.STARS -> stringResource(R.string.cosmos_cat_stars)
                                    CosmosCategory.BLACK_HOLES -> stringResource(R.string.cosmos_cat_blackholes)
                                    CosmosCategory.PLANETS -> stringResource(R.string.cosmos_cat_planets)
                                    CosmosCategory.MISSIONS -> stringResource(R.string.cosmos_cat_missions)
                                },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CosmicPrimary,
                            selectedLabelColor = CosmicTextPrimary,
                            containerColor = CosmicSurface,
                            labelColor = CosmicTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = CosmicCardBorder,
                            selectedBorderColor = CosmicSecondary
                        )
                    )
                }
            }

            // Topics List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(topics, key = { it.id }) { topic ->
                    CosmosTopicCard(
                        topic = topic,
                        onOpenUrl = { viewModel.openSourceArticle(context, topic.sourceUrl) }
                    )
                }
                item {
                    com.limitcrosses.app.ui.components.PankajYadavFooter(detailed = true)
                }
            }
        }
    }
}

@Composable
private fun CosmosTopicCard(
    topic: CosmosTopic,
    onOpenUrl: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CosmicCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CosmicSecondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = topic.category.name,
                        color = CosmicSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.cosmos_verified_date, topic.lastVerifiedDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CosmicTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = topic.fullDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicTextSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cosmos_source_label, topic.sourceName),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextMuted
                )

                TextButton(onClick = onOpenUrl) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        tint = CosmicSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.cosmos_btn_open_source),
                        color = CosmicSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

