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
import io.github.simon1uo.autopilot.engine.UpdateChecker
import io.github.simon1uo.autopilot.ui.AutoPilotNav
import io.github.simon1uo.autopilot.ui.theme.AutoPilotTheme
import io.github.simon1uo.autopilot.ui.theme.ThemeState

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

/** 启动时自动检查更新（可在设置里关闭），发现新版本弹窗提示。 */
@Composable
private fun StartupUpdateCheck() {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    var update by remember { mutableStateOf<UpdateChecker.Result?>(null) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!store.autoCheckUpdate) return@LaunchedEffect
        val r = UpdateChecker.check()
        // 同一版本只提示一次
        if (r.hasUpdate && r.latestVersion != store.lastCheckedUpdateVersion) {
            update = r; shown = true
            store.lastCheckedUpdateVersion = r.latestVersion
        }
    }

    update?.let { r ->
        if (shown) AlertDialog(
            onDismissRequest = { shown = false },
            title = { Text("发现新版本") },
            text = { Text("新版本 v${r.latestVersion} 可用（当前 v${r.currentVersion}）。是否前往下载？") },
            confirmButton = {
                TextButton(onClick = {
                    shown = false
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse(if (r.releaseUrl.isNotBlank()) r.releaseUrl else "https://github.com/simon1uo/AutoPilot/releases")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }) { Text("前往下载") }
            },
            dismissButton = { TextButton(onClick = { shown = false }) { Text("以后再说") } },
        )
    }
}