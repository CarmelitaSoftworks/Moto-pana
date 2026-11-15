package com.carmelitasoftworks.motointercom.core.audio

import android.media.AudioDeviceInfo

/** Pure routing rules, kept free of platform state so they can be unit tested. */
internal object RouteSelector {
    private val priority = listOf(
        AudioRoute.BLUETOOTH_LE,
        AudioRoute.BLUETOOTH_SCO,
        AudioRoute.WIRED_HEADSET,
        AudioRoute.SPEAKER,
    )

    /** The usable routes among [available], best first. */
    fun ranked(available: Set<AudioRoute>): List<AudioRoute> = priority.filter { it in available }

    /** The best route among [available]; the speaker when nothing better exists. */
    fun best(available: Set<AudioRoute>): AudioRoute = ranked(available).firstOrNull() ?: AudioRoute.SPEAKER

    /** Maps an [AudioDeviceInfo] type to the route it provides, or null if we never route to it. */
    fun routeForDeviceType(type: Int): AudioRoute? = when (type) {
        AudioDeviceInfo.TYPE_BLE_HEADSET -> AudioRoute.BLUETOOTH_LE
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioRoute.BLUETOOTH_SCO
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        -> AudioRoute.WIRED_HEADSET
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioRoute.SPEAKER
        else -> null
    }
}
