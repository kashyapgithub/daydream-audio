package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
    var selectedHrtf by remember { mutableStateOf(uiState.hrtfProfile) }

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
                            val isSelected = selectedHrtf == profile
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(GlassTokens.radiusPill)
                                    .background(
                                        if (isSelected) GlassTokens.AccentStart
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .clickable { selectedHrtf = profile }
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
                }
            }
        }

        // Preset Export / Share (PRD 6.2 & FR-12)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Preset Bundle",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                        Text(
                            text = "Save full 6-stage chain configuration as shareable JSON",
                            fontSize = 12.sp,
                            color = GlassTokens.TextSecondary
                        )
                    }

                    Button(
                        onClick = {
                            val json = viewModel.exportCurrentPresetJson()
                            clipboardManager.setText(AnnotatedString(json))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                        shape = GlassTokens.radiusPill
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
