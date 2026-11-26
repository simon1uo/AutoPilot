package io.github.simon1uo.autopilot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.simon1uo.autopilot.engine.Permissions
import io.github.simon1uo.autopilot.model.Task
import io.github.simon1uo.autopilot.ui.theme.*

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
    val c = LocalAutoColors.current

    // 搜索 & 筛选 & 批量
    var query by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    var showOnlyEnabled by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }
    var refreshing by remember { mutableStateOf(false) }

    val filtered = remember(tasks, query, showOnlyEnabled) {
        tasks.filter { t ->
            (query.isBlank() || t.name.contains(query, true) || t.actions.any { it.appLabel.contains(query, true) }) &&
            (!showOnlyEnabled || t.enabled)
        }
    }

    fun doRefresh() {
        refreshing = true
        vm.reload()
        refreshing = false
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // ===== Header =====
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AutoPilot", color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (tasks.isEmpty()) "还没有任务" else "共 ${tasks.size} 个任务" + if (filtered.size != tasks.size) " · 筛选出 ${filtered.size}" else "",
                        color = c.textTertiary, style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onOpenHistory) { Icon(Icons.Outlined.History, "执行历史", tint = c.textSecondary) }
                IconButton(onClick = {
                    selectionMode = !selectionMode
                    if (!selectionMode) selected.clear()
                }) {
                    Icon(
                        if (selectionMode) Icons.Outlined.Close else Icons.Outlined.Checklist,
                        "选择", tint = if (selectionMode) c.accent else c.textSecondary,
                    )
                }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, "设置", tint = c.textSecondary) }
            }

            // ===== Search bar（紧凑胶囊 + 可展开筛选）=====
            if (tasks.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompactSearchField(
                        value = query, onChange = { query = it },
                        modifier = Modifier.weight(1f),
                    )
                    // 筛选开关（圆形图标钮）
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(50))
                            .background(if (showFilter || showOnlyEnabled) c.accent else c.card)
                            .clickable { showFilter = !showFilter },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.FilterList, "筛选",
                            tint = if (showFilter || showOnlyEnabled) c.onAccent else c.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                AnimatedVisibility(visible = showFilter) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = showOnlyEnabled, onClick = { showOnlyEnabled = !showOnlyEnabled },
                            label = { Text("仅启用", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = c.accent, selectedLabelColor = c.onAccent,
                                containerColor = c.card, labelColor = c.textSecondary,
                            ),
                        )
                        if (showOnlyEnabled || query.isNotBlank()) {
                            FilterChip(
                                selected = false, onClick = { showOnlyEnabled = false; query = "" },
                                label = { Text("清除", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = c.card, labelColor = c.textSecondary,
                                ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (!Permissions.isGranted(ctx, Permissions.Perm.EXACT_ALARM) && Permissions.isRelevant(ctx, Permissions.Perm.EXACT_ALARM))
                ExactAlarmBanner(ctx)

            // ===== 批量操作条 =====
            if (selectionMode) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp)).background(c.cardHigh).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("已选 ${selected.size}", color = c.accent, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.setEnabledMany(selected.toSet(), true); selected.clear() }, enabled = selected.isNotEmpty()) { Text("启用", color = c.accent) }
                    TextButton(onClick = { vm.setEnabledMany(selected.toSet(), false); selected.clear() }, enabled = selected.isNotEmpty()) { Text("停用", color = c.textSecondary) }
                    TextButton(onClick = { vm.deleteMany(selected.toSet()); selected.clear(); selectionMode = false }) { Text("删除", color = c.danger) }
                }
            }

            // ===== List =====
            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Bolt, null, tint = c.accent.copy(alpha = 0.7f), modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("开始创建第一个自动化任务", color = c.textSecondary, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("右下角 + 新建任务", color = c.textTertiary, style = MaterialTheme.typography.bodySmall)
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
                        items(filtered.size, key = { filtered[it].id }) { i ->
                            val task = filtered[i]
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
                containerColor = c.accent, contentColor = c.onAccent, shape = RoundedCornerShape(20.dp),
                icon = { Icon(Icons.Default.Add, null, tint = c.onAccent) },
                text = { Text("新建任务", fontWeight = FontWeight.Bold, color = c.onAccent) },
            )
        }
    }
}

/** 紧凑胶囊搜索框：小号字、约 44dp 高 */
@Composable
fun CompactSearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAutoColors.current
    OutlinedTextField(
        value = value, onValueChange = onChange,
        modifier = modifier.height(46.dp),
        placeholder = { Text("搜索任务或应用", color = c.textTertiary, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = c.textTertiary, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Close, "清除", tint = c.textTertiary, modifier = Modifier.size(16.dp))
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.textPrimary),
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = c.card, unfocusedContainerColor = c.card,
            focusedBorderColor = c.accent, unfocusedBorderColor = c.hairline,
            focusedTextColor = c.textPrimary, unfocusedTextColor = c.textPrimary, cursorColor = c.accent,
        ),
    )
}

@Composable
fun ExactAlarmBanner(ctx: android.content.Context) {
    val c = LocalAutoColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp)).background(c.cardHigh).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Schedule, null, tint = c.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("需授予精确闹钟权限才能准时唤醒", color = c.textSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        TextButton(onClick = { Permissions.openSettingsFor(ctx, Permissions.Perm.EXACT_ALARM) }) { Text("授权", color = c.accent, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun TaskCard(
    task: Task, nextText: String, selectionMode: Boolean, selected: Boolean,
    onToggleSelect: () -> Unit, onToggle: () -> Unit, onClick: () -> Unit,
    onTrigger: () -> Unit, onDelete: () -> Unit, onClone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAutoColors.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(if (selected) c.cardHigh else c.card)
            .clickable { onClick() }.padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                Checkbox(
                    checked = selected, onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = c.accent, uncheckedColor = c.textTertiary, checkmarkColor = c.onAccent),
                )
                Spacer(Modifier.width(4.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(task.name, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.timeText, color = c.accent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(if (task.enabled) "· 下次 $nextText" else "· 已停用", color = c.textTertiary, style = MaterialTheme.typography.bodySmall)
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
                IconButton(onClick = onTrigger) { Icon(Icons.Outlined.PlayArrow, "立即执行", tint = c.accent, modifier = Modifier.size(22.dp)) }
                Text("立即执行", color = c.textSecondary, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClone) { Icon(Icons.Outlined.ContentCopy, "复制", tint = c.textTertiary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "删除", tint = c.textTertiary, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun NeonSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = LocalAutoColors.current
    Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(
        checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
        uncheckedThumbColor = c.textTertiary, uncheckedTrackColor = c.cardHigh, uncheckedBorderColor = c.hairline,
    ))
}
