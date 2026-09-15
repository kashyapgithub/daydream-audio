package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioDeviceManager
import com.example.audio.AudioEngine
import com.example.audio.SystemAudioEffectManager
import com.example.model.AudioComplaint
import com.example.model.DemoTrack
import com.example.model.OutputDevice
import com.example.model.ParametricBand
import com.example.model.PlainBand
import com.example.model.PresetExportBundle
import com.example.model.TimeMachinePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random

enum class AppNavTab(val title: String, val iconTag: String) {
    RESTORE("Restore", "GraphicEq"),
    ADVANCED("Advanced", "Tune"),
    TIME_MACHINE("Time Machine", "History"),
    EAR_TRAINER("Ear Trainer", "Hearing"),
    SETTINGS("Settings", "Settings")
}

data class GoldenEarChallenge(
    val targetBand: PlainBand,
    val isBoost: Boolean,
    val deltaDb: Float,
    val questionNumber: Int
)

data class DaydreamUiState(
    val currentTab: AppNavTab = AppNavTab.RESTORE,
    val isPlaying: Boolean = false,
    val isBypassed: Boolean = false, // A/B compare toggle
    val currentTrack: DemoTrack? = null,
    val currentDevice: OutputDevice = OutputDevice.BLUETOOTH_EARBUDS,
    val devicePrompt: OutputDevice? = null,
    val isOnboardingCompleted: Boolean = true,
    val activeSystemSessions: List<String> = emptyList(),
    // PRD 12.1 addendum: whether the best-effort session-0 global hook actually
    // attached on this device. False means this device/ROM doesn't honor it and
    // the app effects only reach cooperating apps (see PRD 12.1 for the list).
    val isGlobalHookActive: Boolean = false,

    // Spectrum & Audio Reactive (PRD 22.7 Sonic Glass)
    val spectrum: FloatArray = FloatArray(8) { 0.1f },
    val audioRms: Float = 0.05f,
    val isMonoDetected: Boolean = false,
    val showMonoWarning: Boolean = false,

    // Simple Mode EQ & Restoration Chains
    val eqGains: Map<PlainBand, Float> = mapOf(
        PlainBand.RUMBLE to 0f,
        PlainBand.WARMTH to 0f,
        PlainBand.BODY to 0f,
        PlainBand.CLARITY to 0f,
        PlainBand.AIR to 0f
    ),
    val spacePercent: Float = 35f,
    val punchPercent: Float = 25f,
    val clarityMacroPercent: Float = 15f,
    val loudnessPercent: Float = 0f,
    val hissRemovalPercent: Float = 0f,
    val deHumEnabled: Boolean = false,
    val humFrequency: Int = 60,
    val deCrackleEnabled: Boolean = false,

    // Advanced 10-Band Independent Parametric EQ (PRD 6.2 & 8.3)
    val isAdvancedModeActive: Boolean = false,
    val advancedBands: List<ParametricBand> = listOf(
        ParametricBand(31, "Sub-Rumble", 0f, 0.8f),
        ParametricBand(63, "Deep Bass", 0f, 0.8f),
        ParametricBand(125, "Warmth Punch", 0f, 0.8f),
        ParametricBand(250, "Low-Mid Fullness", 0f, 0.8f),
        ParametricBand(500, "Vocal Body", 0f, 0.8f),
        ParametricBand(1000, "Vocal Presence", 0f, 0.8f),
        ParametricBand(2000, "Instrument Edge", 0f, 0.8f),
        ParametricBand(4000, "Vocal Articulation", 0f, 0.8f),
        ParametricBand(8000, "Treble Detail", 0f, 0.8f),
        ParametricBand(16000, "Air & Sparkle", 0f, 0.8f)
    ),
    val compThresholdDb: Float = -18f,
    val compRatio: Float = 2.5f,
    val compAttackMs: Float = 20f,
    val compReleaseMs: Float = 150f,
    val limiterCeilingDb: Float = -0.5f,

    // Time Machine & Vintage-ify
    val activePresetId: String? = null,
    val isVintageMode: Boolean = false,
    val wowFlutterDepth: Float = 45f,
    val vintageNoiseLevel: Float = 50f,
    val vintageEraName: String = "70s Cassette",

    // Diagnostic Wizard
    val showWizardDialog: Boolean = false,
    val lastAppliedComplaint: AudioComplaint? = null,
    val lastWizardFixSummary: String? = null,

    // Golden Ear Trainer
    val activeChallenge: GoldenEarChallenge? = null,
    val earTrainerScore: Int = 0,
    val earTrainerStreak: Int = 0,
    val earTrainerTotalGuesses: Int = 0,
    val lastAnswerCorrect: Boolean? = null,
    val earTrainerLevel: String = "Beginner Ear",

    // Accessibility & Settings
    val showTechnicalValues: Boolean = false,
    val reduceGlass: Boolean = false,
    val reduceMotion: Boolean = false,
    val activeTooltipBand: PlainBand? = null,
    val notificationMessage: String? = null,

    // Legacy Mode & Fallback (PRD 9.0 & FR-11)
    val isLegacyMode: Boolean = false,

    // Static Spatial Room Simulation (PRD 6.7a) & Virtualizer HRTF Profile (PRD 6.3)
    val spatialRoomType: String = "Natural", // "Natural", "Intimate Studio", "Concert Hall", "Cathedral"
    val hrtfProfile: String = "Natural", // "Narrow", "Natural", "Wide"

    // Golden Ear Trainer Sonic Glass Hint Layer (PRD 22.7 & 22.8)
    val showSonicHintInEarTrainer: Boolean = false,

    // Dialog Visibility
    val showImportPresetDialog: Boolean = false,
    val showMemoryPostcardDialog: Boolean = false,

    // Time & Space FX: Reverb, Echo, Tempo & Lofi Mode
    val playbackSpeed: Float = 1.0f,
    val isLofiMode: Boolean = false,
    val reverbWetPercent: Float = 0f,
    val reverbRoomSizePercent: Float = 75f,
    val reverbDampingPercent: Float = 35f,
    val echoWetPercent: Float = 0f,
    val echoTimeMs: Int = 320,
    val echoFeedbackPercent: Float = 30f,
    val roomSize: com.example.audio.AudioEngine.RoomSize = com.example.audio.AudioEngine.RoomSize.LARGE_HALL,
    val wallMaterial: com.example.audio.AudioEngine.WallMaterial = com.example.audio.AudioEngine.WallMaterial.PLASTER
)

class DaydreamViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("daydream_audio_prefs", Context.MODE_PRIVATE)
    private val audioEngine = AudioEngine()
    private val systemEffects = SystemAudioEffectManager.instance
    private val deviceManager = AudioDeviceManager(application)

    // Pre-lofi saved state for smooth restoration when Lofi Mode is toggled off
    private var preLofiSpeed = 1.0f
    private var preLofiReverbWet = 0f
    private var preLofiReverbRoom = 75f
    private var preLofiReverbDamp = 35f
    private var preLofiEchoWet = 0f
    private var preLofiEchoTime = 320
    private var preLofiEchoFeedback = 30f
    private var preLofiWarmth = 0f
    private var preLofiAir = 0f
    private var preLofiVintageMode = false
    private var preLofiWowDepth = 0f
    private var preLofiVintageNoise = 0f

    private val _uiState = MutableStateFlow(
        DaydreamUiState(
            currentTrack = audioEngine.demoTracks.first(),
            isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        )
    )
    val uiState: StateFlow<DaydreamUiState> = _uiState.asStateFlow()

    // Pre-configured Time Machine presets (PRD 6.11)
    val timeMachinePresets = listOf(
        TimeMachinePreset(
            id = "60s_mono",
            eraTitle = "60s Mono Transfer",
            subtitle = "Vinyl & Tube Console",
            description = "Midrange warmth, gentle high-shelf rolloff, 60Hz mains notch, centered mono space.",
            rumbleDb = -2f, warmthDb = 3f, bodyDb = 4f, clarityDb = -1f, airDb = -4f,
            punchRatio = 35f, spacePercent = 15f, hissRemoval = 40f, deHumEnabled = true, deCrackleEnabled = true
        ),
        TimeMachinePreset(
            id = "70s_cassette",
            eraTitle = "70s Magnetic Tape",
            subtitle = "Type I Ferric Cassette",
            description = "Deep analog punch, tape hiss reduction, high-mid presence boost to counter tape saturation.",
            rumbleDb = 2f, warmthDb = 5f, bodyDb = 2f, clarityDb = 3f, airDb = -2f,
            punchRatio = 45f, spacePercent = 40f, hissRemoval = 65f, deHumEnabled = false, deCrackleEnabled = false
        ),
        TimeMachinePreset(
            id = "80s_cinema",
            eraTitle = "80s Cinema Print",
            subtitle = "Optical Soundtrack",
            description = "Vocal dialogue intelligibility, dialogue punch, optical hiss gate, dynamic leveling.",
            rumbleDb = -1f, warmthDb = 2f, bodyDb = 5f, clarityDb = 4f, airDb = 1f,
            punchRatio = 55f, spacePercent = 45f, hissRemoval = 50f, deHumEnabled = true, deCrackleEnabled = false
        ),
        TimeMachinePreset(
            id = "90s_radio",
            eraTitle = "90s FM Broadcast",
            subtitle = "Heavy Airplay Compression",
            description = "Loud, punchy FM curve: boosted bass punch, crispy highs, and high dynamic range compression.",
            rumbleDb = 4f, warmthDb = 3f, bodyDb = -1f, clarityDb = 3f, airDb = 4f,
            punchRatio = 60f, spacePercent = 55f, hissRemoval = 20f, deHumEnabled = false, deCrackleEnabled = false
        ),
        TimeMachinePreset(
            id = "early_mp3",
            eraTitle = "Early Digital (128kbps)",
            subtitle = "De-Harsh & Swirl Removal",
            description = "Smooths metallic compression artifacts, de-harshens 3-5kHz sibilance, and restores low-end body.",
            rumbleDb = 2f, warmthDb = 4f, bodyDb = 2f, clarityDb = -3f, airDb = -3f,
            punchRatio = 20f, spacePercent = 30f, hissRemoval = 0f, deHumEnabled = false, deCrackleEnabled = false
        ),
        TimeMachinePreset(
            id = "audiophile_hifi",
            eraTitle = "Studio Hi-Fi Master",
            subtitle = "Linear & Transparent",
            description = "Subtle acoustic correction, wide natural stereo virtualizer, and transparent dynamic balance.",
            rumbleDb = 1f, warmthDb = 1f, bodyDb = 0f, clarityDb = 2f, airDb = 3f,
            punchRatio = 15f, spacePercent = 60f, hissRemoval = 15f, deHumEnabled = false, deCrackleEnabled = false
        )
    )

    init {
        // Connect internal audio engine callbacks
        audioEngine.onSpectrumUpdated = { bands, rms ->
            _uiState.update { state ->
                val isMono = audioEngine.correlationMetric > 0.90f
                state.copy(
                    spectrum = bands.clone(),
                    audioRms = rms,
                    isMonoDetected = isMono,
                    showMonoWarning = isMono && state.spacePercent > 35f
                )
            }
        }

        // Listen for hardware output device changes (PRD FR-10)
        viewModelScope.launch {
            deviceManager.currentDevice.collect { device ->
                _uiState.update { it.copy(currentDevice = device) }
            }
        }
        viewModelScope.launch {
            deviceManager.deviceChangePrompt.collect { promptDevice ->
                _uiState.update { it.copy(devicePrompt = promptDevice) }
            }
        }

        // Listen for system audio sessions (Spotify, YT Music, etc.)
        viewModelScope.launch {
            systemEffects.activeSessionsSummary.collect { sessions ->
                _uiState.update { it.copy(activeSystemSessions = sessions) }
            }
        }

        // PRD 12.1 addendum: track whether the best-effort session-0 global hook
        // actually attached on this device, so the UI can be honest about it.
        viewModelScope.launch {
            systemEffects.isGlobalHookActive.collect { active ->
                _uiState.update { it.copy(isGlobalHookActive = active) }
            }
        }

        syncAllEngineParameters()
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _uiState.update { it.copy(isOnboardingCompleted = true) }
    }

    fun restartOnboarding() {
        _uiState.update { it.copy(isOnboardingCompleted = false) }
    }

    fun setTab(tab: AppNavTab) {
        val isAdv = tab == AppNavTab.ADVANCED
        audioEngine.isAdvancedParametricMode = isAdv
        systemEffects.isParametricModeActive = isAdv
        _uiState.update { it.copy(currentTab = tab, isAdvancedModeActive = isAdv) }
    }

    fun togglePlayPause() {
        val playing = audioEngine.togglePlayPause(viewModelScope)
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun selectTrack(index: Int) {
        if (index in audioEngine.demoTracks.indices) {
            audioEngine.currentTrackIndex = index
            _uiState.update { it.copy(currentTrack = audioEngine.demoTracks[index]) }
        }
    }

    fun toggleBypassAB() {
        val newBypass = !audioEngine.isBypassed.get()
        audioEngine.isBypassed.set(newBypass)
        systemEffects.setBypassed(newBypass)
        _uiState.update {
            it.copy(
                isBypassed = newBypass,
                notificationMessage = if (newBypass) "A/B: Original Raw Sound (Bypassed)" else "A/B: Restored Daydream Audio"
            )
        }
    }

    fun setEqGain(band: PlainBand, gainDb: Float) {
        val clamped = gainDb.coerceIn(-12f, 12f)
        val updated = _uiState.value.eqGains.toMutableMap()
        updated[band] = clamped

        systemEffects.isParametricModeActive = false
        audioEngine.isAdvancedParametricMode = false
        audioEngine.eqGains[band] = clamped
        audioEngine.updateDspCoefficients()
        systemEffects.updatePlainEqGains(updated)

        _uiState.update { it.copy(eqGains = updated, activePresetId = null) }
    }

    fun setParametricGain(hz: Int, gainDb: Float) {
        val clamped = gainDb.coerceIn(-12f, 12f)
        val updatedBands = _uiState.value.advancedBands.map {
            if (it.hz == hz) it.copy(gainDb = clamped) else it
        }

        systemEffects.isParametricModeActive = true
        audioEngine.isAdvancedParametricMode = true
        audioEngine.parametricGains[hz] = clamped
        audioEngine.updateDspCoefficients()
        systemEffects.updateParametricGains(mapOf(hz to clamped))

        _uiState.update { it.copy(advancedBands = updatedBands, activePresetId = null) }
    }

    fun setParametricQ(hz: Int, q: Float) {
        val clampedQ = q.coerceIn(0.2f, 10.0f)
        val updatedBands = _uiState.value.advancedBands.map {
            if (it.hz == hz) it.copy(q = clampedQ) else it
        }
        audioEngine.parametricQ[hz] = clampedQ
        audioEngine.updateDspCoefficients()
        _uiState.update { it.copy(advancedBands = updatedBands) }
    }

    fun setHrtfProfile(profile: String) {
        audioEngine.hrtfProfile = profile
        systemEffects.updateHrtfProfile(profile)
        _uiState.update { it.copy(hrtfProfile = profile) }
    }

    fun setSpacePercent(value: Float) {
        val isMono = _uiState.value.isMonoDetected
        // PRD FR-4: Enforce space cap on mono source
        val effectiveValue = if (isMono) value.coerceIn(0f, 35f) else value.coerceIn(0f, 100f)

        audioEngine.spaceAmount = effectiveValue
        systemEffects.updateSpace(effectiveValue, isMono)

        _uiState.update {
            it.copy(
                spacePercent = effectiveValue,
                showMonoWarning = isMono && value > 35f,
                activePresetId = null
            )
        }
    }

    fun setPunchPercent(value: Float) {
        val clamped = value.coerceIn(0f, 100f)
        audioEngine.punchAmount = clamped
        systemEffects.updatePunch(clamped)
        _uiState.update { it.copy(punchPercent = clamped, activePresetId = null) }
    }

    fun setCompThresholdDb(thresholdDb: Float) {
        val clamped = thresholdDb.coerceIn(-40f, 0f)
        audioEngine.compThresholdDb = clamped
        systemEffects.updateDynamicsCompressor(
            thresholdDb = clamped,
            ratio = _uiState.value.compRatio,
            attackMs = _uiState.value.compAttackMs,
            releaseMs = _uiState.value.compReleaseMs
        )
        _uiState.update { it.copy(compThresholdDb = clamped) }
    }

    fun setCompRatio(ratio: Float) {
        val clamped = ratio.coerceIn(1f, 10f)
        audioEngine.compRatio = clamped
        systemEffects.updateDynamicsCompressor(
            thresholdDb = _uiState.value.compThresholdDb,
            ratio = clamped,
            attackMs = _uiState.value.compAttackMs,
            releaseMs = _uiState.value.compReleaseMs
        )
        _uiState.update { it.copy(compRatio = clamped) }
    }

    fun setCompAttackMs(attackMs: Float) {
        val clamped = attackMs.coerceIn(1f, 100f)
        audioEngine.compAttackMs = clamped
        systemEffects.updateDynamicsCompressor(
            thresholdDb = _uiState.value.compThresholdDb,
            ratio = _uiState.value.compRatio,
            attackMs = clamped,
            releaseMs = _uiState.value.compReleaseMs
        )
        _uiState.update { it.copy(compAttackMs = clamped) }
    }

    fun setCompReleaseMs(releaseMs: Float) {
        val clamped = releaseMs.coerceIn(10f, 500f)
        audioEngine.compReleaseMs = clamped
        systemEffects.updateDynamicsCompressor(
            thresholdDb = _uiState.value.compThresholdDb,
            ratio = _uiState.value.compRatio,
            attackMs = _uiState.value.compAttackMs,
            releaseMs = clamped
        )
        _uiState.update { it.copy(compReleaseMs = clamped) }
    }

    fun setClarityMacroPercent(value: Float) {
        val clamped = value.coerceIn(0f, 100f)
        audioEngine.clarityMacroAmount = clamped
        systemEffects.updateClarity(clamped)
        _uiState.update { it.copy(clarityMacroPercent = clamped, activePresetId = null) }
    }

    fun setLoudnessPercent(value: Float) {
        val clamped = value.coerceIn(0f, 100f)
        audioEngine.loudnessBoost = clamped
        systemEffects.updateLoudness(clamped)
        _uiState.update { it.copy(loudnessPercent = clamped) }
    }

    fun setHissRemovalPercent(value: Float) {
        val clamped = value.coerceIn(0f, 100f)
        audioEngine.hissRemoval = clamped
        audioEngine.updateDspCoefficients()
        systemEffects.updateNoiseReduction(
            hissRemovalPercent = clamped,
            deHumEnabled = _uiState.value.deHumEnabled,
            humFrequency = _uiState.value.humFrequency
        )
        _uiState.update { it.copy(hissRemovalPercent = clamped, activePresetId = null) }
    }

    fun toggleDeHum() {
        val current = !_uiState.value.deHumEnabled
        audioEngine.deHumEnabled = current
        audioEngine.updateDspCoefficients()
        systemEffects.updateNoiseReduction(
            hissRemovalPercent = _uiState.value.hissRemovalPercent,
            deHumEnabled = current,
            humFrequency = _uiState.value.humFrequency
        )
        _uiState.update { it.copy(deHumEnabled = current, activePresetId = null) }
    }

    fun setHumFrequency(freq: Int) {
        audioEngine.humFrequency = freq
        audioEngine.updateDspCoefficients()
        systemEffects.updateNoiseReduction(
            hissRemovalPercent = _uiState.value.hissRemovalPercent,
            deHumEnabled = _uiState.value.deHumEnabled,
            humFrequency = freq
        )
        _uiState.update { it.copy(humFrequency = freq) }
    }

    fun toggleDeCrackle() {
        val current = !_uiState.value.deCrackleEnabled
        audioEngine.deCrackleEnabled = current
        _uiState.update { it.copy(deCrackleEnabled = current, activePresetId = null) }
    }

    // Time & Space FX: Tempo, Reverb, Echo, and Lofi Mode
    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 1.5f)
        audioEngine.setPlaybackSpeed(clamped)
        _uiState.update { it.copy(playbackSpeed = clamped) }
    }

    fun setReverbWet(percent: Float) {
        val clamped = percent.coerceIn(0f, 100f)
        audioEngine.reverbWet = clamped
        systemEffects.updateReverb(
            wetPercent = clamped,
            roomSizePercent = _uiState.value.reverbRoomSizePercent,
            dampingPercent = _uiState.value.reverbDampingPercent,
            echoTimeMs = _uiState.value.echoTimeMs,
            echoWetPercent = _uiState.value.echoWetPercent
        )
        _uiState.update { it.copy(reverbWetPercent = clamped) }
    }

    fun setReverbRoomSize(percent: Float) {
        val clamped = percent.coerceIn(0f, 100f)
        audioEngine.reverbRoomSize = clamped
        systemEffects.updateReverb(
            wetPercent = _uiState.value.reverbWetPercent,
            roomSizePercent = clamped,
            dampingPercent = _uiState.value.reverbDampingPercent,
            echoTimeMs = _uiState.value.echoTimeMs,
            echoWetPercent = _uiState.value.echoWetPercent
        )
        _uiState.update { it.copy(reverbRoomSizePercent = clamped) }
    }

    fun setReverbDamping(percent: Float) {
        val clamped = percent.coerceIn(0f, 100f)
        audioEngine.reverbDamping = clamped
        systemEffects.updateReverb(
            wetPercent = _uiState.value.reverbWetPercent,
            roomSizePercent = _uiState.value.reverbRoomSizePercent,
            dampingPercent = clamped,
            echoTimeMs = _uiState.value.echoTimeMs,
            echoWetPercent = _uiState.value.echoWetPercent
        )
        _uiState.update { it.copy(reverbDampingPercent = clamped) }
    }

    fun setEchoWet(percent: Float) {
        val clamped = percent.coerceIn(0f, 100f)
        audioEngine.echoWet = clamped
        systemEffects.updateReverb(
            wetPercent = _uiState.value.reverbWetPercent,
            roomSizePercent = _uiState.value.reverbRoomSizePercent,
            dampingPercent = _uiState.value.reverbDampingPercent,
            echoTimeMs = _uiState.value.echoTimeMs,
            echoWetPercent = clamped
        )
        _uiState.update { it.copy(echoWetPercent = clamped) }
    }

    fun setEchoTimeMs(timeMs: Int) {
        val clamped = timeMs.coerceIn(50, 2000)
        audioEngine.echoTimeMs = clamped
        systemEffects.updateReverb(
            wetPercent = _uiState.value.reverbWetPercent,
            roomSizePercent = _uiState.value.reverbRoomSizePercent,
            dampingPercent = _uiState.value.reverbDampingPercent,
            echoTimeMs = clamped,
            echoWetPercent = _uiState.value.echoWetPercent
        )
        _uiState.update { it.copy(echoTimeMs = clamped) }
    }

    fun setEchoFeedback(percent: Float) {
        val clamped = percent.coerceIn(0f, 92f)
        audioEngine.echoFeedback = clamped
        systemEffects.updateReverb(
            wetPercent = _uiState.value.reverbWetPercent,
            roomSizePercent = _uiState.value.reverbRoomSizePercent,
            dampingPercent = _uiState.value.reverbDampingPercent,
            echoTimeMs = _uiState.value.echoTimeMs,
            echoWetPercent = _uiState.value.echoWetPercent,
            echoFeedbackPercent = clamped
        )
        _uiState.update { it.copy(echoFeedbackPercent = clamped) }
    }

    // Room Size & Wall Material character presets (PRD 6.15)
    fun setRoomSize(size: com.example.audio.AudioEngine.RoomSize) {
        audioEngine.roomSize = size
        _uiState.update { it.copy(roomSize = size) }
    }

    fun setWallMaterial(material: com.example.audio.AudioEngine.WallMaterial) {
        audioEngine.wallMaterial = material
        _uiState.update { it.copy(wallMaterial = material) }
    }

    fun toggleLofiMode() {
        val current = _uiState.value
        val turnOn = !current.isLofiMode

        if (turnOn) {
            // Save current settings for restoration
            preLofiSpeed = current.playbackSpeed
            preLofiReverbWet = current.reverbWetPercent
            preLofiReverbRoom = current.reverbRoomSizePercent
            preLofiReverbDamp = current.reverbDampingPercent
            preLofiEchoWet = current.echoWetPercent
            preLofiEchoTime = current.echoTimeMs
            preLofiEchoFeedback = current.echoFeedbackPercent
            preLofiWarmth = current.eqGains[PlainBand.WARMTH] ?: 0f
            preLofiAir = current.eqGains[PlainBand.AIR] ?: 0f
            preLofiVintageMode = current.isVintageMode
            preLofiWowDepth = current.wowFlutterDepth
            preLofiVintageNoise = current.vintageNoiseLevel

            // Apply Lofi coordinates: slowed tempo, dreamy reverb, analog echo, warm tape EQ, tape flutter
            val lofiSpeed = 0.85f
            val lofiReverbWet = 35f
            val lofiReverbRoom = 75f
            val lofiReverbDamp = 40f
            val lofiEchoWet = 20f
            val lofiEchoTime = 320
            val lofiEchoFeedback = 35f
            val lofiWarmth = 4f
            val lofiAir = -5f

            audioEngine.setPlaybackSpeed(lofiSpeed)
            audioEngine.reverbWet = lofiReverbWet
            audioEngine.reverbRoomSize = lofiReverbRoom
            audioEngine.reverbDamping = lofiReverbDamp
            audioEngine.echoWet = lofiEchoWet
            audioEngine.echoTimeMs = lofiEchoTime
            audioEngine.echoFeedback = lofiEchoFeedback
            audioEngine.eqGains[PlainBand.WARMTH] = lofiWarmth
            audioEngine.eqGains[PlainBand.AIR] = lofiAir
            audioEngine.vintageMode = true
            audioEngine.wowFlutterDepth = 40f
            audioEngine.vintageNoiseLevel = 30f

            val updatedEq = current.eqGains.toMutableMap()
            updatedEq[PlainBand.WARMTH] = lofiWarmth
            updatedEq[PlainBand.AIR] = lofiAir
            audioEngine.updateDspCoefficients()
            systemEffects.updatePlainEqGains(updatedEq)
            systemEffects.updateReverb(
                wetPercent = lofiReverbWet,
                roomSizePercent = lofiReverbRoom,
                dampingPercent = lofiReverbDamp,
                echoTimeMs = lofiEchoTime,
                echoWetPercent = lofiEchoWet
            )

            _uiState.update {
                it.copy(
                    isLofiMode = true,
                    playbackSpeed = lofiSpeed,
                    reverbWetPercent = lofiReverbWet,
                    reverbRoomSizePercent = lofiReverbRoom,
                    reverbDampingPercent = lofiReverbDamp,
                    echoWetPercent = lofiEchoWet,
                    echoTimeMs = lofiEchoTime,
                    echoFeedbackPercent = lofiEchoFeedback,
                    eqGains = updatedEq,
                    isVintageMode = true,
                    wowFlutterDepth = 40f,
                    vintageNoiseLevel = 30f,
                    notificationMessage = "Lofi Mode Active: 0.85x Slowed • Dreamy Reverb • Warm Rolloff"
                )
            }
        } else {
            // Restore previous settings
            audioEngine.setPlaybackSpeed(preLofiSpeed)
            audioEngine.reverbWet = preLofiReverbWet
            audioEngine.reverbRoomSize = preLofiReverbRoom
            audioEngine.reverbDamping = preLofiReverbDamp
            audioEngine.echoWet = preLofiEchoWet
            audioEngine.echoTimeMs = preLofiEchoTime
            audioEngine.echoFeedback = preLofiEchoFeedback
            audioEngine.eqGains[PlainBand.WARMTH] = preLofiWarmth
            audioEngine.eqGains[PlainBand.AIR] = preLofiAir
            audioEngine.vintageMode = preLofiVintageMode
            audioEngine.wowFlutterDepth = preLofiWowDepth
            audioEngine.vintageNoiseLevel = preLofiVintageNoise

            val restoredEq = current.eqGains.toMutableMap()
            restoredEq[PlainBand.WARMTH] = preLofiWarmth
            restoredEq[PlainBand.AIR] = preLofiAir
            audioEngine.updateDspCoefficients()
            systemEffects.updatePlainEqGains(restoredEq)
            systemEffects.updateReverb(
                wetPercent = preLofiReverbWet,
                roomSizePercent = preLofiReverbRoom,
                dampingPercent = preLofiReverbDamp,
                echoTimeMs = preLofiEchoTime,
                echoWetPercent = preLofiEchoWet
            )

            _uiState.update {
                it.copy(
                    isLofiMode = false,
                    playbackSpeed = preLofiSpeed,
                    reverbWetPercent = preLofiReverbWet,
                    reverbRoomSizePercent = preLofiReverbRoom,
                    reverbDampingPercent = preLofiReverbDamp,
                    echoWetPercent = preLofiEchoWet,
                    echoTimeMs = preLofiEchoTime,
                    echoFeedbackPercent = preLofiEchoFeedback,
                    eqGains = restoredEq,
                    isVintageMode = preLofiVintageMode,
                    wowFlutterDepth = preLofiWowDepth,
                    vintageNoiseLevel = preLofiVintageNoise,
                    notificationMessage = "Lofi Mode disabled — restored previous sound profile"
                )
            }
        }
    }

    // Diagnostics Wizard (PRD 6.1)
    fun openWizardDialog() {
        _uiState.update { it.copy(showWizardDialog = true) }
    }

    fun closeWizardDialog() {
        _uiState.update { it.copy(showWizardDialog = false) }
    }

    fun applyWizardComplaint(complaint: AudioComplaint) {
        val updatedEq = _uiState.value.eqGains.toMutableMap()
        var space = _uiState.value.spacePercent
        var punch = _uiState.value.punchPercent
        var clarityMacro = _uiState.value.clarityMacroPercent
        var hiss = _uiState.value.hissRemovalPercent
        var deHum = _uiState.value.deHumEnabled
        var deCrackle = _uiState.value.deCrackleEnabled
        val summary: String

        when (complaint) {
            AudioComplaint.THIN_TINNY -> {
                updatedEq[PlainBand.RUMBLE] = 3f
                updatedEq[PlainBand.WARMTH] = 6f
                updatedEq[PlainBand.BODY] = 4f
                updatedEq[PlainBand.AIR] = -2f
                punch = 35f
                summary = "Boosted Warmth (+6dB) & Body (+4dB), added Punch to solidify thin audio."
            }
            AudioComplaint.MUDDY_BOXY -> {
                updatedEq[PlainBand.WARMTH] = -2f
                updatedEq[PlainBand.BODY] = -5f
                updatedEq[PlainBand.CLARITY] = 5f
                updatedEq[PlainBand.AIR] = 2f
                clarityMacro = 40f
                summary = "Cut muddy Body (-5dB) and boosted Clarity (+5dB) to open up vocals."
            }
            AudioComplaint.VOCALS_BURIED -> {
                updatedEq[PlainBand.WARMTH] = -1f
                updatedEq[PlainBand.BODY] = 6f
                updatedEq[PlainBand.CLARITY] = 5f
                punch = 40f
                clarityMacro = 35f
                summary = "Brought vocals to front with Body (+6dB), Clarity (+5dB), and vocal compressor punch."
            }
            AudioComplaint.TOO_HARSH -> {
                updatedEq[PlainBand.CLARITY] = -5f
                updatedEq[PlainBand.AIR] = -4f
                updatedEq[PlainBand.WARMTH] = 2f
                clarityMacro = 0f
                summary = "Attenuated harsh frequencies (-5dB Clarity & -4dB Air) for fatigue-free listening."
            }
            AudioComplaint.FLAT_LIFELESS -> {
                updatedEq[PlainBand.RUMBLE] = 5f
                updatedEq[PlainBand.WARMTH] = 2f
                updatedEq[PlainBand.BODY] = 0f
                updatedEq[PlainBand.CLARITY] = 3f
                updatedEq[PlainBand.AIR] = 5f
                space = 60f
                punch = 45f
                summary = "Applied musical smiley curve, widened Space to 60%, and added Punch."
            }
            AudioComplaint.HISSY_NOISY -> {
                hiss = 75f
                deHum = true
                deCrackle = true
                updatedEq[PlainBand.AIR] = -2f
                summary = "Routed to Noise Reduction: Hiss Removal 75%, 60Hz De-Hum ON, De-Crackle ON."
            }
        }

        audioEngine.eqGains.putAll(updatedEq)
        systemEffects.updatePlainEqGains(updatedEq)

        audioEngine.spaceAmount = space
        systemEffects.updateSpace(space, _uiState.value.isMonoDetected)

        audioEngine.punchAmount = punch
        systemEffects.updatePunch(punch)

        audioEngine.clarityMacroAmount = clarityMacro
        systemEffects.updateClarity(clarityMacro)
        audioEngine.hissRemoval = hiss
        audioEngine.deHumEnabled = deHum
        audioEngine.deCrackleEnabled = deCrackle
        audioEngine.updateDspCoefficients()

        _uiState.update {
            it.copy(
                eqGains = updatedEq,
                spacePercent = space,
                punchPercent = punch,
                clarityMacroPercent = clarityMacro,
                hissRemovalPercent = hiss,
                deHumEnabled = deHum,
                deCrackleEnabled = deCrackle,
                showWizardDialog = false,
                lastAppliedComplaint = complaint,
                lastWizardFixSummary = summary,
                activePresetId = null,
                notificationMessage = "Fix applied! Tap A/B to compare with original."
            )
        }
    }

    // Time Machine Presets (PRD 6.11)
    fun applyTimeMachinePreset(preset: TimeMachinePreset) {
        val updatedEq = mapOf(
            PlainBand.RUMBLE to preset.rumbleDb,
            PlainBand.WARMTH to preset.warmthDb,
            PlainBand.BODY to preset.bodyDb,
            PlainBand.CLARITY to preset.clarityDb,
            PlainBand.AIR to preset.airDb
        )
        audioEngine.eqGains.putAll(updatedEq)
        systemEffects.updatePlainEqGains(updatedEq)

        audioEngine.spaceAmount = preset.spacePercent
        systemEffects.updateSpace(preset.spacePercent, _uiState.value.isMonoDetected)

        audioEngine.punchAmount = preset.punchRatio
        systemEffects.updatePunch(preset.punchRatio)

        audioEngine.hissRemoval = preset.hissRemoval
        audioEngine.deHumEnabled = preset.deHumEnabled
        audioEngine.deCrackleEnabled = preset.deCrackleEnabled
        audioEngine.vintageMode = false
        audioEngine.wowFlutterDepth = 0f
        audioEngine.updateDspCoefficients()

        _uiState.update {
            it.copy(
                activePresetId = preset.id,
                eqGains = updatedEq,
                spacePercent = preset.spacePercent,
                punchPercent = preset.punchRatio,
                hissRemovalPercent = preset.hissRemoval,
                deHumEnabled = preset.deHumEnabled,
                deCrackleEnabled = preset.deCrackleEnabled,
                isVintageMode = false,
                notificationMessage = "Loaded preset: ${preset.eraTitle}"
            )
        }
    }

    // Reverse Time Machine ("Vintage-ify", PRD 6.13)
    fun setVintageMode(enabled: Boolean) {
        audioEngine.vintageMode = enabled
        audioEngine.wowFlutterDepth = if (enabled) _uiState.value.wowFlutterDepth else 0f
        audioEngine.vintageNoiseLevel = if (enabled) _uiState.value.vintageNoiseLevel else 0f
        systemEffects.updateVintageMode(enabled)

        _uiState.update {
            it.copy(
                isVintageMode = enabled,
                notificationMessage = if (enabled) "Vintage-ify Active: Simulating analog tape warble & noise!" else "Vintage-ify Disabled"
            )
        }
    }

    fun setWowFlutterDepth(depth: Float) {
        val clamped = depth.coerceIn(0f, 100f)
        audioEngine.wowFlutterDepth = clamped
        _uiState.update { it.copy(wowFlutterDepth = clamped) }
    }

    fun setVintageNoiseLevel(level: Float) {
        val clamped = level.coerceIn(0f, 100f)
        audioEngine.vintageNoiseLevel = clamped
        _uiState.update { it.copy(vintageNoiseLevel = clamped) }
    }

    // Output Device Profile (PRD 6.4)
    fun setOutputDevice(device: OutputDevice) {
        audioEngine.spaceAmount = device.defaultSpace
        audioEngine.punchAmount = device.defaultPunch
        val updatedEq = _uiState.value.eqGains.toMutableMap()
        updatedEq[PlainBand.WARMTH] = device.defaultWarmth

        audioEngine.eqGains[PlainBand.WARMTH] = device.defaultWarmth
        audioEngine.updateDspCoefficients()
        systemEffects.updatePlainEqGains(updatedEq)
        systemEffects.updateSpace(device.defaultSpace, _uiState.value.isMonoDetected)
        systemEffects.updatePunch(device.defaultPunch)

        _uiState.update {
            it.copy(
                currentDevice = device,
                devicePrompt = null,
                spacePercent = device.defaultSpace,
                punchPercent = device.defaultPunch,
                eqGains = updatedEq,
                notificationMessage = "Profile auto-tuned for ${device.displayName}"
            )
        }
    }

    fun dismissDevicePrompt() {
        deviceManager.clearDevicePrompt()
        _uiState.update { it.copy(devicePrompt = null) }
    }

    // Preset JSON Serialization & Export / Import (PRD 6.2 & FR-12)
    fun exportCurrentPresetJson(): String {
        val s = _uiState.value
        val json = JSONObject().apply {
            put("version", 1)
            put("name", "Custom Preset")
            val eqObj = JSONObject()
            s.eqGains.forEach { (band, gain) -> eqObj.put(band.name, gain) }
            put("eqGains", eqObj)
            put("spacePercent", s.spacePercent)
            put("punchPercent", s.punchPercent)
            put("clarityMacroPercent", s.clarityMacroPercent)
            put("loudnessPercent", s.loudnessPercent)
            put("hissRemovalPercent", s.hissRemovalPercent)
            put("deHumEnabled", s.deHumEnabled)
            put("deCrackleEnabled", s.deCrackleEnabled)
            put("compThresholdDb", s.compThresholdDb)
            put("compRatio", s.compRatio)
            put("compAttackMs", s.compAttackMs)
            put("compReleaseMs", s.compReleaseMs)
            put("reverbWetPercent", s.reverbWetPercent)
            put("reverbRoomSizePercent", s.reverbRoomSizePercent)
            put("reverbDampingPercent", s.reverbDampingPercent)
            put("echoTimeMs", s.echoTimeMs)
            put("echoFeedbackPercent", s.echoFeedbackPercent)
            put("echoWetPercent", s.echoWetPercent)
            put("roomSize", s.roomSize.name)
            put("wallMaterial", s.wallMaterial.name)
            put("playbackSpeed", s.playbackSpeed)
            put("isLofiMode", s.isLofiMode)
        }
        return json.toString(2)
    }

    fun importPresetJson(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            val eqObj = json.optJSONObject("eqGains")
            val importedEq = _uiState.value.eqGains.toMutableMap()
            if (eqObj != null) {
                PlainBand.entries.forEach { band ->
                    if (eqObj.has(band.name)) {
                        importedEq[band] = eqObj.getDouble(band.name).toFloat()
                    }
                }
            }
            val space = json.optDouble("spacePercent", 35.0).toFloat()
            val punch = json.optDouble("punchPercent", 25.0).toFloat()
            val clarity = json.optDouble("clarityMacroPercent", 0.0).toFloat()
            val loudness = json.optDouble("loudnessPercent", 0.0).toFloat()
            val hiss = json.optDouble("hissRemovalPercent", 0.0).toFloat()
            val deHum = json.optBoolean("deHumEnabled", false)
            val deCrackle = json.optBoolean("deCrackleEnabled", false)
            val reverbWet = json.optDouble("reverbWetPercent", 0.0).toFloat()
            val reverbRoom = json.optDouble("reverbRoomSizePercent", 75.0).toFloat()
            val reverbDamp = json.optDouble("reverbDampingPercent", 35.0).toFloat()
            val echoTime = json.optInt("echoTimeMs", 320)
            val echoFeedback = json.optDouble("echoFeedbackPercent", 30.0).toFloat()
            val echoWet = json.optDouble("echoWetPercent", 0.0).toFloat()
            val roomSizeName = json.optString("roomSize", com.example.audio.AudioEngine.RoomSize.LARGE_HALL.name)
            val wallMaterialName = json.optString("wallMaterial", com.example.audio.AudioEngine.WallMaterial.PLASTER.name)
            val roomSizeValue = runCatching { com.example.audio.AudioEngine.RoomSize.valueOf(roomSizeName) }
                .getOrDefault(com.example.audio.AudioEngine.RoomSize.LARGE_HALL)
            val wallMaterialValue = runCatching { com.example.audio.AudioEngine.WallMaterial.valueOf(wallMaterialName) }
                .getOrDefault(com.example.audio.AudioEngine.WallMaterial.PLASTER)
            val compThreshold = json.optDouble("compThresholdDb", -18.0).toFloat()
            val compRatio = json.optDouble("compRatio", 2.5).toFloat()
            val compAttack = json.optDouble("compAttackMs", 15.0).toFloat()
            val compRelease = json.optDouble("compReleaseMs", 80.0).toFloat()
            val speed = json.optDouble("playbackSpeed", 1.0).toFloat()
            val lofi = json.optBoolean("isLofiMode", false)

            audioEngine.eqGains.putAll(importedEq)
            systemEffects.updatePlainEqGains(importedEq)
            audioEngine.spaceAmount = space
            systemEffects.updateSpace(space, _uiState.value.isMonoDetected)
            audioEngine.punchAmount = punch
            systemEffects.updatePunch(punch)
            audioEngine.clarityMacroAmount = clarity
            systemEffects.updateClarity(clarity)
            audioEngine.loudnessBoost = loudness
            systemEffects.updateLoudness(loudness)
            audioEngine.hissRemoval = hiss
            audioEngine.deHumEnabled = deHum
            audioEngine.deCrackleEnabled = deCrackle
            audioEngine.compThresholdDb = compThreshold
            audioEngine.compRatio = compRatio
            audioEngine.compAttackMs = compAttack
            audioEngine.compReleaseMs = compRelease
            systemEffects.updateDynamicsCompressor(
                thresholdDb = compThreshold,
                ratio = compRatio,
                attackMs = compAttack,
                releaseMs = compRelease
            )
            audioEngine.reverbWet = reverbWet
            audioEngine.reverbRoomSize = reverbRoom
            audioEngine.reverbDamping = reverbDamp
            audioEngine.echoTimeMs = echoTime
            audioEngine.echoFeedback = echoFeedback
            audioEngine.echoWet = echoWet
            audioEngine.roomSize = roomSizeValue
            audioEngine.wallMaterial = wallMaterialValue
            systemEffects.updateReverb(
                wetPercent = reverbWet,
                roomSizePercent = reverbRoom,
                dampingPercent = reverbDamp,
                echoTimeMs = echoTime,
                echoWetPercent = echoWet
            )
            audioEngine.setPlaybackSpeed(speed)
            audioEngine.updateDspCoefficients()

            _uiState.update {
                it.copy(
                    eqGains = importedEq,
                    spacePercent = space,
                    punchPercent = punch,
                    clarityMacroPercent = clarity,
                    loudnessPercent = loudness,
                    hissRemovalPercent = hiss,
                    deHumEnabled = deHum,
                    deCrackleEnabled = deCrackle,
                    compThresholdDb = compThreshold,
                    compRatio = compRatio,
                    compAttackMs = compAttack,
                    compReleaseMs = compRelease,
                    reverbWetPercent = reverbWet,
                    reverbRoomSizePercent = reverbRoom,
                    reverbDampingPercent = reverbDamp,
                    echoTimeMs = echoTime,
                    echoFeedbackPercent = echoFeedback,
                    echoWetPercent = echoWet,
                    roomSize = roomSizeValue,
                    wallMaterial = wallMaterialValue,
                    playbackSpeed = speed,
                    isLofiMode = lofi,
                    notificationMessage = "Preset imported successfully!"
                )
            }
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(notificationMessage = "Failed to import preset: Invalid JSON") }
            false
        }
    }

    // Golden Ear Trainer (PRD 6.12)
    fun startNewEarChallenge() {
        val bands = PlainBand.entries
        val chosen = bands.random()
        val isBoost = Random.nextBoolean()
        val delta = if (isBoost) 6f else -6f

        val map = PlainBand.entries.associateWith { if (it == chosen) delta else 0f }
        audioEngine.eqGains.clear()
        audioEngine.eqGains.putAll(map)
        audioEngine.updateDspCoefficients()
        systemEffects.updatePlainEqGains(map)

        val challenge = GoldenEarChallenge(
            targetBand = chosen,
            isBoost = isBoost,
            deltaDb = delta,
            questionNumber = _uiState.value.earTrainerTotalGuesses + 1
        )

        _uiState.update {
            it.copy(
                activeChallenge = challenge,
                lastAnswerCorrect = null
            )
        }
    }

    fun submitEarGuess(guessedBand: PlainBand) {
        val current = _uiState.value.activeChallenge ?: return
        val isCorrect = guessedBand == current.targetBand
        val newScore = if (isCorrect) _uiState.value.earTrainerScore + 100 else _uiState.value.earTrainerScore
        val newStreak = if (isCorrect) _uiState.value.earTrainerStreak + 1 else 0
        val total = _uiState.value.earTrainerTotalGuesses + 1

        val rank = when {
            newStreak >= 8 -> "Master Audiophile"
            newStreak >= 5 -> "Gold Sound Engineer"
            newStreak >= 3 -> "Silver Ear"
            else -> "Bronze Listener"
        }

        _uiState.update {
            it.copy(
                lastAnswerCorrect = isCorrect,
                earTrainerScore = newScore,
                earTrainerStreak = newStreak,
                earTrainerTotalGuesses = total,
                earTrainerLevel = rank,
                notificationMessage = if (isCorrect) "Spot on! That was ${current.targetBand.title} (${if (current.isBoost) "+6dB boost" else "-6dB cut"})" else "Not quite! That was ${current.targetBand.title}."
            )
        }
        syncAllEngineParameters()
    }

    // Settings & Display
    fun toggleShowTechnicalValues() {
        _uiState.update { it.copy(showTechnicalValues = !it.showTechnicalValues) }
    }

    fun toggleReduceGlass() {
        _uiState.update { it.copy(reduceGlass = !it.reduceGlass) }
    }

    fun toggleReduceMotion() {
        _uiState.update { it.copy(reduceMotion = !it.reduceMotion) }
    }

    fun showTooltip(band: PlainBand?) {
        _uiState.update { it.copy(activeTooltipBand = band) }
    }

    fun resetAllToFlat() {
        val flat = PlainBand.entries.associateWith { 0f }
        flat.forEach { (b, g) -> audioEngine.eqGains[b] = g }
        systemEffects.updatePlainEqGains(flat)

        audioEngine.spaceAmount = 30f
        systemEffects.updateSpace(30f, false)
        audioEngine.punchAmount = 0f
        systemEffects.updatePunch(0f)
        audioEngine.clarityMacroAmount = 0f
        systemEffects.updateClarity(0f)
        audioEngine.loudnessBoost = 0f
        systemEffects.updateLoudness(0f)
        audioEngine.hissRemoval = 0f
        audioEngine.deHumEnabled = false
        audioEngine.deCrackleEnabled = false
        audioEngine.vintageMode = false
        audioEngine.compThresholdDb = -18f
        audioEngine.compRatio = 2.5f
        audioEngine.compAttackMs = 15f
        audioEngine.compReleaseMs = 80f
        systemEffects.updateDynamicsCompressor(-18f, 2.5f, 15f, 80f)
        audioEngine.reverbWet = 0f
        audioEngine.echoWet = 0f
        audioEngine.roomSize = com.example.audio.AudioEngine.RoomSize.LARGE_HALL
        audioEngine.wallMaterial = com.example.audio.AudioEngine.WallMaterial.PLASTER
        audioEngine.setPlaybackSpeed(1.0f)
        systemEffects.updateReverb(0f, 75f, 35f, 320, 0f)

        _uiState.update {
            it.copy(
                eqGains = flat,
                spacePercent = 30f,
                punchPercent = 0f,
                clarityMacroPercent = 0f,
                loudnessPercent = 0f,
                hissRemovalPercent = 0f,
                deHumEnabled = false,
                deCrackleEnabled = false,
                isVintageMode = false,
                compThresholdDb = -18f,
                compRatio = 2.5f,
                compAttackMs = 15f,
                compReleaseMs = 80f,
                reverbWetPercent = 0f,
                echoWetPercent = 0f,
                roomSize = com.example.audio.AudioEngine.RoomSize.LARGE_HALL,
                wallMaterial = com.example.audio.AudioEngine.WallMaterial.PLASTER,
                playbackSpeed = 1.0f,
                isLofiMode = false,
                activePresetId = null,
                notificationMessage = "All sliders reset to neutral flat"
            )
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    fun toggleLegacyMode() {
        val newMode = !_uiState.value.isLegacyMode
        _uiState.update {
            it.copy(
                isLegacyMode = newMode,
                notificationMessage = if (newMode) "Legacy Mode: In-App Player active (OEM fallback)" else "Global System Audio Hooking active"
            )
        }
    }

    fun setSpatialRoomType(roomType: String) {
        audioEngine.spatialRoomType = roomType
        systemEffects.updateSpatialRoom(roomType)
        _uiState.update {
            it.copy(
                spatialRoomType = roomType,
                notificationMessage = "Spatial Room Simulation: $roomType"
            )
        }
    }

    fun toggleSonicHintInEarTrainer() {
        _uiState.update { it.copy(showSonicHintInEarTrainer = !it.showSonicHintInEarTrainer) }
    }

    fun openImportPresetDialog() {
        _uiState.update { it.copy(showImportPresetDialog = true) }
    }

    fun closeImportPresetDialog() {
        _uiState.update { it.copy(showImportPresetDialog = false) }
    }

    fun openMemoryPostcardDialog() {
        _uiState.update { it.copy(showMemoryPostcardDialog = true) }
    }

    fun closeMemoryPostcardDialog() {
        _uiState.update { it.copy(showMemoryPostcardDialog = false) }
    }

    private fun syncAllEngineParameters() {
        val s = _uiState.value
        s.eqGains.forEach { (band, gain) -> audioEngine.eqGains[band] = gain }
        systemEffects.updatePlainEqGains(s.eqGains)

        audioEngine.spaceAmount = s.spacePercent
        systemEffects.updateSpace(s.spacePercent, s.isMonoDetected)
        audioEngine.hrtfProfile = s.hrtfProfile
        systemEffects.updateHrtfProfile(s.hrtfProfile)

        audioEngine.punchAmount = s.punchPercent
        systemEffects.updatePunch(s.punchPercent)

        audioEngine.clarityMacroAmount = s.clarityMacroPercent
        systemEffects.updateClarity(s.clarityMacroPercent)
        audioEngine.loudnessBoost = s.loudnessPercent
        systemEffects.updateLoudness(s.loudnessPercent)

        audioEngine.hissRemoval = s.hissRemovalPercent
        audioEngine.deHumEnabled = s.deHumEnabled
        audioEngine.humFrequency = s.humFrequency
        audioEngine.deCrackleEnabled = s.deCrackleEnabled
        systemEffects.updateNoiseReduction(s.hissRemovalPercent, s.deHumEnabled, s.humFrequency)

        audioEngine.vintageMode = s.isVintageMode
        audioEngine.wowFlutterDepth = s.wowFlutterDepth
        audioEngine.vintageNoiseLevel = s.vintageNoiseLevel
        systemEffects.updateVintageMode(s.isVintageMode)

        audioEngine.compThresholdDb = s.compThresholdDb
        audioEngine.compRatio = s.compRatio
        audioEngine.compAttackMs = s.compAttackMs
        audioEngine.compReleaseMs = s.compReleaseMs
        audioEngine.limiterCeilingDb = s.limiterCeilingDb
        systemEffects.updateDynamicsCompressor(
            thresholdDb = s.compThresholdDb,
            ratio = s.compRatio,
            attackMs = s.compAttackMs,
            releaseMs = s.compReleaseMs
        )

        audioEngine.spatialRoomType = s.spatialRoomType
        systemEffects.updateSpatialRoom(s.spatialRoomType)

        audioEngine.reverbWet = s.reverbWetPercent
        audioEngine.reverbRoomSize = s.reverbRoomSizePercent
        audioEngine.reverbDamping = s.reverbDampingPercent
        audioEngine.echoWet = s.echoWetPercent
        audioEngine.echoTimeMs = s.echoTimeMs
        audioEngine.echoFeedback = s.echoFeedbackPercent
        audioEngine.roomSize = s.roomSize
        audioEngine.wallMaterial = s.wallMaterial
        systemEffects.updateReverb(
            wetPercent = s.reverbWetPercent,
            roomSizePercent = s.reverbRoomSizePercent,
            dampingPercent = s.reverbDampingPercent,
            echoTimeMs = s.echoTimeMs,
            echoWetPercent = s.echoWetPercent,
            echoFeedbackPercent = s.echoFeedbackPercent
        )
        audioEngine.setPlaybackSpeed(s.playbackSpeed)
        audioEngine.updateDspCoefficients()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopPlayback()
        deviceManager.unregisterCallback()
        // Note: systemEffects are intentionally NOT released here so background
        // audio enhancement (YouTube, Spotify, etc.) managed by AudioProcessingService
        // continues uninterrupted when the UI activity/ViewModel is cleared.
    }
}
