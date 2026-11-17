package com.carmelitasoftworks.motointercom.feature.ride

import androidx.lifecycle.ViewModel
import com.carmelitasoftworks.motointercom.core.service.IntercomSession
import com.carmelitasoftworks.motointercom.core.service.IntercomSessionManager
import com.carmelitasoftworks.motointercom.core.service.TalkMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class RideViewModel @Inject constructor(private val manager: IntercomSessionManager) : ViewModel() {
    val session: StateFlow<IntercomSession> = manager.session

    fun setSelfMuted(muted: Boolean) = manager.setSelfMuted(muted)

    fun setTalkMode(mode: TalkMode) = manager.setTalkMode(mode)

    fun setPushToTalkHeld(held: Boolean) = manager.setPushToTalkHeld(held)

    fun setRiderMuted(riderId: String, muted: Boolean) = manager.setRiderMuted(riderId, muted)

    fun leave() = manager.leave()

    fun dismissError() = manager.dismissError()
}
