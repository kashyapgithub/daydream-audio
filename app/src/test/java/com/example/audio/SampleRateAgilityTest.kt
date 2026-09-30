package com.example.audio

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Pillar: 44.1/48kHz engine agility + monitor-sim fidelity.
 * Rate switches must reallocate without crashing, keep time-based effects
 * accurate in milliseconds, keep metering sane, and leave global state clean.
 */
class SampleRateAgilityTest {

    @Before
    fun setUp() {
        AudioEngine.globalSampleRate = AudioEngine.SAMPLE_RATE
    }

    @After
    fun tearDown() {
        // Never leak 48kHz global state into other suites (decoder + bounce
        // defaults read it). Set directly: setEngineSampleRate() early-returns
        // on a fresh 44.1kHz engine without touching the global.
        AudioEngine.globalSampleRate = AudioEngine.SAMPLE_RATE
    }

    @Test
    fun testRateSwitchReallocatesAndProcesses() {
        val e = AudioEngine()
        e.isBypassed.set(false)
        assertEquals(44100, e.engineSampleRate)
        assertTrue(e.setEngineSampleRate(48000))
        assertEquals(48000, e.engineSampleRate)
        assertEquals(48000, AudioEngine.globalSampleRate)
        assertFalse(e.setEngineSampleRate(48000)) // no-op same rate
        e.updateDspCoefficients()

        // 1kHz tone stays finite and full-scale through the whole chain.
        var peak = 0.0
        for (i in 0 until 6000) {
            val s = sin(2 * PI * 1000 * i / 48000) * 0.5
            val (l, r) = e.processStereoSample(s, s)
            assertFalse(l.isNaN())
            assertFalse(r.isInfinite())
            if (i > 4000) peak = maxOf(peak, abs(l))
        }
        assertTrue("48kHz chain must pass tone ($peak)", peak > 0.3)

        assertTrue(e.setEngineSampleRate(44100))
        assertEquals(44100, e.engineSampleRate)
        e.updateDspCoefficients()
        var peak44 = 0.0
        for (i in 0 until 6000) {
            val s = sin(2 * PI * 1000 * i / 44100) * 0.5
            val (l, _) = e.processStereoSample(s, s)
            if (i > 4000) peak44 = maxOf(peak44, abs(l))
        }
        assertTrue(peak44 > 0.3)
    }

    @Test
    fun testEchoDelayStaysMillisecondAccurateAt48k() {
        val e = AudioEngine()
        e.isBypassed.set(false)
        e.setEngineSampleRate(48000)
        e.echoWet = 100f
        e.echoTimeMs = 200
        e.echoFeedback = 0f
        e.reverbWet = 0f
        e.updateDspCoefficients()

        e.processStereoSample(1.0, 1.0) // impulse
        var echoAt = -1
        var echoPeak = 0.0
        for (i in 1 until 12000) {
            val (l, _) = e.processStereoSample(0.0, 0.0)
            if (i > 500 && abs(l) > echoPeak) {
                echoPeak = abs(l)
                echoAt = i
            }
        }
        // 200ms @48kHz = 9600 samples; allow filter-settling tolerance.
        assertTrue("Echo repeat audible ($echoPeak at $echoAt)", echoPeak > 0.03)
        assertTrue("Echo must land near 9600 samples (got $echoAt)", echoAt in 9000..10200)
    }

    @Test
    fun testLufsAndTruePeakSaneAt48k() {
        val e = AudioEngine()
        e.isBypassed.set(false)
        e.setEngineSampleRate(48000)
        for (i in 0 until 48000) {
            val s = sin(2 * PI * 1000 * i / 48000) * 0.5
            e.processLufsSample(s, s)
        }
        val m = e.calculateLufsMetrics()
        assertTrue("48k integrated sane (${m.integratedLufs})", m.integratedLufs in -20f..-4f)
        assertTrue("48k true peak sane (${m.truePeakDbtp})", m.truePeakDbtp in -9f..1f)
    }

    @Test
    fun testBounceHonorsSampleRate() {
        val wine = File.createTempFile("bounce48", ".wav")
        try {
            val writer = WavStreamWriter(wine, 48000, BounceBitDepth.PCM_16, BounceDither.NONE)
            val l = DoubleArray(480) { 0.1 }
            val r = DoubleArray(480) { 0.1 }
            writer.writeFrames(l, r, 480)
            val stats = writer.close()
            assertEquals(480L, stats.frames)
            val (_, _, sr) = OfflineBounceRenderer.readWav16BitStereo(wine)
            assertEquals(48000, sr)
        } finally {
            wine.delete()
        }
    }

    @Test
    fun testAuratoneLowEdgeIsSteeper() {
        // 4th-order low edge must crush 50Hz sub far below the vocal band.
        fun runAt(freq: Double): Double {
            val e = AudioEngine()
            e.isBypassed.set(false)
            e.referenceMonitor = com.example.model.ReferenceMonitor.AURATONE_5C
            e.updateDspCoefficients()
            var m = 0.0
            for (i in 0 until 5000) {
                val s = sin(2 * PI * freq * i / AudioEngine.SAMPLE_RATE) * 0.5
                val (l, _) = e.processStereoSample(s, s)
                if (i > 3500) m = maxOf(m, abs(l))
            }
            return m
        }
        val sub = runAt(50.0)
        val vocal = runAt(1000.0)
        assertTrue("Auratone must gut 50Hz ($sub vs $vocal)", sub < vocal * 0.12)
    }

    @Test
    fun testMultibandSurvivesSnapshotRoundTrip() {
        val e = AudioEngine()
        e.isBypassed.set(false)
        e.multibandEnabled = true
        e.mbXoverLowHz = 300f
        e.mbXoverHighHz = 5000f
        e.mbThreshMidDb = -24f
        e.mbRatioHigh = 5f
        e.mbKneeDb = 3f
        e.mbSidechainHpfHz = 80
        e.mbSoloHigh = true
        e.updateDspCoefficients()
        val snap = e.captureSnapshot("mb")
        e.multibandEnabled = false
        e.mbXoverLowHz = 250f
        e.mbRatioHigh = 2f
        e.mbSoloHigh = false
        e.restoreSnapshot(snap)
        assertTrue(e.multibandEnabled)
        assertEquals(300f, e.mbXoverLowHz, 1e-5f)
        assertEquals(5000f, e.mbXoverHighHz, 1e-5f)
        assertEquals(-24f, e.mbThreshMidDb, 1e-5f)
        assertEquals(5f, e.mbRatioHigh, 1e-5f)
        assertEquals(3f, e.mbKneeDb, 1e-5f)
        assertEquals(80, e.mbSidechainHpfHz)
        assertTrue(e.mbSoloHigh)
    }
}
