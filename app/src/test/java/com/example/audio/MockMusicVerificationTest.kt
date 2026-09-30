package com.example.audio

import com.example.model.LimiterMode
import com.example.model.MidSideMode
import com.example.model.PlainBand
import com.example.model.ReferenceMonitor
import com.example.model.SubCutFilter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Full-send proof: mock demo music through EVERY DSP feature.
 * No dummy passes allowed - each test renders audible mock music through
 * AudioEngine.processStereoSample and asserts the knob actually changes sound.
 */
class MockMusicVerificationTest {

    private lateinit var engine: AudioEngine

    @Before
    fun setUp() {
        engine = AudioEngine()
        engine.isBypassed.set(false)
        engine.updateDspCoefficients()
    }

    // ---------- mock music helpers ----------

    /** Deterministic 1s mock track: kick + bass + mids + presence + air + clicks, stereo. */
    private fun mockMusic(n: Int = 44100, seed: Long = 7L): Pair<DoubleArray, DoubleArray> {
        val rnd = Random(seed)
        val l = DoubleArray(n)
        val r = DoubleArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / AudioEngine.SAMPLE_RATE
            val kick = if (i % 11025 < 900) sin(2 * PI * 55 * t) * 0.5 else 0.0
            val bass = sin(2 * PI * 110 * t) * 0.30
            val mids = sin(2 * PI * 440 * t) * 0.22 + sin(2 * PI * 880 * t) * 0.12
            val presence = sin(2 * PI * 2500 * t) * 0.10
            val air = sin(2 * PI * 8000 * t) * 0.06
            val click = if (i % 22050 == 0) 0.4 else 0.0
            val hiss = (rnd.nextDouble() - 0.5) * 0.02
            val hum = sin(2 * PI * 60 * t) * 0.03
            l[i] = (kick + bass + mids + presence + air + click + hiss + hum).coerceIn(-0.95, 0.95)
            r[i] = (kick * 0.9 + bass * 0.9 + mids + presence * 1.05 + air + click + hiss + hum).coerceIn(-0.95, 0.95)
        }
        return Pair(l, r)
    }

    private fun render(inL: DoubleArray, inR: DoubleArray): Pair<DoubleArray, DoubleArray> {
        val outL = DoubleArray(inL.size)
        val outR = DoubleArray(inR.size)
        for (i in inL.indices) {
            val (l, r) = engine.processStereoSample(inL[i], inR[i])
            assertFalse("NaN L at $i", l.isNaN())
            assertFalse("Inf R at $i", r.isInfinite())
            outL[i] = l
            outR[i] = r
        }
        return Pair(outL, outR)
    }

    private fun rms(x: DoubleArray, from: Int = 0): Double {
        var s = 0.0
        for (i in from until x.size) s += x[i] * x[i]
        return sqrt(s / (x.size - from))
    }

    /** Amplitude of freq in mono mix via Hann-windowed sine/cosine projection. */
    private fun bandAmp(l: DoubleArray, r: DoubleArray, freq: Double, from: Int = 0): Double {
        var s = 0.0
        var c = 0.0
        var wsum = 0.0
        val n = l.size - from
        for (k in 0 until n) {
            val i = from + k
            // Hann window kills spectral leakage from loud neighbors (kick 55Hz
            // would otherwise swamp the weak 40Hz bin over a short window).
            val w = 0.5 * (1 - kotlin.math.cos(2 * PI * k / (n - 1).coerceAtLeast(1)))
            val m = (l[i] + r[i]) * 0.5
            val ph = 2 * PI * freq * i / AudioEngine.SAMPLE_RATE
            s += m * w * sin(ph)
            c += m * w * sin(ph + PI / 2)
            wsum += w
        }
        return 2 * sqrt(s * s + c * c) / wsum.coerceAtLeast(1e-9)
    }

    private fun freshEngine(): AudioEngine {
        val e = AudioEngine()
        e.isBypassed.set(false)
        e.updateDspCoefficients()
        return e
    }

    private fun neutralEngine(): AudioEngine {
        // Isolate the stage under test: kill cross-stage interactions
        // (default punch=25/space=30 would otherwise eat EQ boosts via the
        // compressor detector and smear M/S math via crossfeed).
        return freshEngine().apply {
            spaceAmount = 0f
            punchAmount = 0f
            clarityMacroAmount = 0f
            loudnessBoost = 0f
            hissRemoval = 0f
            reverbWet = 0f
            echoWet = 0f
            vintageMode = false
            updateDspCoefficients()
        }
    }

    private fun flatOut(n: Int = 6000): Pair<DoubleArray, DoubleArray> {
        val e = freshEngine()
        val (ml, mr) = mockMusic(n)
        val ol = DoubleArray(n)
        val or = DoubleArray(n)
        for (i in 0 until n) {
            val (l, r) = e.processStereoSample(ml[i], mr[i])
            ol[i] = l
            or[i] = r
        }
        return Pair(ol, or)
    }

    // ---------- EQ ----------

    @Test
    fun testFiveBandEqEachBandMovesItsOwnRange() {
        val cases = listOf(
            Triple(PlainBand.RUMBLE, 40.0, 1000.0),
            Triple(PlainBand.WARMTH, 120.0, 3000.0),
            Triple(PlainBand.BODY, 800.0, 6000.0),
            Triple(PlainBand.CLARITY, 3500.0, 300.0),
            Triple(PlainBand.AIR, 10000.0, 300.0)
        )
        for ((band, targetFreq, otherFreq) in cases) {
            val n = 12000
            val settle = 4000
            val e = neutralEngine()
            val (ml, mr) = mockMusic(n)
            e.eqGains[band] = 8f
            e.updateDspCoefficients()
            val (bl, br) = DoubleArray(n) to DoubleArray(n)
            for (i in 0 until n) {
                val (l, r) = e.processStereoSample(ml[i], mr[i])
                bl[i] = l
                br[i] = r
            }
            val boosted = bandAmp(bl, br, targetFreq, settle)
            val fe = neutralEngine()
            val (fl0, fr0) = mockMusic(n)
            val fl = DoubleArray(n)
            val fr = DoubleArray(n)
            for (i in 0 until n) {
                val (l, r) = fe.processStereoSample(fl0[i], fr0[i])
                fl[i] = l
                fr[i] = r
            }
            val flat = bandAmp(fl, fr, targetFreq, settle)
            assertTrue("$band +8dB must lift its range ($boosted vs $flat)", boosted > flat * 1.15)
            val boostedOther = bandAmp(bl, br, otherFreq, settle)
            val flatOther = bandAmp(fl, fr, otherFreq, settle)
            // Selectivity: the absolute lift at the target must exceed any
            // spillover elsewhere (ratios lie when the far bin is near-silent).
            val targetDelta = abs(boosted - flat)
            val otherDelta = abs(boostedOther - flatOther)
            assertTrue("$band boost must be selective (target Δ=$targetDelta other Δ=$otherDelta)", targetDelta > otherDelta)
        }
    }

    @Test
    fun testParametricQChangesBandwidth() {
        val (ml, mr) = mockMusic(6000)
        val wide = freshEngine().apply {
            isAdvancedParametricMode = true
            parametricGains[1000] = 8f
            parametricQ[1000] = 0.5f
            updateDspCoefficients()
        }
        val narrow = freshEngine().apply {
            isAdvancedParametricMode = true
            parametricGains[1000] = 8f
            parametricQ[1000] = 8f
            updateDspCoefficients()
        }
        val wl = DoubleArray(6000)
        val wr = DoubleArray(6000)
        val nl = DoubleArray(6000)
        val nr = DoubleArray(6000)
        for (i in 0 until 6000) {
            val (a, b) = wide.processStereoSample(ml[i], mr[i])
            wl[i] = a
            wr[i] = b
            val (c, d) = narrow.processStereoSample(ml[i], mr[i])
            nl[i] = c
            nr[i] = d
        }
        // Neighbor 500Hz: wide Q lifts it more than narrow Q
        val wideNeighbor = bandAmp(wl, wr, 500.0, 2000)
        val narrowNeighbor = bandAmp(nl, nr, 500.0, 2000)
        assertTrue("Wide Q must spill to neighbor more ($wideNeighbor vs $narrowNeighbor)", wideNeighbor > narrowNeighbor * 1.1)
    }

    // ---------- noise ----------

    @Test
    fun testDeHumKills60HzButKeeps1kHz() {
        val e = freshEngine()
        e.deHumEnabled = true
        e.humFrequency = 60
        e.updateDspCoefficients()
        var humOut = 0.0
        for (i in 0 until 9000) {
            val s = sin(2 * PI * 60 * i / AudioEngine.SAMPLE_RATE) * 0.5
            val (l, _) = e.processStereoSample(s, s)
            if (i > 7000) humOut = maxOf(humOut, abs(l))
        }
        var refOut = 0.0
        for (i in 0 until 9000) {
            val s = sin(2 * PI * 1000 * i / AudioEngine.SAMPLE_RATE) * 0.5
            val (l, _) = e.processStereoSample(s, s)
            if (i > 7000) refOut = maxOf(refOut, abs(l))
        }
        assertTrue("60Hz hum must be notched ($humOut vs $refOut)", humOut < refOut * 0.2)
    }

    @Test
    fun testDeCrackleTamesImpulseInMockMusic() {
        val e = freshEngine()
        e.deCrackleEnabled = true
        e.updateDspCoefficients()
        e.processStereoSample(0.0, 0.0)
        e.processStereoSample(0.0, 0.0)
        val (l, r) = e.processStereoSample(0.8, 0.8)
        assertTrue("Click must be interpolated below 0.5 (got $l)", abs(l) < 0.5 && abs(r) < 0.5)
    }

    @Test
    fun testHissRemovalLowersNoiseFloor() {
        val n = 8000
        val rnd = Random(11)
        val e = freshEngine()
        e.hissRemoval = 85f
        e.updateDspCoefficients()
        var outEnergy = 0.0
        for (i in 0 until n) {
            val hiss = (rnd.nextDouble() - 0.5) * 0.06
            val (l, r) = e.processStereoSample(hiss, hiss)
            if (i > 2000) outEnergy += l * l + r * r
        }
        val rnd2 = Random(11)
        var inEnergy = 0.0
        for (i in 0 until n) {
            if (i <= 2000) {
                rnd2.nextDouble()
                continue
            }
            val hiss = (rnd2.nextDouble() - 0.5) * 0.06
            inEnergy += hiss * hiss * 2
        }
        assertTrue("Hiss gate+shelf must reduce noise energy", outEnergy < inEnergy * 0.9)
    }

    // ---------- dynamics / color ----------

    @Test
    fun testPunchControlsLoudBurst() {
        val dt = 2 * PI * 200 / AudioEngine.SAMPLE_RATE
        val flat = freshEngine().apply { punchAmount = 0f; updateDspCoefficients() }
        val punched = freshEngine().apply { punchAmount = 80f; updateDspCoefficients() }
        var flatPeak = 0.0
        var punchPeak = 0.0
        for (i in 0 until 4000) {
            val s = sin(i * dt) * 1.0
            val (fl, _) = flat.processStereoSample(s, s)
            val (pl, _) = punched.processStereoSample(s, s)
            if (i > 3000) {
                flatPeak = maxOf(flatPeak, abs(fl))
                punchPeak = maxOf(punchPeak, abs(pl))
            }
        }
        assertTrue("Both paths finite", flatPeak in 0.05..1.6 && punchPeak in 0.05..1.6)
        assertTrue("Punch must audibly change burst (flat=$flatPeak punch=$punchPeak)", abs(punchPeak - flatPeak) > 0.02)
    }

    @Test
    fun testTransientDesignerShapesAttack() {
        val boost = freshEngine().apply { transientAttackPercent = 100f; updateDspCoefficients() }
        repeat(100) { boost.processStereoSample(0.0, 0.0) }
        val (hot, _) = boost.processStereoSample(0.8, 0.8)
        assertTrue("Attack+ must lift transient edge ($hot)", hot > 0.8)
        val cut = freshEngine().apply { transientAttackPercent = -100f; updateDspCoefficients() }
        repeat(100) { cut.processStereoSample(0.0, 0.0) }
        val (soft, _) = cut.processStereoSample(0.8, 0.8)
        assertTrue("Attack- must soften edge ($soft)", soft < 0.8)
    }

    @Test
    fun testSaturationTypesColorMockMusic() {
        val (ml, mr) = mockMusic(3000)
        val clean = freshEngine()
        val cl = DoubleArray(3000)
        for (i in 0 until 3000) {
            val (l, _) = clean.processStereoSample(ml[i], mr[i])
            cl[i] = l
        }
        for (type in listOf(
            com.example.model.HarmonicSaturationType.TUBE_TRIODE,
            com.example.model.HarmonicSaturationType.TAPE_PENTODE,
            com.example.model.HarmonicSaturationType.CONSOLE_TRANSFORMER
        )) {
            val e = freshEngine().apply {
                harmonicSaturationType = type
                harmonicDrivePercent = 80f
                updateDspCoefficients()
            }
            var diff = 0.0
            for (i in 0 until 3000) {
                val (l, _) = e.processStereoSample(ml[i], mr[i])
                if (i > 1000) diff += abs(l - cl[i])
            }
            assertTrue("$type must color sound (diff=$diff)", diff > 1.0)
        }
    }

    @Test
    fun testDeEsserBitesSibilanceInMockMusic() {
        val e = freshEngine().apply {
            deEsserEnabled = true
            deEsserThresholdDb = -20f
            deEsserMaxReductionDb = 9f
            updateDspCoefficients()
        }
        var maxGR = 0f
        for (i in 0 until 3000) {
            val s = sin(2 * PI * 6500 * i / AudioEngine.SAMPLE_RATE) * 0.6
            e.processStereoSample(s, s)
            if (i > 1500) maxGR = maxOf(maxGR, e.deEsserCurrentReductionDb)
        }
        assertTrue("De-esser must bite sibilance ($maxGR dB)", maxGR > 2f)
    }

    // ---------- space / stereo ----------

    @Test
    fun testSpaceWidensStereoDifference() {
        val (ml, mr) = mockMusic(5000)
        val narrow = freshEngine().apply { spaceAmount = 0f; updateDspCoefficients() }
        val wide = freshEngine().apply { spaceAmount = 85f; updateDspCoefficients() }
        var narrowDiff = 0.0
        var wideDiff = 0.0
        for (i in 0 until 5000) {
            val (a, b) = narrow.processStereoSample(ml[i], mr[i])
            val (c, d) = wide.processStereoSample(ml[i], mr[i])
            if (i > 1000) {
                narrowDiff += (a - b) * (a - b)
                wideDiff += (c - d) * (c - d)
            }
        }
        assertTrue("Space must widen L-R difference", wideDiff > narrowDiff * 1.05)
    }

    @Test
    fun testBassMonoMakerCollapsesOutOfPhaseSub() {
        // 2nd-order 120Hz crossover leaves ~-12dB residual one octave down,
        // so collapse lands near 1/3 rather than absolute zero - still a
        // 68%+ knockdown, which is the club-PA safety the knob promises.
        val e = neutralEngine().apply { bassMonoMakerEnabled = true; updateDspCoefficients() }
        var subOut = 0.0
        for (i in 0 until 4000) {
            val s = sin(2 * PI * 60 * i / AudioEngine.SAMPLE_RATE) * 0.5
            val (l, _) = e.processStereoSample(s, -s) // out-of-phase sub
            if (i > 2500) subOut = maxOf(subOut, abs(l))
        }
        var highOut = 0.0
        for (i in 0 until 4000) {
            val s = sin(2 * PI * 1000 * i / AudioEngine.SAMPLE_RATE) * 0.5
            val (l, _) = e.processStereoSample(s, -s)
            if (i > 2500) highOut = maxOf(highOut, abs(l))
        }
        assertTrue("Out-of-phase sub must collapse ($subOut vs $highOut)", subOut < highOut * 0.45)
    }

    @Test
    fun testFletcherMunsonLiftsQuietLows() {
        fun run85(enabled: Boolean): Double {
            val e = freshEngine().apply { fletcherMunsonEnabled = enabled; updateDspCoefficients() }
            var m = 0.0
            for (i in 0 until 3000) {
                val s = sin(2 * PI * 85 * i / AudioEngine.SAMPLE_RATE) * 0.2
                val (l, _) = e.processStereoSample(s, s)
                if (i > 2000) m = maxOf(m, abs(l))
            }
            return m
        }
        val off = run85(false)
        val on = run85(true)
        assertTrue("Fletcher must lift 85Hz ($on vs $off)", on > off * 1.2)
    }

    // ---------- time / space FX ----------

    @Test
    fun testEchoRepeatsImpulse() {
        val e = freshEngine().apply {
            echoWet = 100f
            echoTimeMs = 200
            echoFeedback = 50f
            reverbWet = 0f
            updateDspCoefficients()
        }
        e.processStereoSample(1.0, 1.0)
        var echoHeard = 0.0
        val delaySamples = (0.2 * AudioEngine.SAMPLE_RATE).toInt()
        for (i in 0 until delaySamples + 2000) {
            val (l, r) = e.processStereoSample(0.0, 0.0)
            if (i in delaySamples - 200..delaySamples + 1000) echoHeard = maxOf(echoHeard, abs(l), abs(r))
        }
        assertTrue("Echo repeat must be audible ($echoHeard)", echoHeard > 0.02)
    }

    @Test
    fun testReverbTailAndFreeze() {
        val e = freshEngine().apply {
            reverbWet = 100f
            reverbRoomSize = 85f
            echoWet = 0f
            updateDspCoefficients()
        }
        e.processStereoSample(1.0, 1.0)
        var tail = 0.0
        for (i in 0 until 2000) {
            val (l, r) = e.processStereoSample(0.0, 0.0)
            tail = maxOf(tail, abs(l), abs(r))
        }
        assertTrue("Reverb tail audible ($tail)", tail > 0.03)
        e.reverbFreezeEnabled = true
        var held = 0.0
        for (i in 0 until 5000) {
            val (l, r) = e.processStereoSample(0.0, 0.0)
            if (i > 4000) held = maxOf(held, abs(l), abs(r))
        }
        assertTrue("Freeze must hold tail ($held)", held > 0.008)
    }

    // ---------- loudness / monitors / audition ----------

    @Test
    fun testLoudnessBoostsButNeverClips() {
        val e = freshEngine().apply {
            loudnessBoost = 100f
            limiterCeilingDb = -0.5f
            updateDspCoefficients()
        }
        var peak = 0.0
        var flatPeak = 0.0
        val flat = freshEngine()
        for (i in 0 until 2000) {
            val s = sin(2 * PI * 440 * i / AudioEngine.SAMPLE_RATE) * 0.9
            val (l, r) = e.processStereoSample(s, s)
            val (fl, _) = flat.processStereoSample(s, s)
            peak = maxOf(peak, abs(l), abs(r))
            flatPeak = maxOf(flatPeak, abs(fl))
        }
        assertTrue("Boosted peak must stay under ceiling ($peak)", peak <= 0.98)
        assertTrue("Boost must be louder than flat", peak > flatPeak)
    }

    @Test
    fun testReferenceMonitorsAreAudibleModels() {
        // Auratone kills sub, phone collapses to mono, flat passes through
        val aur = freshEngine().apply { referenceMonitor = ReferenceMonitor.AURATONE_5C; updateDspCoefficients() }
        var sub = 0.0
        for (i in 0 until 4000) {
            val s = sin(2 * PI * 40 * i / AudioEngine.SAMPLE_RATE) * 0.5
            val (l, _) = aur.processStereoSample(s, s)
            if (i > 2500) sub = maxOf(sub, abs(l))
        }
        assertTrue("Auratone must gut 40Hz ($sub)", sub < 0.15)
        val phone = freshEngine().apply { referenceMonitor = ReferenceMonitor.PHONE_SPEAKER; updateDspCoefficients() }
        val (pl, pr) = phone.processStereoSample(0.4, -0.2)
        assertEquals("Phone check must be mono-ish", pl, pr, 0.08)
    }

    @Test
    fun testMidSideBalancePolarityAllAudible() {
        val e = neutralEngine()
        e.midSideMode = MidSideMode.MONO_SUM
        val (ml, mr) = e.processStereoSample(0.8, 0.2)
        assertEquals(0.5, ml, 0.06)
        assertEquals(0.5, mr, 0.06)
        e.midSideMode = MidSideMode.SIDE_ONLY
        val (sl, sr) = e.processStereoSample(0.8, 0.2)
        assertEquals(0.3, sl, 0.06)
        assertEquals(-0.3, sr, 0.06)
        e.midSideMode = MidSideMode.STEREO
        e.stereoBalanceTrimDb = -6f
        val (bl, br) = e.processStereoSample(0.5, 0.5)
        assertTrue("Hard-left trim must favor L ($bl vs $br)", bl > br * 1.4)
        e.stereoBalanceTrimDb = 0f
        e.invertLeftPolarity = true
        val (il, ir) = e.processStereoSample(0.5, 0.5)
        assertTrue(il < 0 && ir > 0)
    }

    @Test
    fun testVintageAndSubCutBehave() {
        val v = freshEngine().apply {
            vintageMode = true
            wowFlutterDepth = 60f
            vintageNoiseLevel = 70f
            updateDspCoefficients()
        }
        var heard = false
        for (i in 0 until 600) {
            val (l, r) = v.processStereoSample(0.0, 0.0)
            if (abs(l) > 1e-4 || abs(r) > 1e-4) {
                heard = true
                break
            }
        }
        assertTrue("Vintage must synthesize noise on silence", heard)
        val sc = freshEngine().apply {
            subCutFilter = SubCutFilter.CUT_30HZ
            updateDspCoefficients()
        }
        var rumble = 0.0
        for (i in 0 until 4000) {
            val s = sin(2 * PI * 15 * i / AudioEngine.SAMPLE_RATE) * 0.5
            val (l, _) = sc.processStereoSample(s, s)
            if (i > 2500) rumble = maxOf(rumble, abs(l))
        }
        var bass = 0.0
        for (i in 0 until 4000) {
            val s = sin(2 * PI * 120 * i / AudioEngine.SAMPLE_RATE) * 0.5
            val (l, _) = sc.processStereoSample(s, s)
            if (i > 2500) bass = maxOf(bass, abs(l))
        }
        assertTrue("Sub-cut must gut 15Hz vs 120Hz ($rumble vs $bass)", rumble < bass * 0.2)
    }

    // ---------- meters / snapshots / bounce ----------

    @Test
    fun testMockMusicDrivesRealMeters() {
        val e = freshEngine()
        val (ml, mr) = mockMusic(44100)
        for (i in ml.indices) {
            val (l, r) = e.processStereoSample(ml[i], mr[i])
            e.processLufsSample(l, r)
        }
        val m = e.calculateLufsMetrics()
        assertTrue("Integrated loudness sane (${m.integratedLufs})", m.integratedLufs in -30f..-4f)
        assertTrue("True peak sane (${m.truePeakDbtp})", m.truePeakDbtp in -12f..2f)
        assertTrue("Peak-hold tracked (${e.truePeakHoldDbtp})", e.truePeakHoldDbtp > -60f)
        assertTrue("Short-term history recorded", e.lufsShortTermHistory.isNotEmpty())
        assertTrue("Correlation sane (${e.currentPhaseCorrelation})", e.currentPhaseCorrelation in -1f..1f)
        val buf = FloatArray(16)
        assertTrue(e.getVectorScopePoints(buf) > 0)
    }

    @Test
    fun testEchoReverbAudibleOnMusicalMaterial() {
        // Same mock song, not an impulse: 50-60% wet must still be obvious.
        fun renderWith(setup: AudioEngine.() -> Unit): DoubleArray {
            val e = neutralEngine().apply(setup)
            val (ml, mr) = mockMusic(22050)
            val out = DoubleArray(22050)
            for (i in 0 until 22050) {
                val (l, r) = e.processStereoSample(ml[i], mr[i])
                out[i] = (l + r) * 0.5
            }
            return out
        }
        val dry = renderWith {}
        var dryRms = 0.0
        for (i in 8000 until 22050) dryRms += dry[i] * dry[i]
        dryRms = sqrt(dryRms / (22050 - 8000))

        val verb = renderWith { reverbWet = 60f; reverbRoomSize = 80f; updateDspCoefficients() }
        var verbDiff = 0.0
        for (i in 8000 until 22050) {
            val d = verb[i] - dry[i]
            verbDiff += d * d
        }
        verbDiff = sqrt(verbDiff / (22050 - 8000))
        assertTrue("60% reverb must reshape music (diff=$verbDiff vs dry=$dryRms)", verbDiff > dryRms * 0.08)

        val echo = renderWith { echoWet = 60f; echoTimeMs = 320; echoFeedback = 40f; updateDspCoefficients() }
        var echoDiff = 0.0
        for (i in 8000 until 22050) {
            val d = echo[i] - dry[i]
            echoDiff += d * d
        }
        echoDiff = sqrt(echoDiff / (22050 - 8000))
        assertTrue("60% echo must reshape music (diff=$echoDiff vs dry=$dryRms)", echoDiff > dryRms * 0.08)
    }

    @Test
    fun testMeasuredGrTracksRealCompression() {
        // Loud burst with punch engaged: the loop must report true reduction.
        val hot = freshEngine().apply {
            punchAmount = 80f
            updateDspCoefficients()
        }
        val dt = 2 * PI * 200 / AudioEngine.SAMPLE_RATE
        for (i in 0 until 4000) {
            val s = sin(i * dt) * 1.0
            hot.processStereoSample(s, s)
        }
        assertTrue("Hot+compressed must report GR (${hot.measuredGrDb})", hot.measuredGrDb > 0.5f)
        // Release ballistics: the detector needs ~0.94s just to fall back
        // below threshold after a 14dB-deep burst (220ms release from deep GR
        // is genuinely slow - like hardware), then the meter lets go. Give it
        // 2.5s of silence and demand it nearly reach zero.
        val peak = hot.measuredGrDb
        for (i in 0 until (AudioEngine.SAMPLE_RATE * 2.5).toInt()) {
            hot.processStereoSample(0.0, 0.0)
        }
        assertTrue("GR must release toward 0 (${hot.measuredGrDb} from $peak)", hot.measuredGrDb < peak * 0.05f)

        // Transparent chain reports exactly zero, never a stale hold.
        val flat = freshEngine().apply {
            punchAmount = 0f
            updateDspCoefficients()
        }
        for (i in 0 until 4000) {
            val s = sin(i * dt) * 1.0
            flat.processStereoSample(s, s)
        }
        assertEquals(0f, flat.measuredGrDb, 1e-6f)

        // Bypass relaxes a previously hot meter instead of freezing it.
        hot.isBypassed.set(true)
        for (i in 0 until 4000) {
            hot.processStereoSample(0.5, 0.5)
        }
        assertTrue("Bypass must relax GR (${hot.measuredGrDb})", hot.measuredGrDb < 0.5f)
    }

    @Test
    fun testTruePeakOversamplingNeverUnderReports() {
        // Hot complex music: polyphase peak must meet or beat the raw sample
        // peak (the old heuristic could miss inter-sample overshoots).
        val e = freshEngine()
        var samplePeak = 0.0
        for (i in 0 until AudioEngine.SAMPLE_RATE) {
            val t = i.toDouble() / AudioEngine.SAMPLE_RATE
            val s = (0.5 * sin(2 * PI * 5000 * t) + 0.5 * sin(2 * PI * 7000 * t)).coerceIn(-1.0, 1.0)
            samplePeak = maxOf(samplePeak, abs(s))
            e.processLufsSample(s, s)
        }
        val m = e.calculateLufsMetrics()
        val sampleDb = 20 * kotlin.math.log10(samplePeak.coerceAtLeast(1e-6))
        assertTrue(
            "Polyphase ${m.truePeakDbtp} must not under-read sample peak $sampleDb",
            m.truePeakDbtp >= sampleDb - 0.15f
        )
    }

    @Test
    fun testTruePeakUnityDcAndDeterminism() {
        // Raised-cosine ramp into DC proves per-phase unity gain (a broken
        // prototype would scale it); repeated runs must agree bit-exactly.
        // NOTE: a hard 0->DC step is deliberately NOT used - its bandlimited
        // reconstruction correctly rings above flat (Gibbs), which max-hold
        // keeps. The smooth ramp carries ~no HF energy, so ~no ring.
        fun readDc(): Float {
            val e = freshEngine()
            for (i in 0 until 3000) {
                val s = if (i < 500) 0.25 - 0.25 * cos(PI * i / 500.0) else 0.5
                e.processLufsSample(s, s)
            }
            return e.calculateLufsMetrics().truePeakDbtp
        }
        val first = readDc()
        val second = readDc()
        assertTrue("Settled DC 0.5 must read near -6.02 dBTP, got $first", first in -6.4f..-5.6f)
        assertEquals(first, second, 1e-6f)

        // Digital silence sits at the floor, never NaN/positive.
        val e = freshEngine()
        for (i in 0 until 3000) e.processLufsSample(0.0, 0.0)
        assertTrue(e.calculateLufsMetrics().truePeakDbtp <= -59f)
    }

    @Test
    fun testSnapshotBounceRoundTripOnMockMusic() {
        val e = freshEngine().apply {
            eqGains[PlainBand.WARMTH] = 5f
            spaceAmount = 60f
            updateDspCoefficients()
        }
        val snap = e.captureSnapshot("proof")
        e.eqGains[PlainBand.WARMTH] = -5f
        e.restoreSnapshot(snap)
        assertEquals(5f, e.eqGains[PlainBand.WARMTH]!!, 1e-5f)
        val (ml, mr) = mockMusic(8000)
        val (ol, or) = OfflineBounceRenderer.renderOffline(ml, mr) { l, r -> e.processStereoSample(l, r) }
        assertEquals(8000, ol.size)
        val tmp = File.createTempFile("mock_proof", ".wav")
        try {
            OfflineBounceRenderer.writeWav16Bit(tmp, ol, or)
            val (rl, rr, sr) = OfflineBounceRenderer.readWav16BitStereo(tmp)
            assertEquals(AudioEngine.SAMPLE_RATE, sr)
            assertEquals(ol.size, rl.size)
            assertTrue(abs(rl[4000] - ol[4000]) < 0.002)
            assertTrue(abs(rr[4000] - or[4000]) < 0.002)
        } finally {
            tmp.delete()
        }
    }
}
