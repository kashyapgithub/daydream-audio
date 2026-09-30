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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
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
import com.example.model.PlainBand
import com.example.ui.components.ABCompareBar
import com.example.ui.components.EarActivityRings
import com.example.ui.components.IosRowSeparator
import com.example.ui.components.IosSectionHeader
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.iosInsetGroupedCard
import com.example.ui.theme.raisedGlass
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

@Composable
fun GoldenEarScreen(
    viewModel: DaydreamViewModel,
    uiState: DaydreamUiState,
    paddingValues: PaddingValues
) {
    val challenge = uiState.activeChallenge

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
                    text = "Golden Ear",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextPrimary,
                    letterSpacing = (-0.6).sp
                )
                Text(
                    text = "Train your ears to recognize frequency bands and acoustic changes",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Apple Fitness-Style Metric Cards with Activity Rings (Score, Streak, Rank)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(vertical = 12.dp, horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Apple Watch Activity Rings
                    EarActivityRings(
                        score = uiState.earTrainerScore,
                        streak = uiState.earTrainerStreak,
                        challengesCompleted = uiState.earTrainerScore / 100
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatBox(title = "SCORE", value = "${uiState.earTrainerScore}", unit = "pts", tint = GlassTokens.IosBlue)
                        Box(modifier = Modifier.width(0.8.dp).height(32.dp).background(GlassTokens.IosSeparator))
                        StatBox(title = "STREAK", value = "${uiState.earTrainerStreak}", unit = "🔥", tint = GlassTokens.IosOrange)
                        Box(modifier = Modifier.width(0.8.dp).height(32.dp).background(GlassTokens.IosSeparator))
                        StatBox(title = "RANK", value = uiState.earTrainerLevel, unit = "", tint = GlassTokens.IosGreen)
                    }
                }
            }
        }

        // Active Challenge Card
        // Active Challenge Card (Apple Inset Grouped Section)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosInsetGroupedCard(uiState.reduceGlass)
                    .padding(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (challenge == null) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(GlassTokens.radiusMd)
                                .background(GlassTokens.IosBlue.copy(alpha = 0.14f))
                                .border(0.8.dp, GlassTokens.IosBlue.copy(alpha = 0.35f), GlassTokens.radiusMd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = GlassTokens.IosBlue,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Test Your Hearing Acuity",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                        Text(
                            text = "We will modify one plain-language frequency band. Listen carefully and guess which one changed!",
                            fontSize = 13.sp,
                            color = GlassTokens.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (!uiState.isPlaying && !uiState.isExternalPlaybackActive) viewModel.togglePlayPause()
                                viewModel.startNewEarChallenge()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                            shape = GlassTokens.radiusPill,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("start_challenge_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Challenge", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Challenge #${challenge.questionNumber}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.IosBlue
                            )
                            Button(
                                onClick = { viewModel.startNewEarChallenge() },
                                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosGroupedSecondary),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, GlassTokens.IosSeparator),
                                shape = GlassTokens.radiusPill,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = GlassTokens.IosBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Sound", fontSize = 11.sp, color = GlassTokens.IosBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PRD §22.7 & §22.8: Sonic Glass Visual Hint Layer toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassTokens.radiusSm)
                                .background(GlassTokens.IosGroupedSecondary)
                                .border(0.6.dp, GlassTokens.IosSeparator, GlassTokens.radiusSm)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Visual Hint Assistant",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassTokens.TextPrimary
                                )
                                Text(
                                    text = "Subtle reactive spectral glow on target band",
                                    fontSize = 11.sp,
                                    color = GlassTokens.TextSecondary
                                )
                            }
                            Switch(
                                checked = uiState.showSonicHintInEarTrainer,
                                onCheckedChange = { viewModel.toggleSonicHintInEarTrainer() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GlassTokens.IosGreen
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Which plain-language band was boosted or cut?",
                            fontSize = 14.sp,
                            color = GlassTokens.TextPrimary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Band Guess Buttons (Apple Grouped Selectable Rows)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassTokens.radiusMd)
                                .background(GlassTokens.IosGroupedSecondary)
                                .border(0.8.dp, GlassTokens.IosSeparator, GlassTokens.radiusMd)
                        ) {
                            val bands = PlainBand.entries.toList()
                            bands.forEachIndexed { index, band ->
                                val isHinted = uiState.showSonicHintInEarTrainer && challenge.targetBand == band
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.submitEarGuess(band) }
                                        .background(
                                            if (isHinted) GlassTokens.IosBlue.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                        .testTag("guess_button_${band.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = band.title,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isHinted) GlassTokens.IosBlue else GlassTokens.TextPrimary
                                                )
                                                if (isHinted) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                                            .background(GlassTokens.IosBlue)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = band.frequencyRange,
                                                fontSize = 11.sp,
                                                color = GlassTokens.TextSecondary
                                            )
                                        }
                                        Text(
                                            text = band.plainDescription,
                                            fontSize = 11.sp,
                                            color = GlassTokens.TextMuted,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (index < bands.size - 1) {
                                    IosRowSeparator()
                                }
                            }
                        }

                        // Feedback on last answer (Apple Alert Pill)
                        AnimatedVisibility(visible = uiState.lastAnswerCorrect != null) {
                            val correct = uiState.lastAnswerCorrect == true
                            val tintColor = if (correct) GlassTokens.IosGreen else GlassTokens.IosRed
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                                    .clip(GlassTokens.radiusMd)
                                    .background(tintColor.copy(alpha = 0.15f))
                                    .border(0.8.dp, tintColor.copy(alpha = 0.5f), GlassTokens.radiusMd)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (correct) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = tintColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (correct) "Correct! +100 Points" else "Incorrect — listen to the A/B difference!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tintColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // A/B Comparison Bar for Listening to Difference
        item {
            ABCompareBar(
                isBypassed = uiState.isBypassed,
                onToggle = { viewModel.toggleBypassAB() },
                reduceGlass = uiState.reduceGlass
            )
        }
    }
}

@Composable
private fun StatBox(title: String, value: String, unit: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = GlassTokens.TextSecondary,
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = GlassTokens.TextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
