package com.carmelitasoftworks.motointercom.core.audio

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoOpAudioRouterTest {
    @Test
    fun `route follows start and stop`() = runTest {
        val router = NoOpAudioRouter()

        router.route.test {
            assertEquals(AudioRoute.NONE, awaitItem())

            router.start()
            assertEquals(AudioRoute.SPEAKER, awaitItem())

            router.stop()
            assertEquals(AudioRoute.NONE, awaitItem())
        }
    }
}
