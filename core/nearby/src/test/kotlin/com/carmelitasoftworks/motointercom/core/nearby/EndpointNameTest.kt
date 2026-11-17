package com.carmelitasoftworks.motointercom.core.nearby

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EndpointNameTest {
    @Test
    fun `group names round trip, even with separators in the host name`() {
        assertEquals(
            NearbyGroup(endpointId = "e1", groupId = "ABC234", hostName = "Ana | la jefa"),
            EndpointName.parseGroup("e1", EndpointName.group("ABC234", "Ana | la jefa")),
        )
    }

    @Test
    fun `other apps and riders are not groups`() {
        assertNull(EndpointName.parseGroup("e1", "some other app"))
        assertNull(EndpointName.parseGroup("e1", EndpointName.rider("Ana")))
    }

    @Test
    fun `rider names round trip`() {
        assertEquals("Luis", EndpointName.parseRider(EndpointName.rider("Luis")))
        assertEquals("?", EndpointName.parseRider(EndpointName.rider("")))
    }
}
