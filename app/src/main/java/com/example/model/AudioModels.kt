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

