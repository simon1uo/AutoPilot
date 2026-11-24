package io.github.simon1uo.autopilot.engine

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

/**
 * 两个核心动作：
 *  - wakeScreen()  唤醒屏幕
 *  - launchApp()   启动目标应用
 */
object CoreActions {
    private const val TAG = "AutoPilot/CoreActions"

    /**
     * 通过 WakeLock 点亮屏幕：
     *   newWakeLock(0x3000000A).acquire(1000L)
     *   0x0000000A SCREEN_BRIGHT_WAKE_LOCK
     * | 0x10000000 ACQUIRE_CAUSES_WAKEUP
     * | 0x20000000 ON_AFTER_RELEASE
     */
    @Suppress("DEPRECATION")
    fun wakeScreen(ctx: Context) {
        try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isScreenOn) {
                val wl = pm.newWakeLock(0x3000000A, "autopilot:wake").apply { setReferenceCounted(false) }
                wl.acquire(1000L)
                Log.i(TAG, "wakeScreen: acquired WAKE_LOCK 0x3000000A")
            } else {
                Log.i(TAG, "wakeScreen: screen already on")
            }
        } catch (e: Exception) {
            Log.e(TAG, "wakeScreen failed: ${e.message}")
        }
    }

    /**
     * 通过显式 Component 启动目标应用：
     *   Intent(ACTION_MAIN).setClassName(pkg, activity).addFlags(NEW_TASK).startActivity
     */
    fun launchApp(ctx: Context, pkg: String, activity: String): Boolean {
        if (pkg.isBlank()) { Log.w(TAG, "launchApp: empty package"); return false }
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName(pkg, activity.ifBlank { return fallbackLaunch(ctx, pkg) })
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            Log.i(TAG, "launchApp: $pkg/$activity")
            true
        } catch (e: Exception) {
            Log.e(TAG, "launchApp exact failed: ${e.message}, trying fallback")
            fallbackLaunch(ctx, pkg)
        }
    }

    private fun fallbackLaunch(ctx: Context, pkg: String): Boolean = try {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(i); true }
        else { Log.e(TAG, "fallbackLaunch: no launch intent for $pkg"); false }
    } catch (e: Exception) {
        Log.e(TAG, "fallbackLaunch failed: ${e.message}"); false
    }

    /** 执行一个任务的一条龙动作。 */
    fun runTask(ctx: Context, task: io.github.simon1uo.autopilot.model.Task) {
        Log.i(TAG, "runTask: ${task.name} wake=${task.wakeScreen} launch=${task.launchApp}")
        if (task.wakeScreen) wakeScreen(ctx)
        if (task.launchApp) {
            // 亮屏后稍微延迟再启动，效果更稳
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                launchApp(ctx, task.packageName, task.activityName)
            }, if (task.wakeScreen) 400L else 0L)
        }
    }
}