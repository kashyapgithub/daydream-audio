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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlainBand
import com.example.ui.components.ABCompareBar
import com.example.ui.components.BandTooltipDialog
import com.example.ui.components.LiquidSlider
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.raisedGlass
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

@Composable
fun SimpleModeScreen(
    viewModel: DaydreamViewModel,
    uiState: DaydreamUiState,
    paddingValues: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Header & Quick Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daydream Audio",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "Sound the way you remember it",
                        fontSize = 13.sp,
                        color = GlassTokens.TextSecondary
                    )
                }

                // Wizard Diagnosis Button
                Button(
                    onClick = { viewModel.openWizardDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                    shape = GlassTokens.radiusPill,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("wizard_trigger_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
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

        // System Audio Hook Status & Output Device Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Profile Chip
                Row(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), GlassTokens.radiusPill)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = GlassTokens.AccentStart,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.currentDevice.displayName,
                        fontSize = 12.sp,
                        color = GlassTokens.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // System Audio Session Hook Status or Legacy Mode (PRD 8.0, 9.0 & FR-11)
                Row(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(
                            when {
                                uiState.isLegacyMode -> GlassTokens.AccentStart.copy(alpha = 0.18f)
                                uiState.activeSystemSessions.isNotEmpty() -> GlassTokens.AccentSafe.copy(alpha = 0.15f)
                                else -> Color.White.copy(alpha = 0.08f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                uiState.isLegacyMode -> GlassTokens.AccentStart.copy(alpha = 0.6f)
                                uiState.activeSystemSessions.isNotEmpty() -> GlassTokens.AccentSafe.copy(alpha = 0.5f)
                                else -> Color.White.copy(alpha = 0.15f)
                            },
                            GlassTokens.radiusPill
                        )
                        .clickable { viewModel.toggleLegacyMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(GlassTokens.radiusPill)
                            .background(
                                when {
                                    uiState.isLegacyMode -> GlassTokens.AccentStart
                                    uiState.activeSystemSessions.isNotEmpty() -> GlassTokens.AccentSafe
                                    else -> Color.White.copy(alpha = 0.4f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            uiState.isLegacyMode -> "Legacy Mode (In-App Player)"
                            uiState.activeSystemSessions.isNotEmpty() -> "Hooked: ${uiState.activeSystemSessions.first()}"
                            else -> "System Audio: Listening"
                        },
                        fontSize = 12.sp,
                        color = when {
                            uiState.isLegacyMode -> GlassTokens.AccentStart
                            uiState.activeSystemSessions.isNotEmpty() -> GlassTokens.AccentSafe
                            else -> GlassTokens.TextSecondary
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // OEM Hooking Guidance Banner (PRD Section 9.0 & FR-11)
        if (uiState.activeSystemSessions.isEmpty() && !uiState.isLegacyMode) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusMd)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), GlassTokens.radiusMd)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "💡 Listening for external audio...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = "In Spotify/YT Music, turn ON 'Device Broadcast Status'. On Xiaomi/Samsung, switch to Legacy In-App Player.",
                                fontSize = 11.sp,
                                color = GlassTokens.TextSecondary
                            )
                        }
                        Button(
                            onClick = { viewModel.toggleLegacyMode() },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart.copy(alpha = 0.25f)),
                            shape = GlassTokens.radiusPill,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Legacy", fontSize = 11.sp, color = GlassTokens.AccentStart, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Output Device Change Prompt Banner (PRD FR-10)
        if (uiState.devicePrompt != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusMd)
                        .background(GlassTokens.AccentStart.copy(alpha = 0.18f))
                        .border(1.dp, GlassTokens.AccentStart, GlassTokens.radiusMd)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🎧 Switch to ${uiState.devicePrompt.displayName}?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.AccentStart
                            )
                            Text(
                                text = "Audio output change detected. Tap to tune baseline Space & Punch.",
                                fontSize = 11.sp,
                                color = GlassTokens.TextSecondary
                            )
                        }
                        Row {
                            Button(
                                onClick = { viewModel.setOutputDevice(uiState.devicePrompt) },
                                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                                shape = GlassTokens.radiusPill,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Tune", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { viewModel.dismissDevicePrompt() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Dismiss", fontSize = 12.sp, color = GlassTokens.TextMuted)
                            }
                        }
                    }
                }
            }
        }

        // Active Wizard Fix Banner (if applied)
        if (uiState.lastWizardFixSummary != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusMd)
                        .background(GlassTokens.AccentStart.copy(alpha = 0.15f))
                        .border(1.dp, GlassTokens.AccentStart.copy(alpha = 0.4f), GlassTokens.radiusMd)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✨ Active Fix: ${uiState.lastAppliedComplaint?.label ?: "Smart Tune"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.AccentStart
                            )
                            Text(
                                text = uiState.lastWizardFixSummary,
                                fontSize = 11.sp,
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

        // Prominent A/B Instant Compare Bar (PRD FR-3)
        item {
            ABCompareBar(
                isBypassed = uiState.isBypassed,
                onToggle = { viewModel.toggleBypassAB() },
                reduceGlass = uiState.reduceGlass
            )
        }

        // Mono Warning Banner (PRD FR-4)
        item {
            AnimatedVisibility(visible = uiState.showMonoWarning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusMd)
                        .background(GlassTokens.AccentWarning.copy(alpha = 0.15f))
                        .border(1.dp, GlassTokens.AccentWarning.copy(alpha = 0.4f), GlassTokens.radiusMd)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = GlassTokens.AccentWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Mono Recording Detected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.AccentWarning
                            )
                            Text(
                                text = "Virtualizer 'Space' is capped. Expanding mono audio too far creates phase cancellation and hollow vocals.",
                                fontSize = 11.sp,
                                color = GlassTokens.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Lofi Mode Macro Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GlassTokens.radiusMd)
                    .background(
                        if (uiState.isLofiMode) GlassTokens.AccentStart.copy(alpha = 0.20f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (uiState.isLofiMode) GlassTokens.AccentStart else Color.White.copy(alpha = 0.12f),
                        GlassTokens.radiusMd
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "☕ Lofi Mode",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isLofiMode) GlassTokens.AccentStart else GlassTokens.TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(GlassTokens.radiusPill)
                                        .background(
                                            if (uiState.isLofiMode) GlassTokens.AccentStart.copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            1.dp,
                                            if (uiState.isLofiMode) GlassTokens.AccentStart else Color.White.copy(alpha = 0.15f),
                                            GlassTokens.radiusPill
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isLofiMode) "0.85x • Reverb • Warble" else "1-Tap Chill",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isLofiMode) GlassTokens.AccentStart else GlassTokens.TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "Slowed tempo, dreamy algorithmic reverb, warm tape rolloff & subtle flutter",
                                fontSize = 11.sp,
                                color = GlassTokens.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Switch(
                            checked = uiState.isLofiMode,
                            onCheckedChange = { viewModel.toggleLofiMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlassTokens.AccentStart
                            ),
                            modifier = Modifier.testTag("lofi_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Playback Tempo selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.80f to "0.80x", 0.85f to "0.85x", 0.90f to "0.90x", 1.0f to "1.0x (Norm)", 1.15f to "1.15x").forEach { (speed, label) ->
                            val isSelected = kotlin.math.abs(uiState.playbackSpeed - speed) < 0.02f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(GlassTokens.radiusSm)
                                    .background(
                                        if (isSelected) GlassTokens.AccentStart.copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) GlassTokens.AccentStart else Color.White.copy(alpha = 0.10f),
                                        GlassTokens.radiusSm
                                    )
                                    .clickable { viewModel.setPlaybackSpeed(speed) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) GlassTokens.AccentStart else GlassTokens.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 1: Restoration & Noise Reduction (PRD 6.10 Tier 1)
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
                        Column {
                            Text(
                                text = "Analog Restoration",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = "Eliminates hiss, vinyl clicks, and power hum",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                        reduceGlass = uiState.reduceGlass
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // De-Hum & De-Crackle Toggles (PRD FR-7: separate controls)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // De-Hum Toggle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(GlassTokens.radiusSm)
                                .background(Color.White.copy(alpha = if (uiState.deHumEnabled) 0.12f else 0.05f))
                                .border(
                                    1.dp,
                                    if (uiState.deHumEnabled) GlassTokens.AccentStart else Color.White.copy(alpha = 0.1f),
                                    GlassTokens.radiusSm
                                )
                                .clickable { viewModel.toggleDeHum() }
                                .padding(10.dp)
                                .testTag("dehum_toggle")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "De-Hum",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.deHumEnabled) GlassTokens.AccentStart else GlassTokens.TextPrimary
                                    )
                                    Text(
                                        text = "50/60Hz notch",
                                        fontSize = 11.sp,
                                        color = GlassTokens.TextSecondary
                                    )
                                }
                                Switch(
                                    checked = uiState.deHumEnabled,
                                    onCheckedChange = { viewModel.toggleDeHum() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassTokens.AccentStart
                                    )
                                )
                            }
                        }

                        // De-Crackle Toggle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(GlassTokens.radiusSm)
                                .background(Color.White.copy(alpha = if (uiState.deCrackleEnabled) 0.12f else 0.05f))
                                .border(
                                    1.dp,
                                    if (uiState.deCrackleEnabled) GlassTokens.AccentStart else Color.White.copy(alpha = 0.1f),
                                    GlassTokens.radiusSm
                                )
                                .clickable { viewModel.toggleDeCrackle() }
                                .padding(10.dp)
                                .testTag("decrackle_toggle")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "De-Crackle",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.deCrackleEnabled) GlassTokens.AccentStart else GlassTokens.TextPrimary
                                    )
                                    Text(
                                        text = "Vinyl pops",
                                        fontSize = 11.sp,
                                        color = GlassTokens.TextSecondary
                                    )
                                }
                                Switch(
                                    checked = uiState.deCrackleEnabled,
                                    onCheckedChange = { viewModel.toggleDeCrackle() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassTokens.AccentStart
                                    )
                                )
                            }
                        }
                    }

                    // Regional Mains Frequency Selector (PRD 8.3)
                    AnimatedVisibility(visible = uiState.deHumEnabled) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "Mains Frequency Notch",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlassTokens.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(50 to "50Hz (EU/Asia/UK)", 60 to "60Hz (US/Americas)").forEach { (freq, label) ->
                                    val isSelected = uiState.humFrequency == freq
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(GlassTokens.radiusSm)
                                            .background(
                                                if (isSelected) GlassTokens.AccentStart.copy(alpha = 0.22f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) GlassTokens.AccentStart else Color.White.copy(alpha = 0.10f),
                                                GlassTokens.radiusSm
                                            )
                                            .clickable { viewModel.setHumFrequency(freq) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) GlassTokens.AccentStart else GlassTokens.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 2: Plain-English Equalizer (PRD 6.1)
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
                        Column {
                            Text(
                                text = "Tone Shaper",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = "5 plain-English bands — tap any title for info",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PlainBand.entries.forEach { band ->
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
                            reduceGlass = uiState.reduceGlass
                        )
                    }
                }
            }
        }

        // Card 3: Dynamics, Space & Loudness (PRD 6.3, 6.6, 6.8, 6.9)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Acoustic Presence & Space",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "Compressor punch, spatial width & volume booster",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Space (Virtualizer) — PRD FR-4: Auto-detect mono and cap at 35%
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
                        reduceGlass = uiState.reduceGlass
                    )

                    // Punch (Dynamics Compressor)
                    LiquidSlider(
                        title = "Punch (Dynamic Range)",
                        value = uiState.punchPercent,
                        onValueChange = { viewModel.setPunchPercent(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "RMS Soft-Knee Compressor",
                        showTechnical = uiState.showTechnicalValues,
                        reduceGlass = uiState.reduceGlass
                    )

                    // Clarity Macro (Presence exciter)
                    LiquidSlider(
                        title = "Vocal Clarity Macro",
                        value = uiState.clarityMacroPercent,
                        onValueChange = { viewModel.setClarityMacroPercent(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Multiband Harmonic Exciter",
                        showTechnical = uiState.showTechnicalValues,
                        reduceGlass = uiState.reduceGlass
                    )

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
                        accentColor = if (uiState.loudnessPercent > 75f) GlassTokens.AccentWarning else GlassTokens.AccentSafe,
                        reduceGlass = uiState.reduceGlass
                    )
                }
            }
        }

        // Card 4: Atmospheric Reverb & Echo Delay
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
                        Column {
                            Text(
                                text = "Atmospheric Reverb & Echo",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = "Algorithmic room reverb and tape delay repeats",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary
                            )
                        }

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
                                    tint = GlassTokens.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reverb Wet Slider
                    LiquidSlider(
                        title = "Reverb (Space Decay)",
                        value = uiState.reverbWetPercent,
                        onValueChange = { viewModel.setReverbWet(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Freeverb 8-Comb + 4-Allpass",
                        showTechnical = uiState.showTechnicalValues,
                        reduceGlass = uiState.reduceGlass
                    )

                    // Reverb Room Size Slider
                    LiquidSlider(
                        title = "Reverb Room Size",
                        value = uiState.reverbRoomSizePercent,
                        onValueChange = { viewModel.setReverbRoomSize(it) },
                        valueRange = 10f..100f,
                        unit = "%",
                        technicalValue = "Comb Feedback Gain",
                        showTechnical = uiState.showTechnicalValues,
                        reduceGlass = uiState.reduceGlass
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Echo / Delay Wet Slider
                    LiquidSlider(
                        title = "Echo Mix (Delay)",
                        value = uiState.echoWetPercent,
                        onValueChange = { viewModel.setEchoWet(it) },
                        valueRange = 0f..100f,
                        unit = "%",
                        technicalValue = "Stereo Ping-Pong Delay Line",
                        showTechnical = uiState.showTechnicalValues,
                        reduceGlass = uiState.reduceGlass
                    )

                    // Echo Time Slider (ms)
                    LiquidSlider(
                        title = "Echo Time",
                        value = uiState.echoTimeMs.toFloat(),
                        onValueChange = { viewModel.setEchoTimeMs(it.toInt()) },
                        valueRange = 50f..800f,
                        unit = "ms",
                        technicalValue = "${uiState.echoTimeMs}ms delay tap",
                        showTechnical = uiState.showTechnicalValues,
                        reduceGlass = uiState.reduceGlass
                    )

                    // Echo Feedback Slider (%)
                    LiquidSlider(
                        title = "Echo Feedback (Repeats)",
                        value = uiState.echoFeedbackPercent,
                        onValueChange = { viewModel.setEchoFeedback(it) },
                        valueRange = 0f..80f,
                        unit = "%",
                        technicalValue = "Tape-Damped Loop",
                        showTechnical = uiState.showTechnicalValues,
                        reduceGlass = uiState.reduceGlass
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Playback Speed Slider
                    LiquidSlider(
                        title = "Playback Tempo",
                        value = uiState.playbackSpeed,
                        onValueChange = { viewModel.setPlaybackSpeed(it) },
                        valueRange = 0.5f..1.5f,
                        unit = "x",
                        technicalValue = "Sonic Time-Stretch",
                        showTechnical = uiState.showTechnicalValues,
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
