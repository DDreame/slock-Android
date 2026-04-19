package com.slock.app.data.store

import com.slock.app.data.model.Agent
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.ui.agent.AgentActivityInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class AgentRuntimeStatus(
    val agentId: String,
    val status: String
)

@Singleton
class AgentStore @Inject constructor(
    private val socketIOManager: SocketIOManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _agentsById = MutableStateFlow<Map<String, Agent>>(emptyMap())
    val agentsById: StateFlow<Map<String, Agent>> = _agentsById.asStateFlow()

    private val _activityByAgentId = MutableStateFlow<Map<String, AgentActivityInfo>>(emptyMap())
    val activityByAgentId: StateFlow<Map<String, AgentActivityInfo>> = _activityByAgentId.asStateFlow()

    private val _runtimeStatus = MutableStateFlow<Map<String, AgentRuntimeStatus>>(emptyMap())
    val runtimeStatus: StateFlow<Map<String, AgentRuntimeStatus>> = _runtimeStatus.asStateFlow()

    init {
        observeSocketEvents()
    }

    fun setAgents(agents: List<Agent>) {
        val map = agents.associateBy { it.id.orEmpty() }
        _agentsById.value = map
        val activities = agents.mapNotNull { agent ->
            agent.activity?.let {
                agent.id.orEmpty() to AgentActivityInfo(
                    activity = it,
                    message = agent.activityDetail
                )
            }
        }.toMap()
        _activityByAgentId.value = activities
    }

    fun removeAgent(agentId: String) {
        _agentsById.value = _agentsById.value - agentId
        _activityByAgentId.value = _activityByAgentId.value - agentId
        _runtimeStatus.value = _runtimeStatus.value - agentId
    }

    fun upsertAgent(agent: Agent) {
        val id = agent.id.orEmpty()
        _agentsById.value = _agentsById.value + (id to agent)
    }

    fun updateActivity(agentId: String, activity: AgentActivityInfo) {
        _activityByAgentId.value = _activityByAgentId.value + (agentId to activity)
    }

    fun clearActivity(agentId: String) {
        _activityByAgentId.value = _activityByAgentId.value - agentId
    }

    fun updateRuntimeStatus(agentId: String, status: AgentRuntimeStatus) {
        _runtimeStatus.value = _runtimeStatus.value + (agentId to status)
    }

    private fun observeSocketEvents() {
        scope.launch {
            socketIOManager.events.collect { event ->
                when (event) {
                    is SocketIOManager.SocketEvent.AgentActivity -> {
                        updateActivity(
                            event.data.agentId,
                            AgentActivityInfo(
                                activity = event.data.activity,
                                message = event.data.message
                            )
                        )
                    }
                    is SocketIOManager.SocketEvent.AgentDeleted -> {
                        removeAgent(event.agentId)
                    }
                    is SocketIOManager.SocketEvent.AgentCreated -> {
                        upsertAgent(
                            Agent(
                                id = event.data.id,
                                name = event.data.name,
                                description = event.data.description
                            )
                        )
                    }
                    else -> { }
                }
            }
        }
    }
}
