package com.carmelitasoftworks.motointercom.core.rtc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RiderNameTest {
    @Test
    fun `round trips the display name`() {
        assertEquals("Juan", RiderName.display(RiderName.unique(" Juan ", "a1b2")))
    }

    @Test
    fun `keeps names that contain the separator`() {
        assertEquals("a~b", RiderName.display(RiderName.unique("a~b", "zz")))
    }

    @Test
    fun `never shows an empty name`() {
        assertEquals("?", RiderName.display("~zz"))
    }
}
