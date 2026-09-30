package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OutputDevice
import com.example.model.PlainBand
import com.example.model.SoundTargetPreset
import com.example.ui.components.ABCompareBar
import com.example.ui.components.BandTooltipDialog
import com.example.ui.components.HearingComfortBadge
import com.example.ui.components.HomeSpectrumVisualizer
import com.example.ui.components.IosRowSeparator
import com.example.ui.components.IosSectionHeader
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.LiquidSlider
import com.example.ui.components.SoundTargetCarousel
import com.example.ui.components.SpatialStageVisualizer
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.iosInsetGroupedCard
import com.example.ui.theme.raisedGlass
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

@Composable
fun SimpleModeScreen(
    viewModel: DaydreamViewModel,
    uiState: DaydreamUiState,
    paddingValues: PaddingValues,
    onImportMp3Click: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Top Action Bar / Diagnostic Wizard Action
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Apple System Blue Capsule Action
                Button(
                    onClick = { viewModel.openWizardDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                    shape = GlassTokens.radiusPill,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    modifier = Modifier.testTag("wizard_trigger_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Diagnose",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Apple Music Lossless EQ Spectrum Visualizer
        item {
            HomeSpectrumVisualizer(
                spectrum = uiState.spectrum,
                isPlaying = uiState.isPlaying,
                isExternalActive = uiState.isExternalPlaybackActive,
                reduceGlass = uiState.reduceGlass
            )
        }

        // Apple Health Style Hearing Comfort & Live dB SPL Meter
        item {
            HearingComfortBadge(
                audioRms = uiState.audioRms,
                isLimiterActive = uiState.comfortLimiterEnabled,
                onToggleLimiter = { viewModel.toggleComfortLimiter() }
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

        // User Custom Sound Snapshots
        if (uiState.customPresets.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "MY SOUND SNAPSHOTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.customPresets.forEach { preset ->
                            val chipColor = try {
                                val clean = preset.colorHex.removePrefix("#")
                                val colorInt = clean.toLongOrNull(16) ?: 0x0A84FF
                                Color(if (clean.length == 6) (0xFF000000 or colorInt) else colorInt)
                            } catch (_: Exception) {
                                GlassTokens.IosBlue
                            }
                            Row(
                                modifier = Modifier
                                    .clip(GlassTokens.radiusPill)
                                    .background(chipColor.copy(alpha = 0.16f))
                                    .border(0.8.dp, chipColor.copy(alpha = 0.5f), GlassTokens.radiusPill)
                                    .clickable { viewModel.applyCustomPreset(preset) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(chipColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = preset.name,
                                    fontSize = 12.sp,
                                    color = GlassTokens.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete preset",
                                    tint = GlassTokens.TextMuted,
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable { viewModel.deleteCustomPreset(preset.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Apple Section: Private In-App Audio Vault (Isolated Local MP3 Storage & Playback)
        item {
            IosSectionHeader(
                title = "In-App Audio Vault",
                subtitle = "Private local storage • Processed through 32-bit DSP engine"
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = "Local MP3 Library",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = "Stored isolated in this app only (zero leakage)",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        // Apple Inset Pill Button for Import
                        Button(
                            onClick = onImportMp3Click,
                            enabled = !uiState.isImportingTrack,
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                            shape = GlassTokens.radiusPill,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("import_mp3_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isImportingTrack) "Importing…" else "Import MP3",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (uiState.localTracks.isEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassTokens.radiusMd)
                                .background(GlassTokens.IosGroupedSecondary)
                                .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusMd)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = GlassTokens.IosBlue.copy(alpha = 0.8f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No In-App Audio Stored Yet",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassTokens.TextPrimary
                                )
                                Text(
                                    text = "Tap 'Import MP3' to securely copy audio into this app's private sandbox. Songs will run through the full 7-stage DSP audio engine.",
                                    fontSize = 12.sp,
                                    color = GlassTokens.TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.localTracks.forEach { track ->
                                val isCurrentlySelected = uiState.currentLocalTrack?.id == track.id
                                val isCurrentPlaying = isCurrentlySelected && uiState.isPlaying

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(GlassTokens.radiusMd)
                                        .background(
                                            if (isCurrentlySelected) GlassTokens.IosBlue.copy(alpha = 0.12f)
                                            else GlassTokens.IosGroupedSecondary
                                        )
                                        .border(
                                            0.8.dp,
                                            if (isCurrentlySelected) GlassTokens.IosBlue.copy(alpha = 0.45f)
                                            else GlassTokens.IosSeparator,
                                            GlassTokens.radiusMd
                                        )
                                        .clickable {
                                            if (isCurrentlySelected) {
                                                viewModel.togglePlayPause()
                                            } else {
                                                viewModel.playLocalTrack(track)
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Play/Pause Action Icon
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(
                                                    if (isCurrentPlaying) GlassTokens.IosGreen
                                                    else GlassTokens.IosBlue
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isCurrentPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Track Info
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = track.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isCurrentlySelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isCurrentlySelected) GlassTokens.IosBlue else GlassTokens.TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (isCurrentPlaying) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(GlassTokens.radiusPill)
                                                            .background(GlassTokens.IosGreen.copy(alpha = 0.2f))
                                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "32-bit DSP",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = GlassTokens.IosGreen
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = track.artist,
                                                    fontSize = 12.sp,
                                                    color = GlassTokens.TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Text(
                                                    text = "•",
                                                    fontSize = 10.sp,
                                                    color = GlassTokens.TextMuted
                                                )
                                                Text(
                                                    text = track.formattedDuration,
                                                    fontSize = 11.sp,
                                                    color = GlassTokens.TextSecondary
                                                )
                                                Text(
                                                    text = "•",
                                                    fontSize = 10.sp,
                                                    color = GlassTokens.TextMuted
                                                )
                                                Text(
                                                    text = track.formattedFileSize,
                                                    fontSize = 11.sp,
                                                    color = GlassTokens.TextSecondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Delete button
                                        IconButton(
                                            onClick = { viewModel.deleteLocalTrack(track.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete local track",
                                                tint = GlassTokens.TextMuted,
                                                modifier = Modifier.size(18.dp)
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

        // Contextual tip (PRD 12.1 / FR-16)
        if (!uiState.isGlobalHookActive && uiState.currentDevice == OutputDevice.PHONE_SPEAKER) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosInsetGroupedCard(uiState.reduceGlass)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = GlassTokens.IosBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Not hearing a difference on other apps?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = "Some phones only apply system-wide effects over wired or Bluetooth headphones, not the built-in speaker. Try switching your output — this is a device limitation, not a bug.",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // System Audio Hook Status & Output Device Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Profile Chip (Apple Inset Pill)
                Row(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(GlassTokens.IosGroupedPrimary)
                        .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusPill)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = GlassTokens.IosBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.currentDevice.displayName,
                        fontSize = 12.sp,
                        color = GlassTokens.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // System Audio Session Hook Status (Apple Inset Pill)
                Row(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(GlassTokens.IosGroupedPrimary)
                        .border(
                            0.8.dp,
                            when {
                                uiState.isLegacyMode -> GlassTokens.IosBlue.copy(alpha = 0.6f)
                                uiState.activeSystemSessions.isNotEmpty() -> GlassTokens.IosGreen.copy(alpha = 0.6f)
                                else -> GlassTokens.IosSeparator
                            },
                            GlassTokens.radiusPill
                        )
                        .clickable { viewModel.toggleLegacyMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                when {
                                    uiState.isLegacyMode -> GlassTokens.IosBlue
                                    uiState.activeSystemSessions.isNotEmpty() -> GlassTokens.IosGreen
                                    else -> Color(0xFF636366)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            uiState.isLegacyMode -> "Legacy Player"
                            uiState.activeSystemSessions.isNotEmpty() -> "Hooked: ${uiState.activeSystemSessions.first()}"
                            else -> "System Audio: Idle"
                        },
                        fontSize = 12.sp,
                        color = when {
                            uiState.isLegacyMode -> GlassTokens.IosBlue
                            uiState.activeSystemSessions.isNotEmpty() -> GlassTokens.IosGreen
                            else -> GlassTokens.TextSecondary
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // OEM Hooking Guidance Banner (Apple Callout)
        if (uiState.activeSystemSessions.isEmpty() && !uiState.isLegacyMode) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosInsetGroupedCard(uiState.reduceGlass)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = "External Audio Hooking",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = "Enable 'Device Broadcast Status' in Spotify/YT Music. If OEM restrictions block routing, switch to Legacy In-App Player.",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Button(
                            onClick = { viewModel.toggleLegacyMode() },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosGroupedSecondary),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, GlassTokens.IosBlue.copy(alpha = 0.6f)),
                            shape = GlassTokens.radiusPill,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Legacy", fontSize = 12.sp, color = GlassTokens.IosBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Output Device Change Prompt Banner (Apple Action Card)
        if (uiState.devicePrompt != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusLg)
                        .background(GlassTokens.IosGroupedPrimary)
                        .border(1.dp, GlassTokens.IosBlue.copy(alpha = 0.6f), GlassTokens.radiusLg)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "🎧 Switch to ${uiState.devicePrompt.displayName}?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.IosBlue
                            )
                            Text(
                                text = "Audio output change detected. Tap to auto-tune baseline Space & Punch.",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.setOutputDevice(uiState.devicePrompt) },
                                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                                shape = GlassTokens.radiusPill,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Tune", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { viewModel.dismissDevicePrompt() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("Dismiss", fontSize = 12.sp, color = GlassTokens.TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Active Wizard Fix Banner (Apple Inset Card)
        if (uiState.lastWizardFixSummary != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosInsetGroupedCard(uiState.reduceGlass)
                        .border(0.8.dp, GlassTokens.IosBlue.copy(alpha = 0.4f), GlassTokens.radiusLg)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "✨ Active Fix: ${uiState.lastAppliedComplaint?.label ?: "Smart Tune"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.IosBlue
                            )
                            Text(
                                text = uiState.lastWizardFixSummary,
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.resetAllToFlat() }) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset to Flat",
                                tint = GlassTokens.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
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

        // Mono Warning Banner (Apple Caution Callout)
        item {
            AnimatedVisibility(visible = uiState.showMonoWarning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusLg)
                        .background(GlassTokens.IosGroupedPrimary)
                        .border(0.8.dp, GlassTokens.IosOrange.copy(alpha = 0.5f), GlassTokens.radiusLg)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = GlassTokens.IosOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mono Recording Detected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.IosOrange
                            )
                            Text(
                                text = "Virtualizer 'Space' is capped. Expanding mono audio too far creates phase cancellation and hollow vocals.",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Apple Section 1: Lofi Mode & Tempo Deceleration
        item {
            IosSectionHeader(
                title = "Lofi Mode & Tempo",
                subtitle = "Algorithmic tape deceleration & warm acoustic flutter"
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Lofi Mode",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassTokens.TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(GlassTokens.radiusPill)
                                        .background(
                                            if (uiState.isLofiMode) GlassTokens.IosOrange.copy(alpha = 0.20f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            0.6.dp,
                                            if (uiState.isLofiMode) GlassTokens.IosOrange.copy(alpha = 0.6f)
                                            else GlassTokens.IosSeparator,
                                            GlassTokens.radiusPill
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isLofiMode) "0.85x • Reverb • Warble" else "1-Tap Chill",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isLofiMode) GlassTokens.IosOrange else GlassTokens.TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "Slowed tempo, dreamy algorithmic reverb, warm tape rolloff & subtle flutter",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Switch(
                            checked = uiState.isLofiMode,
                            onCheckedChange = { viewModel.toggleLofiMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlassTokens.IosGreen
                            ),
                            modifier = Modifier.testTag("lofi_mode_switch")
                        )
                    }

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "PLAYBACK SPEED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val speeds = listOf(
                        0.80f to "0.80x",
                        0.85f to "0.85x",
                        0.90f to "0.90x",
                        1.0f to "1.0x",
                        1.15f to "1.15x"
                    )
                    val selectedSpeedIndex = speeds.indexOfFirst { kotlin.math.abs(uiState.playbackSpeed - it.first) < 0.02f }
                        .let { if (it >= 0) it else 3 }

                    IosSegmentedControl(
                        items = speeds,
                        selectedIndex = selectedSpeedIndex,
                        onSelect = { index -> viewModel.setPlaybackSpeed(speeds[index].first) },
                        label = { it.second }
                    )
                }
            }
        }

        // Apple Section 2: Analog Restoration
        item {
            IosSectionHeader(
                title = "Analog Restoration",
                subtitle = "Eliminate tape hiss, vinyl crackle, and AC mains hum"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    // Hiss Removal Slider
                    LiquidSlider(
                        title = "Hiss Removal",
                        value = uiState.hissRemovalPercent,
                        onValueChange = { viewModel.setHissRemovalPercent(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "High-Shelf Spectral Gate",
                        showTechnical = uiState.showTechnicalValues,
                        tipDescription = "Reduces tape hiss floor in quiet passages.",
                        accentColor = GlassTokens.IosTeal,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    // De-Hum & De-Crackle Toggles (Apple Grouped Rows)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // De-Hum Toggle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(GlassTokens.radiusSm)
                                .background(GlassTokens.IosGroupedSecondary)
                                .border(
                                    0.8.dp,
                                    if (uiState.deHumEnabled) GlassTokens.IosBlue.copy(alpha = 0.5f) else GlassTokens.IosSeparator,
                                    GlassTokens.radiusSm
                                )
                                .clickable { viewModel.toggleDeHum() }
                                .padding(12.dp)
                                .testTag("dehum_toggle")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                    Text(
                                        text = "De-Hum",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (uiState.deHumEnabled) GlassTokens.IosBlue else GlassTokens.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "50/60Hz notch",
                                        fontSize = 11.sp,
                                        color = GlassTokens.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Switch(
                                    checked = uiState.deHumEnabled,
                                    onCheckedChange = { viewModel.toggleDeHum() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassTokens.IosGreen
                                    )
                                )
                            }
                        }

                        // De-Crackle Toggle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(GlassTokens.radiusSm)
                                .background(GlassTokens.IosGroupedSecondary)
                                .border(
                                    0.8.dp,
                                    if (uiState.deCrackleEnabled) GlassTokens.IosBlue.copy(alpha = 0.5f) else GlassTokens.IosSeparator,
                                    GlassTokens.radiusSm
                                )
                                .clickable { viewModel.toggleDeCrackle() }
                                .padding(12.dp)
                                .testTag("decrackle_toggle")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                    Text(
                                        text = "De-Crackle",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (uiState.deCrackleEnabled) GlassTokens.IosBlue else GlassTokens.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Vinyl pops",
                                        fontSize = 11.sp,
                                        color = GlassTokens.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Switch(
                                    checked = uiState.deCrackleEnabled,
                                    onCheckedChange = { viewModel.toggleDeCrackle() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassTokens.IosGreen
                                    )
                                )
                            }
                        }
                    }

                    // Regional Mains Frequency Selector (Apple Segmented Control)
                    AnimatedVisibility(visible = uiState.deHumEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "MAINS FREQUENCY NOTCH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTokens.TextSecondary,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val freqs = listOf(50 to "50Hz (EU/Asia/UK)", 60 to "60Hz (US/Americas)")
                            val selectedFreqIndex = freqs.indexOfFirst { it.first == uiState.humFrequency }.let { if (it >= 0) it else 0 }
                            IosSegmentedControl(
                                items = freqs,
                                selectedIndex = selectedFreqIndex,
                                onSelect = { index -> viewModel.setHumFrequency(freqs[index].first) },
                                label = { it.second }
                            )
                        }
                    }
                }
            }
        }

        // Apple Section 3: Tone Shaper (5 Plain Bands)
        item {
            IosSectionHeader(
                title = "Tone Shaper",
                subtitle = "5 plain-English bands — tap title for acoustic guidance"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    val bands = PlainBand.entries.toList()
                    bands.forEachIndexed { index, band ->
                        val gain = uiState.eqGains[band] ?: 0f
                        LiquidSlider(
                            title = band.title,
                            value = gain,
                            onValueChange = { viewModel.setEqGain(band, it) },
                            valueRange = -12f..12f,
                            unit = "dB",
                            technicalValue = band.frequencyRange,
                            showTechnical = uiState.showTechnicalValues,
                            onInfoClick = { viewModel.showTooltip(band) },
                            accentColor = GlassTokens.IosBlue,
                            reduceGlass = uiState.reduceGlass
                        )
                        if (index < bands.size - 1) {
                            IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // Apple Section 4: Acoustic Presence & Space
        item {
            IosSectionHeader(
                title = "Acoustic Presence & Space",
                subtitle = "Spatial width, dynamic compression & loudness booster"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    // Space (Virtualizer)
                    LiquidSlider(
                        title = "Space (Width)",
                        value = uiState.spacePercent,
                        onValueChange = { viewModel.setSpacePercent(it) },
                        valueRange = if (uiState.isMonoDetected) 0f..35f else 0f..100f,
                        unit = "%",
                        technicalValue = "HRTF Crossfeed",
                        showTechnical = uiState.showTechnicalValues,
                        isWarning = uiState.isMonoDetected,
                        warningText = if (uiState.isMonoDetected) "Mono input detected — Space capped at 35% to prevent phase cancellation" else null,
                        accentColor = GlassTokens.IosIndigo,
                        reduceGlass = uiState.reduceGlass
                    )

                    // AirPods Pro Spatial Audio Soundstage Arc Visualizer
                    SpatialStageVisualizer(
                        spacePercent = uiState.spacePercent,
                        hrtfProfile = uiState.hrtfProfile,
                        isBypassed = uiState.isBypassed,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    // Punch (Dynamics Compressor)
                    LiquidSlider(
                        title = "Punch (Dynamic Range)",
                        value = uiState.punchPercent,
                        onValueChange = { viewModel.setPunchPercent(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "RMS Soft-Knee Compressor",
                        showTechnical = uiState.showTechnicalValues,
                        accentColor = GlassTokens.IosOrange,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    // Clarity Macro (Presence exciter)
                    LiquidSlider(
                        title = "Vocal Clarity Macro",
                        value = uiState.clarityMacroPercent,
                        onValueChange = { viewModel.setClarityMacroPercent(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Multiband Harmonic Exciter",
                        showTechnical = uiState.showTechnicalValues,
                        accentColor = GlassTokens.IosTeal,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    // Loudness (Volume Boost with auto limiter, PRD FR-5)
                    LiquidSlider(
                        title = "Volume Boost (Loudness)",
                        value = uiState.loudnessPercent,
                        onValueChange = { viewModel.setLoudnessPercent(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "LoudnessEnhancer + True-Peak Limiter",
                        showTechnical = uiState.showTechnicalValues,
                        isWarning = uiState.loudnessPercent > 75f,
                        warningText = if (uiState.loudnessPercent > 75f) "Past 75% loudness trades clarity for output" else "Soft brickwall limiter active (no clipping)",
                        accentColor = if (uiState.loudnessPercent > 75f) GlassTokens.IosOrange else GlassTokens.IosGreen,
                        reduceGlass = uiState.reduceGlass
                    )
                }
            }
        }

        // Apple Section 5: Atmospheric Reverb & Echo
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosSectionHeader(
                    title = "Atmospheric Reverb & Echo",
                    subtitle = "Algorithmic room reverb and tape delay repeats",
                    modifier = Modifier.weight(1f)
                )
                if (uiState.reverbWetPercent > 0f || uiState.echoWetPercent > 0f || uiState.playbackSpeed != 1.0f) {
                    IconButton(
                        onClick = {
                            viewModel.setReverbWet(0f)
                            viewModel.setEchoWet(0f)
                            viewModel.setPlaybackSpeed(1.0f)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Reverb & Echo",
                            tint = GlassTokens.IosBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    // Reverb Wet Slider
                    LiquidSlider(
                        title = "Reverb (Space Decay)",
                        value = uiState.reverbWetPercent,
                        onValueChange = { viewModel.setReverbWet(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Equal-Power Blend (100% wet removes dry)",
                        showTechnical = uiState.showTechnicalValues,
                        accentColor = GlassTokens.IosIndigo,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    // Reverb Room Size Slider
                    LiquidSlider(
                        title = "Reverb Room Size",
                        value = uiState.reverbRoomSizePercent,
                        onValueChange = { viewModel.setReverbRoomSize(it) },
                        valueRange = 10f..100f,
                        unit = "%",
                        technicalValue = "Comb Feedback Gain (0.40..0.988)",
                        showTechnical = uiState.showTechnicalValues,
                        accentColor = GlassTokens.IosIndigo,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

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
                                    "Standard decay based on Room Size."
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

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    // Echo / Delay Wet Slider
                    LiquidSlider(
                        title = "Echo Mix (Delay)",
                        value = uiState.echoWetPercent,
                        onValueChange = { viewModel.setEchoWet(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Analog Tape Output Blend",
                        showTechnical = uiState.showTechnicalValues,
                        accentColor = GlassTokens.IosTeal,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    // Echo Time Slider (ms)
                    LiquidSlider(
                        title = "Echo Time",
                        value = uiState.echoTimeMs.toFloat(),
                        onValueChange = { viewModel.setEchoTimeMs(it.toInt()) },
                        valueRange = 50f..2500f,
                        unit = "ms",
                        technicalValue = "${uiState.echoTimeMs}ms delay tap",
                        showTechnical = uiState.showTechnicalValues,
                        accentColor = GlassTokens.IosTeal,
                        reduceGlass = uiState.reduceGlass
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                    // Echo Feedback Slider (%)
                    LiquidSlider(
                        title = "Echo Feedback (Repeats)",
                        value = uiState.echoFeedbackPercent,
                        onValueChange = { viewModel.setEchoFeedback(it) },
                        valueRange = 0f..95f,
                        unit = "%",
                        technicalValue = "Tape Saturation Feedback Loop",
                        showTechnical = uiState.showTechnicalValues,
                        accentColor = GlassTokens.IosTeal,
                        reduceGlass = uiState.reduceGlass
                    )
                }
            }
        }
    }

    // Active Tooltip Dialog
    if (uiState.activeTooltipBand != null) {
        BandTooltipDialog(
            band = uiState.activeTooltipBand,
            onDismiss = { viewModel.showTooltip(null) }
        )
    }
}
