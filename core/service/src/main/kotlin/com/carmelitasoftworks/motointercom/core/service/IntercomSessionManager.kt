package com.carmelitasoftworks.motointercom.core.service

import com.carmelitasoftworks.motointercom.core.audio.AudioRouter
import com.carmelitasoftworks.motointercom.core.audio.SoundSettingsRepository
import com.carmelitasoftworks.motointercom.core.nearby.NearbyGroup
import com.carmelitasoftworks.motointercom.core.nearby.NearbyIntercom
import com.carmelitasoftworks.motointercom.core.rtc.RiderName
import com.carmelitasoftworks.motointercom.core.rtc.RtcClient
import com.carmelitasoftworks.motointercom.core.rtc.RtcConnectionState
import com.carmelitasoftworks.motointercom.core.rtc.TokenException
import com.carmelitasoftworks.motointercom.core.rtc.TokenProvider
import com.carmelitasoftworks.motointercom.core.rtc.VoiceTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns one ride: transport (internet room or nearby radio), audio route and talk
 * mode. Lives as long as the app process; [IntercomService] keeps that process
 * alive in the background.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class IntercomSessionManager @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val rtcClient: RtcClient,
    private val nearby: NearbyIntercom,
    private val audioRouter: AudioRouter,
    private val soundSettings: SoundSettingsRepository,
    @SessionScope private val scope: CoroutineScope,
) {
    private data class Control(
        val code: String? = null,
        val mode: IntercomMode = IntercomMode.ONLINE,
        /** NEARBY: this phone hosts the group (and relays everyone's voice). */
        val nearbyHost: Boolean = false,
        val error: JoinError? = null,
        val talkMode: TalkMode = TalkMode.OPEN,
        val selfMuted: Boolean = false,
        val pttHeld: Boolean = false,
        /** We were in the room at least once, so a dropped room is a reconnect, not a first connect. */
        val everConnected: Boolean = false,
    ) {
        val wantsMic: Boolean
            get() = !selfMuted && (talkMode == TalkMode.OPEN || pttHeld)
    }

    private val control = MutableStateFlow(Control())
    private var joinJob: Job? = null
    private var soundJob: Job? = null

    private val transport: VoiceTransport
        get() = if (control.value.mode == IntercomMode.NEARBY) nearby else rtcClient

    private val transportState = control
        .map { it.mode }
        .distinctUntilChanged()
        .flatMapLatest { mode -> if (mode == IntercomMode.NEARBY) nearby.state else rtcClient.state }

    val session: StateFlow<IntercomSession> =
        combine(control, transportState, audioRouter.route) { control, room, route ->
            when {
                control.code != null -> IntercomSession.Active(
                    code = control.code,
                    mode = control.mode,
                    nearbyHost = control.nearbyHost,
                    connection = when {
                        room.connection != RtcConnectionState.DISCONNECTED -> room.connection
                        control.everConnected -> RtcConnectionState.RECONNECTING
                        else -> RtcConnectionState.CONNECTING
                    },
                    route = route,
                    talkMode = control.talkMode,
                    selfMuted = control.selfMuted,
                    transmitting = room.microphoneOn,
                    meSpeaking = room.meSpeaking,
                    riders = room.riders,
                )
                control.error != null -> IntercomSession.Failed(control.error)
                else -> IntercomSession.Idle
            }
        }.stateIn(scope, SharingStarted.Eagerly, IntercomSession.Idle)

    /** Joins (or creates: rooms are created on first join) the internet group [code]. Main thread. */
    fun join(code: String, displayName: String) {
        begin(Control(code = code))
        joinJob = scope.launch { stayConnected(code, displayName) }
    }

    /** Starts a group without internet, hosted by this phone. Main thread. */
    fun hostNearby(displayName: String) {
        val code = GroupCode.generate()
        begin(Control(code = code, mode = IntercomMode.NEARBY, nearbyHost = true))
        nearby.host(code, displayName)
        scope.launch { applyMic() }
    }

    /** Joins a group without internet that someone nearby is hosting. Main thread. */
    fun joinNearby(group: NearbyGroup, displayName: String) {
        begin(Control(code = group.groupId, mode = IntercomMode.NEARBY))
        nearby.join(group, displayName)
        scope.launch { applyMic() }
    }

    private fun begin(initial: Control) {
        leave()
        control.value = initial
        audioRouter.start()
        soundJob = scope.launch {
            soundSettings.settings.collect { transport.setPlaybackGain(dbToGain(it.boostDb)) }
        }
    }

    /**
     * Connects, and reconnects with a fresh token whenever the room is lost for
     * good (LiveKit already retries short drops on its own). Only the first
     * connection reports errors; afterwards it keeps retrying until [leave].
     */
    private suspend fun stayConnected(code: String, displayName: String) {
        val participant = RiderName.unique(displayName, Random.nextInt(SUFFIX_RANGE).toString(SUFFIX_RADIX))
        var retryDelayMs = FIRST_RETRY_MS
        while (true) {
            try {
                val details = tokenProvider.fetch(roomName = code, participantName = participant)
                rtcClient.connect(details.serverUrl, details.token)
                control.update { it.copy(everConnected = true) }
                retryDelayMs = FIRST_RETRY_MS
                applyMic()
                rtcClient.state.first { it.connection == RtcConnectionState.DISCONNECTED }
            } catch (e: CancellationException) {
                throw e
            } catch (e: TokenException) {
                val error = e.toJoinError()
                if (!control.value.everConnected || error != JoinError.NO_NETWORK) return fail(error)
            } catch (
                // LiveKit throws plain Exceptions.
                @Suppress(
                    "TooGenericExceptionCaught",
                    "SwallowedException",
                ) e: Exception,
            ) {
                if (!control.value.everConnected) return fail(JoinError.CONNECTION_FAILED)
            }
            delay(retryDelayMs)
            retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
        }
    }

    fun leave() {
        joinJob?.cancel()
        joinJob = null
        soundJob?.cancel()
        soundJob = null
        transport.disconnect()
        audioRouter.stop()
        control.value = Control()
    }

    /** Forgets a failed join so the UI goes back to idle. */
    fun dismissError() {
        if (control.value.code == null) control.value = Control()
    }

    fun setSelfMuted(muted: Boolean) = updateMic { it.copy(selfMuted = muted) }

    fun setTalkMode(mode: TalkMode) = updateMic { it.copy(talkMode = mode, pttHeld = false) }

    fun setPushToTalkHeld(held: Boolean) = updateMic { it.copy(pttHeld = held) }

    fun setRiderMuted(riderId: String, muted: Boolean) = transport.setRiderMuted(riderId, muted)

    private fun updateMic(transform: (Control) -> Control) {
        if (control.value.code == null) return
        control.update(transform)
        scope.launch { applyMic() }
    }

    private suspend fun applyMic() {
        val current = transport
        // The internet room can only publish once connected; nearby takes the flag at any time.
        val online = control.value.mode == IntercomMode.ONLINE
        if (online && current.state.value.connection == RtcConnectionState.DISCONNECTED) return
        val wanted = control.value.wantsMic
        if (current.state.value.microphoneOn != wanted) {
            runCatching { current.setMicrophoneEnabled(wanted) }
        }
    }

    private fun fail(error: JoinError) {
        val failed = Control(error = error)
        leave()
        control.value = failed
    }

    internal companion object {
        private const val FIRST_RETRY_MS = 1_000L
        private const val MAX_RETRY_MS = 8_000L

        private const val SUFFIX_RANGE = 0x10000
        private const val SUFFIX_RADIX = 16
        private const val DB_PER_DECADE = 20.0

        fun dbToGain(db: Int): Double = 10.0.pow(db / DB_PER_DECADE)
    }
}

private fun TokenException.toJoinError(): JoinError = when (reason) {
    TokenException.Reason.NOT_CONFIGURED -> JoinError.SERVER_NOT_CONFIGURED
    TokenException.Reason.NETWORK -> JoinError.NO_NETWORK
    TokenException.Reason.REJECTED -> JoinError.SERVER_REJECTED
}
