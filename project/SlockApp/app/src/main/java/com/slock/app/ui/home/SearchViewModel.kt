package com.slock.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.dao.AgentDao
import com.slock.app.data.local.dao.ChannelDao
import com.slock.app.data.local.dao.MessageDao
import com.slock.app.data.local.toModel
import com.slock.app.data.model.Agent
import com.slock.app.data.model.Channel
import com.slock.app.data.model.Message
import com.slock.app.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val channels: List<Channel> = emptyList(),
    val messages: List<Message> = emptyList(),
    val agents: List<Agent> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val isRemoteSearching: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val channelDao: ChannelDao,
    private val messageDao: MessageDao,
    private val agentDao: AgentDao,
    private val messageRepository: MessageRepository,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var remoteSearchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        if (query.isBlank()) {
            searchJob?.cancel()
            remoteSearchJob?.cancel()
            _state.update { SearchUiState() }
            return
        }
        // Debounce: wait 300ms after last keystroke
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        val serverId = activeServerHolder.serverId
        if (serverId.isNullOrBlank()) return

        _state.update { it.copy(isSearching = true) }

        // Local Room search — all three categories in parallel
        val channelsDeferred = viewModelScope.launch {
            try {
                val results = channelDao.searchChannels(serverId, query)
                _state.update { it.copy(channels = results.map { e -> e.toModel() }) }
            } catch (_: Exception) { }
        }
        val messagesDeferred = viewModelScope.launch {
            try {
                val results = messageDao.searchMessages(query)
                _state.update { it.copy(messages = results.map { e -> e.toModel() }) }
            } catch (_: Exception) { }
        }
        val agentsDeferred = viewModelScope.launch {
            try {
                val results = agentDao.searchAgents(serverId, query)
                _state.update { it.copy(agents = results.map { e -> e.toModel() }) }
            } catch (_: Exception) { }
        }

        channelsDeferred.join()
        messagesDeferred.join()
        agentsDeferred.join()

        _state.update { it.copy(isSearching = false, hasSearched = true) }

        // Also fire remote message search for broader results
        performRemoteSearch(serverId, query)
    }

    private fun performRemoteSearch(serverId: String, query: String) {
        remoteSearchJob?.cancel()
        remoteSearchJob = viewModelScope.launch {
            _state.update { it.copy(isRemoteSearching = true) }
            messageRepository.searchMessages(serverId, query, searchServerId = serverId).fold(
                onSuccess = { remoteMessages ->
                    _state.update { current ->
                        // Merge remote results with local, deduplicate by id
                        val existingIds = current.messages.map { it.id }.toSet()
                        val newMessages = remoteMessages.filter { it.id !in existingIds }
                        current.copy(
                            messages = current.messages + newMessages,
                            isRemoteSearching = false
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(isRemoteSearching = false) }
                }
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        remoteSearchJob?.cancel()
        _state.update { SearchUiState() }
    }
}
