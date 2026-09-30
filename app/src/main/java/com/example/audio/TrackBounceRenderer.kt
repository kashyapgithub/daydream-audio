package com.example.audio

import com.example.model.MidSideMode
import com.example.model.MixSnapshot
import com.example.model.ReferenceMonitor
import com.example.model.StreamingTarget
import java.io.File
import kotlin.math.pow

/**
 * Pillar 1: real track bounce.
 *
 * Renders the actual loaded track (not a test signal) through a dedicated
 * [AudioEngine] configured from a [MixSnapshot]:
 *
 * - Pass 1 decodes + processes the whole track while measuring gated
 *   integrated LUFS (split off from live playback state entirely).
 * - Pass 2 re-decodes, applies the true-peak-safe loudness-match gain, and
 *   streams 16/24-bit dithered PCM to disk in chunks (no full-track buffers).
 * - The post-gain stream is measured again: the result carries the achieved
 *   integrated LUFS and true peak, so the UI can prove the target was hit.
 *
 * Studio rule baked in: monitoring/audition stages (reference monitor sims,
 * mid/side audition matrix) are NEVER bounced - they are listening tools, the
 * same way solo/mute never print to a mixdown.
 */
object TrackBounceRenderer {

    data class TrackBounceResult(
        val filePath: String,
        val framesRendered: Long,
        val bitDepth: BounceBitDepth,
        val sampleRateHz: Int,
        /** Gated integrated LUFS of the processed mix before match gain. */
        val measuredIntegratedLufs: Float,
        val appliedGainDb: Float,
        /** Gated integrated LUFS re-measured after match gain (verify pass). */
        val achievedIntegratedLufs: Float,
        val achievedTruePeakDbtp: Float,
        val targetLufs: Float,
        val targetTruePeakDbtp: Float,
        /** True: reference-monitor + M/S audition were excluded from the print. */
        val monitoringExcluded: Boolean = true
    ) {
        val deltaToTargetLu: Float get() = achievedIntegratedLufs - targetLufs
    }

    private const val CHUNK_FRAMES = 4096
    private const val MAX_CONSECUTIVE_ZEROS = 200

    private fun bounceEngine(
        snapshot: MixSnapshot,
        advancedParametric: Boolean,
        sampleRateHz: Int
    ): AudioEngine {
        return AudioEngine().apply {
            isBypassed.set(false)
            restoreSnapshot(snapshot)
            setEngineSampleRate(sampleRateHz)
            isAdvancedParametricMode = advancedParametric
            // Monitoring/audition never prints to a mixdown.
            midSideMode = MidSideMode.STEREO
            referenceMonitor = ReferenceMonitor.FLAT
            gainMatchedAB = false
            testToneMode = com.example.model.TestToneMode.OFF
            resetPeakHold()
            updateDspCoefficients()
        }
    }

    fun bounceTrack(
        source: PcmSource,
        snapshot: MixSnapshot,
        isAdvancedParametricMode: Boolean,
        target: StreamingTarget,
        bitDepth: BounceBitDepth,
        dither: BounceDither,
        applyLoudnessMatch: Boolean,
        outFile: File,
        onProgress: (fraction: Float) -> Unit = {},
        sampleRateHz: Int = AudioEngine.globalSampleRate
    ): TrackBounceResult {
        val total = source.totalFrames.takeIf { it > 0 } ?: -1L
        fun reportPass1(done: Long) {
            if (total > 0) onProgress((0.45f * done.toFloat() / total.toFloat()).coerceIn(0f, 0.45f))
        }
        fun reportPass2(done: Long) {
            if (total > 0) onProgress((0.45f + 0.55f * done.toFloat() / total.toFloat()).coerceIn(0.45f, 1f))
        }

        val interleaved = FloatArray(CHUNK_FRAMES * 2)
        val chunkL = DoubleArray(CHUNK_FRAMES)
        val chunkR = DoubleArray(CHUNK_FRAMES)

        // ---------- Pass 1: process + measure ----------
        val measureEngine = bounceEngine(snapshot, isAdvancedParametricMode, sampleRateHz)
        source.reset()
        var zeros = 0
        var pass1Frames = 0L
        while (true) {
            val n = source.readFrames(interleaved, 0, CHUNK_FRAMES)
            if (n <= 0) {
                if (source.isDrained() || ++zeros > MAX_CONSECUTIVE_ZEROS) break else continue
            }
            zeros = 0
            for (i in 0 until n) {
                val (l, r) = measureEngine.processStereoSample(
                    interleaved[i * 2].toDouble(),
                    interleaved[i * 2 + 1].toDouble()
                )
                measureEngine.processLufsSample(l, r)
            }
            pass1Frames += n
            reportPass1(pass1Frames)
        }
        val measured = measureEngine.calculateLufsMetrics()
        val wantedGain = if (applyLoudnessMatch) {
            MixSnapshot.safeLoudnessGainDb(
                currentIntegratedLufs = measured.integratedLufs,
                targetLufs = target.targetLufs,
                currentTruePeakDbtp = measured.truePeakDbtp,
                ceilingDbtp = target.maxTruePeakDbtp
            )
        } else 0f
        val gainLin = 10.0.pow(wantedGain / 20.0)

        // ---------- Pass 2: process + gain + measure + write ----------
        val printEngine = bounceEngine(snapshot, isAdvancedParametricMode, sampleRateHz)
        source.reset()
        val writer = WavStreamWriter(outFile, sampleRateHz, bitDepth, dither)
        zeros = 0
        var pass2Frames = 0L
        while (true) {
            val n = source.readFrames(interleaved, 0, CHUNK_FRAMES)
            if (n <= 0) {
                if (source.isDrained() || ++zeros > MAX_CONSECUTIVE_ZEROS) break else continue
            }
            zeros = 0
            for (i in 0 until n) {
                val (l, r) = printEngine.processStereoSample(
                    interleaved[i * 2].toDouble(),
                    interleaved[i * 2 + 1].toDouble()
                )
                chunkL[i] = (l * gainLin).coerceIn(-4.0, 4.0)
                chunkR[i] = (r * gainLin).coerceIn(-4.0, 4.0)
                printEngine.processLufsSample(chunkL[i], chunkR[i])
            }
            writer.writeFrames(chunkL, chunkR, n)
            pass2Frames += n
            reportPass2(pass2Frames)
        }
        val stats = writer.close()
        val achieved = printEngine.calculateLufsMetrics()
        onProgress(1f)

        return TrackBounceResult(
            filePath = outFile.absolutePath,
            framesRendered = stats.frames,
            bitDepth = bitDepth,
            sampleRateHz = sampleRateHz,
            measuredIntegratedLufs = measured.integratedLufs,
            appliedGainDb = wantedGain,
            achievedIntegratedLufs = achieved.integratedLufs,
            achievedTruePeakDbtp = achieved.truePeakDbtp,
            targetLufs = target.targetLufs,
            targetTruePeakDbtp = target.maxTruePeakDbtp
        )
    }
}
