package com.example.audio

import com.example.model.PlainBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class AudioEngineTest {

    private lateinit var engine: AudioEngine

    @Before
    fun setUp() {
        engine = AudioEngine()
        engine.isBypassed.set(false)
        engine.updateDspCoefficients()
    }

    @Test
    fun testBiquadAntiDenormalAndStability() {
        val biquad = AudioEngine.BiquadState()
        biquad.b0 = 0.5
        biquad.a1 = -0.9

        // Test normal signal processing
        val normalOut = biquad.processL(0.1)
        assertTrue(normalOut > 0.0)

        // Test anti-denormal flush on extremely small values (< 1e-18)
        val tinySignal = 1e-22
        val tinyOut = biquad.processL(tinySignal)
        assertFalse(tinyOut.isNaN())
        assertFalse(tinyOut.isInfinite())

        // Test NaN / Infinity isolation guard
        val nanOut = biquad.processL(Double.NaN)
        assertTrue(nanOut.isNaN()) // Should return input without throwing or permanent state corruption

        val infOut = biquad.processL(Double.POSITIVE_INFINITY)
        assertTrue(infOut.isInfinite())

        // Ensure state recovers after NaN/Inf reset
        val recoveredOut = biquad.processL(0.5)
        assertFalse(recoveredOut.isNaN())
        assertFalse(recoveredOut.isInfinite())
    }

    @Test
    fun testShelvingVsPeakingBiquadCalculations() {
        // Boost Rumble (Low Shelf) and Air (High Shelf)
        engine.eqGains[PlainBand.RUMBLE] = 6f
        engine.eqGains[PlainBand.WARMTH] = 0f
        engine.eqGains[PlainBand.BODY] = 0f
        engine.eqGains[PlainBand.CLARITY] = 0f
        engine.eqGains[PlainBand.AIR] = 6f
        engine.spaceAmount = 0f
        engine.punchAmount = 0f
        engine.loudnessBoost = 0f
        engine.hissRemoval = 0f
        engine.deHumEnabled = false
        engine.deCrackleEnabled = false
        engine.updateDspCoefficients()

        // 30Hz sine wave (in Rumble range)
        val dtLow = 2.0 * PI * 30.0 / AudioEngine.SAMPLE_RATE
        var maxLowOut = 0.0
        for (i in 0 until 500) {
            val inSample = sin(i * dtLow) * 0.2
            val (outL, _) = engine.processStereoSample(inSample, inSample)
            if (i > 200) {
                maxLowOut = maxOf(maxLowOut, abs(outL))
            }
        }
        // With +6dB boost, output amplitude should be larger than 0.2
        assertTrue("Expected boosted sub-bass, got $maxLowOut", maxLowOut > 0.22)

        // 12kHz sine wave (in Air range)
        val dtHigh = 2.0 * PI * 12000.0 / AudioEngine.SAMPLE_RATE
        var maxHighOut = 0.0
        for (i in 0 until 500) {
            val inSample = sin(i * dtHigh) * 0.2
            val (outL, _) = engine.processStereoSample(inSample, inSample)
            if (i > 200) {
                maxHighOut = maxOf(maxHighOut, abs(outL))
            }
        }
        assertTrue("Expected boosted air treble, got $maxHighOut", maxHighOut > 0.22)
    }

    @Test
    fun testDeHumMultiHarmonicNotchFilter() {
        engine.deHumEnabled = true
        engine.humFrequency = 60
        engine.hissRemoval = 0f
        engine.deCrackleEnabled = false
        engine.spaceAmount = 0f
        engine.punchAmount = 0f
        engine.loudnessBoost = 0f
        engine.updateDspCoefficients()

        // Test 60Hz fundamental attenuation
        val dt60 = 2.0 * PI * 60.0 / AudioEngine.SAMPLE_RATE
        var steadyHumOut = 0.0
        for (i in 0 until 10000) {
            val s = sin(i * dt60) * 0.5
            val (outL, _) = engine.processStereoSample(s, s)
            if (i > 8000) {
                steadyHumOut = maxOf(steadyHumOut, abs(outL))
            }
        }

        // Test 1000Hz reference frequency (should pass through unattenuated)
        val dt1k = 2.0 * PI * 1000.0 / AudioEngine.SAMPLE_RATE
        var steady1kOut = 0.0
        for (i in 0 until 10000) {
            val s = sin(i * dt1k) * 0.5
            val (outL, _) = engine.processStereoSample(s, s)
            if (i > 8000) {
                steady1kOut = maxOf(steady1kOut, abs(outL))
            }
        }

        // The 60Hz notch should significantly attenuate 60Hz compared to 1000Hz
        assertTrue("60Hz hum should be heavily notched ($steadyHumOut vs $steady1kOut)", steadyHumOut < steady1kOut * 0.1)
    }

    @Test
    fun testTimeDomainDerivativeDeCrackle() {
        engine.deCrackleEnabled = true
        engine.deHumEnabled = false
        engine.hissRemoval = 0f
        engine.spaceAmount = 0f
        engine.punchAmount = 0f
        engine.loudnessBoost = 0f
        engine.updateDspCoefficients()

        // Feed baseline silence then a single-sample transient spike (0.8 discontinuity)
        engine.processStereoSample(0.0, 0.0)
        engine.processStereoSample(0.0, 0.0)
        val (outL, outR) = engine.processStereoSample(0.8, 0.8)

        // De-crackle interpolates spike with preceding sample (0.7 * prev + 0.3 * curr)
        // 0.7 * 0.0 + 0.3 * 0.8 = 0.24
        assertTrue("Expected spike to be suppressed below 0.5, got $outL", abs(outL) < 0.5)
        assertTrue("Expected spike to be suppressed below 0.5, got $outR", abs(outR) < 0.5)
    }

    @Test
    fun testClarityMacroHarmonicExciterAndDynamicDeHarsher() {
        engine.clarityMacroAmount = 80f
        engine.spaceAmount = 0f
        engine.punchAmount = 0f
        engine.loudnessBoost = 0f
        engine.hissRemoval = 0f
        engine.updateDspCoefficients()

        val dt = 2.0 * PI * 4000.0 / AudioEngine.SAMPLE_RATE
        var maxOut = 0.0
        for (i in 0 until 1000) {
            val s = sin(i * dt) * 0.3
            val (outL, _) = engine.processStereoSample(s, s)
            if (i > 500) {
                maxOut = maxOf(maxOut, abs(outL))
            }
        }
        assertTrue("Clarity exciter should process high band without zeroing", maxOut > 0.1)
    }

    @Test
    fun testRMSDynamicsCompressionAndPunch() {
        engine.punchAmount = 75f
        engine.spaceAmount = 0f
        engine.loudnessBoost = 0f
        engine.hissRemoval = 0f
        engine.updateDspCoefficients()

        // Feed high amplitude sine wave (1.0)
        val dt = 2.0 * PI * 200.0 / AudioEngine.SAMPLE_RATE
        var maxCompressedOut = 0.0
        for (i in 0 until 3000) {
            val s = sin(i * dt) * 1.0
            val (outL, _) = engine.processStereoSample(s, s)
            if (i > 2000) {
                maxCompressedOut = maxOf(maxCompressedOut, abs(outL))
            }
        }
        // Soft-knee compressor and limiter should keep signal controlled
        assertTrue("Signal should be controlled and finite", maxCompressedOut in 0.2..1.5)
    }

    @Test
    fun testTruePeakLimiterPreventsClipping() {
        // Max loudness boost (+12dB)
        engine.loudnessBoost = 100f
        engine.limiterCeilingDb = -0.5f // ~0.944 linear ceiling
        engine.spaceAmount = 0f
        engine.punchAmount = 0f
        engine.updateDspCoefficients()

        val dt = 2.0 * PI * 440.0 / AudioEngine.SAMPLE_RATE
        var peakOut = 0.0
        for (i in 0 until 2000) {
            val s = sin(i * dt) * 0.95
            val (outL, outR) = engine.processStereoSample(s, s)
            peakOut = maxOf(peakOut, abs(outL), abs(outR))
        }

        // Must never exceed 0.98, guaranteeing zero digital clipping (FR-5)
        assertTrue("Peak ($peakOut) must not exceed 0.98 ceiling", peakOut <= 0.98)
    }

    @Test
    fun testInstantABToggleBypass() {
        engine.isBypassed.set(true)
        engine.eqGains[PlainBand.WARMTH] = 12f
        engine.loudnessBoost = 100f
        engine.updateDspCoefficients()

        val testL = 0.423
        val testR = -0.718
        val (outL, outR) = engine.processStereoSample(testL, testR)

        assertEquals("Bypassed output must match raw input", testL, outL, 1e-6)
        assertEquals("Bypassed output must match raw input", testR, outR, 1e-6)
    }

    @Test
    fun testVintageIfyAnalogModulationAndNoise() {
        engine.vintageMode = true
        engine.wowFlutterDepth = 60f
        engine.vintageNoiseLevel = 70f
        engine.humFrequency = 60
        engine.updateDspCoefficients()

        var noiseDetected = false
        for (i in 0 until 500) {
            val (outL, outR) = engine.processStereoSample(0.0, 0.0)
            if (abs(outL) > 0.0001 || abs(outR) > 0.0001) {
                noiseDetected = true
                break
            }
        }
        assertTrue("Vintage-ify should synthesize analog tape hiss, crackle & mains hum", noiseDetected)
    }
}
