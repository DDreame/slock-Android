package com.slock.app.ui.machine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Machine
import com.slock.app.data.repository.MachineRepository
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MachineUiState(
    val machines: List<Machine> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MachineViewModel @Inject constructor(
    private val machineRepository: MachineRepository,
    private val socketIOManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(MachineUiState())
    val state: StateFlow<MachineUiState> = _state.asStateFlow()

    private var socketEventsJob: Job? = null
    private var currentServerId: String? = null

    init {
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        socketEventsJob = viewModelScope.launch {
            socketIOManager.events.collect { event ->
                when (event) {
                    is SocketIOManager.SocketEvent.MachineStatus -> {
                        _state.update { current ->
                            current.copy(
                                machines = current.machines.map { m ->
                                    if (m.id == event.machineId) m.copy(status = event.status) else m
                                }
                            )
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

    fun loadMachines(serverId: String) {
        currentServerId = serverId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.machines.isEmpty(), error = null) }
            machineRepository.getMachines(serverId).fold(
                onSuccess = { machines ->
                    _state.update { it.copy(machines = machines, isLoading = false) }
                },
                onFailure = { err ->
                    _state.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    fun deleteMachine(machineId: String) {
        val serverId = currentServerId ?: activeServerHolder.serverId ?: return
        viewModelScope.launch {
            machineRepository.deleteMachine(serverId, machineId).fold(
                onSuccess = {
                    _state.update { it.copy(machines = it.machines.filter { m -> m.id != machineId }) }
                },
                onFailure = { }
            )
        }
    }

    fun retry() {
        val serverId = currentServerId ?: activeServerHolder.serverId ?: return
        loadMachines(serverId)
    }
}
