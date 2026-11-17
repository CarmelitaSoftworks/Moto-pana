package com.carmelitasoftworks.motointercom.core.nearby

import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MuLawTest {
    @Test
    fun `round trip keeps samples close`() {
        val pcm = shortArrayOf(0, 100, -100, 1000, -1000, 8000, -8000, 32767, -32768)
        val decoded = MuLaw.decode(MuLaw.encode(pcm))
        pcm.indices.forEach { i ->
            val error = abs(decoded[i] - pcm[i])
            // µ-law keeps relative error small: under ~6% plus a few counts near zero.
            assertTrue(error <= abs(pcm[i].toInt()) / 16 + 16, "sample $i: ${pcm[i]} -> ${decoded[i]}")
        }
    }

    @Test
    fun `one byte per sample`() {
        assertEquals(320, MuLaw.encode(ShortArray(320)).size)
    }
}
