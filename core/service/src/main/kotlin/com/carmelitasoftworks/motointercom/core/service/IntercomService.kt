package com.carmelitasoftworks.motointercom.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.carmelitasoftworks.motointercom.core.audio.AudioRouter
import com.carmelitasoftworks.motointercom.core.rtc.RtcClient
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that will own the intercom session (LiveKit room + audio
 * routing) so it survives screen-off and backgrounding (see CLAUDE.md §5.2).
 *
 * Skeleton only: the notification, startForeground() and the session lifecycle
 * arrive in work order step 5.
 */
@AndroidEntryPoint
class IntercomService : Service() {
    @Inject lateinit var audioRouter: AudioRouter

    @Inject lateinit var rtcClient: RtcClient

    override fun onBind(intent: Intent?): IBinder? = null
}
