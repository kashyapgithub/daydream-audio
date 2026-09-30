package com.example.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// OpenDesign Apple Design System & HIG Tokens (od://design-systems/apple/)
object GlassTokens {
    // Spacing (8px Apple grid + micro intervals)
    val spaceXs: Dp = 4.dp
    val spaceSm: Dp = 8.dp
    val spaceMd: Dp = 16.dp
    val spaceLg: Dp = 20.dp
    val spaceXl: Dp = 28.dp
    val spaceXxl: Dp = 40.dp
    val screenMargin: Dp = 16.dp

    // Apple Continuous Corner Radii
    val radiusXs = RoundedCornerShape(6.dp)
    val radiusSm = RoundedCornerShape(10.dp) // Apple compact controls & inner tags
    val radiusMd = RoundedCornerShape(14.dp) // Segmented controls & badges
    val radiusLg = RoundedCornerShape(20.dp) // Apple Inset Grouped card containers
    val radiusXl = RoundedCornerShape(26.dp) // Apple Modal sheets & spotlight tiles
    val radiusPill = RoundedCornerShape(980.dp) // Apple signature capsule CTA

    // Apple Official System Palette (Dark Mode)
    val IosBlue = Color(0xFF0A84FF)       // Apple System Blue (interactive/primary)
    val IosGreen = Color(0xFF30D158)      // Apple System Green (active/safe/bypass-off)
    val IosOrange = Color(0xFFFF9F0A)     // Apple System Orange (warmth/equalizer)
    val IosRed = Color(0xFFFF453A)        // Apple System Coral Red (warning/limit)
    val IosTeal = Color(0xFF64D2FF)       // Apple System Teal / Cyan (air/detail)
    val IosIndigo = Color(0xFF5E5CE6)     // Apple System Indigo (presence/space)
    val IosPurple = Color(0xFFBF5AF2)     // Apple System Purple (ambience)
    val IosYellow = Color(0xFFFFD60A)     // Apple System Yellow (meters/warning)
    val IosPink = Color(0xFFFF375F)       // Apple System Pink

    // Backward compatibility aliases
    val AccentStart = IosBlue
    val AccentEnd = IosTeal
    val AccentSafe = IosGreen
    val AccentWarning = IosRed
    val AccentCyan = IosTeal
    val AccentCool = IosIndigo

    // Apple OLED Midnight Canvas
    val BackdropBase = Color(0xFF000000)   // True OLED Pitch Black
    val BackdropScrim = Color(0xFF070709)  // Secondary background

    // Apple Dark Mode Grouped Surface Tiers
    val IosGroupedBase = Color(0xFF000000)        // Background canvas
    val IosGroupedPrimary = Color(0xFF1C1C1E)     // System Gray 6 (Grouped container)
    val IosGroupedSecondary = Color(0xFF2C2C2E)   // System Gray 5 (Secondary row/chip)
    val IosGroupedTertiary = Color(0xFF3A3A3C)    // System Gray 4 (Track/active field)
    val IosSeparator = Color(0xFF38383A)          // Hairline separator / divider
    val IosSeparatorSubtle = Color(0xFF2C2C2E)    // Inset list divider

    // Signature Apple Gradients
    val AccentGradient = Brush.horizontalGradient(
        listOf(IosBlue, IosTeal)
    )

    val AccentGradientVertical = Brush.verticalGradient(
        listOf(IosBlue, IosTeal)
    )

    // SF Pro Text Typography Contrast Tokens
    val TextPrimary = Color(0xFFFFFFFF)                   // SF Pro Primary Label (100%)
    val TextSecondary = Color(0xFF8E8E93)                 // Apple System Gray Secondary Label
    val TextMuted = Color(0xFF636366)                     // Apple System Gray 2 Tertiary Label
    val TextQuaternary = Color(0xFF48484A)                // Apple System Gray 3 Quaternary

    // Apple Translucent Materials
    val BaseGlassFill = Color(0xFF1C1C1E).copy(alpha = 0.85f)
    val BaseGlassBorder = Color.White.copy(alpha = 0.12f)
    val BaseGlassBlur: Dp = 24.dp

    val RaisedGlassFill = Color(0xFF242426).copy(alpha = 0.90f)
    val RaisedGlassBorder = Color.White.copy(alpha = 0.16f)
    val RaisedGlassBlur: Dp = 20.dp

    val FloatingGlassFill = Color(0xFF1C1C1E).copy(alpha = 0.92f)
    val FloatingGlassBorder = Color.White.copy(alpha = 0.20f)
    val FloatingGlassSpecular = Color.White.copy(alpha = 0.40f)
    val FloatingGlassBlur: Dp = 16.dp

    // Solid High-Contrast Fallback (PRD 22.3 "Reduce Glass")
    val SolidCardFill = Color(0xFF1C1C1E)
    val SolidCardBorder = Color(0xFF38383A)
}

data class GlassStyleConfig(
    val reduceGlass: Boolean = false,
    val reduceMotion: Boolean = false,
    val showTechnicalValues: Boolean = false
)

val LocalGlassConfig = staticCompositionLocalOf { GlassStyleConfig() }

// Modifier Extensions for Apple iOS Surfaces (Liquid Glass Material Spec)
fun Modifier.baseGlass(reduceGlass: Boolean = false): Modifier {
    return if (reduceGlass) {
        this.clip(GlassTokens.radiusLg)
            .background(GlassTokens.SolidCardFill)
            .border(1.dp, GlassTokens.SolidCardBorder, GlassTokens.radiusLg)
    } else {
        this.clip(GlassTokens.radiusLg)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF262A36).copy(alpha = 0.88f),
                        Color(0xFF171922).copy(alpha = 0.93f),
                        Color(0xFF0F1016).copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.18f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(300f, 300f)
                ),
                GlassTokens.radiusLg
            )
    }
}

fun Modifier.raisedGlass(reduceGlass: Boolean = false): Modifier {
    return if (reduceGlass) {
        this.clip(GlassTokens.radiusLg)
            .background(GlassTokens.SolidCardFill)
            .border(1.dp, GlassTokens.SolidCardBorder, GlassTokens.radiusLg)
    } else {
        this.clip(GlassTokens.radiusLg)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2D3242).copy(alpha = 0.89f),
                        Color(0xFF1E212D).copy(alpha = 0.93f),
                        Color(0xFF13151D).copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.38f),
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.22f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(350f, 350f)
                ),
                GlassTokens.radiusLg
            )
    }
}

fun Modifier.floatingGlass(reduceGlass: Boolean = false): Modifier {
    return if (reduceGlass) {
        this.clip(GlassTokens.radiusXl)
            .background(GlassTokens.SolidCardFill)
            .border(1.dp, GlassTokens.IosSeparator, GlassTokens.radiusXl)
    } else {
        this.clip(GlassTokens.radiusXl)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF32384A).copy(alpha = 0.90f),
                        Color(0xFF222634).copy(alpha = 0.94f),
                        Color(0xFF14161F).copy(alpha = 0.97f)
                    )
                )
            )
            .border(
                1.2.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.28f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(400f, 400f)
                ),
                GlassTokens.radiusXl
            )
    }
}

// Authentic Apple iOS Inset Grouped Container Modifier (Liquid Glass Material)
fun Modifier.iosInsetGroupedCard(reduceGlass: Boolean = false): Modifier {
    return if (reduceGlass) {
        this.clip(GlassTokens.radiusLg)
            .background(GlassTokens.IosGroupedPrimary)
            .border(1.dp, GlassTokens.IosSeparator, GlassTokens.radiusLg)
    } else {
        this.clip(GlassTokens.radiusLg)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF262A38).copy(alpha = 0.86f),
                        Color(0xFF191B24).copy(alpha = 0.92f),
                        Color(0xFF111218).copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                0.9.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.16f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(350f, 350f)
                ),
                GlassTokens.radiusLg
            )
    }
}

// Apple iOS Spring Press Feedback for buttons, chips, and cards
fun Modifier.iosPressable(
    enabled: Boolean = true,
    pressScale: Float = 0.965f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ios_press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
}

fun Modifier.iosScaleOnPress(
    interactionSource: MutableInteractionSource,
    pressScale: Float = 0.965f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ios_scale_press"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = GlassTokens.IosBlue,
    secondary = GlassTokens.IosTeal,
    tertiary = GlassTokens.IosGreen,
    background = GlassTokens.BackdropBase,
    surface = GlassTokens.IosGroupedPrimary,
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
