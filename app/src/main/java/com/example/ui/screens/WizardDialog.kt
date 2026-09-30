package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioComplaint
import com.example.ui.theme.GlassTokens

@Composable
fun WizardDiagnosisDialog(
    onSelectComplaint: (AudioComplaint) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedComplaint by remember { mutableStateOf<AudioComplaint?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Apple Modal Sheet Grabber Handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color(0xFF5A5A5E))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassTokens.IosBlue.copy(alpha = 0.18f)),
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
                            text = "Audio Diagnosis",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTokens.TextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(GlassTokens.IosGroupedSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = GlassTokens.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Pick the symptom you hear in plain words. Daydream will diagnose and calibrate the signal chain automatically.",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 380.dp)
                ) {
                    items(AudioComplaint.entries) { complaint ->
                        val isSelected = selectedComplaint == complaint
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
                                .background(
                                    if (isSelected) GlassTokens.IosBlue.copy(alpha = 0.16f)
                                    else GlassTokens.IosGroupedSecondary
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (isSelected) GlassTokens.IosBlue else GlassTokens.IosSeparator,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedComplaint = complaint }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("wizard_option_${complaint.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            if (isSelected) GlassTokens.IosBlue
                                            else GlassTokens.IosGroupedTertiary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else GlassTokens.TextPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = complaint.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) GlassTokens.IosBlue else GlassTokens.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = complaint.description,
                                        fontSize = 12.sp,
                                        color = GlassTokens.TextSecondary,
                                        lineHeight = 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(GlassTokens.IosBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedComplaint?.let { onSelectComplaint(it) }
                },
                enabled = selectedComplaint != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassTokens.IosBlue,
                    disabledContainerColor = GlassTokens.IosGroupedSecondary
                ),
                shape = GlassTokens.radiusPill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("wizard_apply_button")
            ) {
                Text(
                    text = "Apply Smart Fix",
                    color = if (selectedComplaint != null) Color.White else GlassTokens.TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    color = GlassTokens.IosBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = GlassTokens.IosGroupedPrimary,
        shape = RoundedCornerShape(26.dp)
    )
}

