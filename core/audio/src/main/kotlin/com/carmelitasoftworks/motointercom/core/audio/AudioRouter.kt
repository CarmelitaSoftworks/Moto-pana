package com.carmelitasoftworks.motointercom.core.audio

import kotlinx.coroutines.flow.StateFlow

/** Where intercom audio is currently being played and captured. */
enum class AudioRoute {
    /** Nothing is routed: no session, or a Bluetooth link that is still opening. */
    NONE,

    /** Helmet headset over classic Bluetooth hands-free (HFP/SCO). */
    BLUETOOTH_SCO,

    /** Helmet headset over LE Audio. */
    BLUETOOTH_LE,

    /** Wired or USB headset. */
    WIRED_HEADSET,

    /** Phone loudspeaker, used as fallback when no headset is connected. */
    SPEAKER,
    ;

    /** True when the rider hears the audio privately, so playback cannot feed back into the mic. */
    val isHeadset: Boolean
        get() = this == BLUETOOTH_SCO || this == BLUETOOTH_LE || this == WIRED_HEADSET
}

/**
 * Owns audio routing for an intercom session: audio mode, audio focus and the
 * choice of headset vs. speaker fallback.
 *
 * Works with any headset the phone treats as a hands-free device, whatever the
 * brand. Kept behind an interface so it can be faked in tests and so
 * manufacturer quirks stay contained in the implementation (see CLAUDE.md §5.1).
 *
 * [start] and [stop] must be called from the main thread.
 */
interface AudioRouter {
    /** The route currently in use; updates when a headset connects or drops. */
    val route: StateFlow<AudioRoute>

    /** Enters communication mode, requests audio focus and picks the best route. */
    fun start()

    /** Restores the audio state that was in place before [start]. */
    fun stop()
}
