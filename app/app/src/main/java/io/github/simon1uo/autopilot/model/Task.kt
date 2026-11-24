package io.github.simon1uo.autopilot.model

import kotlinx.serialization.Serializable

/**
 * 一个自动化任务：定时 -> 亮屏 -> 打开指定 App。
 * 一个任务包含：定时规则 + 到点动作(亮屏/启动App)。
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
    /* 目标 App */
    val packageName: String = "",
    val activityName: String = "",
    val appLabel: String = "",
    /* 动作开关 */
    val wakeScreen: Boolean = true,
    val launchApp: Boolean = true,
) {
    /** 是否有任何启用的星期 */
    val hasAnyDay: Boolean get() = daysOfWeek.any { it }
}