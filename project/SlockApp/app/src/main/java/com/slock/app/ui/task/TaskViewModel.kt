package com.slock.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Task
import com.slock.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val channelId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(TaskUiState())
    val state: StateFlow<TaskUiState> = _state.asStateFlow()

    fun loadTasks(channelId: String) {
        viewModelScope.launch {
            _state.update { it.copy(channelId = channelId, isLoading = true) }
            taskRepository.getTasks(activeServerHolder.serverId ?: "", channelId).fold(
                onSuccess = { tasks -> _state.update { it.copy(tasks = tasks, isLoading = false) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun createTask(title: String, description: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            taskRepository.createTask(activeServerHolder.serverId ?: "", _state.value.channelId, title, description, null, null).fold(
                onSuccess = { task -> _state.update { it.copy(tasks = it.tasks + task, isLoading = false) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
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
