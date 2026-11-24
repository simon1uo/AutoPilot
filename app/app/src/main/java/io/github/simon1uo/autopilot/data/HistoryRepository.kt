package io.github.simon1uo.autopilot.data

import android.content.Context
import io.github.simon1uo.autopilot.model.HistoryEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** 任务执行历史，JSON 文件持久化，最多保留 200 条。 */
class HistoryRepository(ctx: Context) {
    private val file = File(ctx.filesDir, "history.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
    private val maxEntries = 200

    fun load(): MutableList<HistoryEntry> = try {
        if (file.exists()) json.decodeFromString<List<HistoryEntry>>(file.readText()).toMutableList()
        else mutableListOf()
    } catch (e: Exception) { mutableListOf() }

    /** 最新在前。 */
    fun add(entry: HistoryEntry) {
        val list = load()
        list.add(0, entry)
        while (list.size > maxEntries) list.removeAt(list.size - 1)
        save(list)
    }

    fun save(list: List<HistoryEntry>) {
        try { file.writeText(json.encodeToString<List<HistoryEntry>>(list)) } catch (_: Exception) {}
    }

    fun clear() { try { file.writeText("[]") } catch (_: Exception) {} }
}
