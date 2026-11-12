package com.limitcrosses.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CosmicColorScheme = darkColorScheme(
    primary = CosmicPrimary,
    onPrimary = CosmicTextPrimary,
    secondary = CosmicSecondary,
    onSecondary = CosmicBackground,
    tertiary = CosmicAccent,
    background = CosmicBackground,
    onBackground = CosmicTextPrimary,
    surface = CosmicSurface,
    onSurface = CosmicTextPrimary,
    surfaceVariant = CosmicCard,
    onSurfaceVariant = CosmicTextSecondary,
    outline = CosmicCardBorder,
    error = CosmicDanger,
    onError = CosmicTextPrimary
)

@Composable
fun LimitCrossesTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CosmicBackground.toArgb()
                window.navigationBarColor = CosmicBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}

