package com.carmelitasoftworks.motointercom.core.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UnimplementedAudioRouterTest {
    @Test
    fun `never reports a route it did not set up`() {
        val router = UnimplementedAudioRouter()

        assertThrows(NotImplementedError::class.java) { router.start() }
        assertEquals(AudioRoute.NONE, router.route.value)

        router.stop()
        assertEquals(AudioRoute.NONE, router.route.value)
    }
}
