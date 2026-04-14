package com.slock.app.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.model.Server
import com.slock.app.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerUiState(
    val servers: List<Server> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ServerViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ServerUiState())
    val state: StateFlow<ServerUiState> = _state.asStateFlow()

    var selectedServerId: String? = null
        private set

    fun selectServer(serverId: String) {
        selectedServerId = serverId
    }

    init { loadServers() }
    
    fun loadServers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.servers.isEmpty(), error = null) }
            // Get cached data first (instant)
            serverRepository.getServers().fold(
                onSuccess = { servers -> _state.update { it.copy(servers = servers, isLoading = false) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
            // Then refresh from cloud (cloud always wins)
            serverRepository.refreshServers().fold(
                onSuccess = { servers -> _state.update { it.copy(servers = servers) } },
                onFailure = { /* keep cached data on network failure */ }
            )
        }
    }
    
    fun createServer(name: String, slug: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            serverRepository.createServer(name, slug).fold(
                onSuccess = { server -> _state.update { it.copy(servers = it.servers + server, isLoading = false) } },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }
    
    fun deleteServer(serverId: String) {
        viewModelScope.launch {
            serverRepository.deleteServer(serverId).fold(
                onSuccess = { _state.update { it.copy(servers = it.servers.filter { s -> s.id != serverId }) } },
                onFailure = { err -> _state.update { it.copy(error = err.message) } }
            )
        }
    }
}
