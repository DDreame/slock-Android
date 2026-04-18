package com.slock.app.ui.message

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.repository.ServerRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageRepository: MessageRepository = mock()
    private val channelRepository: ChannelRepository = mock()
    private val serverRepository: ServerRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val activeServerHolder: ActiveServerHolder = mock()
    private val presenceTracker = PresenceTracker()

    @Test
    fun `messageNew with threadChannelId preserves it in state`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)

        whenever(activeServerHolder.serverId).thenReturn(null)

        val viewModel = MessageViewModel(
            messageRepository, channelRepository, serverRepository, socketIOManager, activeServerHolder, presenceTracker
        )
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.MessageNew(
                SocketIOManager.MessageNewData(
                    id = "msg-1",
                    channelId = "channel-1",
                    content = "thread message",
                    senderId = "user-1",
                    senderName = "Alice",
                    senderType = "human",
                    seq = 1L,
                    createdAt = "2026-04-18T00:00:00Z",
                    threadChannelId = "thread-ch-1",
                    replyCount = 5,
                    lastReplyAt = "2026-04-18T01:00:00Z",
                    parentMessageId = null
                )
            )
        )
        advanceUntilIdle()

        val msg = viewModel.state.value.messages.find { it.id == "msg-1" }
        assertNotNull("New message should be in state", msg)
        assertEquals("thread-ch-1", msg!!.threadChannelId)
        assertEquals(5, msg.replyCount)
        assertEquals("2026-04-18T01:00:00Z", msg.lastReplyAt)
    }

    @Test
    fun `messageNew without threadChannelId leaves it null in state`() = runTest {
        val events = MutableSharedFlow<SocketIOManager.SocketEvent>()
        whenever(socketIOManager.events).thenReturn(events)

        whenever(activeServerHolder.serverId).thenReturn(null)

        val viewModel = MessageViewModel(
            messageRepository, channelRepository, serverRepository, socketIOManager, activeServerHolder, presenceTracker
        )
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        events.emit(
            SocketIOManager.SocketEvent.MessageNew(
                SocketIOManager.MessageNewData(
                    id = "msg-2",
                    channelId = "channel-1",
                    content = "normal message",
                    senderId = "user-2",
                    senderName = "Bob",
                    senderType = "human",
                    seq = 2L,
                    createdAt = "2026-04-18T00:00:00Z"
                )
            )
        )
        advanceUntilIdle()

        val msg = viewModel.state.value.messages.find { it.id == "msg-2" }
        assertNotNull("New message should be in state", msg)
        assertEquals(null, msg!!.threadChannelId)
        assertEquals(0, msg.replyCount)
    }
}
