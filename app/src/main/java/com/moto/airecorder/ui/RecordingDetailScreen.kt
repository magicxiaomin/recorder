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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.moto.airecorder.demo.ActionItem
import com.moto.airecorder.demo.CreatedTask
import com.moto.airecorder.demo.DemoSummary
import com.moto.airecorder.demo.TranscriptLine
import com.moto.airecorder.domain.RecorderState
import kotlinx.coroutines.delay

@Composable
fun RecordingDetailScreen(
    state: RecorderState,
    transcript: List<TranscriptLine>,
    summary: DemoSummary,
    tasks: List<CreatedTask>,
    onBack: () -> Unit,
    onCreateTask: (ActionItem) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(DetailTab.Transcript) }
    var generating by remember { mutableStateOf(false) }
    var progressStep by remember { mutableIntStateOf(0) }
    var summaryReady by remember { mutableStateOf(false) }
    var anchorMs by remember { mutableStateOf<Long?>(null) }
    var playbackMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(generating) {
        if (!generating) return@LaunchedEffect
        progressStep = 0
        SUMMARY_STEPS.indices.forEach { index ->
            delay(650)
            progressStep = index + 1
        }
        summaryReady = true
        generating = false
        selectedTab = DetailTab.Summary
    }

    Surface(color = Color(0xFF08080A)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text("返回", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold)
                }
                Text("录音详情", color = Color(0xFF9A958A), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(summary.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "${summary.durationLabel} · ${state.detailStatusLabel()}",
                color = Color(0xFFE9DFC4),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailTabButton("转写", selectedTab == DetailTab.Transcript) { selectedTab = DetailTab.Transcript }
                DetailTabButton("纪要", selectedTab == DetailTab.Summary) { selectedTab = DetailTab.Summary }
                DetailTabButton("任务 ${tasks.size}", selectedTab == DetailTab.Tasks) { selectedTab = DetailTab.Tasks }
            }
            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                DetailTab.Transcript -> TranscriptList(
                    transcript = transcript,
                    anchorMs = anchorMs,
                    playbackMs = playbackMs,
                )
                DetailTab.Summary -> SummaryPane(
                    summary = summary,
                    generating = generating,
                    progressStep = progressStep,
                    summaryReady = summaryReady,
                    onGenerate = {
                        summaryReady = false
                        generating = true
                    },
                    onJump = { timestampMs ->
                        anchorMs = timestampMs
                        playbackMs = timestampMs
                        selectedTab = DetailTab.Transcript
                    },
                    onCreateTask = onCreateTask,
                )
                DetailTab.Tasks -> TasksPane(tasks = tasks)
            }
        }
    }
}

@Composable
fun CreatedTaskSheet(task: CreatedTask, onDismiss: () -> Unit, onOpenTasks: () -> Unit) {
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
            Text("任务已创建", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF08080A), RoundedCornerShape(8.dp))
                    .padding(14.dp),
            ) {
                Text(task.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text("来源 · ${task.source}", color = Color(0xFFE5484D), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("完成", color = Color.White)
                }
                Button(
                    onClick = onOpenTasks,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F8A5B)),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("打开任务", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private enum class DetailTab {
    Transcript,
    Summary,
    Tasks,
}

@Composable
private fun DetailTabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) Color(0xFFE9DFC4) else Color.Transparent
    val content = if (selected) Color(0xFF08080A) else Color.White
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = container),
    ) {
        Text(label, color = content, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TranscriptList(transcript: List<TranscriptLine>, anchorMs: Long?, playbackMs: Long?) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (anchorMs != null) {
            item {
                Text("从纪要定位到转写", color = Color(0xFFE5484D), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF151417), RoundedCornerShape(8.dp))
                        .padding(14.dp),
                ) {
                    Text("音频已跳转到 ${formatTimer(playbackMs ?: anchorMs)}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("正在播放来源片段", color = Color(0xFFE5484D), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
        items(transcript) { line ->
            TranscriptLineRow(line = line, highlighted = line.timestampMs == anchorMs, compact = false)
        }
    }
}

@Composable
private fun SummaryPane(
    summary: DemoSummary,
    generating: Boolean,
    progressStep: Int,
    summaryReady: Boolean,
    onGenerate: () -> Unit,
    onJump: (Long) -> Unit,
    onCreateTask: (ActionItem) -> Unit,
) {
    when {
        generating -> SummaryProgress(progressStep = progressStep)
        summaryReady -> SummaryResult(summary = summary, onJump = onJump, onCreateTask = onCreateTask)
        else -> SummaryConfig(summary = summary, onGenerate = onGenerate)
    }
}

@Composable
private fun SummaryConfig(summary: DemoSummary, onGenerate: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("生成纪要", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("${summary.title} · ${summary.durationLabel}", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold)
        SettingRow("输出", "纪要 + 待办")
        SettingRow("语言", "中文")
        SettingRow("可追溯性", "时间戳锚点")
        Button(
            onClick = onGenerate,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("生成纪要", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryProgress(progressStep: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("正在生成纪要", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        SUMMARY_STEPS.forEachIndexed { index, step ->
            val done = index < progressStep
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(if (done) Color(0xFF1F8A5B) else Color(0xFF3A383D), RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Text(step, color = if (done) Color.White else Color(0xFF9A958A), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryResult(
    summary: DemoSummary,
    onJump: (Long) -> Unit,
    onCreateTask: (ActionItem) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(summary.overview, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Text("关键决策", color = Color(0xFF9A958A), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        items(summary.keyPoints) { point ->
            TimestampCard(timestampMs = point.timestampMs, text = point.text, onJump = onJump)
        }
        item {
            Text("待办事项", color = Color(0xFF9A958A), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        items(summary.actions) { action ->
            ActionItemCard(action = action, onJump = onJump, onCreateTask = onCreateTask)
        }
    }
}

@Composable
private fun TimestampCard(timestampMs: Long, text: String, onJump: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151417), RoundedCornerShape(8.dp))
            .clickable { onJump(timestampMs) }
            .padding(14.dp),
    ) {
        Text(formatTimer(timestampMs), color = Color(0xFFE5484D), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionItemCard(
    action: ActionItem,
    onJump: (Long) -> Unit,
    onCreateTask: (ActionItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151417), RoundedCornerShape(8.dp))
            .padding(14.dp),
    ) {
        Text(action.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("${action.owner} · ${action.dueLabel}", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onJump(action.timestampMs) }, shape = RoundedCornerShape(8.dp)) {
                Text("跳到 ${formatTimer(action.timestampMs)}", color = Color.White)
            }
            Button(
                onClick = { onCreateTask(action) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F8A5B)),
            ) {
                Text("创建任务", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TasksPane(tasks: List<CreatedTask>) {
    if (tasks.isEmpty()) {
        Text("暂无任务", color = Color(0xFF9A958A), fontWeight = FontWeight.Bold)
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(tasks) { task ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151417), RoundedCornerShape(8.dp))
                    .padding(14.dp),
            ) {
                Text(task.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text("来源 · ${task.source}", color = Color(0xFFE5484D), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151417), RoundedCornerShape(8.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color(0xFF9A958A), fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun RecorderState.detailStatusLabel(): String {
    return when (this) {
        RecorderState.Idle -> "未开始"
        is RecorderState.Recording -> "录音中"
        is RecorderState.IncomingCallRinging -> "来电响铃"
        is RecorderState.PausedForCall -> "隐私暂停"
        is RecorderState.Sealing -> "正在保存音频"
        is RecorderState.Saved -> "音频已保存"
    }
}

private val SUMMARY_STEPS = listOf(
    "增强音频",
    "转写文本",
    "区分发言人",
    "校正姓名和术语",
    "撰写纪要",
    "提取待办",
)
