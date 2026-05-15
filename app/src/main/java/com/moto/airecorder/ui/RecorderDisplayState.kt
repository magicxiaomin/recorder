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
        is RecorderState.PausedForCall -> elapsedMs
        is RecorderState.Sealing -> elapsedMs
        is RecorderState.Saved -> durationMs
    }

    return when (this) {
        is RecorderState.Sealing -> RecorderDisplayState(
            statusLabel = "Sealing audio...",
            timerText = formatTimer(elapsedMs),
            supportText = "Audio saved continuously",
            statusColor = Color(0xFFB08A3C),
            primaryAction = "Saving",
            showRecordingControls = false,
        )
        is RecorderState.Saved -> RecorderDisplayState(
            statusLabel = "Saved · Generate notes",
            timerText = formatTimer(elapsedMs),
            supportText = "Tap to open · ${formatTimer(elapsedMs)}",
            statusColor = Color(0xFF1F8A5B),
            primaryAction = "Generate notes",
            showRecordingControls = false,
        )
        else -> RecorderDisplayState(
            statusLabel = "REC",
            timerText = formatTimer(elapsedMs),
            supportText = "Audio saved continuously",
            statusColor = Color(0xFFE5484D),
            primaryAction = "Stop",
            showRecordingControls = true,
        )
    }
}

fun formatTimer(ms: Long): String {
    val totalSeconds = ms / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
