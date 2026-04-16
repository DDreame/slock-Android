package com.slock.app.ui.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Agent
import com.slock.app.data.model.Member
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.repository.ServerRepository
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberItem(
    val id: String,
    val name: String,
    val role: String,
    val isAgent: Boolean,
    val isOnline: Boolean,
    val subtitle: String,
    val model: String? = null
)

data class MembersUiState(
    val members: List<MemberItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val agentRepository: AgentRepository,
    private val socketIOManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(MembersUiState())
    val state: StateFlow<MembersUiState> = _state.asStateFlow()

    private var socketEventsJob: Job? = null
    private var currentServerId: String? = null

    init {
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        socketEventsJob = viewModelScope.launch {
            socketIOManager.events.collect { event ->
                when (event) {
                    is SocketIOManager.SocketEvent.AgentActivity -> {
                        _state.update { current ->
                            current.copy(
                                members = current.members.map { m ->
                                    if (m.id == event.data.agentId && m.isAgent) {
                                        m.copy(subtitle = event.data.activity)
                                    } else m
                                }
                            )
                        }
                    }
                    is SocketIOManager.SocketEvent.AgentCreated,
                    is SocketIOManager.SocketEvent.AgentDeleted -> {
                        currentServerId?.let { loadMembers(it) }
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

    fun loadMembers(serverId: String) {
        currentServerId = serverId
        activeServerHolder.serverId = serverId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.members.isEmpty(), error = null) }

            val memberItems = mutableListOf<MemberItem>()

            // Load human members
            serverRepository.getServerMembers(serverId).fold(
                onSuccess = { members ->
                    members.forEach { member ->
                        val displayName = member.user?.name
                            ?: member.displayName
                            ?: member.name
                            ?: "Unknown"
                        memberItems.add(
                            MemberItem(
                                id = member.id,
                                name = displayName,
                                role = member.role,
                                isAgent = false,
                                isOnline = false, // No presence API yet
                                subtitle = member.role.replaceFirstChar { it.uppercase() }
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _state.update { it.copy(isLoading = false, error = err.message) }
                    return@launch
                }
            )

            // Load agents
            agentRepository.getAgents(serverId).fold(
                onSuccess = { agents ->
                    agents.forEach { agent ->
                        val isActive = agent.status == "active"
                        val subtitle = buildAgentSubtitle(agent)
                        memberItems.add(
                            MemberItem(
                                id = agent.id,
                                name = agent.name,
                                role = "agent",
                                isAgent = true,
                                isOnline = isActive,
                                subtitle = subtitle,
                                model = agent.model
                            )
                        )
                    }
                },
                onFailure = { /* keep human members at least */ }
            )

            _state.update { it.copy(members = memberItems, isLoading = false) }

            // Refresh agents from cloud
            agentRepository.refreshAgents(serverId).fold(
                onSuccess = { agents ->
                    _state.update { current ->
                        val nonAgentMembers = current.members.filter { !it.isAgent }
                        val freshAgents = agents.map { agent ->
                            MemberItem(
                                id = agent.id,
                                name = agent.name,
                                role = "agent",
                                isAgent = true,
                                isOnline = agent.status == "active",
                                subtitle = buildAgentSubtitle(agent),
                                model = agent.model
                            )
                        }
                        current.copy(members = nonAgentMembers + freshAgents)
                    }
                },
                onFailure = { /* keep cached */ }
            )
        }
    }

    fun retryIfEmpty() {
        val serverId = currentServerId ?: activeServerHolder.serverId ?: return
        if (_state.value.members.isEmpty() && !_state.value.isLoading) {
            loadMembers(serverId)
        }
    }

    private fun buildAgentSubtitle(agent: Agent): String {
        val statusText = when {
            agent.activity != null && agent.status == "active" -> agent.activity
            agent.status == "active" -> "Working"
            else -> "Hibernating"
        }
        @Suppress("SENSELESS_COMPARISON")
        val model = if (agent.model != null) agent.model else ""
        val modelShort = when {
            model.contains("opus", ignoreCase = true) -> "Opus"
            model.contains("sonnet", ignoreCase = true) -> "Sonnet"
            model.contains("haiku", ignoreCase = true) -> "Haiku"
            model.contains("gpt", ignoreCase = true) -> "GPT"
            model.isNotEmpty() -> model.take(10)
            else -> "Unknown"
        }
        return "$statusText \u00B7 $modelShort"
    }
}
