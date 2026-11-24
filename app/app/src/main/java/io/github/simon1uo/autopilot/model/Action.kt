package io.github.simon1uo.autopilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 任务的单个动作。多态结构，由 type 区分，各动作带自己的参数。
 * 序列按列表顺序执行。
 */
@Serializable
data class Action(
    val type: ActionType,
    val enabled: Boolean = true,
    /* 启动 App */
    val packageName: String = "",
    val activityName: String = "",
    val appLabel: String = "",
    /* 等待 */
    val waitSeconds: Int = 5,
    /* 音量 0..100 */
    val volumePercent: Int = 50,
    /* 开关类目标状态 */
    val turnOn: Boolean = true,
    /* 通知/提示音文本 */
    val message: String = "",
) {
    companion object {
        fun wake() = Action(type = ActionType.WAKE_SCREEN)
        fun launch(pkg: String, act: String = "", label: String = "") =
            Action(type = ActionType.LAUNCH_APP, packageName = pkg, activityName = act, appLabel = label)
        fun wait(sec: Int = 5) = Action(type = ActionType.WAIT, waitSeconds = sec)
        fun volume(p: Int = 50) = Action(type = ActionType.SET_VOLUME, volumePercent = p)
        fun wifi(on: Boolean = true) = Action(type = ActionType.TOGGLE_WIFI, turnOn = on)
        fun bluetooth(on: Boolean = true) = Action(type = ActionType.TOGGLE_BLUETOOTH, turnOn = on)
        fun notify(msg: String = "AutoPilot 任务已执行") = Action(type = ActionType.NOTIFY, message = msg)
        fun beep() = Action(type = ActionType.BEEP_VIBRATE)
    }
}

@Serializable
enum class ActionType(val label: String, val icon: String) {
    @SerialName("wake_screen") WAKE_SCREEN("唤醒屏幕", "sun"),
    @SerialName("launch_app") LAUNCH_APP("启动 App", "app"),
    @SerialName("wait") WAIT("等待", "timer"),
    @SerialName("set_volume") SET_VOLUME("调节音量", "volume"),
    @SerialName("toggle_wifi") TOGGLE_WIFI("WiFi 开关", "wifi"),
    @SerialName("toggle_bluetooth") TOGGLE_BLUETOOTH("蓝牙开关", "bluetooth"),
    @SerialName("notify") NOTIFY("发送通知", "bell"),
    @SerialName("beep_vibrate") BEEP_VIBRATE("提示音/震动", "sound"),
}

/** 简短摘要，用于卡片/列表显示 */
fun Action.summary(): String = when (type) {
    ActionType.WAKE_SCREEN -> "点亮屏幕"
    ActionType.LAUNCH_APP -> "打开 " + appLabel.ifBlank { packageName.ifBlank { "未知App" } }
    ActionType.WAIT -> "等待 ${waitSeconds} 秒"
    ActionType.SET_VOLUME -> "音量设为 ${volumePercent}%"
    ActionType.TOGGLE_WIFI -> if (turnOn) "打开 WiFi" else "关闭 WiFi"
    ActionType.TOGGLE_BLUETOOTH -> if (turnOn) "打开蓝牙" else "关闭蓝牙"
    ActionType.NOTIFY -> "通知：${message.ifBlank { "(空)" }}"
    ActionType.BEEP_VIBRATE -> "响铃 + 震动"
}
