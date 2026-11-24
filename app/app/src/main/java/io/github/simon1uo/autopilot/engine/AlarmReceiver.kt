package io.github.simon1uo.autopilot.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.simon1uo.autopilot.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        // 用 goAsync 保证动作序列（可能含等待）能执行完
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                CoreActions.runTask(ctx.applicationContext, task)
                Scheduler.schedule(ctx.applicationContext, task)
            } finally { pending.finish() }
        }
    }
}
