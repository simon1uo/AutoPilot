package io.github.simon1uo.autopilot.ui

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.simon1uo.autopilot.model.Task

@Composable
fun AutoPilotNav(vm: TaskViewModel = viewModel()) {
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }

    when {
        creating || editing != null -> {
            TaskEditScreen(
                initial = editing ?: Task(),
                onSave = { t -> vm.save(t); creating = false; editing = null },
                onCancel = { creating = false; editing = null },
            )
        }
        else -> TaskListScreen(
            vm = vm,
            onNew = { creating = true },
            onEdit = { t -> editing = t },
        )
    }
}
