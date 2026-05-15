package com.moto.airecorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
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
                if ((state is RecorderState.Recording || state is RecorderState.IncomingCallRinging) && renderedSecond == lastRenderedSecond) {
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
            ACTION_AI_KEY_TOGGLE -> scope.launch { controller.toggleAiKey() }
            ACTION_DEMO_CALL_RINGING -> controller.onIncomingCallRinging()
            ACTION_DEMO_CALL_ANSWERED -> scope.launch { controller.onCallAnswered() }
            ACTION_DEMO_CALL_ENDED -> scope.launch { controller.onCallEnded() }
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
            is RecorderState.Sealing -> "正在保存音频..."
            is RecorderState.Saved -> "已保存 · 生成纪要"
            is RecorderState.PausedForCall -> "录音已暂停 · 通话不录制"
            is RecorderState.IncomingCallRinging -> "录音中 · 来电响铃"
            else -> "AI 录音进行中"
        }
        val text = when (state) {
            is RecorderState.Saved -> "点按打开 · ${formatTimer(elapsed)}"
            is RecorderState.Sealing -> "音频已持续保存"
            is RecorderState.PausedForCall -> "挂断后自动恢复"
            is RecorderState.IncomingCallRinging -> "录音继续保存"
            else -> "音频已持续保存 · 中文"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_rec)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSubText(formatTimer(elapsed))
            .setWhen(System.currentTimeMillis() - elapsed)
            .setUsesChronometer(state is RecorderState.Recording || state is RecorderState.IncomingCallRinging)
            .setOngoing(state !is RecorderState.Saved)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(activityPendingIntent())

        if (state is RecorderState.Recording || state is RecorderState.IncomingCallRinging) {
            builder
                .setProgress(0, 0, true)
                .addAction(R.drawable.ic_stat_rec, "标记", servicePendingIntent(ACTION_MARK))
                .addAction(R.drawable.ic_stat_rec, "暂停", servicePendingIntent(ACTION_PAUSE))
                .addAction(R.drawable.ic_stat_rec, "停止", servicePendingIntent(ACTION_STOP))
                .addExtras(liveUpdateExtras(title, text))
        } else if (state is RecorderState.PausedForCall) {
            builder.addExtras(liveUpdateExtras(title, text))
        }

        return builder.build()
    }

    private fun liveUpdateExtras(title: String, text: String): Bundle {
        return Bundle().apply {
            putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
            putCharSequence(EXTRA_SHORT_CRITICAL_TEXT, title)
            putBoolean(EXTRA_MOTO_LIVE_CONTENT_NOTIFICATION, true)
            putString(EXTRA_MOTO_LIVE_CONTENT_VERSION, MOTO_LIVE_CONTENT_VERSION)
            putString(EXTRA_MOTO_NOTIFICATION_CATEGORY, MOTO_LIVE_CONTENT_TYPE)
            putString(EXTRA_MOTO_PEEK_TITLE, title)
            putString(EXTRA_MOTO_PEEK_CONTENT, text)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI 录音",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "AI 录音前台采集"
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
        is RecorderState.IncomingCallRinging -> elapsedMs
        is RecorderState.PausedForCall -> elapsedMs
        is RecorderState.Sealing -> elapsedMs
        is RecorderState.Saved -> durationMs
    }

    companion object {
        const val ACTION_START = "com.moto.airecorder.action.START"
        const val ACTION_MARK = "com.moto.airecorder.action.MARK"
        const val ACTION_PAUSE = "com.moto.airecorder.action.PAUSE"
        const val ACTION_STOP = "com.moto.airecorder.action.STOP"
        const val ACTION_AI_KEY_TOGGLE = "com.moto.airecorder.ACTION_AI_KEY_TOGGLE"
        const val ACTION_DEMO_CALL_RINGING = "com.moto.airecorder.action.DEMO_CALL_RINGING"
        const val ACTION_DEMO_CALL_ANSWERED = "com.moto.airecorder.action.DEMO_CALL_ANSWERED"
        const val ACTION_DEMO_CALL_ENDED = "com.moto.airecorder.action.DEMO_CALL_ENDED"
        private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        private const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"
        private const val EXTRA_MOTO_LIVE_CONTENT_NOTIFICATION = "motorola.notification.liveContentNotification"
        private const val EXTRA_MOTO_LIVE_CONTENT_VERSION = "motorola.livecontent.version"
        private const val EXTRA_MOTO_NOTIFICATION_CATEGORY = "motorola.notification.category"
        private const val EXTRA_MOTO_PEEK_TITLE = "motorola.livecontent.peekTitle"
        private const val EXTRA_MOTO_PEEK_CONTENT = "motorola.livecontent.peekContent"
        private const val MOTO_LIVE_CONTENT_TYPE = "pay_attention_live_update"
        private const val MOTO_LIVE_CONTENT_VERSION = "2"
        private const val CHANNEL_ID = "ai_recording"
        private const val NOTIFICATION_ID = 1001
    }
}
