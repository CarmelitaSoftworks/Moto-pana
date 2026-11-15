package com.carmelitasoftworks.motointercom.core.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stand-in until the real Bluetooth routing lands (work order step 4).
 *
 * It never reports a route it did not set up: [route] stays [AudioRoute.NONE]
 * and [start] fails loudly instead of pretending audio is flowing.
 */
@Singleton
class UnimplementedAudioRouter @Inject constructor() : AudioRouter {
    override val route: StateFlow<AudioRoute> = MutableStateFlow(AudioRoute.NONE).asStateFlow()

    override fun start(): Unit = throw NotImplementedError("Audio routing arrives in work order step 4")

    override fun stop() = Unit
}
