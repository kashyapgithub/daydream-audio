package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import com.example.model.HarmonicSaturationType
import com.example.model.LimiterMode
import com.example.model.MidSideMode
import com.example.model.ReferenceMonitor
import com.example.model.SoundTargetPreset
import com.example.model.StreamingTarget
import com.example.model.SubCutFilter
import com.example.model.TestToneMode
import com.example.ui.components.ABCompareBar
import com.example.ui.components.CrestFactorBadge
import com.example.ui.components.DynamicDeEsserBadge
import com.example.ui.components.FletcherMunsonBadge
import com.example.ui.components.HarmonicSaturationCard
import com.example.ui.components.IosRowSeparator
import com.example.ui.components.IosSectionHeader
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.LedCompressionMeter
import com.example.ui.components.LiquidSlider
import com.example.ui.components.LissajousVectorScope
import com.example.ui.components.LufsLoudnessMeter
import com.example.ui.components.MasteringSubCutCard
import com.example.ui.components.MultibandDynamicsCard
import com.example.ui.components.ParametricEqCurveVisualizer
import com.example.ui.components.PhaseCorrelationMeter
import com.example.ui.components.ProMixWorkflowCard
import com.example.ui.components.QcPeakHoldCard
import com.example.ui.components.SoundTargetCarousel
import com.example.ui.components.SpatialStageVisualizer
import com.example.ui.components.StereoBalanceAndPolarityCard
import com.example.ui.components.TestToneGeneratorCard
import com.example.ui.components.TransientDesignerCard
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
 * - Pro Studio Reference & Mastering Bench: Mid/Side matrix, Reference Monitor simulation, Tape Drive, Limiter Mode
 * - Shareable Preset Export & DAW Reference Specification Sheet (FR-12)
 */
@Composable
fun AdvancedModeScreen(
    viewModel: DaydreamViewModel,
    uiState: DaydreamUiState,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
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
                reduceGlass = uiState.reduceGlass,
                gainMatchedAB = uiState.gainMatchedABEnabled,
                onToggleGainMatched = { viewModel.setGainMatchedAB(!uiState.gainMatchedABEnabled) }
            )
        }

        // Cupertino Sound Target Quick Presets Carousel
        item {
            SoundTargetCarousel(
                targets = SoundTargetPreset.ALL,
                activeTargetId = uiState.activeSoundTargetId,
                onSelectTarget = { viewModel.applySoundTarget(it) },
                onOpenSaveDialog = { viewModel.openSavePresetDialog() }
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
                    // Logic Pro Style 10-Band Parametric Transfer Response Oscilloscope with Live Dual RTA
                    ParametricEqCurveVisualizer(
                        bands = uiState.advancedBands,
                        spectrum = uiState.spectrum,
                        isPlaying = uiState.isPlaying,
                        isExternalActive = uiState.isExternalPlaybackActive,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    IosRowSeparator(modifier = Modifier.padding(bottom = 8.dp))

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
                    // Studio Hardware LED Gain Reduction Meter (measured DSP loop
                    // while the in-app engine runs, estimate fallback otherwise).
                    LedCompressionMeter(
                        thresholdDb = uiState.compThresholdDb,
                        ratio = uiState.compRatio,
                        audioRms = if (uiState.isPlaying) uiState.audioRms else 0.001f,
                        modifier = Modifier.padding(bottom = 12.dp),
                        measuredGrDb = if (uiState.isPlaying) uiState.measuredGrDb else -1f
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

        // Pro Studio 3-Band Multiband Compressor (player-only mastering stage)
        item {
            IosSectionHeader(
                title = "Multiband Dynamics",
                subtitle = "LR4 3-band compression with knee & sidechain filter"
            )
            Spacer(modifier = Modifier.height(6.dp))
            MultibandDynamicsCard(
                enabled = uiState.multibandEnabled,
                onToggleEnabled = { viewModel.setMultibandEnabled(it) },
                xoverLowHz = uiState.mbXoverLowHz,
                onXoverLowChange = { viewModel.setMbXoverLowHz(it) },
                xoverHighHz = uiState.mbXoverHighHz,
                onXoverHighChange = { viewModel.setMbXoverHighHz(it) },
                threshLowDb = uiState.mbThreshLowDb,
                threshMidDb = uiState.mbThreshMidDb,
                threshHighDb = uiState.mbThreshHighDb,
                onThreshChange = { band, v -> viewModel.setMbThreshDb(band, v) },
                ratioLow = uiState.mbRatioLow,
                ratioMid = uiState.mbRatioMid,
                ratioHigh = uiState.mbRatioHigh,
                onRatioChange = { band, v -> viewModel.setMbRatio(band, v) },
                attackMs = uiState.mbAttackMs,
                onAttackChange = { viewModel.setMbAttackMs(it) },
                releaseMs = uiState.mbReleaseMs,
                onReleaseChange = { viewModel.setMbReleaseMs(it) },
                kneeDb = uiState.mbKneeDb,
                onKneeChange = { viewModel.setMbKneeDb(it) },
                sidechainHpfHz = uiState.mbSidechainHpfHz,
                onSidechainHpfChange = { viewModel.setMbSidechainHpfHz(it) },
                soloLow = uiState.mbSoloLow,
                soloMid = uiState.mbSoloMid,
                soloHigh = uiState.mbSoloHigh,
                onSoloChange = { band, v -> viewModel.setMbSolo(band, v) },
                grLowDb = uiState.mbGrLowDb,
                grMidDb = uiState.mbGrMidDb,
                grHighDb = uiState.mbGrHighDb,
                reduceGlass = uiState.reduceGlass
            )
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
                    // Path honesty: these Time & Space sliders drive Daydream's own
                    // player DSP. External apps (YouTube etc.) take the System path,
                    // which only carries EQ / Space / Punch / Loudness.
                    if (uiState.isExternalPlaybackActive && !uiState.isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassTokens.radiusSm)
                                .background(GlassTokens.IosOrange.copy(alpha = 0.12f))
                                .border(0.8.dp, GlassTokens.IosOrange.copy(alpha = 0.45f), GlassTokens.radiusSm)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "You're hearing external audio (System path): echo, reverb character and tempo live in Daydream's player, so these sliders won't touch it. System audio gets EQ / Space / Punch / Loudness only.",
                                    fontSize = 11.sp,
                                    color = GlassTokens.IosOrange,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.selectTrack(0) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosOrange),
                                    shape = GlassTokens.radiusPill
                                ) {
                                    Text(
                                        text = "Preview on demo track",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
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
                    }

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
                        technicalValue = "Equal-Power Blend (100% wet removes dry)",
                        showTechnical = true,
                        accentColor = GlassTokens.IosIndigo,
                        reduceGlass = uiState.reduceGlass
                    )

                    if (uiState.reverbWetPercent <= 0.5f) {
                        Text(
                            text = "Wet Mix is at 0% — Room Size, Damping and Freeze are bypassed until you raise it.",
                            fontSize = 11.sp,
                            color = GlassTokens.IosOrange,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    LiquidSlider(
                        title = "Reverb Room Size (Fine Tune)",
                        value = uiState.reverbRoomSizePercent,
                        onValueChange = { viewModel.setReverbRoomSize(it) },
                        valueRange = 10f..100f,
                        unit = "%",
                        technicalValue = "Comb Feedback 0.40..0.988",
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
                        technicalValue = "High-Cut Absorption Coeff",
                        showTechnical = true,
                        accentColor = GlassTokens.IosIndigo,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Reverb Freeze (Infinite Tail)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = if (uiState.reverbFreezeEnabled) {
                                    "Comb feedback locked at 0.999 — audio is held in an infinite ambient wash."
                                } else {
                                    "Standard decay based on Room Size & Damping."
                                },
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.reverbFreezeEnabled,
                            onCheckedChange = { viewModel.toggleReverbFreeze() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlassTokens.IosIndigo
                            )
                        )
                    }

                    IosRowSeparator(modifier = Modifier.padding(vertical = 10.dp))

                    // Echo / Delay Controls
                    LiquidSlider(
                        title = "Echo Delay Wet Mix",
                        value = uiState.echoWetPercent,
                        onValueChange = { viewModel.setEchoWet(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Analog Tape Output Blend",
                        showTechnical = true,
                        accentColor = GlassTokens.IosTeal,
                        reduceGlass = uiState.reduceGlass
                    )

                    if (uiState.echoWetPercent <= 0.5f) {
                        Text(
                            text = "Wet Mix is at 0% — Delay Time and Feedback are bypassed until you raise it.",
                            fontSize = 11.sp,
                            color = GlassTokens.IosOrange,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

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
                        technicalValue = "Tape Saturation Feedback Loop (up to 96%)",
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

        // Pro Studio Reference & Mastering Bench (Apple Inset Grouped Section)
        item {
            IosSectionHeader(
                title = "Studio Reference & Mastering Bench",
                subtitle = "Mid/Side audition matrix, monitor acoustic profiles & loudness mastering"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    // Real-Time Broadcast Loudness Suite (ITU-R BS.1770-4 & EBU R128)
                    LufsLoudnessMeter(
                        metrics = uiState.lufsMetrics,
                        currentTarget = uiState.streamingTarget,
                        onSelectTarget = { viewModel.setStreamingTarget(it) },
                        reduceGlass = uiState.reduceGlass
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real-Time Studio Meters
                    PhaseCorrelationMeter(correlation = uiState.phaseCorrelation)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real-Time 2D Phosphor Lissajous Goniometer & Vector Scope
                    LissajousVectorScope(
                        points = uiState.vectorScopePoints,
                        phaseCorrelation = uiState.phaseCorrelation,
                        reduceGlass = uiState.reduceGlass,
                        reduceMotion = uiState.reduceMotion
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CrestFactorBadge(
                        crestFactorDb = uiState.crestFactorDb,
                        peakDbfs = uiState.peakDbfs,
                        rmsDbfs = uiState.rmsDbfs
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    // Mid / Side Audition Matrix
                    Text(
                        text = "MID / SIDE AUDITION MATRIX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Isolate phantom center (kick, snare, lead vocals) or stereo side ambience to check mix translation outside the studio.",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val midSideModes = listOf(
                        MidSideMode.STEREO to "Stereo",
                        MidSideMode.MONO_SUM to "Mono",
                        MidSideMode.MID_ONLY to "Mid",
                        MidSideMode.SIDE_ONLY to "Side",
                        MidSideMode.PHASE_INVERT to "Ø Invert"
                    )
                    val selectedMsIndex = midSideModes.indexOfFirst { it.first == uiState.midSideMode }.let { if (it >= 0) it else 0 }

                    IosSegmentedControl(
                        items = midSideModes,
                        selectedIndex = selectedMsIndex,
                        onSelect = { viewModel.setMidSideMode(midSideModes[it].first) },
                        label = { it.second }
                    )

                    if (uiState.midSideMode != MidSideMode.STEREO) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassTokens.radiusSm)
                                .background(GlassTokens.IosOrange.copy(alpha = 0.15f))
                                .border(0.8.dp, GlassTokens.IosOrange.copy(alpha = 0.40f), GlassTokens.radiusSm)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = GlassTokens.IosOrange, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AUDITION ACTIVE: ${uiState.midSideMode.displayName}. Tap 'Stereo' when finished.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassTokens.IosOrange,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    // Reference Monitor Simulation
                    Text(
                        text = "REFERENCE MONITOR SIMULATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Emulate classic studio mixing monitors, car sound systems & consumer devices:",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReferenceMonitor.entries.forEach { monitor ->
                            val isSelected = uiState.referenceMonitor == monitor
                            val monitorLabel = when (monitor) {
                                ReferenceMonitor.FLAT -> "Flat Studio"
                                ReferenceMonitor.NS10M -> "Yamaha NS-10M"
                                ReferenceMonitor.AURATONE_5C -> "Auratone 5C"
                                ReferenceMonitor.CAR_TEST -> "Car Test"
                                ReferenceMonitor.AIRPODS_PRO -> "AirPods Pro"
                                ReferenceMonitor.PHONE_SPEAKER -> "Phone Speaker"
                                ReferenceMonitor.CLUB_SYSTEM -> "Club PA"
                                ReferenceMonitor.MACBOOK_PRO -> "MacBook Pro"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(GlassTokens.radiusPill)
                                    .background(if (isSelected) GlassTokens.IosTeal else GlassTokens.IosGroupedSecondary)
                                    .border(
                                        0.8.dp,
                                        if (isSelected) GlassTokens.IosTeal else GlassTokens.IosSeparator,
                                        GlassTokens.radiusPill
                                    )
                                    .clickable { viewModel.setReferenceMonitor(monitor) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = monitorLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else GlassTokens.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GlassTokens.radiusSm)
                            .background(Color(0xFF141416))
                            .border(0.6.dp, GlassTokens.IosSeparator, GlassTokens.radiusSm)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "${uiState.referenceMonitor.label} • ${uiState.referenceMonitor.subtitle}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.IosTeal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.referenceMonitor.description,
                                fontSize = 11.sp,
                                color = GlassTokens.TextSecondary,
                                lineHeight = 15.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ISO 226 Fletcher-Munson Equal-Loudness Calibration
                    FletcherMunsonBadge(
                        enabled = uiState.fletcherMunsonEnabled,
                        onToggle = { viewModel.toggleFletcherMunson(it) }
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    // Dynamic Transient Designer (SPL & Oxford TransMod)
                    TransientDesignerCard(
                        attackPercent = uiState.transientAttackPercent,
                        sustainPercent = uiState.transientSustainPercent,
                        attackActivity = uiState.transientAttackActivity,
                        sustainActivity = uiState.transientSustainActivity,
                        onAttackChange = { viewModel.setTransientAttackPercent(it) },
                        onSustainChange = { viewModel.setTransientSustainPercent(it) },
                        onReset = {
                            viewModel.setTransientAttackPercent(0f)
                            viewModel.setTransientSustainPercent(0f)
                        },
                        reduceGlass = uiState.reduceGlass
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Analog Harmonic Coloration & THD Analyzer
                    HarmonicSaturationCard(
                        selectedType = uiState.harmonicSaturationType,
                        drivePercent = uiState.harmonicDrivePercent,
                        thdPercent = uiState.thdPercent,
                        onTypeChange = { viewModel.setHarmonicSaturationType(it) },
                        onDriveChange = { viewModel.setHarmonicDrivePercent(it) },
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 8.dp))

                    // Bass Mono-Maker (<120Hz)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Bass Mono-Maker (<120Hz)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = "Collapses sub frequencies below 120Hz into pure mono. Eliminates low-end phase cancellations for club subwoofers and vinyl master compatibility.",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.bassMonoMakerEnabled,
                            onCheckedChange = { viewModel.setBassMonoMaker(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlassTokens.IosTeal
                            )
                        )
                    }

                    IosRowSeparator(modifier = Modifier.padding(vertical = 10.dp))

                    // Limiter Ceiling Architecture
                    Text(
                        text = "LIMITER CEILING ARCHITECTURE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = if (uiState.limiterMode == LimiterMode.SOFT_BRICKWALL) {
                            "Soft Brickwall: Transparent 2.5ms lookahead limiter prevents inter-sample clipping."
                        } else {
                            "Hard Clipper: Zero-latency hard clipper retains sharp drum transient impact for modern punch."
                        },
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val limiterModes = listOf(
                        LimiterMode.SOFT_BRICKWALL to "Soft Brickwall",
                        LimiterMode.HARD_CLIPPER to "Hard Clipper"
                    )
                    val selectedLimiterIndex = if (uiState.limiterMode == LimiterMode.SOFT_BRICKWALL) 0 else 1

                    IosSegmentedControl(
                        items = limiterModes,
                        selectedIndex = selectedLimiterIndex,
                        onSelect = { viewModel.setLimiterMode(limiterModes[it].first) },
                        label = { it.second }
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    // Mastering Sub-Cut Filter
                    MasteringSubCutCard(
                        currentFilter = uiState.subCutFilter,
                        onSelectFilter = { viewModel.setSubCutFilter(it) },
                        reduceGlass = uiState.reduceGlass
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Frequency De-Esser
                    DynamicDeEsserBadge(
                        enabled = uiState.deEsserEnabled,
                        onToggleEnabled = { viewModel.setDeEsserEnabled(it) },
                        thresholdDb = uiState.deEsserThresholdDb,
                        onThresholdChange = { viewModel.setDeEsserThresholdDb(it) },
                        maxReductionDb = uiState.deEsserMaxReductionDb,
                        onMaxReductionChange = { viewModel.setDeEsserMaxReductionDb(it) },
                        currentReductionDb = uiState.deEsserReductionDb,
                        reduceGlass = uiState.reduceGlass
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stereo Balance Trim & Channel Polarity (Ø L, Ø R)
                    StereoBalanceAndPolarityCard(
                        balanceTrimDb = uiState.stereoBalanceTrimDb,
                        onBalanceTrimChange = { viewModel.setStereoBalanceTrimDb(it) },
                        invertLeftPolarity = uiState.invertLeftPolarity,
                        onToggleInvertLeft = { viewModel.setInvertLeftPolarity(it) },
                        invertRightPolarity = uiState.invertRightPolarity,
                        onToggleInvertRight = { viewModel.setInvertRightPolarity(it) },
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    // DAW Reference Sheet CTA
                    Button(
                        onClick = { viewModel.setDawExportDialogOpen(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                        shape = GlassTokens.radiusPill,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export DAW Specification Sheet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Studio Calibration & Test Tone Generator Section
        item {
            IosSectionHeader(
                title = "Acoustic Calibration & Test Tones",
                subtitle = "Hardware alignment synthesizer: Pink Noise, 1kHz calibration tone & sweeps"
            )
            Spacer(modifier = Modifier.height(6.dp))
            TestToneGeneratorCard(
                currentMode = uiState.testToneMode,
                onSelectMode = { viewModel.setTestToneMode(it) },
                levelDb = uiState.testToneLevelDb,
                onLevelChange = { viewModel.setTestToneLevelDb(it) },
                reduceGlass = uiState.reduceGlass
            )
        }

        // Pro Studio Mix Workflow: snapshots, QC peak-hold, offline bounce
        item {
            IosSectionHeader(
                title = "Pro Mix Workflow",
                subtitle = "A/B/C/D snapshots, clip QC & WAV bounce with loudness match"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProMixWorkflowCard(
                        snapshots = uiState.mixSnapshots,
                        activeIndex = uiState.activeSnapshotIndex,
                        canUndo = uiState.canUndoMix,
                        onSave = { viewModel.saveMixSnapshot(it) },
                        onRecall = { viewModel.recallMixSnapshot(it) },
                        onClear = { viewModel.clearMixSnapshot(it) },
                        onUndo = { viewModel.undoMixChange() },
                        reduceGlass = uiState.reduceGlass
                    )
                    QcPeakHoldCard(
                        holdDbtp = uiState.truePeakHoldDbtp,
                        clipCount = uiState.clipCount,
                        integratedLufs = uiState.lufsMetrics.integratedLufs,
                        targetLufs = uiState.streamingTarget.targetLufs,
                        autoGainDb = viewModel.loudnessAutoMatchGainDb(),
                        isBouncing = uiState.isBouncing,
                        lastBounceInfo = uiState.lastBouncePath?.let {
                            "Last: ${it.substringAfterLast('/')} (${String.format("%.1f", uiState.lastBounceGainDb)}dB, peak ${String.format("%.1f", uiState.lastBouncePeakDbtp)}dBTP)"
                        },
                        onReset = { viewModel.resetPeakHold() },
                        onBounce = { viewModel.bounceCurrentMixToWav(context) },
                        hasLocalTrack = uiState.currentLocalTrack != null,
                        trackTitle = uiState.currentLocalTrack?.title,
                        bitDepth = uiState.bounceBitDepth,
                        onSelectBitDepth = { viewModel.setBounceBitDepth(it) },
                        bounceProgress = uiState.bounceProgress,
                        verifyText = uiState.bounceVerifyText
                    )
                    Text(
                        text = "Library now imports MP3 / WAV / FLAC / M4A / OGG / Opus — decoder auto-resamples to 44.1kHz.",
                        fontSize = 11.sp,
                        color = GlassTokens.TextSecondary
                    )
                }
            }
        }

        // Preset Export / Import (Apple Inset Grouped Section)
        item {
            IosSectionHeader(
                title = "Preset & DAW Integration",
                subtitle = "Export DAW studio spec sheets or share JSON chain presets"
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
                                Toast.makeText(context, "JSON preset copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosGroupedSecondary),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, GlassTokens.IosSeparator),
                            shape = GlassTokens.radiusPill,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = GlassTokens.IosTeal,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GlassTokens.IosTeal)
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
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GlassTokens.IosBlue)
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
                            Toast.makeText(context, "Preset imported successfully!", Toast.LENGTH_SHORT).show()
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

    // Pro Studio DAW Specification & Preset Export Dialog Modal (Apple HIG Sheet)
    if (uiState.showDawExportDialog) {
        var selectedFormatTab by remember { mutableStateOf(0) } // 0 = DAW Spec Sheet, 1 = JSON Preset
        val dawSheetText = remember(uiState) { viewModel.exportDawReferenceSheet() }
        val jsonPresetText = remember(uiState) { viewModel.exportCurrentPresetJson() }
        val displayedContent = if (selectedFormatTab == 0) dawSheetText else jsonPresetText

        AlertDialog(
            onDismissRequest = { viewModel.setDawExportDialogOpen(false) },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                            text = "Studio Chain Export",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                        IconButton(onClick = { viewModel.setDawExportDialogOpen(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GlassTokens.TextSecondary)
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Transfer your mobile tuning directly into your desktop DAW plugins (FabFilter, Logic, Ableton) or backup as JSON:",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    IosSegmentedControl(
                        items = listOf("DAW Spec Sheet", "JSON Preset"),
                        selectedIndex = selectedFormatTab,
                        onSelect = { selectedFormatTab = it },
                        label = { it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(GlassTokens.radiusSm)
                            .background(Color(0xFF141416))
                            .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusSm)
                            .padding(10.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = displayedContent,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = GlassTokens.TextPrimary,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(displayedContent))
                                Toast.makeText(
                                    context,
                                    if (selectedFormatTab == 0) "DAW Spec copied to clipboard!" else "JSON preset copied to clipboard!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                            shape = GlassTokens.radiusPill,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                val savedFile = viewModel.savePresetToFile(context, isDawSheet = (selectedFormatTab == 0))
                                if (savedFile.isNotEmpty()) {
                                    Toast.makeText(context, "Saved to Downloads: ${savedFile.substringAfterLast('/')}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Could not write to Downloads", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosGroupedSecondary),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, GlassTokens.IosSeparator),
                            shape = GlassTokens.radiusPill,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = GlassTokens.IosTeal, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save File", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GlassTokens.IosTeal)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.setDawExportDialogOpen(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosGroupedSecondary),
                    shape = GlassTokens.radiusPill
                ) {
                    Text("Done", color = GlassTokens.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = GlassTokens.IosGroupedPrimary,
            shape = GlassTokens.radiusXl
        )
    }
}
