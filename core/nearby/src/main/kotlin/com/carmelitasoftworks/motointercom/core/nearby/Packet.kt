package com.carmelitasoftworks.motointercom.core.nearby

import java.nio.ByteBuffer

/** What travels between phones. Clients only talk to the host; the host relays. */
internal sealed interface Packet {
    /** One frame of µ-law voice. [senderId] is set by the host when relaying; clients leave it empty. */
    class Audio(val senderId: String, val muLaw: ByteArray) : Packet

    /** Who else is in the group, as the host tells each client (never includes the recipient). */
    data class Roster(val riders: List<Pair<String, String>>) : Packet

    companion object {
        private const val TYPE_AUDIO: Byte = 1
        private const val TYPE_ROSTER: Byte = 2
        private const val BYTE_MASK = 0xFF

        fun encode(packet: Packet): ByteArray = when (packet) {
            is Audio -> {
                val id = packet.senderId.toByteArray()
                ByteBuffer.allocate(1 + 1 + id.size + packet.muLaw.size)
                    .put(TYPE_AUDIO)
                    .put(id.size.toByte())
                    .put(id)
                    .put(packet.muLaw)
                    .array()
            }
            is Roster -> {
                val entries = packet.riders.map { (id, name) -> id.toByteArray() to name.toByteArray() }
                val size = 1 + 1 + entries.sumOf { 2 + it.first.size + it.second.size }
                val buffer = ByteBuffer.allocate(size).put(TYPE_ROSTER).put(entries.size.toByte())
                entries.forEach { (id, name) ->
                    buffer.put(id.size.toByte()).put(id).put(name.size.toByte()).put(name)
                }
                buffer.array()
            }
        }

        /** Returns null for anything malformed or unknown, so a bad payload never crashes the ride. */
        fun decode(bytes: ByteArray): Packet? = runCatching {
            val buffer = ByteBuffer.wrap(bytes)
            when (buffer.get()) {
                TYPE_AUDIO -> {
                    val id = buffer.string()
                    Audio(id, ByteArray(buffer.remaining()).also { buffer.get(it) })
                }
                TYPE_ROSTER -> {
                    val count = buffer.get().toInt() and BYTE_MASK
                    Roster(List(count) { buffer.string() to buffer.string() })
                }
                else -> null
            }
        }.getOrNull()

        private fun ByteBuffer.string(): String {
            val length = get().toInt() and BYTE_MASK
            return String(ByteArray(length).also { get(it) })
        }
    }
}
