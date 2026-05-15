package com.moto.airecorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.moto.airecorder.demo.ActionItem
import com.moto.airecorder.demo.DemoSummary
import com.moto.airecorder.demo.TranscriptLine
import com.moto.airecorder.domain.RecorderState

@Composable
fun FoldableRecorderUxScreen(
    state: RecorderState,
    transcript: List<TranscriptLine>,
    summary: DemoSummary,
    onAiKeyToggle: () -> Unit,
    onStop: () -> Unit,
    onCreateTask: (ActionItem) -> Unit,
) {
    var selectedRecording by remember { mutableStateOf(0) }
    var selectedTab by remember { mutableStateOf(FoldableTab.Summary) }
    var selectedTemplate by remember { mutableStateOf("访谈") }
    var reminderBanner by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= 720.dp
        Surface(color = Color(0xFFF6F6F7)) {
            if (isTwoPane) {
                Row(Modifier.fillMaxSize()) {
                    RecordingLibraryPane(
                        selectedRecording = selectedRecording,
                        onSelected = { selectedRecording = it },
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight(),
                    )
                    FoldDivider()
                    RecordingWorkspacePane(
                        state = state,
                        transcript = transcript,
                        summary = summary,
                        selectedTab = selectedTab,
                        selectedTemplate = selectedTemplate,
                        reminderBanner = reminderBanner,
                        onTabChange = { selectedTab = it },
                        onTemplateChange = { selectedTemplate = it },
                        onDismissReminder = { reminderBanner = false },
                        onAiKeyToggle = onAiKeyToggle,
                        onStop = onStop,
                        onCreateTask = { action ->
                            reminderBanner = true
                            onCreateTask(action)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                RecordingWorkspacePane(
                    state = state,
                    transcript = transcript,
                    summary = summary,
                    selectedTab = selectedTab,
                    selectedTemplate = selectedTemplate,
                    reminderBanner = reminderBanner,
                    onTabChange = { selectedTab = it },
                    onTemplateChange = { selectedTemplate = it },
                    onDismissReminder = { reminderBanner = false },
                    onAiKeyToggle = onAiKeyToggle,
                    onStop = onStop,
                    onCreateTask = { action ->
                        reminderBanner = true
                        onCreateTask(action)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun RecordingLibraryPane(
    selectedRecording: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recordings = listOf(
        FoldableRecording("In Conversation with Daniel Leyser", "00:19 · Dec 01", "Transcribe"),
        FoldableRecording("In Conversation with Daniel Leyser", "00:26 · Jan 18", "Transcribing..."),
        FoldableRecording("In Conversation with Daniel Leyser", "00:29 · Dec 01", "Transcribed"),
        FoldableRecording("In Conversation with Daniel Leyser", "00:19 · Dec 01", "Transcribing..."),
        FoldableRecording("In Conversation with Daniel Leyser", "00:36 · Jan 10", "Transcribed"),
    )

    Column(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("☰", color = Color(0xFF20242A), fontSize = 20.sp)
            Text("⌕", color = Color(0xFF20242A), fontSize = 18.sp)
        }
        Spacer(Modifier.height(28.dp))
        Text("All\nRecordings", color = Color(0xFF20242A), fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
        Spacer(Modifier.height(24.dp))
        Text("2024 · January", color = Color(0xFF7D858F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recordings.indices.toList()) { index ->
                RecordingListItem(
                    recording = recordings[index],
                    selected = selectedRecording == index,
                    onClick = { onSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun RecordingListItem(recording: FoldableRecording, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) Color(0xFFEFF3F8) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(9.dp).background(if (selected) Color(0xFF1E5BCE) else Color(0xFF20242A), CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(recording.title, color = Color(0xFF20242A), fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
            Text(recording.meta, color = Color(0xFF7D858F), fontSize = 10.sp)
            StatusPill(recording.status)
        }
        Text("⋮", color = Color(0xFF7D858F), fontSize = 16.sp)
    }
}

@Composable
private fun RecordingWorkspacePane(
    state: RecorderState,
    transcript: List<TranscriptLine>,
    summary: DemoSummary,
    selectedTab: FoldableTab,
    selectedTemplate: String,
    reminderBanner: Boolean,
    onTabChange: (FoldableTab) -> Unit,
    onTemplateChange: (String) -> Unit,
    onDismissReminder: () -> Unit,
    onAiKeyToggle: () -> Unit,
    onStop: () -> Unit,
    onCreateTask: (ActionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFFFAFAFB))
            .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        if (reminderBanner) {
            ReminderBanner(onDismiss = onDismissReminder)
            Spacer(Modifier.height(10.dp))
        }
        WorkspaceTopBar()
        Spacer(Modifier.height(16.dp))
        Text("In Conversation with Daniel Leyser", color = Color(0xFF20242A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("MetaDesign is Red Dot", color = Color(0xFF20242A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("00:19 · Dec 01", color = Color(0xFF8A929D), fontSize = 10.sp)
        Spacer(Modifier.height(12.dp))
        RecordingSessionStrip()
        Spacer(Modifier.height(12.dp))
        TabStrip(selectedTab = selectedTab, onTabChange = onTabChange)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(16.dp),
        ) {
            when (selectedTab) {
                FoldableTab.Transcription -> FoldableTranscript(transcript = transcript)
                FoldableTab.Summary -> FoldableSummary(
                    summary = summary,
                    selectedTemplate = selectedTemplate,
                    onTemplateChange = onTemplateChange,
                    onCreateTask = onCreateTask,
                )
                FoldableTab.Chapter -> FoldableChapter()
                FoldableTab.Speakers -> FoldableSpeakers(transcript = transcript)
            }
        }
        Spacer(Modifier.height(12.dp))
        PlaybackControls(
            state = state,
            onAiKeyToggle = onAiKeyToggle,
            onStop = onStop,
        )
    }
}

@Composable
private fun WorkspaceTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("←", color = Color(0xFF20242A), fontSize = 20.sp)
            StatusPill("自动转写中")
            StatusPill("AI 模型在线")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("✎", color = Color(0xFF20242A), fontSize = 16.sp)
            Text("✦", color = Color(0xFF20242A), fontSize = 16.sp)
            Text("⋮", color = Color(0xFF20242A), fontSize = 18.sp)
        }
    }
}

@Composable
private fun TabStrip(selectedTab: FoldableTab, onTabChange: (FoldableTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F3F6), RoundedCornerShape(18.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FoldableTab.entries.forEach { tab ->
            val selected = selectedTab == tab
            Text(
                text = tab.label,
                color = if (selected) Color(0xFF20242A) else Color(0xFF6B737D),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) Color(0xFFA8D9EF) else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable { onTabChange(tab) }
                    .padding(vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun FoldableTranscript(transcript: List<TranscriptLine>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("实时转写", color = Color(0xFF20242A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        items(transcript) { line ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${line.speaker} · ${formatTimer(line.timestampMs)}", color = Color(0xFF1E5BCE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(line.text, color = Color(0xFF20242A), fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun FoldableSummary(
    summary: DemoSummary,
    selectedTemplate: String,
    onTemplateChange: (String) -> Unit,
    onCreateTask: (ActionItem) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("内容总结", color = Color(0xFF20242A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(summary.overview, color = Color(0xFF2B3036), fontSize = 14.sp, lineHeight = 20.sp)
        }
        item {
            MvpAiQualityCard()
        }
        item {
            TemplateChooser(selectedTemplate = selectedTemplate, onTemplateChange = onTemplateChange)
        }
        item {
            Text("会议内容", color = Color(0xFF20242A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            summary.keyPoints.forEachIndexed { index, point ->
                Text("${index + 1}. ${point.text}", color = Color(0xFF2B3036), fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        item {
            Text("待办事项", color = Color(0xFF20242A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        items(summary.actions) { action ->
            ActionTaskRow(action = action, onCreateTask = onCreateTask)
        }
        item {
            SystemLinkageCard()
        }
    }
}

@Composable
private fun TemplateChooser(selectedTemplate: String, onTemplateChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F7FB), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("推荐", "自定义", "会议", "教育", "旅行").forEach { item ->
                StatusPill(item, selected = selectedTemplate == item, onClick = { onTemplateChange(item) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("会议纪要", "会议记录", "课堂笔记", "面试摘要").forEach { card ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selectedTemplate == card) Color.White else Color(0xFFEAF0F7), RoundedCornerShape(8.dp))
                        .clickable { onTemplateChange(card) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(card, color = Color(0xFF20242A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("NEW", color = Color(0xFF1E5BCE), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Button(
            onClick = { onTemplateChange("会议纪要") },
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3478F6)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Use the Template", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionTaskRow(action: ActionItem, onCreateTask: (ActionItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFB), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(action.title, color = Color(0xFF20242A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("${action.owner} · ${action.dueLabel}", color = Color(0xFF7D858F), fontSize = 11.sp)
        }
        TextButton(onClick = { onCreateTask(action) }) {
            Text("添加", color = Color(0xFF1E5BCE), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FoldableChapter() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("录音流程", color = Color(0xFF20242A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        items(MVP_JOURNEY) { step ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F7FB), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(step.title, color = Color(0xFF20242A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(step.capability, color = Color(0xFF1E5BCE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(step.value, color = Color(0xFF6B737D), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FoldableSpeakers(transcript: List<TranscriptLine>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Speakers", color = Color(0xFF20242A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("说话人识别 · 多语言识别", color = Color(0xFF1E5BCE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        transcript.map { it.speaker }.distinct().forEachIndexed { index, speaker ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(11.dp).background(listOf(Color(0xFF8DD1F1), Color(0xFFFFC36D), Color(0xFFB7A6FF))[index % 3], CircleShape))
                Text(speaker, color = Color(0xFF20242A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RecordingSessionStrip() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("录音中", "会议模式", "实时转写", "后台可继续").forEach {
                StatusPill(it)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("当前摘要", "待办", "问答", "分享").forEach {
                StatusPill(it)
            }
        }
    }
}

@Composable
private fun MvpAiQualityCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FBFE), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("智能处理", color = Color(0xFF20242A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("多语言转录 → 摘要 → 问答，生成速度、内容准确率和召回率按 Benchmark 对齐。", color = Color(0xFF2B3036), fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("中文/英文自动识别")
            StatusPill("准确率校验")
            StatusPill("召回率校验")
        }
    }
}

@Composable
private fun SystemLinkageCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F7FB), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("系统应用联动 / 多端知识库", color = Color(0xFF20242A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("一键创建待办，并同步到手机 Recorder、耳机 Companion app 与项目知识库。", color = Color(0xFF2B3036), fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("Reminder")
            StatusPill("Recorder")
            StatusPill("Companion app")
            StatusPill("知识库")
        }
    }
}

@Composable
private fun PlaybackControls(state: RecorderState, onAiKeyToggle: () -> Unit, onStop: () -> Unit) {
    val elapsed = when (state) {
        RecorderState.Idle -> 0L
        is RecorderState.Recording -> state.elapsedMs
        is RecorderState.IncomingCallRinging -> state.elapsedMs
        is RecorderState.PausedForCall -> state.elapsedMs
        is RecorderState.Sealing -> state.elapsedMs
        is RecorderState.Saved -> state.durationMs
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(formatTimer(elapsed), color = Color(0xFF20242A), fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(20.dp))
        OutlinedButton(onClick = onAiKeyToggle, shape = CircleShape) {
            Text(if (state is RecorderState.Idle || state is RecorderState.Saved) "开始" else "暂停", color = Color(0xFF20242A), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Button(onClick = onStop, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D))) {
            Text("■", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReminderBanner(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF4EC), RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Adding to Reminder", color = Color(0xFF20242A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("✓", color = Color(0xFF3478F6), fontSize = 18.sp, modifier = Modifier.clickable(onClick = onDismiss))
    }
}

@Composable
private fun StatusPill(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val modifier = Modifier
        .background(if (selected) Color(0xFFD9ECFF) else Color(0xFFEAF2FF), RoundedCornerShape(18.dp))
        .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
        .padding(horizontal = 10.dp, vertical = 5.dp)
    Text(text, color = Color(0xFF1E5BCE), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun FoldDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(Color(0xFFE1E4E8)),
    )
}

private data class FoldableRecording(
    val title: String,
    val meta: String,
    val status: String,
)

private data class MvpJourneyStep(
    val title: String,
    val capability: String,
    val value: String,
)

private val MVP_JOURNEY = listOf(
    MvpJourneyStep(
        title = "触发：息屏/后台快速开始",
        capability = "无需进入 App，直接进入录音",
        value = "突出硬件入口和低打扰录音。",
    ),
    MvpJourneyStep(
        title = "录音中：实时转写、说话人区分",
        capability = "根据场景保持清晰收音",
        value = "会议、访谈、课堂等场景可依赖。",
    ),
    MvpJourneyStep(
        title = "后台：玲珑台显示实时进程",
        capability = "无需蓝牙传输的稳定实时能力",
        value = "用户退到后台仍可确认录音安全进行。",
    ),
    MvpJourneyStep(
        title = "结束：快速停止并同步玲珑台",
        capability = "低功耗录音与会话封存",
        value = "结束动作清晰，避免误录。",
    ),
    MvpJourneyStep(
        title = "智能处理：摘要、模板、问答",
        capability = "天禧/Qira 多语言转录-摘要-问答",
        value = "生成速度、准确率、召回率与 Benchmark 对齐。",
    ),
    MvpJourneyStep(
        title = "系统联动：一键创建待办",
        capability = "Reminder / 系统应用联动",
        value = "行动项能直接进入用户工作流。",
    ),
    MvpJourneyStep(
        title = "深度应用：跨文件问答与项目复盘",
        capability = "手机 Recorder + 耳机 Companion app + 多端知识库",
        value = "从单次会议走向长期项目知识沉淀。",
    ),
)

private enum class FoldableTab(val label: String) {
    Transcription("Transcription"),
    Summary("Summary"),
    Chapter("Chapter"),
    Speakers("Speakers"),
}
