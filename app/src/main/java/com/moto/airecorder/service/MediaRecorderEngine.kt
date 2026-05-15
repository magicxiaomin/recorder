package com.moto.airecorder.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRecorderEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecorderEngine {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    override suspend fun start(): ActiveRecording {
        val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
        val output = File(recordingsDir, "ai-recording-${System.currentTimeMillis()}.m4a")
        currentFile = output

        recorder = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(SAMPLE_RATE_HZ)
            setAudioEncodingBitRate(BIT_RATE_BPS)
            setOutputFile(output.absolutePath)
            prepare()
            start()
        }

        return ActiveRecording(filePath = output.absolutePath)
    }

    override suspend fun stop(): SavedRecording {
        val output = currentFile
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
        currentFile = null

        return SavedRecording(
            recordingId = UUID.randomUUID().toString(),
            filePath = output?.absolutePath.orEmpty(),
        )
    }

    override suspend fun pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.pause()
        }
    }

    override suspend fun resume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.resume()
        }
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 44_100
        const val BIT_RATE_BPS = 128_000
    }
}
