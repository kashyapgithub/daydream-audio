package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DemoTrack
import com.example.model.PlainBand
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.floatingGlass
import com.example.ui.theme.raisedGlass

/**
 * Apple Ambient Audio Backdrop (OpenDesign Apple Design System & HIG)
 * Renders an organic, deeply immersive dark backdrop with gentle audio-reactive chromatic radiance.
 */
@Composable
fun SonicGlassBackground(
    audioRms: Float,
    spectrum: FloatArray? = null,
    reduceGlass: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedRms by animateFloatAsState(
        targetValue = if (reduceMotion) 0f else audioRms.coerceIn(0f, 0.40f),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "apple_rms_glow"
    )

    val trebleEnergy = if (spectrum != null && spectrum.size >= 8) {
        (spectrum[5] + spectrum[6] + spectrum[7]) / 3f
    } else 0.08f

    val bassEnergy = if (spectrum != null && spectrum.size >= 8) {
        (spectrum[0] + spectrum[1] + spectrum[2]) / 3f
    } else 0.08f

    val infiniteTransition = rememberInfiniteTransition(label = "idle_mesh_drift")
    val idleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_drift"
    )
    val idleShift = if (reduceMotion) 0f else (idleDrift - 0.5f) * 2f

    Box(modifier = modifier.fillMaxSize().background(GlassTokens.BackdropBase)) {
        if (!reduceGlass) {
            // Ambient photo backdrop with smooth Apple Gaussian blur
            Image(
                painter = painterResource(id = R.drawable.bg_ambient_glass),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp)
            )

            // Apple Dark Mode Obsidian scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF000000).copy(alpha = 0.82f),
                                Color(0xFF050508).copy(alpha = 0.90f),
                                Color(0xFF000000).copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            // Subtle Apple Chromatic Light Diffusion (Deep Indigo + Teal + Amber)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Pole A: Apple System Indigo / Violet radiance (Top Left)
                val poleARadius = (size.width * 0.52f) + (animatedRms * 190f) + (bassEnergy * 80f) + (idleShift * 20f)
                val poleAAlpha = (0.11f + animatedRms * 0.18f).coerceIn(0.06f, 0.28f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassTokens.IosIndigo.copy(alpha = poleAAlpha),
                            GlassTokens.IosBlue.copy(alpha = poleAAlpha * 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.22f + (idleShift * 25f), size.height * 0.15f + (idleShift * 18f)),
                        radius = poleARadius
                    ),
                    radius = poleARadius,
                    center = Offset(size.width * 0.22f + (idleShift * 25f), size.height * 0.15f + (idleShift * 18f))
                )

                // Pole B: Apple System Teal / Cyan glow (Center Right)
                val poleBRadius = (size.width * 0.56f) + (animatedRms * 210f) + (trebleEnergy * 90f) - (idleShift * 18f)
                val poleBAlpha = (0.09f + animatedRms * 0.16f).coerceIn(0.05f, 0.24f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassTokens.IosTeal.copy(alpha = poleBAlpha),
                            GlassTokens.IosBlue.copy(alpha = poleBAlpha * 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.80f - (idleShift * 20f), size.height * 0.65f - (idleShift * 25f)),
                        radius = poleBRadius
                    ),
                    radius = poleBRadius,
                    center = Offset(size.width * 0.80f - (idleShift * 20f), size.height * 0.65f - (idleShift * 25f))
                )

                // Pole C: Subtle warm amber core (Sub-bass anchor)
                val poleCRadius = (size.width * 0.38f) + (bassEnergy * 110f) + (idleShift * 15f)
                val poleCAlpha = (0.05f + bassEnergy * 0.08f).coerceIn(0.02f, 0.14f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassTokens.IosOrange.copy(alpha = poleCAlpha),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.45f + (idleShift * 15f), size.height * 0.40f),
                        radius = poleCRadius
                    ),
                    radius = poleCRadius,
                    center = Offset(size.width * 0.45f + (idleShift * 15f), size.height * 0.40f)
                )
            }
        }

        // Overlay scrim for high-contrast legibility (PRD 22.3)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000).copy(alpha = if (reduceGlass) 0.98f else 0.25f))
        )

        content()
    }
}

/**
 * Apple Control Center / Music Slider (OpenDesign Apple Design System & HIG)
 * Recessed track, pure white thumb with physical iOS elevation shadow, tabular figures, and SF Pro typography.
 */
@Composable
fun LiquidSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String = "",
    technicalValue: String? = null,
    showTechnical: Boolean = false,
    tipDescription: String? = null,
    onInfoClick: (() -> Unit)? = null,
    accentColor: Color = GlassTokens.IosBlue,
    isWarning: Boolean = false,
    warningText: String? = null,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            @OptIn(ExperimentalFoundationApi::class)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onInfoClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onInfoClick() },
                        onLongClick = { onInfoClick() }
                    )
                } else Modifier
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTokens.TextPrimary
                )
                if (onInfoClick != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info on $title",
                            tint = GlassTokens.TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Value readout (SF Pro Tabular Figures)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showTechnical && technicalValue != null) {
                    Text(
                        text = technicalValue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                val formattedDisplayValue = when {
                    unit == "x" -> String.format(java.util.Locale.US, "%.2fx", value)
                    unit == ":1" -> String.format(java.util.Locale.US, "%.1f:1", value)
                    unit == "Q" -> String.format(java.util.Locale.US, "%.1f", value)
                    unit == "dB" -> {
                        if (value % 1f == 0f) {
                            if (value > 0) "+${value.toInt()}dB" else "${value.toInt()}dB"
                        } else {
                            if (value > 0) String.format(java.util.Locale.US, "+%.1fdB", value)
                            else String.format(java.util.Locale.US, "%.1fdB", value)
                        }
                    }
                    value % 1f != 0f && (valueRange.endInclusive - valueRange.start <= 20f) -> {
                        String.format(java.util.Locale.US, "%.1f%s", value, unit)
                    }
                    else -> "${value.toInt()}$unit"
                }
                Text(
                    text = formattedDisplayValue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarning) GlassTokens.IosRed else GlassTokens.IosBlue
                )
            }
        }

        if (isWarning && warningText != null) {
            Text(
                text = warningText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = GlassTokens.IosRed,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        val interactionSource = remember { MutableInteractionSource() }
        val isDragged by interactionSource.collectIsDraggedAsState()
        val thumbSize by animateDpAsState(
            targetValue = if (isDragged) 32.dp else 26.dp,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
            label = "apple_thumb_size"
        )

        @OptIn(ExperimentalMaterial3Api::class)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            interactionSource = interactionSource,
            thumb = {
                @OptIn(ExperimentalFoundationApi::class)
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .then(
                            if (onInfoClick != null) {
                                Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = { onInfoClick() }
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val thumbRadius = size.width * 0.44f

                        // Active dragging glow halo (Apple Control Center tactile expansion)
                        if (isDragged) {
                            val haloColor = if (isWarning) GlassTokens.IosRed else accentColor
                            drawCircle(
                                color = haloColor.copy(alpha = 0.28f),
                                radius = thumbRadius + 5.dp.toPx(),
                                center = center
                            )
                        }

                        // Physical iOS drop shadow (0 2px 6px rgba(0,0,0,0.38))
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.38f),
                            radius = thumbRadius,
                            center = Offset(center.x, center.y + 1.8.dp.toPx())
                        )
                        // Apple pure white thumb base
                        drawCircle(
                            color = Color(0xFFFFFFFF),
                            radius = thumbRadius,
                            center = center
                        )
                        // Specular hairline rim
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.08f),
                            radius = thumbRadius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx())
                        )

                        // Subtle active accent indicator dot on drag or warning
                        if (isDragged || isWarning) {
                            val dotColor = if (isWarning) GlassTokens.IosRed else accentColor
                            drawCircle(
                                color = dotColor,
                                radius = 2.8.dp.toPx(),
                                center = center
                            )
                        }
                    }
                }
            },
            track = { sliderState ->
                val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
                val fraction = if (range > 0f) {
                    ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
                } else 0f

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(GlassTokens.radiusPill)
                ) {
                    // Apple recessed background track (System Gray 5 / dark subtle)
                    drawRoundRect(
                        color = Color(0xFF2C2C2E),
                        cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                    )
                    // Inner hairline top edge for machined depth
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.06f),
                        cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.6.dp.toPx())
                    )

                    val activeWidth = size.width * fraction
                    if (activeWidth > 0f) {
                        val activeColor = if (isWarning) GlassTokens.IosRed else GlassTokens.IosBlue
                        // Apple Solid / Subtle Vibrant Progress Bar
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    activeColor,
                                    if (isWarning) GlassTokens.IosRed else GlassTokens.IosTeal
                                ),
                                startX = 0f,
                                endX = size.width
                            ),
                            size = Size(activeWidth, size.height),
                            cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_${title.lowercase().replace(" ", "_")}")
        )
    }
}

/**
 * A/B Compare Floating Bar (PRD FR-3: <50ms instant comparison)
 */
@Composable
fun ABCompareBar(
    isBypassed: Boolean,
    onToggle: () -> Unit,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .floatingGlass(reduceGlass)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag("ab_compare_button")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Apple Control Center Circular Accessory Pill
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBypassed) Color(0xFF2C2C2E)
                            else GlassTokens.IosBlue.copy(alpha = 0.22f)
                        )
                        .border(
                            1.dp,
                            if (isBypassed) Color.White.copy(alpha = 0.12f)
                            else GlassTokens.IosBlue.copy(alpha = 0.65f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "A/B Compare",
                        tint = if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosBlue,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isBypassed) "Original Raw Sound (Bypass)" else "Daydream Restored Audio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isBypassed) GlassTokens.TextPrimary else GlassTokens.IosBlue
                    )
                    Text(
                        text = if (isBypassed) "Bypass active — tap for instant <50ms restoration" else "Tap to instantly audit unprocessed raw input",
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary
                    )
                }
            }

            // Apple iOS Status Capsule
            Box(
                modifier = Modifier
                    .clip(GlassTokens.radiusPill)
                    .background(
                        if (isBypassed) Color.White.copy(alpha = 0.08f)
                        else GlassTokens.IosGreen.copy(alpha = 0.18f)
                    )
                    .border(
                        1.dp,
                        if (isBypassed) Color.White.copy(alpha = 0.16f)
                        else GlassTokens.IosGreen.copy(alpha = 0.60f),
                        GlassTokens.radiusPill
                    )
                    .padding(horizontal = 11.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isBypassed) "RAW" else "ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosGreen
                )
            }
        }
    }
}

/**
 * Apple Music MiniPlayer (OpenDesign Apple Design System & HIG)
 * 58dp floating frosted capsule, squircle artwork, SF Pro typography, and live mini EQ.
 */
@Composable
fun NowPlayingGlassBar(
    track: DemoTrack?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onNextTrack: () -> Unit,
    spectrum: FloatArray,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusLg)
            .background(
                if (reduceGlass) Color(0xFF1C1C1E)
                else Color(0xFF1C1C1E).copy(alpha = 0.94f)
            )
            .border(
                0.8.dp,
                Color.White.copy(alpha = 0.16f),
                GlassTokens.radiusLg
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Info with Apple Squircle Album Art
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(GlassTokens.radiusSm)
                        .background(Color(0xFF2C2C2E))
                        .border(0.6.dp, Color.White.copy(alpha = 0.20f), GlassTokens.radiusSm),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_daydream_logo),
                        contentDescription = "Track Art",
                        modifier = Modifier.size(38.dp).clip(GlassTokens.radiusSm)
                    )
                }

                Spacer(modifier = Modifier.width(11.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: "No Track Selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track?.era ?: "Lossless DSP Engine",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = GlassTokens.IosTeal,
                        maxLines = 1
                    )
                }
            }

            // Live 6-Band Apple Music Equalizer
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .height(24.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                spectrum.take(6).forEach { level ->
                    val barHeight = (level * 22f).coerceIn(3.5f, 22f)
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight.dp)
                            .clip(GlassTokens.radiusPill)
                            .background(
                                if (isPlaying) SolidColor(GlassTokens.IosBlue)
                                else SolidColor(Color.White.copy(alpha = 0.18f))
                            )
                    )
                }
            }

            // Playback controls (Apple SF style)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(36.dp).testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = GlassTokens.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = onNextTrack,
                    modifier = Modifier.size(34.dp).testTag("next_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = GlassTokens.TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Apple Music MiniPlayer hairline scrub progress line
        val infiniteTransition = rememberInfiniteTransition(label = "miniplayer_scrub")
        val scrubProgress by infiniteTransition.animateFloat(
            initialValue = 0.08f,
            targetValue = 0.94f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 24000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scrub_progress"
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(if (isPlaying) scrubProgress else 0.35f)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(GlassTokens.IosBlue, GlassTokens.IosTeal)
                    )
                )
        )
    }
}

/**
 * Apple iOS Modal Sheet Tooltip (OpenDesign Apple Design System & HIG)
 * Features an authentic drag grabber, inset grouped content, and Apple Blue Done action.
 */
@Composable
fun BandTooltipDialog(
    band: PlainBand,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Apple Modal Sheet Drag Indicator Grabber
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(GlassTokens.radiusPill)
                        .background(Color(0xFF5A5A5E))
                        .padding(bottom = 12.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = band.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                        Text(
                            text = band.frequencyRange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = GlassTokens.IosTeal
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GlassTokens.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = band.plainDescription,
                    fontSize = 14.sp,
                    color = GlassTokens.TextPrimary,
                    lineHeight = 20.sp
                )
                // Apple Inset Grouped Callout Row 1: When to use
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusSm)
                        .background(GlassTokens.IosGreen.copy(alpha = 0.12f))
                        .border(0.8.dp, GlassTokens.IosGreen.copy(alpha = 0.35f), GlassTokens.radiusSm)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💡 When to use: ${band.fixTip}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassTokens.IosGreen
                    )
                }
                // Apple Inset Grouped Callout Row 2: Caution
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusSm)
                        .background(GlassTokens.IosRed.copy(alpha = 0.12f))
                        .border(0.8.dp, GlassTokens.IosRed.copy(alpha = 0.35f), GlassTokens.radiusSm)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "⚠️ Watch out: ${band.excessiveWarning}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassTokens.IosRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                shape = GlassTokens.radiusPill,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        containerColor = Color(0xFF1C1C1E),
        shape = GlassTokens.radiusXl
    )
}

/**
 * Apple Music Lossless EQ Spectrum Visualizer
 * Real 8-band bandpass analysis rendered as Apple rounded pill frequency bars.
 */
@Composable
fun HomeSpectrumVisualizer(
    spectrum: FloatArray,
    isPlaying: Boolean,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusLg)
            .background(GlassTokens.IosGroupedPrimary)
            .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusLg)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) GlassTokens.IosGreen else GlassTokens.TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Real-Time Spectrum",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary
                    )
                }
                Text(
                    text = if (isPlaying) "31Hz — 16kHz" else "Playback Paused",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = GlassTokens.TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                // Background Cubic Spline Neon Glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (spectrum.size >= 2) {
                        val bandCount = spectrum.size
                        val stepX = size.width / bandCount
                        val points = mutableListOf<Offset>()
                        for (i in 0 until bandCount) {
                            val lvl = if (isPlaying) spectrum[i].coerceIn(0.05f, 1f) else 0.05f
                            val px = stepX * i + (stepX / 2f)
                            val py = size.height * (1f - (lvl * 0.88f))
                            points.add(Offset(px, py))
                        }

                        val splinePath = Path()
                        val fillPath = Path()
                        splinePath.moveTo(points.first().x, points.first().y)
                        fillPath.moveTo(points.first().x, size.height)
                        fillPath.lineTo(points.first().x, points.first().y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlX1 = (p0.x + p1.x) / 2f
                            val controlY1 = p0.y
                            val controlX2 = (p0.x + p1.x) / 2f
                            val controlY2 = p1.y
                            splinePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                        }
                        fillPath.lineTo(points.last().x, size.height)
                        fillPath.close()

                        // Gradient fill beneath curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    GlassTokens.IosTeal.copy(alpha = if (isPlaying) 0.16f else 0.03f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Top spline glow outline
                        drawPath(
                            path = splinePath,
                            brush = Brush.horizontalGradient(
                                listOf(GlassTokens.IosOrange, GlassTokens.IosTeal, GlassTokens.IosBlue)
                            ),
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                }

                // Foreground 8 Pill Bars
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val bandCount = spectrum.size.coerceAtLeast(1)
                    for (index in spectrum.indices) {
                        val rawLevel = spectrum[index]
                        val animatedLevel by animateFloatAsState(
                            targetValue = if (isPlaying) rawLevel.coerceIn(0.04f, 1f) else 0.04f,
                            animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
                            label = "apple_spectrum_bar_$index"
                        )
                        // Apple Spectral gradient: Warm Amber (Bass) to Electric Teal (Treble)
                        val fraction = index.toFloat() / (bandCount - 1).coerceAtLeast(1)
                        val barColor = androidx.compose.ui.graphics.lerp(
                            GlassTokens.IosOrange,
                            GlassTokens.IosTeal,
                            fraction
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(animatedLevel)
                                .clip(GlassTokens.radiusPill)
                                .background(
                                    if (isPlaying) barColor.copy(alpha = 0.85f)
                                    else Color.White.copy(alpha = 0.10f)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * AirPods Pro Spatial Audio Soundstage Arc Visualizer
 * Renders an acoustic top-down head glyph, stereo speaker coordinates, and expanding soundstage field arc.
 */
@Composable
fun SpatialStageVisualizer(
    spacePercent: Float,
    hrtfProfile: String,
    isBypassed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedSpace by animateFloatAsState(
        targetValue = if (isBypassed) 15f else spacePercent.coerceIn(0f, 100f),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "stage_width_anim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161618))
            .border(0.6.dp, GlassTokens.IosSeparator, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosIndigo)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Spatial Soundstage Field",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary
                    )
                }
                Text(
                    text = if (isBypassed) "Direct Stereo (Bypassed)" else "Arc: ${(30 + animatedSpace * 0.9f).toInt()}° • $hrtfProfile",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosTeal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val cx = size.width / 2f
                val cy = size.height * 0.78f

                // Outer acoustic boundary rings
                for (r in listOf(0.35f, 0.65f, 0.95f)) {
                    val ringRadius = size.height * r
                    drawCircle(
                        color = Color.White.copy(alpha = 0.04f),
                        radius = ringRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }

                // Soundstage Arc Sweep Angle (from 35 degrees up to 135 degrees)
                val sweepAngle = 35f + (animatedSpace * 0.95f)
                val startAngle = 270f - (sweepAngle / 2f)
                val arcRadius = size.height * 0.70f

                // Luminous acoustic sound field fan
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isBypassed) Color.White.copy(alpha = 0.05f)
                            else GlassTokens.IosIndigo.copy(alpha = 0.22f),
                            if (isBypassed) Color.Transparent
                            else GlassTokens.IosTeal.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = arcRadius * 1.1f
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(cx - arcRadius, cy - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2)
                )

                // Stereo Arc outline stroke
                drawArc(
                    brush = Brush.horizontalGradient(
                        listOf(GlassTokens.IosTeal, GlassTokens.IosIndigo, GlassTokens.IosBlue)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - arcRadius, cy - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Left & Right virtual speaker nodes
                val leftRad = Math.toRadians(startAngle.toDouble())
                val rightRad = Math.toRadians((startAngle + sweepAngle).toDouble())

                val lx = (cx + arcRadius * Math.cos(leftRad)).toFloat()
                val ly = (cy + arcRadius * Math.sin(leftRad)).toFloat()
                val rx = (cx + arcRadius * Math.cos(rightRad)).toFloat()
                val ry = (cy + arcRadius * Math.sin(rightRad)).toFloat()

                // Left speaker glow & node
                drawCircle(color = GlassTokens.IosTeal.copy(alpha = 0.35f), radius = 6.dp.toPx(), center = Offset(lx, ly))
                drawCircle(color = Color.White, radius = 2.8.dp.toPx(), center = Offset(lx, ly))

                // Right speaker glow & node
                drawCircle(color = GlassTokens.IosIndigo.copy(alpha = 0.35f), radius = 6.dp.toPx(), center = Offset(rx, ry))
                drawCircle(color = Color.White, radius = 2.8.dp.toPx(), center = Offset(rx, ry))

                // Center Listener Head Glyph (Cupertino style)
                drawCircle(color = Color(0xFF2C2C2E), radius = 9.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = Color.White.copy(alpha = 0.15f), radius = 9.dp.toPx(), center = Offset(cx, cy), style = Stroke(0.8.dp.toPx()))
                // Ear indicators
                drawRoundRect(
                    color = if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosBlue,
                    topLeft = Offset(cx - 11.5.dp.toPx(), cy - 4.dp.toPx()),
                    size = Size(2.5.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
                drawRoundRect(
                    color = if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosBlue,
                    topLeft = Offset(cx + 9.dp.toPx(), cy - 4.dp.toPx()),
                    size = Size(2.5.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
        }
    }
}

/**
 * Professional Studio LED Gain Reduction Meter
 * High-precision ladder display with dynamic dB thresholds (-1dB to -20dB)
 */
@Composable
fun LedCompressionMeter(
    thresholdDb: Float,
    ratio: Float,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val inputDb = (20f * kotlin.math.log10(audioRms.coerceAtLeast(0.001f))).coerceIn(-60f, 0f)
    val overThreshold = (inputDb - thresholdDb).coerceAtLeast(0f)
    val estimatedGrDb = if (overThreshold > 0f) overThreshold * (1f - (1f / ratio)) else 0f

    val animatedGr by animateFloatAsState(
        targetValue = estimatedGrDb,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "compressor_gr_anim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141416))
            .border(0.6.dp, GlassTokens.IosSeparator, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GAIN REDUCTION (GR)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (animatedGr > 0.3f) "-${String.format(java.util.Locale.US, "%.1f", animatedGr)} dB" else "0.0 dB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (animatedGr > 6f) GlassTokens.IosRed
                            else if (animatedGr > 2f) GlassTokens.IosOrange
                            else GlassTokens.IosGreen
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // 10-segment LED ladder
            Row(
                modifier = Modifier.fillMaxWidth().height(9.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val thresholds = listOf(1f, 2f, 3f, 4f, 6f, 8f, 10f, 12f, 16f, 20f)
                for (th in thresholds) {
                    val isActive = animatedGr >= th
                    val segmentColor = when {
                        th >= 12f -> GlassTokens.IosRed
                        th >= 6f -> GlassTokens.IosOrange
                        else -> GlassTokens.IosGreen
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isActive) segmentColor
                                else segmentColor.copy(alpha = 0.12f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Apple iOS Segmented Control (OpenDesign Apple Design System & HIG)
 * Recessed container with sliding rounded thumb, clean typography, and tactile feel.
 */
@Composable
fun <T> IosSegmentedControl(
    items: List<T>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(GlassTokens.radiusMd)
            .background(Color(0xFF161618))
            .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusMd)
            .padding(2.dp)
    ) {
        val tabWidth = maxWidth / items.size
        val animatedOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f),
            label = "ios_seg_offset"
        )
        // Sliding active white/tinted pill
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(GlassTokens.radiusSm)
                .background(Color(0xFF2C2C2E))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), GlassTokens.radiusSm)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(GlassTokens.radiusSm)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(item),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) GlassTokens.TextPrimary else GlassTokens.TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Apple Inset Grouped Section Header (OpenDesign Apple Design System & HIG)
 * Uppercase small caption header with optional leading icon.
 */
@Composable
fun IosSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GlassTokens.IosBlue,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.TextSecondary,
                letterSpacing = 0.6.sp
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = GlassTokens.TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Apple Inset Grouped Hairline Divider (OpenDesign Apple Design System & HIG)
 */
@Composable
fun IosRowSeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.6.dp)
            .background(GlassTokens.IosSeparator)
    )
}

/**
 * Cupertino Analog Cassette Deck
 * Displays dual rotating reels, magnetic tape path, and warm tape-bias VU level.
 */
@Composable
fun AnalogCassetteDeck(
    isPlaying: Boolean,
    audioRms: Float = 0.05f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cassette_spool_transition")
    val spoolAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cassette_spool_angle"
    )

    val currentSpoolAngle = if (isPlaying) spoolAngle else 0f
    val rmsLevel = (audioRms.coerceIn(0.01f, 0.9f) * 10f).coerceIn(1f, 8f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161618))
            .border(0.8.dp, Color(0xFF2C2C2E), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header Bar: Vintage Cassette Branding & Play Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color(0xFFFF453A) else Color(0xFF48484A))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DAYDREAM C-90 • HIGH BIAS 70µs",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4A373),
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = if (isPlaying) "4.75 cm/s" else "STOPPED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlaying) GlassTokens.IosGreen else GlassTokens.TextMuted,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cassette Window with Rotating Dual Spools
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D0D0E))
                    .border(0.6.dp, Color(0xFF252528), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f

                    val leftSpoolX = w * 0.28f
                    val rightSpoolX = w * 0.72f
                    val spoolRadius = 26.dp.toPx()
                    val hubRadius = 13.dp.toPx()
                    val centerHoleRadius = 5.dp.toPx()

                    // Magnetic tape ribbon background
                    drawRect(
                        color = Color(0xFF261912),
                        topLeft = Offset(leftSpoolX - spoolRadius * 0.4f, centerY - 2.5.dp.toPx()),
                        size = Size(rightSpoolX - leftSpoolX + spoolRadius * 0.8f, 5.dp.toPx())
                    )

                    // Draw spools function
                    fun drawSpool(centerX: Float, rotationDeg: Float, tapePackRadius: Float) {
                        // Outer tape pack
                        drawCircle(
                            color = Color(0xFF332014),
                            radius = tapePackRadius,
                            center = Offset(centerX, centerY)
                        )

                        // Plastic spool hub
                        drawCircle(
                            color = Color(0xFFE5E5EA),
                            radius = hubRadius,
                            center = Offset(centerX, centerY)
                        )

                        // 6 teeth / spokes
                        for (i in 0 until 6) {
                            val angleRad = Math.toRadians((rotationDeg + i * 60.0)).toFloat()
                            val cosA = kotlin.math.cos(angleRad)
                            val sinA = kotlin.math.sin(angleRad)
                            val spokeStart = Offset(centerX + cosA * (centerHoleRadius + 1f), centerY + sinA * (centerHoleRadius + 1f))
                            val spokeEnd = Offset(centerX + cosA * (hubRadius - 1.5f), centerY + sinA * (hubRadius - 1.5f))
                            drawLine(
                                color = Color(0xFF1C1C1E),
                                start = spokeStart,
                                end = spokeEnd,
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // Center spindle hole
                        drawCircle(
                            color = Color(0xFF0D0D0E),
                            radius = centerHoleRadius,
                            center = Offset(centerX, centerY)
                        )
                    }

                    // Left supply spool (unwinding)
                    drawSpool(leftSpoolX, currentSpoolAngle, spoolRadius * 0.95f)

                    // Right takeup spool (winding)
                    drawSpool(rightSpoolX, currentSpoolAngle * 1.05f, spoolRadius * 0.75f)

                    // Tape guide scale line in center
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(w * 0.46f, centerY - 14.dp.toPx()),
                        end = Offset(w * 0.46f, centerY + 14.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(w * 0.50f, centerY - 18.dp.toPx()),
                        end = Offset(w * 0.50f, centerY + 18.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(w * 0.54f, centerY - 14.dp.toPx()),
                        end = Offset(w * 0.54f, centerY + 14.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Analog VU Warm Amber Level Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TAPE SATURATION VU",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextMuted,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..8) {
                        val active = isPlaying && (i <= rmsLevel)
                        val barColor = when {
                            i >= 7 -> Color(0xFFFF453A)
                            i >= 5 -> Color(0xFFFF9F0A)
                            else -> Color(0xFF30D158)
                        }
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (active) barColor else barColor.copy(alpha = 0.12f))
                        )
                    }
                }
            }
        }
    }
}


