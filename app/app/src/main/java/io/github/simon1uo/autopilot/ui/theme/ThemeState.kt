package io.github.simon1uo.autopilot.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.simon1uo.autopilot.data.SettingsStore

/**
 * 全局可观察的主题状态。SettingsScreen 修改后立即反映到 AutoPilotTheme。
 * 进程内单例，足够支撑「点完立即生效」。
 */
object ThemeState {
    private var store: SettingsStore? = null
    var mode by mutableStateOf(ThemeMode.DARK)
        private set

    fun init(ctx: Context) {
        if (store == null) {
            store = SettingsStore(ctx.applicationContext)
            mode = store!!.themeMode
        }
    }

    fun set(ctx: Context, newMode: String) {
        if (store == null) store = SettingsStore(ctx.applicationContext)
        store!!.themeMode = newMode
        mode = newMode
    }
}
