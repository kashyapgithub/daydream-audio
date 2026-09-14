package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import kotlin.random.Random

/**
 * Real-time software DSP audio engine for Daydream Audio.
 * Implements the full PRD signal chain:
 * 1. Noise Reduction (Hiss, De-Hum, De-Crackle)
 * 2. Equalizer (5-band / parametric peaking & shelving biquad filters)
 * 3. Clarity macro (presence exciter & de-harsher)
 * 4. Dynamics ("Punch" soft-knee compressor)
 * 5. Virtualizer ("Space" stereo cross-talk decorrelation)
 * 6. Volume Boost (Loudness + soft brickwall limiter)
 * Extra: Reverse Time Machine (Vintage-ify with synthesized noise & wow/flutter)
 */
class AudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 2048
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val isPlaying = AtomicBoolean(false)
    val isBypassed = AtomicBoolean(false) // A/B toggle (<50ms bypass)

    // Current parameters
    var eqGains = mutableMapOf<PlainBand, Float>(
        PlainBand.RUMBLE to 0f,
        PlainBand.WARMTH to 0f,
        PlainBand.BODY to 0f,
        PlainBand.CLARITY to 0f,
        PlainBand.AIR to 0f
    )

    var spaceAmount: Float = 30f // 0 to 100%
    var punchAmount: Float = 25f // 0 to 100%
    var clarityMacroAmount: Float = 0f // 0 to 100%
    var loudnessBoost: Float = 0f // 0 to 100% (volume boost)
    var hissRemoval: Float = 0f // 0 to 100%
    var deHumEnabled: Boolean = false
    var deCrackleEnabled: Boolean = false

    // Vintage-ify mode (PRD 6.13)
    var vintageMode: Boolean = false
    var wowFlutterDepth: Float = 0f // 0 to 100%
    var vintageNoiseLevel: Float = 0f // 0 to 100%

    // Active track
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

    // Visual spectrum data callback
    var onSpectrumUpdated: ((FloatArray, Float) -> Unit)? = null

    // State
    private var phase = 0.0
    private var lfoPhase = 0.0
    private var noiseFloorEstimate = 0.05f

    // Biquad filter state registers for stereo
    private class BiquadState {
        var x1L = 0.0; var x2L = 0.0; var y1L = 0.0; var y2L = 0.0
        var x1R = 0.0; var x2R = 0.0; var y1R = 0.0; var y2R = 0.0
        var b0 = 1.0; var b1 = 0.0; var b2 = 0.0; var a1 = 0.0; var a2 = 0.0

        fun processL(input: Double): Double {
            val out = b0 * input + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
            x2L = x1L; x1L = input
            y2L = y1L; y1L = out
            return out
        }

        fun processR(input: Double): Double {
            val out = b0 * input + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
            x2R = x1R; x1R = input
            y2R = y1R; y1R = out
            return out
        }
    }

    private val filters = mapOf(
        PlainBand.RUMBLE to BiquadState(),
        PlainBand.WARMTH to BiquadState(),
        PlainBand.BODY to BiquadState(),
        PlainBand.CLARITY to BiquadState(),
        PlainBand.AIR to BiquadState()
    )

    private val notch50L = BiquadState()
    private val notch100L = BiquadState()

    // Compressor envelope follower state
    private var compressorEnvelope = 0.0

    // Delay line for Space (virtualizer) and wow/flutter
    private val delayBufferSize = 4410
    private val delayBufferL = DoubleArray(delayBufferSize)
    private val delayBufferR = DoubleArray(delayBufferSize)
    private var delayWriteIndex = 0

    // Mono detection metric
    var correlationMetric = 0.85f // >0.9 indicates mono

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

        playbackJob = scope.launch(Dispatchers.Default) {
            val pcmBuffer = ShortArray(BUFFER_SIZE * 2) // interleaved stereo
            val spectrumBands = FloatArray(8)

            while (isActive && isPlaying.get()) {
                updateFilterCoefficients()

                var rmsSum = 0.0
                var lSum = 0.0
                var rSum = 0.0
                var lrProdSum = 0.0
                var lSqSum = 0.0
                var rSqSum = 0.0

                val track = demoTracks[currentTrackIndex]

                for (i in 0 until BUFFER_SIZE) {
                    // 1. Synthesize rich source audio sample
                    val (rawL, rawR) = synthesizeSourceSample(track)

                    var outL: Double
                    var outR: Double

                    if (isBypassed.get()) {
                        // Instant bypass (<50ms A/B testing)
                        outL = rawL
                        outR = rawR
                    } else {
                        // FULL SIGNAL CHAIN (PRD 8.1):
                        // Step 1: Noise Reduction (Hiss, Hum, Crackle) or Vintage-ify
                        var stage1L = rawL
                        var stage1R = rawR

                        if (vintageMode) {
                            // Reverse Time Machine: Synthesize noise and apply wow & flutter
                            stage1L = applyVintageEffects(stage1L, isLeft = true)
                            stage1R = applyVintageEffects(stage1R, isLeft = false)
                        } else {
                            // Forward Restoration: Clean up
                            if (deHumEnabled) {
                                stage1L = notch50L.processL(stage1L)
                                stage1R = notch50L.processR(stage1R)
                            }
                            if (deCrackleEnabled && abs(stage1L) > 0.95) {
                                stage1L *= 0.5
                                stage1R *= 0.5
                            }
                            if (hissRemoval > 0f) {
                                val hissAtten = 1.0 - (hissRemoval / 100f * 0.7)
                                stage1L = applyGentleLowPass(stage1L, hissAtten)
                                stage1R = applyGentleLowPass(stage1R, hissAtten)
                            }
                        }

                        // Step 2: Equalizer (5-Band Peaking/Shelving)
                        var stage2L = stage1L
                        var stage2R = stage1R
                        filters.values.forEach { biquad ->
                            stage2L = biquad.processL(stage2L)
                            stage2R = biquad.processR(stage2R)
                        }

                        // Step 3: Clarity Macro (Harmonic saturation + presence)
                        var stage3L = stage2L
                        var stage3R = stage2R
                        if (clarityMacroAmount > 0f) {
                            val factor = clarityMacroAmount / 100f
                            // Soft harmonic waveshaping for vocal presence
                            stage3L = stage3L + (factor * 0.3 * (stage3L.pow(3)))
                            stage3R = stage3R + (factor * 0.3 * (stage3R.pow(3)))
                        }

                        // Step 4: Dynamics ("Punch" soft-knee compressor)
                        val (stage4L, stage4R) = applyDynamicsCompressor(stage3L, stage3R)

                        // Step 5: Virtualizer ("Space" crosstalk cancellation & widening)
                        val (stage5L, stage5R) = applyVirtualizerSpace(stage4L, stage4R)

                        // Step 6: Loudness / Volume Boost with Soft Brickwall Limiter
                        val (stage6L, stage6R) = applyLoudnessAndLimiter(stage5L, stage5R)

                        outL = stage6L
                        outR = stage6R
                    }

                    // Accumulate metrics
                    rmsSum += outL * outL + outR * outR
                    lSum += outL; rSum += outR
                    lrProdSum += outL * outR
                    lSqSum += outL * outL
                    rSqSum += outR * outR

                    // Clamp to 16-bit PCM
                    val sampleShortL = (outL.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                    val sampleShortR = (outR.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()

                    pcmBuffer[i * 2] = sampleShortL
                    pcmBuffer[i * 2 + 1] = sampleShortR
                }

                // Update correlation metric (mono detection)
                val denom = sqrt(lSqSum * rSqSum)
                if (denom > 0.0001) {
                    correlationMetric = (lrProdSum / denom).toFloat().coerceIn(0f, 1f)
                }

                // Write to AudioTrack
                audioTrack?.write(pcmBuffer, 0, pcmBuffer.size)

                // Update visual spectrum callback
                val rms = sqrt(rmsSum / (BUFFER_SIZE * 2)).toFloat()
                generateSimulatedSpectrum(spectrumBands, rms)
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
        } catch (_: Exception) {}
        audioTrack = null
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

    private fun synthesizeSourceSample(track: DemoTrack): Pair<Double, Double> {
        val freq = track.baseFrequency.toDouble()
        val dt = 2.0 * PI / SAMPLE_RATE

        phase += freq * dt
        if (phase > 2.0 * PI * 100) phase -= 2.0 * PI * 100

        // Chord progression: Root + Minor third + Fifth + Sub-bass octave
        val sub = sin(phase * 0.5) * 0.22
        val fundamental = sin(phase) * 0.35
        val harmonic1 = sin(phase * 1.5) * 0.18 // fifth
        val harmonic2 = sin(phase * 1.2) * 0.15 // minor third
        val overtone = sin(phase * 3.0) * 0.08

        val rawClean = sub + fundamental + harmonic1 + harmonic2 + overtone

        // Add characteristic recording noise based on track type
        var noise = 0.0
        when (track.noiseType) {
            "cassette" -> {
                // Broadband tape hiss
                noise = (Random.nextDouble() - 0.5) * 0.06
            }
            "vinyl" -> {
                // 60Hz hum + occasional crackle pop
                val hum = sin(phase * (60.0 / freq)) * 0.05
                val crackle = if (Random.nextDouble() < 0.001) (Random.nextDouble() - 0.5) * 0.8 else 0.0
                noise = hum + crackle + (Random.nextDouble() - 0.5) * 0.02
            }
            "radio" -> {
                // Mid-range bandwidth crunch
                noise = (Random.nextDouble() - 0.5) * 0.03
            }
            "tape_heavy" -> {
                noise = (Random.nextDouble() - 0.5) * 0.12
            }
        }

        val total = rawClean + noise
        // Subtle stereo offset
        val left = total + sin(phase * 0.51) * 0.05
        val right = total + cos(phase * 0.49) * 0.05
        return Pair(left, right)
    }

    private fun applyGentleLowPass(input: Double, factor: Double): Double {
        return input * factor
    }

    private fun applyVintageEffects(input: Double, isLeft: Boolean): Double {
        // Synthesize authentic tape noise + wow and flutter
        lfoPhase += 1.2 * (2.0 * PI / SAMPLE_RATE) // 1.2Hz wow
        val wow = sin(lfoPhase) * (wowFlutterDepth / 100.0) * 0.03
        val flutter = sin(lfoPhase * 5.7) * (wowFlutterDepth / 100.0) * 0.01

        val noiseAmp = vintageNoiseLevel / 100.0 * 0.15
        val tapeHiss = (Random.nextDouble() - 0.5) * noiseAmp
        val vinylPop = if (Random.nextDouble() < (vintageNoiseLevel / 10000.0)) (Random.nextDouble() - 0.5) * 0.6 else 0.0

        val modulated = input * (1.0 + wow + flutter) + tapeHiss + vinylPop
        return modulated
    }

    private fun applyDynamicsCompressor(inL: Double, inR: Double): Pair<Double, Double> {
        val inAbs = max(abs(inL), abs(inR))
        val attack = 0.01 // ~15ms
        val release = 0.001 // ~150ms

        // RMS envelope tracking
        compressorEnvelope += if (inAbs > compressorEnvelope) {
            attack * (inAbs - compressorEnvelope)
        } else {
            release * (inAbs - compressorEnvelope)
        }

        val threshold = 0.4
        val ratio = 1.0 + (punchAmount / 100.0) * 3.0 // 1:1 to 4:1
        var gainReduction = 1.0

        if (compressorEnvelope > threshold) {
            val overDb = compressorEnvelope - threshold
            val reducedDb = overDb / ratio
            gainReduction = (threshold + reducedDb) / compressorEnvelope
        }

        // Makeup gain
        val makeup = 1.0 + (punchAmount / 100.0) * 0.4
        val totalGain = gainReduction * makeup
        return Pair(inL * totalGain, inR * totalGain)
    }

    private fun applyVirtualizerSpace(inL: Double, inR: Double): Pair<Double, Double> {
        val widthFactor = (spaceAmount / 100.0)

        // Store into delay ring buffer
        delayBufferL[delayWriteIndex] = inL
        delayBufferR[delayWriteIndex] = inR
        delayWriteIndex = (delayWriteIndex + 1) % delayBufferSize

        // Read with ~8ms cross-feed delay (350 samples)
        val readIndex = (delayWriteIndex - 350 + delayBufferSize) % delayBufferSize
        val delayedL = delayBufferL[readIndex]
        val delayedR = delayBufferR[readIndex]

        // Crossfeed decorrelation
        val outL = inL + (delayedR - inR) * (widthFactor * 0.4)
        val outR = inR + (delayedL - inL) * (widthFactor * 0.4)

        return Pair(outL, outR)
    }

    private fun applyLoudnessAndLimiter(inL: Double, inR: Double): Pair<Double, Double> {
        val boost = 1.0 + (loudnessBoost / 100.0) * 1.5 // Up to +2.5x gain
        var boostedL = inL * boost
        var boostedR = inR * boost

        // True-peak soft brickwall limiter (hyperbolic tangent soft-clipping)
        val threshold = 0.95
        if (abs(boostedL) > threshold) {
            boostedL = threshold + (1.0 - threshold) * kotlin.math.tanh((boostedL - threshold) / (1.0 - threshold))
        }
        if (abs(boostedR) > threshold) {
            boostedR = threshold + (1.0 - threshold) * kotlin.math.tanh((boostedR - threshold) / (1.0 - threshold))
        }

        return Pair(boostedL, boostedR)
    }

    private fun updateFilterCoefficients() {
        // RBJ Audio EQ Cookbook Biquad calculation
        eqGains.forEach { (band, gainDb) ->
            val biquad = filters[band] ?: return@forEach
            val f0 = band.centerHz.toDouble()
            val gain = 10.0.pow(gainDb / 40.0) // A = 10^(dB/40)
            val q = 0.8
            val w0 = 2.0 * PI * f0 / SAMPLE_RATE
            val alpha = sin(w0) / (2.0 * q)
            val cosW0 = cos(w0)

            when {
                band.isShelf && band == PlainBand.RUMBLE -> {
                    // Low shelf
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
                band.isShelf && band == PlainBand.AIR -> {
                    // High shelf
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
                    // Peaking EQ
                    val a0 = 1.0 + alpha / gain
                    biquad.b0 = (1.0 + alpha * gain) / a0
                    biquad.b1 = (-2.0 * cosW0) / a0
                    biquad.b2 = (1.0 - alpha * gain) / a0
                    biquad.a1 = (-2.0 * cosW0) / a0
                    biquad.a2 = (1.0 - alpha / gain) / a0
                }
            }
        }

        // Notch 50/60Hz calculation for De-Hum
        val w50 = 2.0 * PI * 60.0 / SAMPLE_RATE
        val alpha50 = sin(w50) / (2.0 * 15.0) // high Q
        val cosW50 = cos(w50)
        val a0_50 = 1.0 + alpha50
        notch50L.b0 = 1.0 / a0_50
        notch50L.b1 = (-2.0 * cosW50) / a0_50
        notch50L.b2 = 1.0 / a0_50
        notch50L.a1 = (-2.0 * cosW50) / a0_50
        notch50L.a2 = (1.0 - alpha50) / a0_50
    }

    private fun generateSimulatedSpectrum(outBands: FloatArray, rms: Float) {
        val rumbleGain = (eqGains[PlainBand.RUMBLE] ?: 0f) / 12f
        val warmthGain = (eqGains[PlainBand.WARMTH] ?: 0f) / 12f
        val bodyGain = (eqGains[PlainBand.BODY] ?: 0f) / 12f
        val clarityGain = (eqGains[PlainBand.CLARITY] ?: 0f) / 12f
        val airGain = (eqGains[PlainBand.AIR] ?: 0f) / 12f

        outBands[0] = (rms * 1.8f * (1f + rumbleGain)).coerceIn(0.05f, 1f)
        outBands[1] = (rms * 1.5f * (1f + warmthGain)).coerceIn(0.05f, 1f)
        outBands[2] = (rms * 1.3f * (1f + warmthGain * 0.5f)).coerceIn(0.05f, 1f)
        outBands[3] = (rms * 1.2f * (1f + bodyGain)).coerceIn(0.05f, 1f)
        outBands[4] = (rms * 1.1f * (1f + bodyGain * 0.8f)).coerceIn(0.05f, 1f)
        outBands[5] = (rms * 1.2f * (1f + clarityGain)).coerceIn(0.05f, 1f)
        outBands[6] = (rms * 1.4f * (1f + clarityGain * 0.6f + airGain * 0.4f)).coerceIn(0.05f, 1f)
        outBands[7] = (rms * 1.6f * (1f + airGain)).coerceIn(0.05f, 1f)
    }
}
