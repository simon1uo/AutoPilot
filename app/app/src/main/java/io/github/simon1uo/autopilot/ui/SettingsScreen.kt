package io.github.simon1uo.autopilot.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.simon1uo.autopilot.BuildConfig
import io.github.simon1uo.autopilot.control.HttpServerService
import io.github.simon1uo.autopilot.data.SettingsStore
import io.github.simon1uo.autopilot.data.TaskRepository
import io.github.simon1uo.autopilot.engine.Permissions
import io.github.simon1uo.autopilot.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onShowPrivacy: () -> Unit, onShowLicenses: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    var refresh by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // top bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回", tint = TextPrimary) }
                Text("设置", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Hairline()

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                // ============= 权限中心 =============
                SettingsSection("权限中心") {
                    Permissions.Perm.entries.forEach { perm ->
                        PermissionRow(ctx, perm, refreshKey = refresh)
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ============= 通用 =============
                SettingsSection("通用") {
                    ToggleRow("开机自启", "重启后自动恢复所有任务", store.autoStartOnBoot) { store.autoStartOnBoot = it }
                    ToggleRow("新任务默认启用", "新建任务时默认开启", store.newTaskEnabledByDefault) { store.newTaskEnabledByDefault = it }
                    InfoRow("主题", "极简黑白（固定深色）")
                }
                Spacer(Modifier.height(16.dp))

                // ============= HTTP 服务 =============
                SettingsSection("HTTP 控制服务") {
                    ToggleRow("随应用启动", "应用在前台时自动启动 HTTP 服务", store.httpAutoStart) { store.httpAutoStart = it }
                    HttpPortRow(store)
                    val ip = remember { localIp(ctx) }
                    InfoRow("本机地址", if (ip != null) "http://$ip:${store.httpPort}" else "未连接网络")
                }
                Spacer(Modifier.height(16.dp))

                // ============= 通知 =============
                SettingsSection("通知") {
                    ToggleRow("任务执行时通知", "任务完成时发一条本地通知", store.notifyOnTaskRun) { store.notifyOnTaskRun = it }
                    ToggleRow("通知声音", "通知附带提示音", store.notifySound) { store.notifySound = it }
                }
                Spacer(Modifier.height(16.dp))

                // ============= 数据管理 =============
                SettingsSection("数据管理") {
                    ActionRow(Icons.Outlined.FileUpload, "导出任务", "将所有任务导出为 JSON") {
                        DataManager.exportTasks(ctx)
                    }
                    ActionRow(Icons.Outlined.FileDownload, "导入任务", "从剪贴板/文件导入 JSON") {
                        DataManager.importTasks(ctx); refresh++
                    }
                    ActionRow(Icons.Outlined.DeleteSweep, "清空所有任务", "不可恢复，请谨慎", danger = true) {
                        TaskRepository(ctx).save(emptyList())
                        refresh++
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ============= 关于 =============
                SettingsSection("关于") {
                    InfoRow("版本", "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
                    InfoRow("构建类型", BuildConfig.BUILD_TYPE)
                    ActionRow(Icons.Outlined.PrivacyTip, "隐私政策", "本 App 不收集任何数据") { onShowPrivacy() }
                    ActionRow(Icons.Outlined.Description, "开源许可", "第三方库许可声明") { onShowLicenses() }
                    ActionRow(Icons.Outlined.Code, "开源地址", "github.com/simon1uo/AutoPilot") {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/simon1uo/AutoPilot")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

private fun localIp(ctx: Context): String? = try {
    val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    @Suppress("DEPRECATION")
    val ipInt = wm.connectionInfo.ipAddress
    if (ipInt == 0) null else String.format("%d.%d.%d.%d", ipInt and 0xff, ipInt shr 8 and 0xff, ipInt shr 16 and 0xff, ipInt shr 24 and 0xff)
} catch (e: Exception) { null }

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = TextTertiary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(InkElevated)) {
        content()
    }
}

@Composable
fun PermissionRow(ctx: Context, perm: Permissions.Perm, refreshKey: Int) {
    val relevant = remember(refreshKey) { Permissions.isRelevant(ctx, perm) }
    val granted = remember(refreshKey) { Permissions.isGranted(ctx, perm) }
    Row(
        Modifier.fillMaxWidth().clickable(enabled = relevant) { Permissions.openSettingsFor(ctx, perm) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(perm.title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(perm.desc, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        }
        if (!relevant) {
            Pill("不适用", bg = InkElevated2, fg = TextTertiary)
        } else if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("已授权", color = NeonGreen, style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Pill("去授权", bg = NeonGreen, fg = Ink)
        }
    }
    Hairline()
}

@Composable
fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(desc, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(
            checkedThumbColor = Ink, checkedTrackColor = NeonGreen,
            uncheckedThumbColor = TextTertiary, uncheckedTrackColor = InkElevated2,
        ))
    }
    Hairline()
}

@Composable
fun InfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
    Hairline()
}

@Composable
fun ActionRow(icon: ImageVector, title: String, desc: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (danger) DangerRed else TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) DangerRed else TextPrimary, fontWeight = FontWeight.Medium)
            Text(desc, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
    Hairline()
}

@Composable
fun HttpPortRow(store: SettingsStore) {
    var portText by remember { mutableStateOf(store.httpPort.toString()) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("端口", color = TextPrimary, fontWeight = FontWeight.Medium)
            Text("HTTP 服务监听端口 (1024-65535)", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        }
        OutlinedTextField(
            value = portText,
            onValueChange = { t -> portText = t.filter { c -> c.isDigit() }; t.toIntOrNull()?.let { if (it in 1024..65535) store.httpPort = it } },
            modifier = Modifier.width(110.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = InkElevated2, unfocusedContainerColor = InkElevated2,
                focusedBorderColor = NeonGreen, unfocusedBorderColor = Hairline,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = NeonGreen,
            ),
        )
    }
    Hairline()
}

/** 导出/导入/清空工具 */
object DataManager {
    fun exportTasks(ctx: Context) {
        try {
            val repo = TaskRepository(ctx)
            val json = kotlinx.serialization.json.Json { prettyPrint = true; encodeDefaults = true }
                .encodeToString(kotlinx.serialization.builtins.ListSerializer(io.github.simon1uo.autopilot.model.Task.serializer()), repo.load())
            val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clip.setPrimaryClip(android.content.ClipData.newPlainText("AutoPilot tasks", json))
            android.widget.Toast.makeText(ctx, "已复制任务 JSON 到剪贴板", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(ctx, "导出失败：${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun importTasks(ctx: Context) {
        try {
            val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val text = clip.primaryClip?.getItemAt(0)?.text?.toString() ?: run {
                android.widget.Toast.makeText(ctx, "剪贴板为空", android.widget.Toast.LENGTH_SHORT).show(); return
            }
            val list = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true }
                .decodeFromString(kotlinx.serialization.builtins.ListSerializer(io.github.simon1uo.autopilot.model.Task.serializer()), text)
            TaskRepository(ctx).save(list)
            android.widget.Toast.makeText(ctx, "已导入 ${list.size} 个任务", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(ctx, "导入失败：${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
