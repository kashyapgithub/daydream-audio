package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ABCompareBar
import com.example.ui.components.LiquidSlider
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.raisedGlass
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

/**
 * Advanced Mode Screen (PRD 6.2):
 * - 10-Band Independent Parametric EQ with individual Q (0.3 to 10.0) and gain (-12 to +12dB)
 * - True parametric dynamics controls: Threshold, Ratio, Attack, Release
 * - HRTF Profile selection
 * - Shareable Preset Export (FR-12)
 */
@Composable
fun AdvancedModeScreen(
    viewModel: DaydreamViewModel,
    uiState: DaydreamUiState,
    paddingValues: PaddingValues
) {
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = GlassTokens.AccentStart,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Advanced Studio Chain",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                }
                Text(
                    text = "10-Band Parametric EQ & Dynamics Compressor with independent Q and gain",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary
                )
            }
        }

        // Prominent A/B Bar (<50ms instantaneous switch, PRD FR-3)
        item {
            ABCompareBar(
                isBypassed = uiState.isBypassed,
                onToggle = { viewModel.toggleBypassAB() },
                reduceGlass = uiState.reduceGlass
            )
        }

        // 10-Band Independent Parametric EQ Grid (PRD 6.2 & 8.3)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "10-Band Parametric EQ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "Independent center frequencies with user-adjustable Q factor (0.3 to 10.0)",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    uiState.advancedBands.forEach { band ->
                        val formattedTitle = if (band.hz < 1000) "${band.hz}Hz • ${band.anchorLabel}" else "${band.hz / 1000}kHz • ${band.anchorLabel}"

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            LiquidSlider(
                                title = formattedTitle,
                                value = band.gainDb,
                                onValueChange = { viewModel.setParametricGain(band.hz, it) },
                                valueRange = -12f..12f,
                                unit = "dB",
                                technicalValue = "Q=${String.format("%.1f", band.q)}",
                                showTechnical = true,
                                reduceGlass = uiState.reduceGlass
                            )

                            // Optional Q-factor slider
                            LiquidSlider(
                                title = "  ↳ Resonance (Q)",
                                value = band.q,
                                onValueChange = { viewModel.setParametricQ(band.hz, it) },
                                valueRange = 0.3f..10.0f,
                                unit = "Q",
                                showTechnical = false,
                                accentColor = GlassTokens.AccentStart.copy(alpha = 0.8f),
                                reduceGlass = uiState.reduceGlass
                            )
                        }
                    }
                }
            }
        }

        // Dynamics Compressor Controls (PRD 6.2 & 8.3)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Dynamics Processing (RMS Compressor)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "Manual threshold, ratio, attack, and release parameters",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LiquidSlider(
                        title = "Threshold (When it engages)",
                        value = uiState.compThresholdDb,
                        onValueChange = { viewModel.setCompThresholdDb(it) },
                        valueRange = -40f..0f,
                        unit = "dB",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    LiquidSlider(
                        title = "Compression Ratio (Intensity)",
                        value = uiState.compRatio,
                        onValueChange = { viewModel.setCompRatio(it) },
                        valueRange = 1f..10f,
                        unit = ":1",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    LiquidSlider(
                        title = "Attack Time (Speed of clamp)",
                        value = uiState.compAttackMs,
                        onValueChange = { viewModel.setCompAttackMs(it) },
                        valueRange = 1f..100f,
                        unit = "ms",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    LiquidSlider(
                        title = "Release Time (Recovery)",
                        value = uiState.compReleaseMs,
                        onValueChange = { viewModel.setCompReleaseMs(it) },
                        valueRange = 10f..500f,
                        unit = "ms",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )
                }
            }
        }

        // Space HRTF Profile Choice (PRD 6.3)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Virtualizer HRTF Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "Head-related transfer function simulation width",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Narrow", "Natural", "Wide").forEach { profile ->
                            val isSelected = uiState.hrtfProfile == profile
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(GlassTokens.radiusPill)
                                    .background(
                                        if (isSelected) GlassTokens.AccentStart
                                         else Color.White.copy(alpha = 0.08f)
                                    )
                                    .clickable { viewModel.setHrtfProfile(profile) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else GlassTokens.TextPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Spatial Room Simulation (PRD 6.7a)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "Simulates early room reflections and acoustic environment",
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Natural", "Intimate Studio", "Concert Hall", "Cathedral").forEach { room ->
                            val isSelected = uiState.spatialRoomType == room
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(GlassTokens.radiusPill)
                                    .background(
                                        if (isSelected) GlassTokens.AccentStart
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .clickable { viewModel.setSpatialRoomType(room) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = room.replace("Intimate ", "").replace("Concert ", ""),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else GlassTokens.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Time & Space FX: Reverb & Delay
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Algorithmic Reverb & Delay (Time & Space)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "Schroeder-Freeverb 8-comb/4-allpass network & stereo ping-pong delay",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Room Size character preset (PRD 6.15) - changes actual comb
                    // delay length, not just decay time, so each size feels
                    // structurally distinct, not just "longer tail"
                    Text(
                        text = "Room Size",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.audio.AudioEngine.RoomSize.values().forEach { size ->
                            val selected = uiState.roomSize == size
                            Box(
                                modifier = Modifier
                                    .clip(GlassTokens.radiusPill)
                                    .background(if (selected) GlassTokens.AccentStart else GlassTokens.RaisedGlassFill)
                                    .clickable { viewModel.setRoomSize(size) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = size.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else GlassTokens.TextSecondary
                                )
                            }
                        }
                    }

                    // Wall Material character preset (PRD 6.15) - colors the
                    // reflections spectrally (bright/reflective vs warm/absorptive)
                    Text(
                        text = "Wall Material",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.audio.AudioEngine.WallMaterial.values().forEach { material ->
                            val selected = uiState.wallMaterial == material
                            Box(
                                modifier = Modifier
                                    .clip(GlassTokens.radiusPill)
                                    .background(if (selected) GlassTokens.AccentStart else GlassTokens.RaisedGlassFill)
                                    .clickable { viewModel.setWallMaterial(material) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = material.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else GlassTokens.TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reverb Controls
                    LiquidSlider(
                        title = "Reverb Wet Mix",
                        value = uiState.reverbWetPercent,
                        onValueChange = { viewModel.setReverbWet(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Wet/Dry Blend (up to 2.2x wet gain at max)",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    LiquidSlider(
                        title = "Reverb Room Size (Fine Tune)",
                        value = uiState.reverbRoomSizePercent,
                        onValueChange = { viewModel.setReverbRoomSize(it) },
                        valueRange = 10f..100f,
                        unit = "%",
                        technicalValue = "Comb Feedback 0.35..0.985 (biased by Room Size + Wall Material above)",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    LiquidSlider(
                        title = "Reverb HF Damping",
                        value = uiState.reverbDampingPercent,
                        onValueChange = { viewModel.setReverbDamping(it) },
                        valueRange = 5f..100f,
                        unit = "%",
                        technicalValue = "Absorption Coeff",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Echo / Delay Controls
                    LiquidSlider(
                        title = "Echo Delay Wet Mix",
                        value = uiState.echoWetPercent,
                        onValueChange = { viewModel.setEchoWet(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Delay Tap Output",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    LiquidSlider(
                        title = "Echo Delay Time",
                        value = uiState.echoTimeMs.toFloat(),
                        onValueChange = { viewModel.setEchoTimeMs(it.toInt()) },
                        valueRange = 50f..2000f,
                        unit = "ms",
                        technicalValue = "${uiState.echoTimeMs}ms (delay buffer, up to 2s for canyon/dub-style delays)",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    LiquidSlider(
                        title = "Echo Feedback (Repeats)",
                        value = uiState.echoFeedbackPercent,
                        onValueChange = { viewModel.setEchoFeedback(it) },
                        valueRange = 0f..92f,
                        unit = "%",
                        technicalValue = "Crossfeed Loop Gain (near-self-oscillating at max)",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Playback Tempo
                    LiquidSlider(
                        title = "Playback Speed / Tempo",
                        value = uiState.playbackSpeed,
                        onValueChange = { viewModel.setPlaybackSpeed(it) },
                        valueRange = 0.5f..1.5f,
                        unit = "x",
                        technicalValue = "Time-Stretch Ratio",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )
                }
            }
        }

        // Preset Export / Import (PRD 6.2 & FR-12)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Preset Management (FR-12)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = "Share and import complete 6-stage chain presets as JSON",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val json = viewModel.exportCurrentPresetJson()
                                clipboardManager.setText(AnnotatedString(json))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                            shape = GlassTokens.radiusPill,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.openImportPresetDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                            shape = GlassTokens.radiusPill,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Import Preset Dialog Modal (PRD FR-12)
    if (uiState.showImportPresetDialog) {
        var jsonInput by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { viewModel.closeImportPresetDialog() },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Import Preset (JSON)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    IconButton(onClick = { viewModel.closeImportPresetDialog() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GlassTokens.TextSecondary)
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = "Paste a saved Daydream Audio JSON preset bundle below to restore the exact 6-stage chain:",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = {
                            jsonInput = it
                            importError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = { Text("{ \"version\": 1, ... }", fontSize = 12.sp, color = GlassTokens.TextMuted) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = GlassTokens.TextPrimary),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141220),
                            unfocusedContainerColor = Color(0xFF141220),
                            focusedIndicatorColor = GlassTokens.AccentStart,
                            unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    if (importError != null) {
                        Text(
                            text = importError!!,
                            fontSize = 11.sp,
                            color = GlassTokens.AccentWarning,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                jsonInput = clip
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f)),
                        shape = GlassTokens.radiusPill,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Paste From Clipboard", fontSize = 12.sp, color = GlassTokens.AccentStart)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonInput.isBlank()) {
                            importError = "Please paste a JSON preset"
                            return@Button
                        }
                        val success = viewModel.importPresetJson(jsonInput)
                        if (success) {
                            viewModel.closeImportPresetDialog()
                        } else {
                            importError = "Invalid preset format. Check JSON syntax."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                    shape = GlassTokens.radiusPill
                ) {
                    Text("Apply Preset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.closeImportPresetDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Cancel", color = GlassTokens.TextSecondary)
                }
            },
            containerColor = Color(0xFF191626),
            shape = GlassTokens.radiusLg
        )
    }
}
