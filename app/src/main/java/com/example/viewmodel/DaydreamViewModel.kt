package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.media.AudioManager
import android.net.Uri
import com.example.audio.AudioDeviceManager
import com.example.audio.AudioEngine
import com.example.audio.LocalTrackManager
import com.example.audio.SystemAudioEffectManager
import com.example.model.AudioComplaint
import com.example.model.CustomSoundPreset
import com.example.model.DemoTrack
import com.example.model.LocalTrack
import com.example.model.MidSideMode
import com.example.model.ReferenceMonitor
import com.example.model.LimiterMode
import com.example.model.OutputDevice
import com.example.model.ParametricBand
import com.example.model.PlainBand
import com.example.model.PresetExportBundle
import com.example.model.SoundTargetPreset
import com.example.model.TimeMachinePreset
import com.example.model.TestToneMode
import com.example.model.SubCutFilter
import com.example.model.StreamingTarget
import com.example.model.LufsMetrics
import com.example.model.HarmonicSaturationType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    val wallMaterial: com.example.audio.AudioEngine.WallMaterial = com.example.audio.AudioEngine.WallMaterial.PLASTER,
    val varispeedMode: Boolean = true,
    val speedAppliedAsRequested: Boolean = true,
    val confirmedPlaybackSpeed: Float = 1.0f,

    // Pro Studio Reference Suite
    val midSideMode: MidSideMode = MidSideMode.STEREO,
    val bassMonoMakerEnabled: Boolean = false,
    val referenceMonitor: ReferenceMonitor = ReferenceMonitor.FLAT,
    val limiterMode: LimiterMode = LimiterMode.SOFT_BRICKWALL,
    val tapeDrivePercent: Float = 0f,
    val reverbFreezeEnabled: Boolean = false,
    val phaseCorrelation: Float = 0.85f,
    val crestFactorDb: Float = 12.0f,
    val peakDbfs: Float = -6.0f,
    val rmsDbfs: Float = -18.0f,
    val showDawExportDialog: Boolean = false,

    // Pro Studio ITU-R BS.1770-4 LUFS & Streaming Loudness Suite
    val lufsMetrics: LufsMetrics = LufsMetrics(),
    val streamingTarget: StreamingTarget = StreamingTarget.SPOTIFY_14,

    // Pro Studio Calibration & Test Tones
    val testToneMode: TestToneMode = TestToneMode.OFF,
    val testToneLevelDb: Float = -18f,

    // Pro Studio Mastering Infrasonic Sub-Cut Filter
    val subCutFilter: SubCutFilter = SubCutFilter.OFF,

    // Pro Studio Dynamic Frequency De-Esser
    val deEsserEnabled: Boolean = false,
    val deEsserThresholdDb: Float = -18f,
    val deEsserMaxReductionDb: Float = 6f,
    val deEsserReductionDb: Float = 0f,

    // Pro Studio Loudness-Matched A/B Monitoring
    val gainMatchedABEnabled: Boolean = false,

    // Pro Studio Dynamic Transient Designer (SPL / Oxford TransMod)
    val transientAttackPercent: Float = 0f,
    val transientSustainPercent: Float = 0f,
    val transientAttackActivity: Float = 0f,
    val transientSustainActivity: Float = 0f,

    // Pillar 2: measured compressor gain reduction from the DSP loop.
    val measuredGrDb: Float = 0f,

    // Pro Studio 3-band multiband compressor (player-only DSP).
    val multibandEnabled: Boolean = false,
    val mbXoverLowHz: Float = 250f,
    val mbXoverHighHz: Float = 4000f,
    val mbThreshLowDb: Float = -18f,
    val mbThreshMidDb: Float = -18f,
    val mbThreshHighDb: Float = -18f,
    val mbRatioLow: Float = 2f,
    val mbRatioMid: Float = 2f,
    val mbRatioHigh: Float = 2f,
    val mbAttackMs: Float = 20f,
    val mbReleaseMs: Float = 150f,
    val mbKneeDb: Float = 6f,
    val mbSidechainHpfHz: Int = 0,
    val mbSoloLow: Boolean = false,
    val mbSoloMid: Boolean = false,
    val mbSoloHigh: Boolean = false,
    val mbGrLowDb: Float = 0f,
    val mbGrMidDb: Float = 0f,
    val mbGrHighDb: Float = 0f,

    // Engine processing rate (pillar: 44.1/48kHz agility).
    val sampleRateMode: com.example.model.SampleRateMode = com.example.model.SampleRateMode.AUTO,
    val effectiveSampleRateHz: Int = 44100,

    // Pro Studio Analog Harmonic Saturation Color Topology
    val harmonicSaturationType: HarmonicSaturationType = HarmonicSaturationType.CLEAN,
    val harmonicDrivePercent: Float = 0f,
    val thdPercent: Float = 0f,

    // Pro Studio ISO 226 Fletcher-Munson Equal-Loudness Calibration
    val fletcherMunsonEnabled: Boolean = false,

    // Pro Studio Lissajous Goniometer Vector Scope Points (256 (X, Y) coordinate pairs)
    val vectorScopePoints: FloatArray = FloatArray(512),

    // Pro Studio Stereo Balance Trim & Polarity Inversion
    val stereoBalanceTrimDb: Float = 0f,
    val invertLeftPolarity: Boolean = false,
    val invertRightPolarity: Boolean = false,

    // Sound Targets & Custom Presets Bank
    val activeSoundTargetId: String? = null,
    val customPresets: List<CustomSoundPreset> = listOf(
        CustomSoundPreset(
            id = "preset_warm_bass",
            name = "Warm Vinyl Bass",
            colorHex = "#FF9F0A",
            eqGains = mapOf(
                PlainBand.RUMBLE to 3f,
                PlainBand.WARMTH to 2f,
                PlainBand.BODY to 0.5f,
                PlainBand.CLARITY to 1f,
                PlainBand.AIR to 2f
            ),
            spacePercent = 40f,
            punchPercent = 35f,
            clarityPercent = 20f
        ),
        CustomSoundPreset(
            id = "preset_crisp_vocal",
            name = "Intimate Vocal",
            colorHex = "#0A84FF",
            eqGains = mapOf(
                PlainBand.RUMBLE to -2f,
                PlainBand.WARMTH to 0f,
                PlainBand.BODY to 1.5f,
                PlainBand.CLARITY to 4f,
                PlainBand.AIR to 3f
            ),
            spacePercent = 25f,
            punchPercent = 20f,
            clarityPercent = 45f
        )
    ),
    val showSavePresetDialog: Boolean = false,

    // Smart Device Routing & Hearing Comfort
    val autoSwitchDeviceProfiles: Boolean = true,
    val comfortLimiterEnabled: Boolean = false,

    // In-App Local MP3 Library & External Playback State
    val localTracks: List<LocalTrack> = emptyList(),
    val currentLocalTrack: LocalTrack? = null,
    val isExternalPlaybackActive: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val playbackDurationMs: Long = 0L,
    val isImportingTrack: Boolean = false,

    // Pro Studio workflow: A/B/C/D mix snapshots, undo depth, QC peak-hold,
    // offline bounce export with loudness auto-match.
    val mixSnapshots: List<com.example.model.MixSnapshot?> = List(4) { null },
    val activeSnapshotIndex: Int = -1,
    val canUndoMix: Boolean = false,
    val truePeakHoldDbtp: Float = -90f,
    val clipCount: Int = 0,
    val isBouncing: Boolean = false,
    val lastBouncePath: String? = null,
    val lastBounceGainDb: Float = 0f,
    val lastBouncePeakDbtp: Float = -90f,
    // Pillar 1: real track bounce output options + verify readout.
    val bounceBitDepth: com.example.audio.BounceBitDepth = com.example.audio.BounceBitDepth.PCM_16,
    val bounceProgress: Float? = null, // 0..1 while rendering, null when idle
    val bounceVerifyText: String? = null
)

class DaydreamViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("daydream_audio_prefs", Context.MODE_PRIVATE)
    private val audioEngine = AudioEngine()
    private val systemEffects = SystemAudioEffectManager.instance
    private val deviceManager = AudioDeviceManager(application)
    private val localTrackManager = LocalTrackManager(application)

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

    // Pro Studio undo stack (up to 20 full-chain snapshots)
    private val mixUndoStack = ArrayDeque<com.example.model.MixSnapshot>()

    private fun pushMixUndo(label: String = "Mix change") {
        try {
            mixUndoStack.addLast(audioEngine.captureSnapshot(label))
            while (mixUndoStack.size > com.example.model.MixSnapshot.MAX_UNDO) {
                mixUndoStack.removeFirst()
            }
            _uiState.update { it.copy(canUndoMix = true) }
        } catch (_: Exception) { }
    }

    private fun snapshotToUi(s: com.example.model.MixSnapshot): DaydreamUiState {
        val cur = _uiState.value
        val eq = cur.eqGains.toMutableMap()
        s.eqGains.forEach { (k, v) ->
            runCatching { eq[PlainBand.valueOf(k)] = v }
        }
        val adv = cur.advancedBands.map { b ->
            val g = s.parametricGains[b.hz.toString()] ?: b.gainDb
            val q = s.parametricQ[b.hz.toString()] ?: b.q
            b.copy(gainDb = g, q = q)
        }
        return cur.copy(
            eqGains = eq,
            advancedBands = adv,
            spacePercent = s.spacePercent,
            punchPercent = s.punchPercent,
            clarityMacroPercent = s.clarityMacroPercent,
            loudnessPercent = s.loudnessPercent,
            hissRemovalPercent = s.hissRemovalPercent,
            deHumEnabled = s.deHumEnabled,
            humFrequency = s.humFrequency,
            deCrackleEnabled = s.deCrackleEnabled,
            compThresholdDb = s.compThresholdDb,
            compRatio = s.compRatio,
            compAttackMs = s.compAttackMs,
            compReleaseMs = s.compReleaseMs,
            limiterCeilingDb = s.limiterCeilingDb,
            reverbWetPercent = s.reverbWetPercent,
            reverbRoomSizePercent = s.reverbRoomSizePercent,
            reverbDampingPercent = s.reverbDampingPercent,
            reverbFreezeEnabled = s.reverbFreezeEnabled,
            echoTimeMs = s.echoTimeMs,
            echoFeedbackPercent = s.echoFeedbackPercent,
            echoWetPercent = s.echoWetPercent,
            roomSize = runCatching { com.example.audio.AudioEngine.RoomSize.valueOf(s.roomSizeName) }.getOrDefault(cur.roomSize),
            wallMaterial = runCatching { com.example.audio.AudioEngine.WallMaterial.valueOf(s.wallMaterialName) }.getOrDefault(cur.wallMaterial),
            midSideMode = runCatching { MidSideMode.valueOf(s.midSideName) }.getOrDefault(cur.midSideMode),
            bassMonoMakerEnabled = s.bassMonoMakerEnabled,
            referenceMonitor = runCatching { ReferenceMonitor.valueOf(s.referenceMonitorName) }.getOrDefault(cur.referenceMonitor),
            limiterMode = runCatching { LimiterMode.valueOf(s.limiterModeName) }.getOrDefault(cur.limiterMode),
            transientAttackPercent = s.transientAttackPercent,
            transientSustainPercent = s.transientSustainPercent,
            harmonicSaturationType = runCatching { com.example.model.HarmonicSaturationType.valueOf(s.harmonicSaturationName) }.getOrDefault(cur.harmonicSaturationType),
            harmonicDrivePercent = s.harmonicDrivePercent,
            fletcherMunsonEnabled = s.fletcherMunsonEnabled,
            subCutFilter = runCatching { SubCutFilter.valueOf(s.subCutName) }.getOrDefault(cur.subCutFilter),
            deEsserEnabled = s.deEsserEnabled,
            deEsserThresholdDb = s.deEsserThresholdDb,
            deEsserMaxReductionDb = s.deEsserMaxReductionDb,
            stereoBalanceTrimDb = s.stereoBalanceTrimDb,
            invertLeftPolarity = s.invertLeftPolarity,
            invertRightPolarity = s.invertRightPolarity,
            multibandEnabled = s.multibandEnabled,
            mbXoverLowHz = s.mbXoverLowHz,
            mbXoverHighHz = s.mbXoverHighHz,
            mbThreshLowDb = s.mbThreshLowDb,
            mbThreshMidDb = s.mbThreshMidDb,
            mbThreshHighDb = s.mbThreshHighDb,
            mbRatioLow = s.mbRatioLow,
            mbRatioMid = s.mbRatioMid,
            mbRatioHigh = s.mbRatioHigh,
            mbAttackMs = s.mbAttackMs,
            mbReleaseMs = s.mbReleaseMs,
            mbKneeDb = s.mbKneeDb,
            mbSidechainHpfHz = s.mbSidechainHpfHz,
            mbSoloLow = s.mbSoloLow,
            mbSoloMid = s.mbSoloMid,
            mbSoloHigh = s.mbSoloHigh,
            playbackSpeed = s.playbackSpeed,
            varispeedMode = s.varispeedMode,
            isVintageMode = s.isVintageMode,
            wowFlutterDepth = s.wowFlutterDepth,
            vintageNoiseLevel = s.vintageNoiseLevel
        )
    }

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

        audioEngine.onStudioMetricsUpdated = { phase, crest, peak, rms ->
            _uiState.update { state ->
                state.copy(
                    phaseCorrelation = phase,
                    crestFactorDb = crest,
                    peakDbfs = peak,
                    rmsDbfs = rms
                )
            }
        }

        audioEngine.onLufsMetricsUpdated = { metrics ->
            _uiState.update { it.copy(lufsMetrics = metrics) }
        }

        audioEngine.onDeEsserReductionUpdated = { reductionDb ->
            _uiState.update { it.copy(deEsserReductionDb = reductionDb) }
        }

        audioEngine.onVectorScopeUpdated = { points ->
            _uiState.update { it.copy(vectorScopePoints = points.clone()) }
        }

        audioEngine.onTransientActivityUpdated = { attack, sustain ->
            _uiState.update { it.copy(transientAttackActivity = attack, transientSustainActivity = sustain) }
        }

        audioEngine.onThdUpdated = { thd ->
            _uiState.update { it.copy(thdPercent = thd) }
        }

        audioEngine.onGrDbUpdated = { grDb ->
            _uiState.update { it.copy(measuredGrDb = grDb) }
        }

        audioEngine.onMbGrUpdated = { lowDb, midDb, highDb ->
            _uiState.update { it.copy(mbGrLowDb = lowDb, mbGrMidDb = midDb, mbGrHighDb = highDb) }
        }

        audioEngine.onPeakHoldUpdated = { hold, clips ->
            _uiState.update { it.copy(truePeakHoldDbtp = hold, clipCount = clips) }
        }

        // Listen for hardware output device changes (PRD FR-10)
        viewModelScope.launch {
            deviceManager.currentDevice.collect { device ->
                _uiState.update { it.copy(currentDevice = device) }
            }
        }
        viewModelScope.launch {
            deviceManager.deviceChangePrompt.collect { promptDevice ->
                if (promptDevice != null && _uiState.value.autoSwitchDeviceProfiles) {
                    setOutputDevice(promptDevice)
                } else {
                    _uiState.update { it.copy(devicePrompt = promptDevice) }
                }
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

        // Initialize local MP3 storage library
        val initialLocalTracks = localTrackManager.getAllTracks()
        _uiState.update { it.copy(localTracks = initialLocalTracks) }

        audioEngine.onPlaybackFinished = {
            nextTrack()
        }

        // Coroutine for local track position and duration updates
        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isPlaying && _uiState.value.currentLocalTrack != null) {
                    val pos = audioEngine.getCurrentPositionMs()
                    val dur = audioEngine.getDurationMs()
                    _uiState.update { it.copy(playbackPositionMs = pos, playbackDurationMs = dur) }
                }
                delay(250)
            }
        }

        // Coroutine for external audio presence detection.
        // Honest limitation: Android offers no public API to capture another app's
        // PCM without privileged capture, so there is no real spectrum/RMS for
        // external audio. We only flip the presence flag and let the last real
        // in-app meter values decay to idle - never fabricate dancing bars.
        val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        viewModelScope.launch {
            while (isActive) {
                val internalPlaying = audioEngine.isCurrentlyPlaying()
                val extActive = !internalPlaying && (audioManager?.isMusicActive == true || _uiState.value.activeSystemSessions.isNotEmpty())
                if (extActive != _uiState.value.isExternalPlaybackActive) {
                    _uiState.update { it.copy(isExternalPlaybackActive = extActive) }
                }
                if (extActive) {
                    _uiState.update {
                        val decayed = FloatArray(8) { i ->
                            (it.spectrum.getOrElse(i) { 0.1f } * 0.92f).coerceIn(0.04f, 1f)
                        }
                        it.copy(
                            spectrum = decayed,
                            audioRms = (it.audioRms * 0.92f).coerceIn(0.03f, 1f)
                        )
                    }
                }
                delay(250)
            }
        }

        // Pillar: resolve the engine rate from the device's native output.
        // Applied while idle in init; later changes stop playback first.
        applySampleRateMode(_uiState.value.sampleRateMode, silent = true)

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
            audioEngine.playDemoTrack(index, viewModelScope)
            _uiState.update {
                it.copy(
                    currentTrack = audioEngine.demoTracks[index],
                    currentLocalTrack = null,
                    isPlaying = true
                )
            }
        }
    }

    fun playLocalTrack(track: LocalTrack) {
        audioEngine.playLocalTrack(track, viewModelScope)
        _uiState.update {
            it.copy(
                currentLocalTrack = track,
                currentTrack = null,
                isPlaying = true,
                playbackPositionMs = 0L,
                playbackDurationMs = track.durationMs
            )
        }
    }

    fun importLocalMp3(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingTrack = true) }
            val track = localTrackManager.importTrackFromUri(uri)
            val all = localTrackManager.getAllTracks()
            _uiState.update {
                it.copy(
                    localTracks = all,
                    isImportingTrack = false,
                    notificationMessage = if (track != null) "Imported: ${track.title}" else "Failed to import MP3 file"
                )
            }
            if (track != null) {
                playLocalTrack(track)
            }
        }
    }

    fun deleteLocalTrack(trackId: String) {
        if (_uiState.value.currentLocalTrack?.id == trackId) {
            audioEngine.stopPlayback()
            _uiState.update { it.copy(isPlaying = false, currentLocalTrack = null) }
        }
        localTrackManager.deleteTrack(trackId)
        val all = localTrackManager.getAllTracks()
        _uiState.update {
            it.copy(
                localTracks = all,
                notificationMessage = "Track removed from in-app library"
            )
        }
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        _uiState.update { it.copy(playbackPositionMs = positionMs) }
    }

    fun nextTrack() {
        val state = _uiState.value
        if (state.currentLocalTrack != null && state.localTracks.isNotEmpty()) {
            val currentIdx = state.localTracks.indexOfFirst { it.id == state.currentLocalTrack.id }
            val nextIdx = if (currentIdx >= 0) (currentIdx + 1) % state.localTracks.size else 0
            playLocalTrack(state.localTracks[nextIdx])
        } else {
            val currentIdx = audioEngine.currentTrackIndex
            val nextIdx = (currentIdx + 1) % audioEngine.demoTracks.size
            selectTrack(nextIdx)
        }
    }

    fun previousTrack() {
        val state = _uiState.value
        if (state.currentLocalTrack != null && state.localTracks.isNotEmpty()) {
            val currentIdx = state.localTracks.indexOfFirst { it.id == state.currentLocalTrack.id }
            val prevIdx = if (currentIdx > 0) currentIdx - 1 else state.localTracks.size - 1
            playLocalTrack(state.localTracks[prevIdx])
        } else {
            val currentIdx = audioEngine.currentTrackIndex
            val prevIdx = if (currentIdx > 0) currentIdx - 1 else audioEngine.demoTracks.size - 1
            selectTrack(prevIdx)
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
        val clamped = speed.coerceIn(0.25f, 2.0f)
        audioEngine.setPlaybackSpeed(clamped)
        // Honesty check (PRD 12.1 pattern): reflect what the OS actually
        // confirmed, not just what was requested - some devices/OEMs clamp
        // extreme speed values rather than applying them as-is.
        _uiState.update {
            it.copy(
                playbackSpeed = clamped,
                speedAppliedAsRequested = audioEngine.speedAppliedAsRequested,
                confirmedPlaybackSpeed = audioEngine.lastConfirmedSpeed
            )
        }
    }

    fun setVarispeedMode(enabled: Boolean) {
        audioEngine.varispeedMode = enabled
        audioEngine.setPlaybackSpeed(audioEngine.playbackSpeed) // re-apply pitch immediately
        _uiState.update { it.copy(varispeedMode = enabled) }
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
        val clamped = timeMs.coerceIn(50, 3000)
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
        val clamped = percent.coerceIn(0f, 96f)
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

    // Pro Studio Reference Suite Setters
    fun setMidSideMode(mode: MidSideMode) {
        audioEngine.midSideMode = mode
        _uiState.update { it.copy(midSideMode = mode) }
    }

    fun setBassMonoMaker(enabled: Boolean) {
        audioEngine.bassMonoMakerEnabled = enabled
        _uiState.update { it.copy(bassMonoMakerEnabled = enabled) }
    }

    fun setReferenceMonitor(monitor: ReferenceMonitor) {
        audioEngine.referenceMonitor = monitor
        audioEngine.updateDspCoefficients()
        _uiState.update { it.copy(referenceMonitor = monitor) }
    }

    fun setLimiterMode(mode: LimiterMode) {
        audioEngine.limiterMode = mode
        _uiState.update { it.copy(limiterMode = mode) }
    }

    fun setTapeDrivePercent(percent: Float) {
        val clamped = percent.coerceIn(0f, 100f)
        audioEngine.tapeDrivePercent = clamped
        _uiState.update { it.copy(tapeDrivePercent = clamped) }
    }

    fun setTransientAttackPercent(percent: Float) {
        val clamped = percent.coerceIn(-100f, 100f)
        audioEngine.transientAttackPercent = clamped
        _uiState.update { it.copy(transientAttackPercent = clamped) }
    }

    fun setTransientSustainPercent(percent: Float) {
        val clamped = percent.coerceIn(-100f, 100f)
        audioEngine.transientSustainPercent = clamped
        _uiState.update { it.copy(transientSustainPercent = clamped) }
    }

    fun setHarmonicSaturationType(type: HarmonicSaturationType) {
        audioEngine.harmonicSaturationType = type
        _uiState.update { it.copy(harmonicSaturationType = type) }
    }

    fun setHarmonicDrivePercent(percent: Float) {
        val clamped = percent.coerceIn(0f, 100f)
        audioEngine.harmonicDrivePercent = clamped
        _uiState.update { it.copy(harmonicDrivePercent = clamped) }
    }

    fun toggleFletcherMunson(enabled: Boolean) {
        audioEngine.fletcherMunsonEnabled = enabled
        _uiState.update { it.copy(fletcherMunsonEnabled = enabled) }
    }

    fun toggleReverbFreeze() {
        val next = !_uiState.value.reverbFreezeEnabled
        audioEngine.reverbFreezeEnabled = next
        _uiState.update { it.copy(reverbFreezeEnabled = next) }
    }

    fun setDawExportDialogOpen(open: Boolean) {
        _uiState.update { it.copy(showDawExportDialog = open) }
    }

    fun setTestToneMode(mode: TestToneMode) {
        audioEngine.testToneMode = mode
        _uiState.update { it.copy(testToneMode = mode) }
    }

    fun setTestToneLevelDb(levelDb: Float) {
        val clamped = levelDb.coerceIn(-36f, 0f)
        audioEngine.testToneLevelDb = clamped
        _uiState.update { it.copy(testToneLevelDb = clamped) }
    }

    fun setSubCutFilter(filter: SubCutFilter) {
        audioEngine.subCutFilter = filter
        audioEngine.updateDspCoefficients()
        _uiState.update { it.copy(subCutFilter = filter) }
    }

    fun setDeEsserEnabled(enabled: Boolean) {
        audioEngine.deEsserEnabled = enabled
        _uiState.update { it.copy(deEsserEnabled = enabled) }
    }

    fun setDeEsserThresholdDb(thresholdDb: Float) {
        val clamped = thresholdDb.coerceIn(-36f, -6f)
        audioEngine.deEsserThresholdDb = clamped
        _uiState.update { it.copy(deEsserThresholdDb = clamped) }
    }

    fun setDeEsserMaxReductionDb(maxReductionDb: Float) {
        val clamped = maxReductionDb.coerceIn(1f, 18f)
        audioEngine.deEsserMaxReductionDb = clamped
        _uiState.update { it.copy(deEsserMaxReductionDb = clamped) }
    }

    fun setGainMatchedAB(enabled: Boolean) {
        audioEngine.gainMatchedAB = enabled
        _uiState.update { it.copy(gainMatchedABEnabled = enabled) }
    }

    fun setStreamingTarget(target: StreamingTarget) {
        audioEngine.streamingTarget = target
        _uiState.update { it.copy(streamingTarget = target) }
    }

    // Pro Studio 3-band multiband compressor setters (player-only DSP stage).
    fun setMultibandEnabled(enabled: Boolean) {
        audioEngine.multibandEnabled = enabled
        _uiState.update { it.copy(multibandEnabled = enabled) }
    }

    fun setMbXoverLowHz(hz: Float) {
        val clamped = hz.coerceIn(60f, 800f)
        audioEngine.mbXoverLowHz = clamped
        audioEngine.updateDspCoefficients()
        _uiState.update { it.copy(mbXoverLowHz = clamped) }
    }

    fun setMbXoverHighHz(hz: Float) {
        val clamped = hz.coerceIn(1000f, 12000f)
        audioEngine.mbXoverHighHz = clamped
        audioEngine.updateDspCoefficients()
        _uiState.update { it.copy(mbXoverHighHz = clamped) }
    }

    fun setMbThreshDb(band: Int, threshDb: Float) {
        val clamped = threshDb.coerceIn(-40f, 0f)
        when (band.coerceIn(0, 2)) {
            0 -> audioEngine.mbThreshLowDb = clamped
            1 -> audioEngine.mbThreshMidDb = clamped
            else -> audioEngine.mbThreshHighDb = clamped
        }
        _uiState.update {
            when (band.coerceIn(0, 2)) {
                0 -> it.copy(mbThreshLowDb = clamped)
                1 -> it.copy(mbThreshMidDb = clamped)
                else -> it.copy(mbThreshHighDb = clamped)
            }
        }
    }

    fun setMbRatio(band: Int, ratio: Float) {
        val clamped = ratio.coerceIn(1f, 10f)
        when (band.coerceIn(0, 2)) {
            0 -> audioEngine.mbRatioLow = clamped
            1 -> audioEngine.mbRatioMid = clamped
            else -> audioEngine.mbRatioHigh = clamped
        }
        _uiState.update {
            when (band.coerceIn(0, 2)) {
                0 -> it.copy(mbRatioLow = clamped)
                1 -> it.copy(mbRatioMid = clamped)
                else -> it.copy(mbRatioHigh = clamped)
            }
        }
    }

    fun setMbAttackMs(attackMs: Float) {
        val clamped = attackMs.coerceIn(1f, 100f)
        audioEngine.mbAttackMs = clamped
        _uiState.update { it.copy(mbAttackMs = clamped) }
    }

    fun setMbReleaseMs(releaseMs: Float) {
        val clamped = releaseMs.coerceIn(10f, 500f)
        audioEngine.mbReleaseMs = clamped
        _uiState.update { it.copy(mbReleaseMs = clamped) }
    }

    fun setMbKneeDb(kneeDb: Float) {
        val clamped = kneeDb.coerceIn(0f, 12f)
        audioEngine.mbKneeDb = clamped
        _uiState.update { it.copy(mbKneeDb = clamped) }
    }

    fun setMbSidechainHpfHz(hz: Int) {
        val clamped = when (hz) {
            80, 150 -> hz
            else -> 0
        }
        audioEngine.mbSidechainHpfHz = clamped
        audioEngine.updateDspCoefficients()
        _uiState.update { it.copy(mbSidechainHpfHz = clamped) }
    }

    fun setMbSolo(band: Int, solo: Boolean) {
        when (band.coerceIn(0, 2)) {
            0 -> audioEngine.mbSoloLow = solo
            1 -> audioEngine.mbSoloMid = solo
            else -> audioEngine.mbSoloHigh = solo
        }
        _uiState.update {
            when (band.coerceIn(0, 2)) {
                0 -> it.copy(mbSoloLow = solo)
                1 -> it.copy(mbSoloMid = solo)
                else -> it.copy(mbSoloHigh = solo)
            }
        }
    }

    fun setStereoBalanceTrimDb(trimDb: Float) {
        val clamped = trimDb.coerceIn(-6f, 6f)
        audioEngine.stereoBalanceTrimDb = clamped
        _uiState.update { it.copy(stereoBalanceTrimDb = clamped) }
    }

    fun setInvertLeftPolarity(invert: Boolean) {
        audioEngine.invertLeftPolarity = invert
        _uiState.update { it.copy(invertLeftPolarity = invert) }
    }

    fun setInvertRightPolarity(invert: Boolean) {
        audioEngine.invertRightPolarity = invert
        _uiState.update { it.copy(invertRightPolarity = invert) }
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
            systemEffects.updateVintageMode(true)
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
            systemEffects.updateVintageMode(preLofiVintageMode)
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
        systemEffects.updateNoiseReduction(hiss, deHum, _uiState.value.humFrequency)
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
        systemEffects.updateNoiseReduction(preset.hissRemoval, preset.deHumEnabled, _uiState.value.humFrequency)
        audioEngine.vintageMode = false
        audioEngine.wowFlutterDepth = 0f
        systemEffects.updateVintageMode(false)
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

    // Sound Targets (Studio Target Curves)
    fun applySoundTarget(target: SoundTargetPreset) {
        val updatedEq = mapOf(
            PlainBand.RUMBLE to target.rumbleDb,
            PlainBand.WARMTH to target.warmthDb,
            PlainBand.BODY to target.bodyDb,
            PlainBand.CLARITY to target.clarityDb,
            PlainBand.AIR to target.airDb
        )
        audioEngine.eqGains.putAll(updatedEq)
        systemEffects.updatePlainEqGains(updatedEq)

        audioEngine.spaceAmount = target.spacePercent
        systemEffects.updateSpace(target.spacePercent, _uiState.value.isMonoDetected)

        audioEngine.punchAmount = target.punchPercent
        systemEffects.updatePunch(target.punchPercent)

        audioEngine.clarityMacroAmount = target.clarityPercent
        systemEffects.updateClarity(target.clarityPercent)
        audioEngine.updateDspCoefficients()

        _uiState.update {
            it.copy(
                activeSoundTargetId = target.id,
                activePresetId = null,
                eqGains = updatedEq,
                spacePercent = target.spacePercent,
                punchPercent = target.punchPercent,
                clarityMacroPercent = target.clarityPercent,
                notificationMessage = "${target.iconEmoji} Target Applied: ${target.title}"
            )
        }
    }

    // User Custom Presets Bank
    fun openSavePresetDialog() {
        _uiState.update { it.copy(showSavePresetDialog = true) }
    }

    fun closeSavePresetDialog() {
        _uiState.update { it.copy(showSavePresetDialog = false) }
    }

    fun saveCustomPreset(name: String, colorHex: String) {
        val cleanName = name.trim().ifEmpty { "My Custom Sound" }
        val newPreset = CustomSoundPreset(
            id = "preset_${System.currentTimeMillis()}",
            name = cleanName,
            colorHex = colorHex,
            eqGains = _uiState.value.eqGains.toMap(),
            spacePercent = _uiState.value.spacePercent,
            punchPercent = _uiState.value.punchPercent,
            clarityPercent = _uiState.value.clarityMacroPercent
        )
        val updatedList = listOf(newPreset) + _uiState.value.customPresets
        _uiState.update {
            it.copy(
                customPresets = updatedList,
                showSavePresetDialog = false,
                notificationMessage = "Saved preset: $cleanName"
            )
        }
    }

    fun applyCustomPreset(preset: CustomSoundPreset) {
        audioEngine.eqGains.putAll(preset.eqGains)
        systemEffects.updatePlainEqGains(preset.eqGains)

        audioEngine.spaceAmount = preset.spacePercent
        systemEffects.updateSpace(preset.spacePercent, _uiState.value.isMonoDetected)

        audioEngine.punchAmount = preset.punchPercent
        systemEffects.updatePunch(preset.punchPercent)

        audioEngine.clarityMacroAmount = preset.clarityPercent
        systemEffects.updateClarity(preset.clarityPercent)
        audioEngine.updateDspCoefficients()

        _uiState.update {
            it.copy(
                activeSoundTargetId = null,
                activePresetId = null,
                eqGains = preset.eqGains,
                spacePercent = preset.spacePercent,
                punchPercent = preset.punchPercent,
                clarityMacroPercent = preset.clarityPercent,
                notificationMessage = "Loaded preset: ${preset.name}"
            )
        }
    }

    fun deleteCustomPreset(id: String) {
        val updated = _uiState.value.customPresets.filterNot { it.id == id }
        _uiState.update { it.copy(customPresets = updated) }
    }

    fun toggleComfortLimiter() {
        val newVal = !_uiState.value.comfortLimiterEnabled
        val targetCeiling = if (newVal) -2.0f else -0.5f
        audioEngine.limiterCeilingDb = targetCeiling
        audioEngine.updateDspCoefficients()
        systemEffects.updateComfortLimiter(newVal)
        _uiState.update {
            it.copy(
                comfortLimiterEnabled = newVal,
                limiterCeilingDb = targetCeiling,
                notificationMessage = if (newVal) "🛡️ Hearing Comfort Limiter ON (-2.0 dB Peak)" else "Comfort Limiter Disabled"
            )
        }
    }

    fun toggleAutoSwitchDeviceProfiles() {
        val newVal = !_uiState.value.autoSwitchDeviceProfiles
        _uiState.update {
            it.copy(
                autoSwitchDeviceProfiles = newVal,
                notificationMessage = if (newVal) "Auto-Switch Device Profiles ON" else "Auto-Switch Device Profiles OFF"
            )
        }
    }

    // Preset JSON Serialization & Export / Import (PRD 6.2 & FR-12)
    fun exportCurrentPresetJson(): String {
        val s = _uiState.value
        val json = JSONObject().apply {
            put("version", 2)
            put("name", "Custom Studio Preset")
            val eqObj = JSONObject()
            s.eqGains.forEach { (band, gain) -> eqObj.put(band.name, gain) }
            put("eqGains", eqObj)
            val paramObj = JSONObject()
            s.advancedBands.forEach { b -> paramObj.put(b.hz.toString(), b.gainDb) }
            put("parametricGains", paramObj)
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
            put("reverbFreezeEnabled", s.reverbFreezeEnabled)
            put("echoTimeMs", s.echoTimeMs)
            put("echoFeedbackPercent", s.echoFeedbackPercent)
            put("echoWetPercent", s.echoWetPercent)
            put("roomSize", s.roomSize.name)
            put("wallMaterial", s.wallMaterial.name)
            put("playbackSpeed", s.playbackSpeed)
            put("varispeedMode", s.varispeedMode)
            put("isLofiMode", s.isLofiMode)
            put("midSideMode", s.midSideMode.name)
            put("bassMonoMakerEnabled", s.bassMonoMakerEnabled)
            put("referenceMonitor", s.referenceMonitor.name)
            put("limiterMode", s.limiterMode.name)
            put("tapeDrivePercent", s.tapeDrivePercent)
            put("transientAttackPercent", s.transientAttackPercent)
            put("transientSustainPercent", s.transientSustainPercent)
            put("harmonicSaturationType", s.harmonicSaturationType.name)
            put("harmonicDrivePercent", s.harmonicDrivePercent)
            put("fletcherMunsonEnabled", s.fletcherMunsonEnabled)
            put("multibandEnabled", s.multibandEnabled)
            put("mbXoverLowHz", s.mbXoverLowHz)
            put("mbXoverHighHz", s.mbXoverHighHz)
            put("mbThreshLowDb", s.mbThreshLowDb)
            put("mbThreshMidDb", s.mbThreshMidDb)
            put("mbThreshHighDb", s.mbThreshHighDb)
            put("mbRatioLow", s.mbRatioLow)
            put("mbRatioMid", s.mbRatioMid)
            put("mbRatioHigh", s.mbRatioHigh)
            put("mbAttackMs", s.mbAttackMs)
            put("mbReleaseMs", s.mbReleaseMs)
            put("mbKneeDb", s.mbKneeDb)
            put("mbSidechainHpfHz", s.mbSidechainHpfHz)
            put("mbSoloLow", s.mbSoloLow)
            put("mbSoloMid", s.mbSoloMid)
            put("mbSoloHigh", s.mbSoloHigh)
        }
        return json.toString(2)
    }

    /**
     * Generates a comprehensive, human-readable DAW Studio Reference Sheet.
     * Mixing and mastering engineers can directly copy or download this specification to recreate
     * the exact acoustic calibration curves in FabFilter Pro-Q, Pro-C, Pro-R, Logic, Ableton, or Pro Tools.
     */
    fun exportDawReferenceSheet(): String {
        val s = _uiState.value
        val sb = StringBuilder()
        sb.appendLine("=========================================================")
        sb.appendLine("      DAYDREAM AUDIO — PRO STUDIO REFERENCE SHEET        ")
        sb.appendLine("=========================================================")
        sb.appendLine("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())}")
        sb.appendLine("Monitoring Bench:      ${s.referenceMonitor.label} (${s.referenceMonitor.subtitle})")
        sb.appendLine("Mid/Side Audition:     ${s.midSideMode.label}")
        sb.appendLine("Bass Mono-Maker (<120Hz): ${if (s.bassMonoMakerEnabled) "ENGAGED" else "BYPASSED"}")
        sb.appendLine("ISO 226 Calibration:   ${if (s.fletcherMunsonEnabled) "ENGAGED (+4.5dB @ 85Hz, +2.5dB @ 8.5kHz)" else "BYPASSED (Linear)"}")
        sb.appendLine("Console Tape/Tube Drive: +${String.format(java.util.Locale.US, "%.1f", s.tapeDrivePercent * 0.18f)} dB (${s.tapeDrivePercent.toInt()}%)")
        sb.appendLine("Mastering Limiter:     ${s.limiterMode.label} (Ceiling: ${s.limiterCeilingDb} dBFS)")
        sb.appendLine()
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("1. 10-BAND PARAMETRIC EQ (FabFilter Pro-Q3 / Logic / DAW)")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine(String.format(java.util.Locale.US, "%-8s | %-10s | %-8s | %s", "BAND", "FREQUENCY", "GAIN", "Q FACTOR / ANCHOR"))
        sb.appendLine("---------|------------|----------|-----------------------")
        s.advancedBands.forEach { b ->
            val sign = if (b.gainDb >= 0) "+" else ""
            sb.appendLine(String.format(java.util.Locale.US, "%-8s | %6d Hz  | %s%5.1f dB | Q = %.2f (%s)",
                "${b.hz}Hz", b.hz, sign, b.gainDb, b.q, b.anchorLabel))
        }
        sb.appendLine()
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("2. DYNAMICS COMPRESSOR (FabFilter Pro-C2 / SSL G-Master)")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("  • Threshold:     ${String.format(java.util.Locale.US, "%.1f", s.compThresholdDb)} dBFS")
        sb.appendLine("  • Ratio:         ${String.format(java.util.Locale.US, "%.1f:1", s.compRatio)}")
        sb.appendLine("  • Attack Time:   ${String.format(java.util.Locale.US, "%.1f", s.compAttackMs)} ms")
        sb.appendLine("  • Release Time:  ${String.format(java.util.Locale.US, "%.1f", s.compReleaseMs)} ms")
        sb.appendLine("  • Auto Makeup:   +${String.format(java.util.Locale.US, "%.1f", -s.compThresholdDb * 0.35f)} dB")
        sb.appendLine()
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("2b. MULTIBAND COMPRESSOR (FabFilter Pro-MB style, player-only)")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("  • Engaged:       ${if (s.multibandEnabled) "YES" else "BYPASSED"}")
        sb.appendLine("  • Crossovers:    LR4 ${s.mbXoverLowHz.toInt()} Hz / ${s.mbXoverHighHz.toInt()} Hz")
        sb.appendLine("  • Low Band:      Thr ${String.format(java.util.Locale.US, "%.1f", s.mbThreshLowDb)} dBFS, Ratio ${String.format(java.util.Locale.US, "%.1f:1", s.mbRatioLow)}")
        sb.appendLine("  • Mid Band:      Thr ${String.format(java.util.Locale.US, "%.1f", s.mbThreshMidDb)} dBFS, Ratio ${String.format(java.util.Locale.US, "%.1f:1", s.mbRatioMid)}")
        sb.appendLine("  • High Band:     Thr ${String.format(java.util.Locale.US, "%.1f", s.mbThreshHighDb)} dBFS, Ratio ${String.format(java.util.Locale.US, "%.1f:1", s.mbRatioHigh)}")
        sb.appendLine("  • Attack/Release:${String.format(java.util.Locale.US, "%.1f", s.mbAttackMs)} ms / ${String.format(java.util.Locale.US, "%.0f", s.mbReleaseMs)} ms (linked)")
        sb.appendLine("  • Knee:          ${String.format(java.util.Locale.US, "%.1f", s.mbKneeDb)} dB (${if (s.mbKneeDb < 0.5f) "hard" else "soft"})")
        sb.appendLine("  • Sidechain HPF: ${if (s.mbSidechainHpfHz <= 0) "Off" else "${s.mbSidechainHpfHz} Hz"}")
        sb.appendLine()
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("3. DYNAMIC TRANSIENT DESIGNER (SPL / Oxford TransMod)")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("  • Attack Sculpt: ${if (s.transientAttackPercent >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", (s.transientAttackPercent / 100f) * 12f)} dB (${s.transientAttackPercent.toInt()}%)")
        sb.appendLine("  • Sustain Tail:  ${if (s.transientSustainPercent >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", (s.transientSustainPercent / 100f) * 12f)} dB (${s.transientSustainPercent.toInt()}%)")
        sb.appendLine("  • Time Constants: τ_fast 1.5ms, τ_slow 25ms, τ_sustain 180ms")
        sb.appendLine()
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("4. ANALOG HARMONIC COLORATION & SATURATION")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("  • Topology:      ${s.harmonicSaturationType.label} (${s.harmonicSaturationType.order})")
        sb.appendLine("  • Drive:         +${String.format(java.util.Locale.US, "%.1f", (s.harmonicDrivePercent / 100f) * 15f)} dB (${s.harmonicDrivePercent.toInt()}%)")
        sb.appendLine("  • Measured THD:  ${String.format(java.util.Locale.US, "%.2f", s.thdPercent)}%")
        sb.appendLine()
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("5. ATMOSPHERIC REVERB (FabFilter Pro-R / Valhalla Room)")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("  • Room Space:    ${s.roomSize.label} (${s.wallMaterial.label})")
        sb.appendLine("  • Wet Mix:       ${s.reverbWetPercent.toInt()}% (Equal-power blend)")
        sb.appendLine("  • Room Size:     ${s.reverbRoomSizePercent.toInt()}% (Continuous RT60)")
        sb.appendLine("  • HF Damping:    ${s.reverbDampingPercent.toInt()}%")
        sb.appendLine("  • Reverb Freeze: ${if (s.reverbFreezeEnabled) "LOCKED (Infinite Ambient Hold)" else "Normal Exponential Decay"}")
        sb.appendLine()
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("6. ANALOG TAPE DELAY / ECHO (Space Echo / EchoBoy)")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("  • Delay Time:    ${s.echoTimeMs} ms")
        sb.appendLine("  • Feedback:      ${s.echoFeedbackPercent.toInt()}%")
        sb.appendLine("  • Wet Mix:       ${s.echoWetPercent.toInt()}% (Punchy direct crossfade)")
        sb.appendLine("  • Topology:      Stereo Ping-Pong with Tape Saturation Feedback")
        sb.appendLine()
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("7. MASTERING SUB-CUT & DE-ESSER")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("  • Infrasonic Sub-Cut:   ${s.subCutFilter.label} (${s.subCutFilter.slope})")
        sb.appendLine("  • Dynamic De-Esser:     ${if (s.deEsserEnabled) "ENGAGED" else "BYPASSED"}")
        if (s.deEsserEnabled) {
            sb.appendLine("    - Sidechain Band:     6.5 kHz (Bell, Q=2.2)")
            sb.appendLine("    - Threshold:          ${String.format(java.util.Locale.US, "%.1f", s.deEsserThresholdDb)} dBFS")
            sb.appendLine("    - Max Attenuation:    ${String.format(java.util.Locale.US, "%.1f", s.deEsserMaxReductionDb)} dB")
            sb.appendLine("    - Live Gain Reduction: -${String.format(java.util.Locale.US, "%.1f", s.deEsserReductionDb)} dB")
        }
        sb.appendLine("  • Stereo Balance Trim:  ${if (s.stereoBalanceTrimDb >= 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", s.stereoBalanceTrimDb)} dB")
        sb.appendLine("  • Channel Polarity:     Left Ø = ${if (s.invertLeftPolarity) "INVERTED (180°)" else "NORMAL"}, Right Ø = ${if (s.invertRightPolarity) "INVERTED (180°)" else "NORMAL"}")
        sb.appendLine()
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("6. BROADCAST LOUDNESS & STREAMING (ITU-R BS.1770-4 / EBU R128)")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("  • Target Standard:      ${s.streamingTarget.platform} (${s.streamingTarget.targetLufs} LUFS, TP: ${s.streamingTarget.maxTruePeakDbtp} dBTP)")
        sb.appendLine("  • Integrated Loudness:  ${String.format(java.util.Locale.US, "%.1f", s.lufsMetrics.integratedLufs)} LUFS (Target Delta: ${String.format(java.util.Locale.US, "%+.1f", s.lufsMetrics.integratedLufs - s.streamingTarget.targetLufs)} LU)")
        sb.appendLine("  • Short-Term (3s):      ${String.format(java.util.Locale.US, "%.1f", s.lufsMetrics.shortTermLufs)} LUFS")
        sb.appendLine("  • Momentary (400ms):    ${String.format(java.util.Locale.US, "%.1f", s.lufsMetrics.momentaryLufs)} LUFS")
        sb.appendLine("  • Loudness Range (LRA): ${String.format(java.util.Locale.US, "%.1f", s.lufsMetrics.loudnessRangeLu)} LU")
        sb.appendLine("  • 4x True Peak (dBTP):  ${String.format(java.util.Locale.US, "%.1f", s.lufsMetrics.truePeakDbtp)} dBTP")
        sb.appendLine()
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("7. MEASURED REFERENCE METRICS")
        sb.appendLine("---------------------------------------------------------")
        sb.appendLine("  • Phase Correlation:    ${String.format(java.util.Locale.US, "%.2f", s.phaseCorrelation)} (-1.0 to +1.0)")
        sb.appendLine("  • Crest Factor (DR):    ${String.format(java.util.Locale.US, "%.1f", s.crestFactorDb)} dB")
        sb.appendLine("  • Peak dBFS:            ${String.format(java.util.Locale.US, "%.1f", s.peakDbfs)} dBFS")
        sb.appendLine("  • RMS dBFS:             ${String.format(java.util.Locale.US, "%.1f", s.rmsDbfs)} dBFS")
        sb.appendLine("=========================================================")
        return sb.toString()
    }

    /**
     * Saves the current preset or DAW calibration sheet to internal/external storage.
     */
    fun savePresetToFile(context: Context, isDawSheet: Boolean): String {
        return try {
            val content = if (isDawSheet) exportDawReferenceSheet() else exportCurrentPresetJson()
            val extension = if (isDawSheet) "txt" else "json"
            val fileName = "daydream_studio_reference_${System.currentTimeMillis()}.$extension"
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = java.io.File(dir, fileName)
            file.writeText(content)
            _uiState.update { it.copy(notificationMessage = "Saved preset to ${file.name}") }
            file.absolutePath
        } catch (e: Exception) {
            _uiState.update { it.copy(notificationMessage = "Failed to save file: ${e.message}") }
            ""
        }
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
            val varispeed = json.optBoolean("varispeedMode", true)
            val lofi = json.optBoolean("isLofiMode", false)
            val midSide = runCatching { MidSideMode.valueOf(json.optString("midSideMode")) }.getOrDefault(MidSideMode.STEREO)
            val bassMono = json.optBoolean("bassMonoMakerEnabled", false)
            val refMon = runCatching { ReferenceMonitor.valueOf(json.optString("referenceMonitor")) }.getOrDefault(ReferenceMonitor.FLAT)
            val limMode = runCatching { LimiterMode.valueOf(json.optString("limiterMode")) }.getOrDefault(LimiterMode.SOFT_BRICKWALL)
            val tapeDrive = json.optDouble("tapeDrivePercent", 0.0).toFloat()
            val revFreeze = json.optBoolean("reverbFreezeEnabled", false)
            val mbEnabled = json.optBoolean("multibandEnabled", false)
            val mbXoLow = json.optDouble("mbXoverLowHz", 250.0).toFloat()
            val mbXoHigh = json.optDouble("mbXoverHighHz", 4000.0).toFloat()
            val mbThL = json.optDouble("mbThreshLowDb", -18.0).toFloat()
            val mbThM = json.optDouble("mbThreshMidDb", -18.0).toFloat()
            val mbThH = json.optDouble("mbThreshHighDb", -18.0).toFloat()
            val mbRaL = json.optDouble("mbRatioLow", 2.0).toFloat()
            val mbRaM = json.optDouble("mbRatioMid", 2.0).toFloat()
            val mbRaH = json.optDouble("mbRatioHigh", 2.0).toFloat()
            val mbAtk = json.optDouble("mbAttackMs", 20.0).toFloat()
            val mbRel = json.optDouble("mbReleaseMs", 150.0).toFloat()
            val mbKnee = json.optDouble("mbKneeDb", 6.0).toFloat()
            val mbSc = json.optInt("mbSidechainHpfHz", 0)
            val mbSoL = json.optBoolean("mbSoloLow", false)
            val mbSoM = json.optBoolean("mbSoloMid", false)
            val mbSoH = json.optBoolean("mbSoloHigh", false)

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
            audioEngine.reverbFreezeEnabled = revFreeze
            audioEngine.echoTimeMs = echoTime
            audioEngine.echoFeedback = echoFeedback
            audioEngine.echoWet = echoWet
            audioEngine.roomSize = roomSizeValue
            audioEngine.wallMaterial = wallMaterialValue
            audioEngine.midSideMode = midSide
            audioEngine.bassMonoMakerEnabled = bassMono
            audioEngine.referenceMonitor = refMon
            audioEngine.limiterMode = limMode
            audioEngine.tapeDrivePercent = tapeDrive
            systemEffects.updateReverb(
                wetPercent = reverbWet,
                roomSizePercent = reverbRoom,
                dampingPercent = reverbDamp,
                echoTimeMs = echoTime,
                echoWetPercent = echoWet
            )
            audioEngine.varispeedMode = varispeed
            audioEngine.setPlaybackSpeed(speed)
            audioEngine.multibandEnabled = mbEnabled
            audioEngine.mbXoverLowHz = mbXoLow
            audioEngine.mbXoverHighHz = mbXoHigh
            audioEngine.mbThreshLowDb = mbThL
            audioEngine.mbThreshMidDb = mbThM
            audioEngine.mbThreshHighDb = mbThH
            audioEngine.mbRatioLow = mbRaL
            audioEngine.mbRatioMid = mbRaM
            audioEngine.mbRatioHigh = mbRaH
            audioEngine.mbAttackMs = mbAtk
            audioEngine.mbReleaseMs = mbRel
            audioEngine.mbKneeDb = mbKnee
            audioEngine.mbSidechainHpfHz = mbSc
            audioEngine.mbSoloLow = mbSoL
            audioEngine.mbSoloMid = mbSoM
            audioEngine.mbSoloHigh = mbSoH
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
                    reverbFreezeEnabled = revFreeze,
                    echoTimeMs = echoTime,
                    echoFeedbackPercent = echoFeedback,
                    echoWetPercent = echoWet,
                    roomSize = roomSizeValue,
                    wallMaterial = wallMaterialValue,
                    playbackSpeed = speed,
                    varispeedMode = varispeed,
                    isLofiMode = lofi,
                    midSideMode = midSide,
                    bassMonoMakerEnabled = bassMono,
                    referenceMonitor = refMon,
                    limiterMode = limMode,
                    tapeDrivePercent = tapeDrive,
                    multibandEnabled = mbEnabled,
                    mbXoverLowHz = mbXoLow,
                    mbXoverHighHz = mbXoHigh,
                    mbThreshLowDb = mbThL,
                    mbThreshMidDb = mbThM,
                    mbThreshHighDb = mbThH,
                    mbRatioLow = mbRaL,
                    mbRatioMid = mbRaM,
                    mbRatioHigh = mbRaH,
                    mbAttackMs = mbAtk,
                    mbReleaseMs = mbRel,
                    mbKneeDb = mbKnee,
                    mbSidechainHpfHz = mbSc,
                    mbSoloLow = mbSoL,
                    mbSoloMid = mbSoM,
                    mbSoloHigh = mbSoH,
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
        audioEngine.varispeedMode = true
        audioEngine.setPlaybackSpeed(1.0f)
        systemEffects.updateReverb(0f, 75f, 35f, 320, 0f)
        audioEngine.updateDspCoefficients()

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
                varispeedMode = true,
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
        systemEffects.updateComfortLimiter(s.comfortLimiterEnabled)
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
        audioEngine.varispeedMode = s.varispeedMode
        audioEngine.setPlaybackSpeed(s.playbackSpeed)
        audioEngine.multibandEnabled = s.multibandEnabled
        audioEngine.mbXoverLowHz = s.mbXoverLowHz
        audioEngine.mbXoverHighHz = s.mbXoverHighHz
        audioEngine.mbThreshLowDb = s.mbThreshLowDb
        audioEngine.mbThreshMidDb = s.mbThreshMidDb
        audioEngine.mbThreshHighDb = s.mbThreshHighDb
        audioEngine.mbRatioLow = s.mbRatioLow
        audioEngine.mbRatioMid = s.mbRatioMid
        audioEngine.mbRatioHigh = s.mbRatioHigh
        audioEngine.mbAttackMs = s.mbAttackMs
        audioEngine.mbReleaseMs = s.mbReleaseMs
        audioEngine.mbKneeDb = s.mbKneeDb
        audioEngine.mbSidechainHpfHz = s.mbSidechainHpfHz
        audioEngine.mbSoloLow = s.mbSoloLow
        audioEngine.mbSoloMid = s.mbSoloMid
        audioEngine.mbSoloHigh = s.mbSoloHigh
        audioEngine.updateDspCoefficients()
    }

    // Pro Studio workflow: snapshots, undo, QC reset, loudness auto-match, offline bounce.
    fun saveMixSnapshot(slot: Int, name: String? = null) {
        val idx = slot.coerceIn(0, 3)
        pushMixUndo("Before snapshot save")
        val slotName = name?.takeIf { it.isNotBlank() } ?: listOf("A", "B", "C", "D")[idx]
        val snap = audioEngine.captureSnapshot("Slot $slotName")
        val updated = _uiState.value.mixSnapshots.toMutableList()
        while (updated.size < 4) updated.add(null)
        updated[idx] = snap.copy(name = "Slot $slotName")
        _uiState.update {
            it.copy(
                mixSnapshots = updated,
                activeSnapshotIndex = idx,
                notificationMessage = "Snapshot $slotName saved — full chain recalled instantly"
            )
        }
    }

    fun recallMixSnapshot(slot: Int) {
        val idx = slot.coerceIn(0, 3)
        val snap = _uiState.value.mixSnapshots.getOrNull(idx) ?: run {
            _uiState.update { it.copy(notificationMessage = "Snapshot slot empty — save first") }
            return
        }
        pushMixUndo("Before recall ${snap.name}")
        audioEngine.restoreSnapshot(snap)
        val mapped = snapshotToUi(snap)
        // Push engine-side parametric + system effects to stay in sync
        systemEffects.updatePlainEqGains(mapped.eqGains)
        systemEffects.updateDynamicsCompressor(
            thresholdDb = mapped.compThresholdDb,
            ratio = mapped.compRatio,
            attackMs = mapped.compAttackMs,
            releaseMs = mapped.compReleaseMs
        )
        systemEffects.updateReverb(
            wetPercent = mapped.reverbWetPercent,
            roomSizePercent = mapped.reverbRoomSizePercent,
            dampingPercent = mapped.reverbDampingPercent,
            echoTimeMs = mapped.echoTimeMs,
            echoWetPercent = mapped.echoWetPercent,
            echoFeedbackPercent = mapped.echoFeedbackPercent
        )
        _uiState.update {
            mapped.copy(
                mixSnapshots = it.mixSnapshots,
                activeSnapshotIndex = idx,
                canUndoMix = true,
                notificationMessage = "Recalled ${snap.name} — instant A/B/C/D compare"
            )
        }
    }

    fun clearMixSnapshot(slot: Int) {
        val idx = slot.coerceIn(0, 3)
        val updated = _uiState.value.mixSnapshots.toMutableList()
        while (updated.size < 4) updated.add(null)
        updated[idx] = null
        _uiState.update { it.copy(mixSnapshots = updated, activeSnapshotIndex = -1) }
    }

    fun undoMixChange() {
        val prev = mixUndoStack.removeLastOrNull() ?: run {
            _uiState.update { it.copy(notificationMessage = "Nothing to undo") }
            return
        }
        audioEngine.restoreSnapshot(prev)
        val mapped = snapshotToUi(prev)
        _uiState.update {
            mapped.copy(
                mixSnapshots = it.mixSnapshots,
                activeSnapshotIndex = it.activeSnapshotIndex,
                canUndoMix = mixUndoStack.isNotEmpty(),
                notificationMessage = "Undone: restored ${prev.name}"
            )
        }
    }

    fun resetPeakHold() {
        audioEngine.resetPeakHold()
        _uiState.update { it.copy(truePeakHoldDbtp = -90f, clipCount = 0) }
    }

    fun loudnessAutoMatchGainDb(): Float {
        val s = _uiState.value
        return com.example.model.MixSnapshot.safeLoudnessGainDb(
            currentIntegratedLufs = s.lufsMetrics.integratedLufs,
            targetLufs = s.streamingTarget.targetLufs,
            currentTruePeakDbtp = s.lufsMetrics.truePeakDbtp,
            ceilingDbtp = s.streamingTarget.maxTruePeakDbtp
        )
    }

    fun setBounceBitDepth(depth: com.example.audio.BounceBitDepth) {
        _uiState.update { it.copy(bounceBitDepth = depth) }
    }

    private fun deviceNativeSampleRateHz(): Int {
        return try {
            val am = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()?.let {
                if (it >= 48000) 48000 else 44100
            } ?: 44100
        } catch (_: Exception) {
            44100
        }
    }

    private fun resolveSampleRateHz(mode: com.example.model.SampleRateMode): Int {
        return when (mode) {
            com.example.model.SampleRateMode.RATE_48000 -> 48000
            com.example.model.SampleRateMode.RATE_44100 -> 44100
            com.example.model.SampleRateMode.AUTO -> deviceNativeSampleRateHz()
        }
    }

    private fun applySampleRateMode(mode: com.example.model.SampleRateMode, silent: Boolean) {
        val rate = resolveSampleRateHz(mode)
        val wasPlaying = _uiState.value.isPlaying
        if (wasPlaying) {
            audioEngine.stopPlayback()
        }
        audioEngine.setEngineSampleRate(rate)
        _uiState.update {
            it.copy(
                sampleRateMode = mode,
                effectiveSampleRateHz = rate,
                isPlaying = false,
                notificationMessage = if (silent || !wasPlaying) {
                    it.notificationMessage
                } else {
                    "Engine rate set to ${rate}Hz — press play to resume"
                }
            )
        }
    }

    fun setSampleRateMode(mode: com.example.model.SampleRateMode) {
        if (mode == _uiState.value.sampleRateMode) return
        applySampleRateMode(mode, silent = false)
    }

    /**
     * Offline bounce entry point. With a local track loaded this renders the
     * ACTUAL song through the full chain (two-pass measure -> match -> print
     * with verify). Without one it falls back to the 5s test-signal bounce
     * used for auditioning the chain on demo synth. Runs on Dispatchers.IO.
     */
    fun bounceCurrentMixToWav(
        context: Context,
        applyLoudnessMatch: Boolean = true,
        durationSeconds: Float = 5f
    ) {
        if (_uiState.value.isBouncing) return
        val local = _uiState.value.currentLocalTrack
        if (local != null) {
            bounceLocalTrackToWav(context, applyLoudnessMatch)
        } else {
            bounceTestSignalToWav(context, applyLoudnessMatch, durationSeconds)
        }
    }

    fun bounceLocalTrackToWav(context: Context, applyLoudnessMatch: Boolean = true) {
        val track = _uiState.value.currentLocalTrack ?: run {
            _uiState.update { it.copy(notificationMessage = "Load a track from the vault first") }
            return
        }
        if (_uiState.value.isBouncing) return
        val snapshot = audioEngine.captureSnapshot("bounce")
        val advanced = _uiState.value.isAdvancedModeActive
        val target = _uiState.value.streamingTarget
        val depth = _uiState.value.bounceBitDepth
        _uiState.update { it.copy(isBouncing = true, bounceProgress = 0f, bounceVerifyText = null) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val source = com.example.audio.DecoderPcmSource(track.filePath)
            try {
                val safeTitle = track.title.replace("[^a-zA-Z0-9._-]".toRegex(), "_").take(40)
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = java.io.File(dir, "daydream_bounce_${safeTitle}_${System.currentTimeMillis()}.wav")
                val result = com.example.audio.TrackBounceRenderer.bounceTrack(
                    source = source,
                    snapshot = snapshot,
                    isAdvancedParametricMode = advanced,
                    target = target,
                    bitDepth = depth,
                    dither = com.example.audio.BounceDither.TPDF,
                    applyLoudnessMatch = applyLoudnessMatch,
                    outFile = file,
                    onProgress = { p ->
                        _uiState.update { it.copy(bounceProgress = p) }
                    },
                    sampleRateHz = _uiState.value.effectiveSampleRateHz
                )
                val verify = "Measured ${fmtLufs(result.measuredIntegratedLufs)} → " +
                    "${fmtGain(result.appliedGainDb)} → landed ${fmtLufs(result.achievedIntegratedLufs)} " +
                    "(target ${fmtLufs(result.targetLufs)}, Δ${fmtDelta(result.deltaToTargetLu)}, " +
                    "peak ${fmtPeak(result.achievedTruePeakDbtp)}). Monitoring excluded."
                _uiState.update {
                    it.copy(
                        isBouncing = false,
                        bounceProgress = null,
                        lastBouncePath = result.filePath,
                        lastBounceGainDb = result.appliedGainDb,
                        lastBouncePeakDbtp = result.achievedTruePeakDbtp,
                        bounceVerifyText = verify,
                        notificationMessage = "Bounced ${file.name} (${depth.bits}-bit/${result.sampleRateHz}Hz, Δ${fmtDelta(result.deltaToTargetLu)} to ${target.platform})"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBouncing = false,
                        bounceProgress = null,
                        notificationMessage = "Track bounce failed: ${e.message}"
                    )
                }
            } finally {
                try {
                    source.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun fmtLufs(v: Float): String =
        if (v <= -60f) "-∞ LUFS" else String.format(java.util.Locale.US, "%.1f LUFS", v)

    private fun fmtGain(v: Float): String =
        String.format(java.util.Locale.US, "%+.1f dB", v)

    private fun fmtDelta(v: Float): String =
        String.format(java.util.Locale.US, "%+.1f LU", v)

    private fun fmtPeak(v: Float): String =
        if (v <= -60f) "-∞ dBTP" else String.format(java.util.Locale.US, "%+.1f dBTP", v)

    private fun bounceTestSignalToWav(
        context: Context,
        applyLoudnessMatch: Boolean = true,
        durationSeconds: Float = 5f
    ) {
        if (_uiState.value.isBouncing) return
        _uiState.update { it.copy(isBouncing = true, bounceProgress = null, bounceVerifyText = null) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val seconds = durationSeconds.coerceIn(1f, 30f)
                val n = (AudioEngine.SAMPLE_RATE * seconds).toInt()
                val inL = DoubleArray(n)
                val inR = DoubleArray(n)
                // Deterministic studio test signal: 440Hz + 1kHz + transient click
                for (i in 0 until n) {
                    val t = i.toDouble() / AudioEngine.SAMPLE_RATE
                    val tone = 0.35 * kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * t) +
                        0.25 * kotlin.math.sin(2.0 * kotlin.math.PI * 1000.0 * t)
                    val click = if (i % 22050 == 0) 0.5 else 0.0
                    inL[i] = (tone + click).coerceIn(-0.9, 0.9)
                    inR[i] = (tone * 0.9 + click).coerceIn(-0.9, 0.9)
                }
                val (outL, outR) = com.example.audio.OfflineBounceRenderer.renderOffline(inL, inR) { l, r ->
                    audioEngine.processStereoSample(l, r)
                }
                var appliedGain = 0f
                if (applyLoudnessMatch) {
                    appliedGain = loudnessAutoMatchGainDb()
                    com.example.audio.OfflineBounceRenderer.applyGainDb(outL, outR, appliedGain)
                }
                val peak = com.example.audio.OfflineBounceRenderer.peakDbtp(outL, outR)
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = java.io.File(dir, "daydream_bounce_${System.currentTimeMillis()}.wav")
                com.example.audio.OfflineBounceRenderer.writeWav16Bit(file, outL, outR)
                _uiState.update {
                    it.copy(
                        isBouncing = false,
                        lastBouncePath = file.absolutePath,
                        lastBounceGainDb = appliedGain,
                        lastBouncePeakDbtp = peak,
                        notificationMessage = "Bounced WAV: ${file.name} (${String.format("%.1f", appliedGain)}dB match, peak ${String.format("%.1f", peak)}dBTP)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isBouncing = false, notificationMessage = "Bounce failed: ${e.message}")
                }
            }
        }
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
