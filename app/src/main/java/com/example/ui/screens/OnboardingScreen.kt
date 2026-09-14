package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioComplaint
import com.example.ui.components.ABCompareBar
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.floatingGlass
import com.example.ui.theme.raisedGlass
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

/**
 * Mandatory first-run onboarding flow according to PRD Section 7:
 * Step 1: Welcome message ("Sound the way you remember it")
 * Step 2: Output device detection & starting profile tuning
 * Step 3: Upfront "What's wrong with your sound?" diagnostic wizard
 * Step 4: Instant A/B compare trial (<50ms)
 * Step 5: Transition into the core app
 */
@Composable
fun OnboardingScreen(
    viewModel: DaydreamViewModel,
    uiState: DaydreamUiState,
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding_steps"
        ) { step ->
            when (step) {
                1 -> OnboardingWelcomeStep(
                    onNext = { currentStep = 2 }
                )
                2 -> OnboardingDeviceStep(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNext = { currentStep = 3 }
                )
                3 -> OnboardingWizardStep(
                    onSelectComplaint = { complaint ->
                        viewModel.applyWizardComplaint(complaint)
                        currentStep = 4
                    },
                    onSkip = { currentStep = 4 }
                )
                4 -> OnboardingCompareStep(
                    uiState = uiState,
                    viewModel = viewModel,
                    onFinish = onComplete
                )
            }
        }
    }
}

@Composable
private fun OnboardingWelcomeStep(onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .floatingGlass()
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(GlassTokens.radiusPill)
                    .background(GlassTokens.AccentStart.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = GlassTokens.AccentStart,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Daydream Audio",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Sound the way you remember it.",
                fontSize = 15.sp,
                color = GlassTokens.AccentStart,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Fix thin, hissy, muddy, or flat recordings in the language of what you actually hear — no audio jargon required.",
                fontSize = 14.sp,
                color = GlassTokens.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                shape = GlassTokens.radiusPill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_welcome_next")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OnboardingDeviceStep(
    uiState: DaydreamUiState,
    viewModel: DaydreamViewModel,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .floatingGlass()
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(GlassTokens.radiusPill)
                    .background(GlassTokens.AccentSafe.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = GlassTokens.AccentSafe,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Output Device Detected",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = uiState.currentDevice.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.AccentStart,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "We automatically calibrated initial Space and Punch settings tailored for your gear. You can fine-tune or change profiles anytime.",
                fontSize = 13.sp,
                color = GlassTokens.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                shape = GlassTokens.radiusPill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_device_next")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OnboardingWizardStep(
    onSelectComplaint: (AudioComplaint) -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .floatingGlass()
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = GlassTokens.AccentStart,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "What's wrong with your music?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassTokens.TextPrimary
                )
            }

            Text(
                text = "Pick the symptom you hear most often. We will configure your audio chain immediately.",
                fontSize = 12.sp,
                color = GlassTokens.TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            AudioComplaint.entries.take(4).forEach { complaint ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(GlassTokens.radiusMd)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), GlassTokens.radiusMd)
                        .clickable { onSelectComplaint(complaint) }
                        .padding(12.dp)
                        .testTag("onboarding_complaint_${complaint.name.lowercase()}")
                ) {
                    Column {
                        Text(
                            text = complaint.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.AccentStart
                        )
                        Text(
                            text = complaint.description,
                            fontSize = 11.sp,
                            color = GlassTokens.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSkip,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Skip for now", color = GlassTokens.TextMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun OnboardingCompareStep(
    uiState: DaydreamUiState,
    viewModel: DaydreamViewModel,
    onFinish: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .floatingGlass()
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = GlassTokens.AccentSafe,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Acoustic Chain Calibrated!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = uiState.lastWizardFixSummary ?: "Restoration, Equalizer, and Space are configured.",
                fontSize = 13.sp,
                color = GlassTokens.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // Live A/B Toggle demonstration
            ABCompareBar(
                isBypassed = uiState.isBypassed,
                onToggle = { viewModel.toggleBypassAB() },
                reduceGlass = uiState.reduceGlass
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.AccentStart),
                shape = GlassTokens.radiusPill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_finish_button")
            ) {
                Text(
                    text = "Open Daydream Audio",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
