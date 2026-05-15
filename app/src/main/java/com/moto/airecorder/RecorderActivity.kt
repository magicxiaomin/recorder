package com.moto.airecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.moto.airecorder.demo.CreatedTask
import com.moto.airecorder.demo.DemoRecordingRepository
import com.moto.airecorder.service.RecorderForegroundService
import com.moto.airecorder.service.RecorderSessionController
import com.moto.airecorder.ui.CreatedTaskSheet
import com.moto.airecorder.ui.FocusMinimizedScreen
import com.moto.airecorder.ui.FocusRecordingScreen
import com.moto.airecorder.ui.RecorderScreen
import com.moto.airecorder.ui.RecordingDetailScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecorderActivity : ComponentActivity() {
    @Inject lateinit var controller: RecorderSessionController
    @Inject lateinit var demoRepository: DemoRecordingRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startRecorderService(RecorderForegroundService.ACTION_START)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasRecordAudioPermission()) {
            startRecorderService(RecorderForegroundService.ACTION_START)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MaterialTheme {
                val recorderState by controller.state.collectAsState()
                var route by remember { mutableStateOf(AppRoute.Recorder) }
                val transcript = remember { demoRepository.loadTranscript() }
                val summary = remember { demoRepository.loadSummary() }
                val tasks = remember {
                    mutableStateListOf<CreatedTask>().apply {
                        addAll(demoRepository.loadCreatedTasks())
                    }
                }
                var taskSheet by remember { mutableStateOf<CreatedTask?>(null) }

                Box(Modifier.fillMaxSize()) {
                    when (route) {
                        AppRoute.Recorder -> RecorderScreen(
                            state = recorderState,
                            transcript = transcript,
                            onMark = { startRecorderService(RecorderForegroundService.ACTION_MARK) },
                            onPause = { startRecorderService(RecorderForegroundService.ACTION_PAUSE) },
                            onStop = { startRecorderService(RecorderForegroundService.ACTION_STOP) },
                            onDemoCallRinging = { startRecorderService(RecorderForegroundService.ACTION_DEMO_CALL_RINGING) },
                            onDemoCallAnswered = { startRecorderService(RecorderForegroundService.ACTION_DEMO_CALL_ANSWERED) },
                            onDemoCallEnded = { startRecorderService(RecorderForegroundService.ACTION_DEMO_CALL_ENDED) },
                            onGenerateNotes = { route = AppRoute.Detail },
                            onStartFocus = { route = AppRoute.Focus },
                        )
                        AppRoute.Focus -> FocusRecordingScreen(
                            state = recorderState,
                            onMark = { startRecorderService(RecorderForegroundService.ACTION_MARK) },
                            onPause = { startRecorderService(RecorderForegroundService.ACTION_PAUSE) },
                            onStopAndSave = {
                                startRecorderService(RecorderForegroundService.ACTION_STOP)
                                route = AppRoute.Detail
                            },
                            onMinimize = { route = AppRoute.FocusMinimized },
                            onExitFocus = { route = AppRoute.Recorder },
                        )
                        AppRoute.FocusMinimized -> FocusMinimizedScreen(
                            state = recorderState,
                            onReturnToFocus = { route = AppRoute.Focus },
                            onExitFocus = { route = AppRoute.Recorder },
                        )
                        AppRoute.Detail -> RecordingDetailScreen(
                            state = recorderState,
                            transcript = transcript,
                            summary = summary,
                            tasks = tasks,
                            onBack = { route = AppRoute.Recorder },
                            onCreateTask = { action ->
                                val task = demoRepository.createTask(action)
                                tasks += task
                                taskSheet = task
                            },
                        )
                    }

                    taskSheet?.let { task ->
                        CreatedTaskSheet(
                            task = task,
                            onDismiss = { taskSheet = null },
                            onOpenTasks = {
                                taskSheet = null
                                route = AppRoute.Detail
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            startRecorderService(RecorderForegroundService.ACTION_AI_KEY_TOGGLE)
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startRecorderService(action: String) {
        ContextCompat.startForegroundService(
            this,
            Intent(this, RecorderForegroundService::class.java).setAction(action),
        )
    }
}

private enum class AppRoute {
    Recorder,
    Focus,
    FocusMinimized,
    Detail,
}
