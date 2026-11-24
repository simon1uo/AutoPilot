package io.github.simon1uo.autopilot.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import io.github.simon1uo.autopilot.model.Action
import io.github.simon1uo.autopilot.model.ActionType
import io.github.simon1uo.autopilot.model.Task
import kotlinx.coroutines.delay

/**
 * 动作执行引擎：按顺序执行任务的动作序列。
 */
object CoreActions {
    private const val TAG = "AutoPilot/CoreActions"

    /** 顺序执行一个任务的全部动作（挂起，支持等待）。 */
    suspend fun runTask(ctx: Context, task: Task) {
        Log.i(TAG, "runTask: ${task.name}, ${task.actions.size} actions")
        val settings = io.github.simon1uo.autopilot.data.SettingsStore(ctx)
        if (settings.notifyOnTaskRun) {
            notifyExtra(ctx, "AutoPilot", "任务「${task.name}」已执行", settings.notifySound)
        }
        val stepLog = mutableListOf<String>()
        var failed = 0
        for ((idx, a) in task.actions.withIndex()) {
            if (!a.enabled) { Log.i(TAG, "  [$idx] skipped(disabled) ${a.type}"); stepLog.add("${a.type}: skipped"); continue }
            try {
                Log.i(TAG, "  [$idx] exec ${a.type}")
                execute(ctx, a)
                stepLog.add("${a.type.label}: ok")
            } catch (e: Exception) {
                failed++
                Log.e(TAG, "  [$idx] action ${a.type} failed: ${e.message}")
                stepLog.add("${a.type.label}: 失败 ${e.message}")
            }
        }
        try {
            io.github.simon1uo.autopilot.data.HistoryRepository(ctx).add(
                io.github.simon1uo.autopilot.model.HistoryEntry(
                    taskId = task.id, taskName = task.name,
                    success = failed == 0,
                    message = if (failed == 0) "执行成功 (${task.actions.size} 步)" else "$failed 步失败",
                    stepLog = stepLog,
                )
            )
        } catch (e: Exception) { Log.e(TAG, "history save failed: ${e.message}") }
    }

    private suspend fun execute(ctx: Context, a: Action) {
        when (a.type) {
            ActionType.WAKE_SCREEN -> wakeScreen(ctx)
            ActionType.LAUNCH_APP -> launchApp(ctx, a.packageName, a.activityName)
            ActionType.WAIT -> delay(a.waitSeconds.coerceAtLeast(0) * 1000L)
            ActionType.SET_VOLUME -> setVolume(ctx, a.volumePercent)
            ActionType.TOGGLE_WIFI -> toggleWifi(ctx, a.turnOn)
            ActionType.TOGGLE_BLUETOOTH -> toggleBluetooth(ctx, a.turnOn)
            ActionType.NOTIFY -> notify(ctx, a.message)
            ActionType.BEEP_VIBRATE -> beepAndVibrate(ctx)
        }
    }

    /** WakeLock 点亮屏幕：SCREEN_BRIGHT|ACQUIRE_CAUSES_WAKEUP|ON_AFTER_RELEASE = 0x3000000A */
    @Suppress("DEPRECATION")
    fun wakeScreen(ctx: Context) {
        try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isScreenOn) {
                val wl = pm.newWakeLock(0x3000000A, "autopilot:wake").apply { setReferenceCounted(false) }
                wl.acquire(1000L)
                Log.i(TAG, "wakeScreen: acquired 0x3000000A")
            } else Log.i(TAG, "wakeScreen: already on")
        } catch (e: Exception) { Log.e(TAG, "wakeScreen failed: ${e.message}") }
    }

    fun launchApp(ctx: Context, pkg: String, activity: String): Boolean {
        if (pkg.isBlank()) { Log.w(TAG, "launchApp: empty package"); return false }
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                if (activity.isNotBlank()) setClassName(pkg, activity) else setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // 若没给 activity，用 launch intent 兜底
            if (activity.isBlank()) {
                val li = ctx.packageManager.getLaunchIntentForPackage(pkg)
                if (li != null) { li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(li); return true }
            }
            ctx.startActivity(intent)
            Log.i(TAG, "launchApp: $pkg/$activity")
            true
        } catch (e: Exception) {
            Log.e(TAG, "launchApp exact failed: ${e.message}, fallback")
            try {
                val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
                if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(i); true } else false
            } catch (e2: Exception) { Log.e(TAG, "fallback failed: ${e2.message}"); false }
        }
    }

    private fun setVolume(ctx: Context, percent: Int) {
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (max * percent.coerceIn(0, 100) / 100f).toInt().coerceIn(0, max)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            Log.i(TAG, "setVolume: $percent% -> $target/$max")
        } catch (e: Exception) { Log.e(TAG, "setVolume failed: ${e.message}") }
    }

    @Suppress("DEPRECATION")
    private fun toggleWifi(ctx: Context, on: Boolean) {
        try {
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wm.isWifiEnabled = on   // Android 10+ 通常被系统拒绝，会静默失败
            Log.i(TAG, "toggleWifi: $on (may be restricted on Android 10+)")
        } catch (e: Exception) { Log.e(TAG, "toggleWifi failed: ${e.message}") }
    }

    private fun toggleBluetooth(ctx: Context, on: Boolean) {
        try {
            val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter: BluetoothAdapter? = bm?.adapter
            if (adapter == null) { Log.w(TAG, "toggleBluetooth: no adapter"); return }
            @Suppress("DEPRECATION")
            if (on) adapter.enable() else adapter.disable() // Android 13+ 受限
            Log.i(TAG, "toggleBluetooth: $on")
        } catch (e: Exception) { Log.e(TAG, "toggleBluetooth failed: ${e.message}") }
    }

    private fun notifyExtra(ctx: Context, title: String, msg: String, sound: Boolean) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = "autopilot_run"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(ch) == null) {
                val chan = NotificationChannel(ch, "任务执行", if (sound) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_LOW)
                if (!sound) chan.setSound(null, null)
                nm.createNotificationChannel(chan)
            }
            val n = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) android.app.Notification.Builder(ctx, ch)
                    else @Suppress("DEPRECATION") android.app.Notification.Builder(ctx)
            n.setContentTitle(title).setContentText(msg).setSmallIcon(android.R.drawable.ic_dialog_info).setAutoCancel(true)
            nm.notify(("run_" + System.currentTimeMillis()).hashCode(), n.build())
        } catch (e: Exception) { Log.e(TAG, "notifyExtra failed: ${e.message}") }
    }

    private fun notify(ctx: Context, msg: String) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = "autopilot_task"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(ch) == null) {
                nm.createNotificationChannel(NotificationChannel(ch, "任务通知", NotificationManager.IMPORTANCE_DEFAULT))
            }
            val n = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                android.app.Notification.Builder(ctx, ch)
            else @Suppress("DEPRECATION") android.app.Notification.Builder(ctx)
            n.setContentTitle("AutoPilot").setContentText(msg)
                .setSmallIcon(android.R.drawable.ic_dialog_info).setAutoCancel(true)
            nm.notify(System.currentTimeMillis().toInt(), n.build())
            Log.i(TAG, "notify: $msg")
        } catch (e: Exception) { Log.e(TAG, "notify failed: ${e.message}") }
    }

    private fun beepAndVibrate(ctx: Context) {
        try {
            val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else @Suppress("DEPRECATION") v.vibrate(500)
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.playSoundEffect(AudioManager.FX_KEY_CLICK)
            Log.i(TAG, "beepAndVibrate done")
        } catch (e: Exception) { Log.e(TAG, "beepAndVibrate failed: ${e.message}") }
    }
}