package com.carmelitasoftworks.motointercom.core.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

interface SoundSettingsRepository {
    val settings: StateFlow<SoundSettings>

    /**
     * Applies [transform] to the latest settings, then stores and publishes the
     * result clamped to the supported ranges. Taking a transform (not a value)
     * keeps two quick changes from overwriting each other.
     */
    fun update(transform: (SoundSettings) -> SoundSettings)
}

/** Keeps the settings in SharedPreferences so they survive restarts. */
@Singleton
class PrefsSoundSettingsRepository @Inject constructor(@ApplicationContext context: Context) :
    SoundSettingsRepository {
    private val prefs = context.getSharedPreferences("sound_settings", Context.MODE_PRIVATE)

    private val mutableSettings = MutableStateFlow(
        SoundSettings(
            bassDb = prefs.getInt(KEY_BASS, 0),
            midDb = prefs.getInt(KEY_MID, 0),
            trebleDb = prefs.getInt(KEY_TREBLE, 0),
            boostDb = prefs.getInt(KEY_BOOST, 0),
        ).clamped(),
    )
    override val settings: StateFlow<SoundSettings> = mutableSettings.asStateFlow()

    override fun update(transform: (SoundSettings) -> SoundSettings) {
        val before = mutableSettings.value
        val clamped = mutableSettings.updateAndGet { transform(it).clamped() }
        if (clamped == before) return
        prefs.edit()
            .putInt(KEY_BASS, clamped.bassDb)
            .putInt(KEY_MID, clamped.midDb)
            .putInt(KEY_TREBLE, clamped.trebleDb)
            .putInt(KEY_BOOST, clamped.boostDb)
            .apply()
    }

    private companion object {
        const val KEY_BASS = "bass_db"
        const val KEY_MID = "mid_db"
        const val KEY_TREBLE = "treble_db"
        const val KEY_BOOST = "boost_db"
    }
}
