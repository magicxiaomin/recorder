package com.moto.airecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.moto.airecorder.domain.RecorderState
import com.moto.airecorder.service.RecorderForegroundService
import com.moto.airecorder.service.RecorderSessionController
import com.moto.airecorder.ui.toDisplayState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecorderActivity : ComponentActivity() {
    @Inject lateinit var controller: RecorderSessionController

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
                val state by controller.state.collectAsState()
                RecorderScreen(
                    state = state,
                    onMark = { startRecorderService(RecorderForegroundService.ACTION_MARK) },
                    onPause = { startRecorderService(RecorderForegroundService.ACTION_PAUSE) },
                    onStop = { startRecorderService(RecorderForegroundService.ACTION_STOP) },
                )
            }
        }
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

@Composable
private fun RecorderScreen(
    state: RecorderState,
    onMark: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    val display = state.toDisplayState()

    Surface(color = Color(0xFF08080A)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("AI RECORDING", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("EN · auto", color = Color(0xFF9A958A), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(display.statusColor, RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(8.dp))
                Text(
                    display.statusLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = if (display.statusLabel == "REC") 5.sp else 0.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = display.timerText,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 44.sp,
            )
            Spacer(Modifier.height(16.dp))
            LevelMeter()
            Spacer(Modifier.height(16.dp))
            Text(display.supportText, color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF242326)),
            )
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth()) {
                Text("LIVE TRANSCRIPT", color = Color(0xFF9A958A), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Text("Speaker 1 · 00:03", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold)
                Text("“Let's get started...”", color = Color(0xFF9A958A), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            if (display.showRecordingControls) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ControlButton("⚑ Mark", onMark)
                    ControlButton("Ⅱ Pause", onPause)
                    Button(
                        onClick = onStop,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                    ) { Text("■ Stop", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            } else {
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = display.statusColor),
                ) {
                    Text(display.primaryAction, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("⊕ Use Focus Mode", color = Color(0xFF6F6A62), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ControlButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        modifier = Modifier.border(1.dp, Color(0xFF3A383D), RoundedCornerShape(12.dp)),
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LevelMeter() {
    val bars = listOf(7, 12, 16, 10, 22, 28, 18, 34, 14, 26, 38, 24, 16, 32, 42, 30, 20, 28, 36, 22, 16, 30, 20, 26, 12, 22, 16, 14, 20, 10, 14, 8)
    Canvas(modifier = Modifier.size(width = 190.dp, height = 44.dp)) {
        val gap = size.width / bars.size
        bars.forEachIndexed { index, barHeight ->
            val color = if (index in 14..17) Color(0xFFE5484D) else Color(0xFF8A857A)
            val x = index * gap + gap / 2
            val half = barHeight / 2f
            drawLine(
                color = color,
                start = Offset(x, size.height / 2f - half),
                end = Offset(x, size.height / 2f + half),
                strokeWidth = 3f,
            )
        }
    }
}
