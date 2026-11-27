package io.github.simon1uo.autopilot

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.simon1uo.autopilot.data.SettingsStore
import io.github.simon1uo.autopilot.engine.AppUpdater
import io.github.simon1uo.autopilot.engine.UpdateChecker
import io.github.simon1uo.autopilot.ui.AutoPilotNav
import io.github.simon1uo.autopilot.ui.theme.AutoPilotTheme
import io.github.simon1uo.autopilot.ui.theme.ThemeState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 应用在前台时启动内嵌 HTTP 控制服务（后台启动会被系统拒绝）
        runCatching { if (SettingsStore(this).httpAutoStart) io.github.simon1uo.autopilot.control.HttpServerService.start(this) }
        ThemeState.init(this)
        setContent {
            AutoPilotTheme(mode = ThemeState.mode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AutoPilotNav()
                    StartupUpdateCheck()
                }
            }
        }
    }
}

/** 启动时自动检查更新（可在设置里关闭）。direct 版支持应用内下载安装；foss 版不检查。 */
@Composable
private fun StartupUpdateCheck() {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    val scope = rememberCoroutineScope()
    var update by remember { mutableStateOf<UpdateChecker.Result?>(null) }
    var shown by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (!BuildConfig.SELF_UPDATE) return@LaunchedEffect
        if (!store.autoCheckUpdate) return@LaunchedEffect
        val r = UpdateChecker.check()
        if (r.hasUpdate && r.latestVersion != store.lastCheckedUpdateVersion) {
            update = r; shown = true
            store.lastCheckedUpdateVersion = r.latestVersion
        }
    }

    update?.let { r ->
        if (shown) AlertDialog(
            onDismissRequest = { if (!downloading) shown = false },
            title = { Text(if (downloading) "正在更新" else "发现新版本") },
            text = {
                Text(
                    if (downloading) "正在下载 v${r.latestVersion}… $progress%"
                    else "新版本 v${r.latestVersion} 可用（当前 v${r.currentVersion}）。是否立即更新？"
                )
            },
            confirmButton = {
                TextButton(enabled = !downloading, onClick = {
                    val canSelfUpdate = BuildConfig.SELF_UPDATE && r.apkUrl.isNotBlank()
                    if (canSelfUpdate) {
                        if (!AppUpdater.canInstall(ctx)) {
                            AppUpdater.openInstallPermission(ctx)
                            android.widget.Toast.makeText(ctx, "请允许「安装未知应用」后重试", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            downloading = true
                            scope.launch {
                                runCatching { AppUpdater.downloadAndInstall(ctx, r.apkUrl, r.apkSha256) { progress = it } }
                                    .onFailure { android.widget.Toast.makeText(ctx, "更新失败：${it.message}", android.widget.Toast.LENGTH_LONG).show() }
                                downloading = false
                                shown = false
                            }
                        }
                    } else {
                        shown = false
                        runCatching {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse(if (r.releaseUrl.isNotBlank()) r.releaseUrl else "https://github.com/simon1uo/AutoPilot/releases")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }) { Text(if (BuildConfig.SELF_UPDATE && r.apkUrl.isNotBlank()) "立即更新" else "前往下载") }
            },
            dismissButton = { if (!downloading) TextButton(onClick = { shown = false }) { Text("以后再说") } },
        )
    }
}
