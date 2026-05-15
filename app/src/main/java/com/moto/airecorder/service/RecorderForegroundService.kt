package com.moto.airecorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.moto.airecorder.R
import com.moto.airecorder.RecorderActivity
import com.moto.airecorder.domain.RecorderState
import com.moto.airecorder.ui.formatTimer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecorderForegroundService : Service() {
    @Inject lateinit var controller: RecorderSessionController
    @Inject lateinit var scope: CoroutineScope

    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(controller.state.value))
        notificationJob = scope.launch {
            var lastRenderedSecond = -1L
            controller.state.collect { state ->
                val renderedSecond = state.elapsedMs() / 1_000
                if (state is RecorderState.Recording && renderedSecond == lastRenderedSecond) {
                    return@collect
                }
                lastRenderedSecond = renderedSecond
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(state))
                if (state is RecorderState.Saved) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> scope.launch { controller.start() }
            ACTION_MARK -> controller.mark()
            ACTION_PAUSE -> scope.launch { controller.pauseForUser() }
            ACTION_STOP -> scope.launch { controller.stopAndSeal() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        notificationJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(state: RecorderState): Notification {
        val elapsed = state.elapsedMs()
        val title = when (state) {
            is RecorderState.Sealing -> "Sealing audio\u2026"
            is RecorderState.Saved -> "Saved \u00B7 Generate notes"
            else -> "AI Recording active"
        }
        val text = when (state) {
            is RecorderState.Saved -> "Tap to open \u00B7 ${formatTimer(elapsed)}"
            is RecorderState.Sealing -> "Audio saved continuously"
            else -> "Audio saved continuously \u00B7 EN"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_rec)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(formatTimer(elapsed))
            .setWhen(System.currentTimeMillis() - elapsed)
            .setUsesChronometer(state is RecorderState.Recording)
            .setOngoing(state !is RecorderState.Saved)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(activityPendingIntent())

        if (state is RecorderState.Recording) {
            builder
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(recordingSmallView(elapsed))
                .setCustomBigContentView(recordingBigView(elapsed))
        }

        return builder.build()
    }

    private fun recordingSmallView(elapsedMs: Long): RemoteViews {
        return RemoteViews(packageName, R.layout.notification_recording_small).apply {
            setTextViewText(R.id.notification_title, "AI Recording active")
            setTextViewText(R.id.notification_timer, formatTimer(elapsedMs))
            setOnClickPendingIntent(R.id.notification_root, activityPendingIntent())
        }
    }

    private fun recordingBigView(elapsedMs: Long): RemoteViews {
        return RemoteViews(packageName, R.layout.notification_recording_big).apply {
            setTextViewText(R.id.notification_title, "AI Recording active")
            setTextViewText(R.id.notification_timer, formatTimer(elapsedMs))
            setTextViewText(R.id.notification_body, "Audio saved continuously \u00B7 EN")
            setOnClickPendingIntent(R.id.notification_root, activityPendingIntent())
            setOnClickPendingIntent(R.id.notification_mark, servicePendingIntent(ACTION_MARK))
            setOnClickPendingIntent(R.id.notification_pause, servicePendingIntent(ACTION_PAUSE))
            setOnClickPendingIntent(R.id.notification_stop, servicePendingIntent(ACTION_STOP))
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI recording",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "AI Recorder foreground capture"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun activityPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, RecorderActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        return PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, RecorderForegroundService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun RecorderState.elapsedMs(): Long = when (this) {
        RecorderState.Idle -> 0
        is RecorderState.Recording -> elapsedMs
        is RecorderState.PausedForCall -> elapsedMs
        is RecorderState.Sealing -> elapsedMs
        is RecorderState.Saved -> durationMs
    }

    companion object {
        const val ACTION_START = "com.moto.airecorder.action.START"
        const val ACTION_MARK = "com.moto.airecorder.action.MARK"
        const val ACTION_PAUSE = "com.moto.airecorder.action.PAUSE"
        const val ACTION_STOP = "com.moto.airecorder.action.STOP"
        private const val CHANNEL_ID = "ai_recording"
        private const val NOTIFICATION_ID = 1001
    }
}
