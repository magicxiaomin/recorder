package com.moto.airecorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moto.airecorder.domain.RecorderState

@Composable
fun FocusRecordingScreen(
    state: RecorderState,
    onMark: () -> Unit,
    onPause: () -> Unit,
    onStopAndSave: () -> Unit,
    onMinimize: () -> Unit,
    onExitFocus: () -> Unit,
) {
    val display = state.toDisplayState()
    var confirmStop by remember { mutableStateOf(false) }

    Surface(color = Color(0xFF08080A)) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("专注录音", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("专注", color = Color(0xFFE9DFC4), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(0.9f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(Color(0xFFE5484D), RoundedCornerShape(12.dp)))
                    Spacer(Modifier.width(10.dp))
                    Text("录音中", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = display.timerText,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 58.sp,
                )
                Spacer(Modifier.height(22.dp))
                LevelMeter()
                Spacer(Modifier.height(20.dp))
                Text("音频已持续保存", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ControlButton("标记", onMark)
                    ControlButton("暂停", onPause)
                    Button(
                        onClick = { confirmStop = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                    ) {
                        Text("停止", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "↓ 最小化",
                        color = Color(0xFFE9DFC4),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onMinimize),
                    )
                    Text("+ 添加备注", color = Color(0xFF9A958A), fontWeight = FontWeight.Bold)
                    Text("设置", color = Color(0xFF9A958A), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "退出专注模式",
                    color = Color(0xFF6F6A62),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable(onClick = onExitFocus),
                )
            }

            if (confirmStop) {
                FocusStopSheet(
                    onContinue = { confirmStop = false },
                    onStopAndSave = onStopAndSave,
                    onMinimize = {
                        confirmStop = false
                        onMinimize()
                    },
                    onExitFocus = {
                        confirmStop = false
                        onExitFocus()
                    },
                )
            }
        }
    }
}

@Composable
fun FocusMinimizedScreen(
    state: RecorderState,
    onReturnToFocus: () -> Unit,
    onExitFocus: () -> Unit,
) {
    val display = state.toDisplayState()
    Surface(color = Color(0xFF101012)) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("手机仍可正常使用", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("专注模式仍在录音。", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onExitFocus,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier.border(1.dp, Color(0xFF3A383D), RoundedCornerShape(12.dp)),
                ) {
                    Text("退出专注模式", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 42.dp)
                    .background(Color(0xFF151417), RoundedCornerShape(24.dp))
                    .clickable(onClick = onReturnToFocus)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(Color(0xFFE5484D), RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(8.dp))
                Text("专注录音 · ${display.timerText}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FocusStopSheet(
    onContinue: () -> Unit,
    onStopAndSave: () -> Unit,
    onMinimize: () -> Unit,
    onExitFocus: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF151417), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("停止并保存录音？", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("这会结束专注录音，已录制的音频会安全保存。", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onContinue, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("继续录音", color = Color.White)
                }
                Button(
                    onClick = onStopAndSave,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("停止并保存", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("想继续录音？", color = Color(0xFF9A958A), fontSize = 12.sp)
                Text("最小化", color = Color(0xFFE9DFC4), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onMinimize))
                Text("·", color = Color(0xFF9A958A), fontSize = 12.sp)
                Text("退出专注模式", color = Color(0xFFE9DFC4), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onExitFocus))
            }
        }
    }
}
