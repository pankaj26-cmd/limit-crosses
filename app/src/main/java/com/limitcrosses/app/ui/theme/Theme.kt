package com.limitcrosses.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode(val id: String, val title: String) {
    SYSTEM("SYSTEM", "System Default"),
    DARK("DARK", "Cosmic Dark"),
    LIGHT("LIGHT", "Solar Dawn (Light)");

    companion object {
        fun fromId(id: String?): AppThemeMode {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
        }
    }
}

val LocalThemeMode = compositionLocalOf { AppThemeMode.SYSTEM }
val LocalThemeChanger = compositionLocalOf<(AppThemeMode) -> Unit> { {} }

private val CosmicDarkColorScheme = darkColorScheme(
    primary = BaseCosmicPrimary,
    onPrimary = Color.White,
    secondary = BaseCosmicSecondary,
    onSecondary = Color.Black,
    tertiary = CosmicAccent,
    onTertiary = Color.White,
    background = DeepCosmosNavy,
    onBackground = Color(0xFFF8FAFC),
    surface = DeepCosmosCard,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xDD18223C),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = DeepCosmosBorder,
    error = BaseCosmicDanger,
    onError = Color.White
)

private val CosmicLightColorScheme = lightColorScheme(
    primary = BaseCosmicLightPrimary,
    onPrimary = Color.White,
    secondary = BaseCosmicLightSecondary,
    onSecondary = Color.White,
    tertiary = Color(0xFFDB2777),
    onTertiary = Color.White,
    background = SolarDawnBackground,
    onBackground = Color(0xFF0F172A),
    surface = SolarDawnSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SolarDawnCard,
    onSurfaceVariant = Color(0xFF475569),
    outline = SolarDawnBorder,
    error = BaseCosmicLightDanger,
    onError = Color.White
)

@Composable
fun LimitCrossesTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeChange: (AppThemeMode) -> Unit = {},
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemInDark
    }

    val colorScheme = if (isDark) CosmicDarkColorScheme else CosmicLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalThemeChanger provides onThemeChange
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
