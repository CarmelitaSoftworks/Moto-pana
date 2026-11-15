package com.carmelitasoftworks.motointercom.core.rtc

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Placeholder used until the LiveKit-backed client lands (work order step 3).
 * It only tracks state; it opens no network connection.
 */
@Singleton
class NoOpRtcClient @Inject constructor() : RtcClient {
    private val mutableState = MutableStateFlow(RtcConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<RtcConnectionState> = mutableState.asStateFlow()

    override suspend fun connect(url: String, token: String) {
        mutableState.value = RtcConnectionState.CONNECTED
    }

    override suspend fun setMicrophoneEnabled(enabled: Boolean) = Unit

    override suspend fun disconnect() {
        mutableState.value = RtcConnectionState.DISCONNECTED
    }
}
