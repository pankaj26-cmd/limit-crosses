package com.limitcrosses.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// === Fixed Static Base Colors ===
val DeepCosmosVoid = Color(0xFF070A14)
val DeepCosmosNavy = Color(0xFF0F1526)
val DeepCosmosCard = Color(0xCC16223E)
val DeepCosmosBorder = Color(0x664A669F)

val SolarDawnBackground = Color(0xFFF1F5F9)
val SolarDawnSurface = Color(0xEEFFFFFF)
val SolarDawnCard = Color(0xF5F8FAFC)
val SolarDawnBorder = Color(0xFFCBD5E1)

// === Static Base Palettes for ColorSchemes ===
val BaseCosmicPrimary = Color(0xFF8B5CF6)
val BaseCosmicLightPrimary = Color(0xFF6D28D9)
val BaseCosmicSecondary = Color(0xFF00E5FF)
val BaseCosmicLightSecondary = Color(0xFF0284C7)

val BaseBrandSnapchat = Color(0xFFFFFC00)
val BaseBrandSnapchatLight = Color(0xFFB45309) // Deep amber gold with high contrast on white/light cards

val BaseCosmicSuccess = Color(0xFF10B981)
val BaseCosmicLightSuccess = Color(0xFF059669)
val BaseCosmicWarning = Color(0xFFF59E0B)
val BaseCosmicLightWarning = Color(0xFFD97706)
val BaseCosmicDanger = Color(0xFFEF4444)
val BaseCosmicLightDanger = Color(0xFFDC2626)

val BrandFacebook = Color(0xFF1877F2)
val BrandInstagram = Color(0xFFE1306C)
val CosmicAccent = Color(0xFFFF4081)
val CosmicPrimaryVariant = Color(0xFF7C3AED)

// === Dynamic Composable Tokens (Auto-adapts to Light & Dark Theme for Crisp Contrast) ===
private val isLightMode: Boolean
    @Composable
    @ReadOnlyComposable
    get() {
        val bg = MaterialTheme.colorScheme.background
        return (0.2126f * bg.red + 0.7152f * bg.green + 0.0722f * bg.blue) > 0.5f
    }

val CosmicPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLightMode) BaseCosmicLightPrimary else BaseCosmicPrimary

val CosmicSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLightMode) BaseCosmicLightSecondary else BaseCosmicSecondary

val BrandSnapchat: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLightMode) BaseBrandSnapchatLight else BaseBrandSnapchat

val CosmicSuccess: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLightMode) BaseCosmicLightSuccess else BaseCosmicSuccess

val CosmicWarning: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLightMode) BaseCosmicLightWarning else BaseCosmicWarning

val CosmicDanger: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLightMode) BaseCosmicLightDanger else BaseCosmicDanger

val CosmicBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = Color.Transparent

val CosmicSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val CosmicCard: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val CosmicCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

val CosmicTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val CosmicTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val CosmicTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isLightMode) Color(0xFF475569) else Color(0xFF94A3B8)
