package com.carmelitasoftworks.motointercom.core.nearby

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JitterMixerTest {
    private fun frame(value: Int) = ShortArray(4) { value.toShort() }

    @Test
    fun `waits for the prefill, then plays in order`() {
        val mixer = JitterMixer(frameSize = 4, prefillFrames = 2)
        val out = ShortArray(4)

        mixer.offer("a", frame(1))
        assertFalse(mixer.mixNext(out))

        mixer.offer("a", frame(2))
        assertTrue(mixer.mixNext(out))
        assertArrayEquals(frame(1), out)
        assertTrue(mixer.mixNext(out))
        assertArrayEquals(frame(2), out)
        assertFalse(mixer.mixNext(out))
    }

    @Test
    fun `two riders are summed and clipped`() {
        val mixer = JitterMixer(frameSize = 4, prefillFrames = 1)
        val out = ShortArray(4)
        mixer.offer("a", frame(30_000))
        mixer.offer("b", frame(10_000))

        mixer.mixNext(out)

        assertEquals(Short.MAX_VALUE, out[0])
    }

    @Test
    fun `a muted rider is not heard`() {
        val mixer = JitterMixer(frameSize = 4, prefillFrames = 1)
        mixer.setMuted("a", true)
        mixer.offer("a", frame(5))

        assertFalse(mixer.mixNext(ShortArray(4)))
    }

    @Test
    fun `queue is capped so delay cannot build up`() {
        val mixer = JitterMixer(frameSize = 4, prefillFrames = 1, maxQueuedFrames = 3)
        (1..10).forEach { mixer.offer("a", frame(it)) }
        val out = ShortArray(4)

        mixer.mixNext(out)

        assertArrayEquals(frame(8), out)
    }
}
