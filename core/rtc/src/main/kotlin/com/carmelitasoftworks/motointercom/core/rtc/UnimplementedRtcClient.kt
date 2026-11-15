package com.carmelitasoftworks.motointercom.core.rtc

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stand-in until the LiveKit-backed client lands (work order step 3).
 *
 * It never claims a connection it does not have: [connectionState] stays
 * [RtcConnectionState.DISCONNECTED] and connecting fails loudly.
 */
@Singleton
class UnimplementedRtcClient @Inject constructor() : RtcClient {
    override val connectionState: StateFlow<RtcConnectionState> =
        MutableStateFlow(RtcConnectionState.DISCONNECTED).asStateFlow()

    override suspend fun connect(url: String, token: String): Unit = notImplemented()

    override suspend fun setMicrophoneEnabled(enabled: Boolean): Unit = notImplemented()

    override suspend fun disconnect() = Unit

    private fun notImplemented(): Nothing = throw NotImplementedError("The LiveKit client arrives in work order step 3")
}
