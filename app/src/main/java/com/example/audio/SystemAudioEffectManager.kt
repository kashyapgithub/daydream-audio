package com.example.audio

import android.content.Context
import android.media.audiofx.AudioEffect
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
        var virtualizer: Virtualizer? = null,
        var loudnessEnhancer: LoudnessEnhancer? = null,
        var dynamicsProcessing: DynamicsProcessing? = null
    )

    private val sessions = ConcurrentHashMap<Int, SessionHolder>()
    private val _activeSessionsSummary = MutableStateFlow<List<String>>(emptyList())
    val activeSessionsSummary: StateFlow<List<String>> = _activeSessionsSummary.asStateFlow()

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
    private var isBypassed = false
    private var isMonoInput = false

    fun openSession(context: Context, sessionId: Int, packageName: String) {
        if (sessions.containsKey(sessionId)) {
            Log.d(TAG, "Session $sessionId already registered for $packageName")
            return
        }

        try {
            val holder = SessionHolder(sessionId = sessionId, packageName = packageName)

            // 1. Equalizer (PRD 6.1 / 6.2)
            try {
                val eq = Equalizer(100, sessionId).apply {
                    enabled = !isBypassed
                }
                holder.equalizer = eq
            } catch (e: Exception) {
                Log.w(TAG, "Equalizer unavailable for session $sessionId: ${e.message}")
            }

            // 2. Virtualizer / Space (PRD 6.3)
            try {
                val virt = Virtualizer(100, sessionId).apply {
                    enabled = !isBypassed
                }
                holder.virtualizer = virt
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer unavailable for session $sessionId: ${e.message}")
            }

            // 3. LoudnessEnhancer (PRD 6.6)
            try {
                val loud = LoudnessEnhancer(sessionId).apply {
                    enabled = !isBypassed
                }
                holder.loudnessEnhancer = loud
            } catch (e: Exception) {
                Log.w(TAG, "LoudnessEnhancer unavailable for session $sessionId: ${e.message}")
            }

            // 4. DynamicsProcessing on Android 9+ (PRD 6.8 / 9.0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
                    val dynamics = DynamicsProcessing(100, sessionId, configBuilder.build()).apply {
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
            Log.i(TAG, "Successfully attached AudioEffects to session $sessionId ($packageName)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach AudioEffects to session $sessionId", e)
        }
    }

    fun closeSession(sessionId: Int) {
        val holder = sessions.remove(sessionId)
        if (holder != null) {
            try {
                holder.equalizer?.enabled = false
                holder.equalizer?.release()
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
        sessions.values.forEach { applyEqToHolder(it) }
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
        sessions.values.forEach { applyDynamicsToHolder(it) }
    }

    fun updateLoudness(loudnessPercent: Float) {
        this.currentLoudnessPercent = loudnessPercent
        sessions.values.forEach { applyLoudnessToHolder(it) }
    }

    private fun applyStateToSession(holder: SessionHolder) {
        applyEqToHolder(holder)
        applySpaceToHolder(holder)
        applyDynamicsToHolder(holder)
        applyLoudnessToHolder(holder)
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
        // If parametric gain is explicitly set for this Hz, use it
        if (currentParametricGains.containsKey(freqHz)) {
            return currentParametricGains[freqHz] ?: 0f
        }
        // Otherwise interpolate from 5 plain-language bands
        return when {
            freqHz <= 60 -> currentEqGains[PlainBand.RUMBLE] ?: 0f
            freqHz <= 250 -> currentEqGains[PlainBand.WARMTH] ?: 0f
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
                // Adjust limiter or MBC parameters matching punch
                val limiter = dynamics.getLimiterByChannelIndex(0)
                if (limiter != null) {
                    // Soft-knee ratio increase with punch
                    limiter.ratio = 1f + (currentPunchPercent / 100f) * 3f
                    limiter.threshold = -18f + (currentPunchPercent / 100f) * 6f
                    limiter.postGain = (currentPunchPercent / 100f) * 3f // makeup gain
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
