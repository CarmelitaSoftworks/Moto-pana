package com.carmelitasoftworks.motointercom.core.rtc

import kotlinx.coroutines.flow.StateFlow

enum class RtcConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
}

/** Another rider in the room, as the UI needs it. */
data class RemoteRider(
    val id: String,
    val name: String,
    val speaking: Boolean,
    /** Muted locally by us: we do not hear them. */
    val mutedByMe: Boolean,
)

data class RoomState(
    val connection: RtcConnectionState = RtcConnectionState.DISCONNECTED,
    val microphoneOn: Boolean = false,
    val meSpeaking: Boolean = false,
    val riders: List<RemoteRider> = emptyList(),
)

/**
 * Thin wrapper over the LiveKit room for one riding group: connect, publish the
 * microphone, subscribe to the others and survive network changes
 * (see CLAUDE.md §5.3). One room per group; room name = group code.
 *
 * Audio routing is not its job: [AudioRouter][com.carmelitasoftworks.motointercom.core.audio.AudioRouter]
 * owns it, so the implementation must not touch the platform audio mode or devices.
 */
interface RtcClient {
    val state: StateFlow<RoomState>

    /** Joins the room at [url] with a [token]; returns once connected or throws. */
    suspend fun connect(url: String, token: String)

    /** Publishes or unpublishes the local microphone (mute / push-to-talk). */
    suspend fun setMicrophoneEnabled(enabled: Boolean)

    /** Stops or resumes hearing one rider, without affecting anyone else. */
    fun setRiderMuted(riderId: String, muted: Boolean)

    /** Linear gain applied to everyone we hear; 1.0 is unchanged. */
    fun setPlaybackGain(gain: Double)

    fun disconnect()
}
