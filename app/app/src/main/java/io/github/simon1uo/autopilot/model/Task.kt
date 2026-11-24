package io.github.simon1uo.autopilot.model

import kotlinx.serialization.Serializable

/**
 * 一个自动化任务：定时规则 + 一串按顺序执行的动作。
 */
@Serializable
data class Task(
    val id: Long = System.currentTimeMillis(),
    val name: String = "新任务",
    val enabled: Boolean = true,
    /* 时间 (每天 HH:mm) */
    val hour: Int = 7,
    val minute: Int = 0,
    /* 星期几，下标 0=周一 .. 6=周日 */
    val daysOfWeek: List<Boolean> = List(7) { true },
    /* 重复间隔（分钟），0 = 不启用间隔，仅每天固定时间 */
    val intervalMinutes: Int = 0,
    /* 动作序列（按顺序执行），默认给两个：亮屏 + 启动App */
    val actions: List<Action> = listOf(Action.wake(), Action.launch("", "", "")),
) {
    val hasAnyDay: Boolean get() = daysOfWeek.any { it }
    val timeText: String get() =
        if (intervalMinutes > 0) "每 ${intervalMinutes} 分钟" else String.format("%02d:%02d", hour, minute)
}
