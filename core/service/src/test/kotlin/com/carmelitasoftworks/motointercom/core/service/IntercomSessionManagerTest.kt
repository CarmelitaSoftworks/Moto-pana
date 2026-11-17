package com.carmelitasoftworks.motointercom.core.service

import com.carmelitasoftworks.motointercom.core.audio.AudioRoute
import com.carmelitasoftworks.motointercom.core.audio.AudioRouter
import com.carmelitasoftworks.motointercom.core.audio.SoundSettings
import com.carmelitasoftworks.motointercom.core.audio.SoundSettingsRepository
import com.carmelitasoftworks.motointercom.core.nearby.NearbyError
import com.carmelitasoftworks.motointercom.core.nearby.NearbyGroup
import com.carmelitasoftworks.motointercom.core.nearby.NearbyIntercom
import com.carmelitasoftworks.motointercom.core.rtc.ConnectionDetails
import com.carmelitasoftworks.motointercom.core.rtc.RoomState
import com.carmelitasoftworks.motointercom.core.rtc.RtcClient
import com.carmelitasoftworks.motointercom.core.rtc.RtcConnectionState
import com.carmelitasoftworks.motointercom.core.rtc.TokenException
import com.carmelitasoftworks.motointercom.core.rtc.TokenProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntercomSessionManagerTest {
    private val rtc = FakeRtcClient()
    private val router = FakeAudioRouter()
    private val nearby = FakeNearby()

    private fun TestScope.manager(tokens: TokenProvider = TokenProvider { _, _ -> DETAILS }) =
        IntercomSessionManager(tokens, rtc, nearby, router, FakeSoundSettings(), backgroundScope)

    private val IntercomSessionManager.active: IntercomSession.Active
        get() = session.value as IntercomSession.Active

    @Test
    fun `joining connects, routes audio and opens the mic`() = runTest(UnconfinedTestDispatcher()) {
        val manager = manager()

        manager.join("ABC234", "Ana")

        assertEquals("ABC234", rtc.joinedRoomToken)
        assertEquals(RtcConnectionState.CONNECTED, manager.active.connection)
        assertEquals(AudioRoute.BLUETOOTH_SCO, manager.active.route)
        assertTrue(manager.active.transmitting)
    }

    @Test
    fun `push to talk only transmits while held`() = runTest(UnconfinedTestDispatcher()) {
        val manager = manager()
        manager.join("ABC234", "Ana")

        manager.setTalkMode(TalkMode.PUSH_TO_TALK)
        assertFalse(manager.active.transmitting)

        manager.setPushToTalkHeld(true)
        assertTrue(manager.active.transmitting)

        manager.setPushToTalkHeld(false)
        assertFalse(manager.active.transmitting)
    }

    @Test
    fun `self mute wins over push to talk`() = runTest(UnconfinedTestDispatcher()) {
        val manager = manager()
        manager.join("ABC234", "Ana")
        manager.setTalkMode(TalkMode.PUSH_TO_TALK)

        manager.setSelfMuted(true)
        manager.setPushToTalkHeld(true)

        assertFalse(manager.active.transmitting)
    }

    @Test
    fun `a missing server is reported and nothing stays open`() = runTest(UnconfinedTestDispatcher()) {
        val manager = manager(tokens = { _, _ -> throw TokenException(TokenException.Reason.NOT_CONFIGURED) })

        manager.join("ABC234", "Ana")

        assertEquals(IntercomSession.Failed(JoinError.SERVER_NOT_CONFIGURED), manager.session.value)
        assertEquals(AudioRoute.NONE, router.route.value)

        manager.dismissError()
        assertEquals(IntercomSession.Idle, manager.session.value)
    }

    @Test
    fun `a lost room is rejoined with a fresh token`() = runTest(UnconfinedTestDispatcher()) {
        var tokens = 0
        val manager = manager(tokens = { _, _ -> DETAILS.also { tokens++ } })
        manager.join("ABC234", "Ana")

        rtc.drop()
        assertEquals(RtcConnectionState.RECONNECTING, manager.active.connection)

        advanceTimeBy(1_500)
        assertEquals(2, tokens)
        assertEquals(RtcConnectionState.CONNECTED, manager.active.connection)
        assertTrue(manager.active.transmitting)
    }

    @Test
    fun `hosting a nearby group uses the radio, not the internet`() = runTest(UnconfinedTestDispatcher()) {
        val manager = manager(tokens = { _, _ -> error("no internet in nearby mode") })

        manager.hostNearby("Ana")

        assertEquals(IntercomMode.NEARBY, manager.active.mode)
        assertTrue(manager.active.nearbyHost)
        assertEquals(manager.active.code, nearby.hostedGroup)
        assertTrue(manager.active.transmitting)
        assertEquals(RtcConnectionState.DISCONNECTED, rtc.state.value.connection)
    }

    @Test
    fun `joining a nearby group and leaving it`() = runTest(UnconfinedTestDispatcher()) {
        val manager = manager()
        manager.joinNearby(NearbyGroup(endpointId = "e1", groupId = "XYZ789", hostName = "Luis"), "Ana")

        assertEquals("XYZ789", manager.active.code)
        assertFalse(manager.active.nearbyHost)

        manager.leave()
        assertEquals(IntercomSession.Idle, manager.session.value)
        assertEquals(RtcConnectionState.DISCONNECTED, nearby.state.value.connection)
    }

    @Test
    fun `leaving closes everything`() = runTest(UnconfinedTestDispatcher()) {
        val manager = manager()
        manager.join("ABC234", "Ana")

        manager.leave()

        assertEquals(IntercomSession.Idle, manager.session.value)
        assertEquals(RtcConnectionState.DISCONNECTED, rtc.state.value.connection)
        assertEquals(AudioRoute.NONE, router.route.value)
    }

    private companion object {
        val DETAILS = ConnectionDetails(serverUrl = "wss://example.invalid", token = "ABC234")
    }
}

private class FakeRtcClient : RtcClient {
    override val state = MutableStateFlow(RoomState())
    var joinedRoomToken: String? = null

    override suspend fun connect(url: String, token: String) {
        joinedRoomToken = token
        state.value = RoomState(connection = RtcConnectionState.CONNECTED)
    }

    override suspend fun setMicrophoneEnabled(enabled: Boolean) {
        state.update { it.copy(microphoneOn = enabled) }
    }

    override fun setRiderMuted(riderId: String, muted: Boolean) = Unit

    override fun setPlaybackGain(gain: Double) = Unit

    override fun disconnect() {
        state.value = RoomState()
    }

    fun drop() {
        state.value = RoomState()
    }
}

private class FakeNearby : NearbyIntercom {
    override val state = MutableStateFlow(RoomState())
    override val groups = MutableStateFlow<List<NearbyGroup>>(emptyList())
    override val error = MutableStateFlow<NearbyError?>(null)
    var hostedGroup: String? = null

    override fun startDiscovery() = Unit

    override fun restartDiscovery() = Unit

    override fun stopDiscovery() = Unit

    override fun host(groupId: String, myName: String) {
        hostedGroup = groupId
        state.value = RoomState(connection = RtcConnectionState.CONNECTED)
    }

    override fun join(group: NearbyGroup, myName: String) {
        state.value = RoomState(connection = RtcConnectionState.CONNECTED)
    }

    override suspend fun setMicrophoneEnabled(enabled: Boolean) {
        state.update { it.copy(microphoneOn = enabled) }
    }

    override fun setRiderMuted(riderId: String, muted: Boolean) = Unit

    override fun setPlaybackGain(gain: Double) = Unit

    override fun disconnect() {
        state.value = RoomState()
    }
}

private class FakeAudioRouter : AudioRouter {
    override val route = MutableStateFlow(AudioRoute.NONE)

    override fun start() {
        route.value = AudioRoute.BLUETOOTH_SCO
    }

    override fun stop() {
        route.value = AudioRoute.NONE
    }
}

private class FakeSoundSettings : SoundSettingsRepository {
    override val settings = MutableStateFlow(SoundSettings())

    override fun update(transform: (SoundSettings) -> SoundSettings) {
        settings.update(transform)
    }
}
