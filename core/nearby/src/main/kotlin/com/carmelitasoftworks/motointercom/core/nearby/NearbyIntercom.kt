package com.carmelitasoftworks.motointercom.core.nearby

import com.carmelitasoftworks.motointercom.core.rtc.VoiceTransport
import kotlinx.coroutines.flow.StateFlow

/** A group someone nearby is hosting, found by discovery. */
data class NearbyGroup(
    /** Nearby's id for the host phone; only valid while it stays discoverable. */
    val endpointId: String,
    /** Stable id of the group, survives the host dropping out of range and coming back. */
    val groupId: String,
    val hostName: String,
)

/**
 * Offline group voice, phone to phone over Bluetooth and Wi-Fi (Nearby Connections),
 * no internet needed. One phone hosts and relays everyone's voice; range is that of
 * the radios, typically 50–100 m in the open.
 */
interface NearbyIntercom : VoiceTransport {
    /** Groups currently visible while [startDiscovery] runs. */
    val groups: StateFlow<List<NearbyGroup>>

    /** Starts looking for nearby groups. Needs the nearby-devices permissions. */
    fun startDiscovery()

    fun stopDiscovery()

    /** Starts a new group hosted by this phone. */
    fun host(groupId: String, myName: String)

    /** Joins [group]; if the host drops out of range, keeps looking for it and rejoins. */
    fun join(group: NearbyGroup, myName: String)
}
