package com.carmelitasoftworks.motointercom.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [AudioRouter] on top of the platform [AudioManager].
 *
 * Prefers a Bluetooth headset, then a wired one, then the loudspeaker, and
 * re-evaluates whenever a device connects or drops.
 */
@Singleton
class AndroidAudioRouter @Inject constructor(@ApplicationContext context: Context) : AudioRouter {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val routing: PlatformRouting = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ModernRouting(audioManager)
    } else {
        LegacyRouting(context, audioManager, onScoChanged = ::reroute)
    }

    private val mutableRoute = MutableStateFlow(AudioRoute.NONE)
    override val route: StateFlow<AudioRoute> = mutableRoute.asStateFlow()

    /** How many callers (a ride, the headset test) currently need routing. */
    private var users = 0
    private val started: Boolean
        get() = users > 0
    private var modeBefore = AudioManager.MODE_NORMAL

    // "May duck" lets music and GPS prompts lower their volume instead of stopping.
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        .build()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = reroute()

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = reroute()
    }

    override fun start() {
        if (users++ > 0) return
        modeBefore = audioManager.mode
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.requestAudioFocus(focusRequest)
        // Also fires once right away with the devices already connected.
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        reroute()
    }

    override fun stop() {
        if (users == 0 || --users > 0) return
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        routing.clear()
        audioManager.abandonAudioFocusRequest(focusRequest)
        audioManager.mode = modeBefore
        mutableRoute.value = AudioRoute.NONE
    }

    private fun reroute() {
        if (started) mutableRoute.value = routing.applyBestRoute()
    }
}
