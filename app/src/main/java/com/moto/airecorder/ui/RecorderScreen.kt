package com.moto.airecorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moto.airecorder.demo.TranscriptLine
import com.moto.airecorder.domain.RecorderState

@Composable
fun RecorderScreen(
    state: RecorderState,
    transcript: List<TranscriptLine>,
    onMark: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onDemoCallRinging: () -> Unit,
    onDemoCallAnswered: () -> Unit,
    onDemoCallEnded: () -> Unit,
    onGenerateNotes: () -> Unit,
    onStartFocus: () -> Unit,
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
                Text("AI 录音", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("中文 · 自动", color = Color(0xFF9A958A), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(display.statusColor, RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(8.dp))
                Text(
                    display.statusLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
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
            HorizontalDivider(color = Color(0xFF242326))
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth()) {
                Text("实时转写", color = Color(0xFF9A958A), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                transcript.take(3).forEach { line ->
                    TranscriptLineRow(line = line, highlighted = false, compact = true)
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            if (display.showRecordingControls) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ControlButton("标记", onMark)
                    ControlButton("暂停", onPause)
                    Button(
                        onClick = onStop,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                    ) { Text("停止", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            } else {
                Button(
                    onClick = onGenerateNotes,
                    enabled = state is RecorderState.Saved,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = display.statusColor),
                ) {
                    Text(display.primaryAction, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (display.showRecordingControls) {
                FocusEntryRow(onStartFocus = onStartFocus)
                Spacer(Modifier.height(10.dp))
                DemoCallControls(
                    state = state,
                    onRinging = onDemoCallRinging,
                    onAnswered = onDemoCallAnswered,
                    onEnded = onDemoCallEnded,
                )
            } else {
                Text("使用专注模式", color = Color(0xFF6F6A62), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DemoCallControls(
    state: RecorderState,
    onRinging: () -> Unit,
    onAnswered: () -> Unit,
    onEnded: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF101012), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("来电演示", color = Color(0xFF9A958A), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onRinging,
                enabled = state is RecorderState.Recording,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            ) {
                Text("响铃", color = Color.White, fontSize = 12.sp)
            }
            Button(
                onClick = onAnswered,
                enabled = state is RecorderState.IncomingCallRinging,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB08A3C)),
            ) {
                Text("接听", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onEnded,
                enabled = state is RecorderState.IncomingCallRinging || state is RecorderState.PausedForCall,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F8A5B)),
            ) {
                Text("挂断", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FocusEntryRow(onStartFocus: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151417), RoundedCornerShape(8.dp))
            .clickable(onClick = onStartFocus)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("重要会议？", color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            "切换到专注模式，状态更醒目，干扰更少。",
            color = Color(0xFFE9DFC4),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text("进入专注模式", color = Color(0xFFE5484D), fontWeight = FontWeight.Bold)
    }
}
