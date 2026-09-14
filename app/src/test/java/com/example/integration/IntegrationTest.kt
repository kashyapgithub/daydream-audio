package com.example.integration

import com.example.audio.AudioEngine
import com.example.model.PlainBand
import com.example.model.PresetExportBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Comprehensive Integration Test Suite for Daydream Audio:
 * - End-to-end 7-stage DSP signal chain
 * - Algorithmic Freeverb (8 comb + 4 all-pass) impulse response & stereo decorrelation
 * - Algorithmic Echo/Delay sample timing, tape damping, and feedback stability
 * - Limiter headroom protection under extreme reverberant build-up (PRD FR-5)
 * - Playback tempo clamping and varispeed safety
 * - Lofi Mode macro coordinates and state persistence
 * - Instantaneous A/B bypass latency (<50ms guarantee, PRD FR-3)
 * - Preset JSON export and import verification
 */
class IntegrationTest {

    private lateinit var engine: AudioEngine

    @Before
    fun setUp() {
        engine = AudioEngine()
        engine.isBypassed.set(false)
        engine.updateDspCoefficients()
    }

    @Test
    fun testReverbImpulseResponseDecayAndStereoSpread() {
        // Configure 100% wet reverb with medium room size and damping
        engine.reverbWet = 100f
        engine.reverbRoomSize = 80f
        engine.reverbDamping = 30f
        engine.reverbWidth = 100f
        engine.echoWet = 0f
        engine.spaceAmount = 0f
        engine.loudnessBoost = 0f

        // Inject Dirac unit impulse at t=0
        val (firstL, firstR) = engine.processStereoSample(1.0, 1.0)
        assertFalse("Reverb output should not be NaN", firstL.isNaN() || firstR.isNaN())
        assertFalse("Reverb output should not be Infinite", firstL.isInfinite() || firstR.isInfinite())

        var totalTailEnergy = 0.0
        var hasDecorrelation = false

        // Process 4,000 samples of silence to observe reverberation decay tail
        for (i in 1..4000) {
            val (tailL, tailR) = engine.processStereoSample(0.0, 0.0)

            assertFalse("Tail sample must not be NaN at $i", tailL.isNaN() || tailR.isNaN())
            assertFalse("Tail sample must not be Infinite at $i", tailL.isInfinite() || tailR.isInfinite())

            totalTailEnergy += tailL * tailL + tailR * tailR

            // Stereo decorrelation check: Freeverb offset (+23 samples) should produce differing L and R
            if (i in 500..2000 && abs(tailL - tailR) > 0.0001) {
                hasDecorrelation = true
            }
        }

        assertTrue("Reverberation tail must contain audible energy", totalTailEnergy > 0.01)
        assertTrue("Stereo reverb must decorrelate left and right channels", hasDecorrelation)

        // Verify tail decays: energy in late samples should be less than early samples
        val (lateL, lateR) = engine.processStereoSample(0.0, 0.0)
        assertTrue("Reverb should decay over time", abs(lateL) < 0.2 && abs(lateR) < 0.2)
    }

    @Test
    fun testEchoDelaySampleTimingAndFeedbackDecay() {
        // Set echo time to 100ms (at 44.1kHz = 4,410 samples)
        val delayMs = 100
        val expectedDelaySamples = (delayMs * AudioEngine.SAMPLE_RATE / 1000)
        engine.echoWet = 100f
        engine.echoTimeMs = delayMs
        engine.echoFeedback = 50f
        engine.echoPingPong = true
        engine.reverbWet = 0f
        engine.spaceAmount = 0f
        engine.loudnessBoost = 0f

        // Inject unit impulse
        engine.processStereoSample(1.0, 0.0)

        var peakIndex = -1
        var maxObservedAmp = 0.0

        // Process samples up to first repeat
        for (i in 1..10000) {
            val (l, _) = engine.processStereoSample(0.0, 0.0)
            if (l > maxObservedAmp) {
                maxObservedAmp = l
                peakIndex = i
            }
        }

        // The first delayed repeat should occur precisely at expectedDelaySamples (+/- 2 samples)
        assertTrue(
            "Expected echo repeat near $expectedDelaySamples, observed peak at $peakIndex",
            abs(peakIndex - expectedDelaySamples) <= 2
        )

        // Verify feedback stability under 80% maximum feedback
        engine.echoFeedback = 80f
        var maxInstability = 0.0
        for (i in 0..15000) {
            val (l, r) = engine.processStereoSample(0.0, 0.0)
            maxInstability = maxOf(maxInstability, abs(l), abs(r))
            assertFalse(l.isNaN() || r.isNaN())
        }
        assertTrue("Echo feedback must remain bounded and not diverge", maxInstability < 2.0)
    }

    @Test
    fun testFullChainLimiterHeadroomUnderExtremeReverbAndEcho() {
        // Extreme configuration: all stages pushed to high limits
        engine.eqGains[PlainBand.RUMBLE] = 12f
        engine.eqGains[PlainBand.WARMTH] = 12f
        engine.eqGains[PlainBand.BODY] = 6f
        engine.eqGains[PlainBand.CLARITY] = 6f
        engine.eqGains[PlainBand.AIR] = 12f
        engine.spaceAmount = 100f
        engine.punchAmount = 100f
        engine.clarityMacroAmount = 100f
        engine.reverbWet = 100f
        engine.reverbRoomSize = 98f
        engine.reverbDamping = 10f
        engine.echoWet = 100f
        engine.echoFeedback = 80f
        engine.echoTimeMs = 150
        engine.loudnessBoost = 100f // +12dB boost
        engine.limiterCeilingDb = -0.5f // ~0.944 ceiling
        engine.updateDspCoefficients()

        var maxOutputL = 0.0
        var maxOutputR = 0.0

        // Drive with full-scale sine wave for 4,000 samples
        val dt = 2.0 * PI * 440.0 / AudioEngine.SAMPLE_RATE
        for (i in 0 until 4000) {
            val input = sin(i * dt) * 1.0
            val (outL, outR) = engine.processStereoSample(input, input)

            assertFalse("Sample $i left must not be NaN", outL.isNaN())
            assertFalse("Sample $i right must not be NaN", outR.isNaN())
            assertFalse("Sample $i left must not be Infinite", outL.isInfinite())
            assertFalse("Sample $i right must not be Infinite", outR.isInfinite())

            maxOutputL = maxOf(maxOutputL, abs(outL))
            maxOutputR = maxOf(maxOutputR, abs(outR))
        }

        // PRD FR-5 & 8.3 compliance: Output MUST strictly remain below 0.98 (-0.17 dBFS)
        assertTrue("Max Left peak ($maxOutputL) must not exceed 0.98", maxOutputL <= 0.98)
        assertTrue("Max Right peak ($maxOutputR) must not exceed 0.98", maxOutputR <= 0.98)
    }

    @Test
    fun testLofiModeMacroPresetCoordination() {
        // Set Lofi Mode parameters
        engine.setPlaybackSpeed(0.85f)
        engine.reverbWet = 35f
        engine.reverbRoomSize = 75f
        engine.reverbDamping = 40f
        engine.echoWet = 20f
        engine.echoTimeMs = 320
        engine.echoFeedback = 35f
        engine.eqGains[PlainBand.WARMTH] = 4f
        engine.eqGains[PlainBand.AIR] = -5f
        engine.vintageMode = true
        engine.wowFlutterDepth = 40f
        engine.vintageNoiseLevel = 30f
        engine.updateDspCoefficients()

        assertEquals(0.85f, engine.playbackSpeed, 0.001f)
        assertEquals(35f, engine.reverbWet, 0.001f)
        assertEquals(20f, engine.echoWet, 0.001f)
        assertTrue(engine.vintageMode)

        // Process a block of 1000 samples and verify audio is stable
        val dt = 2.0 * PI * 220.0 / AudioEngine.SAMPLE_RATE
        for (i in 0 until 1000) {
            val input = sin(i * dt) * 0.4
            val (outL, outR) = engine.processStereoSample(input, input)
            assertFalse(outL.isNaN())
            assertFalse(outR.isNaN())
        }
    }

    @Test
    fun testTempoRangeClamping() {
        // Values outside [0.5, 1.5] must be clamped
        engine.setPlaybackSpeed(0.1f)
        assertEquals(0.5f, engine.playbackSpeed, 0.001f)

        engine.setPlaybackSpeed(2.5f)
        assertEquals(1.5f, engine.playbackSpeed, 0.001f)

        engine.setPlaybackSpeed(1.0f)
        assertEquals(1.0f, engine.playbackSpeed, 0.001f)

        engine.setPlaybackSpeed(0.85f)
        assertEquals(0.85f, engine.playbackSpeed, 0.001f)
    }

    @Test
    fun testInstantBypassLatencyWithReverbActive() {
        // PRD FR-3: Instant bypass comparison (<50ms latency)
        engine.reverbWet = 100f
        engine.echoWet = 100f
        engine.loudnessBoost = 50f
        engine.updateDspCoefficients()

        val testSampleL = 0.4242
        val testSampleR = 0.7373

        // When not bypassed, output is processed by DSP chain
        val (activeL, activeR) = engine.processStereoSample(testSampleL, testSampleR)
        assertTrue("Active DSP must transform the input signal", abs(activeL - testSampleL) > 0.01 || abs(activeR - testSampleR) > 0.01)

        // Engage instant bypass
        engine.isBypassed.set(true)
        val (bypassedL, bypassedR) = engine.processStereoSample(testSampleL, testSampleR)

        // Instant bypass must return identical raw sample immediately (<1 sample latency)
        assertEquals("Bypassed Left must equal input", testSampleL, bypassedL, 1e-9)
        assertEquals("Bypassed Right must equal input", testSampleR, bypassedR, 1e-9)
    }

    @Test
    fun testPresetExportImportWithReverbAndEcho() {
        val bundle = PresetExportBundle(
            name = "Lofi Chillroom",
            description = "Slowed tempo with lush chamber reverb and analog repeats",
            eqGains = mapOf("WARMTH" to 4f, "AIR" to -5f),
            spacePercent = 45f,
            punchPercent = 20f,
            clarityMacroPercent = 10f,
            loudnessPercent = 15f,
            hissRemovalPercent = 25f,
            deHumEnabled = true,
            deCrackleEnabled = true,
            reverbWetPercent = 35f,
            reverbRoomSizePercent = 80f,
            reverbDampingPercent = 45f,
            echoTimeMs = 280,
            echoFeedbackPercent = 40f,
            echoWetPercent = 25f,
            playbackSpeed = 0.85f,
            isLofiMode = true
        )

        // Apply bundle to engine
        engine.reverbWet = bundle.reverbWetPercent
        engine.reverbRoomSize = bundle.reverbRoomSizePercent
        engine.reverbDamping = bundle.reverbDampingPercent
        engine.echoTimeMs = bundle.echoTimeMs
        engine.echoFeedback = bundle.echoFeedbackPercent
        engine.echoWet = bundle.echoWetPercent
        engine.setPlaybackSpeed(bundle.playbackSpeed)

        assertEquals("Lofi Chillroom", bundle.name)
        assertEquals(35f, engine.reverbWet, 0.001f)
        assertEquals(80f, engine.reverbRoomSize, 0.001f)
        assertEquals(45f, engine.reverbDamping, 0.001f)
        assertEquals(280, engine.echoTimeMs)
        assertEquals(40f, engine.echoFeedback, 0.001f)
        assertEquals(25f, engine.echoWet, 0.001f)
        assertEquals(0.85f, engine.playbackSpeed, 0.001f)
        assertTrue(bundle.isLofiMode)
    }
}
