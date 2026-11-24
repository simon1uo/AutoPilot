package io.github.simon1uo.autopilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.simon1uo.autopilot.ui.theme.*

/** 通用纯文本阅读页（隐私政策 / 开源许可） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextDocScreen(title: String, body: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回", tint = TextPrimary) }
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Hairline()
            Text(
                body,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            )
        }
    }
}

val PRIVACY_TEXT = """
AutoPilot 隐私政策 / Privacy Policy
最后更新：2026-11-24

【中文】
AutoPilot 是一款完全本地运行的 Android 自动化工具。我们高度重视你的隐私：

1. 数据存储：你的所有任务、设置均仅保存在本设备的本地存储中（应用私有目录），不会上传到任何服务器。
2. 不收集信息：本应用不收集、不传输你的个人信息、位置、通讯录、使用记录等任何隐私数据。
3. 无网络上传：除你主动使用内建 HTTP 控制服务（仅在本地局域网内监听）外，应用不主动联网。
4. 无广告、无追踪、无第三方分析 SDK。
5. 权限用途：
   - 精确闹钟：保证定时任务准时触发；
   - 通知：任务执行提醒与服务状态显示；
   - 电池优化白名单：避免后台任务被系统清理；
   - 通知使用权：仅在需要读取通知状态的动作中可选使用。
6. 卸载应用即删除全部本地数据。
7. 本项目开源，代码可审计：https://github.com/simon1uo/AutoPilot

如有疑问，请通过 GitHub Issues 联系。

【English】
AutoPilot is a fully local Android automation tool. Your privacy matters:

1. Storage: all tasks and settings stay in this device's private app storage; nothing is uploaded.
2. No collection: we do not collect or transmit any personal data (location, contacts, usage, etc.).
3. No upload: the app does not access the network except the optional built-in HTTP server, which only listens on your local network.
4. No ads, no tracking, no third-party analytics SDK.
5. Permissions: exact alarm (timely triggers), notifications (reminders/status), battery optimization whitelist (keep tasks alive), notification access (optional, for reading notification state).
6. Uninstalling removes all local data.
7. Open source & auditable: https://github.com/simon1uo/AutoPilot
""".trimIndent()

val LICENSES_TEXT = """
AutoPilot 开源许可声明 / Open Source Licenses

本应用 (AutoPilot) 以 MIT License 发布。
Copyright (c) 2026 simon1uo
https://github.com/simon1uo/AutoPilot/blob/main/LICENSE

本应用使用了以下优秀的开源库，谨致谢忱：

• Jetpack Compose / AndroidX  (Apache License 2.0)
  https://developer.android.com/jetpack

• Kotlin & kotlinx.serialization  (Apache License 2.0)
  https://github.com/Kotlin/kotlinx.serialization

• Kotlin Coroutines  (Apache License 2.0)
  https://github.com/Kotlin/kotlinx.coroutines

• NanoHTTPD  (BSD 3-Clause License)
  https://github.com/NanoHttpd/nanohttpd
  Copyright (c) 2012-2013 NanoHttpd

以上库的完整许可文本可在其各自仓库中查阅。
完整的第三方许可清单见仓库 LICENSES.md。
""".trimIndent()