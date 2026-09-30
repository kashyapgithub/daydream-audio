package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AudioProcessingService
import com.example.audio.SystemAudioEffectManager
import com.example.ui.components.NowPlayingGlassBar
import com.example.ui.components.NowPlayingModalSheet
import com.example.ui.components.SavePresetDialog
import com.example.ui.components.SonicGlassBackground
import com.example.ui.screens.AdvancedModeScreen
import com.example.ui.screens.GoldenEarScreen
import com.example.ui.screens.OnboardingScreen
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

        // Start foreground audio processing service to keep effects alive across YouTube/system apps
        AudioProcessingService.start(this)

        // Hook global audio session 0 for system-wide baseline output
        SystemAudioEffectManager.instance.openSession(this, 0, "Global System Output")

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
                    spectrum = uiState.spectrum,
                    reduceGlass = uiState.reduceGlass,
                    reduceMotion = uiState.reduceMotion
                ) {
                    var isNowPlayingSheetOpen by remember { mutableStateOf(false) }

                    val audioPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let { viewModel.importLocalMp3(it) }
                    }

                    if (!uiState.isOnboardingCompleted) {
                        OnboardingScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            onComplete = { viewModel.completeOnboarding() }
                        )
                    } else {
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
                                    localTrack = uiState.currentLocalTrack,
                                    isPlaying = uiState.isPlaying,
                                    isExternalActive = uiState.isExternalPlaybackActive,
                                    externalAppName = uiState.activeSystemSessions.firstOrNull(),
                                    onTogglePlay = { viewModel.togglePlayPause() },
                                    onNextTrack = { viewModel.nextTrack() },
                                    spectrum = uiState.spectrum,
                                    reduceGlass = uiState.reduceGlass,
                                    onExpandSheet = { isNowPlayingSheetOpen = true },
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    positionMs = uiState.playbackPositionMs,
                                    durationMs = uiState.playbackDurationMs,
                                    audioRms = uiState.audioRms
                                )

                                // Floating Glass Navigation Tab Bar (PRD 22.5 & 22.11)
                                FloatingGlassNavBar(
                                    currentTab = uiState.currentTab,
                                    onSelectTab = { viewModel.setTab(it) },
                                    reduceGlass = uiState.reduceGlass,
                                    reduceMotion = uiState.reduceMotion
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
                                    paddingValues = innerPadding,
                                    onImportMp3Click = { audioPickerLauncher.launch("audio/*") }
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

                            // Dynamic Island 2.0 (Apple Cupertino HIG)
                            AnimatedVisibility(
                                visible = uiState.notificationMessage != null || uiState.isPlaying || uiState.isExternalPlaybackActive,
                                enter = slideInVertically(
                                    initialOffsetY = { -it },
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 400f)
                                ) + fadeIn(),
                                exit = slideOutVertically(
                                    targetOffsetY = { -it },
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 400f)
                                ) + fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = innerPadding.calculateTopPadding() + 4.dp)
                                    .padding(horizontal = 20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .animateContentSize(
                                            animationSpec = spring(
                                                dampingRatio = 0.78f,
                                                stiffness = 450f
                                            )
                                        )
                                        .clip(GlassTokens.radiusPill)
                                        .background(Color(0xFF0D0D0E))
                                        .border(
                                            0.8.dp,
                                            Color.White.copy(alpha = 0.16f),
                                            GlassTokens.radiusPill
                                        )
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    if (uiState.notificationMessage != null) {
                                        val msg = uiState.notificationMessage!!
                                        LaunchedEffect(msg) {
                                            delay(3000)
                                            viewModel.clearNotification()
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(GlassTokens.IosBlue)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = msg,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = GlassTokens.TextPrimary
                                            )
                                        }
                                    } else if (uiState.isPlaying || uiState.isExternalPlaybackActive) {
                                        // Cupertino Dynamic Island Live Audio Pill
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.clickable {
                                                if (uiState.isPlaying) {
                                                    viewModel.togglePlayPause()
                                                } else {
                                                    isNowPlayingSheetOpen = true
                                                }
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (uiState.isExternalPlaybackActive && !uiState.isPlaying)
                                                            GlassTokens.IosGreen.copy(alpha = 0.25f)
                                                        else GlassTokens.IosBlue.copy(alpha = 0.25f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.GraphicEq,
                                                    contentDescription = null,
                                                    tint = if (uiState.isExternalPlaybackActive && !uiState.isPlaying) GlassTokens.IosGreen else GlassTokens.IosBlue,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }

                                            val islandText = when {
                                                uiState.currentLocalTrack != null && uiState.isPlaying -> uiState.currentLocalTrack!!.title
                                                uiState.currentTrack != null && uiState.isPlaying -> uiState.currentTrack!!.title
                                                uiState.isExternalPlaybackActive -> (uiState.activeSystemSessions.firstOrNull()?.let { "Hooked: $it" } ?: "Live External Audio")
                                                else -> "Active Audio"
                                            }

                                            Text(
                                                text = islandText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = GlassTokens.TextPrimary,
                                                maxLines = 1,
                                                modifier = Modifier.widthIn(max = 140.dp)
                                            )

                                            // 3-bar animated mini EQ waveform
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.Bottom,
                                                modifier = Modifier.height(12.dp)
                                            ) {
                                                val barHeights = listOf(
                                                    ((uiState.spectrum.getOrElse(1) { 0.4f }) * 12f).coerceIn(3f, 12f),
                                                    ((uiState.spectrum.getOrElse(3) { 0.7f }) * 12f).coerceIn(4f, 12f),
                                                    ((uiState.spectrum.getOrElse(5) { 0.5f }) * 12f).coerceIn(3f, 12f)
                                                )
                                                barHeights.forEach { h ->
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.5.dp)
                                                            .height(h.dp)
                                                            .clip(RoundedCornerShape(1.dp))
                                                            .background(GlassTokens.IosGreen)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Cupertino Expandable Now Playing Modal Sheet
                            AnimatedVisibility(
                                visible = isNowPlayingSheetOpen,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)
                                ) + fadeIn(),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)
                                ) + fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            ) {
                                NowPlayingModalSheet(
                                    track = uiState.currentTrack,
                                    localTrack = uiState.currentLocalTrack,
                                    isPlaying = uiState.isPlaying,
                                    isExternalActive = uiState.isExternalPlaybackActive,
                                    externalAppName = uiState.activeSystemSessions.firstOrNull(),
                                    currentPositionMs = uiState.playbackPositionMs,
                                    durationMs = uiState.playbackDurationMs,
                                    onSeekTo = { viewModel.seekTo(it) },
                                    isBypassed = uiState.isBypassed,
                                    onTogglePlay = { viewModel.togglePlayPause() },
                                    onNextTrack = { viewModel.nextTrack() },
                                    onPreviousTrack = { viewModel.previousTrack() },
                                    onToggleBypass = { viewModel.toggleBypassAB() },
                                    onDismiss = { isNowPlayingSheetOpen = false },
                                    audioRms = uiState.audioRms,
                                    comfortLimiterEnabled = uiState.comfortLimiterEnabled,
                                    onToggleComfortLimiter = { viewModel.toggleComfortLimiter() }
                                )
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

                    // Save Custom Sound Preset Modal Dialog
                    if (uiState.showSavePresetDialog) {
                        SavePresetDialog(
                            onSave = { name, colorHex -> viewModel.saveCustomPreset(name, colorHex) },
                            onDismiss = { viewModel.closeSavePresetDialog() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Apple iOS Floating UITabBar (OpenDesign Apple Design System & HIG)
 * 60dp frosted glass bar, sliding subtle pill indicator, SF Pro icons & typography.
 */
@Composable
private fun FloatingGlassNavBar(
    currentTab: AppNavTab,
    onSelectTab: (AppNavTab) -> Unit,
    reduceGlass: Boolean = false,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tabs = AppNavTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(GlassTokens.radiusXl)
            .background(
                if (reduceGlass) Color(0xFF1C1C1E)
                else Color(0xFF1C1C1E).copy(alpha = 0.94f)
            )
            .border(
                0.8.dp,
                Color.White.copy(alpha = 0.16f),
                GlassTokens.radiusXl
            )
            .padding(3.dp)
    ) {
        val tabWidth = maxWidth / tabs.size
        val animatedIndicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = if (reduceMotion) tween(durationMillis = 0)
                            else tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "nav_indicator_offset"
        )

        // Apple Sliding Pill Indicator
        Box(
            modifier = Modifier
                .offset(x = animatedIndicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .padding(horizontal = 3.dp, vertical = 3.dp)
                .clip(GlassTokens.radiusLg)
                .background(Color(0xFF2C2C2E))
                .border(
                    0.5.dp,
                    Color.White.copy(alpha = 0.12f),
                    GlassTokens.radiusLg
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
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
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(GlassTokens.radiusLg)
                        .clickable { onSelectTab(tab) }
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
                            tint = if (isSelected) GlassTokens.IosBlue else GlassTokens.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) GlassTokens.IosBlue else GlassTokens.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

