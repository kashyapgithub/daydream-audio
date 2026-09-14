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
 * Advanced 10-band parametric band
 */
data class ParametricBand(
    val hz: Int,
    val gainDb: Float = 0f,
    val q: Float = 1.0f
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
