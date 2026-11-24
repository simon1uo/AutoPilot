package io.github.simon1uo.autopilot.data

import android.content.Context
import io.github.simon1uo.autopilot.model.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** 极简 JSON 文件持久化（快速简洁，避免引入 Room）。 */
class TaskRepository(ctx: Context) {
    private val file = File(ctx.filesDir, "tasks.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    fun load(): MutableList<Task> = try {
        if (file.exists()) json.decodeFromString<List<Task>>(file.readText()).toMutableList()
        else mutableListOf()
    } catch (e: Exception) { mutableListOf() }

    fun save(tasks: List<Task>) {
        try { file.writeText(json.encodeToString<List<Task>>(tasks)) } catch (_: Exception) {}
    }
}