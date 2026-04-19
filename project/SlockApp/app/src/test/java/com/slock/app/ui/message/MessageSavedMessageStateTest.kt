package com.slock.app.ui.message

import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.model.Message
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.repository.TaskRepository
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MessageSavedMessageStateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageRepository: MessageRepository = mock()
    private val taskRepository: TaskRepository = mock()
    private val channelRepository: ChannelRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val activeServerHolder: ActiveServerHolder = mock()
    private val presenceTracker = PresenceTracker()

    @Before
    fun stubBaseState() {
        runBlocking {
            whenever(socketIOManager.events).thenReturn(emptyFlow())
            whenever(activeServerHolder.serverId).thenReturn("server-1")
            whenever(messageRepository.isCachedMessagesFresh(any(), any())).thenReturn(true)
        }
    }

    @Test
    fun loadMessages_exposesSavedMessageIdsFromRepository() = runTest {
        val messages = listOf(
            Message(id = "message-1", channelId = "channel-1", content = "first"),
            Message(id = "message-2", channelId = "channel-1", content = "second")
        )
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(messages))
        whenever(channelRepository.checkSavedMessages("server-1", listOf("message-2", "message-1")))
            .thenReturn(Result.success(listOf("message-2")))

        val viewModel = MessageViewModel(messageRepository, taskRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)

        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        assertEquals(setOf("message-2"), viewModel.state.value.savedMessageIds)
        verify(channelRepository).checkSavedMessages("server-1", listOf("message-2", "message-1"))
    }

    @Test
    fun toggleSavedMessage_savesWhenMessageIsNotSaved() = runTest {
        val message = Message(id = "message-1", channelId = "channel-1", content = "hello")
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(listOf(message)))
        whenever(channelRepository.checkSavedMessages("server-1", listOf("message-1")))
            .thenReturn(Result.success(emptyList()))
        whenever(channelRepository.saveMessage("server-1", "message-1")).thenReturn(Result.success(Unit))

        val viewModel = MessageViewModel(messageRepository, taskRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        viewModel.toggleSavedMessage(message)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.savedMessageIds.contains("message-1"))
        assertFalse(viewModel.state.value.savingMessageIds.contains("message-1"))
        verify(channelRepository).saveMessage("server-1", "message-1")
    }

    @Test
    fun toggleSavedMessage_rollsBackWhenSaveFails() = runTest {
        val message = Message(id = "message-1", channelId = "channel-1", content = "hello")
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(listOf(message)))
        whenever(channelRepository.checkSavedMessages("server-1", listOf("message-1")))
            .thenReturn(Result.success(emptyList()))
        whenever(channelRepository.saveMessage("server-1", "message-1"))
            .thenReturn(Result.failure(IllegalStateException("save failed")))

        val viewModel = MessageViewModel(messageRepository, taskRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        viewModel.toggleSavedMessage(message)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.savedMessageIds.contains("message-1"))
        assertFalse(viewModel.state.value.savingMessageIds.contains("message-1"))
        assertEquals("save failed", viewModel.state.value.savedMessageFeedbackMessage)

        viewModel.consumeSavedMessageFeedback()

        assertEquals(null, viewModel.state.value.savedMessageFeedbackMessage)
        verify(channelRepository).saveMessage("server-1", "message-1")
    }

    @Test
    fun toggleSavedMessage_removesWhenMessageIsAlreadySaved() = runTest {
        val message = Message(id = "message-1", channelId = "channel-1", content = "hello")
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(listOf(message)))
        whenever(channelRepository.checkSavedMessages("server-1", listOf("message-1")))
            .thenReturn(Result.success(listOf("message-1")))
        whenever(channelRepository.removeSavedMessage("server-1", "message-1")).thenReturn(Result.success(Unit))

        val viewModel = MessageViewModel(messageRepository, taskRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        viewModel.toggleSavedMessage(message)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.savedMessageIds.contains("message-1"))
        assertFalse(viewModel.state.value.savingMessageIds.contains("message-1"))
        verify(channelRepository).removeSavedMessage("server-1", "message-1")
    }

    @Test
    fun toggleSavedMessage_rollsBackWhenRemoveFails() = runTest {
        val message = Message(id = "message-1", channelId = "channel-1", content = "hello")
        whenever(messageRepository.getMessages("server-1", "channel-1", 50, null, null))
            .thenReturn(Result.success(listOf(message)))
        whenever(channelRepository.checkSavedMessages("server-1", listOf("message-1")))
            .thenReturn(Result.success(listOf("message-1")))
        whenever(channelRepository.removeSavedMessage("server-1", "message-1"))
            .thenReturn(Result.failure(IllegalStateException("remove failed")))

        val viewModel = MessageViewModel(messageRepository, taskRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
        viewModel.loadMessages("channel-1")
        advanceUntilIdle()

        viewModel.toggleSavedMessage(message)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.savedMessageIds.contains("message-1"))
        assertFalse(viewModel.state.value.savingMessageIds.contains("message-1"))
        assertEquals("remove failed", viewModel.state.value.savedMessageFeedbackMessage)

        viewModel.consumeSavedMessageFeedback()

        assertEquals(null, viewModel.state.value.savedMessageFeedbackMessage)
        verify(channelRepository).removeSavedMessage("server-1", "message-1")
    }
}
