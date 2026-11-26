package io.github.simon1uo.autopilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.simon1uo.autopilot.data.HistoryRepository
import io.github.simon1uo.autopilot.model.HistoryEntry
import io.github.simon1uo.autopilot.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { HistoryRepository(ctx) }
    var entries by remember { mutableStateOf<List<HistoryEntry>>(emptyList()) }
    var expanded by remember { mutableStateOf<Long?>(null) }
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }

    val c = LocalAutoColors.current
    fun reload() { entries = repo.load() }
    LaunchedEffect(Unit) { reload() }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回", tint = c.textPrimary) }
                Text("执行历史", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                if (entries.isNotEmpty()) TextButton(onClick = { repo.clear(); reload() }) { Text("清空", color = c.danger) }
            }
            Hairline()

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.History, null, tint = c.textTertiary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("暂无执行记录", color = c.textSecondary)
                        Text("任务执行后这里会显示结果", color = c.textTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(entries, key = { it.id }) { e ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (e.success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                    null, tint = if (e.success) c.accent else c.danger, modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.taskName, color = c.textPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(fmt.format(Date(e.timestamp)), color = c.textTertiary, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(e.message, color = if (e.success) c.textSecondary else c.danger, style = MaterialTheme.typography.labelSmall)
                            }
                            if (e.stepLog.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                TextButton(
                                    onClick = { expanded = if (expanded == e.id) null else e.id },
                                    contentPadding = PaddingValues(0.dp),
                                ) { Text(if (expanded == e.id) "收起明细" else "查看明细", color = c.textTertiary, fontSize = 12.sp) }
                                if (expanded == e.id) {
                                    e.stepLog.forEach { line ->
                                        Text("· $line", color = c.textTertiary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}