package com.moto.airecorder.service

import com.moto.airecorder.domain.RecorderState
import android.os.SystemClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecorderSessionController @Inject constructor(
    private val engine: RecorderEngine,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val timeProvider: TimeProvider = TimeProvider { SystemClock.elapsedRealtime() },
) {
    private val mutableState = MutableStateFlow<RecorderState>(RecorderState.Idle)
    val state: StateFlow<RecorderState> = mutableState.asStateFlow()

    private var timerJob: Job? = null
    private var elapsedMs: Long = 0
    private var startedAtMs: Long = 0
    private var markers: List<Long> = emptyList()

    suspend fun start() = withContext(dispatcher) {
        if (mutableState.value !is RecorderState.Idle) return@withContext
        startRecordingSession()
    }

    suspend fun toggleAiKey() = withContext(dispatcher) {
        when (mutableState.value) {
            RecorderState.Idle,
            is RecorderState.Saved,
            -> startRecordingSession()
            is RecorderState.Recording,
            is RecorderState.IncomingCallRinging,
            is RecorderState.PausedForCall,
            -> stopAndSealSession()
            is RecorderState.Sealing -> Unit
        }
    }

    private suspend fun startRecordingSession() {
        engine.start()
        startedAtMs = timeProvider.nowMs()
        elapsedMs = 0
        markers = emptyList()
        mutableState.value = RecorderState.Recording(elapsedMs = elapsedMs, markers = markers)
        startTimer()
    }

    fun mark() {
        val current = mutableState.value
        when (current) {
            is RecorderState.Recording -> {
                markers = current.markers + current.elapsedMs
                mutableState.value = current.copy(markers = markers)
            }
            is RecorderState.IncomingCallRinging -> {
                markers = current.markers + current.elapsedMs
                mutableState.value = current.copy(markers = markers)
            }
            else -> Unit
        }
    }

    suspend fun stopAndSeal() = withContext(dispatcher) {
        if (
            mutableState.value !is RecorderState.Recording &&
            mutableState.value !is RecorderState.IncomingCallRinging &&
            mutableState.value !is RecorderState.PausedForCall
        ) {
            return@withContext
        }
        stopAndSealSession()
    }

    private suspend fun stopAndSealSession() {
        val duration = elapsedMs
        timerJob?.cancel()
        mutableState.value = RecorderState.Sealing(duration)
        val saved = engine.stop()
        mutableState.value = RecorderState.Saved(saved.recordingId, duration)
    }

    suspend fun pauseForUser() = withContext(dispatcher) {
        engine.pause()
    }

    suspend fun resumeFromUserPause() = withContext(dispatcher) {
        engine.resume()
    }

    fun onIncomingCallRinging() {
        val current = mutableState.value
        if (current is RecorderState.Recording) {
            mutableState.value = RecorderState.IncomingCallRinging(
                elapsedMs = current.elapsedMs,
                markers = current.markers,
            )
        }
    }

    suspend fun onCallAnswered() = withContext(dispatcher) {
        val current = mutableState.value
        if (current is RecorderState.IncomingCallRinging) {
            engine.pause()
            elapsedMs = current.elapsedMs
            mutableState.value = RecorderState.PausedForCall(elapsedMs = current.elapsedMs)
        }
    }

    suspend fun onCallEnded() = withContext(dispatcher) {
        when (mutableState.value) {
            is RecorderState.PausedForCall -> {
                engine.resume()
                startedAtMs = timeProvider.nowMs() - elapsedMs
                mutableState.value = RecorderState.Recording(elapsedMs = elapsedMs, markers = markers)
            }
            is RecorderState.IncomingCallRinging -> {
                mutableState.value = RecorderState.Recording(elapsedMs = elapsedMs, markers = markers)
            }
            else -> Unit
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch(dispatcher) {
            while (true) {
                delay(TICK_MS)
                val runningElapsedMs = timeProvider.nowMs() - startedAtMs
                mutableState.update { current ->
                    when (current) {
                        is RecorderState.Recording -> {
                            elapsedMs = runningElapsedMs
                            current.copy(elapsedMs = elapsedMs, markers = markers)
                        }
                        is RecorderState.IncomingCallRinging -> {
                            elapsedMs = runningElapsedMs
                            current.copy(elapsedMs = elapsedMs, markers = markers)
                        }
                        else -> current
                    }
                }
            }
        }
    }

    private companion object {
        const val TICK_MS = 100L
    }
}

fun interface TimeProvider {
    fun nowMs(): Long
}
