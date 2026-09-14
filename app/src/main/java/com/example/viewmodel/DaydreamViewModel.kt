package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.model.AudioComplaint
import com.example.model.DemoTrack
import com.example.model.OutputDevice
import com.example.model.ParametricBand
import com.example.model.PlainBand
import com.example.model.TimeMachinePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val deCrackleEnabled: Boolean = false,

    // Advanced Mode
    val advancedBands: List<ParametricBand> = listOf(
        ParametricBand(31, 0f, 1.0f),
        ParametricBand(63, 0f, 1.0f),
        ParametricBand(125, 0f, 1.0f),
        ParametricBand(250, 0f, 1.0f),
        ParametricBand(500, 0f, 1.0f),
        ParametricBand(1000, 0f, 1.0f),
        ParametricBand(2000, 0f, 1.0f),
        ParametricBand(4000, 0f, 1.0f),
        ParametricBand(8000, 0f, 1.0f),
        ParametricBand(16000, 0f, 1.0f)
    ),
    val compThresholdDb: Float = -18f,
    val compRatio: Float = 2.5f,
    val compAttackMs: Float = 20f,
    val compReleaseMs: Float = 150f,
    val limiterCeilingDb: Float = -0.5f,
    val hrtfProfile: String = "Natural",

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
    val notificationMessage: String? = null
)

class DaydreamViewModel : ViewModel() {

    private val audioEngine = AudioEngine()

    private val _uiState = MutableStateFlow(
        DaydreamUiState(
            currentTrack = audioEngine.demoTracks.first()
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
        // Connect audio engine callbacks
        audioEngine.onSpectrumUpdated = { bands, rms ->
            _uiState.update { state ->
                val isMono = audioEngine.correlationMetric > 0.90f
                state.copy(
                    spectrum = bands.clone(),
                    audioRms = rms,
                    isMonoDetected = isMono,
                    showMonoWarning = isMono && state.spacePercent > 40f
                )
            }
        }
        syncEngineParameters()
    }

    fun setTab(tab: AppNavTab) {
        _uiState.update { it.copy(currentTab = tab) }
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
        audioEngine.eqGains[band] = clamped
        _uiState.update { it.copy(eqGains = updated, activePresetId = null) }
    }

    fun setSpacePercent(value: Float) {
        val clamped = value.coerceIn(0f, 100f)
        audioEngine.spaceAmount = clamped
        val isMono = _uiState.value.isMonoDetected
        _uiState.update {
            it.copy(
                spacePercent = clamped,
                showMonoWarning = isMono && clamped > 40f,
                activePresetId = null
            )
        }
    }

    fun setPunchPercent(value: Float) {
        val clamped = value.coerceIn(0f, 100f)
        audioEngine.punchAmount = clamped
        _uiState.update { it.copy(punchPercent = clamped, activePresetId = null) }
    }

    fun setClarityMacroPercent(value: Float) {
        val clamped = value.coerceIn(0f, 100f)
        audioEngine.clarityMacroAmount = clamped
        _uiState.update { it.copy(clarityMacroPercent = clamped, activePresetId = null) }
    }

    fun setLoudnessPercent(value: Float) {
        val clamped = value.coerceIn(0f, 100f)
        audioEngine.loudnessBoost = clamped
        _uiState.update { it.copy(loudnessPercent = clamped) }
    }

    fun setHissRemovalPercent(value: Float) {
        val clamped = value.coerceIn(0f, 100f)
        audioEngine.hissRemoval = clamped
        _uiState.update { it.copy(hissRemovalPercent = clamped, activePresetId = null) }
    }

    fun toggleDeHum() {
        val current = !_uiState.value.deHumEnabled
        audioEngine.deHumEnabled = current
        _uiState.update { it.copy(deHumEnabled = current, activePresetId = null) }
    }

    fun toggleDeCrackle() {
        val current = !_uiState.value.deCrackleEnabled
        audioEngine.deCrackleEnabled = current
        _uiState.update { it.copy(deCrackleEnabled = current, activePresetId = null) }
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

        // Apply to audio engine
        updatedEq.forEach { (band, gain) -> audioEngine.eqGains[band] = gain }
        audioEngine.spaceAmount = space
        audioEngine.punchAmount = punch
        audioEngine.clarityMacroAmount = clarityMacro
        audioEngine.hissRemoval = hiss
        audioEngine.deHumEnabled = deHum
        audioEngine.deCrackleEnabled = deCrackle

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
        updatedEq.forEach { (band, gain) -> audioEngine.eqGains[band] = gain }
        audioEngine.spaceAmount = preset.spacePercent
        audioEngine.punchAmount = preset.punchRatio
        audioEngine.hissRemoval = preset.hissRemoval
        audioEngine.deHumEnabled = preset.deHumEnabled
        audioEngine.deCrackleEnabled = preset.deCrackleEnabled

        audioEngine.vintageMode = false
        audioEngine.wowFlutterDepth = 0f

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

        _uiState.update {
            it.copy(
                currentDevice = device,
                spacePercent = device.defaultSpace,
                punchPercent = device.defaultPunch,
                eqGains = updatedEq,
                notificationMessage = "Profile auto-tuned for ${device.displayName}"
            )
        }
    }

    // Golden Ear Trainer (PRD 6.12)
    fun startNewEarChallenge() {
        val bands = PlainBand.entries
        val chosen = bands.random()
        val isBoost = Random.nextBoolean()
        val delta = if (isBoost) 6f else -6f

        // Apply the challenge to audio
        audioEngine.eqGains.forEach { (b, _) -> audioEngine.eqGains[b] = 0f }
        audioEngine.eqGains[chosen] = delta

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

        // Reset EQ back to user's saved state
        syncEngineParameters()
    }

    // Settings
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
        audioEngine.spaceAmount = 30f
        audioEngine.punchAmount = 0f
        audioEngine.clarityMacroAmount = 0f
        audioEngine.loudnessBoost = 0f
        audioEngine.hissRemoval = 0f
        audioEngine.deHumEnabled = false
        audioEngine.deCrackleEnabled = false
        audioEngine.vintageMode = false

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
                activePresetId = null,
                notificationMessage = "All sliders reset to neutral flat"
            )
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    private fun syncEngineParameters() {
        val s = _uiState.value
        s.eqGains.forEach { (band, gain) -> audioEngine.eqGains[band] = gain }
        audioEngine.spaceAmount = s.spacePercent
        audioEngine.punchAmount = s.punchPercent
        audioEngine.clarityMacroAmount = s.clarityMacroPercent
        audioEngine.loudnessBoost = s.loudnessPercent
        audioEngine.hissRemoval = s.hissRemovalPercent
        audioEngine.deHumEnabled = s.deHumEnabled
        audioEngine.deCrackleEnabled = s.deCrackleEnabled
        audioEngine.vintageMode = s.isVintageMode
        audioEngine.wowFlutterDepth = s.wowFlutterDepth
        audioEngine.vintageNoiseLevel = s.vintageNoiseLevel
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopPlayback()
    }
}
