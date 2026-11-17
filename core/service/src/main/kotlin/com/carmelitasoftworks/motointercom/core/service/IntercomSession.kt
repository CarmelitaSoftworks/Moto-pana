package com.carmelitasoftworks.motointercom.core.service

import com.carmelitasoftworks.motointercom.core.audio.AudioRoute
import com.carmelitasoftworks.motointercom.core.rtc.RemoteRider
import com.carmelitasoftworks.motointercom.core.rtc.RtcConnectionState

enum class IntercomMode {
    /** Through the internet voice server: any distance, needs mobile data. */
    ONLINE,

    /** Phone to phone over Bluetooth / Wi-Fi: no internet, about 50–100 m. */
    NEARBY,
}

enum class TalkMode {
    /** Mic always published; Opus DTX keeps silence nearly free (CLAUDE.md §4, "voz abierta"). */
    OPEN,

    /** Mic only while the talk button is held. */
    PUSH_TO_TALK,
}

enum class JoinError {
    /** The app was built without a voice server. */
    SERVER_NOT_CONFIGURED,
    NO_NETWORK,
    SERVER_REJECTED,
    CONNECTION_FAILED,
}

/** Everything the UI and the notification show about the current ride. */
sealed interface IntercomSession {
    data object Idle : IntercomSession

    data class Failed(val error: JoinError) : IntercomSession

    data class Active(
        val code: String,
        val mode: IntercomMode,
        /** NEARBY: this phone hosts the group; leaving ends it for everyone. */
        val nearbyHost: Boolean,
        val connection: RtcConnectionState,
        val route: AudioRoute,
        val talkMode: TalkMode,
        /** The rider chose to mute themselves. */
        val selfMuted: Boolean,
        /** The mic is published right now (open mode and not muted, or PTT held). */
        val transmitting: Boolean,
        val meSpeaking: Boolean,
        val riders: List<RemoteRider>,
    ) : IntercomSession
}
