package com.carmelitasoftworks.motointercom.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carmelitasoftworks.motointercom.core.audio.AudioRoute
import com.carmelitasoftworks.motointercom.core.audio.AudioRouter
import com.carmelitasoftworks.motointercom.core.audio.MicLoopback
import com.carmelitasoftworks.motointercom.core.audio.SoundSettings
import com.carmelitasoftworks.motointercom.core.audio.SoundSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val audioRouter: AudioRouter,
    private val micLoopback: MicLoopback,
    private val soundSettings: SoundSettingsRepository,
) : ViewModel() {
    /** Where audio is going during the headset test. */
    val route: StateFlow<AudioRoute> = audioRouter.route

    /** How the rider hears the others (equalizer and volume boost). */
    val sound: StateFlow<SoundSettings> = soundSettings.settings

    private val mutableTesting = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = mutableTesting.asStateFlow()

    private var testJob: Job? = null

    /** Starts the headset test. The caller must hold the RECORD_AUDIO permission. */
    fun startTest() {
        if (testJob != null) return
        audioRouter.start()
        mutableTesting.value = true
        testJob = viewModelScope.launch {
            try {
                micLoopback.run(shouldPlay = { audioRouter.route.value.isHeadset })
            } finally {
                // Also runs when the ViewModel is cleared mid-test.
                audioRouter.stop()
                mutableTesting.value = false
                testJob = null
            }
        }
    }

    fun stopTest() {
        testJob?.cancel()
    }

    fun updateSound(settings: SoundSettings) = soundSettings.update(settings)
}
