package com.carmelitasoftworks.motointercom.core.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi

/** The API-level specific half of [AndroidAudioRouter]. */
internal interface PlatformRouting {
    /** Routes audio to the best available device and returns the route now in use. */
    fun applyBestRoute(): AudioRoute

    /** Undoes everything [applyBestRoute] changed. */
    fun clear()
}

/** Android 12+: the system exposes communication devices directly. */
@RequiresApi(Build.VERSION_CODES.S)
internal class ModernRouting(private val audioManager: AudioManager) : PlatformRouting {
    override fun applyBestRoute(): AudioRoute {
        val devices = audioManager.availableCommunicationDevices
            .mapNotNull { device -> RouteSelector.routeForDeviceType(device.type)?.let { it to device } }
            .toMap()
        // Some phones refuse a device that is still connecting; fall through to the next best one.
        return RouteSelector.ranked(devices.keys)
            .firstOrNull { route -> audioManager.setCommunicationDevice(devices.getValue(route)) }
            ?: AudioRoute.NONE
    }

    override fun clear() = audioManager.clearCommunicationDevice()
}

/** Android 8–11: Bluetooth needs an explicit SCO link, which opens asynchronously. */
@Suppress("DEPRECATION") // The SCO and speakerphone APIs are the only option below API 31.
internal class LegacyRouting(
    private val context: Context,
    private val audioManager: AudioManager,
    private val onScoChanged: () -> Unit,
) : PlatformRouting {
    private var receiverRegistered = false
    private var scoRequested = false
    private var scoConnected = false
    private var scoFailed = false
    private var speakerphoneBefore = false

    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR)
            val previous = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE, state)
            when (state) {
                AudioManager.SCO_AUDIO_STATE_CONNECTED -> scoConnected = true
                AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                    // Connecting -> disconnected means the headset refused the link.
                    scoFailed = scoFailed || previous == AudioManager.SCO_AUDIO_STATE_CONNECTING
                    scoConnected = false
                }
                else -> return
            }
            onScoChanged()
        }
    }

    override fun applyBestRoute(): AudioRoute {
        registerReceiver()
        val available = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .mapNotNull { RouteSelector.routeForDeviceType(it.type) }
            .toMutableSet()
        available -= AudioRoute.BLUETOOTH_LE // LE Audio routing needs API 31+.
        if (AudioRoute.BLUETOOTH_SCO !in available) scoFailed = false // Headset gone: allow a fresh try later.
        if (scoFailed) available -= AudioRoute.BLUETOOTH_SCO

        return when (RouteSelector.best(available)) {
            AudioRoute.BLUETOOTH_SCO -> {
                audioManager.isSpeakerphoneOn = false
                requestSco()
                if (scoConnected) AudioRoute.BLUETOOTH_SCO else AudioRoute.NONE
            }
            AudioRoute.WIRED_HEADSET -> {
                releaseSco()
                audioManager.isSpeakerphoneOn = false
                AudioRoute.WIRED_HEADSET
            }
            else -> {
                releaseSco()
                audioManager.isSpeakerphoneOn = true
                AudioRoute.SPEAKER
            }
        }
    }

    override fun clear() {
        releaseSco()
        scoFailed = false
        if (receiverRegistered) {
            context.unregisterReceiver(scoReceiver)
            receiverRegistered = false
            audioManager.isSpeakerphoneOn = speakerphoneBefore
        }
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        speakerphoneBefore = audioManager.isSpeakerphoneOn
        context.registerReceiver(scoReceiver, IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))
        receiverRegistered = true
    }

    private fun requestSco() {
        if (scoRequested) return
        scoRequested = true
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
    }

    private fun releaseSco() {
        if (!scoRequested) return
        scoRequested = false
        scoConnected = false
        audioManager.isBluetoothScoOn = false
        audioManager.stopBluetoothSco()
    }
}
