package com.carmelitasoftworks.motointercom.core.nearby

import kotlin.math.PI
import kotlin.math.sin
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VoiceActivityDetectorTest {
    private fun tone(amplitude: Double) =
        ShortArray(320) { (amplitude * sin(2 * PI * 300 * it / 16_000.0)).toInt().toShort() }

    @Test
    fun `silence is not speech, a loud voice is`() {
        val vad = VoiceActivityDetector()
        repeat(20) { assertFalse(vad.isSpeech(tone(50.0))) }
        assertTrue(vad.isSpeech(tone(6_000.0)))
    }

    @Test
    fun `hangover keeps the mic open briefly after speech`() {
        val vad = VoiceActivityDetector(hangoverFrames = 3)
        vad.isSpeech(tone(6_000.0))
        repeat(3) { assertTrue(vad.isSpeech(tone(50.0))) }
        assertFalse(vad.isSpeech(tone(50.0)))
    }

    @Test
    fun `steady wind raises the threshold`() {
        val vad = VoiceActivityDetector()
        val wind = tone(1_500.0)
        // Loud constant noise opens the mic at first, then the floor adapts to it.
        repeat(400) { vad.isSpeech(wind) }
        repeat(20) { vad.isSpeech(wind) }
        assertFalse(vad.isSpeech(wind))
    }
}
