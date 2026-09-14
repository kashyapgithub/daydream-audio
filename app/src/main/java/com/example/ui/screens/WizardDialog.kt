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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GlassTokens.AccentStart,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "What's wrong with your sound?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = GlassTokens.TextSecondary
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Pick the symptom you hear in plain words. Daydream will diagnose and calibrate the signal chain automatically.",
                    fontSize = 13.sp,
                    color = GlassTokens.TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(320.dp)
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
                                .clip(GlassTokens.radiusMd)
                                .background(
                                    if (isSelected) GlassTokens.AccentStart.copy(alpha = 0.20f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    1.5.dp,
                                    if (isSelected) GlassTokens.AccentStart
                                    else Color.White.copy(alpha = 0.12f),
                                    GlassTokens.radiusMd
                                )
                                .clickable { selectedComplaint = complaint }
                                .padding(12.dp)
                                .testTag("wizard_option_${complaint.name.lowercase()}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(GlassTokens.radiusSm)
                                        .background(
                                            if (isSelected) GlassTokens.AccentStart
                                            else Color.White.copy(alpha = 0.1f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else GlassTokens.AccentStart,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = complaint.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) GlassTokens.AccentStart else GlassTokens.TextPrimary
                                    )
                                    Text(
                                        text = complaint.description,
                                        fontSize = 12.sp,
                                        color = GlassTokens.TextSecondary
                                    )
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
                    containerColor = GlassTokens.AccentStart,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = GlassTokens.radiusPill,
                modifier = Modifier.testTag("wizard_apply_button")
            ) {
                Text(
                    text = "Apply Smart Fix",
                    color = if (selectedComplaint != null) Color.White else GlassTokens.TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Cancel", color = GlassTokens.TextSecondary)
            }
        },
        containerColor = Color(0xFF1B1729),
        shape = GlassTokens.radiusLg
    )
}
