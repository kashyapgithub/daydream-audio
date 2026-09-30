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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Waves
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioComplaint
import com.example.ui.components.ABCompareBar
import com.example.ui.theme.GlassTokens
import com.example.viewmodel.DaydreamUiState
import com.example.viewmodel.DaydreamViewModel

/**
 * Mandatory first-run onboarding flow according to PRD Section 7:
 * Apple Setup Assistant / Welcome Flow adhering strictly to Apple HIG & Cupertino design tokens.
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
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding_steps"
        ) { step ->
            when (step) {
                1 -> OnboardingWelcomeStep(
                    reduceGlass = uiState.reduceGlass,
                    onNext = { currentStep = 2 }
                )
                2 -> OnboardingDeviceStep(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNext = { currentStep = 3 }
                )
                3 -> OnboardingWizardStep(
                    reduceGlass = uiState.reduceGlass,
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
private fun StepIndicator(currentStep: Int, totalSteps: Int = 4) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 20.dp)
    ) {
        for (i in 1..totalSteps) {
            val isActive = i == currentStep
            Box(
                modifier = Modifier
                    .size(width = if (isActive) 18.dp else 6.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isActive) GlassTokens.IosBlue
                        else GlassTokens.IosGroupedTertiary
                    )
            )
        }
    }
}

@Composable
private fun OnboardingWelcomeStep(
    reduceGlass: Boolean,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (reduceGlass) GlassTokens.IosGroupedPrimary
                else GlassTokens.IosGroupedPrimary.copy(alpha = 0.92f)
            )
            .border(0.5.dp, GlassTokens.IosSeparator, RoundedCornerShape(26.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StepIndicator(currentStep = 1)

            // Apple Squircle Hero Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassTokens.IosBlue.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = GlassTokens.IosBlue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Daydream Audio",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = GlassTokens.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Sound the way you remember it.",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = GlassTokens.IosBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Apple Setup Assistant Feature Rows
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    iconTint = GlassTokens.IosBlue,
                    title = "Acoustic Diagnosis",
                    subtitle = "Fix muddy, thin, or hissy audio in plain English without confusing audio jargon."
                )
                FeatureRow(
                    icon = Icons.Default.Headphones,
                    iconTint = GlassTokens.IosGreen,
                    title = "Hardware Tuning",
                    subtitle = "Automatic stage width, Harman EQ, and punch tailored specifically to your gear."
                )
                FeatureRow(
                    icon = Icons.Default.History,
                    iconTint = GlassTokens.IosOrange,
                    title = "Vintage Time Machine",
                    subtitle = "Relive the warm analog textures of 70s vinyl, 80s tape, and 90s radio broadcasts."
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
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
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = GlassTokens.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = GlassTokens.TextSecondary,
                lineHeight = 16.sp
            )
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
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (uiState.reduceGlass) GlassTokens.IosGroupedPrimary
                else GlassTokens.IosGroupedPrimary.copy(alpha = 0.92f)
            )
            .border(0.5.dp, GlassTokens.IosSeparator, RoundedCornerShape(26.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StepIndicator(currentStep = 2)

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassTokens.IosGreen.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = GlassTokens.IosGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Output Device Detected",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = GlassTokens.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Apple Inset Grouped Device Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(GlassTokens.IosGroupedSecondary)
                    .border(0.5.dp, GlassTokens.IosSeparator, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = uiState.currentDevice.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTokens.IosGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Apple Inset Details Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassTokens.IosGroupedSecondary)
                    .border(0.5.dp, GlassTokens.IosSeparator, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Acoustic Target", fontSize = 13.sp, color = GlassTokens.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text("Harman Curve", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GlassTokens.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spatial Width", fontSize = 13.sp, color = GlassTokens.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text("Calibrated", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GlassTokens.IosBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Phase Coherence", fontSize = 13.sp, color = GlassTokens.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text("Active (0.2ms)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GlassTokens.IosGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "We automatically calibrated initial Space and Punch settings tailored for your gear. You can fine-tune or change profiles anytime.",
                fontSize = 13.sp,
                color = GlassTokens.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
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
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun OnboardingWizardStep(
    reduceGlass: Boolean,
    onSelectComplaint: (AudioComplaint) -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (reduceGlass) GlassTokens.IosGroupedPrimary
                else GlassTokens.IosGroupedPrimary.copy(alpha = 0.92f)
            )
            .border(0.5.dp, GlassTokens.IosSeparator, RoundedCornerShape(26.dp))
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StepIndicator(currentStep = 3)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassTokens.IosBlue.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GlassTokens.IosBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "What's wrong with your sound?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlassTokens.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Text(
                text = "Pick the symptom you hear most often. We will configure your audio chain immediately.",
                fontSize = 13.sp,
                color = GlassTokens.TextSecondary,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioComplaint.entries.take(4).forEach { complaint ->
                    val icon = when (complaint) {
                        AudioComplaint.THIN_TINNY -> Icons.Default.GraphicEq
                        AudioComplaint.MUDDY_BOXY -> Icons.Default.BlurOn
                        AudioComplaint.VOCALS_BURIED -> Icons.Default.RecordVoiceOver
                        AudioComplaint.TOO_HARSH -> Icons.Default.VolumeOff
                        AudioComplaint.FLAT_LIFELESS -> Icons.Default.Waves
                        AudioComplaint.HISSY_NOISY -> Icons.Default.SurroundSound
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassTokens.IosGroupedSecondary)
                            .border(0.5.dp, GlassTokens.IosSeparator, RoundedCornerShape(14.dp))
                            .clickable { onSelectComplaint(complaint) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("onboarding_complaint_${complaint.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassTokens.IosGroupedTertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = GlassTokens.IosBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = complaint.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GlassTokens.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = complaint.description,
                                    fontSize = 12.sp,
                                    color = GlassTokens.TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = GlassTokens.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSkip,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip for now",
                    color = GlassTokens.IosBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
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
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (uiState.reduceGlass) GlassTokens.IosGroupedPrimary
                else GlassTokens.IosGroupedPrimary.copy(alpha = 0.92f)
            )
            .border(0.5.dp, GlassTokens.IosSeparator, RoundedCornerShape(26.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StepIndicator(currentStep = 4)

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassTokens.IosGreen.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = GlassTokens.IosGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Acoustic Chain Calibrated!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = GlassTokens.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = uiState.lastWizardFixSummary ?: "Restoration, Equalizer, and Space are configured.",
                fontSize = 13.sp,
                color = GlassTokens.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            // Live A/B Toggle demonstration - AirPods Pro pill style
            ABCompareBar(
                isBypassed = uiState.isBypassed,
                onToggle = { viewModel.toggleBypassAB() },
                reduceGlass = uiState.reduceGlass
            )

            Spacer(modifier = Modifier.height(26.dp))

            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = GlassTokens.IosBlue),
                shape = GlassTokens.radiusPill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_finish_button")
            ) {
                Text(
                    text = "Open Daydream Audio",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
