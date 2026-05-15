package com.moto.airecorder.domain

sealed interface RecorderState {
    data object Idle : RecorderState
    data class Recording(
        val elapsedMs: Long,
        val markers: List<Long>,
    ) : RecorderState
    data class PausedForCall(val elapsedMs: Long) : RecorderState
    data class Sealing(val elapsedMs: Long) : RecorderState
    data class Saved(
        val recordingId: String,
        val durationMs: Long,
    ) : RecorderState
}
