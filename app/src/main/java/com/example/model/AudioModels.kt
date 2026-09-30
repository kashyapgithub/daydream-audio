package com.example.model

/**
 * Plain-language EQ bands according to PRD section 6.1
 */
enum class PlainBand(
    val title: String,
    val frequencyRange: String,
    val centerHz: Int,
    val plainDescription: String,
    val fixTip: String,
    val excessiveWarning: String,
    val isShelf: Boolean = false
) {
    RUMBLE(
        title = "Rumble",
        frequencyRange = "20–60Hz",
        centerHz = 45,
        plainDescription = "Deep sub-bass you feel more than hear.",
        fixTip = "Boost for physical depth in club or movie tracks.",
        excessiveWarning = "Too high can cause muddy speaker rattle.",
        isShelf = true
    ),
    WARMTH(
        title = "Warmth",
        frequencyRange = "60–250Hz",
        centerHz = 120,
        plainDescription = "Fullness, bass guitar, and low-end punch.",
        fixTip = "Turn this up if your music sounds thin or hollow.",
        excessiveWarning = "Too high turns the mix boomy and congested."
    ),
    BODY(
        title = "Body",
        frequencyRange = "250Hz–2kHz",
        centerHz = 800,
        plainDescription = "Vocal richness and instrument presence.",
        fixTip = "Turn this up if vocals sound buried or distant.",
        excessiveWarning = "Too high makes music sound like it's inside a cardboard box."
    ),
    CLARITY(
        title = "Clarity",
        frequencyRange = "2–6kHz",
        centerHz = 3500,
        plainDescription = "Sharpness and articulation of consonants and guitars.",
        fixTip = "Turn this up if voices sound muffled or indistinct.",
        excessiveWarning = "Too high causes harsh, piercing fatigue and sibilant 'S' sounds."
    ),
    AIR(
        title = "Air",
        frequencyRange = "6–16kHz",
        centerHz = 10000,
        plainDescription = "Sparkle, cymbals, breath, and acoustic openness.",
        fixTip = "Turn this up to breathe life into dull recordings.",
        excessiveWarning = "Too high accentuates hiss and brittle digital glare.",
        isShelf = true
    )
}

/**
 * Advanced 10-band parametric band with plain-language anchor (PRD 6.2 & 8.3)
 */
data class ParametricBand(
    val hz: Int,
    val anchorLabel: String,
    val gainDb: Float = 0f,
    val q: Float = 0.8f
)

/**
 * Shareable full-chain preset bundle format (PRD 6.2 & FR-12)
 */
data class PresetExportBundle(
    val name: String,
    val description: String,
    val eqGains: Map<String, Float>, // Band or Hz -> Gain
    val spacePercent: Float,
    val punchPercent: Float,
    val clarityMacroPercent: Float,
    val loudnessPercent: Float,
    val hissRemovalPercent: Float,
    val deHumEnabled: Boolean,
    val deCrackleEnabled: Boolean,
    val compThresholdDb: Float = -18f,
    val compRatio: Float = 2.5f,
    val compAttackMs: Float = 20f,
    val compReleaseMs: Float = 150f,
    val reverbWetPercent: Float = 0f,
    val reverbRoomSizePercent: Float = 75f,
    val reverbDampingPercent: Float = 35f,
    val echoTimeMs: Int = 320,
    val echoFeedbackPercent: Float = 30f,
    val echoWetPercent: Float = 0f,
    val playbackSpeed: Float = 1.0f,
    val isLofiMode: Boolean = false
)

/**
 * Diagnostics wizard complaints mapping to PRD 6.1
 */
enum class AudioComplaint(
    val label: String,
    val description: String,
    val iconName: String
) {
    THIN_TINNY(
        label = "Sounds thin / tinny",
        description = "Lacks bass body and low-end presence; sounds cheap or lightweight.",
        iconName = "GraphicEq"
    ),
    MUDDY_BOXY(
        label = "Sounds muddy / boxy",
        description = "Instruments blend into a muffled soup, like inside a cardboard box.",
        iconName = "BlurOn"
    ),
    VOCALS_BURIED(
        label = "Vocals buried",
        description = "Singer is hard to understand behind the instruments.",
        iconName = "RecordVoiceOver"
    ),
    TOO_HARSH(
        label = "Too harsh / piercing",
        description = "Highs pierce your ears, 'S' and 'T' sounds hurt at normal volume.",
        iconName = "VolumeOff"
    ),
    FLAT_LIFELESS(
        label = "Sounds flat / lifeless",
        description = "No excitement, dynamic contrast, or stereo width.",
        iconName = "Waves"
    ),
    HISSY_NOISY(
        label = "Sounds hissy / noisy",
        description = "Noticeable background hiss, hum, or crackle from vintage recordings.",
        iconName = "SurroundSound"
    )
}

/**
 * Time Machine era presets (PRD 6.11)
 */
data class TimeMachinePreset(
    val id: String,
    val eraTitle: String,
    val subtitle: String,
    val description: String,
    val rumbleDb: Float,
    val warmthDb: Float,
    val bodyDb: Float,
    val clarityDb: Float,
    val airDb: Float,
    val punchRatio: Float,
    val spacePercent: Float,
    val hissRemoval: Float,
    val deHumEnabled: Boolean,
    val deCrackleEnabled: Boolean,
    val vintageMode: Boolean = false,
    val wowFlutterDepth: Float = 0f
)

/**
 * Output Device profile (PRD 6.4)
 */
enum class OutputDevice(
    val displayName: String,
    val defaultSpace: Float,
    val defaultPunch: Float,
    val defaultWarmth: Float
) {
    PHONE_SPEAKER("Phone Speaker", defaultSpace = 20f, defaultPunch = 45f, defaultWarmth = 4f),
    WIRED_HEADPHONES("Wired Headphones", defaultSpace = 55f, defaultPunch = 25f, defaultWarmth = 1f),
    BLUETOOTH_EARBUDS("Bluetooth Earbuds", defaultSpace = 60f, defaultPunch = 30f, defaultWarmth = 0f),
    BLUETOOTH_OVER_EAR("Over-Ear Studio Cans", defaultSpace = 50f, defaultPunch = 20f, defaultWarmth = 0f),
    CAR_AUDIO("Car Bluetooth", defaultSpace = 35f, defaultPunch = 40f, defaultWarmth = 3f)
}

/**
 * Audio demo sample for playback
 */
data class DemoTrack(
    val id: String,
    val title: String,
    val era: String,
    val characteristic: String,
    val baseFrequency: Float,
    val noiseType: String
)

/**
 * User Custom Presets Bank
 */
data class CustomSoundPreset(
    val id: String,
    val name: String,
    val colorHex: String = "#0A84FF",
    val eqGains: Map<PlainBand, Float>,
    val spacePercent: Float,
    val punchPercent: Float,
    val clarityPercent: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Curated Studio Sound Target Preset
 */
data class SoundTargetPreset(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val subtitle: String,
    val rumbleDb: Float,
    val warmthDb: Float,
    val bodyDb: Float,
    val clarityDb: Float,
    val airDb: Float,
    val spacePercent: Float,
    val punchPercent: Float,
    val clarityPercent: Float
) {
    companion object {
        val ALL: List<SoundTargetPreset> = listOf(
            SoundTargetPreset(
                id = "vocal",
                title = "Vocal Clarity",
                iconEmoji = "🎙️",
                subtitle = "Podcast & Speech Articulation",
                rumbleDb = -3f,
                warmthDb = -1f,
                bodyDb = 2f,
                clarityDb = 4.5f,
                airDb = 2f,
                spacePercent = 25f,
                punchPercent = 35f,
                clarityPercent = 40f
            ),
            SoundTargetPreset(
                id = "harman",
                title = "Harman Target",
                iconEmoji = "🎧",
                subtitle = "Neutral Audiophile Reference",
                rumbleDb = 2.5f,
                warmthDb = 0.5f,
                bodyDb = -0.5f,
                clarityDb = 2f,
                airDb = 1.5f,
                spacePercent = 40f,
                punchPercent = 25f,
                clarityPercent = 20f
            ),
            SoundTargetPreset(
                id = "club",
                title = "Club Bass",
                iconEmoji = "🔊",
                subtitle = "Sub-Bass & Hard Transient Punch",
                rumbleDb = 6f,
                warmthDb = 4f,
                bodyDb = 0f,
                clarityDb = 1f,
                airDb = 3.5f,
                spacePercent = 50f,
                punchPercent = 60f,
                clarityPercent = 35f
            ),
            SoundTargetPreset(
                id = "latenight",
                title = "Late Night",
                iconEmoji = "🌙",
                subtitle = "Dialogue Lift & Soft Dynamic Peaks",
                rumbleDb = -4f,
                warmthDb = 2f,
                bodyDb = 3f,
                clarityDb = -1f,
                airDb = -2f,
                spacePercent = 20f,
                punchPercent = 70f,
                clarityPercent = 10f
            ),
            SoundTargetPreset(
                id = "acoustic",
                title = "Acoustic Air",
                iconEmoji = "🎸",
                subtitle = "Silky Sparkle & Wide Hall Room",
                rumbleDb = -1.5f,
                warmthDb = 1f,
                bodyDb = 1.5f,
                clarityDb = 3f,
                airDb = 5.5f,
                spacePercent = 60f,
                punchPercent = 20f,
                clarityPercent = 45f
            )
        )
    }
}

/**
 * Locally stored MP3 track stored exclusively inside this app's private files directory.
 */
data class LocalTrack(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "",
    val durationMs: Long = 0L,
    val filePath: String,
    val fileName: String,
    val fileSize: Long = 0L,
    val bitrateKbps: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
        }

    val formattedSize: String
        get() {
            if (fileSize <= 0) return ""
            val mb = fileSize.toDouble() / (1024 * 1024)
            return String.format(java.util.Locale.US, "%.1f MB", mb)
        }

    val formattedFileSize: String
        get() = formattedSize
}

/**
 * Pro Studio Mid/Side Audition Mode
 */
enum class MidSideMode(val label: String, val shortLabel: String, val description: String) {
    STEREO("Full Stereo", "Stereo", "Standard left/right stereo master playback"),
    MONO_SUM("Mono Sum (L+R)", "Mono", "Sums channels to check phase cancellation and mono compatibility"),
    MID_ONLY("Mid Only (Center)", "Mid", "Isolates phantom center (lead vocals, kick, snare, bass)"),
    SIDE_ONLY("Sides Only (L-R)", "Side", "Mutes center; audits stereo width, panning & reverb tails"),
    PHASE_INVERT("Phase Invert (Ø R)", "Ø Invert", "Flips right channel polarity 180° to expose phase issues");

    val displayName: String get() = shortLabel
}

/**
 * Studio Reference Monitor Simulation
 */
enum class ReferenceMonitor(val label: String, val subtitle: String, val description: String) {
    FLAT("Flat Studio Master", "Linear Reference", "Direct high-fidelity transparent monitor path"),
    NS10M("Yamaha NS-10M", "Mid-Forward Sealed Box", "Iconic 1.5kHz paper cone bump & 85Hz sealed rolloff: the ultimate mix balance reference"),
    AURATONE_5C("Auratone 5C Soundcube", "250Hz–5.5kHz Bandpass", "Legendary mid-forward mix cube check for vocal & snare balance"),
    CAR_TEST("Car Test Simulation", "Sub Bump + Cabin Reflection", "Scooped mids, resonant 65Hz cabin cavity, high sizzle, and driver crossfeed"),
    AIRPODS_PRO("AirPods Pro Profile", "Harman Consumer Target", "In-ear acoustic target with sub-bass shelf boost and ear-canal resonance"),
    PHONE_SPEAKER("Phone Speaker Check", "Highpass 450Hz + Harmonics", "Simulates smartphone speaker playback to verify bass translation"),
    CLUB_SYSTEM("Club PA Subwoofer", "Deep Sub <45Hz + Rumble Cut", "Massive low-end impact test for club and festival sound systems"),
    MACBOOK_PRO("MacBook Pro Speakers", "Highpass 160Hz + Virtual Bass", "Laptop micro-transducer simulation with upper-harmonic bass illusion")
}

/**
 * Analog Harmonic Saturation Color Topology
 */
enum class HarmonicSaturationType(val label: String, val order: String, val description: String) {
    CLEAN("Clean Linear", "Zero THD", "Bit-accurate, zero harmonic distortion pass-through"),
    TUBE_TRIODE("Triode Class-A Tube", "2nd Harmonic (2f₀)", "Asymmetric even-order warmth; enriches vocals, bass, and acoustic guitars with musical body"),
    TAPE_PENTODE("Analog Reel Tape", "3rd Harmonic (3f₀)", "Symmetric odd-order tape compression; rounds sharp transients and glues the drum bus"),
    CONSOLE_TRANSFORMER("Console Iron Core", "Low-End Hysteresis", "Magnetic transformer saturation; fattens bass frequencies below 150Hz")
}

/**
 * Mastering Limiter Topology
 */
enum class LimiterMode(val label: String, val description: String) {
    SOFT_BRICKWALL("Soft-Knee Brickwall", "Transparent analog-modeled soft limiter preventing inter-sample peaks"),
    HARD_CLIPPER("Hard True-Peak Clipper", "Aggressive modern mastering clipper for punchy transient retention")
}

/**
 * Studio Calibration & Acoustic Test Tone Generator
 */
enum class TestToneMode(val label: String, val shortLabel: String, val description: String) {
    OFF("Signal Generator Off", "Off", "Standard audio playback"),
    PINK_NOISE("Pink Noise (1/f)", "Pink Noise", "Equal energy per octave for room acoustic analysis & headphone calibration"),
    WHITE_NOISE("White Noise", "White Noise", "Flat power spectral density across the full 20Hz-20kHz audio band"),
    SINE_1KHZ("1 kHz Sine Reference", "1 kHz Sine", "Pure 1000Hz reference tone aligned to -18 dBFS studio standard"),
    SUB_50HZ("50 Hz Sub Bass Check", "50 Hz Sub", "Deep pure sub tone for subwoofer crossover & acoustic rattle checks"),
    LOG_SWEEP("20Hz–20kHz Log Sweep", "Log Sweep", "Continuous logarithmic sine sweep to expose room nodes and distortion");

    val displayName: String get() = shortLabel
}

/**
 * Mastering Infrasonic Sub-Cut Filter (Butterworth High-Pass)
 */
enum class SubCutFilter(
    val label: String,
    val cutoffHz: Float,
    val slope: String,
    val description: String
) {
    OFF("Flat (Off)", 0f, "0 dB/oct", "Full DC-coupled sub-bass pass-through"),
    CUT_20HZ("20 Hz Cut", 20f, "18 dB/oct", "Standard vinyl & infrasonic cutoff; cleans DC rumble"),
    CUT_30HZ("30 Hz Master", 30f, "24 dB/oct", "Streaming standard; reclaims +2dB headroom without affecting musical bass"),
    CUT_40HZ("40 Hz Tight", 40f, "24 dB/oct", "Aggressive low-end cleanup for high-SPL club systems and subwoofers")
}

/**
 * Streaming Platform Loudness Targets (ITU-R BS.1770-4 / EBU R128)
 */
enum class StreamingTarget(
    val platformName: String,
    val targetLufs: Float,
    val maxTruePeakDbtp: Float,
    val description: String
) {
    SPOTIFY_14("Spotify", -14.0f, -1.0f, "Spotify standard loudness normalization (-14 LUFS, -1.0 dBTP)"),
    APPLE_16("Apple Music", -16.0f, -1.0f, "Apple Digital Masters Sound Check standard (-16 LUFS, -1.0 dBTP)"),
    YOUTUBE_14("YouTube", -14.0f, -1.0f, "YouTube audio volume normalization (-14 LUFS, -1.0 dBTP)"),
    CLUB_8("Club / EDM", -8.0f, -0.3f, "High-energy commercial club and festival loudness target (-8 LUFS)"),
    EBU_R128_23("Broadcast (EBU)", -23.0f, -1.0f, "European Broadcast Union television & podcast standard (-23 LUFS)");

    val platform: String get() = platformName
}

/**
 * Real-time ITU-R BS.1770-4 & EBU R128 Loudness Metrics
 */
data class LufsMetrics(
    val momentaryLufs: Float = -24.0f, // 400ms gating window
    val shortTermLufs: Float = -24.0f, // 3-second sliding window
    val integratedLufs: Float = -24.0f, // Gated integrated loudness across track
    val loudnessRangeLu: Float = 6.0f,  // Dynamic Loudness Range (LRA) in LU
    val truePeakDbtp: Float = -6.0f    // 4x oversampled True Peak in dBTP
)

/**
 * Engine processing rate selection.
 * AUTO follows the device's native output rate (bit-transparent path to the
 * DAC on the 48kHz-native majority); the other two pin the engine for
 * repeatable renders and Bluetooth quirks.
 */
enum class SampleRateMode(val label: String) {
    AUTO("Auto (device native)"),
    RATE_44100("44.1 kHz"),
    RATE_48000("48 kHz")
}

/**
 * Pro Studio full-chain mix snapshot.
 * Captures every automatable DSP parameter so A/B/C/D slots and undo
 * can restore bit-identical processing without re-dialing sliders.
 */
data class MixSnapshot(
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eqGains: Map<String, Float> = emptyMap(), // PlainBand.name -> dB
    val parametricGains: Map<String, Float> = emptyMap(), // Hz string -> dB
    val parametricQ: Map<String, Float> = emptyMap(), // Hz string -> Q
    val spacePercent: Float = 30f,
    val punchPercent: Float = 25f,
    val clarityMacroPercent: Float = 0f,
    val loudnessPercent: Float = 0f,
    val hissRemovalPercent: Float = 0f,
    val deHumEnabled: Boolean = false,
    val humFrequency: Int = 60,
    val deCrackleEnabled: Boolean = false,
    val compThresholdDb: Float = -18f,
    val compRatio: Float = 2.5f,
    val compAttackMs: Float = 20f,
    val compReleaseMs: Float = 150f,
    val limiterCeilingDb: Float = -0.5f,
    val reverbWetPercent: Float = 0f,
    val reverbRoomSizePercent: Float = 75f,
    val reverbDampingPercent: Float = 35f,
    val reverbFreezeEnabled: Boolean = false,
    val echoTimeMs: Int = 320,
    val echoFeedbackPercent: Float = 30f,
    val echoWetPercent: Float = 0f,
    val roomSizeName: String = "LARGE_HALL",
    val wallMaterialName: String = "PLASTER",
    val midSideName: String = "STEREO",
    val bassMonoMakerEnabled: Boolean = false,
    val referenceMonitorName: String = "FLAT",
    val limiterModeName: String = "SOFT_BRICKWALL",
    val transientAttackPercent: Float = 0f,
    val transientSustainPercent: Float = 0f,
    val harmonicSaturationName: String = "CLEAN",
    val harmonicDrivePercent: Float = 0f,
    val fletcherMunsonEnabled: Boolean = false,
    val subCutName: String = "OFF",
    val deEsserEnabled: Boolean = false,
    val deEsserThresholdDb: Float = -18f,
    val deEsserMaxReductionDb: Float = 6f,
    val stereoBalanceTrimDb: Float = 0f,
    val invertLeftPolarity: Boolean = false,
    val invertRightPolarity: Boolean = false,
    // Pro Studio 3-band multiband compressor
    val multibandEnabled: Boolean = false,
    val mbXoverLowHz: Float = 250f,
    val mbXoverHighHz: Float = 4000f,
    val mbThreshLowDb: Float = -18f,
    val mbThreshMidDb: Float = -18f,
    val mbThreshHighDb: Float = -18f,
    val mbRatioLow: Float = 2f,
    val mbRatioMid: Float = 2f,
    val mbRatioHigh: Float = 2f,
    val mbAttackMs: Float = 20f,
    val mbReleaseMs: Float = 150f,
    val mbKneeDb: Float = 6f,
    val mbSidechainHpfHz: Int = 0,
    val mbSoloLow: Boolean = false,
    val mbSoloMid: Boolean = false,
    val mbSoloHigh: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val varispeedMode: Boolean = true,
    val isVintageMode: Boolean = false,
    val wowFlutterDepth: Float = 0f,
    val vintageNoiseLevel: Float = 0f
) {
    companion object {
        const val MAX_SLOTS = 4
        const val MAX_UNDO = 20

        /**
         * Gain needed to move current integrated loudness to target.
         * Positive = needs boost, negative = needs cut.
         * Clamped to +/-12dB so auto-match never slams the limiter.
         */
        fun loudnessGainDb(currentIntegratedLufs: Float, targetLufs: Float): Float {
            if (currentIntegratedLufs <= -69f || currentIntegratedLufs.isNaN()) return 0f
            return (targetLufs - currentIntegratedLufs).coerceIn(-12f, 12f)
        }

        /**
         * True-peak-safe gain: reduces loudness gain if it would push
         * current true peak over ceiling. Returns applied gain.
         */
        fun safeLoudnessGainDb(
            currentIntegratedLufs: Float,
            targetLufs: Float,
            currentTruePeakDbtp: Float,
            ceilingDbtp: Float = -1.0f
        ): Float {
            val wanted = loudnessGainDb(currentIntegratedLufs, targetLufs)
            if (wanted <= 0f) return wanted
            val headroom = ceilingDbtp - currentTruePeakDbtp
            return wanted.coerceAtMost(headroom.coerceAtLeast(0f))
        }
    }
}

