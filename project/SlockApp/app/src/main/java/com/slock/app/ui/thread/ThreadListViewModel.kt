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

enum class ThreadInboxTab { FOLLOWING, ALL, DONE }

data class ThreadItem(
    val parentMessage: Message,
    val channelName: String,
    val threadChannelId: String,
    val replyCount: Int = 0,
    val unreadCount: Int = 0,
    val lastActivity: String = ""
)

data class ThreadListUiState(
    val threads: List<ThreadItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTab: ThreadInboxTab = ThreadInboxTab.FOLLOWING,
    val followingThreads: List<ThreadItem> = emptyList(),
    val doneThreads: List<ThreadItem> = emptyList(),
    val actionFeedback: String? = null
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

    fun selectTab(tab: ThreadInboxTab) {
        _state.update { state ->
            val visibleThreads = when (tab) {
                ThreadInboxTab.FOLLOWING -> state.followingThreads
                ThreadInboxTab.ALL -> (state.followingThreads + state.doneThreads)
                    .sortedByDescending { it.lastActivity }
                ThreadInboxTab.DONE -> state.doneThreads
            }
            state.copy(selectedTab = tab, threads = visibleThreads)
        }
    }

    fun markThreadDone(threadChannelId: String) {
        val serverId = activeServerHolder.serverId ?: return
        _state.update { state ->
            val thread = state.followingThreads.find { it.threadChannelId == threadChannelId }
                ?: return@update state
            val newFollowing = state.followingThreads.filter { it.threadChannelId != threadChannelId }
            val newDone = (state.doneThreads + thread).sortedByDescending { it.lastActivity }
            val visibleThreads = when (state.selectedTab) {
                ThreadInboxTab.FOLLOWING -> newFollowing
                ThreadInboxTab.ALL -> (newFollowing + newDone).sortedByDescending { it.lastActivity }
                ThreadInboxTab.DONE -> newDone
            }
            state.copy(followingThreads = newFollowing, doneThreads = newDone, threads = visibleThreads)
        }
        viewModelScope.launch {
            threadRepository.markThreadDone(serverId, threadChannelId).onFailure {
                rollbackDone(threadChannelId)
                _state.update { it.copy(actionFeedback = "Failed to mark done") }
            }
        }
    }

    fun undoThreadDone(threadChannelId: String) {
        val serverId = activeServerHolder.serverId ?: return
        _state.update { state ->
            val thread = state.doneThreads.find { it.threadChannelId == threadChannelId }
                ?: return@update state
            val newDone = state.doneThreads.filter { it.threadChannelId != threadChannelId }
            val newFollowing = (state.followingThreads + thread).sortedByDescending { it.lastActivity }
            val visibleThreads = when (state.selectedTab) {
                ThreadInboxTab.FOLLOWING -> newFollowing
                ThreadInboxTab.ALL -> (newFollowing + newDone).sortedByDescending { it.lastActivity }
                ThreadInboxTab.DONE -> newDone
            }
            state.copy(followingThreads = newFollowing, doneThreads = newDone, threads = visibleThreads)
        }
        viewModelScope.launch {
            threadRepository.undoThreadDone(serverId, threadChannelId).fold(
                onSuccess = { loadThreads(serverId) },
                onFailure = { _state.update { it.copy(actionFeedback = "Failed to undo done") } }
            )
        }
    }

    fun followThread(parentMessageId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            threadRepository.followThread(serverId, parentMessageId).fold(
                onSuccess = { loadThreads(serverId) },
                onFailure = { _state.update { it.copy(actionFeedback = "Failed to follow thread") } }
            )
        }
    }

    fun unfollowThread(threadChannelId: String) {
        val serverId = activeServerHolder.serverId ?: return
        _state.update { state ->
            val newFollowing = state.followingThreads.filter { it.threadChannelId != threadChannelId }
            val visibleThreads = when (state.selectedTab) {
                ThreadInboxTab.FOLLOWING -> newFollowing
                ThreadInboxTab.ALL -> (newFollowing + state.doneThreads).sortedByDescending { it.lastActivity }
                ThreadInboxTab.DONE -> state.doneThreads
            }
            state.copy(followingThreads = newFollowing, threads = visibleThreads)
        }
        viewModelScope.launch {
            threadRepository.unfollowThread(serverId, threadChannelId).onFailure {
                loadThreads(serverId)
                _state.update { it.copy(actionFeedback = "Failed to unfollow thread") }
            }
        }
    }

    fun consumeActionFeedback() {
        _state.update { it.copy(actionFeedback = null) }
    }

    private fun rollbackDone(threadChannelId: String) {
        _state.update { state ->
            val thread = state.doneThreads.find { it.threadChannelId == threadChannelId }
                ?: return@update state
            val newDone = state.doneThreads.filter { it.threadChannelId != threadChannelId }
            val newFollowing = (state.followingThreads + thread).sortedByDescending { it.lastActivity }
            val visibleThreads = when (state.selectedTab) {
                ThreadInboxTab.FOLLOWING -> newFollowing
                ThreadInboxTab.ALL -> (newFollowing + newDone).sortedByDescending { it.lastActivity }
                ThreadInboxTab.DONE -> newDone
            }
            state.copy(followingThreads = newFollowing, doneThreads = newDone, threads = visibleThreads)
        }
    }

    fun loadThreads(serverId: String) {
        activeServerHolder.serverId = serverId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.followingThreads.isEmpty() && it.doneThreads.isEmpty(), error = null) }

            val nameLookup = mutableMapOf<String, String>()

            try {
                val membersResponse = apiService.getServerMembers(serverId)
                if (membersResponse.isSuccessful && membersResponse.body() != null) {
                    membersResponse.body()!!.forEach { member ->
                        val resolvedName = member.user?.name
                            ?: member.displayName
                            ?: member.name
                        if (resolvedName != null) {
                            nameLookup[member.userId.orEmpty()] = resolvedName
                            member.user?.let { nameLookup[it.id.orEmpty()] = resolvedName }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ThreadListVM", "Failed to load members for name lookup", e)
            }

            try {
                agentRepository.refreshAgents(serverId).fold(
                    onSuccess = { agents ->
                        agents.forEach { nameLookup[it.id.orEmpty()] = it.name.orEmpty() }
                    },
                    onFailure = {
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
                        .map { summary -> summaryToThreadItem(summary, nameLookup) }

                    _state.update { state ->
                        val doneIds = state.doneThreads.map { it.threadChannelId }.toSet()
                        val newFollowing = threads.filter { it.threadChannelId !in doneIds }
                        val visibleThreads = when (state.selectedTab) {
                            ThreadInboxTab.FOLLOWING -> newFollowing
                            ThreadInboxTab.ALL -> (newFollowing + state.doneThreads).sortedByDescending { it.lastActivity }
                            ThreadInboxTab.DONE -> state.doneThreads
                        }
                        state.copy(
                            followingThreads = newFollowing,
                            threads = visibleThreads,
                            isLoading = false
                        )
                    }
                },
                onFailure = { err ->
                    _state.update { it.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    private fun summaryToThreadItem(summary: ThreadSummary, nameLookup: Map<String, String>): ThreadItem {
        val senderName = summary.parentMessageSenderName
            ?: nameLookup[summary.parentMessageSenderId]
            ?: if (summary.parentMessageSenderType == "agent") "Agent" else "User"
        return ThreadItem(
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
            unreadCount = summary.unreadCount,
            lastActivity = summary.lastReplyAt ?: ""
        )
    }

    override fun onCleared() {
        super.onCleared()
        socketEventsJob?.cancel()
    }
}
