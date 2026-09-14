package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    reduceGlass: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedRms by animateFloatAsState(
        targetValue = if (reduceMotion) 0f else audioRms.coerceIn(0f, 0.4f),
        animationSpec = tween(durationMillis = 150),
        label = "rms_glow"
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0C0A14))) {
        if (!reduceGlass) {
            // Ambient photo backdrop
            Image(
                painter = painterResource(id = R.drawable.bg_ambient_glass),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp)
            )

            // Audio-reactive Sonic Glass ripple canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.4f)
                val rippleRadius = (size.width * 0.45f) + (animatedRms * 350f)
                val alpha = (0.12f + animatedRms * 0.45f).coerceIn(0f, 0.45f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassTokens.AccentStart.copy(alpha = alpha),
                            GlassTokens.AccentEnd.copy(alpha = alpha * 0.4f),
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
                .background(Color(0xFF090810).copy(alpha = if (reduceGlass) 0.98f else 0.48f))
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    enabled = onInfoClick != null,
                    onClick = { onInfoClick?.invoke() }
                )
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
                        contentDescription = "Info on $title",
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
                Text(
                    text = if (value > 0 && unit == "dB") "+${value.toInt()}$unit" else "${value.toInt()}$unit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarning) GlassTokens.AccentWarning else GlassTokens.AccentStart
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

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = if (isWarning) GlassTokens.AccentWarning else accentColor,
                activeTrackColor = if (isWarning) GlassTokens.AccentWarning else accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            ),
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBypassed) Color.White.copy(alpha = 0.15f)
                            else GlassTokens.AccentStart.copy(alpha = 0.25f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "A/B Compare",
                        tint = if (isBypassed) Color.White.copy(alpha = 0.7f) else GlassTokens.AccentStart,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isBypassed) "A/B: Original Raw Sound" else "A/B: Restored Daydream Audio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBypassed) Color.White.copy(alpha = 0.8f) else GlassTokens.AccentStart
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
                        if (isBypassed) Color.White.copy(alpha = 0.1f)
                        else GlassTokens.AccentSafe.copy(alpha = 0.2f)
                    )
                    .border(
                        1.dp,
                        if (isBypassed) Color.White.copy(alpha = 0.2f)
                        else GlassTokens.AccentSafe.copy(alpha = 0.6f),
                        GlassTokens.radiusPill
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isBypassed) "RAW" else "ACTIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBypassed) Color.White.copy(alpha = 0.7f) else GlassTokens.AccentSafe
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
                        .background(GlassTokens.AccentGradient),
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
                        color = GlassTokens.AccentStart,
                        maxLines = 1
                    )
                }
            }

            // Live 8-Band Visualizer
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .height(28.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                spectrum.take(6).forEach { level ->
                    val barHeight = (level * 24f).coerceIn(4f, 24f)
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight.dp)
                            .clip(GlassTokens.radiusPill)
                            .background(
                                if (isPlaying) GlassTokens.AccentStart else Color.White.copy(alpha = 0.2f)
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
                        color = GlassTokens.AccentStart
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
        containerColor = Color(0xFF191626),
        shape = GlassTokens.radiusLg
    )
}
