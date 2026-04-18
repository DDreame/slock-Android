package com.slock.app.ui.agent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.ActivityLogEntry
import com.slock.app.data.model.Agent
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.ui.navigation.resolveAgentDetailServerId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentDetailUiState(
    val agent: Agent? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val latestActivity: String? = null,
    val latestActivityDetail: String? = null,
    val activityLog: List<ActivityLogEntry> = emptyList(),
    val isLoadingLog: Boolean = false,
    val selectedTab: Int = 0,
    val isResetting: Boolean = false,
    val resetFeedbackMessage: String? = null,
    val isSaving: Boolean = false,
    val updateFeedbackMessage: String? = null
)

@HiltViewModel
class AgentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val agentRepository: AgentRepository,
    private val socketIOManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val agentId: String = savedStateHandle["agentId"] ?: ""
    private val routeServerId: String? = savedStateHandle["serverId"]

    private val _state = MutableStateFlow(AgentDetailUiState())
    val state: StateFlow<AgentDetailUiState> = _state.asStateFlow()

    val serverId: String?
        get() = resolveAgentDetailServerId(routeServerId, activeServerHolder.serverId)

    private var socketJob: Job? = null
    private var socketEntriesDuringLoad = 0

    companion object {
        private const val MAX_LOG_ENTRIES = 200
    }

    init {
        loadAgent()
        observeSocket()
        loadActivityLog()
    }

    private fun loadAgent() {
        val serverId = serverId
        if (serverId.isNullOrBlank()) {
            _state.update { it.copy(isLoading = false, error = "Server context unavailable") }
            return
        }
        if (agentId.isBlank()) {
            _state.update { it.copy(isLoading = false, error = "Agent not found") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            agentRepository.getAgents(serverId).fold(
                onSuccess = { agents ->
                    val agent = agents.find { it.id == agentId }
                    _state.update {
                        it.copy(
                            agent = agent,
                            isLoading = false,
                            latestActivity = agent?.activity,
                            latestActivityDetail = agent?.activityDetail
                        )
                    }
                },
                onFailure = { err ->
                    _state.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    private fun loadActivityLog() {
        val serverId = serverId ?: return
        if (agentId.isBlank()) return
        viewModelScope.launch {
            socketEntriesDuringLoad = 0
            _state.update { it.copy(isLoadingLog = true) }
            agentRepository.getActivityLog(serverId, agentId).fold(
                onSuccess = { entries ->
                    _state.update { currentState ->
                        val liveEntries = currentState.activityLog.take(socketEntriesDuringLoad)
                        currentState.copy(
                            activityLog = (liveEntries + entries).take(MAX_LOG_ENTRIES),
                            isLoadingLog = false
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(isLoadingLog = false) }
                }
            )
        }
    }

    private fun observeSocket() {
        socketJob = viewModelScope.launch {
            socketIOManager.events.collect { event ->
                when (event) {
                    is SocketIOManager.SocketEvent.AgentActivity -> {
                        if (event.data.agentId == agentId) {
                            val newEntry = ActivityLogEntry(
                                timestamp = java.time.Instant.now().toString(),
                                activity = event.data.activity,
                                detail = event.data.message
                            )
                            _state.update {
                                it.copy(
                                    latestActivity = event.data.activity,
                                    latestActivityDetail = event.data.message,
                                    activityLog = (listOf(newEntry) + it.activityLog).take(MAX_LOG_ENTRIES)
                                )
                            }
                            if (_state.value.isLoadingLog) {
                                socketEntriesDuringLoad++
                            }
                        }
                    }
                    else -> { /* ignore */ }
                }
            }
        }
    }

    fun selectTab(tab: Int) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun startAgent() {
        val serverId = serverId ?: return
        viewModelScope.launch {
            agentRepository.startAgent(serverId, agentId).onSuccess {
                _state.update { it.copy(
                    agent = it.agent?.copy(status = "active"),
                    latestActivity = null,
                    latestActivityDetail = null
                ) }
            }
        }
    }

    fun stopAgent() {
        val serverId = serverId ?: return
        viewModelScope.launch {
            agentRepository.stopAgent(serverId, agentId).onSuccess {
                _state.update { it.copy(
                    agent = it.agent?.copy(status = "stopped"),
                    latestActivity = null,
                    latestActivityDetail = null
                ) }
            }
        }
    }

    fun resetAgent() {
        val serverId = serverId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isResetting = true) }
            agentRepository.resetAgent(serverId, agentId).fold(
                onSuccess = {
                    _state.update { it.copy(
                        isResetting = false,
                        resetFeedbackMessage = "Agent reset successful",
                        latestActivity = null,
                        latestActivityDetail = null,
                        activityLog = emptyList()
                    ) }
                    loadActivityLog()
                },
                onFailure = { err ->
                    _state.update { it.copy(
                        isResetting = false,
                        resetFeedbackMessage = err.message ?: "Reset failed"
                    ) }
                }
            )
        }
    }

    fun consumeResetFeedback() {
        _state.update { it.copy(resetFeedbackMessage = null) }
    }

    fun updateAgent(
        name: String?,
        description: String?,
        prompt: String?,
        runtime: String,
        reasoningEffort: String?,
        envVars: Map<String, String>?
    ) {
        val serverId = serverId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            agentRepository.updateAgent(serverId, agentId, name, description, prompt, runtime, reasoningEffort, envVars).fold(
                onSuccess = { updatedAgent ->
                    _state.update { it.copy(
                        agent = updatedAgent,
                        isSaving = false,
                        updateFeedbackMessage = "Agent updated successfully"
                    ) }
                },
                onFailure = { err ->
                    _state.update { it.copy(
                        isSaving = false,
                        updateFeedbackMessage = err.message ?: "Update failed"
                    ) }
                }
            )
        }
    }

    fun consumeUpdateFeedback() {
        _state.update { it.copy(updateFeedbackMessage = null) }
    }

    fun retry() {
        loadAgent()
        loadActivityLog()
    }

    override fun onCleared() {
        super.onCleared()
        socketJob?.cancel()
    }
}
