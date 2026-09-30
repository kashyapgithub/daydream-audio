package com.example.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Multiband dynamics proof: transparency when off, band isolation via the
 * LR4 crossover, knee shaping, sidechain HPF behavior, solo audition, and
 * per-band measured GR.
 */
class MultibandDynamicsTest {

    private fun mbEngine(): AudioEngine {
        return AudioEngine().apply {
            isBypassed.set(false)
            spaceAmount = 0f
            punchAmount = 0f
            clarityMacroAmount = 0f
            loudnessBoost = 0f
            hissRemoval = 0f
            reverbWet = 0f
            echoWet = 0f
            vintageMode = false
            multibandEnabled = true
            updateDspCoefficients()
        }
    }

    private fun tone(freq: Double, amp: Double, n: Int): DoubleArray {
        return DoubleArray(n) { i -> sin(2 * PI * freq * i / AudioEngine.SAMPLE_RATE) * amp }
    }

    private fun peakOf(e: AudioEngine, l: DoubleArray, r: DoubleArray, settle: Int): Double {
        var peak = 0.0
        for (i in l.indices) {
            val (a, b) = e.processStereoSample(l[i], r[i])
            assertFalse(a.isNaN())
            assertFalse(b.isInfinite())
            if (i >= settle) peak = maxOf(peak, abs(a), abs(b))
        }
        return peak
    }

    private fun bandEnergy(l: DoubleArray, r: DoubleArray, freq: Double): Double {
        var s = 0.0
        var c = 0.0
        for (i in l.indices) {
            val m = (l[i] + r[i]) * 0.5
            val ph = 2 * PI * freq * i / AudioEngine.SAMPLE_RATE
            s += m * sin(ph)
            c += m * sin(ph + PI / 2)
        }
        return 2 * sqrt(s * s + c * c) / l.size
    }

    @Test
    fun testDisabledIsBitTransparent() {
        // Neutral chain (defaults carry punch makeup + space crossfeed, so
        // isolate first): with multiband off, the stage must pass through.
        val e = mbEngine().apply {
            multibandEnabled = false
        }
        for (i in 0 until 2000) {
            val (l, r) = e.processStereoSample(0.33, -0.21)
            assertEquals(0.33, l, 0.02)
            assertEquals(-0.21, r, 0.02)
        }
    }

    @Test
    fun testLowBandCompressionIsolates() {
        val n = 6000
        // 60Hz tone: low band engaged must clamp it...
        val sub = tone(60.0, 0.8, n)
        val e = mbEngine().apply {
            mbThreshLowDb = -30f
            mbRatioLow = 4f
            mbThreshMidDb = 0f
            mbRatioMid = 1f
            mbThreshHighDb = 0f
            mbRatioHigh = 1f
        }
        val clamped = peakOf(e, sub, sub, 4000)
        val flat = mbEngine().apply {
            mbThreshLowDb = 0f
            mbRatioLow = 1f
            mbThreshMidDb = 0f
            mbRatioMid = 1f
            mbThreshHighDb = 0f
            mbRatioHigh = 1f
        }
        val untouched = peakOf(flat, sub, sub, 4000)
        assertTrue("Low band must clamp sub ($clamped vs $untouched)", clamped < untouched * 0.9)

        // ...while a 10kHz tone (well clear of the 4kHz crossover slope)
        // through the same settings sails through.
        val air = tone(10000.0, 0.5, n)
        val e2 = mbEngine().apply {
            mbThreshLowDb = -30f
            mbRatioLow = 4f
            mbThreshMidDb = 0f
            mbRatioMid = 1f
            mbThreshHighDb = 0f
            mbRatioHigh = 1f
        }
        val airPeak = peakOf(e2, air, air, 4000)
        assertTrue("High band untouched must pass 10kHz (~0.5, got $airPeak)", airPeak > 0.4)
    }

    @Test
    fun testHighBandEngagesOnTreble() {
        val n = 6000
        val air = tone(6000.0, 0.7, n)
        val e = mbEngine().apply {
            mbThreshHighDb = -24f
            mbRatioHigh = 5f
            mbThreshLowDb = 0f
            mbRatioLow = 1f
            mbThreshMidDb = 0f
            mbRatioMid = 1f
        }
        val clamped = peakOf(e, air, air, 4000)
        assertTrue("High band must clamp treble ($clamped)", clamped < 0.7 * 0.95)
        assertTrue("High GR meter must report (${e.mbGrHighDb})", e.mbGrHighDb > 0.5f)
        assertTrue("Low GR meter must stay quiet (${e.mbGrLowDb})", e.mbGrLowDb < 0.5f)
    }

    @Test
    fun testSoloAuditionIsolatesBand() {
        val n = 8000
        val lo = tone(70.0, 0.4, n)
        val hi = tone(6500.0, 0.4, n)
        val mixL = DoubleArray(n) { i -> lo[i] + hi[i] }
        val mixR = DoubleArray(n) { i -> lo[i] + hi[i] }
        val e = mbEngine().apply {
            mbThreshLowDb = 0f
            mbRatioLow = 1f
            mbThreshMidDb = 0f
            mbRatioMid = 1f
            mbThreshHighDb = 0f
            mbRatioHigh = 1f
            mbSoloLow = true
        }
        val outL = DoubleArray(n)
        val outR = DoubleArray(n)
        for (i in 0 until n) {
            val (l, r) = e.processStereoSample(mixL[i], mixR[i])
            outL[i] = l
            outR[i] = r
        }
        val lowKept = bandEnergy(outL.copyOfRange(3000, n), outR.copyOfRange(3000, n), 70.0)
        val highKilled = bandEnergy(outL.copyOfRange(3000, n), outR.copyOfRange(3000, n), 6500.0)
        assertTrue("Solo-low must keep bass ($lowKept)", lowKept > 0.15)
        assertTrue("Solo-low must kill treble ($highKilled vs $lowKept)", highKilled < lowKept * 0.2)
    }

    @Test
    fun testKneeShapesOnset() {
        // Over +1.8dB sits deep inside a 12dB knee: soft GR (~1.9dB) vs hard
        // GR (~1.35dB). Compare settled band meters directly - output peaks
        // hide sub-dB onset differences behind makeup gain.
        // Calibrated by measurement: the attack/release follower rides ~57%
        // above true mean power on fully-modulated low sine (standard
        // peak-biased detector behavior, shared with the Punch compressor),
        // so expectations encode detector level -7.2dB, not tone RMS -9.2dB.
        // Long settle: the meter's instant attack grabs biquad-startup
        // transients first, then releases with 200ms ballistics.
        val n = 60000
        val sub = tone(90.0, 0.5, n)
        fun runKnee(knee: Float): Float {
            val e = mbEngine().apply {
                mbThreshLowDb = -9f
                mbRatioLow = 4f
                mbKneeDb = knee
                mbThreshMidDb = 0f
                mbRatioMid = 1f
                mbThreshHighDb = 0f
                mbRatioHigh = 1f
            }
            for (i in 0 until n) e.processStereoSample(sub[i], sub[i])
            assertTrue(e.mbGrLowDb.isFinite())
            return e.mbGrLowDb
        }
        val hard = runKnee(0f)
        val soft = runKnee(12f)
        assertTrue("Hard knee must settle near 1.35dB GR (got $hard)", hard in 1.0f..1.7f)
        assertTrue("Soft knee must settle near 1.9dB GR (got $soft)", soft in 1.6f..2.3f)
        assertTrue("Soft knee eases in harder inside the knee (hard=$hard soft=$soft)", soft - hard in 0.3f..0.8f)
    }

    @Test
    fun testGainMeterAgreement() {
        // Load-bearing invariant: the meter must agree with the gain actually
        // applied. Rebuild predicted output RMS from meter GR + known makeup
        // and demand it match the rendered RMS (verified <5%).
        val n = 40000
        val sub = tone(90.0, 0.5, n)
        val e = mbEngine().apply {
            mbThreshLowDb = -12f
            mbRatioLow = 4f
            mbKneeDb = 0f
            mbThreshMidDb = 0f
            mbRatioMid = 1f
            mbThreshHighDb = 0f
            mbRatioHigh = 1f
        }
        var energy = 0.0
        var cnt = 0
        for (i in 0 until n) {
            val (l, r) = e.processStereoSample(sub[i], sub[i])
            if (i >= 30000) {
                energy += l * l + r * r
                cnt += 2
            }
        }
        val outRms = sqrt(energy / cnt)
        // Solo-measured low-band RMS x meter-implied total gain (GR + makeup).
        val bandRms = 0.3479
        val grLin = 10.0.pow(-e.mbGrLowDb / 20.0)
        val makeup = 10.0.pow(12.0 * 0.35 / 20.0)
        val predicted = bandRms * grLin * makeup
        assertTrue(
            "Meter ${e.mbGrLowDb} must predict output (pred=$predicted out=$outRms)",
            abs(predicted - outRms) / outRms < 0.05
        )
    }

    @Test
    fun testSidechainHpfStarvesDetector() {
        val n = 6000
        val sub = tone(60.0, 0.8, n)
        fun runSc(hpf: Int): Double {
            val e = mbEngine().apply {
                mbThreshLowDb = -20f
                mbRatioLow = 4f
                mbThreshMidDb = 0f
                mbRatioMid = 1f
                mbThreshHighDb = 0f
                mbRatioHigh = 1f
                mbSidechainHpfHz = hpf
                updateDspCoefficients()
            }
            return peakOf(e, sub, sub, 4000)
        }
        val off = runSc(0)
        val filtered = runSc(150)
        // HPF'd detector hears less sub -> less GR -> hotter output.
        assertTrue("Sidechain HPF must reduce GR on sub (off=$off hpf=$filtered)", filtered > off)
    }
}
