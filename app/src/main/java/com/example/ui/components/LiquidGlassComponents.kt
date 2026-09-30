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
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import com.example.model.CustomSoundPreset
import com.example.model.DemoTrack
import com.example.model.LocalTrack
import com.example.model.ParametricBand
import com.example.model.PlainBand
import com.example.model.SoundTargetPreset
import com.example.model.LufsMetrics
import com.example.model.StreamingTarget
import com.example.model.TestToneMode
import com.example.model.SubCutFilter
import com.example.model.HarmonicSaturationType
import com.example.model.ReferenceMonitor
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.floatingGlass
import com.example.ui.theme.iosPressable
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

            // Apple Dark Mode Obsidian scrim with luminous liquid fluid transmission
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF030408).copy(alpha = 0.65f),
                                Color(0xFF080912).copy(alpha = 0.72f),
                                Color(0xFF020205).copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Subtle Apple Chromatic Light Diffusion (Deep Indigo + Teal + Amber)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Pole A: Apple System Indigo / Violet radiance (Top Left)
                val poleARadius = (size.width * 0.54f) + (animatedRms * 210f) + (bassEnergy * 85f) + (idleShift * 20f)
                val poleAAlpha = (0.18f + animatedRms * 0.25f).coerceIn(0.12f, 0.45f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassTokens.IosIndigo.copy(alpha = poleAAlpha),
                            GlassTokens.IosBlue.copy(alpha = poleAAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.22f + (idleShift * 25f), size.height * 0.15f + (idleShift * 18f)),
                        radius = poleARadius
                    ),
                    radius = poleARadius,
                    center = Offset(size.width * 0.22f + (idleShift * 25f), size.height * 0.15f + (idleShift * 18f))
                )

                // Pole B: Apple System Teal / Cyan glow (Center Right)
                val poleBRadius = (size.width * 0.58f) + (animatedRms * 230f) + (trebleEnergy * 95f) - (idleShift * 18f)
                val poleBAlpha = (0.15f + animatedRms * 0.22f).coerceIn(0.10f, 0.40f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassTokens.IosTeal.copy(alpha = poleBAlpha),
                            GlassTokens.IosBlue.copy(alpha = poleBAlpha * 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.80f - (idleShift * 20f), size.height * 0.65f - (idleShift * 25f)),
                        radius = poleBRadius
                    ),
                    radius = poleBRadius,
                    center = Offset(size.width * 0.80f - (idleShift * 20f), size.height * 0.65f - (idleShift * 25f))
                )

                // Pole C: Subtle warm amber core (Sub-bass anchor)
                val poleCRadius = (size.width * 0.40f) + (bassEnergy * 120f) + (idleShift * 15f)
                val poleCAlpha = (0.09f + bassEnergy * 0.12f).coerceIn(0.05f, 0.25f)
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

        // Soft overlay scrim for high-contrast legibility (PRD 22.3)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000).copy(alpha = if (reduceGlass) 0.98f else 0.16f))
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
            .padding(vertical = 4.dp)
    ) {
        // Parameter Title & Formatted Value Readout Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            @OptIn(ExperimentalFoundationApi::class)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp)
                    .then(
                        if (onInfoClick != null) {
                            Modifier.combinedClickable(
                                onClick = { onInfoClick() },
                                onLongClick = { onInfoClick() }
                            )
                        } else Modifier
                    )
            ) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (onInfoClick != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info on $title",
                            tint = GlassTokens.TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Value readout (SF Pro Tabular Figures) - strictly pinned right
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
                color = if (isWarning) GlassTokens.IosRed else accentColor,
                maxLines = 1
            )
        }

        // Technical Subtitle line - prevents any row escaping or horizontal clashing
        if (showTechnical && technicalValue != null) {
            Text(
                text = technicalValue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = GlassTokens.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp, bottom = 2.dp)
            )
        }

        if (isWarning && warningText != null) {
            Text(
                text = warningText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = GlassTokens.IosRed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

        val haptic = LocalHapticFeedback.current
        var lastVal by remember { mutableFloatStateOf(value) }

        @OptIn(ExperimentalMaterial3Api::class)
        Slider(
            value = value,
            onValueChange = { newVal ->
                val hasZero = 0f in valueRange && valueRange.start < 0f && valueRange.endInclusive > 0f
                if (hasZero && ((lastVal < 0f && newVal >= 0f) || (lastVal > 0f && newVal <= 0f))) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                lastVal = newVal
                onValueChange(newVal)
            },
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

                        // Liquid radial caustic halo (expands fluidly on drag)
                        val haloAlpha = if (isDragged) 0.35f else 0.12f
                        val haloRadius = if (isDragged) thumbRadius + 7.dp.toPx() else thumbRadius + 2.dp.toPx()
                        val haloColor = if (isWarning) GlassTokens.IosRed else accentColor
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    haloColor.copy(alpha = haloAlpha),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = haloRadius
                            ),
                            radius = haloRadius,
                            center = center
                        )

                        // Physical liquid drop shadow
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.40f),
                            radius = thumbRadius,
                            center = Offset(center.x, center.y + 2.dp.toPx())
                        )

                        // Apple water droplet body (radial gradient lens)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFF2F4F8),
                                    Color(0xFFD6DBE5)
                                ),
                                center = Offset(center.x - thumbRadius * 0.25f, center.y - thumbRadius * 0.25f),
                                radius = thumbRadius
                            ),
                            radius = thumbRadius,
                            center = center
                        )

                        // Specular water glint highlight (top-left dewdrop reflection)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.95f),
                            radius = thumbRadius * 0.30f,
                            center = Offset(center.x - thumbRadius * 0.28f, center.y - thumbRadius * 0.28f)
                        )

                        // Active fluid iris core
                        val dotColor = if (isWarning) GlassTokens.IosRed else accentColor
                        drawCircle(
                            color = dotColor,
                            radius = if (isDragged) 3.5.dp.toPx() else 2.6.dp.toPx(),
                            center = center
                        )
                        // Tiny highlight in iris
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = 1.0.dp.toPx(),
                            center = Offset(center.x - 0.8.dp.toPx(), center.y - 0.8.dp.toPx())
                        )

                        // Water droplet surface tension bevel rim
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.25f)
                                ),
                                start = Offset(center.x - thumbRadius, center.y - thumbRadius),
                                end = Offset(center.x + thumbRadius, center.y + thumbRadius)
                            ),
                            radius = thumbRadius,
                            center = center,
                            style = Stroke(width = 0.9.dp.toPx())
                        )
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
                        .height(8.dp)
                        .clip(GlassTokens.radiusPill)
                ) {
                    // Liquid trough well (deep fluid depression)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF0F1015),
                                Color(0xFF1E202A)
                            )
                        ),
                        cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                    )
                    // Top liquid meniscus rim (water surface tension catch)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.16f)
                            )
                        ),
                        cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                        style = Stroke(width = 0.8.dp.toPx())
                    )

                    val activeWidth = size.width * fraction
                    if (activeWidth > 0f) {
                        val activeColor = if (isWarning) GlassTokens.IosRed else accentColor
                        // Vibrant fluid progress fill
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    activeColor.copy(alpha = 0.88f),
                                    activeColor,
                                    activeColor.copy(alpha = 0.95f)
                                ),
                                start = 0f,
                                endX = activeWidth
                            ),
                            size = Size(activeWidth, size.height),
                            cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                        )
                        // Specular water sheen running along the top half of active track
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.0f)
                                ),
                                startY = 0f,
                                endY = size.height * 0.55f
                            ),
                            size = Size(activeWidth, size.height * 0.55f),
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
    gainMatchedAB: Boolean = false,
    onToggleGainMatched: (() -> Unit)? = null,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                // Apple Control Center Circular Accessory Pill with Liquid Glass Sheen
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBypassed) Brush.verticalGradient(listOf(Color(0xFF32343E), Color(0xFF1E2028)))
                            else Brush.verticalGradient(listOf(GlassTokens.IosBlue.copy(alpha = 0.35f), GlassTokens.IosBlue.copy(alpha = 0.15f)))
                        )
                        .border(
                            1.dp,
                            if (isBypassed) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f)))
                            else Brush.verticalGradient(listOf(GlassTokens.IosBlue.copy(alpha = 0.85f), GlassTokens.IosBlue.copy(alpha = 0.35f))),
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
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = if (isBypassed) "Original Raw Sound (Bypass)" else "Daydream Restored Audio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isBypassed) GlassTokens.TextPrimary else GlassTokens.IosBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isBypassed) {
                            if (gainMatchedAB) "Bypass active (Loudness Matched)" else "Bypass active — tap for instant restoration"
                        } else {
                            if (gainMatchedAB) "Loudness Matched A/B Active" else "Tap to audit unprocessed raw input"
                        },
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onToggleGainMatched != null) {
                    Box(
                        modifier = Modifier
                            .clip(GlassTokens.radiusPill)
                            .background(
                                if (gainMatchedAB) GlassTokens.IosTeal.copy(alpha = 0.22f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                0.8.dp,
                                if (gainMatchedAB) GlassTokens.IosTeal.copy(alpha = 0.65f)
                                else Color.White.copy(alpha = 0.14f),
                                GlassTokens.radiusPill
                            )
                            .clickable { onToggleGainMatched() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "GAIN-MATCH",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gainMatchedAB) GlassTokens.IosTeal else GlassTokens.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Apple iOS Status Capsule with Liquid Specular Sheen
                Box(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(
                            if (isBypassed) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f)))
                            else Brush.verticalGradient(listOf(GlassTokens.IosGreen.copy(alpha = 0.28f), GlassTokens.IosGreen.copy(alpha = 0.10f)))
                        )
                        .border(
                            1.dp,
                            if (isBypassed) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.08f)))
                            else Brush.verticalGradient(listOf(GlassTokens.IosGreen.copy(alpha = 0.80f), GlassTokens.IosGreen.copy(alpha = 0.30f))),
                            GlassTokens.radiusPill
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
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
}

/**
 * Apple Music MiniPlayer (OpenDesign Apple Design System & HIG)
 * 58dp floating frosted capsule, squircle artwork, SF Pro typography, and live mini EQ.
 */
@Composable
fun NowPlayingGlassBar(
    track: DemoTrack? = null,
    localTrack: LocalTrack? = null,
    isPlaying: Boolean,
    isExternalActive: Boolean = false,
    externalAppName: String? = null,
    onTogglePlay: () -> Unit,
    onNextTrack: () -> Unit,
    spectrum: FloatArray,
    reduceGlass: Boolean = false,
    onExpandSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val active = isPlaying || isExternalActive
    val displayTitle = when {
        localTrack != null -> localTrack.title
        isExternalActive -> externalAppName ?: "Streaming Audio"
        track != null -> track.title
        else -> "No Track Selected"
    }
    val displaySubtitle = when {
        localTrack != null -> "${localTrack.artist} • ${localTrack.formattedDuration}"
        isExternalActive -> "Enhancing System Audio"
        track != null -> track.era
        else -> "Lossless DSP Engine"
    }

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
            // Track Info with Apple Squircle Album Art (Tap to expand Cupertino Now Playing Sheet)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExpandSheet() }
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
                        text = displayTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displaySubtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = GlassTokens.IosTeal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                                if (active) SolidColor(GlassTokens.IosBlue)
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
                .fillMaxWidth(if (active) scrubProgress else 0.35f)
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
    isExternalActive: Boolean = false,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    val active = isPlaying || isExternalActive

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
                            .background(if (active) GlassTokens.IosGreen else GlassTokens.TextSecondary)
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
                    text = if (isPlaying) "31Hz — 16kHz" else if (isExternalActive) "Streaming • Active DSP" else "Playback Paused",
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
                            val lvl = if (active) spectrum[i].coerceIn(0.05f, 1f) else 0.05f
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
                                    GlassTokens.IosTeal.copy(alpha = if (active) 0.16f else 0.03f),
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
                            targetValue = if (active) rawLevel.coerceIn(0.04f, 1f) else 0.04f,
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
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF101115), Color(0xFF191B22))
                )
            )
            .border(
                0.8.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.05f))
                ),
                GlassTokens.radiusMd
            )
            .padding(2.5.dp)
    ) {
        val tabWidth = maxWidth / items.size
        val animatedOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f),
            label = "ios_seg_offset"
        )
        // Sliding active liquid glass pill with dual-stop specular sheen
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(GlassTokens.radiusSm)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF383C4A),
                            Color(0xFF22242D)
                        )
                    )
                )
                .border(
                    0.8.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.36f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    GlassTokens.radiusSm
                )
        )
        Row(modifier = Modifier.fillMaxSize()) {
            val haptic = LocalHapticFeedback.current
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(GlassTokens.radiusSm)
                        .clickable {
                            if (index != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onSelect(index)
                        }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(item),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) GlassTokens.TextPrimary else GlassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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

/**
 * Logic Pro Style 10-Band Parametric EQ Transfer Curve Visualizer
 * Real-time logarithmic frequency grid (20Hz - 20kHz, -12dB to +12dB)
 * Renders the composite biquad filter transfer response with glowing neon curve & node markers.
 */
@Composable
fun ParametricEqCurveVisualizer(
    bands: List<ParametricBand>,
    spectrum: FloatArray = FloatArray(8) { 0.1f },
    isPlaying: Boolean = false,
    isExternalActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val active = isPlaying || isExternalActive

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101012))
            .border(0.6.dp, Color(0xFF28282A), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f
            val maxDb = 12f

            // Helper: log frequency (20Hz..20000Hz) to X pixel coordinate
            fun freqToX(hz: Float): Float {
                val minF = 20.0
                val maxF = 20000.0
                val logNorm = (kotlin.math.log10(hz.toDouble() / minF) / kotlin.math.log10(maxF / minF)).toFloat()
                return (logNorm * w).coerceIn(0f, w)
            }

            // Helper: dB (-12..+12) to Y pixel coordinate
            fun dbToY(db: Float): Float {
                val clampedDb = db.coerceIn(-maxDb, maxDb)
                return centerY - (clampedDb / maxDb) * (centerY * 0.85f)
            }

            // 1. Grid lines: dB reference lines
            val dBLines = listOf(12f, 6f, 0f, -6f, -12f)
            for (db in dBLines) {
                val y = dbToY(db)
                val isZero = db == 0f
                drawLine(
                    color = if (isZero) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (isZero) 1.dp.toPx() else 0.6.dp.toPx()
                )
            }

            // Frequency reference vertical lines: 100Hz, 1kHz, 10kHz
            val fGrid = listOf(100f, 1000f, 10000f)
            for (f in fGrid) {
                val x = freqToX(f)
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 0.6.dp.toPx()
                )
            }

            // 1.5 Dual Real-Time Analyzer (Pre vs Post RTA Spectrum Overlay)
            val rtaFreqs = listOf(35f, 90f, 220f, 550f, 1400f, 3600f, 8500f, 16000f)
            val rtaBinWidth = (w / 12f).coerceAtLeast(14.dp.toPx())
            for (i in 0 until 8) {
                val binF = rtaFreqs.getOrElse(i) { 1000f }
                val binX = freqToX(binF)
                val rawEnergy = if (active) (spectrum.getOrElse(i) { 0.05f }).coerceIn(0.04f, 1f) else 0.06f

                var eqBoostDb = 0f
                for (b in bands) {
                    val octDiff = kotlin.math.log2(binF.toDouble() / b.hz.toDouble()).toFloat()
                    val qFactor = b.q.coerceAtLeast(0.2f)
                    val denom = 1f + (octDiff * qFactor * 2.2f) * (octDiff * qFactor * 2.2f)
                    eqBoostDb += b.gainDb / denom
                }
                val postEnergy = (rawEnergy * (1f + (eqBoostDb / 12f) * 0.45f)).coerceIn(0.02f, 1f)

                // Pre-EQ RTA bar (subtle translucent cyan pillar)
                val preBarHeight = (rawEnergy * (h * 0.70f)).coerceAtLeast(3f)
                drawRoundRect(
                    color = GlassTokens.IosTeal.copy(alpha = if (active) 0.16f else 0.06f),
                    topLeft = Offset(binX - rtaBinWidth * 0.42f, h - preBarHeight),
                    size = Size(rtaBinWidth * 0.84f, preBarHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // Post-EQ RTA bar (brighter, neon-tinted active output peak)
                val postBarHeight = (postEnergy * (h * 0.70f)).coerceAtLeast(2f)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassTokens.IosGreen.copy(alpha = if (active) 0.48f else 0.14f),
                            GlassTokens.IosGreen.copy(alpha = if (active) 0.14f else 0.03f)
                        ),
                        startY = h - postBarHeight,
                        endY = h
                    ),
                    topLeft = Offset(binX - rtaBinWidth * 0.25f, h - postBarHeight),
                    size = Size(rtaBinWidth * 0.50f, postBarHeight),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
            }

            // 2. Compute composite transfer function across 80 sample points
            val numSamples = 80
            val curvePoints = mutableListOf<Offset>()
            for (i in 0..numSamples) {
                val norm = i / numSamples.toFloat()
                val minF = 20.0
                val maxF = 20000.0
                val f = (minF * Math.pow(maxF / minF, norm.toDouble())).toFloat()

                // Compute sum of bell filter gains at f
                var totalGainDb = 0f
                for (b in bands) {
                    if (kotlin.math.abs(b.gainDb) > 0.05f) {
                        val octDiff = kotlin.math.log2(f.toDouble() / b.hz.toDouble()).toFloat()
                        val qFactor = b.q.coerceAtLeast(0.2f)
                        val denom = 1f + (octDiff * qFactor * 2.2f) * (octDiff * qFactor * 2.2f)
                        totalGainDb += b.gainDb / denom
                    }
                }
                val px = norm * w
                val py = dbToY(totalGainDb)
                curvePoints.add(Offset(px, py))
            }

            // 3. Draw gradient area fill under the curve
            if (curvePoints.isNotEmpty()) {
                val fillPath = Path().apply {
                    moveTo(curvePoints.first().x, centerY)
                    for (pt in curvePoints) {
                        lineTo(pt.x, pt.y)
                    }
                    lineTo(curvePoints.last().x, centerY)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassTokens.IosTeal.copy(alpha = 0.22f),
                            GlassTokens.IosBlue.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )

                // 4. Draw glowing neon curve line
                val strokePath = Path().apply {
                    moveTo(curvePoints.first().x, curvePoints.first().y)
                    for (i in 1 until curvePoints.size) {
                        val p0 = curvePoints[i - 1]
                        val p1 = curvePoints[i]
                        val midX = (p0.x + p1.x) / 2f
                        val midY = (p0.y + p1.y) / 2f
                        quadraticBezierTo(p0.x, p0.y, midX, midY)
                    }
                    lineTo(curvePoints.last().x, curvePoints.last().y)
                }

                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(GlassTokens.IosBlue, GlassTokens.IosTeal, GlassTokens.IosGreen)
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 5. Draw the 10 Band Node Markers
            for (b in bands) {
                val nodeX = freqToX(b.hz.toFloat())
                val nodeY = dbToY(b.gainDb)
                val isActive = kotlin.math.abs(b.gainDb) > 0.5f

                // Outer halo glow
                drawCircle(
                    color = (if (isActive) GlassTokens.IosTeal else Color.White).copy(alpha = if (isActive) 0.35f else 0.12f),
                    radius = if (isActive) 6.dp.toPx() else 4.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
                // Center solid node dot
                drawCircle(
                    color = if (isActive) GlassTokens.IosTeal else Color(0xFF8E8E93),
                    radius = 3.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
            }
        }

        // Header overlay: Scale markers & Dual RTA status
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("+12 dB", fontSize = 9.sp, color = GlassTokens.TextMuted, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(GlassTokens.IosTeal.copy(alpha = 0.7f)))
                Spacer(modifier = Modifier.width(3.dp))
                Text("PRE", fontSize = 8.sp, color = GlassTokens.IosTeal, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(GlassTokens.IosGreen))
                Spacer(modifier = Modifier.width(3.dp))
                Text("POST RTA", fontSize = 8.sp, color = GlassTokens.IosGreen, fontWeight = FontWeight.Bold)
            }
            Text("-12 dB", fontSize = 9.sp, color = GlassTokens.TextMuted, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Apple Watch Style Ear-Gym Activity Rings
 * Three concentric neon rings representing Ear Trainer Score, Daily Streak, and Accuracy.
 */
@Composable
fun EarActivityRings(
    score: Int,
    streak: Int,
    challengesCompleted: Int,
    modifier: Modifier = Modifier
) {
    val animatedScoreSweep by animateFloatAsState(
        targetValue = if (score > 0) ((score % 500) / 500f * 360f).coerceIn(8f, 360f) else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "score_ring_sweep"
    )

    val animatedStreakSweep by animateFloatAsState(
        targetValue = if (streak > 0) ((streak / 5f) * 360f).coerceIn(8f, 360f) else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "streak_ring_sweep"
    )

    val animatedAccuracySweep by animateFloatAsState(
        targetValue = if (challengesCompleted > 0) ((challengesCompleted / 10f) * 360f).coerceIn(8f, 360f) else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "accuracy_ring_sweep"
    )

    Box(
        modifier = modifier.size(86.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val strokeW = 6.5.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            // Ring 1 (Outer): Score (Apple Cyan/Blue)
            val r1 = (size.width / 2f) - strokeW / 2f
            drawCircle(
                color = GlassTokens.IosBlue.copy(alpha = 0.15f),
                radius = r1,
                style = Stroke(width = strokeW)
            )
            if (animatedScoreSweep > 0f) {
                drawArc(
                    color = GlassTokens.IosBlue,
                    startAngle = -90f,
                    sweepAngle = animatedScoreSweep,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - r1, center.y - r1),
                    size = Size(r1 * 2f, r1 * 2f)
                )
            }

            // Ring 2 (Middle): Streak (Apple Green)
            val r2 = r1 - strokeW - 2.5.dp.toPx()
            drawCircle(
                color = GlassTokens.IosGreen.copy(alpha = 0.15f),
                radius = r2,
                style = Stroke(width = strokeW)
            )
            if (animatedStreakSweep > 0f) {
                drawArc(
                    color = GlassTokens.IosGreen,
                    startAngle = -90f,
                    sweepAngle = animatedStreakSweep,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - r2, center.y - r2),
                    size = Size(r2 * 2f, r2 * 2f)
                )
            }

            // Ring 3 (Inner): Accuracy / Challenges (Apple Coral Red)
            val r3 = r2 - strokeW - 2.5.dp.toPx()
            drawCircle(
                color = GlassTokens.IosRed.copy(alpha = 0.15f),
                radius = r3,
                style = Stroke(width = strokeW)
            )
            if (animatedAccuracySweep > 0f) {
                drawArc(
                    color = GlassTokens.IosRed,
                    startAngle = -90f,
                    sweepAngle = animatedAccuracySweep,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - r3, center.y - r3),
                    size = Size(r3 * 2f, r3 * 2f)
                )
            }
        }

        // Center Icon
        Icon(
            imageVector = Icons.Default.Hearing,
            contentDescription = "Ear Gym Rings",
            tint = GlassTokens.IosBlue,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Direct-Drive 33⅓ RPM Audiophile Vinyl Turntable
 * Features a spinning 12" vinyl disc with microgroove sheen and pivoting chrome tonearm.
 */
@Composable
fun VinylTurntableDeck(
    isPlaying: Boolean,
    trackTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "turntable_rotation_transition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_disc_rotation"
    )

    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 23f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "tonearm_pivot"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141416))
            .border(0.8.dp, Color(0xFF28282A), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header: Turntable Info Bar
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
                            .background(if (isPlaying) Color(0xFF30D158) else Color(0xFF48484A))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TECHNICS DIRECT-DRIVE • 33⅓ RPM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9F0A),
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = if (isPlaying) "STYLUS TRACKING" else "TONEARM RESTED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlaying) GlassTokens.IosGreen else GlassTokens.TextMuted,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Turntable Platter & Disc Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0C0C0D))
                    .border(0.6.dp, Color(0xFF202022), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val w = size.width
                    val h = size.height
                    val platterCenter = Offset(w * 0.45f, h / 2f)
                    val platterRadius = (h / 2f) - 4.dp.toPx()

                    // 1. Brushed Aluminum Platter Rim
                    drawCircle(
                        color = Color(0xFF2C2C2E),
                        radius = platterRadius + 2.dp.toPx(),
                        center = platterCenter
                    )

                    // 2. Spinning Vinyl Record Platter
                    rotate(degrees = if (isPlaying) rotationAngle else 0f, pivot = platterCenter) {
                        drawCircle(
                            color = Color(0xFF101012),
                            radius = platterRadius,
                            center = platterCenter
                        )

                        // Concentric Vinyl Grooves
                        val grooveRadii = listOf(
                            platterRadius * 0.92f,
                            platterRadius * 0.84f,
                            platterRadius * 0.76f,
                            platterRadius * 0.68f,
                            platterRadius * 0.60f
                        )
                        for (gr in grooveRadii) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.05f),
                                radius = gr,
                                center = platterCenter,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Stroboscopic speed dots along outer perimeter
                        for (dotIdx in 0 until 16) {
                            val dotAngle = (dotIdx * (360f / 16f)) * (Math.PI / 180.0)
                            val dotX = platterCenter.x + (platterRadius * 0.96f * kotlin.math.cos(dotAngle)).toFloat()
                            val dotY = platterCenter.y + (platterRadius * 0.96f * kotlin.math.sin(dotAngle)).toFloat()
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = 1.2.dp.toPx(),
                                center = Offset(dotX, dotY)
                            )
                        }

                        // Center Record Label
                        val labelRadius = platterRadius * 0.38f
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(Color(0xFFE05A47), Color(0xFFFF9F0A), Color(0xFFE05A47)),
                                center = platterCenter
                            ),
                            radius = labelRadius,
                            center = platterCenter
                        )

                        // Spindle Hole
                        drawCircle(
                            color = Color(0xFF0C0C0D),
                            radius = 4.dp.toPx(),
                            center = platterCenter
                        )
                    }

                    // 5. Stylus Tonearm (top-right pivot)
                    val pivot = Offset(w * 0.88f, h * 0.22f)
                    // Tonearm base gimbal
                    drawCircle(
                        color = Color(0xFF48484A),
                        radius = 8.dp.toPx(),
                        center = pivot
                    )
                    drawCircle(
                        color = Color(0xFFE5E5EA),
                        radius = 4.dp.toPx(),
                        center = pivot
                    )

                    // Arm angle math
                    val armLength = w * 0.40f
                    val angleRad = Math.toRadians((155.0 - tonearmAngle.toDouble())).toFloat()
                    val cartridgeX = pivot.x + kotlin.math.cos(angleRad) * armLength
                    val cartridgeY = pivot.y + kotlin.math.sin(angleRad) * armLength

                    // Chrome Tonearm Tube
                    drawLine(
                        color = Color(0xFFE5E5EA),
                        start = pivot,
                        end = Offset(cartridgeX, cartridgeY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Headshell / Phono Cartridge
                    drawCircle(
                        color = if (isPlaying) Color(0xFFFF453A) else Color(0xFF2C2C2E),
                        radius = 3.5.dp.toPx(),
                        center = Offset(cartridgeX, cartridgeY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trackTitle ?: "70s Vinyl Acoustic Master",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTokens.TextPrimary
                )
                Text(
                    text = "ANALOG PHONO PREAMP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextMuted,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Apple Music Style Expandable Now Playing Modal Sheet
 * Full-bleed artwork, Lossless badge, interactive timeline scrubber, and Cupertino transport controls.
 */
@Composable
fun NowPlayingModalSheet(
    track: DemoTrack? = null,
    localTrack: LocalTrack? = null,
    isPlaying: Boolean,
    isExternalActive: Boolean = false,
    externalAppName: String? = null,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    onSeekTo: ((Long) -> Unit)? = null,
    isBypassed: Boolean,
    onTogglePlay: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onToggleBypass: () -> Unit,
    onDismiss: () -> Unit,
    audioRms: Float = 0.5f,
    comfortLimiterEnabled: Boolean = false,
    onToggleComfortLimiter: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayTitle = when {
        localTrack != null -> localTrack.title
        isExternalActive -> externalAppName ?: "Streaming Audio"
        track != null -> track.title
        else -> "Daydream Lossless"
    }
    val displaySubtitle = when {
        localTrack != null -> localTrack.artist
        isExternalActive -> "External Media Player • Active DSP Hook"
        track != null -> track.era
        else -> "Audiophile Master Chain"
    }
    val badgeText = when {
        localTrack != null -> "LOCAL MP3 • " + (if (localTrack.bitrateKbps > 0) "${localTrack.bitrateKbps} KBPS • 32-BIT DSP" else "32-BIT DSP MASTER")
        isExternalActive -> "EXTERNAL STREAM • 32-BIT DSP MASTER"
        else -> "LOSSLESS • 24-BIT / 96kHz ALAC"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Color(0xFF161618))
            .border(
                0.8.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                ),
                RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            )
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Dismiss Grabber Pill
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(5.dp)
                    .clip(GlassTokens.radiusPill)
                    .background(Color(0xFF48484A))
                    .clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Large 180dp Album Art Squircle with Soft Glow
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(GlassTokens.radiusXl)
                    .background(Color(0xFF242426))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), GlassTokens.radiusXl),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_daydream_logo),
                    contentDescription = "Now Playing Artwork",
                    modifier = Modifier.size(160.dp).clip(GlassTokens.radiusXl)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track Title & Era
            Text(
                text = displayTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = displaySubtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = GlassTokens.IosTeal
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Apple Lossless Audio Badge
            Box(
                modifier = Modifier
                    .clip(GlassTokens.radiusPill)
                    .background(Color(0xFF2C2C2E))
                    .border(0.6.dp, Color.White.copy(alpha = 0.15f), GlassTokens.radiusPill)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextSecondary,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Apple Health Hearing Comfort Badge
            HearingComfortBadge(
                audioRms = audioRms,
                isLimiterActive = comfortLimiterEnabled,
                onToggleLimiter = onToggleComfortLimiter
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Audio Scrub Timeline Bar
            if (durationMs > 0L) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    var isScrubbing by remember { mutableStateOf(false) }
                    var scrubPos by remember { mutableFloatStateOf(0f) }
                    val displayPos = if (isScrubbing) scrubPos.toLong() else currentPositionMs

                    fun formatTime(ms: Long): String {
                        val totalSec = (ms / 1000).coerceAtLeast(0)
                        val mins = totalSec / 60
                        val secs = totalSec % 60
                        return String.format(java.util.Locale.US, "%d:%02d", mins, secs)
                    }

                    @OptIn(ExperimentalMaterial3Api::class)
                    Slider(
                        value = (if (isScrubbing) scrubPos else currentPositionMs.toFloat()).coerceIn(0f, durationMs.toFloat()),
                        onValueChange = {
                            isScrubbing = true
                            scrubPos = it
                        },
                        onValueChangeFinished = {
                            isScrubbing = false
                            onSeekTo?.invoke(scrubPos.toLong())
                        },
                        valueRange = 0f..durationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = GlassTokens.IosBlue,
                            inactiveTrackColor = Color(0xFF3A3A3C)
                        ),
                        modifier = Modifier.fillMaxWidth().height(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(displayPos), fontSize = 11.sp, color = GlassTokens.TextMuted, fontWeight = FontWeight.Medium)
                        Text("-" + formatTime((durationMs - displayPos).coerceAtLeast(0L)), fontSize = 11.sp, color = GlassTokens.TextMuted, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(GlassTokens.radiusPill)
                            .background(Color(0xFF3A3A3C))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (isPlaying || isExternalActive) 0.75f else 0.25f)
                                .height(4.dp)
                                .clip(GlassTokens.radiusPill)
                                .background(if (isExternalActive) GlassTokens.IosGreen else GlassTokens.IosBlue)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isExternalActive) "Streaming Live Audio" else "Synthesizer Loop", fontSize = 11.sp, color = GlassTokens.TextMuted, fontWeight = FontWeight.Medium)
                        Text(if (isExternalActive) "Active DSP Engine" else "Infinite", fontSize = 11.sp, color = GlassTokens.TextMuted, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Oversized Cupertino Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Track
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .iosPressable { onPreviousTrack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause (Large Apple 64dp Pill)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .iosPressable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Next Track
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .iosPressable { onNextTrack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // AB Compare Capsule Switch inside Sheet
            ABCompareBar(
                isBypassed = isBypassed,
                onToggle = onToggleBypass,
                reduceGlass = false
            )
        }
    }
}

/**
 * Cupertino Sound Target Carousel
 * Fast-switching curated studio target profiles with tactile haptic feedback.
 */
@Composable
fun SoundTargetCarousel(
    targets: List<SoundTargetPreset>,
    activeTargetId: String?,
    onSelectTarget: (SoundTargetPreset) -> Unit,
    onOpenSaveDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SOUND TARGETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.TextSecondary,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "+ Save Custom",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = GlassTokens.IosBlue,
                modifier = Modifier
                    .clip(GlassTokens.radiusSm)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenSaveDialog()
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            targets.forEach { target ->
                val isSelected = target.id == activeTargetId
                val targetBorder = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(GlassTokens.IosBlue, GlassTokens.IosTeal.copy(alpha = 0.7f))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f))
                    )
                }
                val targetBg = if (isSelected) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF222C3E), Color(0xFF141924))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF1D1F2A), Color(0xFF111218))
                    )
                }

                Box(
                    modifier = Modifier
                        .width(138.dp)
                        .clip(GlassTokens.radiusMd)
                        .background(targetBg)
                        .border(
                            width = if (isSelected) 1.2.dp else 0.8.dp,
                            brush = targetBorder,
                            shape = GlassTokens.radiusMd
                        )
                        .clickable {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectTarget(target)
                            }
                        }
                        .padding(11.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(target.iconEmoji, fontSize = 18.sp)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(GlassTokens.radiusPill)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(GlassTokens.IosBlue, GlassTokens.IosTeal)
                                            )
                                        )
                                        .border(0.6.dp, Color.White.copy(alpha = 0.35f), GlassTokens.radiusPill)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(
                            text = target.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = target.subtitle,
                            fontSize = 10.sp,
                            color = GlassTokens.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Apple Health Style Hearing Comfort & Live dB SPL Meter
 * Real-time sound pressure level calculation with safe exposure classification and comfort limiter toggle.
 */
@Composable
fun HearingComfortBadge(
    audioRms: Float,
    isLimiterActive: Boolean,
    onToggleLimiter: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val dBSpl = (60f + (audioRms * 32f)).coerceIn(58f, 96f).toInt()
    val isSafe = dBSpl < 75
    val isModerate = dBSpl in 75..84
    val statusColor = when {
        isSafe -> GlassTokens.IosGreen
        isModerate -> GlassTokens.IosOrange
        else -> GlassTokens.IosRed
    }
    val statusText = when {
        isSafe -> "OK • Safe Level"
        isModerate -> "MODERATE • 8h Exposure"
        else -> "LOUD • Protection Advised"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusMd)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B1D27), Color(0xFF111218))
                )
            )
            .border(
                0.8.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.05f))
                ),
                GlassTokens.radiusMd
            )
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$dBSpl dB SPL",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Apple Health Hearing Protection Standard",
                        fontSize = 9.sp,
                        color = GlassTokens.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onToggleLimiter != null) {
                Box(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(
                            if (isLimiterActive) Brush.verticalGradient(listOf(GlassTokens.IosBlue.copy(alpha = 0.32f), GlassTokens.IosBlue.copy(alpha = 0.12f)))
                            else Brush.verticalGradient(listOf(Color(0xFF2E303A), Color(0xFF1C1E26)))
                        )
                        .border(
                            0.8.dp,
                            if (isLimiterActive) Brush.verticalGradient(listOf(GlassTokens.IosBlue, GlassTokens.IosBlue.copy(alpha = 0.4f)))
                            else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.05f))),
                            GlassTokens.radiusPill
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleLimiter()
                        }
                        .padding(horizontal = 9.dp, vertical = 4.5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Limiter",
                            tint = if (isLimiterActive) GlassTokens.IosBlue else GlassTokens.TextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLimiterActive) "LIMITER ON" else "LIMITER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLimiterActive) GlassTokens.IosBlue else GlassTokens.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun parseHexToColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    val colorInt = clean.toLongOrNull(16) ?: 0x0A84FF
    return Color(if (clean.length == 6) (0xFF000000 or colorInt) else colorInt)
}

/**
 * Cupertino Save Sound Preset Dialog
 * Lets users name their current acoustic balance and pick an accent tint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePresetDialog(
    onSave: (name: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf("") }
    val colors = listOf("#0A84FF", "#30D158", "#BF5AF2", "#FF9F0A", "#FF375F")
    var selectedColor by remember { mutableStateOf(colors.first()) }
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (presetName.isNotBlank()) presetName.trim() else "My Custom Preset"
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(finalName, selectedColor)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                shape = GlassTokens.radiusSm
            ) {
                Text("Save Preset", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = GlassTokens.radiusSm
            ) {
                Text("Cancel", color = GlassTokens.TextSecondary)
            }
        },
        title = {
            Text(
                text = "Save Sound Snapshot",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Save your current EQ curve, spatial stage, and dynamics as a quick preset.",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    placeholder = { Text("e.g. Bass Sanctuary, Late Night Cans", color = GlassTokens.TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = GlassTokens.TextPrimary,
                        unfocusedTextColor = GlassTokens.TextPrimary,
                        cursorColor = GlassTokens.IosBlue,
                        focusedIndicatorColor = GlassTokens.IosBlue,
                        unfocusedIndicatorColor = Color(0xFF38383A),
                        focusedContainerColor = Color(0xFF242426),
                        unfocusedContainerColor = Color(0xFF1C1C1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Color Accent", fontSize = 12.sp, color = GlassTokens.TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colors.forEach { hex ->
                        val color = parseHexToColor(hex)
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedColor = hex
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF1C1C1E),
        shape = GlassTokens.radiusLg
    )
}

/**
 * Pro Audio Goniometer & Phase Correlation Meter (-1.0 to +1.0)
 * Visualizes stereo phase coherence in real time to prevent mono cancellation.
 */
@Composable
fun PhaseCorrelationMeter(
    correlation: Float,
    modifier: Modifier = Modifier
) {
    val clamped = correlation.coerceIn(-1.0f, 1.0f)
    val animatedCorr by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "phase_corr"
    )

    val (statusText, statusColor) = when {
        clamped < -0.3f -> "Anti-Phase" to GlassTokens.IosRed
        clamped < 0.1f -> "Weak Center" to GlassTokens.IosOrange
        clamped < 0.6f -> "Wide Stereo" to GlassTokens.IosYellow
        else -> "Mono Safe" to GlassTokens.IosGreen
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PHASE CORRELATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = GlassTokens.TextSecondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = String.format("%+.2f (%s)", clamped, statusText),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Meter Track with liquid recessed well and surface rim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(GlassTokens.radiusPill)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F1015), Color(0xFF1B1D26))
                    )
                )
                .border(
                    0.8.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.06f))
                    ),
                    GlassTokens.radiusPill
                )
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Zone Gradients
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midX = w / 2f

                // Left track (-1 to 0): Red to Yellow
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFF453A).copy(alpha = 0.35f), Color(0xFFFFD60A).copy(alpha = 0.25f)),
                        startX = 0f,
                        endX = midX
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(midX, h)
                )

                // Right track (0 to +1): Yellow to Green
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFFD60A).copy(alpha = 0.25f), Color(0xFF30D158).copy(alpha = 0.35f)),
                        startX = midX,
                        endX = w
                    ),
                    topLeft = Offset(midX, 0f),
                    size = Size(w - midX, h)
                )

                // Center line at 0.0
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(midX, 0f),
                    end = Offset(midX, h),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Animated needle / indicator pill
            androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalWidth = maxWidth
                val normPos = ((animatedCorr + 1.0f) / 2.0f).coerceIn(0f, 1f)
                val indicatorOffset = (totalWidth - 8.dp) * normPos

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(8.dp)
                        .fillMaxHeight()
                        .clip(GlassTokens.radiusPill)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White, statusColor)
                            )
                        )
                        .border(0.6.dp, Color.White, GlassTokens.radiusPill)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Scale labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-1 (Anti-Phase)", fontSize = 10.sp, color = GlassTokens.IosRed, fontWeight = FontWeight.Medium)
            Text("0 (Stereo)", fontSize = 10.sp, color = GlassTokens.TextMuted, fontWeight = FontWeight.Medium)
            Text("+1 (Mono Safe)", fontSize = 10.sp, color = GlassTokens.IosGreen, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Studio Crest Factor (Dynamic Range) & Peak/RMS Meter Badge
 * Shows real-time dBFS peak, RMS loudness, and crest factor with balanced columns and liquid glass surface.
 */
@Composable
fun CrestFactorBadge(
    crestFactorDb: Float,
    peakDbfs: Float,
    rmsDbfs: Float,
    modifier: Modifier = Modifier
) {
    val (ratingText, ratingColor) = when {
        crestFactorDb >= 14f -> "Open Dynamic" to GlassTokens.IosTeal
        crestFactorDb >= 9f -> "Mastered Commercial" to GlassTokens.IosGreen
        crestFactorDb >= 6f -> "Modern Loud" to GlassTokens.IosYellow
        else -> "Squashed / Over-limited" to GlassTokens.IosRed
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusMd)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B1D26), Color(0xFF121319))
                )
            )
            .border(
                0.8.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                ),
                GlassTokens.radiusMd
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "DYNAMIC RANGE (CREST FACTOR)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTokens.TextSecondary,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (peakDbfs <= -90f) "DR -- dB" else String.format("DR %.1f dB", crestFactorDb.coerceIn(0f, 30f)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ratingColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (peakDbfs <= -90f) "Idle" else ratingText,
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (peakDbfs <= -90f) "Peak: -∞ dBFS" else String.format("Peak: %+.1f dBFS", peakDbfs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (peakDbfs >= -0.1f) GlassTokens.IosRed else GlassTokens.TextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (rmsDbfs <= -90f) "RMS: -∞ dBFS" else String.format("RMS: %+.1f dBFS", rmsDbfs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = GlassTokens.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * ITU-R BS.1770-4 & EBU R128 Broadcast Loudness Suite
 * VisionOS-styled floating badge with Momentary, Short-Term, Gated Integrated LUFS, LRA and 4x True Peak (dBTP).
 * Includes streaming target compliance deltas for Spotify, Apple Music, YouTube, and Club Masters.
 */
@Composable
fun LufsLoudnessMeter(
    metrics: LufsMetrics,
    currentTarget: StreamingTarget,
    onSelectTarget: (StreamingTarget) -> Unit,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .floatingGlass(reduceGlass)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header & Platform Target Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = GlassTokens.IosIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EBU R128 / ITU-R BS.1770-4 LOUDNESS",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Streaming Platform Compliance Delta Pill
                val delta = metrics.integratedLufs - currentTarget.targetLufs
                val isHot = delta > 0.5f
                val isLow = delta < -2.0f
                val deltaColor = when {
                    isHot -> GlassTokens.IosOrange
                    isLow -> GlassTokens.IosYellow
                    else -> GlassTokens.IosGreen
                }
                val deltaText = when {
                    metrics.integratedLufs <= -60f -> "Target: ${currentTarget.targetLufs.toInt()} LUFS"
                    isHot -> String.format("+%.1f LU Hot", delta)
                    isLow -> String.format("%.1f LU Low", delta)
                    else -> String.format("%.1f LU Match", delta)
                }

                Box(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(deltaColor.copy(alpha = 0.15f))
                        .border(0.8.dp, deltaColor.copy(alpha = 0.5f), GlassTokens.radiusPill)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = deltaText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = deltaColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Platform Targets Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StreamingTarget.values().forEach { target ->
                    val isSelected = target == currentTarget
                    Box(
                        modifier = Modifier
                            .clip(GlassTokens.radiusPill)
                            .background(if (isSelected) GlassTokens.IosIndigo else Color.White.copy(alpha = 0.05f))
                            .border(
                                0.8.dp,
                                if (isSelected) GlassTokens.IosIndigo else Color.White.copy(alpha = 0.12f),
                                GlassTokens.radiusPill
                            )
                            .clickable { onSelectTarget(target) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${target.platform} (${target.targetLufs.toInt()})",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else GlassTokens.TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Integrated LUFS Readout + Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "INTEGRATED LOUDNESS (GATED)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.4.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (metrics.integratedLufs <= -60f) "-∞ LUFS" else String.format("%.1f LUFS", metrics.integratedLufs),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (metrics.integratedLufs > currentTarget.targetLufs + 0.5f) GlassTokens.IosOrange else GlassTokens.TextPrimary
                    )
                }

                // True Peak Meter & Loudness Range
                Column(horizontalAlignment = Alignment.End) {
                    val tp = metrics.truePeakDbtp
                    val isTpClipping = tp > currentTarget.maxTruePeakDbtp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "True Peak: ",
                            fontSize = 11.sp,
                            color = GlassTokens.TextSecondary
                        )
                        Text(
                            text = if (tp <= -60f) "-∞ dBTP" else String.format("%+.1f dBTP", tp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTpClipping) GlassTokens.IosRed else GlassTokens.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format("LRA: %.1f LU (Range)", metrics.loudnessRangeLu),
                        fontSize = 11.sp,
                        color = GlassTokens.IosTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-metrics Bar (Momentary 400ms & Short-Term 3s)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GlassTokens.radiusSm)
                    .background(Color(0xFF14151B))
                    .border(0.6.dp, Color.White.copy(alpha = 0.08f), GlassTokens.radiusSm)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Momentary (400ms): ",
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary
                    )
                    Text(
                        text = if (metrics.momentaryLufs <= -60f) "-∞" else String.format("%.1f", metrics.momentaryLufs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary
                    )
                }
                Row {
                    Text(
                        text = "Short-Term (3s): ",
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary
                    )
                    Text(
                        text = if (metrics.shortTermLufs <= -60f) "-∞" else String.format("%.1f", metrics.shortTermLufs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Dynamic Frequency De-Esser (6.5kHz Sibilance Tamer)
 * Real-time dynamic notch attenuation with live Gain Reduction (GR) metering.
 */
@Composable
fun DynamicDeEsserBadge(
    enabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    thresholdDb: Float,
    onThresholdChange: (Float) -> Unit,
    maxReductionDb: Float,
    onMaxReductionChange: (Float) -> Unit,
    currentReductionDb: Float,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .floatingGlass(reduceGlass)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dynamic Frequency De-Esser",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(GlassTokens.radiusPill)
                                .background(GlassTokens.IosPurple.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "6.5 kHz Band",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.IosPurple
                            )
                        }
                    }
                    Text(
                        text = "Real-time dynamic notch clamps vocal sibilance ('s', 'sh', 't') without dulling overall air.",
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggleEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GlassTokens.IosPurple
                    )
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(10.dp))

                // Live Gain Reduction (GR) Meter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LIVE SIBILANCE REDUCTION",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (currentReductionDb > 0.05f) String.format("-%.1f dB GR", currentReductionDb) else "0.0 dB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentReductionDb > 0.05f) GlassTokens.IosPurple else GlassTokens.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Meter Bar
                val grFraction = (currentReductionDb / maxReductionDb.coerceAtLeast(1f)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF191B24))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(grFraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(GlassTokens.IosPurple.copy(alpha = 0.6f), GlassTokens.IosPurple)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LiquidSlider(
                    title = "Sensitivity Threshold",
                    value = thresholdDb,
                    onValueChange = onThresholdChange,
                    valueRange = -36f..-6f,
                    unit = " dBFS",
                    technicalValue = String.format("%.1f dBFS", thresholdDb),
                    showTechnical = true,
                    accentColor = GlassTokens.IosPurple,
                    reduceGlass = reduceGlass
                )

                Spacer(modifier = Modifier.height(4.dp))

                LiquidSlider(
                    title = "Max Attenuation",
                    value = maxReductionDb,
                    onValueChange = onMaxReductionChange,
                    valueRange = 1f..18f,
                    unit = " dB",
                    technicalValue = String.format("-%.1f dB", maxReductionDb),
                    showTechnical = true,
                    accentColor = GlassTokens.IosPurple,
                    reduceGlass = reduceGlass
                )
            }
        }
    }
}

/**
 * Studio Calibration & Test Tone Generator Card
 * Real synthesizer for hardware calibration, room alignment & listening tests.
 */
@Composable
fun TestToneGeneratorCard(
    currentMode: TestToneMode,
    onSelectMode: (TestToneMode) -> Unit,
    levelDb: Float,
    onLevelChange: (Float) -> Unit,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isToneActive = currentMode != TestToneMode.OFF
    Box(
        modifier = modifier
            .fillMaxWidth()
            .floatingGlass(reduceGlass)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isToneActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = if (isToneActive) GlassTokens.IosOrange else GlassTokens.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CALIBRATION TEST TONES",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isToneActive) {
                    Box(
                        modifier = Modifier
                            .clip(GlassTokens.radiusPill)
                            .background(GlassTokens.IosRed.copy(alpha = 0.2f))
                            .border(0.8.dp, GlassTokens.IosRed.copy(alpha = 0.6f), GlassTokens.radiusPill)
                            .clickable { onSelectMode(TestToneMode.OFF) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "MUTE TONE",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.IosRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Waveform Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TestToneMode.values().forEach { mode ->
                    val isSelected = mode == currentMode
                    val activeColor = if (mode == TestToneMode.OFF) GlassTokens.TextSecondary else GlassTokens.IosOrange
                    Box(
                        modifier = Modifier
                            .clip(GlassTokens.radiusPill)
                            .background(if (isSelected) activeColor else Color.White.copy(alpha = 0.05f))
                            .border(
                                0.8.dp,
                                if (isSelected) activeColor else Color.White.copy(alpha = 0.12f),
                                GlassTokens.radiusPill
                            )
                            .clickable { onSelectMode(mode) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) (if (mode == TestToneMode.OFF) Color.White else Color.Black) else GlassTokens.TextPrimary
                        )
                    }
                }
            }

            if (isToneActive) {
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusSm)
                        .background(GlassTokens.IosOrange.copy(alpha = 0.12f))
                        .border(0.6.dp, GlassTokens.IosOrange.copy(alpha = 0.35f), GlassTokens.radiusSm)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "SYNTHESIZING: ${currentMode.description}",
                        fontSize = 11.sp,
                        color = GlassTokens.IosOrange,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LiquidSlider(
                    title = "Generator Output Trim",
                    value = levelDb,
                    onValueChange = onLevelChange,
                    valueRange = -36f..0f,
                    unit = " dBFS",
                    technicalValue = String.format("%.1f dBFS", levelDb),
                    showTechnical = true,
                    accentColor = GlassTokens.IosOrange,
                    reduceGlass = reduceGlass
                )
            }
        }
    }
}

/**
 * Infrasonic Sub-Cut Filter Card
 * 18-24 dB/octave cascaded Butterworth highpass filter.
 */
@Composable
fun MasteringSubCutCard(
    currentFilter: SubCutFilter,
    onSelectFilter: (SubCutFilter) -> Unit,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .floatingGlass(reduceGlass)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Infrasonic Sub-Cut Filter",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "High-order Butterworth filter removes inaudible speaker cone excursion to reclaim mastering headroom.",
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(GlassTokens.IosTeal.copy(alpha = 0.2f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = currentFilter.slope,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.IosTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubCutFilter.values().forEach { filter ->
                    val isSelected = filter == currentFilter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(GlassTokens.radiusPill)
                            .background(if (isSelected) GlassTokens.IosTeal else Color.White.copy(alpha = 0.05f))
                            .border(
                                0.8.dp,
                                if (isSelected) GlassTokens.IosTeal else Color.White.copy(alpha = 0.12f),
                                GlassTokens.radiusPill
                            )
                            .clickable { onSelectFilter(filter) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else GlassTokens.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stereo Balance Trim & Polarity Inversion Card
 * +/- 6dB balance trim and independent channel polarity reversal (Ø L, Ø R).
 */
@Composable
fun StereoBalanceAndPolarityCard(
    balanceTrimDb: Float,
    onBalanceTrimChange: (Float) -> Unit,
    invertLeftPolarity: Boolean,
    onToggleInvertLeft: (Boolean) -> Unit,
    invertRightPolarity: Boolean,
    onToggleInvertRight: (Boolean) -> Unit,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .floatingGlass(reduceGlass)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Stereo Balance & Phase Inversion",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "Fine-tune monitor balance & test stereo microphone phase cancellation with 180° polarity flips.",
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Ø Left button
                    Box(
                        modifier = Modifier
                            .clip(GlassTokens.radiusPill)
                            .background(
                                if (invertLeftPolarity) GlassTokens.IosRed.copy(alpha = 0.3f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                0.8.dp,
                                if (invertLeftPolarity) GlassTokens.IosRed
                                else Color.White.copy(alpha = 0.15f),
                                GlassTokens.radiusPill
                            )
                            .clickable { onToggleInvertLeft(!invertLeftPolarity) }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Ø L",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (invertLeftPolarity) GlassTokens.IosRed else GlassTokens.TextPrimary
                        )
                    }

                    // Ø Right button
                    Box(
                        modifier = Modifier
                            .clip(GlassTokens.radiusPill)
                            .background(
                                if (invertRightPolarity) GlassTokens.IosRed.copy(alpha = 0.3f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .border(
                                0.8.dp,
                                if (invertRightPolarity) GlassTokens.IosRed
                                else Color.White.copy(alpha = 0.15f),
                                GlassTokens.radiusPill
                            )
                            .clickable { onToggleInvertRight(!invertRightPolarity) }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Ø R",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (invertRightPolarity) GlassTokens.IosRed else GlassTokens.TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LiquidSlider(
                title = "Stereo Balance Trim",
                value = balanceTrimDb,
                onValueChange = onBalanceTrimChange,
                valueRange = -6f..6f,
                unit = " dB",
                technicalValue = when {
                    balanceTrimDb < -0.1f -> String.format("L %+.1f dB", balanceTrimDb)
                    balanceTrimDb > 0.1f -> String.format("R %+.1f dB", balanceTrimDb)
                    else -> "Center (0.0 dB)"
                },
                showTechnical = true,
                accentColor = GlassTokens.IosBlue,
                reduceGlass = reduceGlass
            )
        }
    }
}

/**
 * Real-Time 2D Phosphor Lissajous Goniometer & Vector Scope (Pro Studio Suite 2.0).
 * Projects stereo phase coherence onto orthogonal Mid/Side space:
 * - Vertical (+M / -M): Monophonic phantom center (lead vocal, bass, kick).
 * - Horizontal (-S / +S): 100% decorrelated side energy.
 * - Diagonal (±45°): Dedicated Left and Right stereo boundaries.
 * - Reactive anti-phase aura when correlation falls below zero.
 */
@Composable
fun LissajousVectorScope(
    points: FloatArray,
    phaseCorrelation: Float,
    modifier: Modifier = Modifier,
    reduceGlass: Boolean = false,
    reduceMotion: Boolean = false
) {
    val isAntiPhase = phaseCorrelation < -0.15f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusMd)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F1015),
                        Color(0xFF161820)
                    )
                )
            )
            .border(
                1.dp,
                if (isAntiPhase) GlassTokens.IosRed.copy(alpha = 0.6f) else GlassTokens.IosSeparator,
                GlassTokens.radiusMd
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "VECTOR SCOPE (LISSAJOUS GONIOMETER)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Orthogonal M/S Phase Space • Phosphor Trail",
                        fontSize = 10.sp,
                        color = GlassTokens.TextSecondary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isAntiPhase) {
                    Box(
                        modifier = Modifier
                            .clip(GlassTokens.radiusPill)
                            .background(GlassTokens.IosRed.copy(alpha = 0.2f))
                            .border(0.8.dp, GlassTokens.IosRed, GlassTokens.radiusPill)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ANTI-PHASE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.IosRed
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(GlassTokens.radiusPill)
                            .background(GlassTokens.IosGreen.copy(alpha = 0.15f))
                            .border(0.8.dp, GlassTokens.IosGreen.copy(alpha = 0.5f), GlassTokens.radiusPill)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PHASE: ${String.format("%+.2f", phaseCorrelation)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.IosGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Vector Scope
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(GlassTokens.radiusSm)
                    .background(Color(0xFF08090C))
                    .border(0.6.dp, GlassTokens.IosSeparator.copy(alpha = 0.5f), GlassTokens.radiusSm),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val radius = kotlin.math.min(cx, cy) * 0.88f

                    // 1. Graticule concentric circles
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = radius * 0.33f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = radius * 0.66f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = if (isAntiPhase) GlassTokens.IosRed.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.18f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.2f)
                    )

                    // 2. Graticule axes: Mid (vertical), Side (horizontal)
                    drawLine(
                        color = Color.White.copy(alpha = 0.22f),
                        start = Offset(cx, cy - radius),
                        end = Offset(cx, cy + radius),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.22f),
                        start = Offset(cx - radius, cy),
                        end = Offset(cx + radius, cy),
                        strokeWidth = 1f
                    )

                    // Diagonal L and R 45-degree guide axes
                    val diagOffset = radius * 0.7071f
                    drawLine(
                        color = Color.White.copy(alpha = 0.10f),
                        start = Offset(cx - diagOffset, cy - diagOffset),
                        end = Offset(cx + diagOffset, cy + diagOffset),
                        strokeWidth = 0.8f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.10f),
                        start = Offset(cx - diagOffset, cy + diagOffset),
                        end = Offset(cx + diagOffset, cy - diagOffset),
                        strokeWidth = 0.8f
                    )

                    // 3. Phosphor Vector Cloud
                    val numPoints = points.size / 2
                    if (numPoints > 1) {
                        val phosphorColor = if (isAntiPhase) Color(0xFFFF453A) else Color(0xFF00F5D4)
                        for (i in 0 until numPoints) {
                            val px = points[i * 2]
                            val py = points[i * 2 + 1]
                            val screenX = cx + px * radius
                            val screenY = cy - py * radius
                            val progress = i.toFloat() / numPoints.toFloat()
                            val alpha = 0.15f + progress * 0.75f
                            drawCircle(
                                color = phosphorColor.copy(alpha = alpha),
                                radius = if (i == numPoints - 1) 3.5f else 1.8f,
                                center = Offset(screenX, screenY)
                            )
                            if (i > 0) {
                                val prevX = cx + points[(i - 1) * 2] * radius
                                val prevY = cy - points[(i - 1) * 2 + 1] * radius
                                drawLine(
                                    color = phosphorColor.copy(alpha = alpha * 0.6f),
                                    start = Offset(prevX, prevY),
                                    end = Offset(screenX, screenY),
                                    strokeWidth = 1.2f
                                )
                            }
                        }
                    }
                }

                // Graticule Axis Labels
                Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    Text(
                        text = "+M (Center)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    Text(
                        text = "-M",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    Text(
                        text = "-S",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    Text(
                        text = "+S",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                    Text(
                        text = "L",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.IosBlue.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 8.dp)
                    )
                    Text(
                        text = "R",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.IosPurple.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pure Mono = Vertical Line | Wide Stereo = Ellipse | Anti-Phase = Horizontal",
                    fontSize = 10.sp,
                    color = GlassTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Pro Studio Dynamic Transient Designer (SPL & Oxford TransMod Envelope Follower).
 * Allows independent Attack (-12dB to +12dB) and Sustain (-12dB to +12dB) sculpting
 * with live LED activity meters.
 */
@Composable
fun TransientDesignerCard(
    attackPercent: Float,
    sustainPercent: Float,
    attackActivity: Float,
    sustainActivity: Float,
    onAttackChange: (Float) -> Unit,
    onSustainChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    reduceGlass: Boolean = false
) {
    val attackGainDb = (attackPercent / 100f) * 12f
    val sustainGainDb = (sustainPercent / 100f) * 12f

    val attackLedActive = attackActivity > 0.05f
    val sustainLedActive = sustainActivity > 0.05f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusMd)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF13151D),
                        Color(0xFF1A1D27)
                    )
                )
            )
            .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusMd)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "DYNAMIC TRANSIENT DESIGNER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "SPL / Oxford TransMod Differential Envelope Follower",
                        fontSize = 10.sp,
                        color = GlassTokens.TextSecondary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Reset button
                Box(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.15f), GlassTokens.radiusPill)
                        .clickable { onReset() }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Reset 0dB",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassTokens.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Attack Sculpt Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Live Attack Activity LED
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (attackLedActive) Color(0xFF00FFCC)
                                else Color.White.copy(alpha = 0.15f)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ATTACK SCULPT (τ = 1.5ms)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary
                    )
                }
                Text(
                    text = String.format("%+.1f dB", attackGainDb),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (attackPercent != 0f) Color(0xFF00FFCC) else GlassTokens.TextSecondary
                )
            }

            LiquidSlider(
                title = "",
                value = attackPercent,
                onValueChange = onAttackChange,
                valueRange = -100f..100f,
                unit = "%",
                technicalValue = if (attackGainDb >= 0) "+${String.format("%.1f", attackGainDb)} dB Transient Punch" else "${String.format("%.1f", attackGainDb)} dB Softened Attack",
                showTechnical = true,
                accentColor = Color(0xFF00FFCC),
                reduceGlass = reduceGlass
            )

            IosRowSeparator(modifier = Modifier.padding(vertical = 10.dp))

            // Sustain Tail Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Live Sustain Activity LED
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (sustainLedActive) GlassTokens.IosOrange
                                else Color.White.copy(alpha = 0.15f)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SUSTAIN TAIL (τ = 180ms)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary
                    )
                }
                Text(
                    text = String.format("%+.1f dB", sustainGainDb),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sustainPercent != 0f) GlassTokens.IosOrange else GlassTokens.TextSecondary
                )
            }

            LiquidSlider(
                title = "",
                value = sustainPercent,
                onValueChange = onSustainChange,
                valueRange = -100f..100f,
                unit = "%",
                technicalValue = if (sustainGainDb >= 0) "+${String.format("%.1f", sustainGainDb)} dB Room & Body" else "${String.format("%.1f", sustainGainDb)} dB Gated Dryness",
                showTechnical = true,
                accentColor = GlassTokens.IosOrange,
                reduceGlass = reduceGlass
            )
        }
    }
}

/**
 * Pro Studio Analog Harmonic Saturation Color Topology.
 * Select between Triode Class-A (2nd even harmonics), Reel Tape (3rd odd harmonics),
 * and Console Transformer iron core low-end hysteresis, with live THD % calculation.
 */
@Composable
fun HarmonicSaturationCard(
    selectedType: HarmonicSaturationType,
    drivePercent: Float,
    thdPercent: Float,
    onTypeChange: (HarmonicSaturationType) -> Unit,
    onDriveChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    reduceGlass: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusMd)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF13141C),
                        Color(0xFF1B1C26)
                    )
                )
            )
            .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusMd)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "ANALOG HARMONIC COLORATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Triode 2f₀ • Reel Tape 3f₀ • Transformer Hysteresis",
                        fontSize = 10.sp,
                        color = GlassTokens.TextSecondary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Live THD % badge
                Box(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(
                            if (thdPercent > 0.05f) GlassTokens.IosOrange.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                        .border(
                            0.8.dp,
                            if (thdPercent > 0.05f) GlassTokens.IosOrange.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.15f),
                            GlassTokens.radiusPill
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "THD: ${String.format("%.2f", thdPercent)}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (thdPercent > 0.05f) GlassTokens.IosOrange else GlassTokens.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Topology Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HarmonicSaturationType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .clip(GlassTokens.radiusPill)
                            .background(if (isSelected) GlassTokens.IosOrange else GlassTokens.IosGroupedSecondary)
                            .border(
                                0.8.dp,
                                if (isSelected) GlassTokens.IosOrange else GlassTokens.IosSeparator,
                                GlassTokens.radiusPill
                            )
                            .clickable { onTypeChange(type) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = type.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else GlassTokens.TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Topology Description Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GlassTokens.radiusSm)
                    .background(Color(0xFF0C0D12))
                    .border(0.6.dp, GlassTokens.IosSeparator.copy(alpha = 0.5f), GlassTokens.radiusSm)
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "${selectedType.label} • ${selectedType.order}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.IosOrange
                    )
                    Text(
                        text = selectedType.description,
                        fontSize = 10.sp,
                        color = GlassTokens.TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Drive Slider
            LiquidSlider(
                title = "Harmonic Saturation Drive",
                value = drivePercent,
                onValueChange = onDriveChange,
                valueRange = 0f..100f,
                unit = "%",
                technicalValue = if (drivePercent > 0f) "+${String.format("%.1f", (drivePercent / 100f) * 15f)} dB Drive (Auto-Level Comp)" else "Bypassed (0.0 dB)",
                showTechnical = true,
                accentColor = GlassTokens.IosOrange,
                reduceGlass = reduceGlass
            )
        }
    }
}

/**
 * Pro Studio ISO 226 Fletcher-Munson Equal-Loudness Calibration.
 * Calibrates low-volume listening balance (+4.5dB @ 85Hz, +2.5dB @ 8.5kHz)
 * so engineers can audition mixes accurately without ear fatigue.
 */
@Composable
fun FletcherMunsonBadge(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusSm)
            .background(if (enabled) GlassTokens.IosIndigo.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f))
            .border(
                0.8.dp,
                if (enabled) GlassTokens.IosIndigo else GlassTokens.IosSeparator,
                GlassTokens.radiusSm
            )
            .clickable { onToggle(!enabled) }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (enabled) GlassTokens.IosIndigo else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ISO 226 Fletcher-Munson Calibration",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary
                    )
                }
                Text(
                    text = "Inverse Phon curve (+4.5dB @ 85Hz, +2.5dB @ 8.5kHz) for low-volume mix translation without ear fatigue.",
                    fontSize = 11.sp,
                    color = GlassTokens.TextSecondary,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = GlassTokens.IosIndigo
                )
            )
        }
    }
}

