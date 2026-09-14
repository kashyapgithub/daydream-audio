package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
 * Sonic Glass dynamic backdrop (PRD 22.7)
 * Renders the ambient background with subtle real-time audio-reactive glow ripples.
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
        targetValue = if (reduceMotion) 0f else audioRms.coerceIn(0f, 0.45f),
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "rms_glow"
    )

    val trebleEnergy = if (spectrum != null && spectrum.size >= 8) {
        (spectrum[5] + spectrum[6] + spectrum[7]) / 3f
    } else 0.1f

    val bassEnergy = if (spectrum != null && spectrum.size >= 8) {
        (spectrum[0] + spectrum[1] + spectrum[2]) / 3f
    } else 0.1f

    Box(modifier = modifier.fillMaxSize().background(GlassTokens.BackdropBase)) {
        if (!reduceGlass) {
            // Ambient photo backdrop with optical blur
            Image(
                painter = painterResource(id = R.drawable.bg_ambient_glass),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(36.dp)
            )

            // Deep Midnight obsidian glass filter layer over image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF07090E).copy(alpha = 0.85f),
                                Color(0xFF030508).copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // Fluid Apple Aurora light fields (PRD 22.7 & 22.12)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Pole A: Electric Cyan Aurora (Top-Left)
                val poleARadius = (size.width * 0.55f) + (animatedRms * 280f) + (bassEnergy * 120f)
                val poleAAlpha = (0.20f + animatedRms * 0.35f).coerceIn(0.12f, 0.50f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassTokens.AccentEnd.copy(alpha = poleAAlpha),
                            GlassTokens.AccentStart.copy(alpha = poleAAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.20f, size.height * 0.18f),
                        radius = poleARadius
                    ),
                    radius = poleARadius,
                    center = Offset(size.width * 0.20f, size.height * 0.18f)
                )

                // Pole B: Neon Violet / Indigo Aurora (Bottom-Right)
                val poleBRadius = (size.width * 0.65f) + (animatedRms * 320f) + (trebleEnergy * 150f)
                val poleBAlpha = (0.16f + animatedRms * 0.28f).coerceIn(0.10f, 0.42f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassTokens.AccentCool.copy(alpha = poleBAlpha),
                            GlassTokens.AccentStart.copy(alpha = poleBAlpha * 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.72f),
                        radius = poleBRadius
                    ),
                    radius = poleBRadius,
                    center = Offset(size.width * 0.85f, size.height * 0.72f)
                )

                // Center Dynamic Fluid Lens Ripple
                val center = Offset(size.width / 2f, size.height * 0.42f)
                val rippleRadius = (size.width * 0.40f) + (animatedRms * 360f)
                val rippleAlpha = (0.08f + animatedRms * 0.22f).coerceIn(0.04f, 0.28f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassTokens.AccentCyan.copy(alpha = rippleAlpha),
                            GlassTokens.AccentCool.copy(alpha = rippleAlpha * 0.4f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = rippleRadius
                    ),
                    radius = rippleRadius,
                    center = center
                )
            }
        }

        // Overlay scrim for high-contrast legibility (PRD 22.3)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GlassTokens.BackdropScrim.copy(alpha = if (reduceGlass) 0.98f else 0.35f))
        )

        content()
    }
}

/**
 * Liquid Slider component (PRD 22.4 & 22.11)
 * Glassmorphic slider with plain-English labeling, long-press tooltip trigger, and technical values.
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
    accentColor: Color = GlassTokens.AccentStart,
    isWarning: Boolean = false,
    warningText: String? = null,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
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
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info on $title (tap or long-press)",
                        tint = GlassTokens.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Value readout (Plain or Technical)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showTechnical && technicalValue != null) {
                    Text(
                        text = technicalValue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(end = 6.dp)
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
                    color = if (isWarning) GlassTokens.AccentWarning else GlassTokens.AccentEnd
                )
            }
        }

        if (isWarning && warningText != null) {
            Text(
                text = warningText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GlassTokens.AccentWarning,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        val interactionSource = remember { MutableInteractionSource() }
        val isDragged by interactionSource.collectIsDraggedAsState()
        val thumbSize by animateDpAsState(
            targetValue = if (isDragged) 34.dp else 28.dp,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
            label = "liquid_thumb_size"
        )
        val thumbScaleY by animateFloatAsState(
            targetValue = if (isDragged) 0.90f else 1.0f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
            label = "liquid_thumb_squash"
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
                        .graphicsLayer(scaleY = thumbScaleY)
                        .clip(CircleShape)
                        .then(
                            if (onInfoClick != null) {
                                Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = { onInfoClick() }
                                )
                            } else Modifier
                        )
                        .background(
                            if (reduceGlass) SolidColor(GlassTokens.SolidCardFill)
                            else Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF243048),
                                    Color(0xFF121927)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            if (isWarning) SolidColor(GlassTokens.AccentWarning)
                            else Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.20f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Glass pearl 3D specular lens highlight
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.65f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.35f, size.height * 0.30f),
                                radius = size.width * 0.42f
                            ),
                            radius = size.width * 0.38f,
                            center = Offset(size.width * 0.35f, size.height * 0.30f)
                        )
                    }

                    // Fluid jewel center core
                    Box(
                        modifier = Modifier
                            .size(if (isDragged) 12.dp else 10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isWarning) SolidColor(GlassTokens.AccentWarning)
                                else GlassTokens.AccentGradient
                            )
                            .border(0.5.dp, Color.White.copy(alpha = 0.80f), CircleShape)
                    )
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
                        .height(8.dp)
                        .clip(GlassTokens.radiusPill)
                ) {
                    // Apple-style sunken glass trench
                    drawRoundRect(
                        color = Color(0xFF0B101A).copy(alpha = 0.85f),
                        cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )

                    val activeWidth = size.width * fraction
                    if (activeWidth > 0f) {
                        val gradientColors = if (isWarning) {
                            listOf(GlassTokens.AccentWarning, GlassTokens.AccentWarning)
                        } else {
                            listOf(
                                GlassTokens.AccentStart,
                                GlassTokens.AccentEnd
                            )
                        }
                        // Fluid liquid progress capsule
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = gradientColors,
                                startX = 0f,
                                endX = size.width
                            ),
                            size = Size(activeWidth, size.height),
                            cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                        )
                        // Specular gloss surface sheen
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.42f),
                                    Color.Transparent
                                )
                            ),
                            size = Size(activeWidth, size.height * 0.48f),
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("ab_compare_button")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBypassed) SolidColor(Color.White.copy(alpha = 0.12f))
                            else Brush.verticalGradient(
                                listOf(
                                    GlassTokens.AccentStart.copy(alpha = 0.35f),
                                    GlassTokens.AccentEnd.copy(alpha = 0.20f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            if (isBypassed) Color.White.copy(alpha = 0.20f)
                            else GlassTokens.AccentEnd.copy(alpha = 0.60f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "A/B Compare",
                        tint = if (isBypassed) Color.White.copy(alpha = 0.7f) else GlassTokens.AccentEnd,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isBypassed) "A/B: Original Raw Sound" else "A/B: Restored Daydream Audio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBypassed) Color.White.copy(alpha = 0.8f) else GlassTokens.AccentEnd
                    )
                    Text(
                        text = if (isBypassed) "Bypass active — tap to hear restoration" else "Tap to instantly hear original unprocessed source",
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary
                    )
                }
            }

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(GlassTokens.radiusPill)
                    .background(
                        if (isBypassed) Color.White.copy(alpha = 0.10f)
                        else GlassTokens.AccentSafe.copy(alpha = 0.18f)
                    )
                    .border(
                        1.dp,
                        if (isBypassed) Color.White.copy(alpha = 0.22f)
                        else GlassTokens.AccentSafe.copy(alpha = 0.70f),
                        GlassTokens.radiusPill
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isBypassed) "RAW" else "ACTIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBypassed) Color.White.copy(alpha = 0.75f) else GlassTokens.AccentSafe
                )
            }
        }
    }
}

/**
 * Now Playing Bar with live playback controls and spectrum visualizer
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
            .raisedGlass(reduceGlass)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Mini album / tape icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(GlassTokens.radiusSm)
                        .background(GlassTokens.AccentGradient)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), GlassTokens.radiusSm),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_daydream_logo),
                        contentDescription = "Track Art",
                        modifier = Modifier.size(36.dp).clip(GlassTokens.radiusSm)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: "No Track Selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track?.era ?: "Demo Engine",
                        fontSize = 11.sp,
                        color = GlassTokens.AccentEnd,
                        maxLines = 1
                    )
                }
            }

            // Live 8-Band Visualizer (Apple Fluid Spectrum)
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .height(28.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val barBrush = Brush.verticalGradient(
                    listOf(
                        GlassTokens.AccentEnd,
                        GlassTokens.AccentStart
                    )
                )
                spectrum.take(6).forEach { level ->
                    val barHeight = (level * 24f).coerceIn(4f, 24f)
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(barHeight.dp)
                            .clip(GlassTokens.radiusPill)
                            .background(
                                if (isPlaying) barBrush else SolidColor(Color.White.copy(alpha = 0.20f))
                            )
                    )
                }
            }

            // Playback controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(38.dp).testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = GlassTokens.TextPrimary,
                        modifier = Modifier.size(24.dp)
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Plain-language explanation popover dialog (PRD Section 5)
 */
@Composable
fun BandTooltipDialog(
    band: PlainBand,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
                        color = GlassTokens.AccentEnd
                    )
                    Text(
                        text = band.frequencyRange,
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = GlassTokens.TextSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = band.plainDescription,
                    fontSize = 14.sp,
                    color = GlassTokens.TextPrimary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusSm)
                        .background(GlassTokens.AccentSafe.copy(alpha = 0.12f))
                        .border(1.dp, GlassTokens.AccentSafe.copy(alpha = 0.3f), GlassTokens.radiusSm)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 When to use: ${band.fixTip}",
                        fontSize = 13.sp,
                        color = GlassTokens.AccentSafe
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusSm)
                        .background(GlassTokens.AccentWarning.copy(alpha = 0.12f))
                        .border(1.dp, GlassTokens.AccentWarning.copy(alpha = 0.3f), GlassTokens.radiusSm)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "⚠️ Watch out: ${band.excessiveWarning}",
                        fontSize = 13.sp,
                        color = GlassTokens.AccentWarning
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                shape = GlassTokens.radiusPill
            ) {
                Text("Got It", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF101522),
        shape = GlassTokens.radiusLg
    )
}
