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
import androidx.compose.material3.TextField
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
    var summaryConfig by remember { mutableStateOf(SummaryConfigState()) }
    var showSummaryConfig by remember { mutableStateOf(false) }
    var selectedScene by remember { mutableStateOf("会议纪要") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var question by remember { mutableStateOf("") }
    var qaScope by remember { mutableStateOf("当前文件") }
    var answers by remember { mutableStateOf(listOf("可询问本次录音，也可以切换到跨文件问答。")) }

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
        Box(Modifier.fillMaxSize()) {
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

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        DetailTab.Transcript -> TranscriptList(
                            transcript = transcript,
                            anchorMs = anchorMs,
                            playbackMs = playbackMs,
                        )
                        DetailTab.Summary -> SummaryPane(
                            summary = summary,
                            config = summaryConfig,
                            selectedScene = selectedScene,
                            generating = generating,
                            progressStep = progressStep,
                            summaryReady = summaryReady,
                            onEditConfig = { showSummaryConfig = true },
                            onSceneChange = { selectedScene = it },
                            onGenerate = {
                                summaryReady = false
                                feedback = "已使用 ${summaryConfig.sceneTemplate} / ${summaryConfig.granularity} / ${summaryConfig.model}"
                                generating = true
                            },
                            onJump = { timestampMs ->
                                anchorMs = timestampMs
                                playbackMs = timestampMs
                                selectedTab = DetailTab.Transcript
                            },
                            onCreateTask = onCreateTask,
                            onFeedback = { feedback = it },
                        )
                        DetailTab.Tasks -> TasksPane(tasks = tasks)
                    }
                }
                feedback?.let {
                    Spacer(Modifier.height(10.dp))
                    FeedbackBar(message = it, onDismiss = { feedback = null })
                }
                Spacer(Modifier.height(10.dp))
                QaPanel(
                    scope = qaScope,
                    question = question,
                    answers = answers,
                    onScopeChange = { qaScope = it },
                    onQuestionChange = { question = it },
                    onAsk = {
                        if (question.isNotBlank()) {
                            answers = listOf(fakeAnswer(qaScope, question))
                            question = ""
                        }
                    },
                    onReviewDoc = {
                        answers = listOf(PROJECT_REVIEW_DOC)
                        feedback = "复盘文档已生成，可导出或发送到邮件/微信/Teams/飞书/钉钉"
                    },
                    onExport = { feedback = "已导出问答/复盘文档" },
                    onSend = { target -> feedback = "已发送到 $target" },
                )
            }
            if (showSummaryConfig) {
                SummaryConfigSheet(
                    config = summaryConfig,
                    onConfigChange = { summaryConfig = it },
                    onDismiss = { showSummaryConfig = false },
                )
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

private data class SummaryConfigState(
    val sceneTemplate: String = "会议场景模板",
    val granularity: String = "常规颗粒度",
    val model: String = "默认在线模型",
)

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
    config: SummaryConfigState,
    selectedScene: String,
    generating: Boolean,
    progressStep: Int,
    summaryReady: Boolean,
    onEditConfig: () -> Unit,
    onSceneChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onJump: (Long) -> Unit,
    onCreateTask: (ActionItem) -> Unit,
    onFeedback: (String) -> Unit,
) {
    when {
        generating -> SummaryProgress(progressStep = progressStep)
        summaryReady -> SummaryResult(
            summary = summary,
            selectedScene = selectedScene,
            onSceneChange = onSceneChange,
            onJump = onJump,
            onCreateTask = onCreateTask,
            onFeedback = onFeedback,
        )
        else -> SummaryConfig(summary = summary, config = config, onEditConfig = onEditConfig, onGenerate = onGenerate)
    }
}

@Composable
private fun SummaryConfig(
    summary: DemoSummary,
    config: SummaryConfigState,
    onEditConfig: () -> Unit,
    onGenerate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("生成纪要", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("${summary.title} · ${summary.durationLabel}", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold)
        SettingRow("场景模板", config.sceneTemplate)
        SettingRow("摘要颗粒度", config.granularity)
        SettingRow("模型", config.model)
        OutlinedButton(onClick = onEditConfig, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("修改生成配置", color = Color.White, fontWeight = FontWeight.Bold)
        }
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
    selectedScene: String,
    onSceneChange: (String) -> Unit,
    onJump: (Long) -> Unit,
    onCreateTask: (ActionItem) -> Unit,
    onFeedback: (String) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("会议纪要", "项目复盘", "客户访谈").forEach { scene ->
                    DetailTabButton(scene, selectedScene == scene) { onSceneChange(scene) }
                }
            }
        }
        item {
            Text(sceneOverview(selectedScene, summary.overview), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
        item {
            MindMapCard()
        }
        item {
            ExportShareRow(
                onExport = { onFeedback("已导出转写、纪要和思维导图") },
                onShare = { target -> onFeedback("已分享到 $target") },
            )
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

@Composable
private fun SummaryConfigSheet(
    config: SummaryConfigState,
    onConfigChange: (SummaryConfigState) -> Unit,
    onDismiss: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("摘要生成配置", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OptionGroup(
                label = "场景模板",
                options = listOf("会议场景模板", "项目复盘模板", "访谈纪要模板"),
                selected = config.sceneTemplate,
                onSelected = { onConfigChange(config.copy(sceneTemplate = it)) },
            )
            OptionGroup(
                label = "摘要颗粒度",
                options = listOf("常规颗粒度", "简洁摘要", "详细摘要"),
                selected = config.granularity,
                onSelected = { onConfigChange(config.copy(granularity = it)) },
            )
            OptionGroup(
                label = "模型",
                options = listOf("默认在线模型", "高精度在线模型", "本地快速模型"),
                selected = config.model,
                onSelected = { onConfigChange(config.copy(model = it)) },
            )
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("应用配置", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OptionGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color(0xFF9A958A), fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                DetailTabButton(option, selected == option) { onSelected(option) }
            }
        }
    }
}

@Composable
private fun MindMapCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151417), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("思维导图", color = Color(0xFF9A958A), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Text("芝加哥发布会", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("├─ 隐私文案：通话音频不录制", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold)
        Text("├─ 法务审核：免责声明明天午饭前完成", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold)
        Text("└─ 追溯链路：纪要 → 转写 → 音频", color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExportShareRow(onExport: () -> Unit, onShare: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onExport,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F8A5B)),
            ) {
                Text("导出", color = Color.White, fontWeight = FontWeight.Bold)
            }
            listOf("邮件", "微信", "Teams", "飞书", "钉钉").forEach { target ->
                OutlinedButton(onClick = { onShare(target) }, shape = RoundedCornerShape(8.dp)) {
                    Text(target, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun FeedbackBar(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F8A5B), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("关闭", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onDismiss))
    }
}

@Composable
private fun QaPanel(
    scope: String,
    question: String,
    answers: List<String>,
    onScopeChange: (String) -> Unit,
    onQuestionChange: (String) -> Unit,
    onAsk: () -> Unit,
    onReviewDoc: () -> Unit,
    onExport: () -> Unit,
    onSend: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151417), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailTabButton("当前文件", scope == "当前文件") { onScopeChange("当前文件") }
            DetailTabButton("跨文件", scope == "跨文件") { onScopeChange("跨文件") }
            OutlinedButton(onClick = onReviewDoc, shape = RoundedCornerShape(8.dp)) {
                Text("生成项目复盘", color = Color.White)
            }
        }
        answers.take(1).forEach {
            Text(it, color = Color(0xFFE9DFC4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = question,
                onValueChange = onQuestionChange,
                placeholder = { Text("输入问题，例如：隐私声明是谁负责？") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(
                onClick = onAsk,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
            ) {
                Text("提问", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExport, shape = RoundedCornerShape(8.dp)) {
                Text("导出文档", color = Color.White)
            }
            listOf("邮件", "微信", "Teams", "飞书", "钉钉").forEach { target ->
                Text(
                    target,
                    color = Color(0xFFE9DFC4),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSend(target) },
                )
            }
        }
    }
}

private fun sceneOverview(scene: String, defaultOverview: String): String {
    return when (scene) {
        "项目复盘" -> "项目复盘：本次会议确认了发布路径、隐私口径和可追溯任务链路。风险集中在法务免责声明和 SLT 材料打包，后续需要按责任人闭环。"
        "客户访谈" -> "客户访谈：核心关注点是隐私承诺、演示素材可信度，以及任务是否能回溯到原始录音证据。"
        else -> defaultOverview
    }
}

private fun fakeAnswer(scope: String, question: String): String {
    return if (scope == "跨文件") {
        "跨文件回答：结合历史项目会议，$question 的相关结论集中在隐私文案、法务审核和 SLT 发布准备三个主题。"
    } else {
        "当前文件回答：$question 的答案可追溯到 01:16 和 02:28，佳怡负责法务审核，阿俊负责讲稿和来源备注。"
    }
}

private const val PROJECT_REVIEW_DOC =
    "项目复盘草稿：芝加哥发布会 demo 已明确隐私声明、法务审核和追溯链路。建议在下次评审前完成免责声明确认、展台讲稿更新和 SLT 材料投递。"

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
