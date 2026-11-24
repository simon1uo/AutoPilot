package io.github.simon1uo.autopilot.control

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.simon1uo.autopilot.data.TaskRepository
import io.github.simon1uo.autopilot.engine.CoreActions
import io.github.simon1uo.autopilot.engine.Scheduler
import io.github.simon1uo.autopilot.model.Action
import io.github.simon1uo.autopilot.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ADB 直通控制。示例：
 *   adb shell am broadcast -a io.github.simon1uo.autopilot.CONTROL \
 *     -n io.github.simon1uo.autopilot/.control.ControlReceiver --es cmd list
 *   adb ... --es cmd trigger --el id 123
 *   adb ... --es cmd create --es name "起床" --ei hour 7 --ei minute 0 \\
 *       --es pkg com.android.chrome
 */
class ControlReceiver : BroadcastReceiver() {
    companion object { const val TAG = "AutoPilot/Ctl"; const val ACTION = "io.github.simon1uo.autopilot.CONTROL" }

    override fun onReceive(ctx: Context, intent: Intent) {
        val cmd = intent.getStringExtra("cmd") ?: "list"
        val repo = TaskRepository(ctx)
        val list = repo.load()
        Log.i(TAG, "control cmd=$cmd, tasks=${list.size}")
        when (cmd) {
            "list" -> list.forEach { t -> Log.i(TAG, "  [${t.id}] ${t.name} en=${t.enabled} ${t.timeText} actions=${t.actions.size}") }
            "trigger" -> {
                val id = intent.getLongExtra("id", -1L)
                list.firstOrNull { it.id == id }?.let { task ->
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.Default).launch {
                        try { CoreActions.runTask(ctx.applicationContext, task) } finally { pending.finish() }
                    }
                    Log.i(TAG, "triggered ${task.name}")
                }
            }
            "create" -> {
                val pkg = intent.getStringExtra("pkg") ?: ""
                val label = intent.getStringExtra("label") ?: pkg
                val acts = mutableListOf<Action>(Action.wake())
                if (pkg.isNotBlank()) acts.add(Action.launch(pkg, intent.getStringExtra("activity") ?: "", label))
                val t = Task(
                    name = intent.getStringExtra("name") ?: "ADB任务",
                    hour = intent.getIntExtra("hour", 7),
                    minute = intent.getIntExtra("minute", 0),
                    intervalMinutes = intent.getIntExtra("interval", 0),
                    actions = acts,
                )
                list.add(t); repo.save(list); Scheduler.schedule(ctx, t)
                Log.i(TAG, "created task ${t.id}")
            }
            "http-start" -> { HttpServerService.start(ctx); Log.i(TAG, "http starting :8080") }
            "http-stop" -> { HttpServerService.stop(ctx); Log.i(TAG, "http stopping") }
            "delete" -> {
                val id = intent.getLongExtra("id", -1L)
                list.firstOrNull { it.id == id }?.let { Scheduler.cancel(ctx, it) }
                repo.save(list.filter { it.id != id }); Log.i(TAG, "deleted $id")
            }
        }
    }
}
