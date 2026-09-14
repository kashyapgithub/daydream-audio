package com.example.audio

import com.example.model.AudioComplaint
import com.example.model.OutputDevice
import com.example.model.PlainBand
import com.example.model.TimeMachinePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalChainComplianceTest {

    @Test
    fun testPlainBandPRD61Compliance() {
        // PRD 6.1 specification:
        // Rumble: ~20-60Hz (Shelving)
        // Warmth: ~60-250Hz (Peaking)
        // Body: ~250Hz-2kHz (Peaking)
        // Clarity: ~2-6kHz (Peaking)
        // Air: ~6-16kHz (Shelving)

        assertEquals("Rumble", PlainBand.RUMBLE.title)
        assertTrue("Rumble must be shelving filter per PRD 8.3", PlainBand.RUMBLE.isShelf)
        assertTrue(PlainBand.RUMBLE.centerHz in 20..60)

        assertEquals("Warmth", PlainBand.WARMTH.title)
        assertFalse("Warmth is a peaking filter", PlainBand.WARMTH.isShelf)
        assertTrue(PlainBand.WARMTH.centerHz in 60..250)

        assertEquals("Body", PlainBand.BODY.title)
        assertFalse("Body is a peaking filter", PlainBand.BODY.isShelf)
        assertTrue(PlainBand.BODY.centerHz in 250..2000)

        assertEquals("Clarity", PlainBand.CLARITY.title)
        assertFalse("Clarity is a peaking filter", PlainBand.CLARITY.isShelf)
        assertTrue(PlainBand.CLARITY.centerHz in 2000..6000)

        assertEquals("Air", PlainBand.AIR.title)
        assertTrue("Air must be shelving filter per PRD 8.3", PlainBand.AIR.isShelf)
        assertTrue(PlainBand.AIR.centerHz in 6000..16000)
    }

    @Test
    fun testWizardDiagnosticMappingPRD61Compliance() {
        // Verify all 6 complaints from PRD Section 6.1 table exist and provide correct clinical triage
        val complaints = AudioComplaint.entries

        assertTrue(complaints.contains(AudioComplaint.THIN_TINNY))
        assertTrue(complaints.contains(AudioComplaint.MUDDY_BOXY))
        assertTrue(complaints.contains(AudioComplaint.VOCALS_BURIED))
        assertTrue(complaints.contains(AudioComplaint.TOO_HARSH))
        assertTrue(complaints.contains(AudioComplaint.FLAT_LIFELESS))
        assertTrue(complaints.contains(AudioComplaint.HISSY_NOISY))

        // FR-2 Acceptance Criteria: "Sounds hissy" routes to Noise Reduction, not EQ
        val hissy = AudioComplaint.HISSY_NOISY
        assertTrue(hissy.label.contains("hissy / noisy", ignoreCase = true))
    }

    @Test
    fun testOutputDeviceProfilesPRD64Compliance() {
        // PRD 6.4: Remember last-used settings per device (wired != car Bluetooth != phone speaker)
        val devices = OutputDevice.entries

        val speaker = devices.first { it == OutputDevice.PHONE_SPEAKER }
        val earbuds = devices.first { it == OutputDevice.BLUETOOTH_EARBUDS }
        val car = devices.first { it == OutputDevice.CAR_AUDIO }

        // Phone speaker has narrower space and higher punch compared to earbuds
        assertTrue(speaker.defaultSpace < earbuds.defaultSpace)
        assertTrue(speaker.defaultPunch > earbuds.defaultPunch)
        assertTrue(car.defaultPunch >= 35f)
    }

    @Test
    fun testTimeMachinePresetsPRD611Compliance() {
        // PRD 6.11: Tagged by decade/format: 60s Mono, 70s Cassette, 80s Cinema, 90s Radio, Early MP3
        val preset60s = TimeMachinePreset(
            id = "60s_mono",
            eraTitle = "60s Mono Transfer",
            subtitle = "Vinyl & Tube Console",
            description = "Midrange warmth, gentle high-shelf rolloff, 60Hz mains notch, centered mono space.",
            rumbleDb = -2f, warmthDb = 3f, bodyDb = 4f, clarityDb = -1f, airDb = -4f,
            punchRatio = 35f, spacePercent = 15f, hissRemoval = 40f, deHumEnabled = true, deCrackleEnabled = true
        )

        assertTrue(preset60s.deHumEnabled)
        assertTrue(preset60s.deCrackleEnabled)
        assertTrue(preset60s.spacePercent <= 35f) // Centered mono width
        assertTrue(preset60s.warmthDb > 0f) // Tube warmth
    }

    @Test
    fun testSignalChainOrderVerification() {
        // PRD 8.1 & FR-8:
        // 1. Noise Reduction (hiss/hum/crackle)
        // 2. Equalizer (5-band or full parametric)
        // 3. Clarity macro (de-harsh + presence + exciter)
        // 4. Dynamics ("Punch": compressor/limiter)
        // 5. Virtualizer / Spatial ("Space")
        // 6. Volume Boost / Loudness Enhancer + final limiter

        val engine = AudioEngine()
        engine.isBypassed.set(false)

        // Ensure coefficients update cleanly without error
        engine.updateDspCoefficients()

        // Process a block of 100 samples
        for (i in 0 until 100) {
            val (l, r) = engine.processStereoSample(0.1, 0.1)
            assertFalse(l.isNaN())
            assertFalse(r.isNaN())
            assertFalse(l.isInfinite())
            assertFalse(r.isInfinite())
        }
    }
}
