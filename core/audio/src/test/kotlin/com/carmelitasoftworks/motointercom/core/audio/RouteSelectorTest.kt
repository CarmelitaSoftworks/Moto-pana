package com.carmelitasoftworks.motointercom.core.audio

import android.media.AudioDeviceInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RouteSelectorTest {
    @Test
    fun `a bluetooth headset wins over wired and speaker`() {
        val available = setOf(AudioRoute.SPEAKER, AudioRoute.WIRED_HEADSET, AudioRoute.BLUETOOTH_SCO)

        assertEquals(AudioRoute.BLUETOOTH_SCO, RouteSelector.best(available))
        assertEquals(
            listOf(AudioRoute.BLUETOOTH_SCO, AudioRoute.WIRED_HEADSET, AudioRoute.SPEAKER),
            RouteSelector.ranked(available),
        )
    }

    @Test
    fun `LE Audio wins over classic bluetooth`() {
        assertEquals(
            AudioRoute.BLUETOOTH_LE,
            RouteSelector.best(setOf(AudioRoute.BLUETOOTH_SCO, AudioRoute.BLUETOOTH_LE)),
        )
    }

    @Test
    fun `falls back to the speaker when nothing is available`() {
        assertEquals(AudioRoute.SPEAKER, RouteSelector.best(emptySet()))
    }

    @Test
    fun `maps device types to routes and ignores media-only bluetooth`() {
        assertEquals(
            AudioRoute.BLUETOOTH_SCO,
            RouteSelector.routeForDeviceType(AudioDeviceInfo.TYPE_BLUETOOTH_SCO),
        )
        assertEquals(AudioRoute.BLUETOOTH_LE, RouteSelector.routeForDeviceType(AudioDeviceInfo.TYPE_BLE_HEADSET))
        assertEquals(AudioRoute.WIRED_HEADSET, RouteSelector.routeForDeviceType(AudioDeviceInfo.TYPE_USB_HEADSET))
        assertEquals(AudioRoute.SPEAKER, RouteSelector.routeForDeviceType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertNull(RouteSelector.routeForDeviceType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
    }

    @Test
    fun `only headsets count as private playback`() {
        assertEquals(
            setOf(AudioRoute.BLUETOOTH_SCO, AudioRoute.BLUETOOTH_LE, AudioRoute.WIRED_HEADSET),
            AudioRoute.entries.filter { it.isHeadset }.toSet(),
        )
    }
}
