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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.LiquidSlider
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.raisedGlass
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

@Composable
fun AdvancedModeScreen(
    viewModel: DaydreamViewModel,
    uiState: DaydreamUiState,
    paddingValues: PaddingValues
) {
    var selectedHrtf by remember { mutableStateOf(uiState.hrtfProfile) }
    var compThreshold by remember { mutableFloatStateOf(uiState.compThresholdDb) }
    var compRatio by remember { mutableFloatStateOf(uiState.compRatio) }
    var compAttack by remember { mutableFloatStateOf(uiState.compAttackMs) }
    var compRelease by remember { mutableFloatStateOf(uiState.compReleaseMs) }

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
                    text = "10-Band Parametric EQ & Dynamics Compressor with plain-language anchors",
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

        // 10-Band Parametric EQ Grid (PRD 6.2)
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
                        text = "Real Hz frequencies mapped to plain-English acoustic character",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val hzLabels = listOf(
                        Triple(31, "Sub-Rumble", PlainBand.RUMBLE),
                        Triple(63, "Deep Bass", PlainBand.RUMBLE),
                        Triple(125, "Warmth Punch", PlainBand.WARMTH),
                        Triple(250, "Low-Mid Fullness", PlainBand.WARMTH),
                        Triple(500, "Body / Boxiness", PlainBand.BODY),
                        Triple(1000, "Vocal Presence", PlainBand.BODY),
                        Triple(2000, "Instrument Edge", PlainBand.CLARITY),
                        Triple(4000, "Vocal Articulation", PlainBand.CLARITY),
                        Triple(8000, "Treble Detail", PlainBand.AIR),
                        Triple(16000, "Air / Sparkle", PlainBand.AIR)
                    )

                    hzLabels.forEach { (hz, anchor, plainBand) ->
                        val currentGain = uiState.eqGains[plainBand] ?: 0f
                        LiquidSlider(
                            title = if (hz < 1000) "${hz}Hz • $anchor" else "${hz / 1000}kHz • $anchor",
                            value = currentGain,
                            onValueChange = { viewModel.setEqGain(plainBand, it) },
                            valueRange = -12f..12f,
                            unit = "dB",
                            technicalValue = "Q=0.8 Peaking",
                            showTechnical = true,
                            reduceGlass = uiState.reduceGlass
                        )
                    }
                }
            }
        }

        // Dynamics Compressor Controls (PRD 6.2)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Dynamics Processing (Compressor)",
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
                        value = compThreshold,
                        onValueChange = { compThreshold = it },
                        valueRange = -40f..0f,
                        unit = "dB",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    LiquidSlider(
                        title = "Compression Ratio (Intensity)",
                        value = compRatio,
                        onValueChange = {
                            compRatio = it
                            viewModel.setPunchPercent(it * 10f)
                        },
                        valueRange = 1f..10f,
                        unit = ":1",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    LiquidSlider(
                        title = "Attack Time (Speed of clamp)",
                        value = compAttack,
                        onValueChange = { compAttack = it },
                        valueRange = 1f..100f,
                        unit = "ms",
                        showTechnical = true,
                        reduceGlass = uiState.reduceGlass
                    )

                    LiquidSlider(
                        title = "Release Time (Recovery)",
                        value = compRelease,
                        onValueChange = { compRelease = it },
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

        // Preset Export / Share (PRD 6.2)
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
                        onClick = { /* Export simulation */ },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                        shape = GlassTokens.radiusPill
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
