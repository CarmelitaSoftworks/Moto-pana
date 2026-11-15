package com.carmelitasoftworks.motointercom.core.audio

/**
 * How the rider wants to hear the others. Only shapes playback; it never
 * changes what the microphone sends.
 *
 * Three bands are enough: a hands-free link carries narrowband voice, so
 * finer control would not be audible.
 */
data class SoundSettings(
    val bassDb: Int = 0,
    val midDb: Int = 0,
    val trebleDb: Int = 0,
    /** Extra loudness on top of full volume, to get over wind and engine noise. */
    val boostDb: Int = 0,
) {
    /** The equalizer gain that applies at [frequencyHz]. */
    fun gainDbAt(frequencyHz: Int): Int = when {
        frequencyHz < BASS_UPPER_HZ -> bassDb
        frequencyHz < MID_UPPER_HZ -> midDb
        else -> trebleDb
    }

    /** A copy with every value forced into its supported range. */
    fun clamped(): SoundSettings = SoundSettings(
        bassDb = bassDb.coerceIn(EQ_RANGE_DB),
        midDb = midDb.coerceIn(EQ_RANGE_DB),
        trebleDb = trebleDb.coerceIn(EQ_RANGE_DB),
        boostDb = boostDb.coerceIn(BOOST_RANGE_DB),
    )

    companion object {
        val EQ_RANGE_DB = -9..9
        val BOOST_RANGE_DB = 0..12
        private const val BASS_UPPER_HZ = 300
        private const val MID_UPPER_HZ = 2500
    }
}

/** One-tap equalizer shapes. They leave [SoundSettings.boostDb] alone. */
enum class SoundPreset(private val bassDb: Int, private val midDb: Int, private val trebleDb: Int) {
    NORMAL(bassDb = 0, midDb = 0, trebleDb = 0),

    /** Cuts rumble and lifts consonants so speech stays intelligible at speed. */
    CLEAR_VOICE(bassDb = -4, midDb = 3, trebleDb = 6),

    MORE_BASS(bassDb = 6, midDb = 0, trebleDb = -2),
    ;

    fun applyTo(settings: SoundSettings): SoundSettings =
        settings.copy(bassDb = bassDb, midDb = midDb, trebleDb = trebleDb)

    fun matches(settings: SoundSettings): Boolean = applyTo(settings) == settings
}
