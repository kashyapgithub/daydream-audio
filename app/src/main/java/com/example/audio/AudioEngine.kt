package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.util.Log
import com.example.model.DemoTrack
import com.example.model.PlainBand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
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

    // Reverb Parameters (Freeverb 8-Comb + 4-AllPass Network)
    var reverbWet: Float = 0f // 0 to 100%
    var reverbRoomSize: Float = 75f // 0 to 100% - fine continuous control within the selected RoomSize category
    var reverbDamping: Float = 35f // 0 to 100%
    var reverbWidth: Float = 100f // 0 to 100%
    var roomSize: RoomSize = RoomSize.LARGE_HALL // discrete character preset (PRD 6.15)
    var wallMaterial: WallMaterial = WallMaterial.PLASTER // discrete character preset (PRD 6.15)

    // Echo / Delay Parameters (Stereo Ping-Pong with Tape Damping)
    var echoWet: Float = 0f // 0 to 100%
    var echoTimeMs: Int = 320 // 50 to 3000 ms - widened for "a lot of echo" (PRD 6.17)
    var echoFeedback: Float = 30f // 0 to 96% - near-infinite trailing echo at max
    var echoPingPong: Boolean = true

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

    // Callbacks
    var onSpectrumUpdated: ((FloatArray, Float) -> Unit)? = null

    // Internal State
    private var phase = 0.0
    private var lfoPhase = 0.0
    private var noiseFloorEstimate = 0.02
    private var hissDetectorEnvelope = 0.0

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

    fun startPlayback(scope: CoroutineScope) {
        if (isPlaying.get()) return
        isPlaying.set(true)

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

        audioTrack?.play()
        try {
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
                if (!spectrumFiltersConfigured) configureSpectrumFilters()
                val spectrumEnergyAccum = DoubleArray(8)

                val track = demoTracks[currentTrackIndex]

                for (i in 0 until BUFFER_SIZE) {
                    val (rawL, rawR) = synthesizeSourceSample(track)
                    val (outL, outR) = processStereoSample(rawL, rawR)

                    // Metrics
                    rmsSum += outL * outL + outR * outR
                    lrProdSum += outL * outR
                    lSqSum += outL * outL
                    rSqSum += outR * outR

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

                // Update correlation metric (mono detection)
                val denom = sqrt(lSqSum * rSqSum)
                if (denom > 0.0001) {
                    correlationMetric = (lrProdSum / denom).toFloat().coerceIn(0f, 1f)
                }

                audioTrack?.write(pcmBuffer, 0, pcmBuffer.size)

                val rms = sqrt(rmsSum / (BUFFER_SIZE * 2)).toFloat()
                computeSpectrumBands(spectrumEnergyAccum, spectrumBands)
                onSpectrumUpdated?.invoke(spectrumBands, rms)
            }
        }
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
        resetAllEffects()
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
    }

    fun togglePlayPause(scope: CoroutineScope): Boolean {
        if (isPlaying.get()) {
            stopPlayback()
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
            return Pair(rawL, rawR)
        }

        // ==========================================
        // STAGE 1: NOISE REDUCTION (PRD 6.10 & 8.1)
        // ==========================================
        var s1L = rawL
        var s1R = rawR

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
        // STAGE 5: VIRTUALIZER SPACE (PRD 6.3 & FR-4)
        // ==========================================
        val (s5L, s5R) = processVirtualizerSpace(s4L, s4R)

        // ==========================================
        // STAGE 6: TIME & SPACE FX: ECHO & REVERB
        // ==========================================
        val (s6L, s6R) = processStereoEchoAndReverb(s5L, s5R)

        // ==========================================
        // STAGE 7: VOLUME BOOST & LIMITER (PRD 6.6 & FR-5)
        // ==========================================
        val (s7L, s7R) = processLoudnessAndLimiter(s6L, s6R)

        return Pair(s7L, s7R)
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
        val ceiling = 10.0.pow(limiterCeilingDb / 20.0).coerceIn(0.70, 0.98)
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

        return Pair(boostedL, boostedR)
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
        val delaySamples = ((echoTimeMs / 1000.0) * SAMPLE_RATE).toInt().coerceIn(1, echoBufferSize - 1)
        val readIdx = (echoWriteIndex - delaySamples + echoBufferSize) % echoBufferSize

        val delayedL = echoBufferL[readIdx]
        val delayedR = echoBufferR[readIdx]

        // 1-pole low-pass filter on feedback repeats (analog tape damping)
        prevEchoFilterL = delayedL * 0.70 + prevEchoFilterL * 0.30
        prevEchoFilterR = delayedR * 0.70 + prevEchoFilterR * 0.30
        if (abs(prevEchoFilterL) < 1e-15) prevEchoFilterL = 0.0
        if (abs(prevEchoFilterR) < 1e-15) prevEchoFilterR = 0.0

        val feedbackFactor = (echoFeedback / 100.0).coerceIn(0.0, 0.96)
        val crossfeed = if (echoPingPong) 0.25 else 0.0

        // Ping-pong crossfeed into delay buffers
        val feedL = inL + (prevEchoFilterL * (1.0 - crossfeed) + prevEchoFilterR * crossfeed) * feedbackFactor
        val feedR = inR + (prevEchoFilterR * (1.0 - crossfeed) + prevEchoFilterL * crossfeed) * feedbackFactor

        echoBufferL[echoWriteIndex] = if (abs(feedL) < 1e-15) 0.0 else feedL
        echoBufferR[echoWriteIndex] = if (abs(feedR) < 1e-15) 0.0 else feedR
        echoWriteIndex = (echoWriteIndex + 1) % echoBufferSize

        val wetFactor = (echoWet / 100.0).coerceIn(0.0, 1.0)
        if (wetFactor <= 0f) {
            return Pair(inL, inR)
        }

        val outL = inL * (1.0 - wetFactor * 0.92) + delayedL * (wetFactor * 2.4)
        val outR = inR * (1.0 - wetFactor * 0.92) + delayedR * (wetFactor * 2.4)

        return Pair(outL, outR)
    }

    fun processStereoReverb(inL: Double, inR: Double): Pair<Double, Double> {
        val wetGain = (reverbWet / 100.0).coerceIn(0.0, 1.0)
        if (wetGain <= 0f) {
            return Pair(inL, inR)
        }

        // Base feedback/damping from the continuous slider, then biased by the
        // selected RoomSize and WallMaterial presets. Room Size changes both the
        // decay character (feedback/damping) AND the physical comb delay length
        // (activeLength below) - a small room and a cathedral don't just ring for
        // different durations, their early reflections arrive at different times.
        val feedback = (0.55 + (reverbRoomSize / 100.0) * 0.30 + roomSize.feedbackBias + wallMaterial.feedbackBias)
            .coerceIn(0.35, 0.985)
        val damping = (0.05 + (reverbDamping / 100.0) * 0.55 + roomSize.dampingBias + wallMaterial.dampingBias)
            .coerceIn(0.02, 0.85)
        val monoIn = (inL + inR) * 0.035 // fuller body feed into the comb network than the original 0.02

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

        // Wall material spectral coloration - applied after the diffusion network,
        // before stereo width mixing, so it colors the whole tail rather than each
        // individual comb's feedback path (cheaper, and audibly equivalent for a
        // single-pole tilt of this kind).
        outL = reverbToneTiltL.process(outL, wallMaterial.toneTilt)
        outR = reverbToneTiltR.process(outR, wallMaterial.toneTilt)

        val width = (reverbWidth / 100.0).coerceIn(0.0, 1.0)
        val wet1 = (1.0 + width) * 0.5
        val wet2 = (1.0 - width) * 0.5
        val wetL = outL * wet1 + outR * wet2
        val wetR = outR * wet1 + outL * wet2

        // Widened intensity range: previous version could never exceed a 65%
        // dry / 1.4x wet blend even at 100% wet, which is why the effect felt
        // minimal regardless of slider position. Now 100% wet nearly fully
        // replaces the dry signal for genuinely huge, dominant reverb character.
        val finalL = inL * (1.0 - wetGain * 0.90) + wetL * (wetGain * 2.6)
        val finalR = inR * (1.0 - wetGain * 0.90) + wetR * (wetGain * 2.6)

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
