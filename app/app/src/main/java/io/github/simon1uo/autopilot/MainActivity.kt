package io.github.simon1uo.autopilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.simon1uo.autopilot.ui.AutoPilotNav
import io.github.simon1uo.autopilot.ui.TaskViewModel
import io.github.simon1uo.autopilot.ui.theme.AutoPilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 应用在前台时启动内嵌 HTTP 控制服务（后台启动会被系统拒绝）
        runCatching { if (io.github.simon1uo.autopilot.data.SettingsStore(this).httpAutoStart) io.github.simon1uo.autopilot.control.HttpServerService.start(this) }
        setContent {
            AutoPilotTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AutoPilotNav()
                }
            }
        }
    }
}