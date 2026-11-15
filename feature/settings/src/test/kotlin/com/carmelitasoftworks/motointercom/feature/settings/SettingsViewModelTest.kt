package com.carmelitasoftworks.motointercom.feature.settings

import com.carmelitasoftworks.motointercom.core.audio.AudioRoute
import com.carmelitasoftworks.motointercom.core.audio.AudioRouter
import com.carmelitasoftworks.motointercom.core.audio.MicLoopback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `headset test holds the audio route only while it runs`() = runTest(dispatcher) {
        val router = FakeAudioRouter()
        val viewModel = SettingsViewModel(router, micLoopback = { awaitCancellation() })

        viewModel.startTest()
        runCurrent()
        assertTrue(viewModel.testing.value)
        assertEquals(AudioRoute.BLUETOOTH_SCO, viewModel.route.value)

        viewModel.stopTest()
        runCurrent()
        assertFalse(viewModel.testing.value)
        assertEquals(AudioRoute.NONE, viewModel.route.value)
    }

    @Test
    fun `voice is only played back through a headset`() = runTest(dispatcher) {
        val router = FakeAudioRouter(routeWhenStarted = AudioRoute.SPEAKER)
        var playedOnSpeaker: Boolean? = null
        val loopback = MicLoopback { shouldPlay ->
            playedOnSpeaker = shouldPlay()
            awaitCancellation()
        }

        SettingsViewModel(router, loopback).startTest()
        runCurrent()

        assertEquals(false, playedOnSpeaker)
    }
}

private class FakeAudioRouter(private val routeWhenStarted: AudioRoute = AudioRoute.BLUETOOTH_SCO) : AudioRouter {
    override val route = MutableStateFlow(AudioRoute.NONE)

    override fun start() {
        route.value = routeWhenStarted
    }

    override fun stop() {
        route.value = AudioRoute.NONE
    }
}
