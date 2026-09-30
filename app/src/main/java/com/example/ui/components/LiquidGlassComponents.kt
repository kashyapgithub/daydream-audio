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

    val midEnergy = if (spectrum != null && spectrum.size >= 8) {
        (spectrum[2] + spectrum[3] + spectrum[4]) / 3f
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

                // Pole D: midrange magenta presence that follows vocal energy
                val poleDRadius = (size.width * 0.34f) + (midEnergy * 130f) + (animatedRms * 120f)
                val poleDAlpha = (0.07f + midEnergy * 0.14f).coerceIn(0.04f, 0.28f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFBF5AF2).copy(alpha = poleDAlpha),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.62f - (idleShift * 22f), size.height * 0.30f + (idleShift * 14f)),
                        radius = poleDRadius
                    ),
                    radius = poleDRadius,
                    center = Offset(size.width * 0.62f - (idleShift * 22f), size.height * 0.30f + (idleShift * 14f))
                )
                // Cinematic vignette for depth
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = maxOf(size.width, size.height) * 0.72f
                    ),
                    size = size
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                                startX = 0f,
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
                            color = if (gainMatchedAB) GlassTokens.IosTeal else GlassTokens.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        color = if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    modifier: Modifier = Modifier,
    positionMs: Long = 0L,
    durationMs: Long = 0L,
    audioRms: Float = 0.2f
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
        isExternalActive -> "External audio • meter unavailable"
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

            // Pro mini spectrum: single Canvas, neon glow + rounded caps
            Canvas(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .width(52.dp)
                    .height(26.dp)
            ) {
                val n = 8
                val gap = 2.dp.toPx()
                val bw = (size.width - gap * (n - 1)) / n
                for (i in 0 until n) {
                    val lvl = spectrum.getOrElse(i) { 0.08f }.coerceIn(0.06f, 1f)
                    val bh = (lvl * size.height).coerceIn(3f, size.height)
                    val x = i * (bw + gap)
                    val y = size.height - bh
                    val frac = i.toFloat() / (n - 1)
                    val c = androidx.compose.ui.graphics.lerp(GlassTokens.IosOrange, GlassTokens.IosTeal, frac)
                    drawRoundRect(
                        color = c.copy(alpha = if (active) 0.28f else 0.08f),
                        topLeft = Offset(x - 1f, y - 2f),
                        size = Size(bw + 2f, bh + 2f),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = if (active) 0.9f else 0.25f), c),
                            startY = y,
                            endY = y + bh
                        ),
                        topLeft = Offset(x, y),
                        size = Size(bw, bh),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
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

        // Real playback progress when a local track reports position, otherwise an
        // RMS-driven idle shimmer (honest: no fake 24s loop pretending to be a seek bar).
        val hasRealProgress = durationMs > 0L && positionMs >= 0L
        val realFraction = if (hasRealProgress) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        val idleFraction = 0.30f + audioRms.coerceIn(0f, 0.6f) * 0.5f
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(if (hasRealProgress) realFraction.coerceAtLeast(0.02f) else idleFraction)
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = band.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
    // Studio peak-hold caps: fall slowly so transients leave a pro trail.
    val peaks = remember { FloatArray(8) { 0.05f } }
    for (i in 0 until 8) {
        val lvl = if (active) spectrum.getOrElse(i) { 0.05f }.coerceIn(0.03f, 1f) else 0.04f
        peaks[i] = if (lvl >= peaks[i]) lvl else (peaks[i] - 0.022f).coerceAtLeast(lvl)
    }
    val freqLabels = listOf("60", "150", "400", "1k", "2.5k", "5k", "8k", "12k")
    val peakDb = if (active) (20f * kotlin.math.log10(spectrum.maxOrNull()?.toDouble()?.coerceAtLeast(1e-3) ?: 0.05)).toFloat().coerceIn(-48f, 0f) else -48f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusLg)
            .background(Color(0xFF07080C))
            .border(0.8.dp, Color.White.copy(alpha = 0.10f), GlassTokens.radiusLg)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (active) GlassTokens.IosGreen else GlassTokens.TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "SPECTRUM ANALYZER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (!active) "STANDBY" else "PEAK ${String.format(java.util.Locale.US, "%+.0f", peakDb)} dB",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!active) GlassTokens.TextSecondary else GlassTokens.IosTeal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            ) {
                val w = size.width
                val h = size.height * 0.82f
                val refTop = 0f
                // Studio grid: -12/-24/-36 dB lines
                val gridAlphas = listOf(0.16f, 0.10f, 0.07f)
                for (g in 0..2) {
                    val y = refTop + (h / 3f) * (g + 1)
                    drawLine(
                        color = Color.White.copy(alpha = gridAlphas[g]),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }
                val n = 8
                val gap = 8.dp.toPx()
                val barW = (w - gap * (n - 1)) / n
                for (index in 0 until n) {
                    val lvl = if (active) spectrum.getOrElse(index) { 0.05f }.coerceIn(0.03f, 1f) else 0.04f
                    val peak = peaks[index].coerceIn(0.03f, 1f)
                    val x = index * (barW + gap)
                    val barH = (lvl * h).coerceAtLeast(4f)
                    val y = refTop + h - barH
                    val fraction = index.toFloat() / (n - 1).coerceAtLeast(1)
                    val base = androidx.compose.ui.graphics.lerp(GlassTokens.IosOrange, GlassTokens.IosTeal, fraction)
                    val top = androidx.compose.ui.graphics.lerp(base, Color.White, 0.25f)
                    // Outer neon glow
                    drawRoundRect(
                        color = base.copy(alpha = if (active) 0.22f else 0.05f),
                        topLeft = Offset(x - 3f, y - 6f),
                        size = Size(barW + 6f, barH + 8f),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    // Main gradient body
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(top, base, base.copy(alpha = 0.55f)),
                            startY = y,
                            endY = y + barH
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    // Hot top edge
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (active) 0.85f else 0.15f),
                        topLeft = Offset(x + 2f, y),
                        size = Size(barW - 4f, 2.5.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Falling peak cap
                    val peakY = (refTop + h - peak * h).coerceIn(refTop, refTop + h - 2f)
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (active) 0.9f else 0.2f),
                        topLeft = Offset(x, peakY),
                        size = Size(barW, 2.dp.toPx()),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                    // Floor reflection
                    val reflH = (barH * 0.22f).coerceAtMost(size.height - h - 16.dp.toPx())
                    if (reflH > 2f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(base.copy(alpha = 0.20f), Color.Transparent),
                                startY = refTop + h + 3f,
                                endY = refTop + h + 3f + reflH
                            ),
                            topLeft = Offset(x, refTop + h + 3f),
                            size = Size(barW, reflH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                freqLabels.forEach {
                    Text(
                        text = it,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary.copy(alpha = 0.75f)
                    )
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
    // Idle shimmer sweep so the stage feels alive even on static material.
    val shimmer = rememberInfiniteTransition(label = "stage_shimmer")
    val shimmerT by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stage_shimmer_t"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF07080C))
            .border(0.6.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosIndigo)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SOUNDSTAGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val widthLabel = when {
                    isBypassed -> "DIRECT"
                    animatedSpace < 30f -> "NARROW"
                    animatedSpace < 65f -> "NATURAL"
                    else -> "WIDE"
                }
                Text(
                    text = if (isBypassed) "Direct Stereo (Bypassed)" else "${(30 + animatedSpace * 0.9f).toInt()}° • $widthLabel • $hrtfProfile",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosTeal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val cx = size.width / 2f
                val cy = size.height * 0.80f

                // Depth rings with center glow
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            (if (isBypassed) Color.White else GlassTokens.IosIndigo).copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = size.height * 0.95f
                    ),
                    radius = size.height * 0.95f,
                    center = Offset(cx, cy)
                )
                for (r in listOf(0.35f, 0.65f, 0.95f)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = size.height * r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f)
                    )
                }

                val sweepAngle = 35f + (animatedSpace * 0.95f)
                val startAngle = 270f - (sweepAngle / 2f)
                val arcRadius = size.height * 0.72f

                // Field fan: glow pass + core pass
                drawArc(
                    color = (if (isBypassed) Color.White else GlassTokens.IosIndigo).copy(alpha = 0.10f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(cx - arcRadius, cy - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2)
                )
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isBypassed) Color.White.copy(alpha = 0.06f)
                            else GlassTokens.IosIndigo.copy(alpha = 0.30f),
                            if (isBypassed) Color.Transparent
                            else GlassTokens.IosTeal.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = arcRadius
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(cx - arcRadius, cy - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2)
                )
                // Arc outline: soft under-glow + crisp core
                drawArc(
                    color = GlassTokens.IosTeal.copy(alpha = 0.25f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - arcRadius, cy - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 5.dp.toPx())
                )
                drawArc(
                    brush = Brush.horizontalGradient(
                        listOf(Color.White, GlassTokens.IosTeal, GlassTokens.IosIndigo, GlassTokens.IosBlue)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(cx - arcRadius, cy - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Traveling shimmer dot along the arc
                if (!isBypassed) {
                    val dotAngle = startAngle + sweepAngle * shimmerT
                    val rad = Math.toRadians(dotAngle.toDouble())
                    val dx = (cx + arcRadius * Math.cos(rad)).toFloat()
                    val dy = (cy + arcRadius * Math.sin(rad)).toFloat()
                    drawCircle(color = Color.White.copy(alpha = 0.25f), radius = 6.dp.toPx(), center = Offset(dx, dy))
                    drawCircle(color = Color.White, radius = 2.2.dp.toPx(), center = Offset(dx, dy))
                }

                // L/R speaker nodes with pulse rings
                val leftRad = Math.toRadians(startAngle.toDouble())
                val rightRad = Math.toRadians((startAngle + sweepAngle).toDouble())
                val lx = (cx + arcRadius * Math.cos(leftRad)).toFloat()
                val ly = (cy + arcRadius * Math.sin(leftRad)).toFloat()
                val rx = (cx + arcRadius * Math.cos(rightRad)).toFloat()
                val ry = (cy + arcRadius * Math.sin(rightRad)).toFloat()
                drawCircle(color = GlassTokens.IosTeal.copy(alpha = 0.22f), radius = 9.dp.toPx(), center = Offset(lx, ly))
                drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 3.dp.toPx(), center = Offset(lx, ly))
                drawCircle(color = GlassTokens.IosTeal, radius = 1.8.dp.toPx(), center = Offset(lx, ly))
                drawCircle(color = GlassTokens.IosIndigo.copy(alpha = 0.22f), radius = 9.dp.toPx(), center = Offset(rx, ry))
                drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 3.dp.toPx(), center = Offset(rx, ry))
                drawCircle(color = GlassTokens.IosIndigo, radius = 1.8.dp.toPx(), center = Offset(rx, ry))

                // Listener head: glass dome + highlight
                drawCircle(color = Color(0xFF1C1E24), radius = 10.dp.toPx(), center = Offset(cx, cy))
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(cx - 3.dp.toPx(), cy - 4.dp.toPx()),
                        radius = 10.dp.toPx()
                    ),
                    radius = 10.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(color = Color.White.copy(alpha = 0.18f), radius = 10.dp.toPx(), center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
                drawRoundRect(
                    color = if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosBlue,
                    topLeft = Offset(cx - 12.dp.toPx(), cy - 4.dp.toPx()),
                    size = Size(2.5.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
                drawRoundRect(
                    color = if (isBypassed) GlassTokens.TextSecondary else GlassTokens.IosBlue,
                    topLeft = Offset(cx + 9.5.dp.toPx(), cy - 4.dp.toPx()),
                    size = Size(2.5.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("MONO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = GlassTokens.TextSecondary.copy(alpha = 0.6f))
                Text("STEREO WIDTH →", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = GlassTokens.TextSecondary.copy(alpha = 0.6f))
            }
        }
    }
}

/**
 * Professional Studio LED Gain Reduction Meter
 * High-precision ladder display with dynamic dB thresholds (-1dB to -20dB).
 * Reads MEASURED gain reduction from the DSP loop when available
 * ([measuredGrDb] >= 0); falls back to the RMS-vs-threshold estimate only
 * when the engine hasn't reported yet (e.g. never played).
 */
@Composable
fun LedCompressionMeter(
    thresholdDb: Float,
    ratio: Float,
    audioRms: Float,
    modifier: Modifier = Modifier,
    measuredGrDb: Float = -1f
) {
    val inputDb = (20f * kotlin.math.log10(audioRms.coerceAtLeast(0.001f))).coerceIn(-60f, 0f)
    val overThreshold = (inputDb - thresholdDb).coerceAtLeast(0f)
    val estimatedGrDb = if (overThreshold > 0f) overThreshold * (1f - (1f / ratio)) else 0f
    // Measured GR already carries hardware-style ballistics from the engine,
    // so it drives the ladder directly; the estimate keeps spring smoothing.
    val useMeasured = measuredGrDb >= 0f
    val targetGr = if (useMeasured) measuredGrDb else estimatedGrDb

    val animatedGr by animateFloatAsState(
        targetValue = targetGr,
        animationSpec = if (useMeasured) tween(durationMillis = 60) else spring(dampingRatio = 0.85f, stiffness = 400f),
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
                    text = if (useMeasured) "GAIN REDUCTION • MEASURED" else "GAIN REDUCTION • EST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextSecondary,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = if (animatedGr > 0.05f) "-${String.format(java.util.Locale.US, "%.1f", animatedGr)} dB" else "0.0 dB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (animatedGr > 6f) GlassTokens.IosRed
                            else if (animatedGr > 2f) GlassTokens.IosOrange
                            else GlassTokens.IosGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // 12-segment pro LED ladder with glow + scale
            Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
                val thresholds = listOf(1f, 2f, 3f, 4f, 5f, 6f, 8f, 10f, 12f, 14f, 16f, 20f)
                val n = thresholds.size
                val gap = 4.dp.toPx()
                val segW = (size.width - gap * (n - 1)) / n
                thresholds.forEachIndexed { idx, th ->
                    val isActive = animatedGr >= th
                    val base = when {
                        th >= 12f -> GlassTokens.IosRed
                        th >= 6f -> GlassTokens.IosOrange
                        else -> GlassTokens.IosGreen
                    }
                    val x = idx * (segW + gap)
                    if (isActive) {
                        drawRoundRect(
                            color = base.copy(alpha = 0.30f),
                            topLeft = Offset(x - 2f, -2f),
                            size = Size(segW + 4f, size.height + 4f),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )
                    }
                    drawRoundRect(
                        brush = if (isActive) Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.85f), base, base.copy(alpha = 0.6f)),
                            startY = 0f, endY = size.height
                        ) else Brush.verticalGradient(
                            listOf(base.copy(alpha = 0.14f), base.copy(alpha = 0.08f))
                        ),
                        topLeft = Offset(x, 0f),
                        size = Size(segW, size.height),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    if (isActive) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.9f),
                            topLeft = Offset(x + 2f, 1f),
                            size = Size(segW - 4f, 2f),
                            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1dB", fontSize = 8.sp, color = GlassTokens.TextSecondary)
                Text("6dB", fontSize = 8.sp, color = GlassTokens.TextSecondary)
                Text("12dB", fontSize = 8.sp, color = GlassTokens.TextSecondary)
                Text("20dB", fontSize = 8.sp, color = GlassTokens.TextSecondary)
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = if (isPlaying) "4.75 cm/s" else "STOPPED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlaying) GlassTokens.IosGreen else GlassTokens.TextMuted,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

            // Pro deck-bridge VU: glowing needle + scale instead of flat boxes
            Column(modifier = Modifier.fillMaxWidth()) {
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
                    Text(
                        text = if (!isPlaying) "-∞ dB" else String.format(
                            java.util.Locale.US, "%+.1f dB",
                            (20f * kotlin.math.log10(audioRms.coerceAtLeast(0.005f).toDouble())).toFloat().coerceIn(-20f, 3f)
                        ),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4A373)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                    val w = size.width
                    val h = size.height
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.55f),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    // Warm zone wash: green → amber → red
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF30D158).copy(alpha = 0.35f),
                                Color(0xFFFF9F0A).copy(alpha = 0.40f),
                                Color(0xFFFF453A).copy(alpha = 0.45f)
                            )
                        ),
                        topLeft = Offset(2f, 2f),
                        size = Size(w - 4f, h - 4f)
                    )
                    // Scale ticks
                    for (t in 0..10) {
                        val x = w * t / 10f
                        drawLine(
                            color = Color.Black.copy(alpha = 0.55f),
                            start = Offset(x, 2f), end = Offset(x, h - 2f),
                            strokeWidth = 1f
                        )
                    }
                    val norm = ((audioRms.coerceIn(0f, 0.9f)) / 0.9f).coerceIn(0f, 1f)
                    val nx = w * (if (isPlaying) norm else 0.02f)
                    // Glow + needle
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.35f),
                        topLeft = Offset(nx - 7f, -2f),
                        size = Size(14f, h + 4f),
                        cornerRadius = CornerRadius(7f, 7f)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(nx, 0f), end = Offset(nx, h),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
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
            .height(182.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF07080C))
            .border(0.6.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height - 16.dp.toPx()
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

            // Studio grid: dB lines + log freq lines
            val dBLines = listOf(12f, 6f, 0f, -6f, -12f)
            for (db in dBLines) {
                val y = dbToY(db)
                val isZero = db == 0f
                drawLine(
                    color = if (isZero) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.07f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (isZero) 1.2.dp.toPx() else 1f
                )
            }
            val fGrid = listOf(50f, 100f, 200f, 500f, 1000f, 2000f, 5000f, 10000f)
            for (f in fGrid) {
                val x = freqToX(f)
                val major = f == 100f || f == 1000f || f == 10000f
                drawLine(
                    color = Color.White.copy(alpha = if (major) 0.12f else 0.05f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
            }

            // Q bandwidth shading per active band (pro touch: shows resonance width)
            for (b in bands) {
                if (kotlin.math.abs(b.gainDb) > 0.4f) {
                    val cx = freqToX(b.hz.toFloat())
                    val bwOct = (1.2f / b.q.coerceIn(0.3f, 10f)).coerceIn(0.12f, 2.5f)
                    val fLo = (b.hz / Math.pow(2.0, (bwOct / 2).toDouble())).toFloat()
                    val fHi = (b.hz * Math.pow(2.0, (bwOct / 2).toDouble())).toFloat()
                    val x0 = freqToX(fLo.coerceIn(20f, 20000f))
                    val x1 = freqToX(fHi.coerceIn(20f, 20000f))
                    drawRoundRect(
                        color = GlassTokens.IosTeal.copy(alpha = 0.10f),
                        topLeft = Offset(x0, 0f),
                        size = Size((x1 - x0).coerceAtLeast(4f), h),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // Dual Real-Time Analyzer (Pre vs Post RTA Spectrum Overlay)
            val rtaFreqs = listOf(35f, 90f, 220f, 550f, 1400f, 3600f, 8500f, 16000f)
            val rtaBinWidth = (w / 14f).coerceAtLeast(12.dp.toPx())
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

                val preBarHeight = (rawEnergy * (h * 0.62f)).coerceAtLeast(3f)
                drawRoundRect(
                    color = GlassTokens.IosBlue.copy(alpha = if (active) 0.20f else 0.06f),
                    topLeft = Offset(binX - rtaBinWidth * 0.42f, h - preBarHeight),
                    size = Size(rtaBinWidth * 0.84f, preBarHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                val postBarHeight = (postEnergy * (h * 0.62f)).coerceAtLeast(2f)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (active) 0.55f else 0.12f),
                            GlassTokens.IosTeal.copy(alpha = if (active) 0.50f else 0.10f)
                        ),
                        startY = h - postBarHeight,
                        endY = h
                    ),
                    topLeft = Offset(binX - rtaBinWidth * 0.25f, h - postBarHeight),
                    size = Size(rtaBinWidth * 0.50f, postBarHeight),
                    cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                )
            }

            // Compute composite transfer function across 120 sample points
            val numSamples = 120
            val curvePoints = mutableListOf<Offset>()
            for (i in 0..numSamples) {
                val norm = i / numSamples.toFloat()
                val minF = 20.0
                val maxF = 20000.0
                val f = (minF * Math.pow(maxF / minF, norm.toDouble())).toFloat()
                var totalGainDb = 0f
                for (b in bands) {
                    if (kotlin.math.abs(b.gainDb) > 0.05f) {
                        val octDiff = kotlin.math.log2(f.toDouble() / b.hz.toDouble()).toFloat()
                        val qFactor = b.q.coerceAtLeast(0.2f)
                        val denom = 1f + (octDiff * qFactor * 2.2f) * (octDiff * qFactor * 2.2f)
                        totalGainDb += b.gainDb / denom
                    }
                }
                curvePoints.add(Offset(norm * w, dbToY(totalGainDb)))
            }

            if (curvePoints.isNotEmpty()) {
                val fillPath = Path().apply {
                    moveTo(curvePoints.first().x, centerY)
                    for (pt in curvePoints) lineTo(pt.x, pt.y)
                    lineTo(curvePoints.last().x, centerY)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassTokens.IosTeal.copy(alpha = 0.30f),
                            GlassTokens.IosBlue.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                val strokePath = Path().apply {
                    moveTo(curvePoints.first().x, curvePoints.first().y)
                    for (i in 1 until curvePoints.size) {
                        val p0 = curvePoints[i - 1]
                        val p1 = curvePoints[i]
                        val midX = (p0.x + p1.x) / 2f
                        val midY = (p0.y + p1.y) / 2f
                        quadraticTo(p0.x, p0.y, midX, midY)
                    }
                    lineTo(curvePoints.last().x, curvePoints.last().y)
                }
                // Outer glow pass
                drawPath(
                    path = strokePath,
                    color = GlassTokens.IosTeal.copy(alpha = 0.28f),
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )
                // Core neon pass
                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, GlassTokens.IosTeal, GlassTokens.IosBlue)
                    ),
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 10 Band Node Markers with glow
            for (b in bands) {
                val nodeX = freqToX(b.hz.toFloat())
                val nodeY = dbToY(b.gainDb)
                val isActive = kotlin.math.abs(b.gainDb) > 0.5f
                drawCircle(
                    color = (if (isActive) GlassTokens.IosTeal else Color.White).copy(alpha = if (isActive) 0.30f else 0.10f),
                    radius = if (isActive) 9.dp.toPx() else 6.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.6f),
                    radius = 4.6.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
                drawCircle(
                    color = if (isActive) Color.White else Color(0xFF8E8E93),
                    radius = 3.2.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
                if (isActive) {
                    drawCircle(
                        color = GlassTokens.IosTeal,
                        radius = 1.6.dp.toPx(),
                        center = Offset(nodeX, nodeY)
                    )
                }
            }
            // Freq labels drawn in overlay Row below (kept out of Canvas for font clarity)
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

            // Ring 1 (Outer): Score (Apple Cyan/Blue) with glow
            val r1 = (size.width / 2f) - strokeW / 2f
            drawCircle(
                color = GlassTokens.IosBlue.copy(alpha = 0.15f),
                radius = r1,
                style = Stroke(width = strokeW)
            )
            if (animatedScoreSweep > 0f) {
                drawArc(
                    color = GlassTokens.IosBlue.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = animatedScoreSweep,
                    useCenter = false,
                    style = Stroke(width = strokeW + 5.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(center.x - r1, center.y - r1),
                    size = Size(r1 * 2f, r1 * 2f)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color.White, GlassTokens.IosBlue, GlassTokens.IosTeal),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedScoreSweep,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - r1, center.y - r1),
                    size = Size(r1 * 2f, r1 * 2f)
                )
            }

            // Ring 2 (Middle): Streak (Apple Green) with glow
            val r2 = r1 - strokeW - 2.5.dp.toPx()
            drawCircle(
                color = GlassTokens.IosGreen.copy(alpha = 0.15f),
                radius = r2,
                style = Stroke(width = strokeW)
            )
            if (animatedStreakSweep > 0f) {
                drawArc(
                    color = GlassTokens.IosGreen.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = animatedStreakSweep,
                    useCenter = false,
                    style = Stroke(width = strokeW + 5.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(center.x - r2, center.y - r2),
                    size = Size(r2 * 2f, r2 * 2f)
                )
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

            // Ring 3 (Inner): Accuracy / Challenges (Apple Coral Red) with glow
            val r3 = r2 - strokeW - 2.5.dp.toPx()
            drawCircle(
                color = GlassTokens.IosRed.copy(alpha = 0.15f),
                radius = r3,
                style = Stroke(width = strokeW)
            )
            if (animatedAccuracySweep > 0f) {
                drawArc(
                    color = GlassTokens.IosRed.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = animatedAccuracySweep,
                    useCenter = false,
                    style = Stroke(width = strokeW + 5.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(center.x - r3, center.y - r3),
                    size = Size(r3 * 2f, r3 * 2f)
                )
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = if (isPlaying) "STYLUS TRACKING" else "TONEARM RESTED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlaying) GlassTokens.IosGreen else GlassTokens.TextMuted,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

                        // Center Record Label with gloss highlight
                        val labelRadius = platterRadius * 0.38f
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(Color(0xFFE05A47), Color(0xFFFF9F0A), Color(0xFFE05A47)),
                                center = platterCenter
                            ),
                            radius = labelRadius,
                            center = platterCenter
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                                center = Offset(platterCenter.x - labelRadius * 0.35f, platterCenter.y - labelRadius * 0.35f),
                                radius = labelRadius
                            ),
                            radius = labelRadius,
                            center = platterCenter
                        )
                        // Rotating light streak across grooves (vinyl sheen sweep)
                        rotate(degrees = if (isPlaying) rotationAngle * 1.0f else 0f, pivot = platterCenter) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent),
                                    center = platterCenter
                                ),
                                startAngle = 0f,
                                sweepAngle = 70f,
                                useCenter = false,
                                topLeft = Offset(platterCenter.x - platterRadius, platterCenter.y - platterRadius),
                                size = Size(platterRadius * 2f, platterRadius * 2f),
                                style = Stroke(width = platterRadius * 0.55f)
                            )
                        }

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
                    color = GlassTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = "ANALOG PHONO PREAMP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextMuted,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                color = GlassTokens.IosTeal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                    // Honest indeterminate state: no position exists for synth/external
                    // sources, so show an RMS-driven shimmer instead of a fake 75%.
                    val idleFrac = (0.22f + audioRms.coerceIn(0f, 0.6f) * 0.6f).coerceIn(0.15f, 0.85f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(GlassTokens.radiusPill)
                            .background(Color(0xFF3A3A3C))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (isPlaying || isExternalActive) idleFrac else 0.25f)
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
                        Text(if (isExternalActive) "External audio • no capture" else "Demo synth • no timeline", fontSize = 11.sp, color = GlassTokens.TextMuted, fontWeight = FontWeight.Medium)
                        Text(if (isExternalActive) "Meter unavailable" else "Infinite", fontSize = 11.sp, color = GlassTokens.TextMuted, fontWeight = FontWeight.Medium)
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
                            width = 1.2.dp,
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
                                    Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        Spacer(modifier = Modifier.height(7.dp))
                        // Pro mini 5-band EQ preview carved from the preset curve
                        Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                            val vals = listOf(target.rumbleDb, target.warmthDb, target.bodyDb, target.clarityDb, target.airDb)
                            val n = vals.size
                            val gap = 4.dp.toPx()
                            val bw = (size.width - gap * (n - 1)) / n
                            vals.forEachIndexed { i, db ->
                                val norm = ((db + 6f) / 12f).coerceIn(0.08f, 1f)
                                val bh = (norm * size.height).coerceAtLeast(3f)
                                val x = i * (bw + gap)
                                val y = size.height - bh
                                val frac = i.toFloat() / (n - 1)
                                val c = androidx.compose.ui.graphics.lerp(GlassTokens.IosOrange, GlassTokens.IosTeal, frac)
                                drawRoundRect(
                                    color = if (isSelected) c.copy(alpha = 0.85f) else c.copy(alpha = 0.35f),
                                    topLeft = Offset(x, y),
                                    size = Size(bw, bh),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                                if (isSelected) {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.8f),
                                        topLeft = Offset(x + 1.5f, y),
                                        size = Size(bw - 3f, 1.6.dp.toPx()),
                                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                                    )
                                }
                            }
                        }
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
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            color = GlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            color = if (isLimiterActive) GlassTokens.IosBlue else GlassTokens.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Pro SPL exposure gauge 58..96dB with safe/moderate/loud zones
        Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
            val w = size.width
            val h = size.height
            fun xFor(spl: Int): Float = ((spl - 58f) / (96f - 58f)).coerceIn(0f, 1f) * w
            drawRoundRect(
                color = Color.White.copy(alpha = 0.07f),
                topLeft = Offset(0f, 0f), size = Size(w, h),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
            drawRect(color = GlassTokens.IosGreen.copy(alpha = 0.35f), topLeft = Offset(xFor(58), 0f), size = Size(xFor(75) - xFor(58), h))
            drawRect(color = GlassTokens.IosOrange.copy(alpha = 0.40f), topLeft = Offset(xFor(75), 0f), size = Size(xFor(85) - xFor(75), h))
            drawRect(color = GlassTokens.IosRed.copy(alpha = 0.45f), topLeft = Offset(xFor(85), 0f), size = Size(xFor(96) - xFor(85), h))
            val nx = xFor(dBSpl)
            drawRoundRect(
                color = statusColor.copy(alpha = 0.35f),
                topLeft = Offset(nx - 8f, -2f), size = Size(16f, h + 4f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color.White, statusColor)),
                topLeft = Offset(nx - 3.5f, 0f), size = Size(7f, h),
                cornerRadius = CornerRadius(3.5f, 3.5f)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("58 QUIET", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = GlassTokens.TextSecondary.copy(alpha = 0.6f))
            Text("75 / 85 LIMITS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = GlassTokens.TextSecondary.copy(alpha = 0.6f))
            Text("96 LOUD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = GlassTokens.TextSecondary.copy(alpha = 0.6f))
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

        // Meter Track with glow needle + tick marks + trail
        // Short trail so movement reads as analog ballistics, not a jumping pill.
        val trail = remember { FloatArray(12) { 0.85f } }
        val normNow = ((animatedCorr + 1.0f) / 2.0f).coerceIn(0f, 1f)
        for (i in trail.size - 1 downTo 1) trail[i] = trail[i - 1]
        trail[0] = normNow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(GlassTokens.radiusPill)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF07080C), Color(0xFF14161D))
                    )
                )
                .border(
                    0.8.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.06f))
                    ),
                    GlassTokens.radiusPill
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midX = w / 2f
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFF453A).copy(alpha = 0.40f), Color(0xFFFFD60A).copy(alpha = 0.28f)),
                        startX = 0f, endX = midX
                    ),
                    topLeft = Offset(0f, 0f), size = Size(midX, h)
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFFD60A).copy(alpha = 0.28f), Color(0xFF30D158).copy(alpha = 0.40f)),
                        startX = midX, endX = w
                    ),
                    topLeft = Offset(midX, 0f), size = Size(w - midX, h)
                )
                // Tick marks at -1/-0.5/0/+0.5/+1
                for (t in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                    val x = w * t
                    drawLine(
                        color = Color.White.copy(alpha = if (t == 0.5f) 0.7f else 0.30f),
                        start = Offset(x, 0f), end = Offset(x, h),
                        strokeWidth = if (t == 0.5f) 2f else 1f
                    )
                }
                // Fading trail
                for (i in trail.size - 1 downTo 1) {
                    val x = w * trail[i].coerceIn(0f, 1f)
                    drawRoundRect(
                        color = statusColor.copy(alpha = 0.05f + (1f - i.toFloat() / trail.size) * 0.10f),
                        topLeft = Offset(x - 5f, 1f),
                        size = Size(10f, h - 2f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                }
                // Glow + hot core needle
                val nx = w * normNow.coerceIn(0f, 1f)
                drawRoundRect(
                    color = statusColor.copy(alpha = 0.35f),
                    topLeft = Offset(nx - 8f, -2f),
                    size = Size(16f, h + 4f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White, statusColor)),
                    topLeft = Offset(nx - 4f, 0f),
                    size = Size(8f, h),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.95f),
                    start = Offset(nx, 1f), end = Offset(nx, h - 1f),
                    strokeWidth = 1.5f
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
            .background(Color(0xFF07080C))
            .border(
                0.8.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.05f))
                ),
                GlassTokens.radiusMd
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                        text = "DYNAMIC RANGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (peakDbfs <= -90f) "DR -- dB" else String.format("DR %.1f dB", crestFactorDb.coerceIn(0f, 30f)),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = ratingColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(GlassTokens.radiusPill)
                                .background(ratingColor.copy(alpha = 0.15f))
                                .border(0.6.dp, ratingColor.copy(alpha = 0.5f), GlassTokens.radiusPill)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (peakDbfs <= -90f) "Idle" else ratingText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ratingColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (peakDbfs <= -90f) "Peak: -∞ dBFS" else String.format("Peak: %+.1f dBFS", peakDbfs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (peakDbfs >= -0.1f) GlassTokens.IosRed else GlassTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (rmsDbfs <= -90f) "RMS: -∞ dBFS" else String.format("RMS: %+.1f dBFS", rmsDbfs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = GlassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Pro DR gauge 0..20dB with zone colors + glowing needle
            Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.07f),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                // Zones: 0-6 red, 6-9 yellow, 9-14 green, 14-20 teal
                val zones = listOf(
                    0f to 0.3f to GlassTokens.IosRed,
                    0.3f to 0.45f to GlassTokens.IosYellow,
                    0.45f to 0.7f to GlassTokens.IosGreen,
                    0.7f to 1f to GlassTokens.IosTeal
                )
                for ((range, c) in zones) {
                    val (a, b) = range
                    drawRect(
                        color = c.copy(alpha = 0.30f),
                        topLeft = Offset(w * a, 0f),
                        size = Size(w * (b - a), h)
                    )
                }
                val norm = (crestFactorDb.coerceIn(0f, 20f) / 20f).coerceIn(0f, 1f)
                val nx = w * norm
                drawRoundRect(
                    color = ratingColor.copy(alpha = 0.35f),
                    topLeft = Offset(nx - 9f, -3f),
                    size = Size(18f, h + 6f),
                    cornerRadius = CornerRadius(9f, 9f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White, ratingColor)),
                    topLeft = Offset(nx - 4f, 0f),
                    size = Size(8f, h),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0 SQUASHED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = GlassTokens.TextSecondary.copy(alpha = 0.6f))
                Text("20 OPEN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = GlassTokens.TextSecondary.copy(alpha = 0.6f))
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
                        color = deltaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                            color = if (isSelected) Color.White else GlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        color = if (metrics.integratedLufs > currentTarget.targetLufs + 0.5f) GlassTokens.IosOrange else GlassTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // True Peak Meter & Loudness Range
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                    val tp = metrics.truePeakDbtp
                    val isTpClipping = tp > currentTarget.maxTruePeakDbtp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "True Peak: ",
                            fontSize = 11.sp,
                            color = GlassTokens.TextSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = if (tp <= -60f) "-∞ dBTP" else String.format("%+.1f dBTP", tp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTpClipping) GlassTokens.IosRed else GlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format("LRA: %.1f LU (Range)", metrics.loudnessRangeLu),
                        fontSize = 11.sp,
                        color = GlassTokens.IosTeal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        color = GlassTokens.TextSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = if (metrics.momentaryLufs <= -60f) "-∞" else String.format("%.1f", metrics.momentaryLufs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Short-Term (3s): ",
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = if (metrics.shortTermLufs <= -60f) "-∞" else String.format("%.1f", metrics.shortTermLufs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pro loudness bar meters with streaming-target marker (-40..-6 LUFS window)
            @Composable
            fun LufsBar(label: String, value: Float, accent: Color) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GlassTokens.TextSecondary)
                        Text(
                            text = if (value <= -60f) "-∞" else String.format("%.1f LUFS", value),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlassTokens.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                        val lo = -40f
                        val hi = -6f
                        fun xFor(lufs: Float): Float {
                            val n = ((lufs - lo) / (hi - lo)).coerceIn(0f, 1f)
                            return n * size.width
                        }
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.07f),
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )
                        val vw = xFor(value)
                        if (vw > 2f) {
                            drawRoundRect(
                                color = accent.copy(alpha = 0.30f),
                                topLeft = Offset(0f, -2f),
                                size = Size(vw, size.height + 4f),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    listOf(accent.copy(alpha = 0.6f), accent, Color.White.copy(alpha = 0.85f)),
                                    startX = 0f, endX = vw
                                ),
                                topLeft = Offset(0f, 0f),
                                size = Size(vw, size.height),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                        }
                        val tx = xFor(currentTarget.targetLufs)
                        drawLine(
                            color = Color.White.copy(alpha = 0.9f),
                            start = Offset(tx, -3f),
                            end = Offset(tx, size.height + 3f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
            LufsBar("MOMENTARY", metrics.momentaryLufs, GlassTokens.IosTeal)
            Spacer(modifier = Modifier.height(6.dp))
            LufsBar("SHORT-TERM 3s", metrics.shortTermLufs, GlassTokens.IosBlue)
            Spacer(modifier = Modifier.height(6.dp))
            LufsBar("INTEGRATED", metrics.integratedLufs, GlassTokens.IosGreen)
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
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = if (currentReductionDb > 0.05f) String.format("-%.1f dB GR", currentReductionDb) else "0.0 dB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentReductionDb > 0.05f) GlassTokens.IosPurple else GlassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                            color = GlassTokens.IosRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            color = if (isSelected) (if (mode == TestToneMode.OFF) Color.White else Color.Black) else GlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        lineHeight = 15.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
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
                        color = GlassTokens.IosTeal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else GlassTokens.TextPrimary,
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
                            color = GlassTokens.IosRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            color = GlassTokens.IosGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

                    // 3. Phosphor Vector Cloud with bloom (glow pass + hot core pass)
                    val numPoints = points.size / 2
                    if (numPoints > 1) {
                        val phosphorColor = if (isAntiPhase) Color(0xFFFF453A) else Color(0xFF00F5D4)
                        // Bloom under-glow: thick soft trail
                        for (i in 1 until numPoints step 2) {
                            val px = points[i * 2]
                            val py = points[i * 2 + 1]
                            val screenX = cx + px * radius
                            val screenY = cy - py * radius
                            val prevX = cx + points[(i - 1) * 2] * radius
                            val prevY = cy - points[(i - 1) * 2 + 1] * radius
                            val progress = i.toFloat() / numPoints.toFloat()
                            drawLine(
                                color = phosphorColor.copy(alpha = 0.10f + progress * 0.18f),
                                start = Offset(prevX, prevY),
                                end = Offset(screenX, screenY),
                                strokeWidth = 5f
                            )
                        }
                        // Core trail + phosphor dots
                        for (i in 0 until numPoints) {
                            val px = points[i * 2]
                            val py = points[i * 2 + 1]
                            val screenX = cx + px * radius
                            val screenY = cy - py * radius
                            val progress = i.toFloat() / numPoints.toFloat()
                            val alpha = 0.20f + progress * 0.75f
                            if (i > 0) {
                                val prevX = cx + points[(i - 1) * 2] * radius
                                val prevY = cy - points[(i - 1) * 2 + 1] * radius
                                drawLine(
                                    color = phosphorColor.copy(alpha = alpha * 0.75f),
                                    start = Offset(prevX, prevY),
                                    end = Offset(screenX, screenY),
                                    strokeWidth = 1.6f
                                )
                            }
                            drawCircle(
                                color = Color.White.copy(alpha = alpha * 0.55f),
                                radius = if (i == numPoints - 1) 2.6f else 1.5f,
                                center = Offset(screenX, screenY)
                            )
                            drawCircle(
                                color = phosphorColor.copy(alpha = alpha),
                                radius = if (i == numPoints - 1) 4.2f else 2.2f,
                                center = Offset(screenX, screenY)
                            )
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
                    text = "Mono = vertical • Wide = ellipse • Anti-phase = horizontal",
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                        color = GlassTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = String.format("%+.1f dB", attackGainDb),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (attackPercent != 0f) Color(0xFF00FFCC) else GlassTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                        color = GlassTokens.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = String.format("%+.1f dB", sustainGainDb),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sustainPercent != 0f) GlassTokens.IosOrange else GlassTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                        color = if (thdPercent > 0.05f) GlassTokens.IosOrange else GlassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                            color = if (isSelected) Color.Black else GlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        color = GlassTokens.IosOrange,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedType.description,
                        fontSize = 10.sp,
                        color = GlassTokens.TextSecondary,
                        lineHeight = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
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

/**
 * Pro Studio mix workflow: A/B/C/D snapshots + undo.
 * One-tap full-chain recall for mix comparison without re-dialing sliders.
 */
@Composable
fun ProMixWorkflowCard(
    snapshots: List<com.example.model.MixSnapshot?>,
    activeIndex: Int,
    canUndo: Boolean,
    onSave: (Int) -> Unit,
    onRecall: (Int) -> Unit,
    onClear: (Int) -> Unit,
    onUndo: () -> Unit,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusSm)
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusSm)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MIX SNAPSHOTS A/B/C/D",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextSecondary
                )
                Box(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(if (canUndo) GlassTokens.IosBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .border(0.8.dp, if (canUndo) GlassTokens.IosBlue else GlassTokens.IosSeparator, GlassTokens.radiusPill)
                        .clickable(enabled = canUndo) { onUndo() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (canUndo) "↩ UNDO" else "NO UNDO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canUndo) GlassTokens.IosBlue else GlassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = "Capture the full chain (EQ, dynamics, reverb, saturation, monitors) and flip instantly.",
                fontSize = 11.sp,
                color = GlassTokens.TextSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val labels = listOf("A", "B", "C", "D")
                labels.forEachIndexed { idx, label ->
                    val filled = snapshots.getOrNull(idx) != null
                    val active = activeIndex == idx
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassTokens.radiusSm)
                                .background(
                                    when {
                                        active -> GlassTokens.IosTeal.copy(alpha = 0.3f)
                                        filled -> GlassTokens.IosBlue.copy(alpha = 0.18f)
                                        else -> Color.White.copy(alpha = 0.05f)
                                    }
                                )
                                .border(
                                    0.8.dp,
                                    when {
                                        active -> GlassTokens.IosTeal
                                        filled -> GlassTokens.IosBlue.copy(alpha = 0.6f)
                                        else -> GlassTokens.IosSeparator
                                    },
                                    GlassTokens.radiusSm
                                )
                                .clickable {
                                    if (filled) onRecall(idx) else onSave(idx)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filled || active) Color.White else GlassTokens.TextSecondary
                            )
                        }
                        Text(
                            text = when {
                                active && filled -> "Active"
                                filled -> "Recall"
                                else -> "Save"
                            },
                            fontSize = 9.sp,
                            color = GlassTokens.TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        if (filled) {
                            Text(
                                text = "hold to clear",
                                fontSize = 8.sp,
                                color = GlassTokens.TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .clickable { onClear(idx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pro Studio QC: sticky true-peak hold + clip counter + loudness auto-match
 * readout with offline WAV bounce action.
 *
 * The bounce button prints the loaded vault track when one is selected
 * (two-pass measure -> match -> verify), otherwise a 5s chain-audition tone.
 */
@Composable
fun QcPeakHoldCard(
    holdDbtp: Float,
    clipCount: Int,
    integratedLufs: Float,
    targetLufs: Float,
    autoGainDb: Float,
    isBouncing: Boolean,
    lastBounceInfo: String?,
    onReset: () -> Unit,
    onBounce: () -> Unit,
    modifier: Modifier = Modifier,
    hasLocalTrack: Boolean = false,
    trackTitle: String? = null,
    bitDepth: com.example.audio.BounceBitDepth = com.example.audio.BounceBitDepth.PCM_16,
    onSelectBitDepth: ((com.example.audio.BounceBitDepth) -> Unit)? = null,
    bounceProgress: Float? = null,
    verifyText: String? = null
) {
    val over = holdDbtp > -1.0f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusSm)
            .background(if (over) GlassTokens.IosRed.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
            .border(
                0.8.dp,
                if (over) GlassTokens.IosRed.copy(alpha = 0.5f) else GlassTokens.IosSeparator,
                GlassTokens.radiusSm
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QC PEAK-HOLD & CLIP COUNTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (over) GlassTokens.IosRed else GlassTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusPill)
                        .clickable { onReset() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("RESET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlassTokens.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Peak hold: ${String.format(java.util.Locale.US, "%.1f", holdDbtp)} dBTP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (over) GlassTokens.IosRed else GlassTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = "Overs: $clipCount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (clipCount > 0) GlassTokens.IosOrange else GlassTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Integrated ${String.format(java.util.Locale.US, "%.1f", integratedLufs)} LUFS → target ${String.format(java.util.Locale.US, "%.0f", targetLufs)} LUFS • auto-match ${String.format(java.util.Locale.US, "%+.1f", autoGainDb)} dB (true-peak safe)",
                fontSize = 11.sp,
                color = GlassTokens.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (onSelectBitDepth != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BOUNCE FORMAT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    com.example.audio.BounceBitDepth.values().forEach { depth ->
                        val selected = depth == bitDepth
                        Box(
                            modifier = Modifier
                                .clip(GlassTokens.radiusPill)
                                .background(if (selected) GlassTokens.IosTeal.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                .border(
                                    0.8.dp,
                                    if (selected) GlassTokens.IosTeal else GlassTokens.IosSeparator,
                                    GlassTokens.radiusPill
                                )
                                .clickable { onSelectBitDepth(depth) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${depth.bits}-bit WAV",
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) GlassTokens.IosTeal else GlassTokens.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onBounce,
                enabled = !isBouncing,
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosTeal),
                shape = GlassTokens.radiusPill,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = when {
                        isBouncing -> "Bouncing…"
                        hasLocalTrack -> "Bounce track (${bitDepth.bits}-bit WAV)"
                        else -> "Bounce 5s chain-audition tone"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasLocalTrack && trackTitle != null) {
                Text(
                    text = "Source: $trackTitle • monitors & M/S audition excluded from print",
                    fontSize = 10.sp,
                    color = GlassTokens.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isBouncing && bounceProgress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(GlassTokens.radiusPill)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(bounceProgress.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(GlassTokens.radiusPill)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(GlassTokens.IosTeal, GlassTokens.IosBlue)
                                )
                            )
                    )
                }
                Text(
                    text = "Rendering ${(bounceProgress * 100).toInt()}% (measure → match → print)",
                    fontSize = 10.sp,
                    color = GlassTokens.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (verifyText != null) {
                Text(
                    text = verifyText,
                    fontSize = 10.sp,
                    color = GlassTokens.IosTeal,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (lastBounceInfo != null) {
                Text(
                    text = lastBounceInfo,
                    fontSize = 10.sp,
                    color = GlassTokens.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Pro Studio 3-band multiband compressor card (FabFilter Pro-MB style).
 * Subtractive LR4 crossover, per-band threshold/ratio with measured GR,
 * linked attack/release, knee, sidechain HPF and per-band solo.
 * Player-only DSP - disclosed inline since there is no OS mapping.
 */
@Composable
fun MultibandDynamicsCard(
    enabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    xoverLowHz: Float,
    onXoverLowChange: (Float) -> Unit,
    xoverHighHz: Float,
    onXoverHighChange: (Float) -> Unit,
    threshLowDb: Float,
    threshMidDb: Float,
    threshHighDb: Float,
    onThreshChange: (Int, Float) -> Unit,
    ratioLow: Float,
    ratioMid: Float,
    ratioHigh: Float,
    onRatioChange: (Int, Float) -> Unit,
    attackMs: Float,
    onAttackChange: (Float) -> Unit,
    releaseMs: Float,
    onReleaseChange: (Float) -> Unit,
    kneeDb: Float,
    onKneeChange: (Float) -> Unit,
    sidechainHpfHz: Int,
    onSidechainHpfChange: (Int) -> Unit,
    soloLow: Boolean,
    soloMid: Boolean,
    soloHigh: Boolean,
    onSoloChange: (Int, Boolean) -> Unit,
    grLowDb: Float,
    grMidDb: Float,
    grHighDb: Float,
    modifier: Modifier = Modifier,
    reduceGlass: Boolean = false
) {
    val bandNames = listOf("LOW", "MID", "HIGH")
    val bandThresh = listOf(threshLowDb, threshMidDb, threshHighDb)
    val bandRatio = listOf(ratioLow, ratioMid, ratioHigh)
    val bandGr = listOf(grLowDb, grMidDb, grHighDb)
    val bandSolo = listOf(soloLow, soloMid, soloHigh)
    val bandTint = listOf(GlassTokens.IosOrange, GlassTokens.IosTeal, GlassTokens.IosBlue)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassTokens.radiusMd)
            .background(Color(0xFF07080C))
            .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusMd)
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
                        text = "MULTIBAND DYNAMICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "3-band LR4 • per-band GR • solo",
                        fontSize = 10.sp,
                        color = GlassTokens.TextSecondary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GlassTokens.IosOrange
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Player-only stage: Android's DynamicsProcessing is single-band, so this shapes Daydream's player (and bounces), not system audio.",
                fontSize = 10.sp,
                color = GlassTokens.TextSecondary.copy(alpha = 0.8f),
                lineHeight = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            LiquidSlider(
                title = "Crossover Low/Mid",
                value = xoverLowHz,
                onValueChange = onXoverLowChange,
                valueRange = 60f..800f,
                unit = "Hz",
                technicalValue = "LR4 low edge",
                showTechnical = true,
                accentColor = GlassTokens.IosOrange,
                reduceGlass = reduceGlass
            )
            LiquidSlider(
                title = "Crossover Mid/High",
                value = xoverHighHz,
                onValueChange = onXoverHighChange,
                valueRange = 1000f..12000f,
                unit = "Hz",
                technicalValue = "LR4 high edge",
                showTechnical = true,
                accentColor = GlassTokens.IosBlue,
                reduceGlass = reduceGlass
            )

            Spacer(modifier = Modifier.height(6.dp))
            bandNames.forEachIndexed { idx, name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$name BAND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = bandTint[idx],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "GR -${String.format(java.util.Locale.US, "%.1f", bandGr[idx])} dB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bandGr[idx] > 0.1f) bandTint[idx] else GlassTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(GlassTokens.radiusPill)
                            .background(if (bandSolo[idx]) bandTint[idx].copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                0.8.dp,
                                if (bandSolo[idx]) bandTint[idx] else GlassTokens.IosSeparator,
                                GlassTokens.radiusPill
                            )
                            .clickable { onSoloChange(idx, !bandSolo[idx]) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (bandSolo[idx]) "SOLO ON" else "SOLO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bandSolo[idx]) bandTint[idx] else GlassTokens.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                LiquidSlider(
                    title = "$name Threshold",
                    value = bandThresh[idx],
                    onValueChange = { onThreshChange(idx, it) },
                    valueRange = -40f..0f,
                    unit = "dB",
                    showTechnical = false,
                    accentColor = bandTint[idx],
                    reduceGlass = reduceGlass
                )
                LiquidSlider(
                    title = "$name Ratio",
                    value = bandRatio[idx],
                    onValueChange = { onRatioChange(idx, it) },
                    valueRange = 1f..10f,
                    unit = ":1",
                    showTechnical = false,
                    accentColor = bandTint[idx],
                    reduceGlass = reduceGlass
                )
                if (idx < 2) {
                    IosRowSeparator(modifier = Modifier.padding(vertical = 6.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            LiquidSlider(
                title = "Attack (linked)",
                value = attackMs,
                onValueChange = onAttackChange,
                valueRange = 1f..100f,
                unit = "ms",
                showTechnical = false,
                accentColor = GlassTokens.IosOrange,
                reduceGlass = reduceGlass
            )
            LiquidSlider(
                title = "Release (linked)",
                value = releaseMs,
                onValueChange = onReleaseChange,
                valueRange = 10f..500f,
                unit = "ms",
                showTechnical = false,
                accentColor = GlassTokens.IosOrange,
                reduceGlass = reduceGlass
            )
            LiquidSlider(
                title = "Knee",
                value = kneeDb,
                onValueChange = onKneeChange,
                valueRange = 0f..12f,
                unit = "dB",
                technicalValue = if (kneeDb < 0.5f) "Hard knee" else "Soft knee",
                showTechnical = true,
                accentColor = GlassTokens.IosTeal,
                reduceGlass = reduceGlass
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SIDECHAIN HIGHPASS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Off", 80 to "80 Hz", 150 to "150 Hz").forEach { (hz, label) ->
                    val selected = sidechainHpfHz == hz
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(GlassTokens.radiusPill)
                            .background(if (selected) GlassTokens.IosTeal.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                0.8.dp,
                                if (selected) GlassTokens.IosTeal else GlassTokens.IosSeparator,
                                GlassTokens.radiusPill
                            )
                            .clickable { onSidechainHpfChange(hz) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) GlassTokens.IosTeal else GlassTokens.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

