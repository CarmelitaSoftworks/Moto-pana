package com.carmelitasoftworks.motointercom.core.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import javax.inject.Inject

/**
 * Applies the rider's [SoundSettings] to a playback stream using the platform audio effects.
 *
 * Today the only stream is the headset test; the intercom's own playback gets
 * wired in together with the RTC client (work order step 3).
 */
class SoundEffects @Inject constructor(private val repository: SoundSettingsRepository) {
    /**
     * Attaches the effects to [audioSessionId] and keeps them in sync with the
     * settings until the calling coroutine is cancelled.
     *
     * Some phones refuse effects on call audio; a refused effect is skipped
     * without affecting the other one, and playback simply stays unprocessed.
     */
    suspend fun applyTo(audioSessionId: Int): Nothing {
        val equalizer = enabledOrNull { Equalizer(0, audioSessionId) }
        val enhancer = enabledOrNull { LoudnessEnhancer(audioSessionId) }
        try {
            repository.settings.collect { settings ->
                runCatching { equalizer?.setBands(settings) }
                runCatching { enhancer?.setTargetGain(settings.boostDb * MILLIBELS_PER_DB) }
            }
        } finally {
            equalizer?.release()
            enhancer?.release()
        }
    }

    /** Creates and enables an effect, releasing it again if the phone refuses to enable it. */
    private fun <T : AudioEffect> enabledOrNull(create: () -> T): T? {
        val effect = runCatching(create).getOrNull() ?: return null
        return runCatching { effect.apply { enabled = true } }
            .onFailure { effect.release() }
            .getOrNull()
    }

    /** Spreads the three bands over however many bands this phone's equalizer has. */
    private fun Equalizer.setBands(settings: SoundSettings) {
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
