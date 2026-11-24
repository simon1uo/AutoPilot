package io.github.simon1uo.autopilot.model

import kotlinx.serialization.Serializable

/** 一次任务执行记录。 */
@Serializable
data class HistoryEntry(
    val id: Long = System.currentTimeMillis(),
    val taskId: Long,
    val taskName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean,
    val message: String = "",          // 简要结果 / 失败原因
    val stepLog: List<String> = emptyList(),  // 每步执行明细 "WAKE_SCREEN: ok"
)
