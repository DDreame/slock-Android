package com.slock.app.ui.agent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.ActivityLogEntry
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

data class AgentDetailUiState(
    val agent: Agent? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val latestActivity: String? = null,
    val latestActivityDetail: String? = null,
    val activityLog: List<ActivityLogEntry> = emptyList(),
    val isLoadingLog: Boolean = false,
    val selectedTab: Int = 0
)

@HiltViewModel
class AgentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val agentRepository: AgentRepository,
    private val socketIOManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val agentId: String = savedStateHandle["agentId"] ?: ""

    private val _state = MutableStateFlow(AgentDetailUiState())
    val state: StateFlow<AgentDetailUiState> = _state.asStateFlow()

    val serverId: String? get() = activeServerHolder.serverId

    private var socketJob: Job? = null

    companion object {
        private const val MAX_LOG_ENTRIES = 200
    }

    init {
        loadAgent()
        observeSocket()
        loadActivityLog()
    }

    private fun loadAgent() {
        val serverId = activeServerHolder.serverId
        if (serverId.isNullOrBlank()) {
            _state.update { it.copy(isLoading = false, error = "Server not selected") }
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
        val serverId = activeServerHolder.serverId ?: return
        if (agentId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingLog = true) }
            agentRepository.getActivityLog(serverId, agentId).fold(
                onSuccess = { entries ->
                    _state.update { it.copy(activityLog = entries, isLoadingLog = false) }
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
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.startAgent(serverId, agentId).onSuccess {
                _state.update { it.copy(agent = it.agent?.copy(status = "active")) }
            }
        }
    }

    fun stopAgent() {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            agentRepository.stopAgent(serverId, agentId).onSuccess {
                _state.update { it.copy(agent = it.agent?.copy(status = "stopped")) }
            }
        }
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
