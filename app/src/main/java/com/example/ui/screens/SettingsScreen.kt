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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
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
import com.example.model.OutputDevice
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.baseGlass
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

@Composable
fun SettingsScreen(
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
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = GlassTokens.AccentStart,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings & Devices",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                }
                Text(
                    text = "Acoustic calibration, accessibility & signal chain",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary
                )
            }
        }

        // Section 1: Output Device Profiles (PRD 6.4)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .baseGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = GlassTokens.AccentStart,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Output Device Profiles",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                    }
                    Text(
                        text = "Auto-calibrates baseline space and punch for your connected gear",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    OutputDevice.entries.forEach { device ->
                        val isSelected = uiState.currentDevice == device
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(GlassTokens.radiusMd)
                                .background(
                                    if (isSelected) GlassTokens.AccentStart.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) GlassTokens.AccentStart else Color.White.copy(alpha = 0.1f),
                                    GlassTokens.radiusMd
                                )
                                .clickable { viewModel.setOutputDevice(device) }
                                .padding(12.dp)
                                .testTag("device_option_${device.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = device.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) GlassTokens.AccentStart else GlassTokens.TextPrimary
                                )
                                if (isSelected) {
                                    Text(
                                        text = "SELECTED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlassTokens.AccentStart
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Accessibility & Display Options (PRD 22.3, 22.13)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .baseGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = null,
                            tint = GlassTokens.AccentStart,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Display & Accessibility",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                    }

                    // Show Technical Values Toggle (PRD Section 5)
                    SettingToggleRow(
                        title = "Show Technical Values",
                        description = "Displays precise Hz frequencies, Q values, and ratios beside plain-language sliders.",
                        checked = uiState.showTechnicalValues,
                        onCheckedChange = { viewModel.toggleShowTechnicalValues() },
                        testTag = "setting_technical_values"
                    )

                    // Reduce Glass Toggle (PRD 22.3)
                    SettingToggleRow(
                        title = "Reduce Glass (High Contrast)",
                        description = "Replaces translucent frosted glass with solid high-contrast panels for maximum legibility.",
                        checked = uiState.reduceGlass,
                        onCheckedChange = { viewModel.toggleReduceGlass() },
                        testTag = "setting_reduce_glass"
                    )

                    // Reduce Motion Toggle (PRD 22.12)
                    SettingToggleRow(
                        title = "Reduce Motion",
                        description = "Disables audio-reactive Sonic Glass background ripples and animated transitions.",
                        checked = uiState.reduceMotion,
                        onCheckedChange = { viewModel.toggleReduceMotion() },
                        testTag = "setting_reduce_motion"
                    )
                }
            }
        }

        // Section 3: Signal Chain Architecture (PRD 8.1)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .baseGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GlassTokens.AccentStart,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Audio Signal Chain (PRD 8.1)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                    }
                    Text(
                        text = "Rigorous real-time ordering strictly enforced to prevent noise amplification:",
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )

                    val stages = listOf(
                        "1. Noise Reduction (Hiss, De-Hum 60Hz, De-Crackle)",
                        "2. Equalizer (5-Band Peaking & Shelving IIR)",
                        "3. Clarity Macro (Harmonic waveshaping & presence)",
                        "4. Dynamics / Punch (RMS Soft-Knee Compressor)",
                        "5. Virtualizer / Space (Crossfeed decorrelation)",
                        "6. Loudness Booster + True-Peak Soft Limiter"
                    )

                    stages.forEach { stage ->
                        Text(
                            text = stage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlassTokens.TextPrimary,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Reset All Controls Button
        item {
            Button(
                onClick = { viewModel.resetAllToFlat() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = GlassTokens.radiusPill,
                modifier = Modifier.fillMaxWidth().testTag("reset_all_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reset All Sliders to Flat",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = GlassTokens.TextPrimary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = GlassTokens.TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GlassTokens.AccentStart
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
