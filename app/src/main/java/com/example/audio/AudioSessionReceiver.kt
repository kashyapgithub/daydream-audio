package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log

/**
 * Listens for system-wide audio session broadcasts from music/streaming players
 * (Spotify, YouTube Music, local media players) according to PRD Sections 8.0 & 12.1.
 */
class AudioSessionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AudioSessionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "Unknown Player"

        if (sessionId == AudioEffect.ERROR) {
            Log.w(TAG, "Received broadcast with invalid audio session ID")
            return
        }

        when (action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.d(TAG, "Opening audio effect control session: $sessionId for $packageName")
                SystemAudioEffectManager.instance.openSession(context, sessionId, packageName)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.d(TAG, "Closing audio effect control session: $sessionId for $packageName")
                SystemAudioEffectManager.instance.closeSession(sessionId)
            }
        }
    }
}
