package com.slock.app.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Agent
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentUiState(
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val agentActivities: Map<String, String> = emptyMap()
)

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val socketIOManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(AgentUiState())
    val state: StateFlow<AgentUiState> = _state.asStateFlow()

    private var socketEventsJob: Job? = null

    init {
        observeAgentActivities()
    }

    private fun observeAgentActivities() {
        socketEventsJob = viewModelScope.launch {
            socketIOManager.events.collect { event ->
                when (event) {
                    is SocketIOManager.SocketEvent.AgentActivity -> {
                        _state.update { current ->
                            current.copy(
                                agentActivities = current.agentActivities + (event.data.agentId to event.data.activity)
                            )
                        }
                    }
                    is SocketIOManager.SocketEvent.AgentDeleted -> {
                        _state.update { current ->
                            current.copy(
                                agents = current.agents.filter { it.id != event.agentId },
                                agentActivities = current.agentActivities - event.agentId
                            )
                        }
                    }
                    is SocketIOManager.SocketEvent.AgentCreated -> {
                        loadAgents()
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

    fun loadAgents() {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            agentRepository.getAgents(serverId).fold(
                onSuccess = { agents -> _state.update { it.copy(agents = agents, isLoading = false) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun createAgent(name: String, description: String, prompt: String, model: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            agentRepository.createAgent(serverId, name, description, prompt, model).fold(
                onSuccess = { agent -> _state.update { it.copy(agents = it.agents + agent, isLoading = false) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun startAgent(agentId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.startAgent(serverId, agentId).fold(
                onSuccess = { _state.update { it.copy(agents = it.agents.map { a -> if (a.id == agentId) a.copy(status = "running") else a }) } },
                onFailure = { }
            )
        }
    }

    fun stopAgent(agentId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.stopAgent(serverId, agentId).fold(
                onSuccess = { _state.update { it.copy(agents = it.agents.map { a -> if (a.id == agentId) a.copy(status = "stopped") else a }) } },
                onFailure = { }
            )
        }
    }

    fun resetAgent(agentId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.resetAgent(serverId, agentId).fold(
                onSuccess = { _state.update { it.copy(agentActivities = it.agentActivities - agentId) } },
                onFailure = { }
            )
        }
    }

    fun deleteAgent(agentId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.deleteAgent(serverId, agentId).fold(
                onSuccess = { _state.update { it.copy(agents = it.agents.filter { a -> a.id != agentId }) } },
                onFailure = { }
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
