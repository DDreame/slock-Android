package com.slock.app.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.SettingsPreferencesStore
import com.slock.app.data.model.Agent
import com.slock.app.data.model.DEFAULT_AGENT_MODEL_OPTIONS
import com.slock.app.data.model.supportsAgentReasoningEffort
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.store.AgentStore
import com.slock.app.data.store.AgentRuntimeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val availableModels: List<String> = DEFAULT_AGENT_MODEL_OPTIONS,
    val createFeedbackMessage: String? = null
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
    private val agentStore: AgentStore,
    private val activeServerHolder: ActiveServerHolder,
    private val settingsPreferencesStore: SettingsPreferencesStore
) : ViewModel() {

    private val _state = MutableStateFlow(AgentUiState())
    val state: StateFlow<AgentUiState> = _state.asStateFlow()

    private var storeObserverJob: Job? = null
    private var recentModelsJob: Job? = null
    private var recentModels: List<String> = emptyList()

    init {
        observeRecentAgentModels()
        observeStore()
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

    private fun observeStore() {
        storeObserverJob = viewModelScope.launch {
            combine(
                agentStore.agentsById,
                agentStore.activityByAgentId,
                agentStore.runtimeStatus
            ) { agentsMap, activitiesMap, runtimeMap ->
                Triple(agentsMap, activitiesMap, runtimeMap)
            }.collect { (agentsMap, activitiesMap, runtimeMap) ->
                val mergedAgents = agentsMap.values.map { agent ->
                    val override = runtimeMap[agent.id.orEmpty()]
                    if (override != null) agent.copy(status = override.status) else agent
                }
                _state.update { current ->
                    current.copy(
                        agents = mergedAgents,
                        agentActivities = activitiesMap,
                        availableModels = deriveAvailableAgentModels(
                            recentModels = recentModels,
                            discoveredModels = agentsMap.values.mapNotNull { it.model }
                        )
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        storeObserverJob?.cancel()
        recentModelsJob?.cancel()
    }

    private var currentServerId: String? = null

    fun loadAgents(serverId: String) {
        currentServerId = serverId
        activeServerHolder.serverId = serverId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.agents.isEmpty(), error = null) }
            agentRepository.getAgents(serverId).fold(
                onSuccess = { agents ->
                    agentStore.setAgents(agents)
                    _state.update { it.copy(isLoading = false) }
                },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
            agentRepository.refreshAgents(serverId).fold(
                onSuccess = { agents ->
                    agentStore.setAgents(agents)
                    _state.update { it.copy(error = null) }
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

    fun createAgent(
        name: String,
        description: String,
        prompt: String,
        model: String,
        runtime: String?,
        reasoningEffort: String?,
        envVars: Map<String, String>?
    ) {
        val serverId = activeServerHolder.serverId ?: return
        val resolvedReasoningEffort = if (supportsAgentReasoningEffort(runtime)) reasoningEffort else null
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            agentRepository.createAgent(
                serverId = serverId,
                name = name,
                description = description,
                prompt = prompt,
                model = model,
                runtime = runtime,
                reasoningEffort = resolvedReasoningEffort,
                envVars = envVars,
                avatar = null
            ).fold(
                onSuccess = { agent ->
                    settingsPreferencesStore.addRecentAgentModel(model)
                    agentStore.upsertAgent(agent)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            createFeedbackMessage = "Agent created. Open a DM or configure it from the list."
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
                onSuccess = {
                    agentStore.updateRuntimeStatus(agentId, AgentRuntimeStatus(agentId = agentId, status = "active"))
                    agentStore.clearActivity(agentId)
                },
                onFailure = { }
            )
        }
    }

    fun stopAgent(agentId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.stopAgent(serverId, agentId).fold(
                onSuccess = {
                    agentStore.updateRuntimeStatus(agentId, AgentRuntimeStatus(agentId = agentId, status = "stopped"))
                    agentStore.clearActivity(agentId)
                },
                onFailure = { }
            )
        }
    }

    fun resetAgent(agentId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.resetAgent(serverId, agentId).fold(
                onSuccess = { agentStore.clearActivity(agentId) },
                onFailure = { }
            )
        }
    }

    fun deleteAgent(agentId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.deleteAgent(serverId, agentId).fold(
                onSuccess = { agentStore.removeAgent(agentId) },
                onFailure = { }
            )
        }
    }

    fun updateAgent(
        agentId: String,
        name: String?,
        description: String?,
        prompt: String?,
        runtime: String?,
        reasoningEffort: String?,
        envVars: Map<String, String>?
    ) {
        val serverId = activeServerHolder.serverId ?: return
        val resolvedReasoningEffort = if (supportsAgentReasoningEffort(runtime)) reasoningEffort else null
        viewModelScope.launch {
            agentRepository.updateAgent(
                serverId = serverId,
                agentId = agentId,
                name = name,
                description = description,
                prompt = prompt,
                runtime = runtime,
                reasoningEffort = resolvedReasoningEffort,
                envVars = envVars
            ).fold(
                onSuccess = { updatedAgent ->
                    agentStore.upsertAgent(updatedAgent)
                },
                onFailure = { }
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun consumeCreateFeedback() {
        _state.update { it.copy(createFeedbackMessage = null) }
    }
}
