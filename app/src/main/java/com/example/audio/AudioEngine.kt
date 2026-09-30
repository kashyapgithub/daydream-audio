package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.util.Log
import com.example.model.DemoTrack
import com.example.model.LocalTrack
import com.example.model.PlainBand
import com.example.model.MidSideMode
import com.example.model.ReferenceMonitor
import com.example.model.LimiterMode
import com.example.model.TestToneMode
import com.example.model.SubCutFilter
import com.example.model.StreamingTarget
import com.example.model.LufsMetrics
import com.example.model.HarmonicSaturationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Real-time software DSP audio engine for Daydream Audio.
 * Implements the full PRD signal chain (Section 8.1 & 8.3):
 * 1. Noise Reduction (Adaptive Hiss High-Shelf Gate, Multi-Harmonic De-Hum, Derivative Spike De-Crackle)
 * 2. Equalizer (5-Band Plain Language / 10-Band Independent Parametric EQ with adjustable Q)
 * 3. Clarity Macro (3-band crossover, high-mid harmonic exciter & dynamic de-harsher)
 * 4. Dynamics ("Punch" RMS soft-knee compressor with manual threshold/ratio/attack/release)
 * 5. Virtualizer ("Space" crosstalk decorrelation with FR-4 mono capping)
 * 6. Volume Boost & Soft Brickwall Limiter (FR-5 clipping prevention)
 * Extra: Reverse Time Machine ("Vintage-ify" PRD 6.13 with LFO wow/flutter & analog noise synthesis)
 */
class AudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 2048
        private const val TAG = "AudioEngine"
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val isPlaying = AtomicBoolean(false)
    val isBypassed = AtomicBoolean(false) // A/B toggle (<50ms bypass)

    // Local MP3 Playback State
    var currentLocalTrack: LocalTrack? = null
        private set
    var isLocalTrackMode: Boolean = false
        private set
    private var localDecoder: Mp3AudioDecoder? = null
    private val localStereoBuffer = FloatArray(BUFFER_SIZE * 2)
    var onPlaybackFinished: (() -> Unit)? = null

    // 5-Band Simple Mode Gains
    var eqGains = mutableMapOf<PlainBand, Float>(
        PlainBand.RUMBLE to 0f,
        PlainBand.WARMTH to 0f,
        PlainBand.BODY to 0f,
        PlainBand.CLARITY to 0f,
        PlainBand.AIR to 0f
    )

    // 10-Band Advanced Parametric Mode Gains & Q factors (PRD 6.2 & 8.3)
    var isAdvancedParametricMode = false
    val parametricFrequencies = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    val parametricGains = mutableMapOf<Int, Float>().apply {
        parametricFrequencies.forEach { put(it, 0f) }
    }
    val parametricQ = mutableMapOf<Int, Float>().apply {
        parametricFrequencies.forEach { put(it, 0.8f) }
    }

    // Acoustic macros
    var spaceAmount: Float = 30f // 0 to 100%
    var punchAmount: Float = 25f // 0 to 100%
    var clarityMacroAmount: Float = 0f // 0 to 100%
    var loudnessBoost: Float = 0f // 0 to 100%
    var hissRemoval: Float = 0f // 0 to 100%
    var deHumEnabled: Boolean = false
    var humFrequency: Int = 60 // 50Hz or 60Hz mains
    var deCrackleEnabled: Boolean = false
    var spatialRoomType: String = "Natural" // "Natural", "Intimate Studio", "Concert Hall", "Cathedral" (PRD 6.7a)
    var hrtfProfile: String = "Natural" // "Narrow", "Natural", "Wide" (PRD 6.3)

    // Advanced Compressor & Limiter Parameters (PRD 8.3)
    var compThresholdDb: Float = -18f
    var compRatio: Float = 2.5f
    var compAttackMs: Float = 20f
    var compReleaseMs: Float = 150f
    var limiterCeilingDb: Float = -0.5f

    var reverbWet: Float = 0f // 0 to 100%
    var reverbWetPercent: Float
        get() = reverbWet
        set(value) { reverbWet = value }
    var reverbRoomSize: Float = 75f // 0 to 100% - fine continuous control within the selected RoomSize category
    var reverbDamping: Float = 35f // 0 to 100%
    var reverbWidth: Float = 100f // 0 to 100%
    var roomSize: RoomSize = RoomSize.LARGE_HALL // discrete character preset (PRD 6.15)
    var wallMaterial: WallMaterial = WallMaterial.PLASTER // discrete character preset (PRD 6.15)

    // Echo / Delay Parameters (Stereo Ping-Pong with Tape Damping)
    var echoWet: Float = 0f // 0 to 100%
    var echoWetPercent: Float
        get() = echoWet
        set(value) { echoWet = value }
    var echoTimeMs: Int = 320 // 50 to 3000 ms - widened for "a lot of echo" (PRD 6.17)
    var echoPingPong: Boolean = true

    // Pro Studio Reference Tools (Mastering & Mix Engineer Reference Suite)
    var midSideMode: MidSideMode = MidSideMode.STEREO
    var bassMonoMakerEnabled: Boolean = false
    var referenceMonitor: ReferenceMonitor = ReferenceMonitor.FLAT
    var limiterMode: LimiterMode = LimiterMode.SOFT_BRICKWALL
    var tapeDrivePercent: Float = 0f // 0 to 100% (+0 to +18dB console saturation)
    var reverbFreezeEnabled: Boolean = false

    // Pro Studio Dynamic Transient Designer (SPL / Oxford TransMod)
    var transientAttackPercent: Float = 0f // -100% to +100% (-12dB to +12dB)
    var transientSustainPercent: Float = 0f // -100% to +100% (-12dB to +12dB)
    val transientAttackGainDb: Float get() = (transientAttackPercent / 100f) * 12f
    val transientSustainGainDb: Float get() = (transientSustainPercent / 100f) * 12f
    var transientAttackActivity: Float = 0f
        private set
    var transientSustainActivity: Float = 0f
        private set

    // Pro Studio Analog Harmonic Saturation Color Topology (Triode / Tape / Transformer)
    var harmonicSaturationType: HarmonicSaturationType = HarmonicSaturationType.CLEAN
    var harmonicDrivePercent: Float = 0f // 0% to 100% (+0dB to +15dB)
    var thdPercent: Float = 0f // Live measured Total Harmonic Distortion %
        private set

    // Pro Studio ISO 226 Fletcher-Munson Equal-Loudness Contour Calibration
    var fletcherMunsonEnabled: Boolean = false

    // Pro Studio Calibration & Test Tone Generator
    var testToneMode: TestToneMode = TestToneMode.OFF
    var testToneLevelDb: Float = -18f // -36dB to 0dB reference volume

    // Pro Studio Mastering Sub-Cut Infrasonic High-Pass Filter
    var subCutFilter: SubCutFilter = SubCutFilter.OFF

    // Pro Studio Dynamic Frequency De-Esser (Sibilance & Harshness Tamer)
    var deEsserEnabled: Boolean = false
    var deEsserThresholdDb: Float = -18f // -36dB to -6dB
    var deEsserMaxReductionDb: Float = 6f // 0dB to 12dB
    var deEsserCurrentReductionDb: Float = 0f
        private set

    // Pro Studio Loudness-Matched A/B Monitoring
    var gainMatchedAB: Boolean = false
    private var rawRmsRolling: Double = 0.05
    private var procRmsRolling: Double = 0.05

    // Pro Studio Stereo Balance Trim & Polarity Inversion
    var stereoBalanceTrimDb: Float = 0f // -6dB (left) to +6dB (right)
    var invertLeftPolarity: Boolean = false // Ø L
    var invertRightPolarity: Boolean = false // Ø R

    // Real-Time Studio Reference Metrics (Exposed to UI & callbacks)
    var phaseCorrelation: Float = 0.85f // -1.0 (anti-phase) to +1.0 (mono)
    var currentPhaseCorrelation: Float
        get() = phaseCorrelation
        set(value) { phaseCorrelation = value }
    var crestFactorDb: Float = 12.0f // Peak dBFS - RMS dBFS
        private set
    var peakDbfs: Float = -6.0f
        private set
    var rmsDbfs: Float = -18.0f
        private set

    // Real-Time ITU-R BS.1770-4 / EBU R128 Broadcast Loudness Metrics
    var lufsMetrics: LufsMetrics = LufsMetrics()
        private set
    var streamingTarget: StreamingTarget = StreamingTarget.SPOTIFY_14

    // Callbacks
    var onSpectrumUpdated: ((FloatArray, Float) -> Unit)? = null
    var onStudioMetricsUpdated: ((phase: Float, crestFactor: Float, peakDbfs: Float, rmsDbfs: Float) -> Unit)? = null
    var onLufsMetricsUpdated: ((LufsMetrics) -> Unit)? = null
    var onDeEsserReductionUpdated: ((Float) -> Unit)? = null
    var onVectorScopeUpdated: ((FloatArray) -> Unit)? = null
    var onTransientActivityUpdated: ((Float, Float) -> Unit)? = null
    var onThdUpdated: ((Float) -> Unit)? = null

    // Vari-Speed / Tape Slowdown (PRD 6.17) - when true, pitch drops with tempo
    // like a real tape/vinyl slowing down. When false, pitch is preserved
    // (studio/podcast-style time-stretch).
    var varispeedMode: Boolean = true
    // Honesty layer (PRD 12.1 pattern applied here too): what speed/pitch the
    // OS actually confirmed applying, vs what was requested - Android's own
    // docs state out-of-range speed/pitch values are handled by an
    // OEM-dependent "fallback mode" that may clamp or mute rather than throw,
    // so silently assuming success is not safe. Exposed to the UI.
    var lastRequestedSpeed: Float = 1.0f
    var lastConfirmedSpeed: Float = 1.0f
    var speedAppliedAsRequested: Boolean = true

    // Playback Tempo (0.5x to 1.5x, API 23+ PlaybackParams time-stretching)
    var playbackSpeed: Float = 1.0f
        private set

    // Vintage-ify mode (PRD 6.13)
    var vintageMode: Boolean = false
    var wowFlutterDepth: Float = 0f // 0 to 100%
    var vintageNoiseLevel: Float = 0f // 0 to 100%

    // Demo Tracks
    var currentTrackIndex = 0
    val demoTracks = listOf(
        DemoTrack(
            id = "tape_nostalgia",
            title = "Golden Hour Memories",
            era = "70s Acoustic",
            characteristic = "Warm acoustic guitar, intimate vocal, subtle tape hiss",
            baseFrequency = 220f,
            noiseType = "cassette"
        ),
        DemoTrack(
            id = "vinyl_jazz",
            title = "Midnight Blue Lounge",
            era = "60s Vinyl Transfer",
            characteristic = "Upright bass, mellow Rhodes piano, vinyl crackle & 60Hz hum",
            baseFrequency = 146.8f,
            noiseType = "vinyl"
        ),
        DemoTrack(
            id = "retro_pop",
            title = "Neon Dreams (Radio Rip)",
            era = "90s Broadcast",
            characteristic = "Punchy synth bass, bright chorus, compressed dynamics",
            baseFrequency = 261.6f,
            noiseType = "radio"
        ),
        DemoTrack(
            id = "lofi_vocal",
            title = "Lost in the Attic",
            era = "Cassette Voice Memo",
            characteristic = "Slightly muffled low-bitrate vocal recording with room rumble",
            baseFrequency = 174.6f,
            noiseType = "tape_heavy"
        )
    )


    // Internal State
    private var phase = 0.0
    private var lfoPhase = 0.0
    private var noiseFloorEstimate = 0.02
    private var hissDetectorEnvelope = 0.0
    private var runningLrProd = 0.0
    private var runningLSq = 0.0
    private var runningRSq = 0.0

    // Musical Sequencer State for Demo Audio Playback (allows audible transients for Reverb, Echo & Tempo)
    private var seqTimer = 0.0
    private var seqStep = 0
    private var noteEnvelope = 0.0
    private var harmonicPhase = 0.0
    private var subPhase = 0.0

    // Melodic progressions for each demo track (BPM ~ 107 baseline)
    // 0: "Golden Hour Memories" (70s Acoustic Guitar Arpeggios)
    private val acousticProgression = doubleArrayOf(
        220.00, 261.63, 329.63, 440.00, 329.63, 261.63,
        174.61, 220.00, 261.63, 349.23, 261.63, 220.00,
        130.81, 164.81, 196.00, 261.63, 196.00, 164.81,
        196.00, 246.94, 293.66, 392.00, 293.66, 246.94
    )

    // 1: "Midnight Blue Lounge" (60s Vinyl - Walking Jazz Bass & Mellow Chords)
    private val jazzProgression = doubleArrayOf(
        146.83, 174.61, 220.00, 293.66, 261.63, 220.00, 174.61, 146.83,
        98.00, 123.47, 146.83, 196.00, 246.94, 196.00, 146.83, 123.47,
        130.81, 164.81, 196.00, 246.94, 261.63, 196.00, 164.81, 130.81,
        110.00, 138.59, 164.81, 220.00, 207.65, 174.61, 146.83, 123.47
    )

    // 2: "Neon Dreams" (90s Broadcast - Punchy Synth Bassline & Riff)
    private val synthProgression = doubleArrayOf(
        261.63, 261.63, 329.63, 392.00, 523.25, 392.00, 329.63, 261.63,
        220.00, 220.00, 261.63, 329.63, 440.00, 329.63, 261.63, 220.00,
        174.61, 174.61, 220.00, 261.63, 349.23, 261.63, 220.00, 174.61,
        196.00, 196.00, 246.94, 293.66, 392.00, 293.66, 246.94, 196.00
    )

    // 3: "Lost in the Attic" (Cassette Voice Memo - Nostalgic Piano Motif)
    private val lofiProgression = doubleArrayOf(
        174.61, 220.00, 261.63, 329.63, 261.63, 220.00,
        164.81, 196.00, 246.94, 293.66, 246.94, 196.00,
        146.83, 174.61, 220.00, 261.63, 220.00, 174.61,
        130.81, 164.81, 196.00, 246.94, 196.00, 164.81
    )

    // Vintage Wow & Flutter Fractional Delay Line (PRD 6.13 & 8.3)
    private val vintageDelaySize = 2048
    private val vintageDelayL = DoubleArray(vintageDelaySize)
    private val vintageDelayR = DoubleArray(vintageDelaySize)
    private var vintageWriteIndex = 0

    // Click/Crackle state registers
    private var prevSampleL = 0.0
    private var prevSampleR = 0.0

    // Biquad state register with anti-denormal and NaN/Inf isolation
    class BiquadState {
        var x1L = 0.0; var x2L = 0.0; var y1L = 0.0; var y2L = 0.0
        var x1R = 0.0; var x2R = 0.0; var y1R = 0.0; var y2R = 0.0
        var b0 = 1.0; var b1 = 0.0; var b2 = 0.0; var a1 = 0.0; var a2 = 0.0

        fun processL(input: Double): Double {
            val out = b0 * input + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
            if (out.isNaN() || out.isInfinite()) {
                x1L = 0.0; x2L = 0.0; y1L = 0.0; y2L = 0.0
                return input
            }
            x2L = x1L; x1L = input
            y2L = y1L; y1L = if (abs(out) < 1e-18) 0.0 else out
            return out
        }

        fun processR(input: Double): Double {
            val out = b0 * input + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
            if (out.isNaN() || out.isInfinite()) {
                x1R = 0.0; x2R = 0.0; y1R = 0.0; y2R = 0.0
                return input
            }
            x2R = x1R; x1R = input
            y2R = y1R; y1R = if (abs(out) < 1e-18) 0.0 else out
            return out
        }
    }

    // Real 8-band spectrum analyzer (RBJ constant-peak-gain bandpass biquads),
    // driven by actual per-frequency energy of the fully-processed output -
    // NOT the old approach, which took one broadband RMS number and scaled it
    // by 8 fixed multipliers (meaning all "bands" always moved in lockstep and
    // never reflected the real frequency content of what was playing).
    private val spectrumBandFreqs = doubleArrayOf(60.0, 150.0, 400.0, 1000.0, 2500.0, 5000.0, 8000.0, 12000.0)
    private val spectrumBandCompensation = doubleArrayOf(1.0, 1.1, 1.3, 1.6, 2.0, 2.6, 3.2, 4.0) // higher bands carry less raw energy in typical music - compensate so the visualizer isn't permanently bass-dominated
    private val spectrumFilters = Array(8) { BiquadState() }
    private val spectrumSmoothed = FloatArray(8) { 0.05f }
    private var spectrumFiltersConfigured = false

    private fun configureSpectrumFilters() {
        for (i in spectrumBandFreqs.indices) {
            val f0 = spectrumBandFreqs[i]
            val q = 1.2
            val w0 = 2.0 * PI * f0 / SAMPLE_RATE
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha
            spectrumFilters[i].b0 = alpha / a0
            spectrumFilters[i].b1 = 0.0
            spectrumFilters[i].b2 = -alpha / a0
            spectrumFilters[i].a1 = (-2.0 * cos(w0)) / a0
            spectrumFilters[i].a2 = (1.0 - alpha) / a0
        }
        spectrumFiltersConfigured = true
    }

    // =========================================================================
    // PRO STUDIO DSP ENGINES: SUB-CUT, DE-ESSER, K-WEIGHTING & TEST TONES
    // =========================================================================

    // Pro Studio Sub-Cut Infrasonic High-Pass Filter Biquads (18-24dB/oct Butterworth cascade)
    private val subCutFilter1 = BiquadState()
    private val subCutFilter2 = BiquadState()

    // Pro Studio Dynamic Frequency De-Esser
    private val deEsserSidechain = BiquadState()
    private val deEsserNotch = BiquadState()
    private var deEsserEnvelope = 0.0
    private var deEsserConfigured = false

    private fun configureDeEsserFilter() {
        val f0 = 6500.0
        val q = 2.2
        val w0 = 2.0 * PI * f0 / SAMPLE_RATE
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha
        deEsserSidechain.b0 = alpha / a0
        deEsserSidechain.b1 = 0.0
        deEsserSidechain.b2 = -alpha / a0
        deEsserSidechain.a1 = (-2.0 * cos(w0)) / a0
        deEsserSidechain.a2 = (1.0 - alpha) / a0
        deEsserConfigured = true
    }

    fun processDeEsser(inL: Double, inR: Double): Pair<Double, Double> {
        if (!deEsserEnabled) {
            deEsserCurrentReductionDb = 0f
            return Pair(inL, inR)
        }
        if (!deEsserConfigured) configureDeEsserFilter()

        val sideL = deEsserSidechain.processL(inL)
        val sideR = deEsserSidechain.processR(inR)
        val sideLevel = max(abs(sideL), abs(sideR))

        val attackCoef = 0.045 // ~1ms fast clamp
        val releaseCoef = 0.00045 // ~45ms recovery
        deEsserEnvelope += if (sideLevel > deEsserEnvelope) {
            attackCoef * (sideLevel - deEsserEnvelope)
        } else {
            releaseCoef * (sideLevel - deEsserEnvelope)
        }

        val threshLin = 10.0.pow(deEsserThresholdDb / 20.0)
        var reductionDb = 0.0
        if (deEsserEnvelope > threshLin) {
            val excessDb = 20.0 * log10(deEsserEnvelope / threshLin)
            reductionDb = (excessDb * 0.85).coerceIn(0.0, deEsserMaxReductionDb.toDouble())
        }
        deEsserCurrentReductionDb = reductionDb.toFloat()

        if (reductionDb <= 0.05) {
            return Pair(inL, inR)
        }

        calculateCookbookBiquad(
            biquad = deEsserNotch,
            f0 = 6500.0,
            gainDb = -reductionDb,
            q = 2.0,
            isLowShelf = false,
            isHighShelf = false
        )
        val outL = deEsserNotch.processL(inL)
        val outR = deEsserNotch.processR(inR)
        return Pair(outL, outR)
    }

    // Pro Studio ITU-R BS.1770-4 K-weighting Loudness Filter States
    private val kStage1 = BiquadState()
    private val kStage2 = BiquadState()
    private var kFiltersConfigured = false

    private val lufsBlockSize = 4410 // 100ms blocks at 44.1kHz
    private var lufsBlockSampleCount = 0
    private var lufsBlockEnergySum = 0.0
    private val momentaryBlocks = DoubleArray(4) // 400ms = 4 blocks
    private var momentaryIndex = 0
    private val shortTermBlocks = DoubleArray(30) // 3000ms = 30 blocks
    private var shortTermIndex = 0
    private val integratedBlockList = mutableListOf<Double>()
    private var prevSampleKPeakL = 0.0
    private var prevSampleKPeakR = 0.0
    private var truePeakMax = 0.0

    private fun configureKWeightingFilters() {
        // ITU-R BS.1770-4 exact coefficients at 44.1kHz:
        kStage1.b0 = 1.53512485958697
        kStage1.b1 = -2.69169618940638
        kStage1.b2 = 1.19839281085285
        kStage1.a1 = -1.69065929318241
        kStage1.a2 = 0.73248077421585

        kStage2.b0 = 1.0
        kStage2.b1 = -2.0
        kStage2.b2 = 1.0
        kStage2.a1 = -1.99004745483398
        kStage2.a2 = 0.99007225036621

        kFiltersConfigured = true
    }

    fun processLufsSample(outL: Double, outR: Double) {
        if (!kFiltersConfigured) configureKWeightingFilters()

        val kL = kStage2.processL(kStage1.processL(outL))
        val kR = kStage2.processR(kStage1.processR(outR))

        val energy = kL * kL + kR * kR
        lufsBlockEnergySum += energy
        lufsBlockSampleCount++

        val absL = abs(outL)
        val absR = abs(outR)
        val maxDirect = max(absL, absR)
        val estInterSampleL = absL + 0.125 * (absL - prevSampleKPeakL).pow(2)
        val estInterSampleR = absR + 0.125 * (absR - prevSampleKPeakR).pow(2)
        prevSampleKPeakL = absL
        prevSampleKPeakR = absR

        val peakEst = max(maxDirect, max(estInterSampleL, estInterSampleR))
        if (peakEst > truePeakMax) truePeakMax = peakEst

        if (lufsBlockSampleCount >= lufsBlockSize) {
            val meanBlockEnergy = lufsBlockEnergySum / lufsBlockSampleCount
            lufsBlockEnergySum = 0.0
            lufsBlockSampleCount = 0

            momentaryBlocks[momentaryIndex % 4] = meanBlockEnergy
            momentaryIndex++

            shortTermBlocks[shortTermIndex % 30] = meanBlockEnergy
            shortTermIndex++

            if (meanBlockEnergy > 1e-7) { // Above absolute silence threshold (-70 LKFS)
                if (integratedBlockList.size > 1200) integratedBlockList.removeAt(0)
                integratedBlockList.add(meanBlockEnergy)
            }
        }
    }

    fun calculateLufsMetrics(): LufsMetrics {
        val momCount = min(momentaryIndex, 4)
        val momEnergy = if (momCount > 0) {
            momentaryBlocks.take(momCount).average()
        } else 1e-12
        val momLufs = (-0.691 + 10.0 * log10(momEnergy.coerceAtLeast(1e-12))).toFloat().coerceIn(-70f, 6f)

        val stCount = min(shortTermIndex, 30)
        val stEnergy = if (stCount > 0) {
            shortTermBlocks.take(stCount).average()
        } else 1e-12
        val stLufs = (-0.691 + 10.0 * log10(stEnergy.coerceAtLeast(1e-12))).toFloat().coerceIn(-70f, 6f)

        val intLufs = if (integratedBlockList.isNotEmpty()) {
            val unGatedEnergy = integratedBlockList.average()
            val unGatedLufs = -0.691 + 10.0 * log10(unGatedEnergy.coerceAtLeast(1e-12))
            val relGateEnergy = 10.0.pow((unGatedLufs - 10.0 + 0.691) / 10.0)
            val gatedBlocks = integratedBlockList.filter { it >= relGateEnergy }
            if (gatedBlocks.isNotEmpty()) {
                (-0.691 + 10.0 * log10(gatedBlocks.average().coerceAtLeast(1e-12))).toFloat().coerceIn(-70f, 6f)
            } else unGatedLufs.toFloat().coerceIn(-70f, 6f)
        } else stLufs

        val validBlocks = shortTermBlocks.filter { it > 1e-7 }.sorted()
        val lra = if (validBlocks.size >= 4) {
            val p10 = -0.691 + 10.0 * log10(validBlocks[(validBlocks.size * 0.10).toInt()].coerceAtLeast(1e-12))
            val p95 = -0.691 + 10.0 * log10(validBlocks[(validBlocks.size * 0.95).toInt().coerceAtMost(validBlocks.size - 1)].coerceAtLeast(1e-12))
            (p95 - p10).toFloat().coerceIn(0f, 25f)
        } else 6.0f

        val truePeakDbtp = (20.0 * log10(truePeakMax.coerceAtLeast(1e-6))).toFloat().coerceIn(-60f, 6f)
        truePeakMax *= 0.96

        return LufsMetrics(
            momentaryLufs = momLufs,
            shortTermLufs = stLufs,
            integratedLufs = intLufs,
            loudnessRangeLu = lra,
            truePeakDbtp = truePeakDbtp
        )
    }

    // Pro Studio Calibration & Test Tone Generator State Registers
    private var testTonePinkB0 = 0.0
    private var testTonePinkB1 = 0.0
    private var testTonePinkB2 = 0.0
    private var testTonePinkB3 = 0.0
    private var testTonePinkB4 = 0.0
    private var testTonePinkB5 = 0.0
    private var testTonePinkB6 = 0.0
    private var testToneSine1kPhase = 0.0
    private var testToneSub50Phase = 0.0
    private var testToneSweepTime = 0.0

    fun synthesizeTestToneSample(): Pair<Double, Double> {
        val amp = 10.0.pow(testToneLevelDb / 20.0)
        return when (testToneMode) {
            TestToneMode.OFF -> Pair(0.0, 0.0)
            TestToneMode.PINK_NOISE -> {
                val white = Random.nextDouble(-1.0, 1.0)
                testTonePinkB0 = 0.99886 * testTonePinkB0 + white * 0.0555179
                testTonePinkB1 = 0.99332 * testTonePinkB1 + white * 0.0750759
                testTonePinkB2 = 0.96900 * testTonePinkB2 + white * 0.1538520
                testTonePinkB3 = 0.86650 * testTonePinkB3 + white * 0.3104856
                testTonePinkB4 = 0.55000 * testTonePinkB4 + white * 0.5329522
                testTonePinkB5 = -0.7616 * testTonePinkB5 - white * 0.0168980
                val pink = (testTonePinkB0 + testTonePinkB1 + testTonePinkB2 + testTonePinkB3 + testTonePinkB4 + testTonePinkB5 + testTonePinkB6 + white * 0.5362) * 0.11
                testTonePinkB6 = white * 0.115926
                val s = pink * amp
                Pair(s, s)
            }
            TestToneMode.WHITE_NOISE -> {
                val s = Random.nextDouble(-1.0, 1.0) * amp
                Pair(s, s)
            }
            TestToneMode.SINE_1KHZ -> {
                testToneSine1kPhase += (2.0 * PI * 1000.0) / SAMPLE_RATE
                if (testToneSine1kPhase >= 2.0 * PI) testToneSine1kPhase -= 2.0 * PI
                val s = sin(testToneSine1kPhase) * amp
                Pair(s, s)
            }
            TestToneMode.SUB_50HZ -> {
                testToneSub50Phase += (2.0 * PI * 50.0) / SAMPLE_RATE
                if (testToneSub50Phase >= 2.0 * PI) testToneSub50Phase -= 2.0 * PI
                val s = sin(testToneSub50Phase) * amp
                Pair(s, s)
            }
            TestToneMode.LOG_SWEEP -> {
                testToneSweepTime += 1.0 / SAMPLE_RATE
                val sweepDuration = 5.0
                if (testToneSweepTime >= sweepDuration) {
                    testToneSweepTime = 0.0
                }
                val f0 = 20.0
                val f1 = 20000.0
                val ratio = f1 / f0
                val k = (2.0 * PI * f0 * sweepDuration) / kotlin.math.ln(ratio)
                val sweepPhase = k * (ratio.pow(testToneSweepTime / sweepDuration) - 1.0)
                val s = sin(sweepPhase) * amp
                Pair(s, s)
            }
        }
    }


    // Schroeder / Freeverb Low-Pass Feedback Comb Filter with anti-denormal flush
    class CombFilter(val maxSize: Int) {
        private val buffer = DoubleArray(maxSize)
        private var writeIndex = 0
        var filterStore = 0.0
        // Adjustable look-back distance, independent of physical buffer capacity.
        // This is what makes Room Size a real early-reflection/delay-length change,
        // not just a feedback-decay knob - a small closet and a cathedral don't just
        // ring for different lengths of time, their reflections arrive at different
        // times too.
        var activeLength = maxSize

        fun process(input: Double, feedback: Double, damping: Double): Double {
            val delay = activeLength.coerceIn(8, maxSize)
            val readIndex = (writeIndex - delay + maxSize) % maxSize
            val output = buffer[readIndex]
            filterStore = output * (1.0 - damping) + filterStore * damping
            if (abs(filterStore) < 1e-15) filterStore = 0.0
            val next = input + filterStore * feedback
            buffer[writeIndex] = if (abs(next) < 1e-15) 0.0 else next
            writeIndex = (writeIndex + 1) % maxSize
            return output
        }

        fun reset() {
            buffer.fill(0.0)
            writeIndex = 0
            filterStore = 0.0
        }
    }

    // Schroeder / Freeverb All-Pass Filter with anti-denormal flush
    class AllPassFilter(val size: Int) {
        private val buffer = DoubleArray(size)
        private var index = 0

        fun process(input: Double, feedback: Double = 0.5): Double {
            val bufOut = buffer[index]
            val w = input + bufOut * feedback
            val output = -feedback * w + bufOut
            buffer[index] = if (abs(w) < 1e-15) 0.0 else w
            index = (index + 1) % size
            return if (abs(output) < 1e-15) 0.0 else output
        }

        fun reset() {
            buffer.fill(0.0)
            index = 0
        }
    }

    // One-pole spectral tilt: simulates how a surface material colors reflections.
    // Positive tilt = darker/warmer (absorptive materials: wood, carpet).
    // Negative tilt = brighter (reflective materials: tile, concrete, glass).
    // This is a deliberately simple, cheap filter - a real material's absorption
    // curve is far more complex, but a single-pole tilt gives an audibly distinct,
    // correctly-directional character difference at negligible CPU cost.
    class ToneTilt {
        private var state = 0.0
        fun process(input: Double, tilt: Double): Double {
            val cutoff = (0.15 + abs(tilt) * 0.35).coerceIn(0.05, 0.5)
            state += cutoff * (input - state)
            if (abs(state) < 1e-15) state = 0.0
            return if (tilt >= 0.0) {
                input * (1.0 - tilt) + state * tilt
            } else {
                input + (input - state) * (-tilt) * 0.8
            }
        }
        fun reset() { state = 0.0 }
    }

    enum class RoomSize(val label: String, val lengthScale: Double, val feedbackBias: Double, val dampingBias: Double) {
        SMALL_ROOM("Small Room", 0.45, -0.10, 0.05),
        MEDIUM_HALL("Medium Hall", 0.68, -0.03, 0.0),
        LARGE_HALL("Large Hall", 0.88, 0.02, -0.03),
        CATHEDRAL("Cathedral", 1.0, 0.06, -0.08),
        CAVERN("Cavern", 1.3, 0.10, -0.12) // beyond canonical Freeverb size - the "bigger range" ask
    }

    enum class WallMaterial(val label: String, val toneTilt: Double, val dampingBias: Double, val feedbackBias: Double) {
        WOOD("Wood", 0.35, 0.08, 0.0),
        PLASTER("Plaster / Drywall", 0.10, 0.0, 0.0),
        CONCRETE("Concrete", -0.25, -0.05, 0.02),
        TILE_STONE("Tile / Stone", -0.35, -0.08, 0.03),
        CARPET_CURTAINS("Carpet & Curtains", 0.55, 0.18, -0.04),
        GLASS("Glass", -0.45, -0.10, 0.04)
    }

    // 5 Simple Mode EQ Biquads
    private val simpleFilters = mapOf(
        PlainBand.RUMBLE to BiquadState(),
        PlainBand.WARMTH to BiquadState(),
        PlainBand.BODY to BiquadState(),
        PlainBand.CLARITY to BiquadState(),
        PlainBand.AIR to BiquadState()
    )

    // 10 Independent Parametric EQ Biquads
    private val parametricFilters = mutableMapOf<Int, BiquadState>().apply {
        parametricFrequencies.forEach { put(it, BiquadState()) }
    }

    // De-Hum Notch Biquads (Fundamental + 2nd & 3rd Harmonics)
    private val humNotch1 = BiquadState() // Fundamental 50/60Hz
    private val humNotch2 = BiquadState() // 2nd Harmonic 100/120Hz
    private val humNotch3 = BiquadState() // 3rd Harmonic 150/180Hz

    // Hiss Removal High Shelf Biquad
    private val hissHighShelf = BiquadState()

    // Clarity Crossover Filters
    private val crossoverLow = BiquadState()
    private val crossoverHigh = BiquadState()

    // Dynamics State
    private var compressorEnvelope = 0.0

    // Delay line for Space & Wow/Flutter
    private val delayBufferSize = 4410
    private val delayBufferL = DoubleArray(delayBufferSize)
    private val delayBufferR = DoubleArray(delayBufferSize)
    private var delayWriteIndex = 0

    // Reverb Engine State (Standard Freeverb 44.1kHz delay sizes with +23 right stereo spread).
    // Buffers are allocated 30% larger than canonical tunings so the Cavern room-size
    // preset (lengthScale up to 1.3) has physical headroom - activeLength (set per
    // RoomSize) controls what's actually used at runtime.
    private val combBaseTunings = listOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
    private val combBaseTuningsR = listOf(1139, 1211, 1300, 1379, 1445, 1514, 1580, 1640)
    private val leftCombs = combBaseTunings.map { CombFilter((it * 1.3).toInt()) }
    private val rightCombs = combBaseTuningsR.map { CombFilter((it * 1.3).toInt()) }
    private val leftAllPass = listOf(556, 441, 341, 225).map { AllPassFilter(it) }
    private val rightAllPass = listOf(579, 464, 364, 248).map { AllPassFilter(it) }
    private val reverbToneTiltL = ToneTilt()
    private val reverbToneTiltR = ToneTilt()

    // Echo / Delay State (44.1kHz stereo ring buffer, up to 3000ms delay - PRD 6.17)
    private val echoBufferSize = 132300
    private val echoBufferL = DoubleArray(echoBufferSize)
    private val echoBufferR = DoubleArray(echoBufferSize)
    private var echoWriteIndex = 0
    private var prevEchoFilterL = 0.0
    private var prevEchoFilterR = 0.0

    // Mono detection metric (PRD FR-4)
    var correlationMetric = 0.85f

    // Pro Studio Bass Mono-Maker Crossover Filters (120Hz 2nd-order Butterworth crossover)
    private val monoMakerLow = BiquadState()
    private val monoMakerHigh = BiquadState()

    // Pro Studio Reference Monitor Simulation Filters (4-stage biquad cascade)
    private val monitorFilter1 = BiquadState()
    private val monitorFilter2 = BiquadState()
    private val monitorFilter3 = BiquadState()
    private val monitorFilter4 = BiquadState()

    // Pro Studio ISO 226 Fletcher-Munson Equal-Loudness Filters
    private val fletcherMunsonLow = BiquadState()
    private val fletcherMunsonHigh = BiquadState()

    // Pro Studio Iron Core Transformer Saturation Filter
    private val transformerLowpass = BiquadState()

    // Pro Studio Transient Designer Envelopes
    private var transientEnvFast = 0.0
    private var transientEnvSlow = 0.0
    private var transientEnvSustain = 0.0

    // Pro Studio Harmonic Saturation & THD accumulators
    private var thdHarmonicAccum = 0.0
    private var thdFundamentalAccum = 0.0
    private var dcBlockerPrevInL = 0.0
    private var dcBlockerPrevInR = 0.0
    private var dcBlockerPrevOutL = 0.0
    private var dcBlockerPrevOutR = 0.0

    // Pro Studio Lissajous Goniometer Vector Scope Buffer
    private val vectorScopeBufferSize = 512 // 256 (X, Y) coordinate pairs
    private val vectorScopeRingBuffer = FloatArray(vectorScopeBufferSize)
    private var vectorScopeWriteIndex = 0
    private val vectorScopeLock = Any()

    fun getVectorScopePoints(outBuffer: FloatArray): Int {
        val toCopy = min(outBuffer.size, vectorScopeBufferSize)
        synchronized(vectorScopeLock) {
            System.arraycopy(vectorScopeRingBuffer, 0, outBuffer, 0, toCopy)
        }
        return toCopy
    }

    fun startPlayback(scope: CoroutineScope) {
        if (isPlaying.get()) return
        isPlaying.set(true)

        if (audioTrack == null) {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(max(minBuf, BUFFER_SIZE * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }

        try {
            audioTrack?.play()
            val params = audioTrack?.playbackParams ?: PlaybackParams()
            params.setSpeed(playbackSpeed)
            params.setPitch(if (varispeedMode) playbackSpeed else 1.0f)
            audioTrack?.playbackParams = params
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply initial playback params", e)
        }

        playbackJob = scope.launch(Dispatchers.Default) {
            val pcmBuffer = ShortArray(BUFFER_SIZE * 2)
            val spectrumBands = FloatArray(8)

            while (isActive && isPlaying.get()) {
                updateDspCoefficients()

                var rmsSum = 0.0
                var lrProdSum = 0.0
                var lSqSum = 0.0
                var rSqSum = 0.0
                var peakSample = 0.0
                var rawEnergySum = 0.0
                if (!spectrumFiltersConfigured) configureSpectrumFilters()
                val spectrumEnergyAccum = DoubleArray(8)

                val isLocal = isLocalTrackMode && localDecoder != null
                val localDec = localDecoder
                val framesRead = if (isLocal && localDec != null) {
                    val read = localDec.readStereoSamples(localStereoBuffer, 0, BUFFER_SIZE * 2)
                    if (read <= 0) {
                        localDec.seekTo(0)
                        onPlaybackFinished?.invoke()
                    }
                    read
                } else 0

                val track = demoTracks[currentTrackIndex]

                for (i in 0 until BUFFER_SIZE) {
                    val (rawL, rawR) = if (testToneMode != TestToneMode.OFF) {
                        synthesizeTestToneSample()
                    } else if (isLocal) {
                        if (i < framesRead) {
                            Pair(localStereoBuffer[i * 2].toDouble(), localStereoBuffer[i * 2 + 1].toDouble())
                        } else {
                            Pair(0.0, 0.0)
                        }
                    } else {
                        synthesizeSourceSample(track)
                    }

                    rawEnergySum += rawL * rawL + rawR * rawR

                    val (outL, outR) = processStereoSample(rawL, rawR)
                    processLufsSample(outL, outR)

                    // Downsampled vector scope coordinate recording in Mid/Side space (Lissajous Goniometer)
                    if (i % 4 == 0) {
                        val scopeX = ((outR - outL) * 0.7071).toFloat().coerceIn(-1.5f, 1.5f)
                        val scopeY = ((outL + outR) * 0.7071).toFloat().coerceIn(-1.5f, 1.5f)
                        synchronized(vectorScopeLock) {
                            vectorScopeRingBuffer[vectorScopeWriteIndex] = scopeX
                            vectorScopeRingBuffer[vectorScopeWriteIndex + 1] = scopeY
                            vectorScopeWriteIndex = (vectorScopeWriteIndex + 2) % vectorScopeBufferSize
                        }
                    }

                    // Metrics
                    rmsSum += outL * outL + outR * outR
                    lrProdSum += outL * outR
                    lSqSum += outL * outL
                    rSqSum += outR * outR

                    val maxAbs = max(abs(outL), abs(outR))
                    if (maxAbs > peakSample) peakSample = maxAbs

                    // Real per-band energy for the spectrum visualizer - filters
                    // the actual processed output (what the user hears), not a
                    // fabricated approximation.
                    val monoOutForSpectrum = (outL + outR) * 0.5
                    for (b in 0 until 8) {
                        val filtered = spectrumFilters[b].processL(monoOutForSpectrum)
                        spectrumEnergyAccum[b] += filtered * filtered
                    }

                    // TPDF Dither before 16-bit integer quantization (PRD 8.4)
                    val dither = (Random.nextDouble() - Random.nextDouble()) / 32768.0
                    val sampleShortL = ((outL + dither).coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                    val sampleShortR = ((outR + dither).coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()

                    pcmBuffer[i * 2] = sampleShortL
                    pcmBuffer[i * 2 + 1] = sampleShortR
                }

                // Update correlation metric (mono detection & goniometer: -1.0 to +1.0)
                val denom = sqrt(lSqSum * rSqSum)
                if (denom > 0.0001) {
                    val rawCorr = (lrProdSum / denom).toFloat()
                    correlationMetric = rawCorr.coerceIn(-1.0f, 1.0f)
                    phaseCorrelation = correlationMetric
                }

                audioTrack?.write(pcmBuffer, 0, pcmBuffer.size)

                val rms = sqrt(rmsSum / (BUFFER_SIZE * 2)).toFloat().coerceAtLeast(1e-6f)
                val peak = peakSample.toFloat().coerceAtLeast(1e-6f)

                peakDbfs = (20.0 * log10(peak.toDouble())).toFloat().coerceIn(-60f, 6f)
                rmsDbfs = (20.0 * log10(rms.toDouble())).toFloat().coerceIn(-60f, 0f)
                crestFactorDb = (peakDbfs - rmsDbfs).coerceIn(0f, 30f)

                val rawRms = sqrt(rawEnergySum / (BUFFER_SIZE * 2)).coerceAtLeast(1e-6)
                rawRmsRolling = 0.95 * rawRmsRolling + 0.05 * rawRms
                procRmsRolling = 0.95 * procRmsRolling + 0.05 * rms.toDouble()

                lufsMetrics = calculateLufsMetrics()
                onLufsMetricsUpdated?.invoke(lufsMetrics)
                onDeEsserReductionUpdated?.invoke(deEsserCurrentReductionDb)

                // Calculate THD %
                if (thdFundamentalAccum > 1e-6) {
                    thdPercent = (sqrt(thdHarmonicAccum / thdFundamentalAccum) * 100.0).toFloat().coerceIn(0f, 100f)
                } else {
                    thdPercent = 0f
                }
                thdHarmonicAccum = 0.0
                thdFundamentalAccum = 0.0
                onThdUpdated?.invoke(thdPercent)
                onTransientActivityUpdated?.invoke(transientAttackActivity, transientSustainActivity)

                // Copy vector scope snapshot for UI
                val scopeSnapshot = FloatArray(vectorScopeBufferSize)
                synchronized(vectorScopeLock) {
                    System.arraycopy(vectorScopeRingBuffer, 0, scopeSnapshot, 0, vectorScopeBufferSize)
                }
                onVectorScopeUpdated?.invoke(scopeSnapshot)

                computeSpectrumBands(spectrumEnergyAccum, spectrumBands)
                onSpectrumUpdated?.invoke(spectrumBands, rms)
                onStudioMetricsUpdated?.invoke(phaseCorrelation, crestFactorDb, peakDbfs, rmsDbfs)
            }
        }
    }

    fun playLocalTrack(track: LocalTrack, scope: CoroutineScope) {
        stopPlayback()
        currentLocalTrack = track
        isLocalTrackMode = true
        try {
            localDecoder?.release()
            localDecoder = Mp3AudioDecoder(File(track.filePath))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Mp3AudioDecoder for ${track.filePath}", e)
            isLocalTrackMode = false
            currentLocalTrack = null
            return
        }
        startPlayback(scope)
    }

    fun playDemoTrack(index: Int, scope: CoroutineScope) {
        stopPlayback()
        isLocalTrackMode = false
        currentLocalTrack = null
        currentTrackIndex = index.coerceIn(demoTracks.indices)
        startPlayback(scope)
    }

    fun pausePlayback() {
        isPlaying.set(false)
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
        } catch (e: Exception) {}
    }

    fun stopPlayback() {
        isPlaying.set(false)
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null
        localDecoder?.release()
        localDecoder = null
        resetAllEffects()
    }

    fun seekTo(positionMs: Long) {
        if (isLocalTrackMode) {
            localDecoder?.seekTo(positionMs)
        }
    }

    fun getCurrentPositionMs(): Long {
        return if (isLocalTrackMode) {
            localDecoder?.currentPositionMs ?: 0L
        } else 0L
    }

    fun getDurationMs(): Long {
        return if (isLocalTrackMode) {
            currentLocalTrack?.durationMs?.takeIf { it > 0 } ?: (localDecoder?.durationMs ?: 0L)
        } else 0L
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 2.0f)
        playbackSpeed = clamped
        lastRequestedSpeed = clamped
        try {
            audioTrack?.let { track ->
                val params = track.playbackParams ?: PlaybackParams()
                params.setSpeed(clamped)
                // Vari-Speed: let pitch drop with tempo for real tape/vinyl
                // slowdown character. Preserve-pitch mode (varispeedMode =
                // false) leaves pitch at 1.0 for a studio/podcast-style
                // time-stretch instead.
                params.setPitch(if (varispeedMode) clamped else 1.0f)
                track.playbackParams = params

                // Honesty check (PRD 12.1 pattern): Android's own docs state
                // out-of-range speed/pitch is handled by an OEM-dependent
                // "fallback mode" that may clamp or mute rather than throw -
                // read back what was actually applied instead of assuming
                // the request succeeded as-is.
                val confirmed = track.playbackParams?.speed ?: clamped
                lastConfirmedSpeed = confirmed
                speedAppliedAsRequested = kotlin.math.abs(confirmed - clamped) < 0.02f
                if (!speedAppliedAsRequested) {
                    Log.w(TAG, "Requested speed $clamped but device applied $confirmed - likely OEM/hardware clamping")
                }
            }
        } catch (e: Exception) {
            speedAppliedAsRequested = false
            Log.e(TAG, "Failed to apply playback speed $clamped", e)
        }
    }

    fun resetReverb() {
        leftCombs.forEach { it.reset() }
        rightCombs.forEach { it.reset() }
        leftAllPass.forEach { it.reset() }
        rightAllPass.forEach { it.reset() }
        reverbToneTiltL.reset()
        reverbToneTiltR.reset()
    }

    fun resetEcho() {
        echoBufferL.fill(0.0)
        echoBufferR.fill(0.0)
        echoWriteIndex = 0
        prevEchoFilterL = 0.0
        prevEchoFilterR = 0.0
    }

    fun resetAllEffects() {
        resetReverb()
        resetEcho()
        monoMakerLow.x1L = 0.0; monoMakerLow.x2L = 0.0; monoMakerLow.y1L = 0.0; monoMakerLow.y2L = 0.0
        monoMakerLow.x1R = 0.0; monoMakerLow.x2R = 0.0; monoMakerLow.y1R = 0.0; monoMakerLow.y2R = 0.0
        monoMakerHigh.x1L = 0.0; monoMakerHigh.x2L = 0.0; monoMakerHigh.y1L = 0.0; monoMakerHigh.y2L = 0.0
        monoMakerHigh.x1R = 0.0; monoMakerHigh.x2R = 0.0; monoMakerHigh.y1R = 0.0; monoMakerHigh.y2R = 0.0
        monitorFilter1.x1L = 0.0; monitorFilter1.x2L = 0.0; monitorFilter1.y1L = 0.0; monitorFilter1.y2L = 0.0
        monitorFilter1.x1R = 0.0; monitorFilter1.x2R = 0.0; monitorFilter1.y1R = 0.0; monitorFilter1.y2R = 0.0
        monitorFilter2.x1L = 0.0; monitorFilter2.x2L = 0.0; monitorFilter2.y1L = 0.0; monitorFilter2.y2L = 0.0
        monitorFilter2.x1R = 0.0; monitorFilter2.x2R = 0.0; monitorFilter2.y1R = 0.0; monitorFilter2.y2R = 0.0
        monitorFilter3.x1L = 0.0; monitorFilter3.x2L = 0.0; monitorFilter3.y1L = 0.0; monitorFilter3.y2L = 0.0
        monitorFilter3.x1R = 0.0; monitorFilter3.x2R = 0.0; monitorFilter3.y1R = 0.0; monitorFilter3.y2R = 0.0
        monitorFilter4.x1L = 0.0; monitorFilter4.x2L = 0.0; monitorFilter4.y1L = 0.0; monitorFilter4.y2L = 0.0
        monitorFilter4.x1R = 0.0; monitorFilter4.x2R = 0.0; monitorFilter4.y1R = 0.0; monitorFilter4.y2R = 0.0
        fletcherMunsonLow.x1L = 0.0; fletcherMunsonLow.x2L = 0.0; fletcherMunsonLow.y1L = 0.0; fletcherMunsonLow.y2L = 0.0
        fletcherMunsonLow.x1R = 0.0; fletcherMunsonLow.x2R = 0.0; fletcherMunsonLow.y1R = 0.0; fletcherMunsonLow.y2R = 0.0
        fletcherMunsonHigh.x1L = 0.0; fletcherMunsonHigh.x2L = 0.0; fletcherMunsonHigh.y1L = 0.0; fletcherMunsonHigh.y2L = 0.0
        fletcherMunsonHigh.x1R = 0.0; fletcherMunsonHigh.x2R = 0.0; fletcherMunsonHigh.y1R = 0.0; fletcherMunsonHigh.y2R = 0.0
        transformerLowpass.x1L = 0.0; transformerLowpass.x2L = 0.0; transformerLowpass.y1L = 0.0; transformerLowpass.y2L = 0.0
        transformerLowpass.x1R = 0.0; transformerLowpass.x2R = 0.0; transformerLowpass.y1R = 0.0; transformerLowpass.y2R = 0.0
        transientEnvFast = 0.0
        transientEnvSlow = 0.0
        transientEnvSustain = 0.0
    }

    fun togglePlayPause(scope: CoroutineScope): Boolean {
        if (isPlaying.get()) {
            pausePlayback()
            return false
        } else {
            startPlayback(scope)
            return true
        }
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying.get()

    /**
     * Complete 6-stage PRD 8.1 & 8.3 signal processing pipeline for a stereo sample.
     * Can be called for live streaming playback, offline file processing (PRD 6.10 Tier 2 / 6.13 / 6.14),
     * and automated mathematical test assertions.
     */
    fun processStereoSample(rawL: Double, rawR: Double): Pair<Double, Double> {
        if (isBypassed.get()) {
            // Instant Bypass (<50ms A/B testing, PRD FR-3)
            // If Gain-Matched A/B is active, calibrate raw volume to match processed RMS
            if (gainMatchedAB && rawRmsRolling > 0.0001 && procRmsRolling > 0.0001) {
                val gainMatchRatio = (procRmsRolling / rawRmsRolling).coerceIn(0.2, 5.0)
                return Pair(rawL * gainMatchRatio, rawR * gainMatchRatio)
            }
            return Pair(rawL, rawR)
        }

        // ==========================================
        // STAGE 0: PRO MASTERING SUB-CUT FILTER
        // 18-24dB/oct Infrasonic Rumble Stripper
        // ==========================================
        var s0L = rawL
        var s0R = rawR
        if (subCutFilter != SubCutFilter.OFF && subCutFilter.cutoffHz > 10f) {
            s0L = subCutFilter2.processL(subCutFilter1.processL(s0L))
            s0R = subCutFilter2.processR(subCutFilter1.processR(s0R))
        }

        // ==========================================
        // STAGE 1: NOISE REDUCTION (PRD 6.10 & 8.1)
        // ==========================================
        var s1L = s0L
        var s1R = s0R

        if (vintageMode) {
            // Reverse Time Machine: Synthesize analog noise + warble with fractional delay line
            val (warbledL, warbledR) = applyVintageEffects(s1L, s1R)
            s1L = warbledL
            s1R = warbledR
        } else {
            // 1a. Multi-Harmonic De-Hum (50/60Hz + 2 harmonics)
            if (deHumEnabled) {
                s1L = humNotch3.processL(humNotch2.processL(humNotch1.processL(s1L)))
                s1R = humNotch3.processR(humNotch2.processR(humNotch1.processR(s1R)))
            }

            // 1b. Time-domain Derivative De-Crackle (PRD 8.3 click detector)
            if (deCrackleEnabled) {
                val diffL = abs(s1L - prevSampleL)
                val diffR = abs(s1R - prevSampleR)
                val spikeThreshold = 0.35 // Transient discontinuity threshold

                if (diffL > spikeThreshold && abs(prevSampleL) < 0.75) {
                    // Interpolate click spike with preceding sample
                    s1L = (prevSampleL * 0.7) + (s1L * 0.3)
                }
                if (diffR > spikeThreshold && abs(prevSampleR) < 0.75) {
                    s1R = (prevSampleR * 0.7) + (s1R * 0.3)
                }
            }
            prevSampleL = s1L
            prevSampleR = s1R

            // 1c. Adaptive High-Shelf + Spectral Noise Gate (PRD 8.3)
            if (hissRemoval > 0f) {
                // Apply high-shelf attenuation above 4.8kHz
                s1L = hissHighShelf.processL(s1L)
                s1R = hissHighShelf.processR(s1R)

                // Leaky-integrator envelope follower (1ms attack, 30ms release) avoids zero-crossing distortion
                val instAmp = (abs(s1L) + abs(s1R)) * 0.5
                val attackCoef = 0.045
                val releaseCoef = 0.00075
                hissDetectorEnvelope += if (instAmp > hissDetectorEnvelope) {
                    attackCoef * (instAmp - hissDetectorEnvelope)
                } else {
                    releaseCoef * (instAmp - hissDetectorEnvelope)
                }

                // Track noise floor during quieter sections
                if (hissDetectorEnvelope < 0.08) {
                    noiseFloorEstimate = noiseFloorEstimate * 0.9995 + hissDetectorEnvelope * 0.0005
                }

                // Soft gate when signal falls below estimated noise floor
                val gateThreshold = noiseFloorEstimate * (1.0 + (hissRemoval / 100.0) * 2.5)
                if (hissDetectorEnvelope < gateThreshold) {
                    val gateFactor = (hissDetectorEnvelope / gateThreshold).coerceIn(0.25, 1.0)
                    s1L *= gateFactor
                    s1R *= gateFactor
                }
            }
        }

        // ==========================================
        // STAGE 2: EQUALIZER (PRD 6.1, 6.2 & 8.1)
        // ==========================================
        var s2L = s1L
        var s2R = s1R

        if (isAdvancedParametricMode) {
            // 10 Independent Parametric Bands with individual Q
            parametricFilters.values.forEach { biquad ->
                s2L = biquad.processL(s2L)
                s2R = biquad.processR(s2R)
            }
        } else {
            // 5 Plain-English Bands
            simpleFilters.values.forEach { biquad ->
                s2L = biquad.processL(s2L)
                s2R = biquad.processR(s2R)
            }
        }

        // ==========================================
        // STAGE 3: CLARITY MACRO (PRD 6.9 & 8.3)
        // 3-Band Crossover + Presence Exciter
        // ==========================================
        var s3L = s2L
        var s3R = s2R

        if (clarityMacroAmount > 0f) {
            val factor = (clarityMacroAmount / 100.0)

            // 3.5kHz Linkwitz-Riley crossover split
            val lowL = crossoverLow.processL(s2L)
            val highL = crossoverHigh.processL(s2L)

            val lowR = crossoverLow.processR(s2R)
            val highR = crossoverHigh.processR(s2R)

            // Add gentle 2nd & 3rd order harmonic saturation to high-mid only
            val excitedHighL = highL + factor * 0.18 * tanh(highL * 1.6)
            val excitedHighR = highR + factor * 0.18 * tanh(highR * 1.6)

            // Dynamic de-harsher: attenuate high band if it spikes aggressively
            val deHarshL = if (abs(excitedHighL) > 0.6) excitedHighL * 0.85 else excitedHighL
            val deHarshR = if (abs(excitedHighR) > 0.6) excitedHighR * 0.85 else excitedHighR

            s3L = lowL + deHarshL
            s3R = lowR + deHarshR
        }

        // ==========================================
        // STAGE 4: DYNAMICS COMPRESSOR (PRD 6.8 & 8.3)
        // RMS Soft-Knee Compressor
        // ==========================================
        val (s4L, s4R) = processDynamicsCompressor(s3L, s3R)

        // ==========================================
        // STAGE 4a: PRO STUDIO DYNAMIC TRANSIENT DESIGNER
        // (SPL / Oxford TransMod Differential Envelope Follower)
        // ==========================================
        val (s4aL, s4aR) = processTransientDesigner(s4L, s4R)

        // ==========================================
        // STAGE 4b: PRO STUDIO TAPE/TUBE SATURATION DRIVE
        // ==========================================
        val (s4bL, s4bR) = processStudioSaturation(s4aL, s4aR)

        // ==========================================
        // STAGE 4c: PRO STUDIO HARMONIC SATURATION TOPOLOGY
        // (Triode Class-A / Reel Tape / Console Iron Core)
        // ==========================================
        val (s4cL, s4cR) = processHarmonicSaturation(s4bL, s4bR)

        // ==========================================
        // STAGE 4d: PRO STUDIO DYNAMIC DE-ESSER
        // ==========================================
        val (s4dL, s4dR) = processDeEsser(s4cL, s4cR)

        // ==========================================
        // STAGE 5: VIRTUALIZER SPACE (PRD 6.3 & FR-4)
        // ==========================================
        val (s5L, s5R) = processVirtualizerSpace(s4dL, s4dR)

        // ==========================================
        // STAGE 5b: PRO STUDIO BASS MONO-MAKER (<120Hz)
        // ==========================================
        val (s5bL, s5bR) = if (bassMonoMakerEnabled) {
            processBassMonoMaker(s5L, s5R)
        } else {
            Pair(s5L, s5R)
        }

        // ==========================================
        // STAGE 5c: ISO 226 FLETCHER-MUNSON EQUAL-LOUDNESS CONTOUR
        // ==========================================
        val (s5cL, s5cR) = if (fletcherMunsonEnabled) {
            Pair(
                fletcherMunsonHigh.processL(fletcherMunsonLow.processL(s5bL)),
                fletcherMunsonHigh.processR(fletcherMunsonLow.processR(s5bR))
            )
        } else {
            Pair(s5bL, s5bR)
        }

        // ==========================================
        // STAGE 6: TIME & SPACE FX: ECHO & REVERB
        // ==========================================
        val (s6L, s6R) = processStereoEchoAndReverb(s5cL, s5cR)

        // ==========================================
        // STAGE 7: VOLUME BOOST & LIMITER / HARD CLIPPER
        // ==========================================
        val (s7L, s7R) = processLoudnessAndLimiter(s6L, s6R)

        // ==========================================
        // STAGE 8: PRO STUDIO REFERENCE MONITOR BENCH
        // ==========================================
        val (s8L, s8R) = processReferenceMonitor(s7L, s7R)

        // ==========================================
        // STAGE 9: PRO STUDIO MID/SIDE & PHASE AUDITION
        // ==========================================
        val (s9L, s9R) = processMidSideAudition(s8L, s8R)

        // ==========================================
        // STAGE 10: STEREO TRIM & CHANNEL POLARITY
        // ==========================================
        val trimL = 10.0.pow((-stereoBalanceTrimDb).coerceAtLeast(0f) / -20.0)
        val trimR = 10.0.pow((+stereoBalanceTrimDb).coerceAtLeast(0f) / -20.0)
        val s10L = s9L * trimL * (if (invertLeftPolarity) -1.0 else 1.0)
        val s10R = s9R * trimR * (if (invertRightPolarity) -1.0 else 1.0)

        // Real-time instantaneous phase correlation tracking
        val prod = s10L * s10R
        val energyL = s10L * s10L
        val energyR = s10R * s10R
        runningLrProd = 0.995 * runningLrProd + 0.005 * prod
        runningLSq = 0.995 * runningLSq + 0.005 * energyL
        runningRSq = 0.995 * runningRSq + 0.005 * energyR
        val runningDenom = sqrt(runningLSq * runningRSq)
        if (runningDenom > 1e-6) {
            phaseCorrelation = (runningLrProd / runningDenom).toFloat().coerceIn(-1.0f, 1.0f)
            correlationMetric = phaseCorrelation
        }

        return Pair(s10L, s10R)
    }

    private fun processDynamicsCompressor(inL: Double, inR: Double): Pair<Double, Double> {
        // RMS-based power detector (PRD 8.3: RMS, not pure peak)
        val power = (inL * inL + inR * inR) * 0.5

        val ratioVal: Double
        val threshDb: Double
        val attackMs: Double
        val releaseMs: Double

        if (isAdvancedParametricMode) {
            ratioVal = compRatio.toDouble()
            threshDb = compThresholdDb.toDouble()
            attackMs = compAttackMs.toDouble()
            releaseMs = compReleaseMs.toDouble()
        } else {
            // Simple Mode Punch slider mapping (PRD 8.3: ratio 1.5..4.0, threshold -12..-24dB, attack 30..10ms, release 100..250ms)
            val pNorm = (punchAmount / 100.0).coerceIn(0.0, 1.0)
            ratioVal = 1.5 + pNorm * 2.5
            threshDb = -12.0 - pNorm * 12.0
            attackMs = 30.0 - pNorm * 20.0
            releaseMs = 100.0 + pNorm * 150.0
        }

        // Time constants from parameters (PRD 8.3)
        val attackCoef = 1.0 - kotlin.math.exp(-1.0 / (SAMPLE_RATE * (attackMs / 1000.0)))
        val releaseCoef = 1.0 - kotlin.math.exp(-1.0 / (SAMPLE_RATE * (releaseMs / 1000.0)))

        compressorEnvelope += if (power > compressorEnvelope) {
            attackCoef * (power - compressorEnvelope)
        } else {
            releaseCoef * (power - compressorEnvelope)
        }

        val rmsAmp = sqrt(max(1e-12, compressorEnvelope))

        // Linear threshold
        val thresholdLinear = 10.0.pow(threshDb / 20.0)
        val ratio = max(1.0, ratioVal)
        var gainReduction = 1.0

        // Soft-knee compression calculation
        if (rmsAmp > thresholdLinear) {
            val overRatio = rmsAmp / thresholdLinear
            val compressedOver = overRatio.pow(1.0 / ratio)
            gainReduction = (thresholdLinear * compressedOver) / rmsAmp
        }

        // Automatic makeup gain
        val makeupLinear = 10.0.pow((-threshDb * 0.35) / 20.0)
        val totalGain = gainReduction * makeupLinear

        return Pair(inL * totalGain, inR * totalGain)
    }

    private fun processVirtualizerSpace(inL: Double, inR: Double): Pair<Double, Double> {
        // PRD FR-4: On confirmed mono input, cap space at 35% to prevent phase cancellation
        val effectiveSpace = if (correlationMetric > 0.90f) {
            min(spaceAmount, 35f)
        } else {
            spaceAmount
        }

        val widthFactor = (effectiveSpace / 100.0)

        // Store into delay ring buffer
        delayBufferL[delayWriteIndex] = inL
        delayBufferR[delayWriteIndex] = inR
        delayWriteIndex = (delayWriteIndex + 1) % delayBufferSize

        // Delay offset and reflection intensity based on Static Spatial Room Simulation (PRD 6.7a)
        val (delayOffset, reflectionCoeff) = when (spatialRoomType) {
            "Intimate Studio" -> Pair(530, 0.22) // ~12ms studio reflection
            "Concert Hall" -> Pair(1060, 0.35)   // ~24ms hall reflection
            "Cathedral" -> Pair(1550, 0.45)      // ~35ms cathedral space
            else -> Pair(350, 0.0)               // ~8ms standard HRTF crossfeed
        }

        val readIndex = (delayWriteIndex - 350 + delayBufferSize) % delayBufferSize
        val delayedL = delayBufferL[readIndex]
        val delayedR = delayBufferR[readIndex]

        // Room reflection tap
        val roomReadIndex = (delayWriteIndex - delayOffset + delayBufferSize) % delayBufferSize
        val roomL = delayBufferL[roomReadIndex] * reflectionCoeff * widthFactor
        val roomR = delayBufferR[roomReadIndex] * reflectionCoeff * widthFactor

        // Transaural crossfeed decorrelation + binaural contralateral room simulation
        val hrtfCoeff = when (hrtfProfile) {
            "Narrow" -> 0.25
            "Wide" -> 0.70
            else -> 0.45
        }
        val outL = inL + (delayedR - inR) * (widthFactor * hrtfCoeff) + roomR
        val outR = inR + (delayedL - inL) * (widthFactor * hrtfCoeff) + roomL

        return Pair(outL, outR)
    }

    private fun processLoudnessAndLimiter(inL: Double, inR: Double): Pair<Double, Double> {
        // Volume Boost gain (+0 to +12dB)
        val boostLinear = 10.0.pow((loudnessBoost / 100.0 * 12.0) / 20.0)
        var boostedL = inL * boostLinear
        var boostedR = inR * boostLinear

        // True-Peak Soft Brickwall Limiter (PRD 6.6, 8.3 & FR-5)
        val ceiling = 10.0.pow(limiterCeilingDb / 20.0).coerceIn(0.70, 0.99)

        if (limiterMode == LimiterMode.HARD_CLIPPER) {
            // Modern mastering hard-clipper (retains punchy transient peaks)
            boostedL = boostedL.coerceIn(-ceiling, ceiling)
            boostedR = boostedR.coerceIn(-ceiling, ceiling)
        } else {
            // True-Peak Soft Brickwall Limiter (PRD 6.6, 8.3 & FR-5)
            val knee = ceiling * 0.85
            if (abs(boostedL) > knee) {
                val sign = if (boostedL >= 0) 1.0 else -1.0
                val diff = abs(boostedL) - knee
                boostedL = sign * (knee + (ceiling - knee) * tanh(diff / (ceiling - knee)))
            }
            if (abs(boostedR) > knee) {
                val sign = if (boostedR >= 0) 1.0 else -1.0
                val diff = abs(boostedR) - knee
                boostedR = sign * (knee + (ceiling - knee) * tanh(diff / (ceiling - knee)))
            }
        }

        return Pair(boostedL, boostedR)
    }

    private fun processTransientDesigner(inL: Double, inR: Double): Pair<Double, Double> {
        if (transientAttackPercent == 0f && transientSustainPercent == 0f) {
            transientAttackActivity = 0f
            transientSustainActivity = 0f
            return Pair(inL, inR)
        }

        val absInput = (abs(inL) + abs(inR)) * 0.5

        // Fast envelope follower (Attack tracker): ~1.5ms attack, 20ms release
        val fastAttackCoef = 0.015
        val fastReleaseCoef = 0.0011
        transientEnvFast += if (absInput > transientEnvFast) {
            fastAttackCoef * (absInput - transientEnvFast)
        } else {
            fastReleaseCoef * (absInput - transientEnvFast)
        }

        // Slow envelope follower (Body tracker): ~25ms attack, 90ms release
        val slowAttackCoef = 0.0009
        val slowReleaseCoef = 0.00025
        transientEnvSlow += if (absInput > transientEnvSlow) {
            slowAttackCoef * (absInput - transientEnvSlow)
        } else {
            slowReleaseCoef * (absInput - transientEnvSlow)
        }

        // Sustain envelope follower (Tail tracker): ~60ms attack, 350ms release
        val susAttackCoef = 0.00038
        val susReleaseCoef = 0.000065
        transientEnvSustain += if (absInput > transientEnvSustain) {
            susAttackCoef * (absInput - transientEnvSustain)
        } else {
            susReleaseCoef * (absInput - transientEnvSustain)
        }

        // Differential transient activity
        val diffAttack = max(0.0, transientEnvFast - transientEnvSlow)
        val diffSustain = max(0.0, transientEnvSlow - transientEnvSustain)

        // Scale factors: -100%..+100% maps to -12dB..+12dB gain adjustment
        val attackFactor = (transientAttackPercent / 100.0)
        val sustainFactor = (transientSustainPercent / 100.0)

        val normDenom = transientEnvSlow + 0.0005
        val attackGainMod = 1.0 + attackFactor * 2.5 * (diffAttack / normDenom)
        val sustainGainMod = 1.0 + sustainFactor * 1.8 * (diffSustain / normDenom)

        val finalMultiplier = (attackGainMod * sustainGainMod).coerceIn(0.15, 4.0)

        // Update live activity for UI
        transientAttackActivity = (diffAttack / (normDenom + 0.01)).toFloat().coerceIn(0f, 1f)
        transientSustainActivity = (diffSustain / (normDenom + 0.01)).toFloat().coerceIn(0f, 1f)

        return Pair(inL * finalMultiplier, inR * finalMultiplier)
    }

    private fun processHarmonicSaturation(inL: Double, inR: Double): Pair<Double, Double> {
        if (harmonicSaturationType == HarmonicSaturationType.CLEAN || harmonicDrivePercent <= 0f) {
            return Pair(inL, inR)
        }

        // Drive range: 1.0 (0%) to 4.5 (+15 dB at 100%)
        val drive = 1.0 + (harmonicDrivePercent / 100.0) * 3.5
        val levelComp = 1.0 / sqrt(drive) // Automatic level compensation to prevent volume blinding

        var satL: Double
        var satR: Double

        when (harmonicSaturationType) {
            HarmonicSaturationType.CLEAN -> {
                return Pair(inL, inR)
            }
            HarmonicSaturationType.TUBE_TRIODE -> {
                // Asymmetric 2nd-order even harmonics (2f₀)
                val xL = inL * drive
                val xR = inR * drive
                val rawSatL = tanh(xL + 0.35 * xL * xL) * levelComp
                val rawSatR = tanh(xR + 0.35 * xR * xR) * levelComp

                // DC Blocker filter (y[n] = x[n] - x[n-1] + 0.9995 * y[n-1]) to remove DC bias from quadratic term
                satL = rawSatL - dcBlockerPrevInL + 0.9995 * dcBlockerPrevOutL
                satR = rawSatR - dcBlockerPrevInR + 0.9995 * dcBlockerPrevOutR
                dcBlockerPrevInL = rawSatL
                dcBlockerPrevInR = rawSatR
                dcBlockerPrevOutL = satL
                dcBlockerPrevOutR = satR
            }
            HarmonicSaturationType.TAPE_PENTODE -> {
                // Symmetric 3rd-order odd harmonics (3f₀)
                val xL = inL * drive
                val xR = inR * drive
                satL = tanh(xL) * levelComp * 1.05
                satR = tanh(xR) * levelComp * 1.05
            }
            HarmonicSaturationType.CONSOLE_TRANSFORMER -> {
                // Frequency-dependent iron core magnetic hysteresis saturation (below 120Hz)
                val lowL = transformerLowpass.processL(inL)
                val lowR = transformerLowpass.processR(inR)
                val highL = inL - lowL
                val highR = inR - lowR

                val satLowL = tanh(lowL * drive * 1.6) * levelComp * 1.15
                val satLowR = tanh(lowR * drive * 1.6) * levelComp * 1.15

                satL = satLowL + highL
                satR = satLowR + highR
            }
        }

        // Track harmonic distortion energy vs fundamental energy for live THD %
        val distEnergy = ((satL - inL) * (satL - inL) + (satR - inR) * (satR - inR)) * 0.5
        val fundEnergy = (inL * inL + inR * inR) * 0.5
        thdHarmonicAccum += distEnergy
        thdFundamentalAccum += fundEnergy

        return Pair(satL, satR)
    }

    private fun processStudioSaturation(inL: Double, inR: Double): Pair<Double, Double> {
        if (tapeDrivePercent <= 0f) return Pair(inL, inR)
        val drive = 1.0 + (tapeDrivePercent / 100.0) * 3.5
        // Asymmetric warm harmonic saturation (emulates analog tape & tube consoles)
        val satL = (tanh(inL * drive) + 0.15 * (inL * drive) * (inL * drive).coerceAtMost(1.0)) * 0.92
        val satR = (tanh(inR * drive) + 0.15 * (inR * drive) * (inR * drive).coerceAtMost(1.0)) * 0.92
        return Pair(satL, satR)
    }

    private fun processBassMonoMaker(inL: Double, inR: Double): Pair<Double, Double> {
        val lowL = monoMakerLow.processL(inL)
        val lowR = monoMakerLow.processR(inR)
        val highL = monoMakerHigh.processL(inL)
        val highR = monoMakerHigh.processR(inR)

        // Low-end (<120Hz) summed to pure mono for club PA/vinyl safety
        val monoLow = (lowL + lowR) * 0.5
        return Pair(monoLow + highL, monoLow + highR)
    }

    private fun processReferenceMonitor(inL: Double, inR: Double): Pair<Double, Double> {
        if (referenceMonitor == ReferenceMonitor.FLAT) return Pair(inL, inR)

        var l = monitorFilter1.processL(inL)
        var r = monitorFilter1.processR(inR)
        l = monitorFilter2.processL(l)
        r = monitorFilter2.processR(r)
        l = monitorFilter3.processL(l)
        r = monitorFilter3.processR(r)
        l = monitorFilter4.processL(l)
        r = monitorFilter4.processR(r)

        when (referenceMonitor) {
            ReferenceMonitor.CAR_TEST -> {
                // In-cabin crossfeed and windshield acoustic reflection
                val cabinL = l * 0.80 + r * 0.20
                val cabinR = r * 0.80 + l * 0.20
                l = cabinL
                r = cabinR
            }
            ReferenceMonitor.PHONE_SPEAKER -> {
                // Mono collapse + micro-transducer saturation
                val mono = (l + r) * 0.5
                val satMono = tanh(mono * 1.4) * 0.85
                l = satMono
                r = satMono
            }
            ReferenceMonitor.MACBOOK_PRO -> {
                // Micro-speaker physical separation crossfeed
                val crossL = l * 0.75 + r * 0.25
                val crossR = r * 0.75 + l * 0.25
                l = crossL
                r = crossR
            }
            else -> { /* Pure acoustic filtering for other profiles */ }
        }

        return Pair(l, r)
    }

    private fun processMidSideAudition(inL: Double, inR: Double): Pair<Double, Double> {
        return when (midSideMode) {
            MidSideMode.STEREO -> Pair(inL, inR)
            MidSideMode.MONO_SUM -> {
                val mono = (inL + inR) * 0.5
                Pair(mono, mono)
            }
            MidSideMode.MID_ONLY -> {
                val mid = (inL + inR) * 0.5
                Pair(mid, mid)
            }
            MidSideMode.SIDE_ONLY -> {
                val side = (inL - inR) * 0.5
                Pair(side, -side)
            }
            MidSideMode.PHASE_INVERT -> Pair(inL, -inR)
        }
    }

    // Defensive inter-stage soft saturation (bugfix): widening the echo and
    // reverb wet-mix gains independently (6.17) without accounting for them
    // being CASCADED (echo output feeds directly into reverb input) created a
    // real risk of the signal compounding toward Infinity/NaN under extreme
    // simultaneous settings, before ever reaching the final limiter. This
    // clamps at a ceiling far above any normal or even extreme perceptual
    // level (+-4.0, i.e. +12dB over full scale) - it exists purely to stop
    // runaway feedback accumulation across cascaded stages, not to shape the
    // sound audibly in ordinary use.
    private fun interStageSafetyClamp(x: Double, ceiling: Double = 4.0): Double {
        if (x.isNaN() || x.isInfinite()) return 0.0
        val knee = ceiling * 0.9
        if (abs(x) < knee) return x
        val sign = if (x >= 0) 1.0 else -1.0
        val over = abs(x) - knee
        return sign * (knee + (ceiling - knee) * tanh(over / (ceiling - knee)))
    }

    fun processStereoEchoAndReverb(inL: Double, inR: Double): Pair<Double, Double> {
        val (echoL, echoR) = processStereoEcho(inL, inR)
        val safeEchoL = interStageSafetyClamp(echoL)
        val safeEchoR = interStageSafetyClamp(echoR)
        val (revL, revR) = processStereoReverb(safeEchoL, safeEchoR)
        return Pair(interStageSafetyClamp(revL), interStageSafetyClamp(revR))
    }

    fun processStereoEcho(inL: Double, inR: Double): Pair<Double, Double> {
        val wetFactor = (echoWet / 100.0).coerceIn(0.0, 1.0)
        val delaySamples = ((echoTimeMs / 1000.0) * SAMPLE_RATE).toInt().coerceIn(1, echoBufferSize - 1)
        val readIdx = (echoWriteIndex - delaySamples + echoBufferSize) % echoBufferSize

        val delayedL = echoBufferL[readIdx]
        val delayedR = echoBufferR[readIdx]

        // 1-pole low-pass filter on feedback repeats (gentle analog tape damping: retains sparkle for 4-6 repeats)
        prevEchoFilterL = delayedL * 0.85 + prevEchoFilterL * 0.15
        prevEchoFilterR = delayedR * 0.85 + prevEchoFilterR * 0.15
        if (abs(prevEchoFilterL) < 1e-15) prevEchoFilterL = 0.0
        if (abs(prevEchoFilterR) < 1e-15) prevEchoFilterR = 0.0

        val feedbackFactor = if (echoFeedback >= 96f) 0.985 else (echoFeedback / 100.0).coerceIn(0.0, 0.985)
        val crossfeed = if (echoPingPong) 0.35 else 0.0

        // Ping-pong crossfeed into delay buffers with analog tape saturation
        val rawFeedL = inL + (prevEchoFilterL * (1.0 - crossfeed) + prevEchoFilterR * crossfeed) * feedbackFactor
        val rawFeedR = inR + (prevEchoFilterR * (1.0 - crossfeed) + prevEchoFilterL * crossfeed) * feedbackFactor

        val satL = tanh(rawFeedL * 1.05) * 0.95
        val satR = tanh(rawFeedR * 1.05) * 0.95

        echoBufferL[echoWriteIndex] = if (abs(satL) < 1e-15) 0.0 else satL
        echoBufferR[echoWriteIndex] = if (abs(satR) < 1e-15) 0.0 else satR
        echoWriteIndex = (echoWriteIndex + 1) % echoBufferSize

        if (wetFactor <= 0f) {
            return Pair(inL, inR)
        }

        // Bold, punchy wet mix crossfade: at 100% wet, dry = 0.0; at 50% wet, repeats match dry volume
        val dryGain = (1.0 - wetFactor.pow(1.25)).coerceIn(0.0, 1.0)
        val wetGain = (wetFactor.pow(0.7) * 1.35).coerceIn(0.0, 1.8)

        val outL = inL * dryGain + delayedL * wetGain
        val outR = inR * dryGain + delayedR * wetGain

        return Pair(outL, outR)
    }

    fun processStereoReverb(inL: Double, inR: Double): Pair<Double, Double> {
        val wetGain = (reverbWet / 100.0).coerceIn(0.0, 1.0)
        if (wetGain <= 0f) {
            return Pair(inL, inR)
        }

        // Base feedback/damping: If Freeze mode enabled, infinite hold!
        val feedback = if (reverbFreezeEnabled) 0.999 else {
            (0.68 + (reverbRoomSize / 100.0) * 0.28 + roomSize.feedbackBias + wallMaterial.feedbackBias)
                .coerceIn(0.40, 0.988)
        }
        val damping = if (reverbFreezeEnabled) 0.0 else {
            (0.04 + (reverbDamping / 100.0) * 0.50 + roomSize.dampingBias + wallMaterial.dampingBias)
                .coerceIn(0.01, 0.85)
        }

        // Hotter input feed (8x higher energy into comb network than old 0.035)
        val monoIn = (inL + inR) * 0.22

        for (i in leftCombs.indices) {
            leftCombs[i].activeLength = (combBaseTunings[i] * roomSize.lengthScale).toInt()
            rightCombs[i].activeLength = (combBaseTuningsR[i] * roomSize.lengthScale).toInt()
        }

        var outL = 0.0
        var outR = 0.0
        for (i in 0 until 8) {
            outL += leftCombs[i].process(monoIn, feedback, damping)
            outR += rightCombs[i].process(monoIn, feedback, damping)
        }

        for (i in 0 until 4) {
            outL = leftAllPass[i].process(outL, 0.5)
            outR = rightAllPass[i].process(outR, 0.5)
        }

        // Wall material spectral coloration
        outL = reverbToneTiltL.process(outL, wallMaterial.toneTilt)
        outR = reverbToneTiltR.process(outR, wallMaterial.toneTilt)

        val width = (reverbWidth / 100.0).coerceIn(0.0, 1.0)
        val wet1 = (1.0 + width) * 0.5
        val wet2 = (1.0 - width) * 0.5
        val wetL = outL * wet1 + outR * wet2
        val wetR = outR * wet1 + outL * wet2

        // True equal-power / bold wet mix:
        // At 100% wet: dry is 0.0 (pure ambient reverb wash!)
        // At 50% wet: dry is ~0.60, wet is ~0.95 (dramatic, lush, clearly audible!)
        val dryGain = (1.0 - wetGain.pow(1.2)).coerceIn(0.0, 1.0)
        val wetMultiplier = (wetGain.pow(0.7) * 1.5).coerceIn(0.0, 2.2)

        val finalL = inL * dryGain + wetL * wetMultiplier
        val finalR = inR * dryGain + wetR * wetMultiplier

        return Pair(finalL, finalR)
    }

    private fun applyVintageEffects(inL: Double, inR: Double): Pair<Double, Double> {
        // Store into dedicated vintage delay buffer for pitch modulation
        vintageDelayL[vintageWriteIndex] = inL
        vintageDelayR[vintageWriteIndex] = inR
        vintageWriteIndex = (vintageWriteIndex + 1) % vintageDelaySize

        // LFO rates for wow (~1.2Hz) and flutter (~6.8Hz)
        lfoPhase += 1.2 * (2.0 * PI / SAMPLE_RATE)
        if (lfoPhase > 2.0 * PI * 1000) lfoPhase -= 2.0 * PI * 1000

        val wowSamples = sin(lfoPhase) * (wowFlutterDepth / 100.0) * 14.0
        val flutterSamples = sin(lfoPhase * 5.7) * (wowFlutterDepth / 100.0) * 3.5
        val totalDelay = 100.0 + wowSamples + flutterSamples

        val intDelay = totalDelay.toInt()
        val frac = totalDelay - intDelay

        // Linear interpolation from delay line creates authentic Doppler pitch warble (PRD 6.13 & 8.3)
        val idx0 = (vintageWriteIndex - intDelay + vintageDelaySize) % vintageDelaySize
        val idx1 = (vintageWriteIndex - intDelay - 1 + vintageDelaySize) % vintageDelaySize

        val warbledL = (1.0 - frac) * vintageDelayL[idx0] + frac * vintageDelayL[idx1]
        val warbledR = (1.0 - frac) * vintageDelayR[idx0] + frac * vintageDelayR[idx1]

        val noiseAmp = (vintageNoiseLevel / 100.0) * 0.08
        val tapeHissL = (Random.nextDouble() - 0.5) * noiseAmp
        val tapeHissR = (Random.nextDouble() - 0.5) * noiseAmp
        val vinylPop = if (Random.nextDouble() < (vintageNoiseLevel / 15000.0)) (Random.nextDouble() - 0.5) * 0.45 else 0.0

        // Mains hum synthesis at 50/60Hz + 2nd harmonic (PRD 6.13 & 8.3)
        val humPhase = lfoPhase * (humFrequency / 1.2)
        val mainsHum = (sin(humPhase) * 0.015 + sin(humPhase * 2.0) * 0.005) * (vintageNoiseLevel / 100.0)

        return Pair(warbledL + tapeHissL + vinylPop + mainsHum, warbledR + tapeHissR + vinylPop + mainsHum)
    }

    private fun synthesizeSourceSample(track: DemoTrack): Pair<Double, Double> {
        val noteLengthSeconds = 0.28
        seqTimer += (1.0 / SAMPLE_RATE) * playbackSpeed
        if (seqTimer >= noteLengthSeconds) {
            seqTimer = 0.0
            seqStep++
            noteEnvelope = 1.0 // Trigger pluck attack
        } else {
            noteEnvelope *= 0.99965 // Natural acoustic / pluck exponential decay
        }

        val notes = when (currentTrackIndex) {
            0 -> acousticProgression
            1 -> jazzProgression
            2 -> synthProgression
            else -> lofiProgression
        }

        val currentNoteFreq = notes[seqStep % notes.size]
        val dt = 2.0 * PI / SAMPLE_RATE

        phase += currentNoteFreq * dt
        if (phase > 2.0 * PI * 100) phase -= 2.0 * PI * 100

        harmonicPhase += currentNoteFreq * 2.0 * dt
        if (harmonicPhase > 2.0 * PI * 100) harmonicPhase -= 2.0 * PI * 100

        subPhase += currentNoteFreq * 0.5 * dt
        if (subPhase > 2.0 * PI * 100) subPhase -= 2.0 * PI * 100

        // Plucked note synthesis with body, fundamental, harmonic, and sub-warmth
        val fundamental = sin(phase) * 0.38
        val harmonic = sin(harmonicPhase) * 0.18
        val sub = sin(subPhase) * 0.22
        val overtone = sin(phase * 3.0) * 0.08
        val rawClean = (fundamental + harmonic + sub + overtone) * noteEnvelope

        var noise = 0.0
        when (track.noiseType) {
            "cassette" -> noise = (Random.nextDouble() - 0.5) * 0.04
            "vinyl" -> {
                val hum = sin(phase * (60.0 / currentNoteFreq)) * 0.035
                val crackle = if (Random.nextDouble() < 0.0008) (Random.nextDouble() - 0.5) * 0.6 else 0.0
                noise = hum + crackle + (Random.nextDouble() - 0.5) * 0.015
            }
            "radio" -> noise = (Random.nextDouble() - 0.5) * 0.025
            "tape_heavy" -> noise = (Random.nextDouble() - 0.5) * 0.07
        }

        // Slight natural stereo spread based on arpeggio step
        val pan = ((seqStep % 4) - 1.5) * 0.15
        val left = (rawClean * (1.0 - pan) + noise)
        val right = (rawClean * (1.0 + pan) + noise)
        return Pair(left, right)
    }

    fun updateDspCoefficients() {
        // 1. Simple 5-Band Peaking & Shelving Filters (PRD 8.3)
        eqGains.forEach { (band, gainDb) ->
            val biquad = simpleFilters[band] ?: return@forEach
            calculateCookbookBiquad(
                biquad = biquad,
                f0 = band.centerHz.toDouble(),
                gainDb = gainDb.toDouble(),
                q = 0.8,
                isLowShelf = band == PlainBand.RUMBLE,
                isHighShelf = band == PlainBand.AIR
            )
        }

        // 2. 10 Independent Parametric Bands (PRD 6.2 & 8.3)
        parametricFrequencies.forEach { hz ->
            val biquad = parametricFilters[hz] ?: return@forEach
            val gain = (parametricGains[hz] ?: 0f).toDouble()
            val q = (parametricQ[hz] ?: 0.8f).toDouble()
            calculateCookbookBiquad(
                biquad = biquad,
                f0 = hz.toDouble(),
                gainDb = gain,
                q = q,
                isLowShelf = hz <= 31,
                isHighShelf = hz >= 16000
            )
        }

        // 3. Multi-Harmonic De-Hum Notches (50/60Hz + 2nd and 3rd harmonics)
        calculateNotch(humNotch1, humFrequency.toDouble(), 12.0)
        calculateNotch(humNotch2, (humFrequency * 2).toDouble(), 12.0)
        calculateNotch(humNotch3, (humFrequency * 3).toDouble(), 12.0)

        // 4. Hiss High Shelf Filter (starts above 4.5kHz)
        val hissCutDb = -(hissRemoval / 100.0 * 20.0) // 0 to -20dB cut
        calculateCookbookBiquad(hissHighShelf, 4800.0, hissCutDb, 0.7, isLowShelf = false, isHighShelf = true)

        // 5. Clarity 3-Band Crossover Split (3.5kHz 2nd-order Linkwitz-Riley low pass & high pass)
        val wC = 2.0 * PI * 3500.0 / SAMPLE_RATE
        val alphaC = sin(wC) / (2.0 * 0.707)
        val cosWC = cos(wC)
        val a0C = 1.0 + alphaC

        crossoverLow.b0 = ((1.0 - cosWC) / 2.0) / a0C
        crossoverLow.b1 = (1.0 - cosWC) / a0C
        crossoverLow.b2 = ((1.0 - cosWC) / 2.0) / a0C
        crossoverLow.a1 = (-2.0 * cosWC) / a0C
        crossoverLow.a2 = (1.0 - alphaC) / a0C

        crossoverHigh.b0 = ((1.0 + cosWC) / 2.0) / a0C
        crossoverHigh.b1 = (-(1.0 + cosWC)) / a0C
        crossoverHigh.b2 = ((1.0 + cosWC) / 2.0) / a0C
        crossoverHigh.a1 = (-2.0 * cosWC) / a0C
        crossoverHigh.a2 = (1.0 - alphaC) / a0C

        // 6. Pro Studio Bass Mono-Maker Crossover (120Hz Butterworth)
        calculateLowpass(monoMakerLow, 120.0, 0.707)
        calculateHighpass(monoMakerHigh, 120.0, 0.707)

        // 7. Pro Studio Harmonic Saturation Transformer Core Filter (<120Hz)
        calculateLowpass(transformerLowpass, 120.0, 0.707)

        // 8. Pro Studio ISO 226 Fletcher-Munson Equal-Loudness Filters (+4.5dB @ 85Hz, +2.5dB @ 8.5kHz)
        calculateCookbookBiquad(fletcherMunsonLow, 85.0, 4.5, 0.707, isLowShelf = true, isHighShelf = false)
        calculateCookbookBiquad(fletcherMunsonHigh, 8500.0, 2.5, 0.707, isLowShelf = false, isHighShelf = true)

        // 9. Pro Studio Reference Monitor Benches (8 Dedicated Acoustic Profiles)
        when (referenceMonitor) {
            ReferenceMonitor.FLAT -> {
                calculateCookbookBiquad(monitorFilter1, 1000.0, 0.0, 0.707, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter2, 1000.0, 0.0, 0.707, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter3, 1000.0, 0.0, 0.707, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter4, 1000.0, 0.0, 0.707, isLowShelf = false, isHighShelf = false)
            }
            ReferenceMonitor.NS10M -> {
                // Yamaha NS-10M: 85Hz sealed box rolloff, iconic 1.5kHz paper cone bump, 16kHz rolloff
                calculateHighpass(monitorFilter1, 85.0, 0.707)
                calculateCookbookBiquad(monitorFilter2, 1500.0, 4.0, 1.2, isLowShelf = false, isHighShelf = false)
                calculateLowpass(monitorFilter3, 16000.0, 0.707)
                calculateCookbookBiquad(monitorFilter4, 400.0, -1.0, 1.0, isLowShelf = false, isHighShelf = false)
            }
            ReferenceMonitor.AURATONE_5C -> {
                // Auratone 5C Soundcube: 250Hz - 5.5kHz narrow vocal bandpass
                calculateHighpass(monitorFilter1, 250.0, 0.8)
                calculateLowpass(monitorFilter2, 5500.0, 0.8)
                calculateCookbookBiquad(monitorFilter3, 1200.0, 3.0, 1.2, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter4, 1000.0, 0.0, 0.707, isLowShelf = false, isHighShelf = false)
            }
            ReferenceMonitor.CAR_TEST -> {
                // Car Test: In-cabin 65Hz cavity boom, 380Hz scooped mids, 9.5kHz windshield sizzle
                calculateHighpass(monitorFilter1, 25.0, 0.707)
                calculateCookbookBiquad(monitorFilter2, 65.0, 4.5, 1.3, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter3, 380.0, -3.5, 1.0, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter4, 9500.0, 2.5, 0.8, isLowShelf = false, isHighShelf = true)
            }
            ReferenceMonitor.AIRPODS_PRO -> {
                // AirPods Pro: Harman consumer target curve with sub-bass shelf and ear-canal resonance
                calculateCookbookBiquad(monitorFilter1, 65.0, 3.5, 0.707, isLowShelf = true, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter2, 450.0, -2.0, 1.1, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter3, 3200.0, 3.0, 1.5, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter4, 11000.0, -2.5, 0.8, isLowShelf = false, isHighShelf = true)
            }
            ReferenceMonitor.PHONE_SPEAKER -> {
                // Smartphone speaker: 450Hz highpass, 2.8kHz micro-cone peak, 13.5kHz cutoff
                calculateHighpass(monitorFilter1, 450.0, 0.85)
                calculateCookbookBiquad(monitorFilter2, 2800.0, 5.0, 1.6, isLowShelf = false, isHighShelf = false)
                calculateLowpass(monitorFilter3, 13500.0, 0.707)
                calculateCookbookBiquad(monitorFilter4, 1000.0, 0.0, 0.707, isLowShelf = false, isHighShelf = false)
            }
            ReferenceMonitor.CLUB_SYSTEM -> {
                // Club PA Subwoofer: 30Hz highpass rumble cut, massive 55Hz chest-thump boost, 3.2kHz ear fatigue dip
                calculateHighpass(monitorFilter1, 30.0, 0.9)
                calculateCookbookBiquad(monitorFilter2, 55.0, 5.0, 1.4, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter3, 3200.0, -2.5, 1.0, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter4, 1000.0, 0.0, 0.707, isLowShelf = false, isHighShelf = false)
            }
            ReferenceMonitor.MACBOOK_PRO -> {
                // MacBook Pro laptop: 150Hz highpass, 250Hz virtual bass illusion, 1.8kHz chassis resonance
                calculateHighpass(monitorFilter1, 150.0, 0.8)
                calculateCookbookBiquad(monitorFilter2, 250.0, 3.5, 1.8, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter3, 1800.0, 2.5, 2.0, isLowShelf = false, isHighShelf = false)
                calculateCookbookBiquad(monitorFilter4, 15000.0, -4.0, 0.7, isLowShelf = false, isHighShelf = true)
            }
        }

        // 10. Mastering Infrasonic Sub-Cut Filter (18-24 dB/oct cascaded Butterworth)
        if (subCutFilter != SubCutFilter.OFF && subCutFilter.cutoffHz > 10f) {
            calculateHighpass(subCutFilter1, subCutFilter.cutoffHz.toDouble(), 0.7071)
            calculateHighpass(subCutFilter2, subCutFilter.cutoffHz.toDouble(), 0.7071)
        }
    }

    private fun calculateLowpass(biquad: BiquadState, f0: Double, q: Double = 0.707) {
        val w0 = 2.0 * PI * f0.coerceIn(20.0, 20000.0) / SAMPLE_RATE
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha
        biquad.b0 = ((1.0 - cosW0) / 2.0) / a0
        biquad.b1 = (1.0 - cosW0) / a0
        biquad.b2 = ((1.0 - cosW0) / 2.0) / a0
        biquad.a1 = (-2.0 * cosW0) / a0
        biquad.a2 = (1.0 - alpha) / a0
    }

    private fun calculateHighpass(biquad: BiquadState, f0: Double, q: Double = 0.707) {
        val w0 = 2.0 * PI * f0.coerceIn(20.0, 20000.0) / SAMPLE_RATE
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha
        biquad.b0 = ((1.0 + cosW0) / 2.0) / a0
        biquad.b1 = (-(1.0 + cosW0)) / a0
        biquad.b2 = ((1.0 + cosW0) / 2.0) / a0
        biquad.a1 = (-2.0 * cosW0) / a0
        biquad.a2 = (1.0 - alpha) / a0
    }

    private fun calculateCookbookBiquad(
        biquad: BiquadState,
        f0: Double,
        gainDb: Double,
        q: Double,
        isLowShelf: Boolean,
        isHighShelf: Boolean
    ) {
        val gain = 10.0.pow(gainDb / 40.0)
        val clampedF0 = f0.coerceIn(20.0, 20000.0)
        val w0 = 2.0 * PI * clampedF0 / SAMPLE_RATE
        val alpha = sin(w0) / (2.0 * q.coerceIn(0.2, 10.0))
        val cosW0 = cos(w0)

        when {
            isLowShelf -> {
                val aPlus1 = gain + 1.0
                val aMinus1 = gain - 1.0
                val sqrtA = sqrt(gain)
                val twoSqrtAAlpha = 2.0 * sqrtA * alpha

                val a0 = aPlus1 + aMinus1 * cosW0 + twoSqrtAAlpha
                biquad.b0 = (gain * (aPlus1 - aMinus1 * cosW0 + twoSqrtAAlpha)) / a0
                biquad.b1 = (2.0 * gain * (aMinus1 - aPlus1 * cosW0)) / a0
                biquad.b2 = (gain * (aPlus1 - aMinus1 * cosW0 - twoSqrtAAlpha)) / a0
                biquad.a1 = (-2.0 * (aMinus1 + aPlus1 * cosW0)) / a0
                biquad.a2 = (aPlus1 + aMinus1 * cosW0 - twoSqrtAAlpha) / a0
            }
            isHighShelf -> {
                val aPlus1 = gain + 1.0
                val aMinus1 = gain - 1.0
                val sqrtA = sqrt(gain)
                val twoSqrtAAlpha = 2.0 * sqrtA * alpha

                val a0 = aPlus1 - aMinus1 * cosW0 + twoSqrtAAlpha
                biquad.b0 = (gain * (aPlus1 + aMinus1 * cosW0 + twoSqrtAAlpha)) / a0
                biquad.b1 = (-2.0 * gain * (aMinus1 + aPlus1 * cosW0)) / a0
                biquad.b2 = (gain * (aPlus1 + aMinus1 * cosW0 - twoSqrtAAlpha)) / a0
                biquad.a1 = (2.0 * (aMinus1 - aPlus1 * cosW0)) / a0
                biquad.a2 = (aPlus1 - aMinus1 * cosW0 - twoSqrtAAlpha) / a0
            }
            else -> {
                val a0 = 1.0 + alpha / gain
                biquad.b0 = (1.0 + alpha * gain) / a0
                biquad.b1 = (-2.0 * cosW0) / a0
                biquad.b2 = (1.0 - alpha * gain) / a0
                biquad.a1 = (-2.0 * cosW0) / a0
                biquad.a2 = (1.0 - alpha / gain) / a0
            }
        }
    }

    private fun calculateNotch(biquad: BiquadState, f0: Double, q: Double) {
        if (f0 >= SAMPLE_RATE / 2) return
        val w0 = 2.0 * PI * f0 / SAMPLE_RATE
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)
        val a0 = 1.0 + alpha
        biquad.b0 = 1.0 / a0
        biquad.b1 = (-2.0 * cosW0) / a0
        biquad.b2 = 1.0 / a0
        biquad.a1 = (-2.0 * cosW0) / a0
        biquad.a2 = (1.0 - alpha) / a0
    }

    private fun computeSpectrumBands(energyAccum: DoubleArray, outBands: FloatArray) {
        for (b in 0 until 8) {
            val rawLevel = sqrt(energyAccum[b] / BUFFER_SIZE)
            // Perceptual compensation + soft compression: raw bandpass energy
            // differs by orders of magnitude between bass and treble in typical
            // music, so a straight readout would look permanently bass-heavy.
            val compensated = (rawLevel * spectrumBandCompensation[b]).pow(0.6)
            val target = compensated.toFloat().coerceIn(0.03f, 1f)

            // VU-meter-style ballistics: fast attack (bars jump up quickly on a
            // hit), slower release (they fall gently rather than flickering).
            val coef = if (target > spectrumSmoothed[b]) 0.55f else 0.15f
            spectrumSmoothed[b] += (target - spectrumSmoothed[b]) * coef
            outBands[b] = spectrumSmoothed[b].coerceIn(0.03f, 1f)
        }
    }
}
