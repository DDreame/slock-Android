package com.slock.app.ui.message

import com.slock.app.data.api.ApiService
import com.slock.app.data.local.ActiveServerHolder
import com.slock.app.data.local.PresenceTracker
import com.slock.app.data.local.dao.MessageDao
import com.slock.app.data.model.Message
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.repository.MessageRepositoryImpl
import com.slock.app.data.socket.SocketIOManager
import com.slock.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class MessageErrorRecoveryStateTest {

    @Test
    fun `MessageUiState has sendError field defaulting to null`() {
        val state = MessageUiState()
        assertNull(state.sendError)
    }

    @Test
    fun `sendError is independent of error`() {
        val state = MessageUiState(error = null, sendError = "Send failed")
        assertNull(state.error)
        assertEquals("Send failed", state.sendError)
    }

    @Test
    fun `error does not hide messages when messages exist`() {
        val state = MessageUiState(
            messages = listOf(Message(id = "m1", content = "hello")),
            error = "some error"
        )
        assertTrue(state.messages.isNotEmpty())
        assertNotNull(state.error)
    }

    @Test
    fun `sendError can be cleared without affecting error`() {
        val state = MessageUiState(error = "load failed", sendError = "send failed")
        val cleared = state.copy(sendError = null)
        assertEquals("load failed", cleared.error)
        assertNull(cleared.sendError)
    }

    @Test
    fun `full-page error only applies when messages are empty`() {
        val emptyState = MessageUiState(messages = emptyList(), error = "load failed")
        val populatedState = MessageUiState(
            messages = listOf(Message(id = "m1", content = "hello")),
            error = "load failed"
        )
        assertTrue(emptyState.messages.isEmpty() && emptyState.error != null)
        assertFalse(populatedState.messages.isEmpty() && populatedState.error != null)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MessageErrorRecoveryExecutionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageRepository: MessageRepository = mock()
    private val channelRepository: ChannelRepository = mock()
    private val socketIOManager: SocketIOManager = mock()
    private val activeServerHolder: ActiveServerHolder = mock()
    private val presenceTracker = PresenceTracker()

    private fun createViewModel(): MessageViewModel {
        whenever(socketIOManager.events).thenReturn(emptyFlow())
        return MessageViewModel(messageRepository, channelRepository, socketIOManager, activeServerHolder, presenceTracker)
    }

    // Issue 1: retryLoadMessages actually re-calls loadMessages from error state
    @Test
    fun `retryLoadMessages re-fetches from error state with empty messages`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null))
            .thenReturn(Result.failure(Exception("network error")))
            .thenReturn(Result.success(listOf(Message(id = "m1", content = "hello", seq = 1))))
        whenever(messageRepository.refreshMessages("srv-1", "ch-1", 50))
            .thenReturn(Result.failure(Exception("network error")))
            .thenReturn(Result.success(listOf(Message(id = "m1", content = "hello", seq = 1))))

        val vm = createViewModel()

        vm.loadMessages("ch-1")
        advanceUntilIdle()

        assertNotNull("First load should fail and set error", vm.state.value.error)
        assertTrue("Messages should be empty after failed load", vm.state.value.messages.isEmpty())

        vm.retryLoadMessages()
        advanceUntilIdle()

        assertNull("Error should be cleared after successful retry", vm.state.value.error)
        assertEquals(1, vm.state.value.messages.size)
        verify(messageRepository, times(2)).getMessages("srv-1", "ch-1", 50, null, null)
    }

    // Issue 2: sendMessage failure sets sendError, preserves existing messages
    @Test
    fun `sendMessage failure sets sendError not error and preserves messages`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null))
            .thenReturn(Result.success(listOf(Message(id = "m1", content = "existing", seq = 1))))
        whenever(messageRepository.refreshMessages("srv-1", "ch-1", 50))
            .thenReturn(Result.success(listOf(Message(id = "m1", content = "existing", seq = 1))))
        whenever(messageRepository.sendMessage(eq("srv-1"), eq("ch-1"), eq("test msg"), any(), any(), any()))
            .thenReturn(Result.failure(Exception("send failed")))

        val vm = createViewModel()

        vm.loadMessages("ch-1")
        advanceUntilIdle()

        assertEquals(1, vm.state.value.messages.size)
        assertNull(vm.state.value.error)

        vm.sendMessage("test msg")
        advanceUntilIdle()

        assertNull("error must remain null after send failure", vm.state.value.error)
        assertEquals("send failed", vm.state.value.sendError)
        assertTrue(
            "existing messages must be preserved after send failure",
            vm.state.value.messages.any { it.id == "m1" }
        )
    }

    @Test
    fun `dismissSendError clears sendError`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null))
            .thenReturn(Result.success(listOf(Message(id = "m1", content = "hello", seq = 1))))
        whenever(messageRepository.refreshMessages("srv-1", "ch-1", 50))
            .thenReturn(Result.success(listOf(Message(id = "m1", content = "hello", seq = 1))))
        whenever(messageRepository.sendMessage(eq("srv-1"), eq("ch-1"), eq("fail"), any(), any(), any()))
            .thenReturn(Result.failure(Exception("oops")))

        val vm = createViewModel()
        vm.loadMessages("ch-1")
        advanceUntilIdle()
        vm.sendMessage("fail")
        advanceUntilIdle()

        assertNotNull(vm.state.value.sendError)

        vm.dismissSendError()

        assertNull(vm.state.value.sendError)
    }

    // Issue 3: raw-array pagination — verify getMessages passes before param through
    @Test
    fun `loadMoreMessages passes before cursor to repository`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null))
            .thenReturn(Result.success(listOf(Message(id = "m1", content = "msg", seq = 100))))
        whenever(messageRepository.refreshMessages("srv-1", "ch-1", 50))
            .thenReturn(Result.success(listOf(Message(id = "m1", content = "msg", seq = 100))))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, "100", null))
            .thenReturn(Result.success(listOf(Message(id = "m2", content = "older", seq = 50))))

        val vm = createViewModel()
        vm.loadMessages("ch-1")
        advanceUntilIdle()

        vm.loadMoreMessages()
        advanceUntilIdle()

        verify(messageRepository).getMessages("srv-1", "ch-1", 50, "100", null)
        assertEquals(2, vm.state.value.messages.size)
    }
}

class MessageRepositoryRawFallbackTest {

    private val apiService: ApiService = mock()
    private val activeServerHolder: ActiveServerHolder = mock()
    private val messageDao: MessageDao = mock()
    private lateinit var logMock: org.mockito.MockedStatic<android.util.Log>

    private lateinit var repo: MessageRepositoryImpl

    @org.junit.Before
    fun setup() {
        logMock = org.mockito.Mockito.mockStatic(android.util.Log::class.java)
        repo = MessageRepositoryImpl(apiService, activeServerHolder, messageDao)
    }

    @org.junit.After
    fun tearDown() {
        logMock.close()
    }

    @Test
    fun `raw fallback receives before when wrapped returns empty`() = runTest {
        whenever(apiService.getMessages("ch-1", 50, "cursor-100", null))
            .thenReturn(retrofit2.Response.success(com.slock.app.data.model.MessagesResponse(messages = emptyList())))
        whenever(apiService.getMessagesRaw("ch-1", 50, "cursor-100", null))
            .thenReturn(retrofit2.Response.success(listOf(Message(id = "m1", content = "older", seq = 50))))

        val result = repo.getMessages("srv-1", "ch-1", 50, before = "cursor-100", after = null)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
        verify(apiService).getMessagesRaw("ch-1", 50, "cursor-100", null)
    }

    @Test
    fun `raw fallback receives before and after when wrapped throws`() = runTest {
        whenever(apiService.getMessages("ch-1", 50, "cursor-50", null))
            .thenThrow(RuntimeException("deserialization error"))
        whenever(apiService.getMessagesRaw("ch-1", 50, "cursor-50", null))
            .thenReturn(retrofit2.Response.success(listOf(Message(id = "m2", content = "page2", seq = 30))))

        val result = repo.getMessages("srv-1", "ch-1", 50, before = "cursor-50", after = null)

        assertTrue(result.isSuccess)
        verify(apiService).getMessagesRaw("ch-1", 50, "cursor-50", null)
    }

    @Test
    fun `raw fallback forwards after param for forward pagination`() = runTest {
        whenever(apiService.getMessages("ch-1", 50, null, "cursor-200"))
            .thenReturn(retrofit2.Response.success(com.slock.app.data.model.MessagesResponse(messages = emptyList())))
        whenever(apiService.getMessagesRaw("ch-1", 50, null, "cursor-200"))
            .thenReturn(retrofit2.Response.success(listOf(Message(id = "m3", content = "newer", seq = 250))))

        val result = repo.getMessages("srv-1", "ch-1", 50, before = null, after = "cursor-200")

        assertTrue(result.isSuccess)
        verify(apiService).getMessagesRaw("ch-1", 50, null, "cursor-200")
    }
}

class MessageErrorRecoveryStructuralTest {

    private val vmSource: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageViewModel.kt")
    ).first { it.exists() }.readText()

    private val screenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt")
    ).first { it.exists() }.readText()

    private val apiSource: String = listOf(
        File("src/main/java/com/slock/app/data/api/ApiService.kt"),
        File("app/src/main/java/com/slock/app/data/api/ApiService.kt")
    ).first { it.exists() }.readText()

    private val repoSource: String = listOf(
        File("src/main/java/com/slock/app/data/repository/MessageRepository.kt"),
        File("app/src/main/java/com/slock/app/data/repository/MessageRepository.kt")
    ).first { it.exists() }.readText()

    // Issue 1: Retry dead path — retryLoadMessages must exist and re-call loadMessages
    @Test
    fun `retryLoadMessages function exists in ViewModel`() {
        assertTrue(
            "ViewModel must have retryLoadMessages() for error retry",
            vmSource.contains("fun retryLoadMessages()")
        )
    }

    @Test
    fun `retryLoadMessages calls loadMessages with current channelId`() {
        val retryBlock = vmSource
            .substringAfter("fun retryLoadMessages()")
            .substringBefore("fun loadMoreMessages()")
        assertTrue(
            "retryLoadMessages must call loadMessages to actually reload",
            retryBlock.contains("loadMessages(")
        )
    }

    @Test
    fun `screen error state uses onRetryLoad not onLoadMore`() {
        val errorBlock = screenSource
            .substringAfter("消息加载失败")
            .substringBefore("state.messages.isEmpty()")
        assertTrue(
            "Error retry must call onRetryLoad, not onLoadMore",
            errorBlock.contains("onRetryLoad()")
        )
        assertFalse(
            "Error retry must NOT call onLoadMore (dead path when messages empty)",
            errorBlock.contains("onLoadMore()")
        )
    }

    @Test
    fun `screen full-page error only shows when messages are empty`() {
        assertTrue(
            "Error state must be gated by state.messages.isEmpty() to avoid hiding loaded messages",
            screenSource.contains("state.error != null && state.messages.isEmpty()")
        )
    }

    // Issue 2: send/upload error separation
    @Test
    fun `MessageUiState has sendError field`() {
        assertTrue(
            "MessageUiState must have sendError field separate from error",
            vmSource.contains("val sendError: String? = null")
        )
    }

    @Test
    fun `sendMessage sets sendError not error on failure`() {
        val sendBlock = vmSource
            .substringAfter("fun sendMessage(content: String)")
            .substringBefore("fun addAttachment(")
        assertTrue(
            "sendMessage must use sendError for send/upload failures",
            sendBlock.contains("sendError =")
        )
        assertFalse(
            "sendMessage must NOT set error (which triggers full-page error)",
            sendBlock.contains("error = error.message") || sendBlock.contains("error = \"图片上传失败")
        )
    }

    @Test
    fun `screen shows sendError as toast`() {
        assertTrue(
            "Screen must display sendError via Toast",
            screenSource.contains("state.sendError") && screenSource.contains("Toast")
        )
    }

    @Test
    fun `dismissSendError function exists in ViewModel`() {
        assertTrue(
            "ViewModel must have dismissSendError() to clear sendError after display",
            vmSource.contains("fun dismissSendError()")
        )
    }

    // Issue 3: raw-array fallback pagination params
    @Test
    fun `getMessagesRaw has before parameter`() {
        val rawMethodBlock = apiSource
            .substringAfter("fun getMessagesRaw(")
            .substringBefore("): Response<List<Message>>")
        assertTrue(
            "getMessagesRaw must accept before param for pagination",
            rawMethodBlock.contains("before: String?")
        )
    }

    @Test
    fun `getMessagesRaw has after parameter`() {
        val rawMethodBlock = apiSource
            .substringAfter("fun getMessagesRaw(")
            .substringBefore("): Response<List<Message>>")
        assertTrue(
            "getMessagesRaw must accept after param for pagination",
            rawMethodBlock.contains("after: String?")
        )
    }

    @Test
    fun `fetchMessages passes before and after to raw fallback`() {
        val fallbackBlock = repoSource
            .substringAfter("Fallback: plain array")
            .substringBefore("return null")
        assertTrue(
            "Raw fallback must forward before param for pagination",
            fallbackBlock.contains("before") && fallbackBlock.contains("after")
        )
    }
}
