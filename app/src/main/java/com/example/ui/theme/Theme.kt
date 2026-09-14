package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// PRD Section 22.10 Design Tokens
object GlassTokens {
    // Spacing
    val spaceXs: Dp = 4.dp
    val spaceSm: Dp = 8.dp
    val spaceMd: Dp = 16.dp
    val spaceLg: Dp = 24.dp
    val spaceXl: Dp = 32.dp
    val spaceXxl: Dp = 48.dp
    val screenMargin: Dp = 20.dp

    // Corner Radius
    val radiusSm = RoundedCornerShape(12.dp)
    val radiusMd = RoundedCornerShape(24.dp)
    val radiusLg = RoundedCornerShape(32.dp)
    val radiusPill = RoundedCornerShape(999.dp)

    // Colors
    val AccentStart = Color(0xFFF5A623) // Warm amber nostalgia
    val AccentEnd = Color(0xFFF76B1C)   // Tangerine
    val AccentSafe = Color(0xFF34C759)  // Limiter safe indicator
    val AccentWarning = Color(0xFFFF3B30) // Loudness limit warning
    val AccentCyan = Color(0xFF38EF7D)

    val AccentGradient = Brush.horizontalGradient(
        listOf(AccentStart, AccentEnd)
    )

    val AccentGradientVertical = Brush.verticalGradient(
        listOf(AccentStart, AccentEnd)
    )

    // Text Tokens
    val TextPrimary = Color(0xFFFFFFFF).copy(alpha = 0.95f)
    val TextSecondary = Color(0xFFFFFFFF).copy(alpha = 0.65f)
    val TextMuted = Color(0xFFFFFFFF).copy(alpha = 0.40f)

    // PRD Section 22.10 Exact Liquid Glass Tier Tokens
    val BaseGlassFill = Color.White.copy(alpha = 0.10f)
    val BaseGlassBorder = Color.White.copy(alpha = 0.15f)
    val BaseGlassBlur: Dp = 40.dp

    val RaisedGlassFill = Color.White.copy(alpha = 0.18f)
    val RaisedGlassBorder = Color.White.copy(alpha = 0.20f)
    val RaisedGlassBlur: Dp = 24.dp

    val FloatingGlassFill = Color.White.copy(alpha = 0.26f)
    val FloatingGlassBorder = Color.White.copy(alpha = 0.30f)
    val FloatingGlassSpecular = Color.White.copy(alpha = 0.40f)
    val FloatingGlassBlur: Dp = 16.dp

    // Solid High-Contrast Fallback (PRD 22.3 "Reduce Glass")
    val SolidCardFill = Color(0xFF161424)
    val SolidCardBorder = Color(0xFF38334E)
}

data class GlassStyleConfig(
    val reduceGlass: Boolean = false,
    val reduceMotion: Boolean = false,
    val showTechnicalValues: Boolean = false
)

val LocalGlassConfig = staticCompositionLocalOf { GlassStyleConfig() }

// Modifier Extensions for Glass Surfaces (PRD 22.2 & 22.10)
fun Modifier.baseGlass(reduceGlass: Boolean = false): Modifier {
    return if (reduceGlass) {
        this.clip(GlassTokens.radiusMd)
            .background(GlassTokens.SolidCardFill)
            .border(1.dp, GlassTokens.SolidCardBorder, GlassTokens.radiusMd)
    } else {
        this.clip(GlassTokens.radiusMd)
            .background(GlassTokens.BaseGlassFill)
            .border(1.dp, GlassTokens.BaseGlassBorder, GlassTokens.radiusMd)
    }
}

fun Modifier.raisedGlass(reduceGlass: Boolean = false): Modifier {
    return if (reduceGlass) {
        this.clip(GlassTokens.radiusMd)
            .background(GlassTokens.SolidCardFill)
            .border(1.dp, GlassTokens.SolidCardBorder, GlassTokens.radiusMd)
    } else {
        this.clip(GlassTokens.radiusMd)
            .background(GlassTokens.RaisedGlassFill)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                GlassTokens.radiusMd
            )
    }
}

fun Modifier.floatingGlass(reduceGlass: Boolean = false): Modifier {
    return if (reduceGlass) {
        this.clip(GlassTokens.radiusLg)
            .background(Color(0xFF221E31))
            .border(1.5.dp, GlassTokens.AccentStart, GlassTokens.radiusLg)
    } else {
        this.clip(GlassTokens.radiusLg)
            .background(GlassTokens.FloatingGlassFill)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.20f)
                    )
                ),
                GlassTokens.radiusLg
            )
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = GlassTokens.AccentStart,
    secondary = GlassTokens.AccentEnd,
    tertiary = GlassTokens.AccentSafe,
    background = Color(0xFF0D0B14),
    surface = Color(0xFF151320),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = GlassTokens.TextPrimary,
    onSurface = GlassTokens.TextPrimary
)

@Composable
fun DaydreamTheme(
    config: GlassStyleConfig = GlassStyleConfig(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalGlassConfig provides config) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}
