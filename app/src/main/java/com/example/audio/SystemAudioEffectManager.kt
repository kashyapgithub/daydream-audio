package com.example.audio

import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
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
        var dynamicsProcessing: DynamicsProcessing? = null
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

            sessions[sessionId] = holder
            applyStateToSession(holder)
            updateSessionsSummary()

            // PRD 12.1 addendum: session 0 is the global output mix. If we got at
            // least one real effect engine on it, the best-effort system-wide hook
            // is genuinely active on this device.
            if (sessionId == 0) {
                val anyEffectAttached = holder.equalizer != null || holder.bassBoost != null ||
                    holder.virtualizer != null || holder.loudnessEnhancer != null
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
                holder.equalizer?.enabled = !bypassed
                holder.bassBoost?.enabled = !bypassed
                holder.virtualizer?.enabled = !bypassed
                holder.loudnessEnhancer?.enabled = !bypassed
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

    fun updateLoudness(loudnessPercent: Float) {
        this.currentLoudnessPercent = loudnessPercent
        sessions.values.forEach { applyLoudnessToHolder(it) }
    }

    private fun applyStateToSession(holder: SessionHolder) {
        applyEqToHolder(holder)
        applyBassBoostToHolder(holder)
        applySpaceToHolder(holder)
        applyDynamicsToHolder(holder)
        applyLoudnessToHolder(holder)
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
        // If parametric mode is actively selected and has bands, use closest frequency
        if (isParametricModeActive && currentParametricGains.isNotEmpty()) {
            val closest = currentParametricGains.minByOrNull { kotlin.math.abs(it.key - freqHz) }
            if (closest != null) {
                return closest.value
            }
        }
        // Otherwise interpolate from 5 plain-language bands.
        // Band 0 center frequency is typically 60-62.5 Hz on Android HALs, so threshold at 120 Hz.
        return when {
            freqHz <= 120 -> currentEqGains[PlainBand.RUMBLE] ?: 0f
            freqHz <= 450 -> currentEqGains[PlainBand.WARMTH] ?: 0f
            freqHz <= 2000 -> currentEqGains[PlainBand.BODY] ?: 0f
            freqHz <= 6000 -> currentEqGains[PlainBand.CLARITY] ?: 0f
            else -> currentEqGains[PlainBand.AIR] ?: 0f
        }
    }

    private fun applySpaceToHolder(holder: SessionHolder) {
        val virt = holder.virtualizer ?: return
        try {
            if (virt.strengthSupported) {
                // 0 to 1000
                val strength = ((currentSpacePercent / 100f) * 1000f).toInt().coerceIn(0, 1000)
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
                // Adjust limiter or MBC parameters matching punch (PRD 6.8 & 8.3)
                val limiter = dynamics.getLimiterByChannelIndex(0)
                if (limiter != null) {
                    val pNorm = (currentPunchPercent / 100f).coerceIn(0f, 1f)
                    // Ratio 1.5:1 up to 4.0:1, threshold -12dB down to -24dB, and automatic makeup gain
                    limiter.ratio = 1.5f + pNorm * 2.5f
                    limiter.threshold = -12f - pNorm * 12f
                    limiter.postGain = pNorm * 4f
                    dynamics.setLimiterByChannelIndex(0, limiter)
                    dynamics.setLimiterByChannelIndex(1, limiter)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error applying DynamicsProcessing punch: ${e.message}")
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
