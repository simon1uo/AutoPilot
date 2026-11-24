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
        if (idx >= 0) {
            list[idx] = task
        } else {
            val maxOrder = list.maxOfOrNull { it.order } ?: -1
            list.add(if (task.order == 0) task.copy(order = maxOrder + 1) else task)
        }
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

    /** 拖拽排序后保存新顺序 */
    fun reorder(from: Int, to: Int) {
        val list = tasks.value.toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return
        val item = list.removeAt(from)
        list.add(to, item)
        repo.save(list.mapIndexed { i, t -> t.copy(order = i) })
        reload()
    }

    /** 复制/克隆任务 */
    fun clone(task: Task) {
        val list = repo.load()
        val maxOrder = list.maxOfOrNull { it.order } ?: 0
        val copy = task.copy(id = System.currentTimeMillis(), name = task.name + " 副本", order = maxOrder + 1)
        list.add(copy)
        repo.save(list)
        if (copy.enabled) Scheduler.schedule(getApplication(), copy)
        reload()
    }

    fun deleteMany(ids: Set<Long>) {
        val list = repo.load()
        list.filter { it.id in ids }.forEach { Scheduler.cancel(getApplication(), it) }
        repo.save(list.filter { it.id !in ids })
        reload()
    }

    fun setEnabledMany(ids: Set<Long>, enabled: Boolean) {
        val list = repo.load().map { if (it.id in ids) it.copy(enabled = enabled) else it }
        list.filter { it.id in ids }.forEach {
            if (it.enabled) Scheduler.schedule(getApplication(), it) else Scheduler.cancel(getApplication(), it)
        }
        repo.save(list)
        reload()
    }

    fun nextTimeText(task: Task): String {
        val ms = Scheduler.nextTriggerMillis(task) ?: return "-"
        val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return fmt.format(java.util.Date(ms))
    }
}