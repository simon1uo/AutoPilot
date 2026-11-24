package io.github.simon1uo.autopilot.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.simon1uo.autopilot.engine.Scheduler
import io.github.simon1uo.autopilot.model.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(vm: TaskViewModel, onNew: () -> Unit, onEdit: (Task) -> Unit) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AutoPilot", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("新建任务") },
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (!Scheduler.canScheduleExact(ctx)) {
                ExactAlarmBanner(ctx)
            }
            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有任务，点右下角新建一个吧 👇", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            nextText = if (task.enabled) vm.nextTimeText(task) else "已停用",
                            onToggle = { vm.toggle(task) },
                            onClick = { onEdit(task) },
                            onTrigger = { vm.triggerNow(task) },
                            onDelete = { vm.delete(task) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExactAlarmBanner(ctx: Context) {
    Card(
        Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("需要『精确闹钟』权限才能准时唤醒", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }) { Text("去授权") }
        }
    }
}

@Composable
fun TaskCard(
    task: Task, nextText: String, onToggle: () -> Unit, onClick: () -> Unit,
    onTrigger: () -> Unit, onDelete: () -> Unit,
) {
    Card(onClick = onClick, Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                val timeText = if (task.intervalMinutes > 0) "每 ${task.intervalMinutes} 分钟"
                    else String.format("%02d:%02d", task.hour, task.minute)
                Text(timeText + " → ${task.appLabel.ifBlank { "未选App" }}",
                    style = MaterialTheme.typography.bodySmall)
                Text("下次: $nextText", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onTrigger) { Icon(Icons.Default.PlayArrow, "立即触发") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除") }
            Switch(checked = task.enabled, onCheckedChange = { onToggle() })
        }
    }
}
