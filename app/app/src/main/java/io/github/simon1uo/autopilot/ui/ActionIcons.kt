package io.github.simon1uo.autopilot.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.simon1uo.autopilot.model.ActionType

fun ActionType.iconVector(): ImageVector = when (this) {
    ActionType.WAKE_SCREEN -> Icons.Outlined.LightMode
    ActionType.LAUNCH_APP -> Icons.Outlined.Apps
    ActionType.WAIT -> Icons.Outlined.Timer
    ActionType.SET_VOLUME -> Icons.Outlined.VolumeUp
    ActionType.TOGGLE_WIFI -> Icons.Outlined.Wifi
    ActionType.TOGGLE_BLUETOOTH -> Icons.Outlined.Bluetooth
    ActionType.NOTIFY -> Icons.Outlined.Notifications
    ActionType.BEEP_VIBRATE -> Icons.Outlined.Vibration
}
