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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
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
import com.example.ui.components.IosRowSeparator
import com.example.ui.components.IosSectionHeader
import com.example.ui.components.IosSegmentedControl
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.iosInsetGroupedCard
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

/**
 * Settings Screen (Apple iOS Inset Grouped HIG):
 * - Output Device Profiles with Apple SF checkmark rows
 * - System Audio Routing & OEM hooking status
 * - Display & Accessibility options
 * - Signal Chain Architecture
 */
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
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Apple Large Title Header
        item {
            Column(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) {
                Text(
                    text = "Settings",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextPrimary,
                    letterSpacing = (-0.6).sp
                )
                Text(
                    text = "Acoustic calibration, accessibility & signal chain",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Section 1: Output Device Profiles (Apple Inset Grouped)
        item {
            IosSectionHeader(
                title = "Output Device Profiles",
                subtitle = "Auto-calibrates baseline Space & Punch for connected gear"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
            ) {
                Column {
                    val devices = OutputDevice.entries.toList()
                    devices.forEachIndexed { index, device ->
                        val isSelected = uiState.currentDevice == device
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setOutputDevice(device) }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("device_option_${device.name.lowercase()}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = if (isSelected) GlassTokens.IosBlue else GlassTokens.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = device.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) GlassTokens.TextPrimary else GlassTokens.TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = GlassTokens.IosBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < devices.size - 1) {
                            IosRowSeparator(modifier = Modifier.padding(start = 48.dp))
                        }
                    }
                }
            }
        }

        // Section 2: System-Wide Mode Status
        item {
            IosSectionHeader(
                title = "System Audio Routing",
                subtitle = "Session-0 global hook and OEM compatibility status"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (uiState.isGlobalHookActive) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (uiState.isGlobalHookActive) GlassTokens.IosGreen else GlassTokens.IosOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isGlobalHookActive) "System-Wide Mode Active" else "Limited System Routing",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTokens.TextPrimary
                        )
                    }
                    Text(
                        text = if (uiState.isGlobalHookActive) {
                            "Active on this device — effects process audio from other media apps. Apps with hardware-accelerated playback may occasionally bypass routing."
                        } else {
                            "Not supported on this device or output route. Your device ROM blocks master output hooking. Effects continue working for cooperating apps (Spotify, YouTube Music) via broadcast receivers."
                        },
                        fontSize = 12.sp,
                        color = GlassTokens.TextSecondary,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    SettingToggleRow(
                        title = "Legacy Mode (In-App Player)",
                        description = "Bypasses system-wide broadcast receiver and routes playback through internal engine. Recommended if OEM battery optimization suppresses audio sessions.",
                        checked = uiState.isLegacyMode,
                        onCheckedChange = { viewModel.toggleLegacyMode() },
                        testTag = "setting_legacy_mode"
                    )
                }
            }
        }

        // Section 3: Display & Accessibility Options
        item {
            IosSectionHeader(
                title = "Accessibility & Display",
                subtitle = "Legibility, contrast and animation controls"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Show Technical Values Toggle
                    SettingToggleRow(
                        title = "Show Technical Values",
                        description = "Displays precise Hz frequencies, Q values, and ratios beside plain-language sliders.",
                        checked = uiState.showTechnicalValues,
                        onCheckedChange = { viewModel.toggleShowTechnicalValues() },
                        testTag = "setting_technical_values"
                    )

                    IosRowSeparator()

                    // Reduce Glass Toggle
                    SettingToggleRow(
                        title = "Reduce Glass (High Contrast)",
                        description = "Replaces translucent frosted glass with solid high-contrast panels for maximum legibility.",
                        checked = uiState.reduceGlass,
                        onCheckedChange = { viewModel.toggleReduceGlass() },
                        testTag = "setting_reduce_glass"
                    )

                    IosRowSeparator()

                    // Reduce Motion Toggle
                    SettingToggleRow(
                        title = "Reduce Motion",
                        description = "Disables audio-reactive chromatic diffusion and animated transitions.",
                        checked = uiState.reduceMotion,
                        onCheckedChange = { viewModel.toggleReduceMotion() },
                        testTag = "setting_reduce_motion"
                    )

                    IosRowSeparator()

                    // Re-run Onboarding Tour
                    Button(
                        onClick = { viewModel.restartOnboarding() },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosGroupedSecondary),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, GlassTokens.IosSeparator),
                        shape = GlassTokens.radiusPill,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text("Re-run Onboarding Tour", color = GlassTokens.IosBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Section 4: Signal Chain Architecture
        item {
            IosSectionHeader(
                title = "Signal Chain Architecture",
                subtitle = "6-stage real-time DSP pipeline strictly ordered (PRD 8.1)"
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

                    IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "DSP STAGES IN SIGNAL ORDER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassTokens.TextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val stages = listOf(
                        "1. Noise Reduction (Spectral Gate, Notch, De-Crackle)",
                        "2. Equalizer (5-Band Plain / 10-Band Parametric IIR)",
                        "3. Clarity Macro (Multiband Harmonic Exciter)",
                        "4. Dynamics / Punch (RMS Soft-Knee Compressor)",
                        "5. Virtualizer / Space (HRTF Crossfeed Decorrelation)",
                        "6. Loudness Booster (True-Peak Soft Limiter)"
                    )

                    stages.forEachIndexed { i, stage ->
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

        // Section 5: Reset All Controls Button
        item {
            Button(
                onClick = { viewModel.resetAllToFlat() },
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosGroupedPrimary),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, GlassTokens.IosRed.copy(alpha = 0.5f)),
                shape = GlassTokens.radiusPill,
                modifier = Modifier.fillMaxWidth().testTag("reset_all_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = GlassTokens.IosRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reset All Sliders to Flat",
                    color = GlassTokens.IosRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
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
                color = GlassTokens.TextSecondary,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GlassTokens.IosGreen
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

