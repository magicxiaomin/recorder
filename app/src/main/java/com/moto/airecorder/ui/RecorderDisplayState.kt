package com.moto.airecorder.ui

import androidx.compose.ui.graphics.Color
import com.moto.airecorder.domain.RecorderState

data class RecorderDisplayState(
    val statusLabel: String,
    val timerText: String,
    val supportText: String,
    val statusColor: Color,
    val primaryAction: String,
    val showRecordingControls: Boolean,
)

fun RecorderState.toDisplayState(): RecorderDisplayState {
    val elapsedMs = when (this) {
        RecorderState.Idle -> 0
        is RecorderState.Recording -> elapsedMs
        is RecorderState.IncomingCallRinging -> elapsedMs
        is RecorderState.PausedForCall -> elapsedMs
        is RecorderState.Sealing -> elapsedMs
        is RecorderState.Saved -> durationMs
    }

    return when (this) {
        is RecorderState.Sealing -> RecorderDisplayState(
            statusLabel = "正在保存音频...",
            timerText = formatTimer(elapsedMs),
            supportText = "音频已持续保存",
            statusColor = Color(0xFFB08A3C),
            primaryAction = "保存中",
            showRecordingControls = false,
        )
        is RecorderState.Saved -> RecorderDisplayState(
            statusLabel = "已保存 · 生成纪要",
            timerText = formatTimer(elapsedMs),
            supportText = "点按打开 · ${formatTimer(elapsedMs)}",
            statusColor = Color(0xFF1F8A5B),
            primaryAction = "生成纪要",
            showRecordingControls = false,
        )
        is RecorderState.IncomingCallRinging -> RecorderDisplayState(
            statusLabel = "录音中",
            timerText = formatTimer(elapsedMs),
            supportText = "来电响铃中 / 录音继续保存",
            statusColor = Color(0xFFE5484D),
            primaryAction = "停止",
            showRecordingControls = true,
        )
        is RecorderState.PausedForCall -> RecorderDisplayState(
            statusLabel = "录音已暂停",
            timerText = formatTimer(elapsedMs),
            supportText = "通话不录制 / 挂断后自动恢复",
            statusColor = Color(0xFFB08A3C),
            primaryAction = "停止",
            showRecordingControls = true,
        )
        else -> RecorderDisplayState(
            statusLabel = "录音中",
            timerText = formatTimer(elapsedMs),
            supportText = "音频已持续保存",
            statusColor = Color(0xFFE5484D),
            primaryAction = "停止",
            showRecordingControls = true,
        )
    }
}

fun formatTimer(ms: Long): String {
    val totalSeconds = ms / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
