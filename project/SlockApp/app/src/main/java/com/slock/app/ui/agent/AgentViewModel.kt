package com.slock.app.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.model.Agent
import com.slock.app.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentUiState(
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentRepository: AgentRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(AgentUiState())
    val state: StateFlow<AgentUiState> = _state.asStateFlow()
    
    init { loadAgents() }
    
    fun loadAgents() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            agentRepository.getAgents().fold(
                onSuccess = { agents -> _state.update { it.copy(agents = agents, isLoading = false) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }
    
    fun createAgent(name: String, description: String, prompt: String, model: String) {
        viewModelScope.launch {
            agentRepository.createAgent(name, description, prompt, model).fold(
                onSuccess = { agent -> _state.update { it.copy(agents = it.agents + agent) } },
                onFailure = { err -> _state.update { it.copy(error = err.message) } }
            )
        }
    }
    
    fun startAgent(agentId: String) {
        viewModelScope.launch {
            agentRepository.startAgent(agentId).fold(
                onSuccess = { _state.update { it.copy(agents = it.agents.map { a -> if (a.id == agentId) a.copy(status = "running") else a }) } },
                onFailure = { }
            )
        }
    }
    
    fun stopAgent(agentId: String) {
        viewModelScope.launch {
            agentRepository.stopAgent(agentId).fold(
                onSuccess = { _state.update { it.copy(agents = it.agents.map { a -> if (a.id == agentId) a.copy(status = "stopped") else a }) } },
                onFailure = { }
            )
        }
    }
    
    fun deleteAgent(agentId: String) {
        viewModelScope.launch {
            agentRepository.deleteAgent(agentId).fold(
                onSuccess = { _state.update { it.copy(agents = it.agents.filter { a -> a.id != agentId }) } },
                onFailure = { }
            )
        }
    }
}
