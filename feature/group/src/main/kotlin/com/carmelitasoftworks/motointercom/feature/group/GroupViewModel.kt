package com.carmelitasoftworks.motointercom.feature.group

import androidx.lifecycle.ViewModel
import com.carmelitasoftworks.motointercom.core.service.GroupCode
import com.carmelitasoftworks.motointercom.core.service.IntercomSession
import com.carmelitasoftworks.motointercom.core.service.IntercomSessionManager
import com.carmelitasoftworks.motointercom.core.service.RiderProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val manager: IntercomSessionManager,
    private val profile: RiderProfileRepository,
) : ViewModel() {
    val session: StateFlow<IntercomSession> = manager.session
    val name: StateFlow<String> = profile.name

    fun setName(name: String) = profile.setName(name)

    /** Starts a ride in a brand new group. The caller must hold the microphone permission. */
    fun createGroup() = join(GroupCode.generate())

    /** Joins [code] (already normalized); the caller must hold the microphone permission. */
    fun join(code: String) {
        manager.join(code, profile.name.value.trim())
    }

    fun dismissError() = manager.dismissError()
}
