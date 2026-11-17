package com.carmelitasoftworks.motointercom.core.service

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupCodeTest {
    @Test
    fun `generated codes are valid`() {
        val random = Random(42)
        repeat(100) { assertTrue(GroupCode.isValid(GroupCode.generate(random))) }
    }

    @Test
    fun `normalize cleans what riders type`() {
        assertEquals("ABC234", GroupCode.normalize(" abc-234 "))
        assertEquals("ABC234", GroupCode.normalize("abc2345678"))
    }

    @Test
    fun `ambiguous characters are not valid`() {
        assertFalse(GroupCode.isValid("ABC10O"))
        assertFalse(GroupCode.isValid("ABC23"))
    }

    @Test
    fun `boost in decibels becomes a linear gain`() {
        assertEquals(1.0, IntercomSessionManager.dbToGain(0), 1e-9)
        assertEquals(2.0, IntercomSessionManager.dbToGain(6), 0.01)
    }
}
