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
    val radiusMd = RoundedCornerShape(22.dp)
    val radiusLg = RoundedCornerShape(28.dp)
    val radiusPill = RoundedCornerShape(999.dp)

    // Apple Liquid Glass Spectral Palette
    val AccentStart = Color(0xFF0A84FF) // Apple Electric System Blue
    val AccentEnd = Color(0xFF64D2FF)   // Fluid Aurora Cyan
    val AccentSafe = Color(0xFF30D158)  // Apple System Spring Green
    val AccentWarning = Color(0xFFFF453A) // Apple System Coral Red
    val AccentCyan = Color(0xFF00F0FF)  // Electric Cyan
    val AccentCool = Color(0xFF5E5CE6)  // Apple System Indigo / Violet

    // Midnight Obsidian Backdrop
    val BackdropBase = Color(0xFF07090E)
    val BackdropScrim = Color(0xFF030508)

    val AccentGradient = Brush.horizontalGradient(
        listOf(AccentStart, AccentEnd)
    )

    val AccentGradientVertical = Brush.verticalGradient(
        listOf(AccentStart, AccentEnd)
    )

    // Text Tokens (Crisp high-legibility SF Pro contrast)
    val TextPrimary = Color(0xFFFFFFFF).copy(alpha = 0.95f)
    val TextSecondary = Color(0xFFD0D7E2).copy(alpha = 0.70f)
    val TextMuted = Color(0xFF8896AB).copy(alpha = 0.50f)

    // Apple Liquid Glass Material Tiers
    val BaseGlassFill = Color(0xFF141A26).copy(alpha = 0.55f)
    val BaseGlassBorder = Color.White.copy(alpha = 0.14f)
    val BaseGlassBlur: Dp = 40.dp

    val RaisedGlassFill = Color(0xFF1A2234).copy(alpha = 0.65f)
    val RaisedGlassBorder = Color.White.copy(alpha = 0.22f)
    val RaisedGlassBlur: Dp = 24.dp

    val FloatingGlassFill = Color(0xFF202B40).copy(alpha = 0.78f)
    val FloatingGlassBorder = Color.White.copy(alpha = 0.32f)
    val FloatingGlassSpecular = Color.White.copy(alpha = 0.50f)
    val FloatingGlassBlur: Dp = 16.dp

    // Solid High-Contrast Fallback (PRD 22.3 "Reduce Glass")
    val SolidCardFill = Color(0xFF101522)
    val SolidCardBorder = Color(0xFF222D40)
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
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF161E2E).copy(alpha = 0.60f),
                        Color(0xFF0F1420).copy(alpha = 0.70f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                GlassTokens.radiusMd
            )
    }
}

fun Modifier.raisedGlass(reduceGlass: Boolean = false): Modifier {
    return if (reduceGlass) {
        this.clip(GlassTokens.radiusMd)
            .background(GlassTokens.SolidCardFill)
            .border(1.dp, GlassTokens.SolidCardBorder, GlassTokens.radiusMd)
    } else {
        this.clip(GlassTokens.radiusMd)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1D263B).copy(alpha = 0.70f),
                        Color(0xFF131A28).copy(alpha = 0.80f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                GlassTokens.radiusMd
            )
    }
}

fun Modifier.floatingGlass(reduceGlass: Boolean = false): Modifier {
    return if (reduceGlass) {
        this.clip(GlassTokens.radiusLg)
            .background(Color(0xFF161C2C))
            .border(1.5.dp, GlassTokens.AccentStart, GlassTokens.radiusLg)
    } else {
        this.clip(GlassTokens.radiusLg)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF24304A).copy(alpha = 0.82f),
                        Color(0xFF172032).copy(alpha = 0.90f)
                    )
                )
            )
            .border(
                1.2.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.48f),
                        Color.White.copy(alpha = 0.12f)
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
    background = Color(0xFF07090E),
    surface = Color(0xFF121722),
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
