package com.slock.app.ui.channel

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.local.SecureTokenStorage
import com.slock.app.data.model.Channel
import com.slock.app.data.model.Message
import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val channelRepository: ChannelRepository = mock()
    private val messageRepository: MessageRepository = mock()
    private val agentRepository: AgentRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val secureTokenStorage: SecureTokenStorage = mock()
    private val activeServerHolder = ActiveServerHolder(secureTokenStorage)
    private val presenceTracker = PresenceTracker()

    @Test
    fun `loadChannels reconnects socket when selected server changes`() = runTest {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(channelRepository.getChannels(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.refreshChannels(any())).thenReturn(Result.success(emptyList()))
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()

        viewModel.loadChannels("server-a")
        advanceUntilIdle()
        viewModel.loadChannels("server-b")
        advanceUntilIdle()

        verify(socketIOManager).connect("server-a")
        verify(socketIOManager).connect("server-b")
    }

    @Test
    fun `channelUpdated event reloads channels for current server`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(channelRepository.getChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "channel-1", name = "General", type = "text")))
        )
        whenever(channelRepository.refreshChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "channel-1", name = "General", type = "text")))
        )
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(messageRepository.refreshMessages(any(), any(), any())).thenReturn(Result.success(emptyList()))
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.ChannelUpdated(
                SocketIOManager.ChannelUpdatedData(
                    id = "channel-1",
                    name = "General 2",
                    type = "text"
                )
            )
        )
        advanceUntilIdle()

        verify(channelRepository, times(2)).getChannels("server-1")
        verify(channelRepository, times(2)).refreshChannels("server-1")
    }

    @Test
    fun `messageNew event updates preview for known channel`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(channelRepository.getChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "channel-1", name = "General", type = "text")))
        )
        whenever(channelRepository.refreshChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "channel-1", name = "General", type = "text")))
        )
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(messageRepository.refreshMessages(any(), any(), any())).thenReturn(Result.success(emptyList()))
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.MessageNew(
                SocketIOManager.MessageNewData(
                    id = "msg-1",
                    channelId = "channel-1",
                    content = "hello from realtime",
                    senderId = "user-1",
                    senderName = "User",
                    senderType = "user",
                    seq = 1,
                    createdAt = "2026-04-17T00:00:00Z"
                )
            )
        )
        advanceUntilIdle()

        val preview = viewModel.state.value.channelPreviews["channel-1"]
        assertEquals("msg-1", preview?.id)
        assertEquals("hello from realtime", preview?.content)
    }

    @Test
    fun `messageUpdated event refreshes preview content for known channel`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(channelRepository.getChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "channel-1", name = "General", type = "text")))
        )
        whenever(channelRepository.refreshChannels("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "channel-1", name = "General", type = "text")))
        )
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(
            mapOf(
                "channel-1" to Message(
                    id = "msg-1",
                    channelId = "channel-1",
                    content = "before update"
                )
            )
        )
        whenever(messageRepository.refreshMessages(any(), any(), any())).thenReturn(Result.success(emptyList()))
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.MessageUpdated(
                SocketIOManager.MessageUpdatedData(
                    id = "msg-1",
                    channelId = "channel-1",
                    content = "after update"
                )
            )
        )
        advanceUntilIdle()

        assertEquals("after update", viewModel.state.value.channelPreviews["channel-1"]?.content)
    }

    @Test
    fun `dmNew event reloads dms for current server`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)
        whenever(socketIOManager.connectionState).thenReturn(emptyFlow())
        whenever(channelRepository.getChannels(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.refreshChannels(any())).thenReturn(Result.success(emptyList()))
        whenever(channelRepository.getDMs("server-1")).thenReturn(
            Result.success(listOf(Channel(id = "dm-1", name = "Direct Message", type = "dm")))
        )
        whenever(messageRepository.getLatestMessagePerChannel(any())).thenReturn(emptyMap())
        whenever(agentRepository.getAgents(any())).thenReturn(Result.success(emptyList()))

        val viewModel = createViewModel()
        viewModel.loadChannels("server-1")
        viewModel.loadDMs()
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.DMNew(
                SocketIOManager.DMNewData(
                    id = "dm-2",
                    name = "New DM",
                    type = "dm"
                )
            )
        )
        advanceUntilIdle()

        verify(channelRepository, times(2)).getDMs("server-1")
    }

    private fun createViewModel(): ChannelViewModel {
        return ChannelViewModel(
            channelRepository = channelRepository,
            messageRepository = messageRepository,
            agentRepository = agentRepository,
            activeServerHolder = activeServerHolder,
            socketIOManager = socketIOManager,
            presenceTracker = presenceTracker
        )
    }
}
