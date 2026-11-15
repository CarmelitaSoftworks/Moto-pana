package com.carmelitasoftworks.motointercom.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service that will own the intercom session (LiveKit room + audio
 * routing) so it survives screen-off and backgrounding (see CLAUDE.md §5.2).
 *
 * Skeleton only, and not declared in the manifest yet: the notification,
 * startForeground() and the session lifecycle arrive in work order step 5.
 */
class IntercomService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
