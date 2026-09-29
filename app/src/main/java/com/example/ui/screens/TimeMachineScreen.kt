package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimeMachinePreset
import com.example.ui.components.ABCompareBar
import com.example.ui.components.AnalogCassetteDeck
import com.example.ui.components.IosRowSeparator
import com.example.ui.components.IosSectionHeader
import com.example.ui.components.LiquidSlider
import com.example.ui.components.VinylTurntableDeck
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.iosInsetGroupedCard
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
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Apple Large Title Header
        item {
            Column(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) {
                Text(
                    text = "Time Machine",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextPrimary,
                    letterSpacing = (-0.6).sp
                )
                Text(
                    text = "Restoration tailored to each era's medium (tape, vinyl, broadcast, MP3)",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Prominent Apple A/B Bar
        item {
            ABCompareBar(
                isBypassed = uiState.isBypassed,
                onToggle = { viewModel.toggleBypassAB() },
                reduceGlass = uiState.reduceGlass
            )
        }

        // Section 1: Reverse Time Machine ("Vintage-ify", PRD 6.13)
        item {
            IosSectionHeader(
                title = "Vintage-ify Engine",
                subtitle = "Make modern digital audio sound like an analog memory"
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
                            Text(
                                text = "Vintage-ify (Reverse Engine)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = "Synthesize analog tape warble and vinyl surface dust",
                                fontSize = 12.sp,
                                color = GlassTokens.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Switch(
                            checked = uiState.isVintageMode,
                            onCheckedChange = { viewModel.setVintageMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlassTokens.IosGreen
                            ),
                            modifier = Modifier.testTag("vintage_mode_switch")
                        )
                    }

                    if (uiState.isVintageMode) {
                        IosRowSeparator(modifier = Modifier.padding(vertical = 12.dp))

                        // Vintage Analog Cassette Deck Animation
                        AnalogCassetteDeck(
                            isPlaying = uiState.isPlaying,
                            audioRms = uiState.audioRms,
                            modifier = Modifier.padding(bottom = 12.dp)
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
                            accentColor = GlassTokens.IosOrange,
                            reduceGlass = uiState.reduceGlass
                        )

                        IosRowSeparator(modifier = Modifier.padding(vertical = 4.dp))

                        // Synthesized Noise Slider (PRD 6.13)
                        LiquidSlider(
                            title = "Synthesized Hiss & Vinyl Crackle",
                            value = uiState.vintageNoiseLevel,
                            onValueChange = { viewModel.setVintageNoiseLevel(it) },
                            valueRange = 0f..100f,
                            unit = "%",
                            technicalValue = "Reverse Filter Noise Synthesis",
                            showTechnical = uiState.showTechnicalValues,
                            accentColor = GlassTokens.IosTeal,
                            reduceGlass = uiState.reduceGlass
                        )
                    }
                }
            }
        }
        // Section 2: Time Machine Era Presets & Memory Postcard (Apple Music Curated Stations)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosSectionHeader(
                    title = "Historical Era Presets",
                    subtitle = "Cupertino-calibrated restoration profiles",
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { viewModel.openMemoryPostcardDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                    shape = GlassTokens.radiusPill,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        items(viewModel.timeMachinePresets) { preset ->
            val isActive = uiState.activePresetId == preset.id

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .clickable { viewModel.applyTimeMachinePreset(preset) }
                    .border(
                        if (isActive) 1.2.dp else 0.8.dp,
                        if (isActive) GlassTokens.IosBlue else GlassTokens.IosSeparator,
                        GlassTokens.radiusLg
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
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = preset.eraTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) GlassTokens.IosBlue else GlassTokens.TextPrimary
                            )
                            Text(
                                text = preset.subtitle,
                                fontSize = 12.sp,
                                color = if (isActive) GlassTokens.IosBlue.copy(alpha = 0.85f) else GlassTokens.TextSecondary,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }

                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .clip(GlassTokens.radiusPill)
                                    .background(GlassTokens.IosBlue)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = preset.description,
                        fontSize = 13.sp,
                        color = GlassTokens.TextSecondary,
                        lineHeight = 17.sp
                    )

                    if (isActive && preset.id == "70s") {
                        Spacer(modifier = Modifier.height(10.dp))
                        VinylTurntableDeck(
                            isPlaying = uiState.isPlaying,
                            trackTitle = uiState.currentTrack?.title,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else if (isActive && preset.id == "80s") {
                        Spacer(modifier = Modifier.height(10.dp))
                        AnalogCassetteDeck(
                            isPlaying = uiState.isPlaying,
                            audioRms = uiState.audioRms,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badges summarizing preset settings (Apple Inset Capsules)
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

    if (uiState.showMemoryPostcardDialog) {
        MemoryPostcardDialog(
            uiState = uiState,
            onDismiss = { viewModel.closeMemoryPostcardDialog() }
        )
    }
}

/**
 * Apple Modal Sheet Memory Postcard (OpenDesign Apple Design System & HIG)
 */
@Composable
fun MemoryPostcardDialog(
    uiState: DaydreamUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activePresetName = uiState.activePresetId?.let { id ->
        when (id) {
            "70s_vinyl" -> "1970s Warm Vinyl"
            "80s_cassette" -> "1980s Type II Cassette"
            "90s_broadcast" -> "1990s FM Broadcast"
            "00s_early_mp3" -> "2000s 128kbps MP3"
            else -> id.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    } ?: if (uiState.isVintageMode) "Vintage-ify Acoustic Profile" else "Time Machine Master"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val shareText = "🎧 Listening with Daydream Audio Time Machine ($activePresetName)\n" +
                            "Warble: ${uiState.wowFlutterDepth.toInt()}% | Crackle: ${uiState.vintageNoiseLevel.toInt()}%\n" +
                            "✨ Enhanced with Daydream Audio's Liquid Glass DSP engine"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Memory Postcard"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                shape = GlassTokens.radiusPill,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share to Stories / WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Done", color = GlassTokens.TextSecondary)
            }
        },
        containerColor = GlassTokens.IosGroupedPrimary,
        shape = GlassTokens.radiusXl,
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
                        text = "Memory Postcard",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = GlassTokens.TextSecondary)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Postcard Preview Card (Apple Grouped Dark Inset)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GlassTokens.radiusLg)
                        .background(GlassTokens.IosGroupedSecondary)
                        .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusLg)
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Apple Capsule
                        Box(
                            modifier = Modifier
                                .clip(GlassTokens.radiusPill)
                                .background(GlassTokens.IosBlue.copy(alpha = 0.15f))
                                .border(0.6.dp, GlassTokens.IosBlue.copy(alpha = 0.5f), GlassTokens.radiusPill)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "DAYDREAM TIME MACHINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.IosBlue,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = GlassTokens.IosBlue,
                            modifier = Modifier.size(44.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = activePresetName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )

                        Text(
                            text = "Acoustic Medium Emulation",
                            fontSize = 12.sp,
                            color = GlassTokens.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Waveform simulation bars in Apple Cyan/Teal
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val barHeights = listOf(14.dp, 28.dp, 20.dp, 36.dp, 44.dp, 30.dp, 22.dp, 38.dp, 16.dp, 26.dp)
                            barHeights.forEachIndexed { i, h ->
                                val color = if (i % 2 == 0) GlassTokens.IosTeal else GlassTokens.IosBlue
                                Box(
                                    modifier = Modifier
                                        .width(4.5.dp)
                                        .height(h)
                                        .clip(GlassTokens.radiusPill)
                                        .background(color)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PresetBadge(text = "Warble: ${uiState.wowFlutterDepth.toInt()}%")
                            PresetBadge(text = "Noise: ${uiState.vintageNoiseLevel.toInt()}%")
                            PresetBadge(text = uiState.vintageEraName)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "✨ Crafted with Daydream Audio ✨",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlassTokens.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Ready to share with friends on WhatsApp Status, Instagram Stories, or Reels.",
                    fontSize = 12.sp,
                    color = GlassTokens.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
private fun PresetBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(GlassTokens.radiusPill)
            .background(GlassTokens.IosGroupedSecondary)
            .border(0.6.dp, GlassTokens.IosSeparator, GlassTokens.radiusPill)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = GlassTokens.TextSecondary
        )
    }
}
