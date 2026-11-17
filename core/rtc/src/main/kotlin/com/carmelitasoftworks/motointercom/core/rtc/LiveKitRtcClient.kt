package com.carmelitasoftworks.motointercom.core.rtc

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.livekit.android.AudioOptions
import io.livekit.android.LiveKit
import io.livekit.android.LiveKitOverrides
import io.livekit.android.RoomOptions
import io.livekit.android.audio.NoAudioHandler
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.AudioTrackPublishDefaults
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.LocalAudioTrackOptions
import io.livekit.android.room.track.RemoteAudioTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [RtcClient] backed by the LiveKit Android SDK. Audio only. */
@Singleton
class LiveKitRtcClient @Inject constructor(@ApplicationContext private val context: Context) : RtcClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val mutableState = MutableStateFlow(RoomState())
    override val state: StateFlow<RoomState> = mutableState.asStateFlow()

    private var room: Room? = null
    private var eventsJob: Job? = null
    private val mutedRiders = mutableSetOf<String>()
    private var playbackGain = 1.0

    override suspend fun connect(url: String, token: String) {
        disconnect()
        mutableState.value = RoomState(connection = RtcConnectionState.CONNECTING)
        val newRoom = LiveKit.create(
            appContext = context,
            options = RoomOptions(
                adaptiveStream = true,
                dynacast = true,
                audioTrackCaptureDefaults = LocalAudioTrackOptions(
                    noiseSuppression = true,
                    echoCancellation = true,
                    autoGainControl = true,
                    highPassFilter = true,
                ),
                audioTrackPublishDefaults = AudioTrackPublishDefaults(
                    audioBitrate = AUDIO_BITRATE_BPS,
                    dtx = true,
                    red = true,
                ),
            ),
            // AudioRouter owns the audio mode and devices; LiveKit must not touch them.
            overrides = LiveKitOverrides(audioOptions = AudioOptions(audioHandler = NoAudioHandler())),
        )
        room = newRoom
        eventsJob = scope.launch { newRoom.events.collect { onEvent(newRoom, it) } }
        try {
            newRoom.connect(url, token)
        } catch (
            // LiveKit throws plain Exceptions.
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            disconnect()
            throw e
        }
        refresh(newRoom, RtcConnectionState.CONNECTED)
    }

    override suspend fun setMicrophoneEnabled(enabled: Boolean) {
        val current = room ?: return
        current.localParticipant.setMicrophoneEnabled(enabled)
        mutableState.update { it.copy(microphoneOn = enabled, meSpeaking = it.meSpeaking && enabled) }
    }

    override fun setRiderMuted(riderId: String, muted: Boolean) {
        if (muted) mutedRiders += riderId else mutedRiders -= riderId
        room?.let { applyVolumes(it) }
        mutableState.update { state ->
            state.copy(riders = state.riders.map { if (it.id == riderId) it.copy(mutedByMe = muted) else it })
        }
    }

    override fun setPlaybackGain(gain: Double) {
        playbackGain = gain.coerceIn(0.0, MAX_GAIN)
        room?.let { applyVolumes(it) }
    }

    override fun disconnect() {
        eventsJob?.cancel()
        eventsJob = null
        room?.let {
            it.disconnect()
            it.release()
        }
        room = null
        mutedRiders.clear()
        mutableState.value = RoomState()
    }

    private fun onEvent(room: Room, event: RoomEvent) {
        when (event) {
            is RoomEvent.Reconnecting -> refresh(room, RtcConnectionState.RECONNECTING)
            is RoomEvent.Reconnected -> refresh(room, RtcConnectionState.CONNECTED)
            is RoomEvent.Disconnected -> mutableState.update {
                it.copy(connection = RtcConnectionState.DISCONNECTED, riders = emptyList())
            }
            is RoomEvent.TrackSubscribed -> {
                applyVolumes(room)
                refresh(room)
            }
            is RoomEvent.ParticipantConnected,
            is RoomEvent.ParticipantDisconnected,
            is RoomEvent.ActiveSpeakersChanged,
            is RoomEvent.ParticipantNameChanged,
            -> refresh(room)
            else -> Unit
        }
    }

    private fun refresh(room: Room, connection: RtcConnectionState = mutableState.value.connection) {
        val speakers = room.activeSpeakers.map { it.identity?.value }.toSet()
        mutableState.update { state ->
            state.copy(
                connection = connection,
                meSpeaking = room.localParticipant.identity?.value in speakers,
                riders = room.remoteParticipants.values
                    .map { it.toRider(speakers) }
                    .sortedBy { it.name.lowercase() },
            )
        }
    }

    private fun RemoteParticipant.toRider(speakers: Set<String?>): RemoteRider {
        val id = identity?.value.orEmpty()
        return RemoteRider(
            id = id,
            name = RiderName.display(name ?: id),
            speaking = id in speakers,
            mutedByMe = id in mutedRiders,
        )
    }

    private fun applyVolumes(room: Room) {
        room.remoteParticipants.values.forEach { participant ->
            val volume = if (participant.identity?.value in mutedRiders) 0.0 else playbackGain
            participant.trackPublications.values
                .mapNotNull { it.track as? RemoteAudioTrack }
                .forEach { it.setVolume(volume) }
        }
    }

    private companion object {
        /** "Normal" quality from CLAUDE.md §4; Opus with DTX stays well under it in silence. */
        const val AUDIO_BITRATE_BPS = 20_000

        /** WebRTC accepts volume gains up to 10. */
        const val MAX_GAIN = 10.0
    }
}
