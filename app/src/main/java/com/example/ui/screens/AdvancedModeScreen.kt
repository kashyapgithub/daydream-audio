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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.components.IosRowSeparator
import com.example.ui.components.IosSectionHeader
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.LedCompressionMeter
import com.example.ui.components.LiquidSlider
import com.example.ui.components.SpatialStageVisualizer
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.iosInsetGroupedCard
import com.example.ui.theme.raisedGlass
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

/**
 * Advanced Mode Screen (PRD 6.2 & Apple Pro App HIG):
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
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Apple Pro Studio Large Title Header
        item {
            Column(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) {
                Text(
                    text = "Studio Master Chain",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextPrimary,
                    letterSpacing = (-0.6).sp
                )
                Text(
                    text = "10-Band Parametric EQ, RMS Dynamics & Freeverb DSP",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Prominent Apple A/B Instant Compare Bar (PRD FR-3)
        item {
            ABCompareBar(
                isBypassed = uiState.isBypassed,
                onToggle = { viewModel.toggleBypassAB() },
                reduceGlass = uiState.reduceGlass
            )
        }

        // 10-Band Independent Parametric EQ (Apple Inset Grouped Section)
        item {
            IosSectionHeader(
                title = "10-Band Parametric EQ",
                subtitle = "Independent center frequencies with user-adjustable Q factor (0.3 to 10.0)"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    uiState.advancedBands.forEachIndexed { index, band ->
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
                                accentColor = GlassTokens.IosBlue,
                                reduceGlass = uiState.reduceGlass
                            )

                            LiquidSlider(
                                title = "  ↳ Resonance (Q)",
                                value = band.q,
                                onValueChange = { viewModel.setParametricQ(band.hz, it) },
                                valueRange = 0.3f..10.0f,
                                unit = "Q",
                                showTechnical = false,
                                accentColor = GlassTokens.IosTeal,
                                reduceGlass = uiState.reduceGlass
                            )
                        }

                        if (index < uiState.advancedBands.size - 1) {
                            IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // Dynamics Compressor Controls (Apple Inset Grouped Section)
        item {
            IosSectionHeader(
                title = "Dynamics Processor",
                subtitle = "RMS soft-knee compressor with manual threshold & ratio"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    // Studio Hardware LED Gain Reduction Meter
                    LedCompressionMeter(
                        thresholdDb = uiState.compThresholdDb,
                        ratio = uiState.compRatio,
                        audioRms = if (uiState.isPlaying) uiState.audioRms else 0.001f,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LiquidSlider(
                        title = "Threshold (When it engages)",
                        value = uiState.compThresholdDb,
                        onValueChange = { viewModel.setCompThresholdDb(it) },
                        valueRange = -40f..0f,
                        unit = "dB",
                        showTechnical = true,
                        accentColor = GlassTokens.IosOrange,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    LiquidSlider(
                        title = "Compression Ratio (Intensity)",
                        value = uiState.compRatio,
                        onValueChange = { viewModel.setCompRatio(it) },
                        valueRange = 1f..10f,
                        unit = ":1",
                        showTechnical = true,
                        accentColor = GlassTokens.IosOrange,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    LiquidSlider(
                        title = "Attack Time (Speed of clamp)",
                        value = uiState.compAttackMs,
                        onValueChange = { viewModel.setCompAttackMs(it) },
                        valueRange = 1f..100f,
                        unit = "ms",
                        showTechnical = true,
                        accentColor = GlassTokens.IosOrange,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    LiquidSlider(
                        title = "Release Time (Recovery)",
                        value = uiState.compReleaseMs,
                        onValueChange = { viewModel.setCompReleaseMs(it) },
                        valueRange = 10f..500f,
                        unit = "ms",
                        showTechnical = true,
                        accentColor = GlassTokens.IosOrange,
                        reduceGlass = uiState.reduceGlass
                    )
                }
            }
        }

        // Space HRTF Profile Choice (Apple Inset Grouped Section)
        item {
            IosSectionHeader(
                title = "Spatial Audio & HRTF",
                subtitle = "Head-related transfer function & binaural room simulation"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "HRTF VIRTUALIZER WIDTH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val hrtfProfiles = listOf("Narrow", "Natural", "Wide")
                    val selectedHrtfIndex = hrtfProfiles.indexOf(uiState.hrtfProfile).let { if (it >= 0) it else 1 }
                    IosSegmentedControl(
                        items = hrtfProfiles,
                        selectedIndex = selectedHrtfIndex,
                        onSelect = { viewModel.setHrtfProfile(hrtfProfiles[it]) },
                        label = { it }
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "ROOM SIMULATION (PRD 6.7a)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val roomTypes = listOf("Natural", "Intimate Studio", "Concert Hall", "Cathedral")
                    val selectedRoomIndex = roomTypes.indexOf(uiState.spatialRoomType).let { if (it >= 0) it else 0 }
                    IosSegmentedControl(
                        items = roomTypes,
                        selectedIndex = selectedRoomIndex,
                        onSelect = { viewModel.setSpatialRoomType(roomTypes[it]) },
                        label = { it.replace("Intimate ", "").replace("Concert ", "") }
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    // AirPods Pro Stereo Soundstage Arc
                    SpatialStageVisualizer(
                        spacePercent = uiState.spacePercent,
                        hrtfProfile = uiState.hrtfProfile,
                        isBypassed = uiState.isBypassed,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Time & Space FX: Reverb & Delay (Apple Inset Grouped Section)
        item {
            IosSectionHeader(
                title = "Algorithmic Time & Space",
                subtitle = "Schroeder-Freeverb 8-comb/4-allpass network & stereo ping-pong delay"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    // Apple Subtle Capability Disclosure Callout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GlassTokens.radiusSm)
                            .background(GlassTokens.IosGroupedSecondary)
                            .border(0.6.dp, GlassTokens.IosSeparator, GlassTokens.radiusSm)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "💡 Room Size & Wall Material process Daydream's player directly. System-Wide mode routes through standard OS reverb.",
                            fontSize = 11.sp,
                            color = GlassTokens.TextSecondary,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ROOM SIZE CHARACTER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp
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
                                    .background(if (selected) GlassTokens.IosIndigo else GlassTokens.IosGroupedSecondary)
                                    .border(
                                        0.6.dp,
                                        if (selected) GlassTokens.IosIndigo else GlassTokens.IosSeparator,
                                        GlassTokens.radiusPill
                                    )
                                    .clickable { viewModel.setRoomSize(size) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
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

                    Text(
                        text = "WALL REFLECTION MATERIAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 10.dp)
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
                                    .background(if (selected) GlassTokens.IosTeal else GlassTokens.IosGroupedSecondary)
                                    .border(
                                        0.6.dp,
                                        if (selected) GlassTokens.IosTeal else GlassTokens.IosSeparator,
                                        GlassTokens.radiusPill
                                    )
                                    .clickable { viewModel.setWallMaterial(material) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
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

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    // Reverb Controls
                    LiquidSlider(
                        title = "Reverb Wet Mix",
                        value = uiState.reverbWetPercent,
                        onValueChange = { viewModel.setReverbWet(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Wet/Dry Blend (up to 2.6x wet gain)",
                        showTechnical = true,
                        accentColor = GlassTokens.IosIndigo,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    LiquidSlider(
                        title = "Reverb Room Size (Fine Tune)",
                        value = uiState.reverbRoomSizePercent,
                        onValueChange = { viewModel.setReverbRoomSize(it) },
                        valueRange = 10f..100f,
                        unit = "%",
                        technicalValue = "Comb Feedback 0.35..0.985",
                        showTechnical = true,
                        accentColor = GlassTokens.IosIndigo,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    LiquidSlider(
                        title = "Reverb HF Damping",
                        value = uiState.reverbDampingPercent,
                        onValueChange = { viewModel.setReverbDamping(it) },
                        valueRange = 5f..100f,
                        unit = "%",
                        technicalValue = "Absorption Coeff",
                        showTechnical = true,
                        accentColor = GlassTokens.IosIndigo,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 10.dp))

                    // Echo / Delay Controls
                    LiquidSlider(
                        title = "Echo Delay Wet Mix",
                        value = uiState.echoWetPercent,
                        onValueChange = { viewModel.setEchoWet(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Delay Tap Output",
                        showTechnical = true,
                        accentColor = GlassTokens.IosTeal,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    LiquidSlider(
                        title = "Echo Delay Time",
                        value = uiState.echoTimeMs.toFloat(),
                        onValueChange = { viewModel.setEchoTimeMs(it.toInt()) },
                        valueRange = 50f..3000f,
                        unit = "ms",
                        technicalValue = "${uiState.echoTimeMs}ms delay line",
                        showTechnical = true,
                        accentColor = GlassTokens.IosTeal,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    LiquidSlider(
                        title = "Echo Feedback (Repeats)",
                        value = uiState.echoFeedbackPercent,
                        onValueChange = { viewModel.setEchoFeedback(it) },
                        valueRange = 0f..96f,
                        unit = "%",
                        technicalValue = "Crossfeed Loop Gain",
                        showTechnical = true,
                        accentColor = GlassTokens.IosTeal,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 10.dp))

                    // Playback Tempo
                    LiquidSlider(
                        title = "Playback Speed / Tempo",
                        value = uiState.playbackSpeed,
                        onValueChange = { viewModel.setPlaybackSpeed(it) },
                        valueRange = 0.25f..2.0f,
                        unit = "x",
                        technicalValue = if (uiState.varispeedMode) "Vari-Speed (pitch follows tempo)" else "Time-Stretch (pitch preserved)",
                        showTechnical = true,
                        accentColor = GlassTokens.IosOrange,
                        reduceGlass = uiState.reduceGlass
                    )

                    if (!uiState.speedAppliedAsRequested) {
                        Text(
                            text = "Your device applied ${String.format("%.2f", uiState.confirmedPlaybackSpeed)}x instead of ${String.format("%.2f", uiState.playbackSpeed)}x (OEM hardware limit).",
                            fontSize = 11.sp,
                            color = GlassTokens.IosOrange,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    IosRowSeparator(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Vari-Speed (Tape Slowdown)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = if (uiState.varispeedMode) {
                                    "Pitch drops as you slow down — classic analog tape slowdown tone."
                                } else {
                                    "Pitch stays natural regardless of speed — studio time-stretch."
                                },
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.varispeedMode,
                            onCheckedChange = { viewModel.setVarispeedMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlassTokens.IosGreen
                            )
                        )
                    }
                }
            }
        }

        // Preset Export / Import (Apple Inset Grouped Section)
        item {
            IosSectionHeader(
                title = "Preset Management",
                subtitle = "Share and import complete 6-stage chain presets as JSON"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val json = viewModel.exportCurrentPresetJson()
                                clipboardManager.setText(AnnotatedString(json))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                            shape = GlassTokens.radiusPill,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.openImportPresetDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosGroupedSecondary),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, GlassTokens.IosSeparator),
                            shape = GlassTokens.radiusPill,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = GlassTokens.IosBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GlassTokens.IosBlue)
                        }
                    }
                }
            }
        }
    }

    // Import Preset Dialog Modal (Apple Modal Sheet HIG)
    if (uiState.showImportPresetDialog) {
        var jsonInput by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { viewModel.closeImportPresetDialog() },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Apple Modal Sheet Grabber
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(5.dp)
                            .clip(GlassTokens.radiusPill)
                            .background(Color(0xFF5A5A5E))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Import Preset",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                        IconButton(onClick = { viewModel.closeImportPresetDialog() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GlassTokens.TextSecondary)
                        }
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = "Paste a saved Daydream Audio JSON preset bundle to restore the exact 6-stage chain:",
                        fontSize = 13.sp,
                        color = GlassTokens.TextSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = {
                            jsonInput = it
                            importError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("{ \"version\": 1, ... }", fontSize = 12.sp, color = GlassTokens.TextMuted) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = GlassTokens.TextPrimary),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GlassTokens.IosGroupedSecondary,
                            unfocusedContainerColor = GlassTokens.IosGroupedSecondary,
                            focusedIndicatorColor = GlassTokens.IosBlue,
                            unfocusedIndicatorColor = GlassTokens.IosSeparator
                        )
                    )

                    if (importError != null) {
                        Text(
                            text = importError!!,
                            fontSize = 12.sp,
                            color = GlassTokens.IosRed,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                jsonInput = clip
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosGroupedSecondary),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, GlassTokens.IosSeparator),
                        shape = GlassTokens.radiusPill,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Paste From Clipboard", fontSize = 13.sp, color = GlassTokens.IosBlue, fontWeight = FontWeight.Medium)
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
                    colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
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
            containerColor = GlassTokens.IosGroupedPrimary,
            shape = GlassTokens.radiusXl
        )
    }
}
