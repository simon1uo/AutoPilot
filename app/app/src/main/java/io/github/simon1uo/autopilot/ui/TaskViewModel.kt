package io.github.simon1uo.autopilot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.simon1uo.autopilot.data.TaskRepository
import io.github.simon1uo.autopilot.engine.CoreActions
import io.github.simon1uo.autopilot.engine.Scheduler
import io.github.simon1uo.autopilot.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TaskRepository(app)
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    init { reload() }

    fun reload() { _tasks.value = repo.load() }

    fun save(task: Task) {
        val list = repo.load()
        val idx = list.indexOfFirst { it.id == task.id }
        if (idx >= 0) list[idx] = task else list.add(task)
        repo.save(list)
        if (task.enabled) Scheduler.schedule(getApplication(), task) else Scheduler.cancel(getApplication(), task)
        reload()
    }

    fun delete(task: Task) {
        val list = repo.load().filter { it.id != task.id }
        repo.save(list)
        Scheduler.cancel(getApplication(), task)
        reload()
    }

    fun toggle(task: Task) = save(task.copy(enabled = !task.enabled))

    fun triggerNow(task: Task) {
        viewModelScope.launch { CoreActions.runTask(getApplication(), task) }
    }

    fun nextTimeText(task: Task): String {
        val ms = Scheduler.nextTriggerMillis(task) ?: return "-"
        val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return fmt.format(java.util.Date(ms))
    }
}
