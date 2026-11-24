package io.github.simon1uo.autopilot.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.simon1uo.autopilot.data.SettingsStore
import io.github.simon1uo.autopilot.model.Task

private enum class Screen { LIST, EDIT, SETTINGS, PRIVACY, LICENSES }

@Composable
fun AutoPilotNav(vm: TaskViewModel = viewModel()) {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf(Screen.LIST) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }

    when (screen) {
        Screen.EDIT -> {
            val initial = remember(editing, creating) {
                editing ?: Task(enabled = SettingsStore(ctx).newTaskEnabledByDefault)
            }
            TaskEditScreen(
                initial = initial,
                onSave = { t -> vm.save(t); creating = false; editing = null; screen = Screen.LIST },
                onCancel = { creating = false; editing = null; screen = Screen.LIST },
            )
        }
        Screen.SETTINGS -> SettingsScreen(
            onBack = { screen = Screen.LIST; vm.reload() },
            onShowPrivacy = { screen = Screen.PRIVACY },
            onShowLicenses = { screen = Screen.LICENSES },
        )
        Screen.PRIVACY -> TextDocScreen("隐私政策 Privacy", PRIVACY_TEXT) { screen = Screen.SETTINGS }
        Screen.LICENSES -> TextDocScreen("开源许可 Licenses", LICENSES_TEXT) { screen = Screen.SETTINGS }
        Screen.LIST -> TaskListScreen(
            vm = vm,
            onNew = { creating = true; editing = null; screen = Screen.EDIT },
            onEdit = { t -> editing = t; creating = false; screen = Screen.EDIT },
            onOpenSettings = { screen = Screen.SETTINGS },
        )
    }
}
