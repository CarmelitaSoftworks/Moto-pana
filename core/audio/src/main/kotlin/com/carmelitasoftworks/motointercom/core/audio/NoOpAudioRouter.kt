package com.carmelitasoftworks.motointercom.core.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Placeholder used until the real Bluetooth routing lands (work order step 4).
 * It only tracks state; it does not touch the platform AudioManager.
 */
@Singleton
class NoOpAudioRouter @Inject constructor() : AudioRouter {
    private val mutableRoute = MutableStateFlow(AudioRoute.NONE)
    override val route: StateFlow<AudioRoute> = mutableRoute.asStateFlow()

    override fun start() {
        mutableRoute.value = AudioRoute.SPEAKER
    }

    override fun stop() {
        mutableRoute.value = AudioRoute.NONE
    }
}
