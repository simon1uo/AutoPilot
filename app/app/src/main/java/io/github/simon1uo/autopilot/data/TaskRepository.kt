package io.github.simon1uo.autopilot.data

import android.content.Context
import io.github.simon1uo.autopilot.model.Action
import io.github.simon1uo.autopilot.model.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import java.io.File

/** 极简 JSON 文件持久化，含 v1->v2 自动迁移。 */
class TaskRepository(ctx: Context) {
    private val file = File(ctx.filesDir, "tasks.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    fun load(): MutableList<Task> = try {
        val list = if (file.exists()) migrate(file.readText()) else mutableListOf()
        list.sortedBy { it.order }.toMutableList()
    } catch (e: Exception) { mutableListOf() }

    fun save(tasks: List<Task>) {
        try { file.writeText(json.encodeToString<List<Task>>(tasks)) } catch (_: Exception) {}
    }

    /**
     * 读取并迁移。v1 任务无 actions 字段，只有 wakeScreen/launchApp 两个布尔，
     * 自动转换为动作列表。
     */
    private fun migrate(text: String): MutableList<Task> {
        // 先尝试直接按 v2 解析
        val raw = json.parseToJsonElement(text).jsonArray
        val result = mutableListOf<Task>()
        var changed = false
        for (el in raw) {
            val obj = el.jsonObject
            if (obj.containsKey("actions")) {
                result.add(json.decodeFromJsonElement(Task.serializer(), obj))
            } else {
                // v1 -> v2 迁移
                changed = true
                val id = obj["id"]?.jsonPrimitive?.let { runCatching { it.toString().toLong() }.getOrNull() } ?: System.currentTimeMillis()
                val name = obj["name"]?.jsonPrimitive?.toString()?.trim('"') ?: "新任务"
                val enabled = obj["enabled"]?.jsonPrimitive?.boolean ?: true
                val hour = obj["hour"]?.jsonPrimitive?.int ?: 7
                val minute = obj["minute"]?.jsonPrimitive?.int ?: 0
                val interval = obj["intervalMinutes"]?.jsonPrimitive?.let { runCatching { it.int }.getOrNull() } ?: 0
                val days = (obj["daysOfWeek"]?.jsonArray)?.map { it.jsonPrimitive.boolean } ?: List(7) { true }
                val pkg = obj["packageName"]?.jsonPrimitive?.toString()?.trim('"') ?: ""
                val act = obj["activityName"]?.jsonPrimitive?.toString()?.trim('"') ?: ""
                val label = obj["appLabel"]?.jsonPrimitive?.toString()?.trim('"') ?: ""
                val wake = obj["wakeScreen"]?.jsonPrimitive?.boolean ?: true
                val launch = obj["launchApp"]?.jsonPrimitive?.boolean ?: true
                val acts = mutableListOf<Action>()
                if (wake) acts.add(Action.wake())
                if (launch && pkg.isNotBlank()) acts.add(Action.launch(pkg, act, label))
                if (acts.isEmpty()) acts.add(Action.wake())
                result.add(Task(id, name, enabled, hour, minute, days, interval, acts))
            }
        }
        // 为缺少 order 的旧任务按当前顺序补 order（全部为 0 视为未设置）
        if (result.isNotEmpty() && result.all { it.order == 0 }) {
            for ((i, t) in result.withIndex()) result[i] = t.copy(order = i)
            changed = true
        }
        if (changed) save(result)   // 迁移后回写
        return result
    }
}