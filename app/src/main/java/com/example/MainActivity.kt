package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.NowPlayingGlassBar
import com.example.ui.components.SonicGlassBackground
import com.example.ui.screens.AdvancedModeScreen
import com.example.ui.screens.GoldenEarScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimpleModeScreen
import com.example.ui.screens.TimeMachineScreen
import com.example.ui.screens.WizardDiagnosisDialog
import com.example.ui.theme.DaydreamTheme
import com.example.ui.theme.GlassStyleConfig
import com.example.ui.theme.GlassTokens
import com.example.ui.theme.floatingGlass
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.DaydreamViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: DaydreamViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            DaydreamTheme(
                config = GlassStyleConfig(
                    reduceGlass = uiState.reduceGlass,
                    reduceMotion = uiState.reduceMotion,
                    showTechnicalValues = uiState.showTechnicalValues
                )
            ) {
                SonicGlassBackground(
                    audioRms = uiState.audioRms,
                    reduceGlass = uiState.reduceGlass,
                    reduceMotion = uiState.reduceMotion
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.statusBars,
                        bottomBar = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                // Now Playing floating glass bar
                                NowPlayingGlassBar(
                                    track = uiState.currentTrack,
                                    isPlaying = uiState.isPlaying,
                                    onTogglePlay = { viewModel.togglePlayPause() },
                                    onNextTrack = {
                                        val nextIdx = ((uiState.currentTrack?.let {
                                            viewModel.timeMachinePresets.indices.firstOrNull() ?: 0
                                        } ?: 0) + 1) % 4
                                        viewModel.selectTrack(nextIdx)
                                    },
                                    spectrum = uiState.spectrum,
                                    reduceGlass = uiState.reduceGlass,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Floating Glass Navigation Tab Bar (PRD 22.11)
                                FloatingGlassNavBar(
                                    currentTab = uiState.currentTab,
                                    onSelectTab = { viewModel.setTab(it) },
                                    reduceGlass = uiState.reduceGlass
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Primary Screen Content
                            when (uiState.currentTab) {
                                AppNavTab.RESTORE -> SimpleModeScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    paddingValues = innerPadding
                                )
                                AppNavTab.ADVANCED -> AdvancedModeScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    paddingValues = innerPadding
                                )
                                AppNavTab.TIME_MACHINE -> TimeMachineScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    paddingValues = innerPadding
                                )
                                AppNavTab.EAR_TRAINER -> GoldenEarScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    paddingValues = innerPadding
                                )
                                AppNavTab.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    paddingValues = innerPadding
                                )
                            }

                            // Notification Banner Toast
                            AnimatedVisibility(
                                visible = uiState.notificationMessage != null,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = innerPadding.calculateTopPadding() + 8.dp)
                                    .padding(horizontal = 24.dp)
                            ) {
                                uiState.notificationMessage?.let { msg ->
                                    LaunchedEffect(msg) {
                                        delay(3000)
                                        viewModel.clearNotification()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(GlassTokens.radiusPill)
                                            .background(Color(0xFF1E1A2C).copy(alpha = 0.95f))
                                            .border(1.dp, GlassTokens.AccentStart.copy(alpha = 0.6f), GlassTokens.radiusPill)
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = msg,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GlassTokens.AccentStart
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Diagnostic Wizard Modal Dialog
                    if (uiState.showWizardDialog) {
                        WizardDiagnosisDialog(
                            onSelectComplaint = { viewModel.applyWizardComplaint(it) },
                            onDismiss = { viewModel.closeWizardDialog() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Floating Glass Navigation Bar (PRD 22.11)
 */
@Composable
private fun FloatingGlassNavBar(
    currentTab: AppNavTab,
    onSelectTab: (AppNavTab) -> Unit,
    reduceGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .floatingGlass(reduceGlass)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppNavTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val icon: ImageVector = when (tab) {
                    AppNavTab.RESTORE -> Icons.Default.GraphicEq
                    AppNavTab.ADVANCED -> Icons.Default.Tune
                    AppNavTab.TIME_MACHINE -> Icons.Default.History
                    AppNavTab.EAR_TRAINER -> Icons.Default.Hearing
                    AppNavTab.SETTINGS -> Icons.Default.Settings
                }

                Box(
                    modifier = Modifier
                        .clip(GlassTokens.radiusPill)
                        .background(
                            if (isSelected) GlassTokens.AccentStart.copy(alpha = 0.22f)
                            else Color.Transparent
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) GlassTokens.AccentStart else GlassTokens.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) GlassTokens.AccentStart else GlassTokens.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
