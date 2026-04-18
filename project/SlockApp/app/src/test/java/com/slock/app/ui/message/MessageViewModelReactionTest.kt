package com.slock.app.ui.message

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.model.Message
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.repository.ServerRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MessageViewModelReactionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageRepository: MessageRepository = mock()
    private val channelRepository: ChannelRepository = mock()
    private val serverRepository: ServerRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val activeServerHolder: ActiveServerHolder = mock()
    private val presenceTracker = PresenceTracker()

    @Test
    fun toggleReaction_updatesLocalReactionOverrides() = runTest {
        whenever(socketIOManager.events).thenReturn(emptyFlow())

        val viewModel = MessageViewModel(messageRepository, channelRepository, serverRepository, socketIOManager, activeServerHolder, presenceTracker)
        val message = Message(id = "msg-1", content = "hello")

        viewModel.toggleReaction(message, "👍")

        assertEquals(
            listOf(MessageReactionUiModel(emoji = "👍", count = 1, isSelected = true)),
            viewModel.state.value.reactionOverridesByMessageId["msg-1"]
        )
    }

    @Test
    fun toggleReaction_ignoresMessagesWithoutId() = runTest {
        whenever(socketIOManager.events).thenReturn(emptyFlow())

        val viewModel = MessageViewModel(messageRepository, channelRepository, serverRepository, socketIOManager, activeServerHolder, presenceTracker)

        viewModel.toggleReaction(Message(id = null, content = "draft"), "👍")

        assertTrue(viewModel.state.value.reactionOverridesByMessageId.isEmpty())
    }

    @Test
    fun messageUpdatedEvent_refreshesVisibleMessageContent() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)
        whenever(activeServerHolder.serverId).thenReturn("server-1")
        whenever(channelRepository.isChannelSaved("server-1", "channel-1")).thenReturn(Result.success(false))

        val existing = Message(id = "msg-1", channelId = "channel-1", content = "before")
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(listOf(existing)))
        whenever(messageRepository.refreshMessages("server-1", "channel-1", 50))
            .thenReturn(Result.success(listOf(existing)))

        val viewModel = MessageViewModel(messageRepository, channelRepository, serverRepository, socketIOManager, activeServerHolder, presenceTracker)
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.MessageUpdated(
                SocketIOManager.MessageUpdatedData(
                    id = "msg-1",
                    channelId = "channel-1",
                    content = "after"
                )
            )
        )
        advanceUntilIdle()

        assertEquals("after", viewModel.state.value.messages.first().content)
    }

    @Test
    fun userPresenceEvent_updatesOnlineIds() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)

        val viewModel = MessageViewModel(messageRepository, channelRepository, serverRepository, socketIOManager, activeServerHolder, presenceTracker)
        advanceUntilIdle()

        events.emit(SocketIOManager.SocketEvent.UserPresence(userId = "user-1", status = "online"))
        advanceUntilIdle()
        assertTrue("user-1" in viewModel.state.value.onlineIds)

        events.emit(SocketIOManager.SocketEvent.UserPresence(userId = "user-1", status = "offline"))
        advanceUntilIdle()
        assertTrue("user-1" !in viewModel.state.value.onlineIds)
    }

    @Test
    fun existingPresence_isExposedOnInit() = runTest {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        presenceTracker.setOnline("agent-1")

        val viewModel = MessageViewModel(messageRepository, channelRepository, serverRepository, socketIOManager, activeServerHolder, presenceTracker)
        advanceUntilIdle()

        assertTrue("agent-1" in viewModel.state.value.onlineIds)
    }

    @Test
    fun agentActivityEvent_marksAgentOnline() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)

        val viewModel = MessageViewModel(messageRepository, channelRepository, serverRepository, socketIOManager, activeServerHolder, presenceTracker)
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.AgentActivity(
                SocketIOManager.AgentActivityData(
                    agentId = "agent-1",
                    activity = "thinking",
                    message = "working"
                )
            )
        )
        advanceUntilIdle()

        assertTrue("agent-1" in viewModel.state.value.onlineIds)
    }
}
