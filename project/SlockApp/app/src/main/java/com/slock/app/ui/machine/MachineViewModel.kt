package com.slock.app.ui.machine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Machine
import com.slock.app.data.repository.MachineRepository
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AddMachineStep { ChooseType, Connecting, Connected }

data class MachineUiState(
    val machines: List<Machine> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val addMachineStep: AddMachineStep? = null,
    val newMachineId: String? = null,
    val newMachineApiKey: String? = null,
    val newMachineName: String? = null,
    val connectedMachine: Machine? = null,
    val actionFeedback: String? = null,
    val deleteBlockedMachine: Machine? = null
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
    private var pollingJob: Job? = null
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
                        if (event.machineId == _state.value.newMachineId && event.status == "connected") {
                            onPolledMachineConnected()
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
        pollingJob?.cancel()
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

    fun startAddMachine() {
        _state.update { it.copy(addMachineStep = AddMachineStep.ChooseType) }
    }

    fun createMachine(name: String) {
        val serverId = currentServerId ?: activeServerHolder.serverId ?: return
        viewModelScope.launch {
            machineRepository.createMachine(serverId, name).fold(
                onSuccess = { response ->
                    _state.update {
                        it.copy(
                            addMachineStep = AddMachineStep.Connecting,
                            newMachineId = response.machine.id,
                            newMachineApiKey = response.apiKey,
                            newMachineName = name
                        )
                    }
                    startPolling(serverId, response.machine.id.orEmpty())
                },
                onFailure = { err ->
                    _state.update { it.copy(actionFeedback = "Create machine failed: ${err.message}") }
                }
            )
        }
    }

    private fun startPolling(serverId: String, machineId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                machineRepository.getMachines(serverId).onSuccess { machines ->
                    _state.update { it.copy(machines = machines) }
                    val target = machines.find { it.id == machineId }
                    if (target?.status == "connected") {
                        onPolledMachineConnected()
                        return@launch
                    }
                }
            }
        }
    }

    private fun onPolledMachineConnected() {
        pollingJob?.cancel()
        val machineId = _state.value.newMachineId ?: return
        val connected = _state.value.machines.find { it.id == machineId }
        _state.update {
            it.copy(
                addMachineStep = AddMachineStep.Connected,
                connectedMachine = connected,
                newMachineName = connected?.hostname ?: it.newMachineName
            )
        }
    }

    fun finishAddMachine(newName: String) {
        val serverId = currentServerId ?: activeServerHolder.serverId ?: return
        val machineId = _state.value.newMachineId ?: return
        val originalName = _state.value.connectedMachine?.name
        viewModelScope.launch {
            if (newName.isNotBlank() && newName != originalName) {
                machineRepository.renameMachine(serverId, machineId, newName).onFailure { err ->
                    _state.update { it.copy(actionFeedback = "Rename failed: ${err.message}") }
                }
            }
            machineRepository.getMachines(serverId).onSuccess { machines ->
                _state.update { it.copy(machines = machines) }
            }
            _state.update {
                it.copy(
                    addMachineStep = null,
                    newMachineId = null,
                    newMachineApiKey = null,
                    newMachineName = null,
                    connectedMachine = null
                )
            }
        }
    }

    fun cancelAddMachine() {
        pollingJob?.cancel()
        _state.update {
            it.copy(
                addMachineStep = null,
                newMachineId = null,
                newMachineApiKey = null,
                newMachineName = null,
                connectedMachine = null
            )
        }
    }

    fun requestDeleteMachine(machine: Machine) {
        if (!machine.runningAgents.isNullOrEmpty()) {
            _state.update { it.copy(deleteBlockedMachine = machine) }
            return
        }
        deleteMachine(machine.id.orEmpty())
    }

    fun dismissDeleteBlocked() {
        _state.update { it.copy(deleteBlockedMachine = null) }
    }

    fun deleteMachine(machineId: String) {
        val serverId = currentServerId ?: activeServerHolder.serverId ?: return
        viewModelScope.launch {
            machineRepository.deleteMachine(serverId, machineId).fold(
                onSuccess = {
                    _state.update { it.copy(machines = it.machines.filter { m -> m.id != machineId }) }
                },
                onFailure = { err ->
                    _state.update { it.copy(actionFeedback = "Delete failed: ${err.message}") }
                }
            )
        }
    }

    fun consumeActionFeedback() {
        _state.update { it.copy(actionFeedback = null) }
    }

    fun retry() {
        val serverId = currentServerId ?: activeServerHolder.serverId ?: return
        loadMachines(serverId)
    }
}
