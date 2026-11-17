package com.carmelitasoftworks.motointercom.feature.group

import androidx.lifecycle.ViewModel
import com.carmelitasoftworks.motointercom.core.nearby.NearbyGroup
import com.carmelitasoftworks.motointercom.core.nearby.NearbyIntercom
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
    private val nearby: NearbyIntercom,
) : ViewModel() {
    val session: StateFlow<IntercomSession> = manager.session
    val name: StateFlow<String> = profile.name
    val nearbyGroups: StateFlow<List<NearbyGroup>> = nearby.groups

    fun setName(name: String) = profile.setName(name)

    /** Starts a ride in a brand new internet group. The caller must hold the microphone permission. */
    fun createGroup() = join(GroupCode.generate())

    /** Joins internet group [code] (already normalized); the caller must hold the microphone permission. */
    fun join(code: String) = manager.join(code, profile.name.value.trim())

    /** Needs the nearby permissions. */
    fun startNearbyDiscovery() = nearby.startDiscovery()

    fun stopNearbyDiscovery() = nearby.stopDiscovery()

    /** Hosts a group without internet; needs the microphone and nearby permissions. */
    fun hostNearby() {
        nearby.stopDiscovery()
        manager.hostNearby(profile.name.value.trim())
    }

    /** Joins [group] without internet; needs the microphone and nearby permissions. */
    fun joinNearby(group: NearbyGroup) = manager.joinNearby(group, profile.name.value.trim())

    fun dismissError() = manager.dismissError()

    override fun onCleared() {
        // Discovery is only for this screen; an active ride keeps its own radio links.
        if (manager.session.value !is IntercomSession.Active) nearby.stopDiscovery()
    }
}
