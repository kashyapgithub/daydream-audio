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
import com.example.ui.theme.GlassTokens
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
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = GlassTokens.AccentStart,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Golden Ear Trainer",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                }
                Text(
                    text = "Train your ears to recognize frequency bands and acoustic changes",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary
                )
            }
        }

        // Stats Card (Score, Streak, Rank)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBox(title = "Score", value = "${uiState.earTrainerScore} pts")
                    StatBox(title = "Streak", value = "${uiState.earTrainerStreak} 🔥")
                    StatBox(title = "Rank", value = uiState.earTrainerLevel)
                }
            }
        }

        // Active Challenge Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .raisedGlass(uiState.reduceGlass)
                    .padding(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (challenge == null) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = GlassTokens.AccentStart,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
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
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                if (!uiState.isPlaying) viewModel.togglePlayPause()
                                viewModel.startNewEarChallenge()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                            shape = GlassTokens.radiusPill,
                            modifier = Modifier.testTag("start_challenge_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Challenge", fontWeight = FontWeight.Bold)
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
                                color = GlassTokens.AccentStart
                            )
                            Button(
                                onClick = { viewModel.startNewEarChallenge() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                shape = GlassTokens.radiusPill,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Sound", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // PRD §22.7 & §22.8: Sonic Glass Visual Hint Layer toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassTokens.radiusMd)
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sonic Glass Visual Hint",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassTokens.TextPrimary
                                )
                                Text(
                                    text = "Subtle reactive ambient glow on target band (PRD §22.7)",
                                    fontSize = 10.sp,
                                    color = GlassTokens.TextSecondary
                                )
                            }
                            Switch(
                                checked = uiState.showSonicHintInEarTrainer,
                                onCheckedChange = { viewModel.toggleSonicHintInEarTrainer() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GlassTokens.AccentStart,
                                    uncheckedThumbColor = GlassTokens.TextMuted,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Which plain-language band was boosted or cut?",
                            fontSize = 14.sp,
                            color = GlassTokens.TextPrimary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Band Guess Buttons
                        PlainBand.entries.forEach { band ->
                            val isHinted = uiState.showSonicHintInEarTrainer && challenge.targetBand == band
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(GlassTokens.radiusMd)
                                    .background(
                                        if (isHinted) GlassTokens.AccentStart.copy(alpha = 0.22f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isHinted) GlassTokens.AccentStart.copy(alpha = 0.7f)
                                        else Color.White.copy(alpha = 0.15f),
                                        GlassTokens.radiusMd
                                    )
                                    .clickable { viewModel.submitEarGuess(band) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("guess_button_${band.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = band.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isHinted) GlassTokens.AccentStart else GlassTokens.TextPrimary
                                            )
                                            if (isHinted) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "●",
                                                    fontSize = 10.sp,
                                                    color = GlassTokens.AccentStart
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
                                        color = GlassTokens.TextMuted
                                    )
                                }
                            }
                        }

                        // Feedback on last answer
                        AnimatedVisibility(visible = uiState.lastAnswerCorrect != null) {
                            val correct = uiState.lastAnswerCorrect == true
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                                    .clip(GlassTokens.radiusMd)
                                    .background(
                                        if (correct) GlassTokens.AccentSafe.copy(alpha = 0.2f)
                                        else GlassTokens.AccentWarning.copy(alpha = 0.2f)
                                    )
                                    .border(
                                        1.dp,
                                        if (correct) GlassTokens.AccentSafe else GlassTokens.AccentWarning,
                                        GlassTokens.radiusMd
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (correct) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (correct) GlassTokens.AccentSafe else GlassTokens.AccentWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (correct) "Correct! +100 Points" else "Incorrect — listen to the A/B difference!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (correct) GlassTokens.AccentSafe else GlassTokens.AccentWarning
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
private fun StatBox(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = GlassTokens.TextSecondary
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = GlassTokens.AccentStart
        )
    }
}
