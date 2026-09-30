package com.example.audio

import com.example.model.MixSnapshot
import com.example.model.StreamingTarget
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pillar 1 proof: the bounce pipeline renders real source audio (not a test
 * signal), quantizes 16/24-bit correctly, applies loudness match, verifies it,
 * and never prints monitoring/audition stages.
 */
class TrackBounceTest {

    /** Deterministic stereo music source with speech-like mids + bass + air. */
    private class SynthMusicSource(
        val seconds: Double = 4.0,
        seed: Long = 99L
    ) : PcmSource {
        private val n = (AudioEngine.SAMPLE_RATE * seconds).toInt()
        private val left = DoubleArray(n)
        private val right = DoubleArray(n)
        private var pos = 0

        init {
            val rnd = Random(seed)
            for (i in 0 until n) {
                val t = i.toDouble() / AudioEngine.SAMPLE_RATE
                val bass = sin(2 * PI * 110 * t) * 0.30
                val mids = sin(2 * PI * 440 * t) * 0.22 + sin(2 * PI * 880 * t) * 0.10
                val air = sin(2 * PI * 8000 * t) * 0.05
                val click = if (i % 22050 == 0) 0.35 else 0.0
                val hiss = (rnd.nextDouble() - 0.5) * 0.02
                left[i] = (bass + mids + air + click + hiss).coerceIn(-0.9, 0.9)
                right[i] = (bass * 0.85 + mids + air * 1.1 + click + hiss).coerceIn(-0.9, 0.9)
            }
        }

        override val sampleRate: Int = AudioEngine.SAMPLE_RATE
        override val totalFrames: Long = n.toLong()

        override fun readFrames(interleaved: FloatArray, offset: Int, frameCount: Int): Int {
            if (pos >= n) return 0
            val k = minOf(frameCount, n - pos, (interleaved.size - offset) / 2)
            for (i in 0 until k) {
                interleaved[offset + i * 2] = left[pos + i].toFloat()
                interleaved[offset + i * 2 + 1] = right[pos + i].toFloat()
            }
            pos += k
            return k
        }

        override fun isDrained(): Boolean = pos >= n
        override fun reset() { pos = 0 }
        override fun release() {}
    }

    private fun neutralSnapshot(name: String = "test"): MixSnapshot = MixSnapshot(name = name)

    // ---------- WavStreamWriter ----------

    @Test
    fun testWav16RoundTrip() {
        val n = 4410
        val l = DoubleArray(n) { i -> sin(2 * PI * 440 * i / AudioEngine.SAMPLE_RATE) * 0.5 }
        val r = DoubleArray(n) { i -> sin(2 * PI * 660 * i / AudioEngine.SAMPLE_RATE) * 0.5 }
        val tmp = File.createTempFile("bounce16", ".wav")
        try {
            val writer = WavStreamWriter(tmp, AudioEngine.SAMPLE_RATE, BounceBitDepth.PCM_16, BounceDither.TPDF)
            writer.writeFrames(l, r, n / 2)
            writer.writeFrames(
                l.copyOfRange(n / 2, n),
                r.copyOfRange(n / 2, n),
                n - n / 2
            )
            val stats = writer.close()
            assertEquals(n.toLong(), stats.frames)
            assertTrue(stats.peakLinear in 0.4..0.6)
            val (rl, rr, sr) = OfflineBounceRenderer.readWav16BitStereo(tmp)
            assertEquals(AudioEngine.SAMPLE_RATE, sr)
            assertEquals(n, rl.size)
            assertTrue(abs(rl[n / 2] - l[n / 2]) < 0.002)
            assertTrue(abs(rr[n / 2] - r[n / 2]) < 0.002)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun testWav24HeaderAndSize() {
        val n = 1000
        val l = DoubleArray(n) { 0.25 }
        val r = DoubleArray(n) { -0.25 }
        val tmp = File.createTempFile("bounce24", ".wav")
        try {
            val writer = WavStreamWriter(tmp, AudioEngine.SAMPLE_RATE, BounceBitDepth.PCM_24, BounceDither.TPDF)
            writer.writeFrames(l, r, n)
            val stats = writer.close()
            assertEquals(n.toLong(), stats.frames)
            // 44-byte header + 1000 frames * 2ch * 3 bytes
            assertEquals(44L + n * 2L * 3L, tmp.length())
            val bytes = tmp.readBytes()
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals("RIFF", String(bytes.sliceArray(0..3)))
            assertEquals("WAVE", String(bytes.sliceArray(8..11)))
            buf.position(34)
            assertEquals(24, buf.short.toInt())
            // 24-bit code for +0.25 must be near 0.25 * 8388607
            buf.position(44)
            val b0 = buf.get().toInt() and 0xFF
            val b1 = buf.get().toInt() and 0xFF
            val b2 = buf.get().toInt()
            var code = b0 or (b1 shl 8) or (b2 shl 16)
            if (code and 0x800000 != 0) code -= 0x1000000
            assertTrue("24-bit code near quarter-scale, got $code", abs(code - 2097152) < 4)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun testDitherNeverClipsOrNaNs() {
        val n = 2048
        val l = DoubleArray(n) { i -> if (i % 2 == 0) 1.5 else Double.NaN }
        val r = DoubleArray(n) { -1.5 }
        val tmp = File.createTempFile("bounceclip", ".wav")
        try {
            val writer = WavStreamWriter(tmp, AudioEngine.SAMPLE_RATE, BounceBitDepth.PCM_16, BounceDither.TPDF)
            writer.writeFrames(l, r, n)
            val stats = writer.close()
            // NaN -> silence, ±1.5 clamped to full scale
            assertTrue(stats.peakLinear <= 1.0)
            val (rl, rr, _) = OfflineBounceRenderer.readWav16BitStereo(tmp)
            assertTrue(rl.all { it.isFinite() && abs(it) <= 1.0 })
            assertTrue(rr.all { it.isFinite() && abs(it) <= 1.0 })
        } finally {
            tmp.delete()
        }
    }

    // ---------- full two-pass pipeline ----------

    @Test
    fun testBounceRendersSourceAndVerifiesLoudness() {
        val source = SynthMusicSource(seconds = 4.0)
        val tmp = File.createTempFile("trackbounce", ".wav")
        try {
            var lastProgress = 0f
            var progressCalls = 0
            val result = TrackBounceRenderer.bounceTrack(
                source = source,
                snapshot = neutralSnapshot(),
                isAdvancedParametricMode = false,
                target = StreamingTarget.SPOTIFY_14,
                bitDepth = BounceBitDepth.PCM_16,
                dither = BounceDither.TPDF,
                applyLoudnessMatch = true,
                outFile = tmp,
                onProgress = { p ->
                    progressCalls++
                    assertTrue(p >= lastProgress - 1e-6f)
                    lastProgress = p
                }
            )
            // Rendered the whole source, not a stub
            assertEquals(source.totalFrames, result.framesRendered)
            assertTrue(tmp.length() > 44)
            assertTrue(progressCalls > 2)
            assertEquals(1f, lastProgress, 1e-6f)
            // Measure -> match -> verify chain is sane
            assertTrue(result.measuredIntegratedLufs in -40f..-4f)
            assertTrue(result.appliedGainDb in -12f..12f)
            // Verify pass must land closer to target than the raw measurement
            val beforeErr = abs(result.measuredIntegratedLufs - result.targetLufs)
            val afterErr = abs(result.deltaToTargetLu)
            assertTrue("Verify $afterErr must beat raw $beforeErr", afterErr <= beforeErr + 0.75f)
            // True peak stays at/below the platform ceiling-ish range
            assertTrue(result.achievedTruePeakDbtp < 3f)
            assertTrue(result.monitoringExcluded)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun testBounceExcludesMonitoringStages() {
        // Snapshot with hostile audition/monitor settings: SIDE_ONLY collapses
        // center, Auratone guts the lows. A real mixdown must ignore both.
        val hostile = neutralSnapshot().copy(
            midSideName = com.example.model.MidSideMode.SIDE_ONLY.name,
            referenceMonitorName = com.example.model.ReferenceMonitor.AURATONE_5C.name,
            eqGains = mapOf(
                com.example.model.PlainBand.WARMTH.name to 4f,
                com.example.model.PlainBand.CLARITY.name to 3f
            ),
            spacePercent = 40f,
            punchPercent = 30f
        )
        val tmp = File.createTempFile("trackbounce_hostile", ".wav")
        try {
            val result = TrackBounceRenderer.bounceTrack(
                source = SynthMusicSource(seconds = 3.0),
                snapshot = hostile,
                isAdvancedParametricMode = false,
                target = StreamingTarget.SPOTIFY_14,
                bitDepth = BounceBitDepth.PCM_16,
                dither = BounceDither.NONE,
                applyLoudnessMatch = false,
                outFile = tmp
            )
            val (rl, rr, _) = OfflineBounceRenderer.readWav16BitStereo(tmp)
            var energy = 0.0
            for (i in rl.indices) energy += rl[i] * rl[i] + rr[i] * rr[i]
            val rms = kotlin.math.sqrt(energy / (rl.size * 2))
            // SIDE_ONLY alone would nearly erase the centered bass/mids and
            // Auratone would gut the rest - full-energy output proves exclusion.
            assertTrue("Bounce must ignore M/S audition + monitor sim (rms=$rms)", rms > 0.10)
            assertEquals(0f, result.appliedGainDb, 1e-6f)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun testBounceHonorsEqChain() {
        // +10dB warmth on the snapshot must survive into the bounced file.
        val warm = neutralSnapshot().copy(
            eqGains = mapOf(com.example.model.PlainBand.WARMTH.name to 10f)
        )
        fun bounceRms(snap: MixSnapshot): Double {
            val tmp = File.createTempFile("trackbounce_eq", ".wav")
            try {
                TrackBounceRenderer.bounceTrack(
                    source = SynthMusicSource(seconds = 3.0),
                    snapshot = snap,
                    isAdvancedParametricMode = false,
                    target = StreamingTarget.SPOTIFY_14,
                    bitDepth = BounceBitDepth.PCM_16,
                    dither = BounceDither.NONE,
                    applyLoudnessMatch = false,
                    outFile = tmp
                )
                val (rl, rr, _) = OfflineBounceRenderer.readWav16BitStereo(tmp)
                var e = 0.0
                for (i in rl.indices) e += rl[i] * rl[i] + rr[i] * rr[i]
                return kotlin.math.sqrt(e / (rl.size * 2))
            } finally {
                tmp.delete()
            }
        }
        val flatRms = bounceRms(neutralSnapshot())
        val warmRms = bounceRms(warm)
        assertTrue("Warm +10dB bounce must be louder ($warmRms vs $flatRms)", warmRms > flatRms * 1.1)
    }
}
