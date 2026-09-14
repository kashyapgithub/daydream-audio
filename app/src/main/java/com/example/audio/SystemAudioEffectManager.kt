package com.example.audio

import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.util.Log
import com.example.model.PlainBand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages native Android AudioEffect instances attached to system-wide audio sessions
 * (Spotify, YouTube Music, local media, and global session 0).
 *
 * Implements PRD Section 8, Section 9 (Cross-Android Strategy), and Section 12.1.
 */
class SystemAudioEffectManager private constructor() {

    companion object {
        private const val TAG = "SystemAudioEffects"
        val instance: SystemAudioEffectManager by lazy { SystemAudioEffectManager() }
    }

    data class SessionHolder(
        val sessionId: Int,
        val packageName: String,
        var equalizer: Equalizer? = null,
        var bassBoost: BassBoost? = null,
        var virtualizer: Virtualizer? = null,
        var loudnessEnhancer: LoudnessEnhancer? = null,
        var dynamicsProcessing: DynamicsProcessing? = null,
        var environmentalReverb: EnvironmentalReverb? = null,
        var presetReverb: PresetReverb? = null
    )

    private val sessions = ConcurrentHashMap<Int, SessionHolder>()
    private val _activeSessionsSummary = MutableStateFlow<List<String>>(emptyList())
    val activeSessionsSummary: StateFlow<List<String>> = _activeSessionsSummary.asStateFlow()

    // PRD 12.1 addendum: best-effort global (session 0) hook status.
    // Session 0 attachment is an unofficial, deprecated-but-never-removed Android
    // mechanism (see PRD 12.1 for full explanation). It is inconsistent across
    // OEMs/output routes, and is very likely bypassed entirely for hardware-tunneled
    // video/audio playback paths that some apps (notably video apps) use. This flag
    // lets the UI tell the user the truth about whether it actually attached on
    // their specific device, rather than silently doing nothing.
    private val _isGlobalHookActive = MutableStateFlow(false)
    val isGlobalHookActive: StateFlow<Boolean> = _isGlobalHookActive.asStateFlow()

    // Current synchronized effect states
    private var currentEqGains = mutableMapOf<PlainBand, Float>(
        PlainBand.RUMBLE to 0f,
        PlainBand.WARMTH to 0f,
        PlainBand.BODY to 0f,
        PlainBand.CLARITY to 0f,
        PlainBand.AIR to 0f
    )
    private var currentParametricGains = mutableMapOf<Int, Float>() // Hz -> GainDb
    private var currentSpacePercent = 30f
    private var currentPunchPercent = 25f
    private var currentLoudnessPercent = 0f
    private var currentReverbWet = 0f
    private var currentReverbRoomSize = 75f
    private var currentReverbDamping = 35f
    private var currentEchoTimeMs = 320
    private var currentEchoWet = 0f
    private var currentClarityPercent = 15f
    private var currentCompThresholdDb = -18f
    private var currentCompRatio = 2.5f
    private var currentCompAttackMs = 20f
    private var currentCompReleaseMs = 150f
    private var currentEchoFeedback = 30f
    private var currentHrtfProfile = "Natural"
    private var currentSpatialRoomType = "Natural"
    private var currentHissRemovalPercent = 0f
    private var currentDeHumEnabled = false
    private var currentHumFrequency = 60
    private var currentVintageMode = false
    var isParametricModeActive: Boolean = false
    private var isBypassed = false
    private var isMonoInput = false

    fun openSession(context: Context, sessionId: Int, packageName: String) {
        if (sessions.containsKey(sessionId)) {
            Log.d(TAG, "Session $sessionId already registered for $packageName")
            return
        }

        try {
            val holder = SessionHolder(sessionId = sessionId, packageName = packageName)
            val priority = 1000 // High priority to override OEM presets/defaults

            // 1. Equalizer (PRD 6.1 / 6.2)
            try {
                val eq = try {
                    Equalizer(priority, sessionId)
                } catch (e: Exception) {
                    Equalizer(0, sessionId)
                }.apply {
                    enabled = !isBypassed
                }
                holder.equalizer = eq
            } catch (e: Exception) {
                Log.w(TAG, "Equalizer unavailable for session $sessionId: ${e.message}")
            }

            // 2. BassBoost (Essential for smartphone speakers and headphones)
            try {
                val bb = try {
                    BassBoost(priority, sessionId)
                } catch (e: Exception) {
                    BassBoost(0, sessionId)
                }.apply {
                    enabled = !isBypassed
                }
                holder.bassBoost = bb
            } catch (e: Exception) {
                Log.w(TAG, "BassBoost unavailable for session $sessionId: ${e.message}")
            }

            // 3. Virtualizer / Space (PRD 6.3)
            try {
                val virt = try {
                    Virtualizer(priority, sessionId)
                } catch (e: Exception) {
                    Virtualizer(0, sessionId)
                }.apply {
                    enabled = !isBypassed
                }
                holder.virtualizer = virt
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer unavailable for session $sessionId: ${e.message}")
            }

            // 4. LoudnessEnhancer (PRD 6.6)
            try {
                val loud = LoudnessEnhancer(sessionId).apply {
                    enabled = !isBypassed
                }
                holder.loudnessEnhancer = loud
            } catch (e: Exception) {
                Log.w(TAG, "LoudnessEnhancer unavailable for session $sessionId: ${e.message}")
            }

            // 5. DynamicsProcessing on Android 9+ for dedicated player sessions only
            // NEVER attach to session 0: Android HALs do not support global DynamicsProcessing
            // and doing so overrides/breaks the Equalizer in AudioFlinger.
            if (sessionId != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val configBuilder = DynamicsProcessing.Config.Builder(
                        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        2, // channels
                        true, // eq in
                        5,    // eq in bands
                        true, // mbc
                        1,    // mbc bands
                        false, // post eq
                        0,
                        true  // limiter
                    )
                    val dynamics = DynamicsProcessing(priority, sessionId, configBuilder.build()).apply {
                        enabled = !isBypassed
                    }
                    holder.dynamicsProcessing = dynamics
                } catch (e: Exception) {
                    Log.w(TAG, "DynamicsProcessing unavailable for session $sessionId: ${e.message}")
                }
            }

            // 6. Reverb (EnvironmentalReverb or PresetReverb fallback)
            try {
                val env = try {
                    EnvironmentalReverb(priority, sessionId)
                } catch (e: Exception) {
                    EnvironmentalReverb(0, sessionId)
                }.apply {
                    enabled = !isBypassed && (currentReverbWet > 0f || currentEchoWet > 0f)
                }
                holder.environmentalReverb = env
            } catch (e: Exception) {
                Log.w(TAG, "EnvironmentalReverb unavailable for session $sessionId: ${e.message}")
                try {
                    val pre = try {
                        PresetReverb(priority, sessionId)
                    } catch (e2: Exception) {
                        PresetReverb(0, sessionId)
                    }.apply {
                        enabled = !isBypassed && (currentReverbWet > 0f || currentEchoWet > 0f)
                    }
                    holder.presetReverb = pre
                } catch (e2: Exception) {
                    Log.w(TAG, "PresetReverb unavailable for session $sessionId: ${e2.message}")
                }
            }

            sessions[sessionId] = holder
            applyStateToSession(holder)
            updateSessionsSummary()

            // PRD 12.1 addendum: session 0 is the global output mix. If we got at
            // least one real effect engine on it, the best-effort system-wide hook
            // is genuinely active on this device.
            if (sessionId == 0) {
                val anyEffectAttached = holder.equalizer != null || holder.bassBoost != null ||
                    holder.virtualizer != null || holder.loudnessEnhancer != null ||
                    holder.environmentalReverb != null || holder.presetReverb != null
                _isGlobalHookActive.value = anyEffectAttached
                if (!anyEffectAttached) {
                    Log.w(TAG, "Global session 0 hook unsupported on this device/ROM")
                }
            }

            Log.i(TAG, "Successfully attached AudioEffects to session $sessionId ($packageName)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach AudioEffects to session $sessionId", e)
            if (sessionId == 0) {
                _isGlobalHookActive.value = false
            }
        }
    }

    fun closeSession(sessionId: Int) {
        val holder = sessions.remove(sessionId)
        if (holder != null) {
            try {
                holder.equalizer?.enabled = false
                holder.equalizer?.release()
                holder.bassBoost?.enabled = false
                holder.bassBoost?.release()
                holder.virtualizer?.enabled = false
                holder.virtualizer?.release()
                holder.loudnessEnhancer?.enabled = false
                holder.loudnessEnhancer?.release()
                holder.environmentalReverb?.enabled = false
                holder.environmentalReverb?.release()
                holder.presetReverb?.enabled = false
                holder.presetReverb?.release()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    holder.dynamicsProcessing?.enabled = false
                    holder.dynamicsProcessing?.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing session $sessionId: ${e.message}")
            }
            if (sessionId == 0) {
                _isGlobalHookActive.value = false
            }
            updateSessionsSummary()
            Log.d(TAG, "Released audio session $sessionId")
        }
    }

    /**
     * Instantly (<50ms) toggle bypass state across all attached hardware effects.
     */
    fun setBypassed(bypassed: Boolean) {
        this.isBypassed = bypassed
        sessions.values.forEach { holder ->
            try {
                val isReverbActive = currentReverbWet > 0f || currentEchoWet > 0f || currentSpatialRoomType != "Natural"
                holder.equalizer?.enabled = !bypassed
                holder.bassBoost?.enabled = !bypassed
                holder.virtualizer?.enabled = !bypassed
                holder.loudnessEnhancer?.enabled = !bypassed
                holder.environmentalReverb?.enabled = !bypassed && isReverbActive
                holder.presetReverb?.enabled = !bypassed && isReverbActive
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    holder.dynamicsProcessing?.enabled = !bypassed
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error setting bypass state: ${e.message}")
            }
        }
    }

    fun updatePlainEqGains(gains: Map<PlainBand, Float>) {
        currentEqGains.putAll(gains)
        sessions.values.forEach {
            applyEqToHolder(it)
            applyBassBoostToHolder(it)
        }
    }

    fun updateParametricGains(gains: Map<Int, Float>) {
        currentParametricGains.putAll(gains)
        sessions.values.forEach { applyEqToHolder(it) }
    }

    fun updateClarity(clarityPercent: Float) {
        this.currentClarityPercent = clarityPercent
        sessions.values.forEach { applyEqToHolder(it) }
    }

    fun updateSpace(spacePercent: Float, isMono: Boolean) {
        this.isMonoInput = isMono
        // PRD FR-4: Enforce space cap on mono source
        this.currentSpacePercent = if (isMono) spacePercent.coerceAtMost(35f) else spacePercent
        sessions.values.forEach { applySpaceToHolder(it) }
    }

    fun updatePunch(punchPercent: Float) {
        this.currentPunchPercent = punchPercent
        sessions.values.forEach {
            applyDynamicsToHolder(it)
            applyBassBoostToHolder(it)
        }
    }

    fun updateDynamicsCompressor(
        thresholdDb: Float,
        ratio: Float,
        attackMs: Float,
        releaseMs: Float
    ) {
        this.currentCompThresholdDb = thresholdDb
        this.currentCompRatio = ratio
        this.currentCompAttackMs = attackMs
        this.currentCompReleaseMs = releaseMs
        sessions.values.forEach { applyDynamicsToHolder(it) }
    }

    fun updateLoudness(loudnessPercent: Float) {
        this.currentLoudnessPercent = loudnessPercent
        sessions.values.forEach { applyLoudnessToHolder(it) }
    }

    fun updateReverb(
        wetPercent: Float,
        roomSizePercent: Float,
        dampingPercent: Float,
        echoTimeMs: Int = currentEchoTimeMs,
        echoWetPercent: Float = currentEchoWet,
        echoFeedbackPercent: Float = currentEchoFeedback
    ) {
        this.currentReverbWet = wetPercent
        this.currentReverbRoomSize = roomSizePercent
        this.currentReverbDamping = dampingPercent
        this.currentEchoTimeMs = echoTimeMs
        this.currentEchoWet = echoWetPercent
        this.currentEchoFeedback = echoFeedbackPercent
        sessions.values.forEach { applyReverbToHolder(it) }
    }

    fun updateSpatialRoom(roomType: String) {
        this.currentSpatialRoomType = roomType
        sessions.values.forEach { applyReverbToHolder(it) }
    }

    fun updateHrtfProfile(profile: String) {
        this.currentHrtfProfile = profile
        sessions.values.forEach { applySpaceToHolder(it) }
    }

    fun updateNoiseReduction(
        hissRemovalPercent: Float,
        deHumEnabled: Boolean,
        humFrequency: Int = currentHumFrequency
    ) {
        this.currentHissRemovalPercent = hissRemovalPercent
        this.currentDeHumEnabled = deHumEnabled
        this.currentHumFrequency = humFrequency
        sessions.values.forEach { applyEqToHolder(it) }
    }

    fun updateVintageMode(enabled: Boolean) {
        this.currentVintageMode = enabled
        sessions.values.forEach { applyEqToHolder(it) }
    }

    private fun applyStateToSession(holder: SessionHolder) {
        applyEqToHolder(holder)
        applyBassBoostToHolder(holder)
        applySpaceToHolder(holder)
        applyDynamicsToHolder(holder)
        applyLoudnessToHolder(holder)
        applyReverbToHolder(holder)
    }

    private fun applyBassBoostToHolder(holder: SessionHolder) {
        val bb = holder.bassBoost ?: return
        try {
            if (bb.strengthSupported) {
                // Calculate bass strength from Rumble gain, Warmth gain, and Punch
                val rumbleGain = (currentEqGains[PlainBand.RUMBLE] ?: 0f).coerceAtLeast(0f)
                val warmthGain = (currentEqGains[PlainBand.WARMTH] ?: 0f).coerceAtLeast(0f)
                val eqContrib = ((rumbleGain * 0.7f + warmthGain * 0.3f) / 12f) * 600f
                val punchContrib = (currentPunchPercent / 100f) * 400f
                val strength = (eqContrib + punchContrib).toInt().coerceIn(0, 1000)
                bb.setStrength(strength.toShort())
                bb.enabled = !isBypassed && strength > 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying BassBoost to session ${holder.sessionId}: ${e.message}")
        }
    }

    private fun applyEqToHolder(holder: SessionHolder) {
        val eq = holder.equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0] // e.g. -1500 mB
            val maxLevel = eq.bandLevelRange[1] // e.g. +1500 mB

            for (i in 0 until numBands) {
                val centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000 // mHz to Hz
                // Find matching plain band by closest frequency
                val targetGainDb = findBestGainForFrequency(centerFreqHz)
                // Convert dB to millibels (1 dB = 100 mB)
                val mB = (targetGainDb * 100f).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                eq.setBandLevel(i.toShort(), mB.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying EQ to session ${holder.sessionId}: ${e.message}")
        }
    }

    private fun findBestGainForFrequency(freqHz: Int): Float {
        // Add clarity macro boost around vocal presence (2kHz - 6kHz, peak at 3.5kHz)
        val clarityBoost = if (freqHz in 2000..6000) (currentClarityPercent / 100f) * 4.5f else 0f

        // Hiss removal cut on high frequencies (>= 3500 Hz)
        val hissCut = if (freqHz >= 3500) -((currentHissRemovalPercent / 100f) * 8.0f) else 0f

        // De-hum cut on mains hum band (<= 120 Hz)
        val deHumCut = if (currentDeHumEnabled && freqHz <= 120) -5.0f else 0f

        // Vintage-ify warm tape curve (slight warmth boost + high roll-off)
        val vintageMod = if (currentVintageMode) {
            when {
                freqHz in 80..350 -> 2.0f
                freqHz >= 8000 -> -4.0f
                else -> 0f
            }
        } else 0f

        val modifier = clarityBoost + hissCut + deHumCut + vintageMod

        // If parametric mode is actively selected and has bands, use closest frequency
        if (isParametricModeActive && currentParametricGains.isNotEmpty()) {
            val closest = currentParametricGains.minByOrNull { kotlin.math.abs(it.key - freqHz) }
            if (closest != null) {
                return closest.value + modifier
            }
        }
        // Otherwise interpolate from 5 plain-language bands.
        // Band 0 center frequency is typically 60-62.5 Hz on Android HALs, so threshold at 120 Hz.
        val baseGain = when {
            freqHz <= 120 -> currentEqGains[PlainBand.RUMBLE] ?: 0f
            freqHz <= 450 -> currentEqGains[PlainBand.WARMTH] ?: 0f
            freqHz <= 2000 -> currentEqGains[PlainBand.BODY] ?: 0f
            freqHz <= 6000 -> currentEqGains[PlainBand.CLARITY] ?: 0f
            else -> currentEqGains[PlainBand.AIR] ?: 0f
        }
        return baseGain + modifier
    }

    private fun applySpaceToHolder(holder: SessionHolder) {
        val virt = holder.virtualizer ?: return
        try {
            if (virt.strengthSupported) {
                // Scale strength based on HRTF profile
                val hrtfMultiplier = when (currentHrtfProfile) {
                    "Narrow" -> 0.6f
                    "Wide" -> 1.35f
                    else -> 1.0f // "Natural"
                }
                val rawStrength = (currentSpacePercent / 100f) * 1000f * hrtfMultiplier
                val strength = rawStrength.toInt().coerceIn(0, 1000)
                virt.setStrength(strength.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying virtualizer space: ${e.message}")
        }
    }

    private fun applyDynamicsToHolder(holder: SessionHolder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val dynamics = holder.dynamicsProcessing ?: return
            try {
                val limiter = dynamics.getLimiterByChannelIndex(0)
                if (limiter != null) {
                    if (isParametricModeActive) {
                        limiter.ratio = currentCompRatio.coerceIn(1f, 10f)
                        limiter.threshold = currentCompThresholdDb.coerceIn(-40f, 0f)
                        limiter.attackTime = currentCompAttackMs.coerceIn(1f, 100f)
                        limiter.releaseTime = currentCompReleaseMs.coerceIn(10f, 500f)
                        limiter.postGain = (-currentCompThresholdDb * 0.25f).coerceIn(0f, 8f)
                    } else {
                        val pNorm = (currentPunchPercent / 100f).coerceIn(0f, 1f)
                        limiter.ratio = 1.5f + pNorm * 2.5f
                        limiter.threshold = -12f - pNorm * 12f
                        limiter.postGain = pNorm * 4f
                    }
                    dynamics.setLimiterByChannelIndex(0, limiter)
                    dynamics.setLimiterByChannelIndex(1, limiter)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error applying DynamicsProcessing: ${e.message}")
            }
        }
    }

    private fun applyLoudnessToHolder(holder: SessionHolder) {
        val loud = holder.loudnessEnhancer ?: return
        try {
            // Target gain in millibels (0 to 1500 mB = 0 to 15dB)
            val gainMb = ((currentLoudnessPercent / 100f) * 1200f).toInt().coerceIn(0, 1500)
            loud.setTargetGain(gainMb)
        } catch (e: Exception) {
            Log.w(TAG, "Error applying loudness enhancer: ${e.message}")
        }
    }

    private fun applyReverbToHolder(holder: SessionHolder) {
        val env = holder.environmentalReverb
        val pre = holder.presetReverb
        val isEngaged = currentReverbWet > 0f || currentEchoWet > 0f || currentSpatialRoomType != "Natural"
        val shouldEnable = !isBypassed && isEngaged

        if (env != null) {
            try {
                env.enabled = shouldEnable
                if (shouldEnable) {
                    val wetFactor = (currentReverbWet / 100f).coerceIn(0f, 1f)
                    val roomFactor = (currentReverbRoomSize / 100f).coerceIn(0f, 1f)
                    val feedbackFactor = (currentEchoFeedback / 100f).coerceIn(0f, 0.8f)

                    // Room level: -6000 mB to 0 mB
                    val roomLevelMb = ((-6000f * (1f - wetFactor))).toInt().coerceIn(-9000, 0)
                    env.roomLevel = roomLevelMb.toShort()

                    // Reverb level: -5000 mB to +1000 mB
                    val reverbLevelMb = ((-5000f + wetFactor * 6000f)).toInt().coerceIn(-9000, 2000)
                    env.reverbLevel = reverbLevelMb.toShort()

                    // Decay time based on Room Size, Echo Feedback & Spatial Room
                    val roomBaseDecay = when (currentSpatialRoomType) {
                        "Intimate Studio" -> 800
                        "Concert Hall" -> 2500
                        "Cathedral" -> 4500
                        else -> 1200
                    }
                    val decayMs = ((roomBaseDecay * 0.4f) + (roomFactor * 4000f) + (feedbackFactor * 1500f)).toInt().coerceIn(100, 15000)
                    env.decayTime = decayMs

                    // Decay HF Ratio based on Damping: higher damping reduces high frequencies faster
                    val hfRatio = (2000 - (currentReverbDamping / 100f * 1500f)).toInt().coerceIn(100, 2000)
                    env.decayHFRatio = hfRatio.toShort()

                    // Early reflections (Echo/delay slapback integration + spatial room simulation)
                    val spatialReflDelay = when (currentSpatialRoomType) {
                        "Intimate Studio" -> 12
                        "Concert Hall" -> 24
                        "Cathedral" -> 35
                        else -> 8
                    }
                    val echoFactor = (currentEchoWet / 100f).coerceIn(0f, 1f)
                    val reflDelay = if (currentEchoWet > 0f) currentEchoTimeMs.coerceIn(0, 300) else spatialReflDelay
                    env.reflectionsDelay = reflDelay
                    val reflLevelMb = ((-6000f + (echoFactor + if (currentSpatialRoomType != "Natural") 0.25f else 0f) * 7000f)).toInt().coerceIn(-9000, 1000)
                    env.reflectionsLevel = reflLevelMb.toShort()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error applying EnvironmentalReverb to session ${holder.sessionId}: ${e.message}")
            }
        } else if (pre != null) {
            try {
                pre.enabled = shouldEnable
                if (shouldEnable) {
                    val preset = when (currentSpatialRoomType) {
                        "Intimate Studio" -> PresetReverb.PRESET_SMALLROOM
                        "Concert Hall" -> PresetReverb.PRESET_LARGEHALL
                        "Cathedral" -> PresetReverb.PRESET_LARGEROOM
                        else -> when {
                            currentReverbRoomSize >= 85f -> PresetReverb.PRESET_LARGEHALL
                            currentReverbRoomSize >= 65f -> PresetReverb.PRESET_MEDIUMHALL
                            currentReverbRoomSize >= 45f -> PresetReverb.PRESET_LARGEROOM
                            currentReverbRoomSize >= 25f -> PresetReverb.PRESET_MEDIUMROOM
                            else -> PresetReverb.PRESET_SMALLROOM
                        }
                    }
                    pre.preset = preset
                } else {
                    pre.preset = PresetReverb.PRESET_NONE
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error applying PresetReverb to session ${holder.sessionId}: ${e.message}")
            }
        }
    }

    private fun updateSessionsSummary() {
        val list = sessions.values.map {
            val name = it.packageName.substringAfterLast('.')
            "${name.replaceFirstChar { c -> c.uppercase() }} (Session #${it.sessionId})"
        }
        _activeSessionsSummary.value = list
    }

    fun releaseAll() {
        sessions.keys.toList().forEach { closeSession(it) }
    }
}
