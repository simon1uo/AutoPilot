package io.github.simon1uo.autopilot.data

import android.content.Context

/**
 * 应用设置，SharedPreferences 轻量持久化。
 * 附带一次性的 Reactive 通知，可选。
 */
class SettingsStore(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("autopilot_settings", Context.MODE_PRIVATE)

    var autoStartOnBoot: Boolean
        get() = sp.getBoolean("auto_start_on_boot", true)
        set(v) = sp.edit().putBoolean("auto_start_on_boot", v).apply()

    var newTaskEnabledByDefault: Boolean
        get() = sp.getBoolean("new_task_enabled", true)
        set(v) = sp.edit().putBoolean("new_task_enabled", v).apply()

    /** 主题：dark / amoled / light / system —— 见 ThemeMode */
    var themeMode: String
        get() = sp.getString("theme_mode", "dark") ?: "dark"
        set(v) = sp.edit().putString("theme_mode", v).apply()

    var httpAutoStart: Boolean
        get() = sp.getBoolean("http_auto_start", true)
        set(v) = sp.edit().putBoolean("http_auto_start", v).apply()

    var httpPort: Int
        get() = sp.getInt("http_port", 8080)
        set(v) = sp.edit().putInt("http_port", v.coerceIn(1024, 65535)).apply()

    var notifyOnTaskRun: Boolean
        get() = sp.getBoolean("notify_on_task_run", true)
        set(v) = sp.edit().putBoolean("notify_on_task_run", v).apply()

    var notifySound: Boolean
        get() = sp.getBoolean("notify_sound", true)
        set(v) = sp.edit().putBoolean("notify_sound", v).apply()

    var autoCheckUpdate: Boolean
        get() = sp.getBoolean("auto_check_update", true)
        set(v) = sp.edit().putBoolean("auto_check_update", v).apply()

    var lastCheckedUpdateVersion: String
        get() = sp.getString("last_checked_update", "") ?: ""
        set(v) = sp.edit().putString("last_checked_update", v).apply()
}