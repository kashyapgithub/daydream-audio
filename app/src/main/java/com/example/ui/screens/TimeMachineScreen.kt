package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.example.model.TimeMachinePreset
import com.example.ui.components.ABCompareBar
import com.example.ui.components.LiquidSlider
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.raisedGlass
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

@Composable
fun TimeMachineScreen(
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
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = GlassTokens.AccentStart,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Time Machine Presets",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                }
                Text(
                    text = "Restoration tailored to each era's medium (tape, vinyl, broadcast, early MP3)",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary
                )
            }
        }

        // Prominent A/B Bar
        item {
            ABCompareBar(
                isBypassed = uiState.isBypassed,
                onToggle = { viewModel.toggleBypassAB() },
                reduceGlass = uiState.reduceGlass
            )
        }

        // Section 1: Reverse Time Machine ("Vintage-ify", PRD 6.13)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = GlassTokens.AccentStart,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Vintage-ify (Reverse Engine)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassTokens.TextPrimary
                                )
                                Text(
                                    text = "Make modern audio sound like an analog memory",
                                    fontSize = 12.sp,
                                    color = GlassTokens.TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isVintageMode,
                            onCheckedChange = { viewModel.setVintageMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlassTokens.AccentStart
                            ),
                            modifier = Modifier.testTag("vintage_mode_switch")
                        )
                    }

                    if (uiState.isVintageMode) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Analog Tape & Turntable Simulation",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTokens.AccentStart
                        )

                        // Wow & Flutter Slider (PRD 6.13)
                        LiquidSlider(
                            title = "Tape Warble (Wow & Flutter)",
                            value = uiState.wowFlutterDepth,
                            onValueChange = { viewModel.setWowFlutterDepth(it) },
                            valueRange = 0f..100f,
                            unit = "%",
                            technicalValue = "LFO Dual Delay Pitch Mod",
                            showTechnical = uiState.showTechnicalValues,
                            reduceGlass = uiState.reduceGlass
                        )

                        // Synthesized Noise Slider (PRD 6.13)
                        LiquidSlider(
                            title = "Synthesized Hiss & Vinyl Crackle",
                            value = uiState.vintageNoiseLevel,
                            onValueChange = { viewModel.setVintageNoiseLevel(it) },
                            valueRange = 0f..100f,
                            unit = "%",
                            technicalValue = "Reverse Filter Noise Synthesis",
                            showTechnical = uiState.showTechnicalValues,
                            reduceGlass = uiState.reduceGlass
                        )
                    }
                }
            }
        }

        // Section 2: Time Machine Era Presets (PRD 6.11)
        item {
            Text(
                text = "Historical Era Presets",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.TextPrimary
            )
        }

        items(viewModel.timeMachinePresets) { preset ->
            val isActive = uiState.activePresetId == preset.id

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .clickable { viewModel.applyTimeMachinePreset(preset) }
                    .border(
                        1.5.dp,
                        if (isActive) GlassTokens.AccentStart else Color.White.copy(alpha = 0.12f),
                        GlassTokens.radiusMd
                    )
                    .padding(16.dp)
                    .testTag("preset_${preset.id}")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = preset.eraTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) GlassTokens.AccentStart else GlassTokens.TextPrimary
                            )
                            Text(
                                text = preset.subtitle,
                                fontSize = 12.sp,
                                color = GlassTokens.AccentStart.copy(alpha = 0.8f)
                            )
                        }

                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .clip(GlassTokens.radiusPill)
                                    .background(GlassTokens.AccentStart)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = preset.description,
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Small badges summarizing preset settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (preset.hissRemoval > 0f) {
                            PresetBadge(text = "Hiss: ${preset.hissRemoval.toInt()}%")
                        }
                        if (preset.deHumEnabled) {
                            PresetBadge(text = "De-Hum 60Hz")
                        }
                        PresetBadge(text = "Punch: ${preset.punchRatio.toInt()}%")
                        PresetBadge(text = "Space: ${preset.spacePercent.toInt()}%")
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(GlassTokens.radiusPill)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = GlassTokens.TextSecondary
        )
    }
}
