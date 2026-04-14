package com.slock.app.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.model.Message
import com.slock.app.data.model.ThreadSummary
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.repository.ThreadRepository
import com.slock.app.data.api.ApiService
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThreadItem(
    val parentMessage: Message,
    val channelName: String,
    val threadChannelId: String,
    val replyCount: Int = 0,
    val lastActivity: String = ""
)

data class ThreadListUiState(
    val threads: List<ThreadItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ThreadListViewModel @Inject constructor(
    private val threadRepository: ThreadRepository,
    private val agentRepository: AgentRepository,
    private val apiService: ApiService,
    private val socketIOManager: SocketIOManager,
    private val activeServerHolder: ActiveServerHolder
) : ViewModel() {

    private val _state = MutableStateFlow(ThreadListUiState())
    val state: StateFlow<ThreadListUiState> = _state.asStateFlow()

    private var socketEventsJob: Job? = null

    init {
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        socketEventsJob = viewModelScope.launch {
            try {
                socketIOManager.events.collect { event ->
                    when (event) {
                        is SocketIOManager.SocketEvent.ThreadUpdated -> {
                            val serverId = activeServerHolder.serverId ?: return@collect
                            loadThreads(serverId)
                        }
                        else -> { /* ignore */ }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun loadThreads(serverId: String) {
        activeServerHolder.serverId = serverId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.threads.isEmpty(), error = null) }

            // Build name lookup from members and agents
            val nameLookup = mutableMapOf<String, String>()
            try {
                val membersResponse = apiService.getServerMembers(serverId)
                if (membersResponse.isSuccessful && membersResponse.body() != null) {
                    membersResponse.body()!!.forEach { member ->
                        member.user?.let { nameLookup[member.userId] = it.name }
                    }
                }
            } catch (_: Exception) { }
            try {
                agentRepository.getAgents(serverId).fold(
                    onSuccess = { agents -> agents.forEach { nameLookup[it.id] = it.name } },
                    onFailure = { }
                )
            } catch (_: Exception) { }

            threadRepository.getFollowedThreads(serverId).fold(
                onSuccess = { summaries ->
                    val threads = summaries
                        .filter { it.threadChannelId.isNotBlank() }
                        .sortedByDescending { it.lastReplyAt ?: "" }
                        .map { summary ->
                            val senderName = summary.parentMessageSenderName
                                ?: nameLookup[summary.parentMessageSenderId]
                                ?: if (summary.parentMessageSenderType == "agent") "Agent" else "User"
                            ThreadItem(
                                parentMessage = Message(
                                    id = summary.parentMessageId,
                                    channelId = summary.parentChannelId,
                                    content = summary.parentMessagePreview,
                                    senderId = summary.parentMessageSenderId,
                                    senderName = senderName,
                                    senderType = summary.parentMessageSenderType
                                ),
                                channelName = summary.channelName,
                                threadChannelId = summary.threadChannelId,
                                replyCount = summary.replyCount,
                                lastActivity = summary.lastReplyAt ?: ""
                            )
                        }
                    _state.update { it.copy(threads = threads, isLoading = false) }
                },
                onFailure = { err ->
                    _state.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketEventsJob?.cancel()
    }
}
