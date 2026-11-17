package com.carmelitasoftworks.motointercom.core.nearby

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PacketTest {
    @Test
    fun `audio round trips`() {
        val decoded = Packet.decode(Packet.encode(Packet.Audio("XY12", byteArrayOf(1, 2, 3)))) as Packet.Audio
        assertEquals("XY12", decoded.senderId)
        assertArrayEquals(byteArrayOf(1, 2, 3), decoded.muLaw)
    }

    @Test
    fun `roster round trips with accents`() {
        val roster = Packet.Roster(listOf("host" to "Ana", "XY12" to "Íñigo"))
        assertEquals(roster, Packet.decode(Packet.encode(roster)))
    }

    @Test
    fun `garbage is ignored`() {
        assertNull(Packet.decode(byteArrayOf()))
        assertNull(Packet.decode(byteArrayOf(9, 9)))
        assertNull(Packet.decode(byteArrayOf(2, 5, 1)))
    }
}
