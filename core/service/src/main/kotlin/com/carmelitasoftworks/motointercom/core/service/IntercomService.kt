package com.carmelitasoftworks.motointercom.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.carmelitasoftworks.motointercom.core.rtc.RtcConnectionState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the ride alive with the screen off or the app in the background
 * (CLAUDE.md §5.2). The session itself lives in [IntercomSessionManager]; this
 * service holds the process in the foreground, shows the persistent
 * notification and speaks connection notices.
 */
@AndroidEntryPoint
class IntercomService : Service() {
    @Inject lateinit var manager: IntercomSessionManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var announcer: VoiceAnnouncer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        announcer = VoiceAnnouncer(this)
        // Keeps the CPU awake for audio with the screen off; released in onDestroy.
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "motointercom:ride")
            .apply { acquire() }
        scope.launch { observeSession() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground(buildNotification(manager.session.value))
        when (intent?.action) {
            ACTION_LEAVE -> manager.leave()
            ACTION_TOGGLE_MUTE -> (manager.session.value as? IntercomSession.Active)
                ?.let { manager.setSelfMuted(!it.selfMuted) }
        }
        if (manager.session.value !is IntercomSession.Active) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        announcer?.release()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    private suspend fun observeSession() {
        var previous: RtcConnectionState? = null
        manager.session.collect { session ->
            if (session !is IntercomSession.Active) {
                stopSelf()
                return@collect
            }
            announceChange(previous, session.connection)
            previous = session.connection
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(session))
        }
    }

    private fun announceChange(previous: RtcConnectionState?, current: RtcConnectionState) {
        when {
            current == RtcConnectionState.RECONNECTING && previous == RtcConnectionState.CONNECTED ->
                announcer?.say(getString(R.string.announce_lost))
            current == RtcConnectionState.CONNECTED && previous == RtcConnectionState.RECONNECTING ->
                announcer?.say(getString(R.string.announce_back))
        }
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(session: IntercomSession): Notification {
        val active = session as? IntercomSession.Active
        val text = when {
            active == null -> ""
            active.connection == RtcConnectionState.RECONNECTING -> getString(R.string.notification_reconnecting)
            active.connection != RtcConnectionState.CONNECTED -> getString(R.string.notification_connecting)
            active.selfMuted -> getString(R.string.notification_muted)
            else -> resources.getQuantityString(R.plurals.notification_riders, active.riders.size, active.riders.size)
        }
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val muteLabel = if (active?.selfMuted == true) R.string.action_unmute else R.string.action_mute
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title, active?.code.orEmpty()))
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(action(getString(muteLabel), ACTION_TOGGLE_MUTE))
            .addAction(action(getString(R.string.action_leave), ACTION_LEAVE))
            .build()
    }

    private fun action(label: String, action: String): Notification.Action =
        Notification.Action.Builder(null, label, selfIntent(action)).build()

    private fun selfIntent(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, IntercomService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "intercom"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_LEAVE = "leave"
        private const val ACTION_TOGGLE_MUTE = "toggle_mute"

        /** Call right after [IntercomSessionManager.join], from the foreground (a visible screen). */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, IntercomService::class.java))
        }
    }
}
