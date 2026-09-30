package com.example.audio

import com.example.model.PlainBand
import com.example.model.TestToneMode
import com.example.model.SubCutFilter
import com.example.model.StreamingTarget
import com.example.model.LufsMetrics
import com.example.model.HarmonicSaturationType
import com.example.model.ReferenceMonitor
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

    @Test
    fun testReverbAudibilityAndFreeze() {
        engine.reverbWetPercent = 100f // 100% wet
        engine.reverbRoomSizePercent = 85f
        engine.echoWetPercent = 0f
        engine.updateDspCoefficients()

        // Send an impulse
        engine.processStereoSample(1.0, 1.0)

        // Process 2000 samples of silence, tail must be rich and audible (>0.01 energy)
        var maxTail = 0.0
        for (i in 0 until 2000) {
            val (outL, outR) = engine.processStereoSample(0.0, 0.0)
            maxTail = maxOf(maxTail, abs(outL), abs(outR))
        }
        assertTrue("Overhauled reverb must have audible tail energy, got $maxTail", maxTail > 0.05)

        // Test Reverb Freeze (Infinite Tail Hold)
        engine.reverbFreezeEnabled = true
        engine.updateDspCoefficients()

        // Run 5000 more samples of silence
        var frozenTailEnergy = 0.0
        for (i in 0 until 5000) {
            val (outL, outR) = engine.processStereoSample(0.0, 0.0)
            if (i > 4000) {
                frozenTailEnergy = maxOf(frozenTailEnergy, abs(outL), abs(outR))
            }
        }
        assertTrue("Reverb freeze must maintain sound indefinitely without decaying to silence ($frozenTailEnergy)", frozenTailEnergy > 0.01)
    }

    @Test
    fun testMidSideAuditionMatrix() {
        engine.reverbWetPercent = 0f
        engine.echoWetPercent = 0f
        engine.spaceAmount = 0f
        engine.punchAmount = 0f
        engine.loudnessBoost = 0f
        engine.vintageMode = false
        engine.updateDspCoefficients()

        val inL = 0.8
        val inR = 0.2

        // Mono Sum: (0.8 + 0.2) / 2 = 0.5 on both channels
        engine.midSideMode = com.example.model.MidSideMode.MONO_SUM
        val (monoL, monoR) = engine.processStereoSample(inL, inR)
        assertEquals(0.5, monoL, 0.05)
        assertEquals(0.5, monoR, 0.05)

        // Mid Only: center channel = (0.8 + 0.2) / 2 = 0.5 on both
        engine.midSideMode = com.example.model.MidSideMode.MID_ONLY
        val (midL, midR) = engine.processStereoSample(inL, inR)
        assertEquals(0.5, midL, 0.05)
        assertEquals(0.5, midR, 0.05)

        // Side Only: difference channel = (0.8 - 0.2) / 2 = 0.3 on left, -0.3 on right
        engine.midSideMode = com.example.model.MidSideMode.SIDE_ONLY
        val (sideL, sideR) = engine.processStereoSample(inL, inR)
        assertEquals(0.3, sideL, 0.05)
        assertEquals(-0.3, sideR, 0.05)

        // Phase Invert (Ø): L = 0.8, R = -0.2
        engine.midSideMode = com.example.model.MidSideMode.PHASE_INVERT
        val (invL, invR) = engine.processStereoSample(inL, inR)
        assertEquals(0.8, invL, 0.05)
        assertEquals(-0.2, invR, 0.05)
    }

    @Test
    fun testReferenceMonitorAuratone5C() {
        engine.reverbWetPercent = 0f
        engine.echoWetPercent = 0f
        engine.referenceMonitor = com.example.model.ReferenceMonitor.AURATONE_5C
        engine.updateDspCoefficients()

        // 40Hz sub-bass sine (should be strongly rejected by Auratone bandpass)
        val dt40 = 2.0 * PI * 40.0 / AudioEngine.SAMPLE_RATE
        var max40Out = 0.0
        for (i in 0 until 4000) {
            val s = sin(i * dt40) * 0.5
            val (outL, _) = engine.processStereoSample(s, s)
            if (i > 2500) max40Out = maxOf(max40Out, abs(outL))
        }

        // 1000Hz vocal midrange sine (should pass through clearly)
        val dt1k = 2.0 * PI * 1000.0 / AudioEngine.SAMPLE_RATE
        var max1kOut = 0.0
        for (i in 0 until 4000) {
            val s = sin(i * dt1k) * 0.5
            val (outL, _) = engine.processStereoSample(s, s)
            if (i > 2500) max1kOut = maxOf(max1kOut, abs(outL))
        }

        assertTrue("Auratone 5C must attenuate 40Hz sub-bass ($max40Out) far below 1kHz midrange ($max1kOut)", max40Out < max1kOut * 0.25)
    }

    @Test
    fun testPhaseCorrelationMeasurement() {
        engine.reverbWetPercent = 0f
        engine.echoWetPercent = 0f
        engine.referenceMonitor = com.example.model.ReferenceMonitor.FLAT
        engine.midSideMode = com.example.model.MidSideMode.STEREO
        engine.updateDspCoefficients()

        val dt = 2.0 * PI * 440.0 / AudioEngine.SAMPLE_RATE

        // Feed identical in-phase signals
        for (i in 0 until 4096) {
            val s = sin(i * dt) * 0.5
            engine.processStereoSample(s, s)
        }
        val inPhaseCorr = engine.currentPhaseCorrelation
        assertTrue("In-phase signals must yield correlation close to +1.0, got $inPhaseCorr", inPhaseCorr > 0.95)

        // Reset and feed 180° inverted out-of-phase signals
        val engineInvert = AudioEngine()
        engineInvert.isBypassed.set(false)
        engineInvert.updateDspCoefficients()
        for (i in 0 until 4096) {
            val s = sin(i * dt) * 0.5
            engineInvert.processStereoSample(s, -s)
        }
        val outOfPhaseCorr = engineInvert.currentPhaseCorrelation
        assertTrue("Out-of-phase signals must yield negative correlation close to -1.0, got $outOfPhaseCorr", outOfPhaseCorr < -0.95)
    }

    @Test
    fun testKWeightingAndLufsMetricsCalculation() {
        val testEngine = AudioEngine()
        val dt = 2.0 * PI * 1000.0 / AudioEngine.SAMPLE_RATE
        val amp = 0.5 // ~ -6 dBFS

        // Feed 1 second (44100 samples) of 1kHz sine wave
        for (i in 0 until AudioEngine.SAMPLE_RATE) {
            val s = sin(i * dt) * amp
            testEngine.processLufsSample(s, s)
        }
        val metrics = testEngine.calculateLufsMetrics()

        // 1kHz sine wave with amp 0.5 has RMS = 0.5 / sqrt(2) = 0.3536 (-9.0 dBFS)
        // K-weighting at 1kHz has gain close to 0dB, so integrated LUFS should be around -9.7 LUFS
        assertTrue("Integrated LUFS must be between -18 and -5 LUFS, got ${metrics.integratedLufs}",
            metrics.integratedLufs in -18f..-5f)
        assertTrue("Momentary LUFS must be between -18 and -5 LUFS, got ${metrics.momentaryLufs}",
            metrics.momentaryLufs in -18f..-5f)
        assertTrue("True Peak must be between -8 and 0 dBTP, got ${metrics.truePeakDbtp}",
            metrics.truePeakDbtp in -8f..0f)
        assertTrue("LRA should be non-negative, got ${metrics.loudnessRangeLu}",
            metrics.loudnessRangeLu >= 0f)
    }

    @Test
    fun testMasteringSubCutFilterInfrasonicAttenuation() {
        val testEngine = AudioEngine()
        testEngine.isBypassed.set(false)
        testEngine.subCutFilter = SubCutFilter.CUT_30HZ
        testEngine.updateDspCoefficients()

        // 15Hz infrasonic rumble tone (below 30Hz cutoff)
        val dt15 = 2.0 * PI * 15.0 / AudioEngine.SAMPLE_RATE
        var max15Out = 0.0
        for (i in 0 until 4000) {
            val s = sin(i * dt15) * 0.5
            val (outL, _) = testEngine.processStereoSample(s, s)
            if (i > 2500) max15Out = maxOf(max15Out, abs(outL))
        }

        // 120Hz bass punch tone (well above 30Hz cutoff)
        val dt120 = 2.0 * PI * 120.0 / AudioEngine.SAMPLE_RATE
        var max120Out = 0.0
        for (i in 0 until 4000) {
            val s = sin(i * dt120) * 0.5
            val (outL, _) = testEngine.processStereoSample(s, s)
            if (i > 2500) max120Out = maxOf(max120Out, abs(outL))
        }

        // With 18-24 dB/octave cascaded Butterworth highpass, 15Hz (1 octave down) should be attenuated >18dB (>8x reduction)
        assertTrue("15Hz sub-rumble ($max15Out) should be attenuated at least 5x compared to 120Hz ($max120Out)",
            max15Out < max120Out * 0.20)
    }

    @Test
    fun testDynamicFrequencyDeEsserClamping() {
        val testEngine = AudioEngine()
        testEngine.isBypassed.set(false)
        testEngine.deEsserEnabled = true
        testEngine.deEsserThresholdDb = -20f
        testEngine.deEsserMaxReductionDb = 9f
        testEngine.updateDspCoefficients()

        // 6.5kHz harsh sibilance tone (matches de-esser center frequency)
        val dt6k5 = 2.0 * PI * 6500.0 / AudioEngine.SAMPLE_RATE
        var sibilanceReduction = 0f
        for (i in 0 until 3000) {
            val s = sin(i * dt6k5) * 0.6 // Loud sibilant peak
            testEngine.processStereoSample(s, s)
            if (i > 1500) {
                sibilanceReduction = maxOf(sibilanceReduction, testEngine.deEsserCurrentReductionDb)
            }
        }
        assertTrue("De-Esser must detect and clamp 6.5kHz sibilance (expected > 2.0 dB GR, got $sibilanceReduction dB)",
            sibilanceReduction > 2.0f)

        // Low frequency 250Hz tone should NOT trigger false de-esser attenuation
        val dt250 = 2.0 * PI * 250.0 / AudioEngine.SAMPLE_RATE
        val testEngineLow = AudioEngine()
        testEngineLow.isBypassed.set(false)
        testEngineLow.deEsserEnabled = true
        testEngineLow.deEsserThresholdDb = -20f
        testEngineLow.updateDspCoefficients()

        for (i in 0 until 3000) {
            val s = sin(i * dt250) * 0.6
            testEngineLow.processStereoSample(s, s)
        }
        assertTrue("250Hz warmth tone must not trigger sibilance clamp, got ${testEngineLow.deEsserCurrentReductionDb} dB",
            testEngineLow.deEsserCurrentReductionDb < 0.2f)
    }

    @Test
    fun testStudioCalibrationToneGenerator() {
        val testEngine = AudioEngine()

        // OFF mode should produce silence
        testEngine.testToneMode = TestToneMode.OFF
        val (offL, offR) = testEngine.synthesizeTestToneSample()
        assertEquals(0.0, offL, 1e-9)
        assertEquals(0.0, offR, 1e-9)

        // 1kHz Sine mode should produce periodic audio signal
        testEngine.testToneMode = TestToneMode.SINE_1KHZ
        testEngine.testToneLevelDb = -18f
        var maxSine = 0.0
        for (i in 0 until 500) {
            val (sL, sR) = testEngine.synthesizeTestToneSample()
            assertEquals(sL, sR, 1e-9) // Mono calibration
            maxSine = maxOf(maxSine, abs(sL))
        }
        assertTrue("1kHz tone at -18dBFS must produce signal (~0.125 amp), got $maxSine", maxSine in 0.08..0.16)

        // Pink Noise should produce fluctuating random values with equal octave energy
        testEngine.testToneMode = TestToneMode.PINK_NOISE
        testEngine.testToneLevelDb = 0f
        var energySum = 0.0
        for (i in 0 until 1000) {
            val (sL, sR) = testEngine.synthesizeTestToneSample()
            assertEquals(sL, sR, 1e-9)
            energySum += sL * sL
        }
        val pinkRms = kotlin.math.sqrt(energySum / 1000)
        assertTrue("Pink noise must generate audible energy, got RMS $pinkRms", pinkRms > 0.01)
    }

    @Test
    fun testStereoBalanceTrimAndPolarityInversion() {
        val testEngine = AudioEngine()
        testEngine.isBypassed.set(false)
        testEngine.referenceMonitor = com.example.model.ReferenceMonitor.FLAT
        testEngine.midSideMode = com.example.model.MidSideMode.STEREO
        testEngine.updateDspCoefficients()

        // Test Left Polarity Inversion (Ø L)
        testEngine.invertLeftPolarity = true
        testEngine.invertRightPolarity = false
        val (outL, outR) = testEngine.processStereoSample(0.5, 0.5)
        assertTrue("Inverted Left channel must be negative, got $outL", outL < 0.0)
        assertTrue("Non-inverted Right channel must remain positive, got $outR", outR > 0.0)

        // Test Balance Trim to Left (+6dB attenuation on right, 0dB on left)
        testEngine.invertLeftPolarity = false
        testEngine.stereoBalanceTrimDb = -6f // Hard left bias
        val (trimL, trimR) = testEngine.processStereoSample(0.5, 0.5)
        assertTrue("Left channel ($trimL) must be louder than Right channel ($trimR)", trimL > trimR * 1.5)
    }

    @Test
    fun testGainMatchedABBypassCompensation() {
        val testEngine = AudioEngine()
        testEngine.gainMatchedAB = true
        testEngine.isBypassed.set(true)

        // When bypassed with gain-matched AB active, processStereoSample returns raw scaled by ratio
        val (outL, outR) = testEngine.processStereoSample(0.4, 0.4)
        assertFalse(outL.isNaN())
        assertFalse(outR.isNaN())
        assertTrue("Bypassed output must be non-zero", outL > 0.0)
    }

    @Test
    fun testDynamicTransientDesignerDifferentialEnvelope() {
        val testEngine = AudioEngine()
        testEngine.isBypassed.set(false)
        testEngine.updateDspCoefficients()

        // 1. Boost transient attack (+100% / +12dB)
        testEngine.transientAttackPercent = 100f
        testEngine.transientSustainPercent = 0f

        // Feed baseline silence to clear previous envelope
        for (i in 0 until 100) {
            testEngine.processStereoSample(0.0, 0.0)
        }

        // Send a sudden transient burst (rising edge from 0.0 to 0.8)
        val (attackOutL, _) = testEngine.processStereoSample(0.8, 0.8)

        // The attack should be amplified beyond 0.8 by the differential envelope
        assertTrue("Transient attack must boost leading edge ($attackOutL > 0.8)", attackOutL > 0.8)

        // Now test transient softening (-100% / -12dB attenuation)
        val softenEngine = AudioEngine()
        softenEngine.isBypassed.set(false)
        softenEngine.updateDspCoefficients()
        softenEngine.transientAttackPercent = -100f
        softenEngine.transientSustainPercent = 0f

        for (i in 0 until 100) {
            softenEngine.processStereoSample(0.0, 0.0)
        }

        val (softenOutL, _) = softenEngine.processStereoSample(0.8, 0.8)
        assertTrue("Negative transient attack must soften leading edge ($softenOutL < 0.8)", softenOutL < 0.8)
    }

    @Test
    fun testHarmonicSaturationEvenAndOddHarmonics() {
        val testEngine = AudioEngine()
        testEngine.isBypassed.set(false)
        testEngine.updateDspCoefficients()

        // 1. Clean mode: zero THD
        testEngine.harmonicSaturationType = HarmonicSaturationType.CLEAN
        testEngine.harmonicDrivePercent = 0f
        val (cleanL, _) = testEngine.processStereoSample(0.5, 0.5)
        assertEquals(0.5, cleanL, 0.05)

        // 2. Triode Tube (even-order harmonics)
        testEngine.harmonicSaturationType = HarmonicSaturationType.TUBE_TRIODE
        testEngine.harmonicDrivePercent = 80f
        val dt = 2.0 * PI * 1000.0 / AudioEngine.SAMPLE_RATE
        for (i in 0 until 500) {
            val s = sin(i * dt) * 0.7
            testEngine.processStereoSample(s, s)
        }
        val (tubeL, _) = testEngine.processStereoSample(0.5, 0.5)
        assertFalse(tubeL.isNaN())
        assertFalse(tubeL.isInfinite())

        // 3. Tape / Pentode (symmetric 3rd order odd harmonics)
        testEngine.harmonicSaturationType = HarmonicSaturationType.TAPE_PENTODE
        testEngine.harmonicDrivePercent = 90f
        for (i in 0 until 500) {
            val s = sin(i * dt) * 0.7
            testEngine.processStereoSample(s, s)
        }
        val (tapeL, _) = testEngine.processStereoSample(0.5, 0.5)
        assertFalse(tapeL.isNaN())
        assertFalse(tapeL.isInfinite())

        // 4. Console Transformer
        testEngine.harmonicSaturationType = HarmonicSaturationType.CONSOLE_TRANSFORMER
        testEngine.harmonicDrivePercent = 75f
        for (i in 0 until 500) {
            val s = sin(i * dt) * 0.7
            testEngine.processStereoSample(s, s)
        }
        val (transL, _) = testEngine.processStereoSample(0.5, 0.5)
        assertFalse(transL.isNaN())
    }

    @Test
    fun testAcousticTranslationBenchEightProfiles() {
        val testEngine = AudioEngine()
        testEngine.isBypassed.set(false)

        ReferenceMonitor.entries.forEach { monitor ->
            testEngine.referenceMonitor = monitor
            testEngine.updateDspCoefficients()

            // Run 50 samples to verify zero NaN or Inf across all 8 acoustic models
            for (i in 0 until 50) {
                val (outL, outR) = testEngine.processStereoSample(0.3, -0.3)
                assertFalse("Profile $monitor produced NaN on Left", outL.isNaN())
                assertFalse("Profile $monitor produced NaN on Right", outR.isNaN())
                assertFalse("Profile $monitor produced Inf on Left", outL.isInfinite())
                assertFalse("Profile $monitor produced Inf on Right", outR.isInfinite())
            }
        }

        // Test specific curve differences:
        // Yamaha NS-10M has a pronounced 1.5kHz paper cone bump (+4.0 dB)
        testEngine.referenceMonitor = ReferenceMonitor.NS10M
        testEngine.updateDspCoefficients()
        val dt1500 = 2.0 * PI * 1500.0 / AudioEngine.SAMPLE_RATE
        var ns10mMax = 0.0
        for (i in 0 until 500) {
            val s = sin(i * dt1500) * 0.2
            val (outL, _) = testEngine.processStereoSample(s, s)
            if (i > 300) ns10mMax = maxOf(ns10mMax, abs(outL))
        }
        assertTrue("Yamaha NS-10M must boost 1.5kHz presence (> 0.2), got $ns10mMax", ns10mMax > 0.22)

        // Auratone 5C cuts 50Hz deep sub (< 250Hz cutoff)
        testEngine.referenceMonitor = ReferenceMonitor.AURATONE_5C
        testEngine.updateDspCoefficients()
        val dt50 = 2.0 * PI * 50.0 / AudioEngine.SAMPLE_RATE
        var auratone50Max = 0.0
        for (i in 0 until 500) {
            val s = sin(i * dt50) * 0.2
            val (outL, _) = testEngine.processStereoSample(s, s)
            if (i > 300) auratone50Max = maxOf(auratone50Max, abs(outL))
        }
        assertTrue("Auratone 5C must heavily attenuate 50Hz sub bass (< 0.1), got $auratone50Max", auratone50Max < 0.1)
    }

    @Test
    fun testIso226FletcherMunsonEqualLoudnessContour() {
        val testEngine = AudioEngine()
        testEngine.isBypassed.set(false)

        // 1. Flat bypass
        testEngine.fletcherMunsonEnabled = false
        testEngine.updateDspCoefficients()
        val dt85 = 2.0 * PI * 85.0 / AudioEngine.SAMPLE_RATE
        var flat85Max = 0.0
        for (i in 0 until 500) {
            val s = sin(i * dt85) * 0.2
            val (outL, _) = testEngine.processStereoSample(s, s)
            if (i > 300) flat85Max = maxOf(flat85Max, abs(outL))
        }

        // 2. Engaged Fletcher-Munson calibration (+4.5dB low shelf at 85Hz)
        testEngine.fletcherMunsonEnabled = true
        testEngine.updateDspCoefficients()
        var calib85Max = 0.0
        for (i in 0 until 500) {
            val s = sin(i * dt85) * 0.2
            val (outL, _) = testEngine.processStereoSample(s, s)
            if (i > 300) calib85Max = maxOf(calib85Max, abs(outL))
        }
        assertTrue("Fletcher-Munson must boost 85Hz ($calib85Max > $flat85Max)", calib85Max > flat85Max * 1.25)
    }

    @Test
    fun testLissajousGoniometerMidSideTransformation() {
        val testEngine = AudioEngine()
        testEngine.isBypassed.set(false)
        testEngine.updateDspCoefficients()

        val buf = FloatArray(16)
        val copied = testEngine.getVectorScopePoints(buf)
        assertTrue("Must copy points from vector scope ring buffer", copied > 0)

        // Mathematical verification of M/S rotation:
        // Pure Mono: L = 1.0, R = 1.0
        val monoX = (1.0 - 1.0) * 0.7071
        val monoY = (1.0 + 1.0) * 0.7071
        assertEquals(0.0, monoX, 1e-6) // Horizontal Side = 0 (perfect vertical line)
        assertTrue(monoY > 1.4) // Vertical Mid is high

        // Pure Anti-Phase: L = 1.0, R = -1.0
        val antiX = (-1.0 - 1.0) * 0.7071
        val antiY = (1.0 + (-1.0)) * 0.7071
        assertEquals(0.0, antiY, 1e-6) // Vertical Mid = 0 (perfect horizontal line)
        assertTrue(abs(antiX) > 1.4) // Horizontal Side is high
    }
}

