package com.slock.app.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.model.Channel
import com.slock.app.data.model.Message
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.socket.SocketIOManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelUiState(
    val channels: List<Channel> = emptyList(),
    val dms: List<Channel> = emptyList(),
    val channelPreviews: Map<String, Message> = emptyMap(),
    val onlineIds: Set<String> = emptySet(),
    val serverId: String = "",
    val isLoading: Boolean = false,
    val isDmLoading: Boolean = false,
    val error: String? = null,
    val actionFeedbackMessage: String? = null
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val messageRepository: MessageRepository,
    private val agentRepository: AgentRepository,
    private val activeServerHolder: ActiveServerHolder,
    private val socketIOManager: SocketIOManager,
    private val presenceTracker: PresenceTracker
) : ViewModel() {

    private val _state = MutableStateFlow(ChannelUiState())
    val state: StateFlow<ChannelUiState> = _state.asStateFlow()

    private var socketEventsJob: Job? = null
    private var connectionJob: Job? = null
    private var currentServerId: String? = null
    private var dmsLoaded = CompletableDeferred<Boolean>()

    init {
        observeSocketEvents()
        observeConnection()
    }

    private fun observeConnection() {
        connectionJob = viewModelScope.launch {
            socketIOManager.connectionState.collect { state ->
                if (state == SocketIOManager.ConnectionState.CONNECTED) {
                    presenceTracker.clear()
                    val serverId = activeServerHolder.serverId ?: return@collect
                    agentRepository.getAgents(serverId).onSuccess { agents ->
                        agents.forEach { agent ->
                            if (agent.status == "active") {
                                presenceTracker.setOnline(agent.id.orEmpty())
                            }
                        }
                        _state.update { it.copy(onlineIds = presenceTracker.onlineIds.value) }
                    }

                    if (_state.value.serverId == serverId) {
                        loadChannels(serverId)
                        loadDMs()
                    }
                }
            }
        }
    }

    private fun observeSocketEvents() {
        socketEventsJob = viewModelScope.launch {
            socketIOManager.events.collect { event ->
                when (event) {
                    is SocketIOManager.SocketEvent.UserPresence -> {
                        if (event.status == "online") {
                            presenceTracker.setOnline(event.userId)
                        } else {
                            presenceTracker.setOffline(event.userId)
                        }
                        _state.update { it.copy(onlineIds = presenceTracker.onlineIds.value) }
                    }
                    is SocketIOManager.SocketEvent.AgentActivity -> {
                        presenceTracker.setOnline(event.data.agentId)
                        _state.update { it.copy(onlineIds = presenceTracker.onlineIds.value) }
                    }
                    is SocketIOManager.SocketEvent.ChannelUpdated -> {
                        val serverId = currentServerId ?: activeServerHolder.serverId ?: return@collect
                        if (event.data.type == "dm") {
                            loadDMs()
                        } else {
                            loadChannels(serverId)
                        }
                    }
                    is SocketIOManager.SocketEvent.DMNew -> {
                        val dmId = event.data.id
                        val alreadyExists = _state.value.dms.any { it.id == dmId }
                        if (!alreadyExists) {
                            socketIOManager.joinChannel(dmId)
                            refreshDMs()
                        }
                    }
                    is SocketIOManager.SocketEvent.MessageNew -> {
                        _state.update { current ->
                            val knownChannelIds = (current.channels + current.dms).mapNotNull { it.id }.toSet()
                            if (event.data.channelId !in knownChannelIds) {
                                current
                            } else {
                                current.copy(
                                    channelPreviews = current.channelPreviews + (
                                        event.data.channelId to Message(
                                            id = event.data.id,
                                            channelId = event.data.channelId,
                                            content = event.data.content,
                                            senderId = event.data.senderId,
                                            senderName = event.data.senderName,
                                            senderType = event.data.senderType,
                                            createdAt = event.data.createdAt
                                        )
                                    )
                                )
                            }
                        }
                    }
                    is SocketIOManager.SocketEvent.MessageUpdated -> {
                        _state.update { current ->
                            val existingPreview = current.channelPreviews[event.data.channelId]
                            if (existingPreview == null || existingPreview.id != event.data.id) {
                                current
                            } else {
                                current.copy(
                                    channelPreviews = current.channelPreviews + (
                                        event.data.channelId to existingPreview.copy(
                                            content = event.data.content.ifBlank { existingPreview.content }
                                        )
                                    )
                                )
                            }
                        }
                    }
                    else -> { /* ignore */ }
                }
            }
        }
    }

    fun ensureDMsLoaded() {
        if (!dmsLoaded.isCompleted) {
            loadDMs()
        } else if (dmsLoaded.getCompleted() == false) {
            dmsLoaded = CompletableDeferred()
            loadDMs()
        }
    }

    private fun refreshDMs() {
        val serverId = activeServerHolder.serverId ?: return
        dmsLoaded = CompletableDeferred()
        viewModelScope.launch {
            channelRepository.getDMs(serverId).fold(
                onSuccess = { dms ->
                    _state.update { it.copy(dms = dms) }
                    dmsLoaded.complete(true)
                },
                onFailure = { dmsLoaded.complete(false) }
            )
        }
    }

    fun loadChannels(serverId: String) {
        currentServerId = serverId
        activeServerHolder.serverId = serverId
        socketIOManager.connect(serverId)
        viewModelScope.launch {
            _state.update { it.copy(serverId = serverId, isLoading = it.channels.isEmpty()) }
            channelRepository.getChannels(serverId).fold(
                onSuccess = { channels ->
                    _state.update { it.copy(channels = channels, isLoading = false) }
                    loadChannelPreviews(channels)
                },
                onFailure = { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
            )
            channelRepository.refreshChannels(serverId).fold(
                onSuccess = { channels ->
                    _state.update { it.copy(channels = channels) }
                    loadChannelPreviews(channels)
                },
                onFailure = { /* keep cached data */ }
            )
        }
    }

    fun createChannel(name: String, type: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            channelRepository.createChannel(serverId, name, type).fold(
                onSuccess = { channel -> _state.update { it.copy(channels = it.channels + channel) } },
                onFailure = { err -> _state.update { it.copy(error = err.message) } }
            )
        }
    }

    fun loadDMs() {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDmLoading = it.dms.isEmpty()) }
            channelRepository.getDMs(serverId).fold(
                onSuccess = { dms ->
                    _state.update { it.copy(dms = dms, isDmLoading = false) }
                    dmsLoaded.complete(true)
                },
                onFailure = { err ->
                    _state.update { it.copy(isDmLoading = false, error = err.message) }
                    dmsLoaded.complete(false)
                }
            )
            agentRepository.getAgents(serverId).onSuccess { agents ->
                agents.forEach { agent ->
                    if (agent.status == "active") {
                        presenceTracker.setOnline(agent.id.orEmpty())
                    }
                }
                _state.update { it.copy(onlineIds = presenceTracker.onlineIds.value) }
            }
        }
    }

    fun createDM(agentId: String? = null, userId: String? = null, onSuccess: (Channel) -> Unit, onError: (String) -> Unit = {}) {
        val serverId = activeServerHolder.serverId ?: return

        viewModelScope.launch {
            ensureDMsLoaded()
            val loaded = dmsLoaded.await()
            if (!loaded) {
                onError("Unable to load existing conversations — please try again")
                return@launch
            }

            val existingDm = findExistingDM(agentId, userId)
            if (existingDm != null) {
                onSuccess(existingDm)
                return@launch
            }

            channelRepository.createDM(serverId, agentId = agentId, userId = userId).fold(
                onSuccess = { dmChannel ->
                    _state.update { current ->
                        val exists = current.dms.any { it.id == dmChannel.id }
                        if (!exists) current.copy(dms = current.dms + dmChannel) else current
                    }
                    socketIOManager.joinChannel(dmChannel.id.orEmpty())
                    onSuccess(dmChannel)
                },
                onFailure = { err ->
                    _state.update { it.copy(error = err.message) }
                    onError(err.message ?: "Failed to create DM")
                }
            )
        }
    }

    internal fun findExistingDM(agentId: String?, userId: String?): Channel? {
        return _state.value.dms.firstOrNull { dm ->
            dm.members?.any { member ->
                (agentId != null && member.agentId == agentId) ||
                    (userId != null && member.userId == userId)
            } == true
        }
    }

    fun updateChannel(channelId: String, newName: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            channelRepository.updateChannel(serverId, channelId, newName).fold(
                onSuccess = { updated ->
                    _state.update { current ->
                        current.copy(
                            channels = current.channels.map { if (it.id == channelId) updated else it },
                            actionFeedbackMessage = "Channel renamed"
                        )
                    }
                },
                onFailure = { err ->
                    _state.update { it.copy(actionFeedbackMessage = "Rename failed: ${err.message}") }
                }
            )
        }
    }

    fun deleteChannel(channelId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            channelRepository.deleteChannel(serverId, channelId).fold(
                onSuccess = {
                    _state.update { current ->
                        current.copy(
                            channels = current.channels.filter { it.id != channelId },
                            actionFeedbackMessage = "Channel deleted"
                        )
                    }
                },
                onFailure = { err ->
                    _state.update { it.copy(actionFeedbackMessage = "Delete failed: ${err.message}") }
                }
            )
        }
    }

    fun leaveChannel(channelId: String) {
        val serverId = activeServerHolder.serverId ?: return
        viewModelScope.launch {
            channelRepository.leaveChannel(serverId, channelId).fold(
                onSuccess = {
                    _state.update { current ->
                        current.copy(
                            channels = current.channels.filter { it.id != channelId },
                            actionFeedbackMessage = "Left channel"
                        )
                    }
                },
                onFailure = { err ->
                    _state.update { it.copy(actionFeedbackMessage = "Leave failed: ${err.message}") }
                }
            )
        }
    }

    fun consumeActionFeedback() {
        _state.update { it.copy(actionFeedbackMessage = null) }
    }

    private fun loadChannelPreviews(channels: List<Channel>) {
        viewModelScope.launch {
            val channelIds = channels.mapNotNull { it.id }
            if (channelIds.isEmpty()) return@launch

            val cached = messageRepository.getLatestMessagePerChannel(channelIds)
            if (cached.isNotEmpty()) {
                _state.update { it.copy(channelPreviews = it.channelPreviews + cached) }
            }

            val missingIds = channelIds.filter { it !in cached }
            if (missingIds.isEmpty()) return@launch

            val serverId = _state.value.serverId
            val apiPreviews = mutableMapOf<String, Message>()
            for (channelId in missingIds) {
                messageRepository.refreshMessages(serverId, channelId, limit = 1).onSuccess { messages ->
                    messages.lastOrNull()?.let { apiPreviews[channelId] = it }
                }
            }
            if (apiPreviews.isNotEmpty()) {
                _state.update { it.copy(channelPreviews = it.channelPreviews + apiPreviews) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketEventsJob?.cancel()
        connectionJob?.cancel()
    }
}
