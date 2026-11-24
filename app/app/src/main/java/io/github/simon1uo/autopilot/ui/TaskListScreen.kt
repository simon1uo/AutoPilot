package io.github.simon1uo.autopilot.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.simon1uo.autopilot.engine.Scheduler
import io.github.simon1uo.autopilot.model.Task
import io.github.simon1uo.autopilot.model.summary
import io.github.simon1uo.autopilot.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(vm: TaskViewModel, onNew: () -> Unit, onEdit: (Task) -> Unit) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("AutoPilot", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (tasks.isEmpty()) "还没有任务" else "${tasks.size} 个任务",
                        color = TextTertiary, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (!Scheduler.canScheduleExact(ctx)) ExactAlarmBanner(ctx)

            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Bolt, null, tint = NeonGreen.copy(alpha = 0.7f), modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("开始创建第一个自动化任务", color = TextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("右下角 + 新建任务", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
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

        // FAB
        ExtendedFloatingActionButton(
            onClick = onNew,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).navigationBarsPadding(),
            containerColor = NeonGreen,
            contentColor = Ink,
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Default.Add, null, tint = Ink) },
            text = { Text("新建任务", fontWeight = FontWeight.Bold, color = Ink) },
        )
    }
}

@Composable
fun ExactAlarmBanner(ctx: Context) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp)).background(InkElevated2).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Schedule, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("需授予精确闹钟权限才能准时唤醒", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        TextButton(onClick = {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }) { Text("授权", color = NeonGreen, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun TaskCard(
    task: Task, nextText: String, onToggle: () -> Unit, onClick: () -> Unit,
    onTrigger: () -> Unit, onDelete: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(InkElevated)
            .clickable { onClick() }.padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(task.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.timeText, color = NeonGreen, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(if (task.enabled) "· 下次 $nextText" else "· 已停用", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
            NeonSwitch(checked = task.enabled, onCheckedChange = { onToggle() })
        }
        Spacer(Modifier.height(12.dp))
        // 动作摘要
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            task.actions.take(3).forEach { a -> Pill(a.type.label) }
            if (task.actions.size > 3) Pill("+${task.actions.size - 3}")
        }
        Spacer(Modifier.height(14.dp))
        Hairline()
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTrigger) {
                Icon(Icons.Outlined.PlayArrow, "立即执行", tint = NeonGreen, modifier = Modifier.size(22.dp))
            }
            Text("立即执行", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, "删除", tint = TextTertiary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun NeonSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Ink,
            checkedTrackColor = NeonGreen,
            uncheckedThumbColor = TextTertiary,
            uncheckedTrackColor = InkElevated2,
            uncheckedBorderColor = Hairline,
        ),
    )
}
