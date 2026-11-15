package com.carmelitasoftworks.motointercom.core.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SoundSettingsRepository {
    val settings: StateFlow<SoundSettings>

    /** Stores [settings] (clamped to the supported ranges) and publishes them. */
    fun update(settings: SoundSettings)
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

    override fun update(settings: SoundSettings) {
        val clamped = settings.clamped()
        mutableSettings.value = clamped
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
