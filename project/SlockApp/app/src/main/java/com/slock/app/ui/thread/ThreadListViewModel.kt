package com.slock.app.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
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

            // Build name lookup from server members and agents
            val nameLookup = mutableMapOf<String, String>()

            // 1. Server members — map userId -> name using all available name fields
            try {
                val membersResponse = apiService.getServerMembers(serverId)
                if (membersResponse.isSuccessful && membersResponse.body() != null) {
                    membersResponse.body()!!.forEach { member ->
                        val resolvedName = member.user?.name
                            ?: member.displayName
                            ?: member.name
                        if (resolvedName != null) {
                            nameLookup[member.userId.orEmpty()] = resolvedName
                            // Also map by user.id in case it differs from member.userId
                            member.user?.let { nameLookup[it.id.orEmpty()] = resolvedName }
                        }
                    }
                }
                Log.d("ThreadListVM", "Members lookup: ${nameLookup.size} entries")
            } catch (e: Exception) {
                Log.e("ThreadListVM", "Failed to load members for name lookup", e)
            }

            // 2. Agents — use refreshAgents to ensure fresh data from API
            try {
                agentRepository.refreshAgents(serverId).fold(
                    onSuccess = { agents ->
                        agents.forEach { nameLookup[it.id.orEmpty()] = it.name.orEmpty() }
                        Log.d("ThreadListVM", "Agents lookup: added ${agents.size} agents")
                    },
                    onFailure = {
                        // Fallback to cache
                        agentRepository.getAgents(serverId).fold(
                            onSuccess = { agents -> agents.forEach { nameLookup[it.id.orEmpty()] = it.name.orEmpty() } },
                            onFailure = { }
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ThreadListVM", "Failed to load agents for name lookup", e)
            }

            threadRepository.getFollowedThreads(serverId).fold(
                onSuccess = { summaries ->
                    val threads = summaries
                        .filter { it.threadChannelId?.isNotBlank() == true }
                        .sortedByDescending { it.lastReplyAt ?: "" }
                        .map { summary ->
                            val senderName = summary.parentMessageSenderName
                                ?: nameLookup[summary.parentMessageSenderId]
                                ?: run {
                                    Log.w("ThreadListVM", "Unresolved sender: id=${summary.parentMessageSenderId}, type=${summary.parentMessageSenderType}, lookup keys=${nameLookup.keys}")
                                    if (summary.parentMessageSenderType == "agent") "Agent" else "User"
                                }
                            ThreadItem(
                                parentMessage = Message(
                                    id = summary.parentMessageId.orEmpty(),
                                    channelId = summary.parentChannelId.orEmpty(),
                                    content = summary.parentMessagePreview,
                                    senderId = summary.parentMessageSenderId,
                                    senderName = senderName,
                                    senderType = summary.parentMessageSenderType
                                ),
                                channelName = summary.channelName.orEmpty(),
                                threadChannelId = summary.threadChannelId.orEmpty(),
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
