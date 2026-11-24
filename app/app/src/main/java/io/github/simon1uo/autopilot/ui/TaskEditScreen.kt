package io.github.simon1uo.autopilot.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.simon1uo.autopilot.model.Task

data class AppEntry(val label: String, val pkg: String, val activity: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(initial: Task, onSave: (Task) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var days by remember { mutableStateOf(initial.daysOfWeek) }
    var interval by remember { mutableStateOf(initial.intervalMinutes) }
    var pkg by remember { mutableStateOf(initial.packageName) }
    var activity by remember { mutableStateOf(initial.activityName) }
    var label by remember { mutableStateOf(initial.appLabel) }
    var wake by remember { mutableStateOf(initial.wakeScreen) }
    var launch by remember { mutableStateOf(initial.launchApp) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial.packageName.isEmpty()) "新建任务" else "编辑任务") },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = {
                        onSave(initial.copy(
                            name = name, hour = hour, minute = minute, daysOfWeek = days,
                            intervalMinutes = interval, packageName = pkg, activityName = activity,
                            appLabel = label, wakeScreen = wake, launchApp = launch,
                        ))
                    }) { Icon(Icons.Default.Check, "保存") }
                },
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("任务名称") }, modifier = Modifier.fillMaxWidth())

            // 时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Text("时间: %02d:%02d".format(hour, minute))
                }
            }
            OutlinedTextField(
                value = if (interval == 0) "" else interval.toString(),
                onValueChange = { interval = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 },
                label = { Text("重复间隔(分钟，0=不启用)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            if (interval == 0) {
                Text("星期", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val labels = listOf("一","二","三","四","五","六","日")
                    labels.forEachIndexed { i, lb ->
                        FilterChip(
                            selected = days[i],
                            onClick = { days = days.toMutableList().also { it[i] = !it[i] } },
                            label = { Text(lb) },
                        )
                    }
                }
            }

            // 目标 App
            OutlinedButton(onClick = { showAppPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (label.isBlank()) "选择目标 App" else "目标: $label")
            }

            Text("到点时执行动作", fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = wake, onCheckedChange = { wake = it }); Text("唤醒屏幕")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = launch, onCheckedChange = { launch = it }); Text("打开 App")
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(hour, minute, onConfirm = { h, m -> hour = h; minute = m; showTimePicker = false }, onDismiss = { showTimePicker = false })
    }
    if (showAppPicker) {
        AppPickerDialog(onPick = { e -> label = e.label; pkg = e.pkg; activity = e.activity; showAppPicker = false }, onDismiss = { showAppPicker = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(hour: Int, minute: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("确定") }
                }
            }
        }
    }
}

@Composable
fun AppPickerDialog(onPick: (AppEntry) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val apps = remember {
        val pm = ctx.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
            val ai = ri.activityInfo ?: return@mapNotNull null
            AppEntry(ri.loadLabel(pm).toString(), ai.packageName, ai.name)
        }.filter { it.pkg != ctx.packageName }.sortedBy { it.label.lowercase() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            Column(Modifier.padding(12.dp)) {
                Text("选择 App", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(apps) { e ->
                        ListItem(
                            headlineContent = { Text(e.label) },
                            supportingContent = { Text(e.pkg, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.clickable { onPick(e) },
                        )
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("取消") }
            }
        }
    }
}