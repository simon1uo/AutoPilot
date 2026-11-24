package io.github.simon1uo.autopilot.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.simon1uo.autopilot.data.TaskRepository

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_FIRE = "io.github.simon1uo.autopilot.ALARM_FIRE"
        const val EXTRA_TASK_ID = "task_id"
    }
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val repo = TaskRepository(ctx)
        val task = repo.load().firstOrNull { it.id == id } ?: return
        if (!task.enabled) return
        CoreActions.runTask(ctx, task)
        // 重新排下一次
        Scheduler.schedule(ctx, task)
    }
}
