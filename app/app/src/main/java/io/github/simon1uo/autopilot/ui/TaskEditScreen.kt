package io.github.simon1uo.autopilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import io.github.simon1uo.autopilot.model.*
import io.github.simon1uo.autopilot.ui.theme.*

data class AppEntry(val label: String, val pkg: String, val activity: String)

/** 动作列表行的稳定标识（增删时保持身份，避免 LazyColumn 复用错位） */
data class ActionItem(val uid: Long, val action: Action)

private var uidSeq = 0L
private fun nextUid(): Long = ++uidSeq

private fun List<Action>.toItems(): MutableList<ActionItem> = map { ActionItem(nextUid(), it) }.toMutableList()
private fun List<ActionItem>.toActions(): List<Action> = map { it.action }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(initial: Task, onSave: (Task) -> Unit, onCancel: () -> Unit) {
    val c = LocalAutoColors.current
    var name by remember { mutableStateOf(initial.name) }
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var days by remember { mutableStateOf(initial.daysOfWeek) }
    var interval by remember { mutableStateOf(initial.intervalMinutes) }
    val items = remember { initial.actions.toItems() }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingUid by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()

    // 拖拽状态（仅作用于动作列表的拖拽手柄）
    var draggingUid by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val rowHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }

    fun saveTask() {
        // 校验：启动 App 动作必须选了目标 App
        val bad = items.firstOrNull { it.action.type == ActionType.LAUNCH_APP && it.action.packageName.isBlank() }
        if (bad != null) { editingUid = bad.uid; return }
        onSave(initial.copy(
            name = name.ifBlank { "新任务" }, hour = hour, minute = minute,
            daysOfWeek = days, intervalMinutes = interval, actions = items.toActions(),
        ))
    }

    /** 添加动作：需要配置的类型自动弹出配置框；无需配置的直接加 */
    fun addAction(type: ActionType) {
        val item = ActionItem(nextUid(), defaultAction(type))
        items.add(item)
        if (type.needsConfig) editingUid = item.uid
    }

    fun moveByUid(fromIndex: Int, toIndex: Int) {
        if (toIndex in items.indices && fromIndex in items.indices && fromIndex != toIndex) {
            val it = items.removeAt(fromIndex)
            items.add(toIndex, it)
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Top bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, "返回", tint = c.textPrimary) }
                Text(if (initial.actions.size <= 2 && initial.name == "新任务") "新建任务" else "编辑任务",
                    color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { saveTask() }) { Icon(Icons.Default.Check, "保存", tint = c.accent) }
            }
            Hairline()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { FieldLabel("任务名称") }
                item { NeonTextField(name, { name = it }, "给任务起个名字") }

                item { FieldLabel("触发时间") }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showTimePicker = true }, shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = c.accent)) {
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
                        onChange = { interval = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 },
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
                        Text("${items.size} 步 · 长按手柄拖动排序", color = c.textTertiary, style = MaterialTheme.typography.labelSmall)
                    }
                }

                itemsIndexed(items, key = { _, it -> it.uid }) { i, item ->
                    val isDragging = draggingUid == item.uid
                    ActionRow(
                        action = item.action,
                        index = i,
                        total = items.size,
                        isDragging = isDragging,
                        dragOffset = if (isDragging) dragOffset else 0f,
                        onEdit = { editingUid = item.uid },
                        onDelete = { items.removeAt(i) },
                        dragHandleModifier = Modifier.pointerInput(item.uid) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggingUid = item.uid; dragOffset = 0f },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val curIdx = items.indexOfFirst { it.uid == item.uid }
                                    if (dragOffset > rowHeightPx / 2 && curIdx < items.size - 1) {
                                        moveByUid(curIdx, curIdx + 1); dragOffset -= rowHeightPx
                                    } else if (dragOffset < -rowHeightPx / 2 && curIdx > 0) {
                                        moveByUid(curIdx, curIdx - 1); dragOffset += rowHeightPx
                                    }
                                },
                                onDragEnd = { draggingUid = null; dragOffset = 0f },
                                onDragCancel = { draggingUid = null; dragOffset = 0f },
                            )
                        },
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { if (isDragging) { translationY = dragOffset; shadowElevation = 16f } },
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showAddSheet = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = c.accent),
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
        onPick = { type -> addAction(type); showAddSheet = false },
        onDismiss = { showAddSheet = false },
    )
    val editingItem = items.firstOrNull { it.uid == editingUid }
    if (editingItem != null) {
        ActionConfigDialog(
            action = editingItem.action,
            onSave = { updated ->
                val idx = items.indexOfFirst { it.uid == editingItem.uid }
                if (idx >= 0) items[idx] = editingItem.copy(action = updated)
                editingUid = null
            },
            onDismiss = { editingUid = null },
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

/** 需要进入配置的动作类型 */
private val ActionType.needsConfig: Boolean
    get() = when (this) {
        ActionType.WAKE_SCREEN, ActionType.BEEP_VIBRATE -> false
        else -> true
    }

@Composable
fun FieldLabel(text: String) {
    Text(text, color = LocalAutoColors.current.textSecondary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun NeonTextField(value: String, onChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text) {
    val c = LocalAutoColors.current
    OutlinedTextField(
        value = value, onValueChange = onChange,
        placeholder = { Text(placeholder, color = c.textTertiary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.card, unfocusedContainerColor = c.card,
            focusedBorderColor = c.accent, unfocusedBorderColor = c.hairline,
            focusedTextColor = c.textPrimary, unfocusedTextColor = c.textPrimary,
            cursorColor = c.accent,
        ),
    )
}

@Composable
fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAutoColors.current
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.accent else c.cardHigh)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) c.onAccent else c.textSecondary, fontWeight = FontWeight.Bold) }
}

@Composable
fun ActionRow(
    action: Action, index: Int, total: Int,
    isDragging: Boolean, dragOffset: Float,
    onEdit: () -> Unit, onDelete: () -> Unit,
    dragHandleModifier: Modifier, modifier: Modifier = Modifier,
) {
    val c = LocalAutoColors.current
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 拖拽手柄
        Box(
            dragHandleModifier.size(30.dp),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.DragIndicator, "拖动排序", tint = c.textTertiary, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.cardHigh), contentAlignment = Alignment.Center) {
            Icon(action.type.iconVector(), null, tint = c.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        // 整行可点编辑
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { onEdit() }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(action.type.label, color = c.textPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(action.summary(), color = c.textTertiary, style = MaterialTheme.typography.labelSmall)
            }
            // 空目标提示
            if (action.type == ActionType.LAUNCH_APP && action.packageName.isBlank()) {
                Pill("未选 App", bg = c.danger.copy(alpha = 0.15f), fg = c.danger)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Close, "删除", tint = c.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionSheet(onPick: (ActionType) -> Unit, onDismiss: () -> Unit) {
    val c = LocalAutoColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.card) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("添加动作", color = c.textPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            ActionType.entries.forEach { type ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onPick(type) }.padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(c.cardHigh), contentAlignment = Alignment.Center) {
                        Icon(type.iconVector(), null, tint = c.accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(type.label, color = c.textPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ActionConfigDialog(action: Action, onSave: (Action) -> Unit, onDismiss: () -> Unit) {
    val c = LocalAutoColors.current
    var current by remember { mutableStateOf(action) }
    var showAppPicker by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = c.card), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(action.type.label + " 设置", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                when (action.type) {
                    ActionType.WAIT -> NeonTextField(current.waitSeconds.toString(),
                        { current = current.copy(waitSeconds = it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0) },
                        "等待秒数", KeyboardType.Number)
                    ActionType.SET_VOLUME -> {
                        Text("音量：${current.volumePercent}%", color = c.textSecondary)
                        Slider(
                            value = current.volumePercent.toFloat(), onValueChange = { current = current.copy(volumePercent = it.toInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.cardHigh),
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
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = c.accent)) {
                            Text(if (current.appLabel.isBlank()) "选择 App" else "目标：${current.appLabel}")
                        }
                    }
                    else -> Text("此动作无需额外配置", color = c.textTertiary)
                }
                if (action.type == ActionType.LAUNCH_APP && current.packageName.isBlank()) {
                    Text("请先选择一个要启动的 App", color = c.danger, style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = c.textSecondary) }
                    TextButton(
                        onClick = { onSave(current) },
                        enabled = !(action.type == ActionType.LAUNCH_APP && current.packageName.isBlank()),
                    ) { Text("确定", color = c.accent, fontWeight = FontWeight.Bold) }
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
    val c = LocalAutoColors.current
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) c.accent else c.cardHigh)
        .clickable { onClick() }.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(text, color = if (selected) c.onAccent else c.textSecondary, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(hour: Int, minute: Int, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val c = LocalAutoColors.current
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = c.card), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state, colors = TimePickerDefaults.colors(
                    clockDialColor = c.cardHigh, clockDialSelectedContentColor = c.onAccent,
                    selectorColor = c.accent, containerColor = c.card, timeSelectorSelectedContainerColor = c.accent,
                    timeSelectorSelectedContentColor = c.onAccent, timeSelectorUnselectedContainerColor = c.cardHigh,
                    timeSelectorUnselectedContentColor = c.textPrimary,
                ))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = c.textSecondary) }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("确定", color = c.accent, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun AppPickerDialog(onPick: (AppEntry) -> Unit, onDismiss: () -> Unit) {
    val c = LocalAutoColors.current
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
        Card(Modifier.fillMaxWidth().fillMaxHeight(0.85f), colors = CardDefaults.cardColors(containerColor = c.card), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("选择 App", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                NeonTextField(query, { query = it }, "搜索应用")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    itemsIndexed(filtered) { _, e ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(e) }.padding(vertical = 10.dp, horizontal = 6.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(e.label, color = c.textPrimary, fontWeight = FontWeight.Medium)
                                Text(e.pkg, color = c.textTertiary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}