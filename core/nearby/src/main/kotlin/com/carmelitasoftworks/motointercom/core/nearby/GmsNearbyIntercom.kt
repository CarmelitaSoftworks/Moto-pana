package com.carmelitasoftworks.motointercom.core.nearby

import android.content.Context
import android.os.SystemClock
import com.carmelitasoftworks.motointercom.core.audio.SoundEffects
import com.carmelitasoftworks.motointercom.core.rtc.RemoteRider
import com.carmelitasoftworks.motointercom.core.rtc.RoomState
import com.carmelitasoftworks.motointercom.core.rtc.RtcConnectionState
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * [NearbyIntercom] over Google Play services' Nearby Connections, star topology:
 * the host relays every client's voice to the others and plays it itself.
 *
 * Nearby callbacks arrive on the main thread; captured audio on a background thread.
 */
// An adapter over Nearby's callback API: many small handlers are clearer than fewer big ones.
@Suppress("TooManyFunctions")
@Singleton
class GmsNearbyIntercom @Inject constructor(
    @ApplicationContext context: Context,
    soundEffects: SoundEffects,
) : NearbyIntercom {
    private val client = Nearby.getConnectionsClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val mutableState = MutableStateFlow(RoomState())
    override val state: StateFlow<RoomState> = mutableState.asStateFlow()

    private val mutableGroups = MutableStateFlow<List<NearbyGroup>>(emptyList())
    override val groups: StateFlow<List<NearbyGroup>> = mutableGroups.asStateFlow()

    private val mutableError = MutableStateFlow<NearbyError?>(null)
    override val error: StateFlow<NearbyError?> = mutableError.asStateFlow()

    private enum class Role { NONE, HOST, CLIENT }

    @Volatile private var role = Role.NONE
    private var myName = ""
    private var discovering = false

    /** CLIENT: the group we belong to, and the host endpoint while connected to it. */
    private var targetGroupId: String? = null

    @Volatile private var hostEndpoint: String? = null

    /** HOST: connected clients by endpoint id -> name. Read from the capture thread. */
    @Volatile private var clients: Map<String, String> = emptyMap()
    private val pendingNames = HashMap<String, String>()

    /** CLIENT: everyone else, as the host's roster says. */
    private var roster: List<Pair<String, String>> = emptyList()
    private val mutedRiders = HashSet<String>()
    private val lastHeard = HashMap<String, Long>()
    private var tickerJob: Job? = null

    private val pipeline = VoicePipeline(
        soundEffects = soundEffects,
        onFrame = ::sendOwnFrame,
        onSpeaking = { speaking -> mutableState.update { it.copy(meSpeaking = speaking) } },
    )

    override fun startDiscovery() {
        if (discovering) return
        discovering = true
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        client.startDiscovery(SERVICE_ID, discoveryCallback, options)
            .addOnSuccessListener { mutableError.value = null }
            .addOnFailureListener {
                discovering = false
                mutableError.value = NearbyError.from(it)
            }
    }

    override fun restartDiscovery() {
        if (discovering) {
            discovering = false
            client.stopDiscovery()
        }
        startDiscovery()
    }

    override fun stopDiscovery() {
        if (!discovering) return
        discovering = false
        client.stopDiscovery()
        mutableGroups.value = emptyList()
    }

    override fun host(groupId: String, myName: String) {
        disconnect()
        role = Role.HOST
        this.myName = myName
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        client.startAdvertising(EndpointName.group(groupId, myName), SERVICE_ID, connectionCallback, options)
            .addOnSuccessListener {
                mutableError.value = null
                mutableState.update { it.copy(connection = RtcConnectionState.CONNECTED) }
            }
            .addOnFailureListener { failure ->
                mutableError.value = NearbyError.from(failure)
                mutableState.update { it.copy(connection = RtcConnectionState.DISCONNECTED) }
            }
        startAudio(RtcConnectionState.CONNECTING)
    }

    override fun join(group: NearbyGroup, myName: String) {
        disconnect()
        role = Role.CLIENT
        this.myName = myName
        targetGroupId = group.groupId
        startAudio(RtcConnectionState.CONNECTING)
        requestConnection(group.endpointId)
    }

    override suspend fun setMicrophoneEnabled(enabled: Boolean) {
        pipeline.transmitting = enabled
        mutableState.update { it.copy(microphoneOn = enabled, meSpeaking = it.meSpeaking && enabled) }
    }

    override fun setRiderMuted(riderId: String, muted: Boolean) {
        if (muted) mutedRiders += riderId else mutedRiders -= riderId
        pipeline.mixer.setMuted(riderId, muted)
        publishRiders()
    }

    /** The volume boost reaches this mode through [SoundEffects] on our own playback track. */
    override fun setPlaybackGain(gain: Double) = Unit

    override fun disconnect() {
        if (role == Role.HOST) client.stopAdvertising()
        stopDiscovery()
        client.stopAllEndpoints()
        pipeline.stop()
        tickerJob?.cancel()
        tickerJob = null
        role = Role.NONE
        targetGroupId = null
        hostEndpoint = null
        clients = emptyMap()
        pendingNames.clear()
        roster = emptyList()
        mutedRiders.clear()
        lastHeard.clear()
        mutableError.value = null
        mutableState.value = RoomState()
    }

    private fun startAudio(connection: RtcConnectionState) {
        mutableState.value = RoomState(connection = connection)
        pipeline.start()
        tickerJob = scope.launch {
            // Speaking indicators fade out when a rider's frames stop arriving.
            while (isActive) {
                delay(SPEAKING_TICK_MS)
                publishRiders()
            }
        }
    }

    private fun requestConnection(endpointId: String) {
        client.requestConnection(EndpointName.rider(myName), endpointId, connectionCallback)
            .addOnFailureListener {
                mutableError.value = NearbyError.from(it)
                lookForHostAgain()
            }
    }

    /** CLIENT: the host went out of range (or refused); wait for it to show up again. */
    private fun lookForHostAgain() {
        if (role != Role.CLIENT) return
        hostEndpoint = null
        roster = emptyList()
        mutableState.update { it.copy(connection = RtcConnectionState.RECONNECTING) }
        publishRiders()
        startDiscovery()
    }

    private fun sendOwnFrame(frame: ShortArray) {
        val bytes = Packet.encode(Packet.Audio(senderId = "", muLaw = MuLaw.encode(frame)))
        val targets = when (role) {
            Role.HOST -> clients.keys.toList()
            Role.CLIENT -> listOfNotNull(hostEndpoint)
            Role.NONE -> emptyList()
        }
        if (targets.isNotEmpty()) client.sendPayload(targets, Payload.fromBytes(bytes))
    }

    private fun onPacket(from: String, packet: Packet) {
        when (packet) {
            is Packet.Audio -> {
                // Clients send with an empty id; the host stamps who said it before relaying.
                val sender = if (role == Role.HOST) from else packet.senderId.ifEmpty { HOST_ID }
                if (role == Role.HOST) {
                    val relay = Packet.encode(Packet.Audio(sender, packet.muLaw))
                    val others = clients.keys.filter { it != from }
                    if (others.isNotEmpty()) client.sendPayload(others, Payload.fromBytes(relay))
                }
                if (sender !in mutedRiders) {
                    pipeline.mixer.offer(sender, MuLaw.decode(packet.muLaw))
                    lastHeard[sender] = SystemClock.elapsedRealtime()
                }
            }
            is Packet.Roster -> if (role == Role.CLIENT) {
                roster = packet.riders
                publishRiders()
            }
        }
    }

    /** HOST: tells each client who else is in the group (everyone but themselves). */
    private fun broadcastRoster() {
        val everyone = listOf(HOST_ID to myName) + clients.toList()
        clients.keys.forEach { recipient ->
            val packet = Packet.Roster(everyone.filter { it.first != recipient })
            client.sendPayload(recipient, Payload.fromBytes(Packet.encode(packet)))
        }
        publishRiders()
    }

    private fun publishRiders() {
        val now = SystemClock.elapsedRealtime()
        val others = if (role == Role.HOST) clients.toList() else roster
        val riders = others.map { (id, name) ->
            RemoteRider(
                id = id,
                name = name,
                speaking = (lastHeard[id] ?: 0L) > now - SPEAKING_WINDOW_MS,
                mutedByMe = id in mutedRiders,
            )
        }.sortedBy { it.name.lowercase() }
        if (riders != mutableState.value.riders) mutableState.update { it.copy(riders = riders) }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            Packet.decode(bytes)?.let { onPacket(endpointId, it) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Riders join by picking the group on screen; there is nothing secret to protect here.
            if (role == Role.HOST) pendingNames[endpointId] = EndpointName.parseRider(info.endpointName)
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            val ok = resolution.status.isSuccess
            when (role) {
                Role.HOST -> {
                    val name = pendingNames.remove(endpointId)
                    if (ok && name != null) {
                        clients = clients + (endpointId to name)
                        broadcastRoster()
                    }
                }
                Role.CLIENT -> if (ok) {
                    mutableError.value = null
                    hostEndpoint = endpointId
                    // Discovery competes with the link for the radio; stop it while connected.
                    stopDiscovery()
                    mutableState.update { it.copy(connection = RtcConnectionState.CONNECTED) }
                } else {
                    lookForHostAgain()
                }
                Role.NONE -> Unit
            }
        }

        override fun onDisconnected(endpointId: String) {
            when (role) {
                Role.HOST -> {
                    clients = clients - endpointId
                    pipeline.mixer.remove(endpointId)
                    broadcastRoster()
                }
                Role.CLIENT -> if (endpointId == hostEndpoint) lookForHostAgain()
                Role.NONE -> Unit
            }
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val group = EndpointName.parseGroup(endpointId, info.endpointName) ?: return
            mutableGroups.update { list -> list.filter { it.endpointId != endpointId } + group }
            // A client that lost its host rejoins as soon as the same group is in range again.
            if (role == Role.CLIENT && hostEndpoint == null && group.groupId == targetGroupId) {
                requestConnection(endpointId)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            mutableGroups.update { list -> list.filter { it.endpointId != endpointId } }
        }
    }

    private companion object {
        const val SERVICE_ID = "com.carmelitasoftworks.motointercom.nearby"
        val STRATEGY: Strategy = Strategy.P2P_STAR

        /** How clients refer to the host in rosters and audio. */
        const val HOST_ID = "host"
        const val SPEAKING_TICK_MS = 250L
        const val SPEAKING_WINDOW_MS = 400L
    }
}

/** How groups and riders name themselves to Nearby: "G|groupId|host name" and "R|name". */
internal object EndpointName {
    private const val GROUP = "G"
    private const val RIDER = "R"
    private const val SEPARATOR = "|"
    private const val GROUP_PARTS = 3

    fun group(groupId: String, hostName: String) = listOf(GROUP, groupId, hostName).joinToString(SEPARATOR)

    fun rider(name: String) = RIDER + SEPARATOR + name

    fun parseGroup(endpointId: String, endpointName: String): NearbyGroup? {
        val parts = endpointName.split(SEPARATOR, limit = GROUP_PARTS)
        if (parts.size != GROUP_PARTS || parts[0] != GROUP || parts[1].isBlank()) return null
        return NearbyGroup(endpointId = endpointId, groupId = parts[1], hostName = parts[2].ifBlank { "?" })
    }

    fun parseRider(endpointName: String): String = endpointName.removePrefix(RIDER + SEPARATOR).ifBlank { "?" }
}
