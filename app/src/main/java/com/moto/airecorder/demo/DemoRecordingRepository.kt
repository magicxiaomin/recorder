package com.moto.airecorder.demo

import android.content.Context
import com.moto.airecorder.ui.formatTimer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

data class TranscriptLine(
    val timestampMs: Long,
    val speaker: String,
    val text: String,
)

data class TimestampedText(
    val timestampMs: Long,
    val text: String,
)

data class ActionItem(
    val id: String,
    val title: String,
    val owner: String,
    val dueLabel: String,
    val timestampMs: Long,
)

data class DemoSummary(
    val title: String,
    val durationLabel: String,
    val overview: String,
    val keyPoints: List<TimestampedText>,
    val actions: List<ActionItem>,
)

data class CreatedTask(
    val title: String,
    val source: String,
    val timestampMs: Long,
)

@Singleton
class DemoRecordingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun loadTranscript(): List<TranscriptLine> {
        return context.assets.open("demo/transcript.jsonl").bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }
                .map { JSONObject(it) }
                .map { json ->
                    TranscriptLine(
                        timestampMs = json.getLong("timestampMs"),
                        speaker = json.getString("speaker"),
                        text = json.getString("text"),
                    )
                }
                .toList()
        }
    }

    fun loadSummary(): DemoSummary {
        val json = JSONObject(context.assets.open("demo/summary.json").bufferedReader().use { it.readText() })
        val keyPoints = json.getJSONArray("keyPoints").let { array ->
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                TimestampedText(
                    timestampMs = item.getLong("timestampMs"),
                    text = item.getString("text"),
                )
            }
        }
        val actions = json.getJSONArray("actions").let { array ->
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                ActionItem(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    owner = item.getString("owner"),
                    dueLabel = item.getString("dueLabel"),
                    timestampMs = item.getLong("timestampMs"),
                )
            }
        }

        return DemoSummary(
            title = json.getString("title"),
            durationLabel = json.getString("durationLabel"),
            overview = json.getString("overview"),
            keyPoints = keyPoints,
            actions = actions,
        )
    }

    fun createTask(action: ActionItem): CreatedTask {
        val task = CreatedTask(
            title = action.title,
            source = "AI 录音 ${formatTimer(action.timestampMs)}",
            timestampMs = action.timestampMs,
        )
        saveCreatedTasks(loadCreatedTasks() + task)
        return task
    }

    fun loadCreatedTasks(): List<CreatedTask> {
        val raw = preferences.getString(KEY_CREATED_TASKS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            CreatedTask(
                title = item.getString("title"),
                source = item.getString("source"),
                timestampMs = item.getLong("timestampMs"),
            )
        }
    }

    private fun saveCreatedTasks(tasks: List<CreatedTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject()
                    .put("title", task.title)
                    .put("source", task.source)
                    .put("timestampMs", task.timestampMs),
            )
        }
        preferences.edit().putString(KEY_CREATED_TASKS, array.toString()).apply()
    }

    private val preferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private companion object {
        const val PREFERENCES_NAME = "demo_recording_repository"
        const val KEY_CREATED_TASKS = "created_tasks"
    }
}
