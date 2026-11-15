package com.carmelitasoftworks.motointercom.core.audio

import kotlinx.coroutines.flow.StateFlow

/** Where intercom audio is currently being played and captured. */
enum class AudioRoute {
    /** No session audio is routed. */
    NONE,

    /** Helmet headset over classic Bluetooth (HFP/SCO). */
    BLUETOOTH_SCO,

    /** Helmet headset over LE Audio. */
    BLUETOOTH_LE,

    /** Phone loudspeaker, used as fallback when no headset is connected. */
    SPEAKER,
}

/**
 * Owns audio routing for an intercom session: audio mode, audio focus and the
 * choice of Bluetooth headset vs. speaker fallback.
 *
 * Kept behind an interface so it can be faked in tests and so manufacturer
 * quirks stay contained in the implementation (see CLAUDE.md §5.1).
 */
interface AudioRouter {
    /** The route currently in use; updates when the headset connects or drops. */
    val route: StateFlow<AudioRoute>

    /** Enters communication mode, requests audio focus and picks the best route. */
    fun start()

    /** Restores the audio state that was in place before [start]. */
    fun stop()
}
