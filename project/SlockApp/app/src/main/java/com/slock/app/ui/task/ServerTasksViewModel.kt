package com.slock.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Task
import com.slock.app.data.repository.ServerRepository
import com.slock.app.data.repository.TaskRepository
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerTasksUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val collapsedGroups: Set<String> = setOf("todo", "done"),
    val memberNames: Map<String, String> = emptyMap()
)

@HiltViewModel
class ServerTasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val serverRepository: ServerRepository,
    private val socketIOManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(ServerTasksUiState())
    val state: StateFlow<ServerTasksUiState> = _state.asStateFlow()

    private var socketEventsJob: Job? = null
    private var currentServerId: String? = null

    init {
        observeTaskEvents()
    }

    private fun observeTaskEvents() {
        socketEventsJob = viewModelScope.launch {
            socketIOManager.events.collect { event ->
                when (event) {
                    is SocketIOManager.SocketEvent.TaskCreated -> {
                        // Reload all tasks to get the full task object
                        currentServerId?.let { loadAllTasks(it) }
                    }
                    is SocketIOManager.SocketEvent.TaskUpdated -> {
                        _state.update { current ->
                            current.copy(
                                tasks = current.tasks.map { task ->
                                    if (task.id == event.data.id) task.copy(status = event.data.status) else task
                                }
                            )
                        }
                    }
                    is SocketIOManager.SocketEvent.TaskDeleted -> {
                        _state.update { current ->
                            current.copy(tasks = current.tasks.filter { it.id != event.taskId })
                        }
                    }
                    else -> { /* ignore */ }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketEventsJob?.cancel()
    }

    fun loadAllTasks(serverId: String) {
        currentServerId = serverId
        activeServerHolder.serverId = serverId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.tasks.isEmpty(), error = null) }

            taskRepository.getServerTasks(serverId).fold(
                onSuccess = { tasks ->
                    _state.update { it.copy(tasks = tasks, isLoading = false) }
                },
                onFailure = { err ->
                    _state.update { it.copy(isLoading = false, error = err.message) }
                }
            )
            loadMemberNames(serverId)
        }
    }

    private fun loadMemberNames(serverId: String) {
        viewModelScope.launch {
            serverRepository.getServerMembers(serverId).fold(
                onSuccess = { members ->
                    val names = members.associate { m ->
                        (m.userId ?: m.id.orEmpty()) to
                            (m.user?.name ?: m.displayName ?: m.name ?: "?")
                    }
                    _state.update { it.copy(memberNames = names) }
                },
                onFailure = { }
            )
        }
    }

    fun retryIfEmpty() {
        val serverId = currentServerId ?: activeServerHolder.serverId ?: return
        if (_state.value.tasks.isEmpty() && !_state.value.isLoading) {
            loadAllTasks(serverId)
        }
    }

    fun toggleGroup(status: String) {
        _state.update { current ->
            val newCollapsed = if (status in current.collapsedGroups) {
                current.collapsedGroups - status
            } else {
                current.collapsedGroups + status
            }
            current.copy(collapsedGroups = newCollapsed)
        }
    }

    fun updateTaskStatus(taskId: String, status: String) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(activeServerHolder.serverId ?: "", taskId, status).fold(
                onSuccess = { updatedTask ->
                    _state.update { state ->
                        state.copy(tasks = state.tasks.map { if (it.id == taskId) updatedTask else it })
                    }
                },
                onFailure = { err -> _state.update { it.copy(error = err.message) } }
            )
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(activeServerHolder.serverId ?: "", taskId).fold(
                onSuccess = { _state.update { it.copy(tasks = it.tasks.filter { t -> t.id != taskId }) } },
                onFailure = { err -> _state.update { it.copy(error = err.message) } }
            )
        }
    }
}
