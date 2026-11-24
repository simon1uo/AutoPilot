package io.github.simon1uo.autopilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.simon1uo.autopilot.model.*
import io.github.simon1uo.autopilot.ui.theme.*

data class AppEntry(val label: String, val pkg: String, val activity: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(initial: Task, onSave: (Task) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var days by remember { mutableStateOf(initial.daysOfWeek) }
    var interval by remember { mutableStateOf(initial.intervalMinutes) }
    var actions by remember { mutableStateOf(initial.actions.toMutableList()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(-1) }

    fun move(from: Int, to: Int) {
        if (to in actions.indices && from in actions.indices) {
            val item = actions.removeAt(from); actions.add(to, item)
        }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Top bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, "返回", tint = TextPrimary) }
                Text(if (initial.actions.size <= 2 && initial.name == "新任务") "新建任务" else "编辑任务",
                    color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    onSave(initial.copy(
                        name = name.ifBlank { "新任务" }, hour = hour, minute = minute,
                        daysOfWeek = days, intervalMinutes = interval, actions = actions.toList(),
                    ))
                }) { Icon(Icons.Default.Check, "保存", tint = NeonGreen) }
            }
            Hairline()

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { FieldLabel("任务名称") }
                item {
                    NeonTextField(name, { name = it }, "给任务起个名字")
                }

                item { FieldLabel("触发时间") }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showTimePicker = true }, shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen)) {
                            Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(String.format("%02d:%02d", hour, minute), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                }
                item {
                    NeonTextField(
                        value = if (interval == 0) "" else interval.toString(),
                        onChange = { interval = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 },
                        placeholder = "重复间隔（分钟，0 = 不启用）",
                        keyboardType = KeyboardType.Number,
                    )
                }

                if (interval == 0) {
                    item { FieldLabel("星期") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val labels = listOf("一","二","三","四","五","六","日")
                            labels.forEachIndexed { i, lb ->
                                DayChip(lb, days[i]) { days = days.toMutableList().also { it[i] = !it[i] } }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FieldLabel("动作序列")
                        Spacer(Modifier.weight(1f))
                        Text("${actions.size} 步 · 按顺序执行", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                    }
                }

                itemsIndexedHelper(actions, { a, i ->
                    ActionRow(
                        action = a, index = i, total = actions.size,
                        onEdit = { editingIndex = i },
                        onDelete = { actions.removeAt(i) },
                        onMoveUp = { move(i, i - 1) },
                        onMoveDown = { move(i, i + 1) },
                    )
                })

                item {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showAddSheet = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("添加动作", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }

    if (showTimePicker) TimePickerDialog(hour, minute, { h, m -> hour = h; minute = m; showTimePicker = false }, { showTimePicker = false })
    if (showAddSheet) AddActionSheet(
        onPick = { type -> actions.add(defaultAction(type)); showAddSheet = false },
        onDismiss = { showAddSheet = false },
    )
    if (editingIndex in actions.indices) {
        ActionConfigDialog(
            action = actions[editingIndex],
            onSave = { actions[editingIndex] = it; editingIndex = -1 },
            onDismiss = { editingIndex = -1 },
        )
    }
}

private fun defaultAction(type: ActionType): Action = when (type) {
    ActionType.WAKE_SCREEN -> Action.wake()
    ActionType.LAUNCH_APP -> Action.launch("", "", "")
    ActionType.WAIT -> Action.wait(5)
    ActionType.SET_VOLUME -> Action.volume(50)
    ActionType.TOGGLE_WIFI -> Action.wifi(true)
    ActionType.TOGGLE_BLUETOOTH -> Action.bluetooth(true)
    ActionType.NOTIFY -> Action.notify()
    ActionType.BEEP_VIBRATE -> Action.beep()
}

/** 带 IndexedItems 的辅助（LazyListScope 扩展） */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedHelper(
    list: List<Action>, row: @Composable (Action, Int) -> Unit,
) {
    items(list.size) { i -> row(list[i], i) }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, color = TextSecondary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun NeonTextField(value: String, onChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        placeholder = { Text(placeholder, color = TextTertiary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = InkElevated, unfocusedContainerColor = InkElevated,
            focusedBorderColor = NeonGreen, unfocusedBorderColor = Hairline,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
            cursorColor = NeonGreen,
        ),
    )
}

@Composable
fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) NeonGreen else InkElevated2)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) Ink else TextSecondary, fontWeight = FontWeight.Bold) }
}

@Composable
fun ActionRow(action: Action, index: Int, total: Int, onEdit: () -> Unit, onDelete: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(InkElevated).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(InkElevated2), contentAlignment = Alignment.Center) {
            Icon(action.type.iconVector(), null, tint = NeonGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).clickable { onEdit() }) {
            Text(action.type.label, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(action.summary(), color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        }
        // 排序
        Column {
            IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.KeyboardArrowUp, "上移", tint = if (index > 0) TextSecondary else Hairline)
            }
            IconButton(onClick = onMoveDown, enabled = index < total - 1, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.KeyboardArrowDown, "下移", tint = if (index < total - 1) TextSecondary else Hairline)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Close, "删除", tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionSheet(onPick: (ActionType) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = InkElevated) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("添加动作", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            ActionType.entries.forEach { type ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onPick(type) }.padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(InkElevated2), contentAlignment = Alignment.Center) {
                        Icon(type.iconVector(), null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(type.label, color = TextPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ActionConfigDialog(action: Action, onSave: (Action) -> Unit, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf(action) }
    var showAppPicker by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = InkElevated), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(action.type.label + " 设置", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                when (action.type) {
                    ActionType.WAIT -> NeonTextField(current.waitSeconds.toString(),
                        { current = current.copy(waitSeconds = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0) },
                        "等待秒数", KeyboardType.Number)
                    ActionType.SET_VOLUME -> {
                        Text("音量：${current.volumePercent}%", color = TextSecondary)
                        Slider(
                            value = current.volumePercent.toFloat(), onValueChange = { current = current.copy(volumePercent = it.toInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen, inactiveTrackColor = InkElevated2),
                        )
                    }
                    ActionType.TOGGLE_WIFI, ActionType.TOGGLE_BLUETOOTH -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleChip("打开", current.turnOn) { current = current.copy(turnOn = true) }
                            ToggleChip("关闭", !current.turnOn) { current = current.copy(turnOn = false) }
                        }
                    }
                    ActionType.NOTIFY -> NeonTextField(current.message, { current = current.copy(message = it) }, "通知内容")
                    ActionType.LAUNCH_APP -> {
                        OutlinedButton(onClick = { showAppPicker = true }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen)) {
                            Text(if (current.appLabel.isBlank()) "选择 App" else "目标：${current.appLabel}")
                        }
                    }
                    else -> Text("此动作无需额外配置", color = TextTertiary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
                    TextButton(onClick = { onSave(current) }) { Text("确定", color = NeonGreen, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    if (showAppPicker) AppPickerDialog(
        onPick = { e -> current = current.copy(packageName = e.pkg, activityName = e.activity, appLabel = e.label); showAppPicker = false },
        onDismiss = { showAppPicker = false },
    )
}

@Composable
fun ToggleChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) NeonGreen else InkElevated2)
        .clickable { onClick() }.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(text, color = if (selected) Ink else TextSecondary, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(hour: Int, minute: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = InkElevated), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state, colors = TimePickerDefaults.colors(
                    clockDialColor = InkElevated2, clockDialSelectedContentColor = Ink,
                    selectorColor = NeonGreen, containerColor = InkElevated, timeSelectorSelectedContainerColor = NeonGreen,
                    timeSelectorSelectedContentColor = Ink, timeSelectorUnselectedContainerColor = InkElevated2,
                    timeSelectorUnselectedContentColor = TextPrimary,
                ))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("确定", color = NeonGreen, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun AppPickerDialog(onPick: (AppEntry) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val apps = remember {
        val pm = ctx.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
            val ai = ri.activityInfo ?: return@mapNotNull null
            AppEntry(ri.loadLabel(pm).toString(), ai.packageName, ai.name)
        }.filter { it.pkg != ctx.packageName }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    }
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, true) || it.pkg.contains(query, true) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().fillMaxHeight(0.85f), colors = CardDefaults.cardColors(containerColor = InkElevated), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("选择 App", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                NeonTextField(query, { query = it }, "搜索应用")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(filtered) { e ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(e) }.padding(vertical = 10.dp, horizontal = 6.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(e.label, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text(e.pkg, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}