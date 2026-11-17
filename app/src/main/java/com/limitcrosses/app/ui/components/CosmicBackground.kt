package com.limitcrosses.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.limitcrosses.app.R
import com.limitcrosses.app.ui.theme.AppThemeMode
import com.limitcrosses.app.ui.theme.LocalThemeMode
import kotlinx.coroutines.delay
import kotlin.random.Random

data class RealCosmicScene(
    val id: String,
    val name: String,
    val constellation: String,
    val drawableRes: Int,
    val starlightAccent: Color,
    val seed: Long
)

object CosmicAtmosphereManager {
    // 4 high-definition deep space wallpapers for Dark Mode (Moon, Spiral Galaxy, Nebula, Earth & Lunar Orbit)
    val darkScenes = listOf(
        RealCosmicScene(
            id = "moon",
            name = "Luminous Lunar Corona",
            constellation = "Luna Celestial",
            drawableRes = R.drawable.cosmos_moon,
            starlightAccent = Color(0xFF00E5FF),
            seed = 101L
        ),
        RealCosmicScene(
            id = "galaxy",
            name = "Andromeda Spiral Galaxy",
            constellation = "Andromeda",
            drawableRes = R.drawable.cosmos_galaxy,
            starlightAccent = Color(0xFFF472B6),
            seed = 202L
        ),
        RealCosmicScene(
            id = "nebula",
            name = "Pillars of Creation Nebula",
            constellation = "Serpens Deep",
            drawableRes = R.drawable.cosmos_nebula,
            starlightAccent = Color(0xFF34D399),
            seed = 303L
        ),
        RealCosmicScene(
            id = "earth_moon",
            name = "Earth & Moon Orbital Horizon",
            constellation = "Solar System",
            drawableRes = R.drawable.cosmos_earth,
            starlightAccent = Color(0xFF60A5FA),
            seed = 404L
        )
    )

    // Ethereal Solar Dawn scene for Light Mode (Crescent Moon, Golden Sunlight, Stardust)
    val lightScene = RealCosmicScene(
        id = "solar_dawn",
        name = "Solar Dawn & Crescent Moon",
        constellation = "Helios Aurora",
        drawableRes = R.drawable.cosmos_dawn,
        starlightAccent = Color(0xFFF59E0B),
        seed = 999L
    )

    /**
     * Rotates dark cosmos scene every 10 minutes (600,000 ms).
     */
    fun getCurrentDarkSceneIndex(): Int {
        val tenMinuteSlot = System.currentTimeMillis() / (10 * 60 * 1000L)
        return (tenMinuteSlot % darkScenes.size).toInt()
    }
}

@Composable
fun CosmicBackgroundContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val themeMode = LocalThemeMode.current
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemInDark
    }

    var darkSceneIndex by remember { mutableStateOf(CosmicAtmosphereManager.getCurrentDarkSceneIndex()) }

    // Auto-refresh dark scene every 10 minutes
    LaunchedEffect(isDark) {
        if (isDark) {
            while (true) {
                delay(15_000L)
                val updated = CosmicAtmosphereManager.getCurrentDarkSceneIndex()
                if (updated != darkSceneIndex) {
                    darkSceneIndex = updated
                }
            }
        }
    }

    val activeScene = if (isDark) {
        CosmicAtmosphereManager.darkScenes[darkSceneIndex]
    } else {
        CosmicAtmosphereManager.lightScene
    }

    // Twinkle animation for stars
    val infiniteTransition = rememberInfiniteTransition(label = "cosmicEffects")
    val twinkleFactor by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    // Animated shooting star (meteor) across the cosmos
    val shootingStarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shootingStar"
    )

    // Precompute ambient stardust
    val stars = remember(activeScene.seed) {
        val rand = Random(activeScene.seed)
        List(45) {
            CosmicParticle(
                normX = rand.nextFloat(),
                normY = rand.nextFloat(),
                radius = 1.2f + rand.nextFloat() * 2.2f,
                alpha = 0.4f + rand.nextFloat() * 0.6f,
                twinkleSpeed = 0.6f + rand.nextFloat() * 1.4f
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. High-Resolution Wallpaper Layer with smooth crossfade on 10-minute switch
        Crossfade(
            targetState = activeScene.drawableRes,
            animationSpec = tween(1200),
            label = "cosmosWallpaperCrossfade"
        ) { resId ->
            Image(
                painter = painterResource(id = resId),
                contentDescription = activeScene.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 2. Atmospheric Contrast Scrim / Tint Layer
        if (isDark) {
            // Dark Mode: Deep space gradient scrim ensuring crystal-clear text contrast while preserving celestial bodies
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x99040711), // Top: 60% deep cosmos void (protects top headers & icons over moon)
                                Color(0xB8070B1A), // Mid-top: 72% deep space navy (eliminates lunar washout)
                                Color(0xD00A1024), // Mid-bottom: 82% deep space navy (high-contrast body)
                                Color(0xEE04060E)  // Bottom: 93% deep void (rock-solid footer contrast)
                            )
                        )
                    )
            )
        } else {
            // Light Mode: Ethereal Solar Dawn veil ensuring clear contrast for dark text
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xB0FFFFFF), // Top: 69% clean white veil
                                Color(0xC4FFFFFF), // Mid-top: 77% clean white veil
                                Color(0xD8FFFFFF), // Mid-bottom: 85% clean white veil
                                Color(0xF0FFFFFF)  // Bottom: 94% clean white veil
                            )
                        )
                    )
            )
        }

        // 3. Dynamic Animated Cosmic Effects Canvas (Twinkling Stars & Shooting Star)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Render twinkling stars
            stars.forEach { star ->
                val dynamicAlpha = (star.alpha * twinkleFactor * star.twinkleSpeed).coerceIn(0.15f, 1f)
                val starColor = if (isDark) Color.White.copy(alpha = dynamicAlpha) else activeScene.starlightAccent.copy(alpha = dynamicAlpha * 0.8f)

                drawCircle(
                    color = starColor,
                    radius = star.radius,
                    center = Offset(star.normX * width, star.normY * height)
                )

                // Accent halo on diamond stars
                if (star.radius > 2.2f && isDark) {
                    drawCircle(
                        color = activeScene.starlightAccent.copy(alpha = dynamicAlpha * 0.45f),
                        radius = star.radius * 2.5f,
                        center = Offset(star.normX * width, star.normY * height)
                    )
                }
            }

            // Render Shooting Star / Meteor streak in Dark Mode
            if (isDark) {
                // Active during the first 18% of the cycle
                if (shootingStarProgress < 0.18f) {
                    val progress = shootingStarProgress / 0.18f
                    val startX = width * 0.85f - (progress * width * 0.70f)
                    val startY = height * 0.10f + (progress * height * 0.30f)
                    val trailLength = width * 0.22f
                    val endX = startX + trailLength * 0.8f
                    val endY = startY - trailLength * 0.35f
                    val meteorAlpha = (1f - progress).coerceIn(0f, 1f) * 0.85f

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                activeScene.starlightAccent.copy(alpha = meteorAlpha * 0.5f),
                                Color.White.copy(alpha = meteorAlpha)
                            ),
                            start = Offset(endX, endY),
                            end = Offset(startX, startY)
                        ),
                        start = Offset(endX, endY),
                        end = Offset(startX, startY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // 4. Foreground UI Content inside the cosmic container
        content()
    }
}

private data class CosmicParticle(
    val normX: Float,
    val normY: Float,
    val radius: Float,
    val alpha: Float,
    val twinkleSpeed: Float
)
