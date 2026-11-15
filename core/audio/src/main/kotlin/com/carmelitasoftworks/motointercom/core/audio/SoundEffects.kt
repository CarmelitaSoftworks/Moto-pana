package com.carmelitasoftworks.motointercom.core.audio

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import javax.inject.Inject

/** Applies the rider's [SoundSettings] to a playback stream using the platform audio effects. */
class SoundEffects @Inject constructor(private val repository: SoundSettingsRepository) {
    /**
     * Attaches the effects to [audioSessionId] and keeps them in sync with the
     * settings until the calling coroutine is cancelled.
     *
     * Some phones refuse effects on call audio; those are skipped and playback
     * simply stays unprocessed.
     */
    suspend fun applyTo(audioSessionId: Int): Nothing {
        val equalizer = runCatching { Equalizer(0, audioSessionId).apply { enabled = true } }.getOrNull()
        val enhancer = runCatching { LoudnessEnhancer(audioSessionId).apply { enabled = true } }.getOrNull()
        try {
            repository.settings.collect { settings ->
                runCatching {
                    equalizer?.apply(settings)
                    enhancer?.setTargetGain(settings.boostDb * MILLIBELS_PER_DB)
                }
            }
        } finally {
            equalizer?.release()
            enhancer?.release()
        }
    }

    /** Spreads the three bands over however many bands this phone's equalizer has. */
    private fun Equalizer.apply(settings: SoundSettings) {
        val (minLevel, maxLevel) = bandLevelRange
        for (band in 0 until numberOfBands) {
            val centerHz = getCenterFreq(band.toShort()) / MILLIHERTZ_PER_HZ
            val level = (settings.gainDbAt(centerHz) * MILLIBELS_PER_DB).coerceIn(minLevel.toInt(), maxLevel.toInt())
            setBandLevel(band.toShort(), level.toShort())
        }
    }

    private companion object {
        const val MILLIBELS_PER_DB = 100
        const val MILLIHERTZ_PER_HZ = 1000
    }
}
