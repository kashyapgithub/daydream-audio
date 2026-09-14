package com.example.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.model.OutputDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages real-time audio output hardware detection (<2s response per PRD FR-10).
 * Detects transitions between phone speakers, wired headphones, USB-C DACs, and Bluetooth audio.
 */
class AudioDeviceManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioDeviceManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _currentDevice = MutableStateFlow(OutputDevice.PHONE_SPEAKER)
    val currentDevice: StateFlow<OutputDevice> = _currentDevice.asStateFlow()

    private val _deviceChangePrompt = MutableStateFlow<OutputDevice?>(null)
    val deviceChangePrompt: StateFlow<OutputDevice?> = _deviceChangePrompt.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            detectCurrentOutputDevice(promptUser = true)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            detectCurrentOutputDevice(promptUser = true)
        }
    }

    init {
        registerCallback()
        detectCurrentOutputDevice(promptUser = false)
    }

    private fun registerCallback() {
        try {
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register audio device callback: ${e.message}")
        }
    }

    fun unregisterCallback() {
        try {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister audio device callback: ${e.message}")
        }
    }

    fun detectCurrentOutputDevice(promptUser: Boolean = false): OutputDevice {
        val detected = queryActiveOutputDevice()
        val previous = _currentDevice.value
        _currentDevice.value = detected

        if (promptUser && detected != previous) {
            _deviceChangePrompt.value = detected
        }
        return detected
    }

    fun clearDevicePrompt() {
        _deviceChangePrompt.value = null
    }

    private fun queryActiveOutputDevice(): OutputDevice {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (dev in devices) {
            when (dev.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_HEARING_AID -> {
                    val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        dev.productName.toString().lowercase()
                    } else ""
                    return if (name.contains("headphone") || name.contains("over") || name.contains("studio") || name.contains("wh-")) {
                        OutputDevice.BLUETOOTH_OVER_EAR
                    } else if (name.contains("car") || name.contains("auto") || name.contains("sync")) {
                        OutputDevice.CAR_AUDIO
                    } else {
                        OutputDevice.BLUETOOTH_EARBUDS
                    }
                }
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET -> {
                    return OutputDevice.WIRED_HEADPHONES
                }
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                    // Continue checking if other higher priority outputs are active
                }
            }
        }
        return OutputDevice.PHONE_SPEAKER
    }
}
