package com.slock.app.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.SettingsPreferencesStore
import com.slock.app.data.model.Agent
import com.slock.app.data.model.DEFAULT_AGENT_MODEL_OPTIONS
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

data class AgentActivityInfo(
    val activity: String,
    val message: String? = null
)

data class AgentUiState(
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val agentActivities: Map<String, AgentActivityInfo> = emptyMap(),
    val availableModels: List<String> = DEFAULT_AGENT_MODEL_OPTIONS
)

internal fun deriveAvailableAgentModels(
    recentModels: List<String>,
    discoveredModels: List<String>,
    seedModels: List<String> = DEFAULT_AGENT_MODEL_OPTIONS
): List<String> {
    val mergedModels = LinkedHashSet<String>()

    fun addModels(models: List<String>) {
        models.forEach { model ->
            val trimmed = model.trim()
            if (trimmed.isNotEmpty()) {
                mergedModels += trimmed
            }
        }
    }

    addModels(recentModels)
    addModels(discoveredModels)
    addModels(seedModels)

    return mergedModels.toList()
}

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val socketIOManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder,
    private val settingsPreferencesStore: SettingsPreferencesStore
) : ViewModel() {

    private val _state = MutableStateFlow(AgentUiState())
    val state: StateFlow<AgentUiState> = _state.asStateFlow()

    private var socketEventsJob: Job? = null
    private var recentModelsJob: Job? = null
    private var recentModels: List<String> = emptyList()

    init {
        observeRecentAgentModels()
        observeAgentActivities()
    }

    private fun observeRecentAgentModels() {
        recentModelsJob = viewModelScope.launch {
            settingsPreferencesStore.recentAgentModelsFlow.collect { models ->
                recentModels = models
                _state.update { current ->
                    current.copy(
                        availableModels = deriveAvailableAgentModels(
                            recentModels = recentModels,
                            discoveredModels = current.agents.mapNotNull { it.model }
                        )
                    )
                }
            }
        }
    }

    private fun observeAgentActivities() {
        socketEventsJob = viewModelScope.launch {
            socketIOManager.events.collect { event ->
                when (event) {
                    is SocketIOManager.SocketEvent.AgentActivity -> {
                        _state.update { current ->
                            current.copy(
                                agentActivities = current.agentActivities + (event.data.agentId to AgentActivityInfo(
                                    activity = event.data.activity,
                                    message = event.data.message
                                ))
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
                        val serverId = activeServerHolder.serverId ?: return@collect
                        loadAgents(serverId)
                    }
                    else -> { /* ignore */ }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketEventsJob?.cancel()
        recentModelsJob?.cancel()
    }

    private var currentServerId: String? = null

    fun loadAgents(serverId: String) {
        currentServerId = serverId
        activeServerHolder.serverId = serverId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.agents.isEmpty(), error = null) }
            // Get cached data first
            agentRepository.getAgents(serverId).fold(
                onSuccess = { agents ->
                    val activities = agents.mapNotNull { agent ->
                        agent.activity?.let {
                            agent.id.orEmpty() to AgentActivityInfo(
                                activity = it,
                                message = agent.activityDetail
                            )
                        }
                    }.toMap()
                    _state.update {
                        it.copy(
                            agents = agents,
                            agentActivities = it.agentActivities + activities,
                            isLoading = false,
                            availableModels = deriveAvailableAgentModels(
                                recentModels = recentModels,
                                discoveredModels = agents.mapNotNull { agent -> agent.model }
                            )
                        )
                    }
                },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
            // Then refresh from cloud
            agentRepository.refreshAgents(serverId).fold(
                onSuccess = { agents ->
                    val activities = agents.mapNotNull { agent ->
                        agent.activity?.let {
                            agent.id.orEmpty() to AgentActivityInfo(
                                activity = it,
                                message = agent.activityDetail
                            )
                        }
                    }.toMap()
                    _state.update {
                        it.copy(
                            agents = agents,
                            agentActivities = it.agentActivities + activities,
                            error = null,
                            availableModels = deriveAvailableAgentModels(
                                recentModels = recentModels,
                                discoveredModels = agents.mapNotNull { agent -> agent.model }
                            )
                        )
                    }
                },
                onFailure = { /* keep cached data */ }
            )
        }
    }

    fun retryIfEmpty() {
        val serverId = currentServerId ?: activeServerHolder.serverId ?: return
        if (_state.value.agents.isEmpty() && !_state.value.isLoading) {
            loadAgents(serverId)
        }
    }

    fun createAgent(name: String, description: String, prompt: String, model: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            agentRepository.createAgent(serverId, name, description, prompt, model).fold(
                onSuccess = { agent ->
                    settingsPreferencesStore.addRecentAgentModel(model)
                    _state.update {
                        val updatedAgents = it.agents + agent
                        it.copy(
                            agents = updatedAgents,
                            isLoading = false,
                            availableModels = deriveAvailableAgentModels(
                                recentModels = recentModels,
                                discoveredModels = updatedAgents.mapNotNull { existing -> existing.model } + model
                            )
                        )
                    }
                },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun startAgent(agentId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.startAgent(serverId, agentId).fold(
                onSuccess = { _state.update { it.copy(agents = it.agents.map { a -> if (a.id == agentId) a.copy(status = "active") else a }) } },
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

    fun updateAgent(agentId: String, name: String?, description: String?, prompt: String?) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.updateAgent(serverId, agentId, name, description, prompt).fold(
                onSuccess = { updatedAgent ->
                    _state.update { it.copy(agents = it.agents.map { a -> if (a.id == agentId) updatedAgent else a }) }
                },
                onFailure = { }
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
