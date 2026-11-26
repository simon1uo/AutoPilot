package io.github.simon1uo.autopilot.engine

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * 权限检测与跳转引导。每项提供：是否已授予 + 打开系统授权入口的 Intent + 是否受系统版本限制。
 */
object Permissions {

    enum class Perm(val key: String, val title: String, val desc: String) {
        EXACT_ALARM("exact_alarm", "精确闹钟", "保证定时任务准时触发，不被系统延迟"),
        POST_NOTIF("notif", "通知权限", "任务执行提醒、HTTP 服务常驻通知"),
        BATTERY_OPT("battery", "电池优化白名单", "避免后台任务被系统清理/休眠"),
        OVERLAY("overlay", "悬浮窗权限", "部分自动化动作需要（可选）"),
    }

    /** 该权限在当前系统版本是否「需要」且「可检查」 */
    fun isRelevant(ctx: Context, p: Perm): Boolean = when (p) {
        Perm.EXACT_ALARM -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Perm.POST_NOTIF -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        Perm.BATTERY_OPT -> true
        Perm.OVERLAY -> true
    }

    fun isGranted(ctx: Context, p: Perm): Boolean = when (p) {
        Perm.EXACT_ALARM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        } else true
        Perm.POST_NOTIF -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true
        Perm.BATTERY_OPT -> (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
        Perm.OVERLAY -> Settings.canDrawOverlays(ctx)
    }

    /** 跳转系统授权入口。返回是否成功发起 Intent（通知权限建议在 Activity 里走运行时请求）。 */
    fun openSettingsFor(ctx: Context, p: Perm): Boolean {
        val intent: Intent = when (p) {
            Perm.EXACT_ALARM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                else return false
            }
            Perm.BATTERY_OPT -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
            Perm.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
            Perm.POST_NOTIF -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent); true
        } catch (e: Exception) {
            try {
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: Exception) { false }
        }
    }
}