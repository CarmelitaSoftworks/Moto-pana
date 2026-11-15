package com.carmelitasoftworks.motointercom.core.rtc

import kotlinx.coroutines.flow.StateFlow

enum class RtcConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
}

/**
 * Thin wrapper over the LiveKit room for one riding group: connect, publish the
 * microphone, subscribe to the others and survive network changes
 * (see CLAUDE.md §5.3). One room per group; room name = group id.
 */
interface RtcClient {
    val connectionState: StateFlow<RtcConnectionState>

    /** Joins the room at [url] with a [token] signed by the backend worker. */
    suspend fun connect(url: String, token: String)

    /** Publishes or unpublishes the local microphone (mute / push-to-talk). */
    suspend fun setMicrophoneEnabled(enabled: Boolean)

    suspend fun disconnect()
}
