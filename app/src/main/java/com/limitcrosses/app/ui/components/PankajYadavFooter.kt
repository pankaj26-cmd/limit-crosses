package com.limitcrosses.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limitcrosses.app.R
import com.limitcrosses.app.ui.theme.*

@Composable
fun PankajYadavFooter(
    modifier: Modifier = Modifier,
    detailed: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing accent gradient divider
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CosmicPrimary.copy(alpha = 0.6f),
                            CosmicSecondary.copy(alpha = 0.8f),
                            CosmicPrimary.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(1.dp)
                )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CosmicCard.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CosmicSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                
                val creatorText = buildAnnotatedString {
                    append("Created by ")
                    withStyle(style = SpanStyle(color = CosmicSecondary, fontWeight = FontWeight.Bold)) {
                        append("Pankaj Yadav")
                    }
                }

                Text(
                    text = creatorText,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = CosmicTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (detailed) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.footer_crafted_by),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = CosmicTextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

