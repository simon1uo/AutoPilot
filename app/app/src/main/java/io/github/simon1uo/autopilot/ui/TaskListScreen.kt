package io.github.simon1uo.autopilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.simon1uo.autopilot.engine.Permissions
import io.github.simon1uo.autopilot.model.Task
import io.github.simon1uo.autopilot.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    vm: TaskViewModel,
    onNew: () -> Unit,
    onEdit: (Task) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    // 搜索 & 筛选 & 批量
    var query by remember { mutableStateOf("") }
    var showOnlyEnabled by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }
    var refreshing by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    val filtered = remember(tasks, query, showOnlyEnabled) {
        tasks.filter { t ->
            (query.isBlank() || t.name.contains(query, true) || t.actions.any { it.appLabel.contains(query, true) }) &&
            (!showOnlyEnabled || t.enabled)
        }
    }

    // 下拉刷新
    fun doRefresh() {
        refreshing = true
        vm.reload()
        refreshing = false
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // ===== Header =====
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AutoPilot", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (tasks.isEmpty()) "还没有任务" else "共 ${tasks.size} 个任务" + if (filtered.size != tasks.size) " · 筛选出 ${filtered.size}" else "",
                        color = TextTertiary, style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onOpenHistory) { Icon(Icons.Outlined.History, "执行历史", tint = TextSecondary) }
                IconButton(onClick = {
                    selectionMode = !selectionMode
                    if (!selectionMode) selected.clear()
                }) {
                    Icon(
                        if (selectionMode) Icons.Outlined.Close else Icons.Outlined.Checklist,
                        "选择", tint = if (selectionMode) NeonGreen else TextSecondary,
                    )
                }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, "设置", tint = TextSecondary) }
            }

            // ===== Search bar =====
            if (tasks.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("搜索任务或应用", color = TextTertiary, style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextTertiary, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = InkElevated, unfocusedContainerColor = InkElevated,
                            focusedBorderColor = NeonGreen, unfocusedBorderColor = Hairline,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = NeonGreen,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = showOnlyEnabled, onClick = { showOnlyEnabled = !showOnlyEnabled },
                        label = { Text("仅启用", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen, selectedLabelColor = Ink,
                            containerColor = InkElevated, labelColor = TextSecondary,
                        ),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (!Permissions.isGranted(ctx, Permissions.Perm.EXACT_ALARM) && Permissions.isRelevant(ctx, Permissions.Perm.EXACT_ALARM))
                ExactAlarmBanner(ctx)

            // ===== 批量操作条 =====
            if (selectionMode) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp)).background(InkElevated2).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("已选 ${selected.size}", color = NeonGreen, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.setEnabledMany(selected.toSet(), true); selected.clear() }, enabled = selected.isNotEmpty()) { Text("启用", color = NeonGreen) }
                    TextButton(onClick = { vm.setEnabledMany(selected.toSet(), false); selected.clear() }, enabled = selected.isNotEmpty()) { Text("停用", color = TextSecondary) }
                    TextButton(onClick = { vm.deleteMany(selected.toSet()); selected.clear(); selectionMode = false }) { Text("删除", color = DangerRed) }
                }
            }

            // ===== List =====
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
                PullToRefreshBox(
                    isRefreshing = refreshing, onRefresh = { doRefresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                    ) {
                        items(filtered, key = { it.id }) { task ->
                            val shownIndex = filtered.indexOf(task)
                            val isDragging = dragIndex == shownIndex && dragIndex >= 0
                            TaskCard(
                                task = task,
                                nextText = if (task.enabled) vm.nextTimeText(task) else "已停用",
                                selectionMode = selectionMode,
                                selected = selected.contains(task.id),
                                onToggleSelect = {
                                    if (selected.contains(task.id)) selected.remove(task.id) else selected.add(task.id)
                                },
                                onToggle = { vm.toggle(task) },
                                onClick = { if (selectionMode) { if (selected.contains(task.id)) selected.remove(task.id) else selected.add(task.id) } else onEdit(task) },
                                onTrigger = { vm.triggerNow(task) },
                                onDelete = { vm.delete(task) },
                                onClone = { vm.clone(task) },
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer { if (isDragging) { translationY = dragOffset; shadowElevation = 12f } }
                                    .then(
                                        if (!selectionMode && query.isBlank() && !showOnlyEnabled) Modifier.pointerInput(filtered.size) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { if (shownIndex >= 0) { dragIndex = shownIndex; dragOffset = 0f } },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    dragOffset += amount.y
                                                    val rowHeightPx = 150.dp.toPx()  // 约卡片高度+间距
                                                    if (dragOffset > rowHeightPx / 2 && dragIndex < filtered.size - 1) {
                                                        vm.reorder(dragIndex, dragIndex + 1); dragIndex++; dragOffset -= rowHeightPx
                                                    } else if (dragOffset < -rowHeightPx / 2 && dragIndex > 0) {
                                                        vm.reorder(dragIndex, dragIndex - 1); dragIndex--; dragOffset += rowHeightPx
                                                    }
                                                },
                                                onDragEnd = { dragIndex = -1; dragOffset = 0f },
                                                onDragCancel = { dragIndex = -1; dragOffset = 0f },
                                            )
                                        } else Modifier
                                    )
                            )
                        }
                    }
                }
            }
        }

        if (!selectionMode) {
            ExtendedFloatingActionButton(
                onClick = onNew,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).navigationBarsPadding(),
                containerColor = NeonGreen, contentColor = Ink, shape = RoundedCornerShape(20.dp),
                icon = { Icon(Icons.Default.Add, null, tint = Ink) },
                text = { Text("新建任务", fontWeight = FontWeight.Bold, color = Ink) },
            )
        }
    }
}

@Composable
fun ExactAlarmBanner(ctx: android.content.Context) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp)).background(InkElevated2).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Schedule, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("需授予精确闹钟权限才能准时唤醒", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        TextButton(onClick = { Permissions.openSettingsFor(ctx, Permissions.Perm.EXACT_ALARM) }) { Text("授权", color = NeonGreen, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun TaskCard(
    task: Task, nextText: String, selectionMode: Boolean, selected: Boolean,
    onToggleSelect: () -> Unit, onToggle: () -> Unit, onClick: () -> Unit,
    onTrigger: () -> Unit, onDelete: () -> Unit, onClone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(if (selected) InkElevated2 else InkElevated)
            .clickable { onClick() }.padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                Checkbox(
                    checked = selected, onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = NeonGreen, uncheckedColor = TextTertiary, checkmarkColor = Ink),
                )
                Spacer(Modifier.width(4.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(task.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.timeText, color = NeonGreen, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(if (task.enabled) "· 下次 $nextText" else "· 已停用", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!selectionMode) NeonSwitch(task.enabled) { onToggle() }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            task.actions.take(3).forEach { a -> Pill(a.type.label) }
            if (task.actions.size > 3) Pill("+${task.actions.size - 3}")
        }
        if (!selectionMode) {
            Spacer(Modifier.height(14.dp)); Hairline(); Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTrigger) { Icon(Icons.Outlined.PlayArrow, "立即执行", tint = NeonGreen, modifier = Modifier.size(22.dp)) }
                Text("立即执行", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClone) { Icon(Icons.Outlined.ContentCopy, "复制", tint = TextTertiary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "删除", tint = TextTertiary, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun NeonSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(
        checkedThumbColor = Ink, checkedTrackColor = NeonGreen,
        uncheckedThumbColor = TextTertiary, uncheckedTrackColor = InkElevated2, uncheckedBorderColor = Hairline,
    ))
}