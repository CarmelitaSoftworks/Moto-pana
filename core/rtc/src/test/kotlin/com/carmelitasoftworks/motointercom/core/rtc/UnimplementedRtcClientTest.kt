package com.carmelitasoftworks.motointercom.core.rtc

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnimplementedRtcClientTest {
    @Test
    fun `never claims a connection it does not have`() = runTest {
        val client = UnimplementedRtcClient()

        val result = runCatching { client.connect(url = "wss://example.invalid", token = "token") }
        assertTrue(result.exceptionOrNull() is NotImplementedError)
        assertEquals(RtcConnectionState.DISCONNECTED, client.connectionState.value)

        client.disconnect()
        assertEquals(RtcConnectionState.DISCONNECTED, client.connectionState.value)
    }
}
