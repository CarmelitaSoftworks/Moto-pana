package com.carmelitasoftworks.motointercom.core.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SoundSettingsTest {
    @Test
    fun `each band covers its part of the spectrum`() {
        val settings = SoundSettings(bassDb = 1, midDb = 2, trebleDb = 3)

        assertEquals(1, settings.gainDbAt(60))
        assertEquals(2, settings.gainDbAt(910))
        assertEquals(3, settings.gainDbAt(3600))
    }

    @Test
    fun `out of range values are clamped`() {
        assertEquals(
            SoundSettings(bassDb = -9, midDb = 9, trebleDb = 0, boostDb = 12),
            SoundSettings(bassDb = -40, midDb = 40, trebleDb = 0, boostDb = 99).clamped(),
        )
        assertEquals(0, SoundSettings(boostDb = -5).clamped().boostDb)
    }

    @Test
    fun `a preset changes the equalizer but keeps the boost`() {
        val result = SoundPreset.CLEAR_VOICE.applyTo(SoundSettings(bassDb = 5, boostDb = 8))

        assertEquals(8, result.boostDb)
        assertTrue(SoundPreset.CLEAR_VOICE.matches(result))
        assertFalse(SoundPreset.NORMAL.matches(result))
    }

    @Test
    fun `default settings are the normal preset`() {
        assertTrue(SoundPreset.NORMAL.matches(SoundSettings()))
    }
}
