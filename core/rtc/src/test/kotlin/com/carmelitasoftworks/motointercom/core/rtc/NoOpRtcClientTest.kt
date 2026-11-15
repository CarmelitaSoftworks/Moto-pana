package com.carmelitasoftworks.motointercom.core.rtc

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoOpRtcClientTest {
    @Test
    fun `state follows connect and disconnect`() = runTest {
        val client = NoOpRtcClient()
        assertEquals(RtcConnectionState.DISCONNECTED, client.connectionState.value)

        client.connect(url = "wss://example.invalid", token = "token")
        assertEquals(RtcConnectionState.CONNECTED, client.connectionState.value)

        client.disconnect()
        assertEquals(RtcConnectionState.DISCONNECTED, client.connectionState.value)
    }
}
