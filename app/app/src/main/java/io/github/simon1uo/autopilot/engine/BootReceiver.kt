package io.github.simon1uo.autopilot.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.simon1uo.autopilot.data.TaskRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val repo = TaskRepository(ctx)
        repo.load().filter { it.enabled }.forEach { Scheduler.schedule(ctx, it) }
    }
}
