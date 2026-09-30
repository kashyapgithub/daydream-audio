package com.example.audio

import com.example.model.MixSnapshot
import com.example.model.PlainBand
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class ProStudioWorkflowTest {

    private lateinit var engine: AudioEngine

    @Before
    fun setUp() {
        engine = AudioEngine()
        engine.isBypassed.set(false)
        engine.updateDspCoefficients()
    }

    @Test
    fun testSnapshotRoundTripPreservesProcessing() {
        engine.eqGains[PlainBand.WARMTH] = 6f
        engine.spaceAmount = 70f
        engine.punchAmount = 50f
        engine.compThresholdDb = -12f
        engine.compRatio = 4f
        engine.updateDspCoefficients()

        val snap = engine.captureSnapshot("Slot A")
        assertEquals(6f, snap.eqGains[PlainBand.WARMTH.name] ?: 0f, 1e-5f)
        assertEquals(70f, snap.spacePercent, 1e-5f)

        // Mutate then restore
        engine.eqGains[PlainBand.WARMTH] = -6f
        engine.spaceAmount = 0f
        engine.updateDspCoefficients()
        engine.restoreSnapshot(snap)

        assertEquals(6f, engine.eqGains[PlainBand.WARMTH] ?: 0f, 1e-5f)
        assertEquals(70f, engine.spaceAmount, 1e-5f)

        // Processing must be finite after restore
        val (l, r) = engine.processStereoSample(0.3, 0.3)
        assertFalse(l.isNaN())
        assertFalse(r.isInfinite())
    }

    @Test
    fun testLoudnessGainMathClampedAndTruePeakSafe() {
        // Needs +6dB to reach target
        assertEquals(6f, MixSnapshot.loudnessGainDb(-20f, -14f), 1e-5f)
        // Clamped to +/-12
        assertEquals(12f, MixSnapshot.loudnessGainDb(-40f, -14f), 1e-5f)
        assertEquals(-12f, MixSnapshot.loudnessGainDb(0f, -14f), 1e-5f)
        // Invalid reading -> 0
        assertEquals(0f, MixSnapshot.loudnessGainDb(-90f, -14f), 1e-5f)
        // True-peak safety: only 0.5dB headroom, wanted +6 -> apply +0.5
        assertEquals(0.5f, MixSnapshot.safeLoudnessGainDb(-20f, -14f, -1.5f, -1.0f), 1e-5f)
        // Cuts are never reduced by ceiling
        assertEquals(-4f, MixSnapshot.safeLoudnessGainDb(-10f, -14f, -0.2f, -1.0f), 1e-5f)
    }

    @Test
    fun testOfflineRenderAndWavRoundTrip() {
        engine.spaceAmount = 0f
        engine.punchAmount = 0f
        engine.loudnessBoost = 0f
        engine.updateDspCoefficients()

        val n = 4410
        val inL = DoubleArray(n) { i -> sin(2.0 * PI * 440.0 * i / AudioEngine.SAMPLE_RATE) * 0.5 }
        val inR = DoubleArray(n) { i -> sin(2.0 * PI * 440.0 * i / AudioEngine.SAMPLE_RATE) * 0.5 }
        val (outL, outR) = OfflineBounceRenderer.renderOffline(inL, inR) { l, r ->
            engine.processStereoSample(l, r)
        }
        assertEquals(n, outL.size)
        var energy = 0.0
        for (v in outL) {
            assertFalse(v.isNaN())
            energy += v * v
        }
        assertTrue("Rendered bounce must contain energy", energy > 1.0)

        val tmp = File.createTempFile("bounce_test", ".wav")
        try {
            OfflineBounceRenderer.writeWav16Bit(tmp, outL, outR)
            assertTrue(tmp.length() > 44)
            val (rl, rr, sr) = OfflineBounceRenderer.readWav16BitStereo(tmp)
            assertEquals(AudioEngine.SAMPLE_RATE, sr)
            assertEquals(outL.size, rl.size)
            // 16-bit quantization tolerance
            assertTrue(abs(rl[n / 2] - outL[n / 2]) < 0.001)
            assertTrue(abs(rr[n / 2] - outR[n / 2]) < 0.001)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun testPeakHoldAndClipCounter() {
        engine.resetPeakHold()
        assertEquals(-90f, engine.truePeakHoldDbtp, 1e-5f)
        assertEquals(0, engine.clipCount)

        // Loud 0dBFS sine through limiter-bypassed loud path: force overs
        // by feeding processLufsSample directly with hot signal.
        for (i in 0 until AudioEngine.SAMPLE_RATE) {
            engine.processLufsSample(1.2, 1.2)
        }
        engine.calculateLufsMetrics()
        assertTrue("Hold must capture hot peak", engine.truePeakHoldDbtp > -1f)
        assertTrue("Clip counter must increment on >=0dBFS", engine.clipCount > 0)

        engine.resetPeakHold()
        assertEquals(0, engine.clipCount)
    }

    @Test
    fun testApplyGainDbScalesPeakPredictably() {
        val l = doubleArrayOf(0.25, -0.25, 0.5)
        val r = doubleArrayOf(0.25, -0.25, 0.5)
        OfflineBounceRenderer.applyGainDb(l, r, 6.02f)
        // +6.02dB ~= x2
        assertEquals(0.5, l[0], 0.01)
        assertEquals(1.0, l[2], 0.02)
    }
}
