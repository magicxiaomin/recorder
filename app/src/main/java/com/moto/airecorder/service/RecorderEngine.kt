package com.moto.airecorder.service

data class ActiveRecording(val filePath: String)

data class SavedRecording(
    val recordingId: String,
    val filePath: String,
)

interface RecorderEngine {
    suspend fun start(): ActiveRecording
    suspend fun stop(): SavedRecording
    suspend fun pause()
    suspend fun resume()
}
