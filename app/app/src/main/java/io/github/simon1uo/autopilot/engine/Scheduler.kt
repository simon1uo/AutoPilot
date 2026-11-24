package io.github.simon1uo.autopilot.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.simon1uo.autopilot.model.Task
import java.util.Calendar

/**
 * 定时调度器：
 * - 计算下一次触发时刻(毫秒)。
 * - 用 setAlarmClock(可在 Doze 下唤醒) 或 setExactAndAllowWhileIdle。
 */
object Scheduler {

    private fun alarmManager(ctx: Context) =
        ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** pendingIntent 的 requestCode 用 task.id 的低位，保证同一任务可取消。 */
    private fun pendingIntent(ctx: Context, task: Task, flags: Int): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id)
        }
        return PendingIntent.getBroadcast(
            ctx,
            task.id.toInt(),
            intent,
            flags or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun canScheduleExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager(ctx).canScheduleExactAlarms()
    }

    /** 调度一个任务（若 enabled 且有效）。返回下次触发时间 ms，或 null。 */
    fun schedule(ctx: Context, task: Task): Long? {
        if (!task.enabled) { cancel(ctx, task); return null }
        val next = nextTriggerMillis(task) ?: run { cancel(ctx, task); return null }
        val pi = pendingIntent(ctx, task, PendingIntent.FLAG_IMMUTABLE)

        try {
            if (canScheduleExact(ctx)) {
                // 优先 setAlarmClock，可在待机/Doze 中准时唤醒
                val info = AlarmManager.AlarmClockInfo(next, null)
                alarmManager(ctx).setAlarmClock(info, pi)
            } else {
                // 无精确闹钟权限：退化为 setAndAllowWhileIdle（可能不准）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
                } else {
                    alarmManager(ctx).set(AlarmManager.RTC_WAKEUP, next, pi)
                }
            }
        } catch (e: SecurityException) {
            alarmManager(ctx).set(AlarmManager.RTC_WAKEUP, next, pi)
        }
        return next
    }

    fun cancel(ctx: Context, task: Task) {
        alarmManager(ctx).cancel(pendingIntent(ctx, task, PendingIntent.FLAG_IMMUTABLE))
    }

    /** 计算“最近一次未来触发时刻”。 */
    fun nextTriggerMillis(task: Task): Long? {
        val now = Calendar.getInstance()
        val candidates = mutableListOf<Long>()

        if (task.intervalMinutes > 0) {
            // 间隔模式：从现在起 intervalMinutes 之后（简单可靠）
            candidates.add(now.timeInMillis + task.intervalMinutes * 60_000L)
        } else {
            // 每天固定 HH:mm + 选中的星期
            val curDow = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 转成 0=周一
            for (d in 0 until 7) {
                if (!task.daysOfWeek[d]) continue
                val c = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, task.hour)
                    set(Calendar.MINUTE, task.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                var delta = (d - curDow + 7) % 7
                if (delta == 0 && c.timeInMillis <= now.timeInMillis) delta = 7
                c.add(Calendar.DAY_OF_YEAR, delta)
                candidates.add(c.timeInMillis)
            }
        }
        return candidates.minOrNull()
    }
}