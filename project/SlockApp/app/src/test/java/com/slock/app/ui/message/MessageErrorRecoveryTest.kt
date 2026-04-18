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
import kotlinx.coroutines.CompletableDeferred
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
import org.mockito.kotlin.anyOrNull
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

    private fun createViewModel(
        repository: MessageRepository = messageRepository,
        channels: ChannelRepository = channelRepository,
        socketManager: SocketIOManager = socketIOManager
    ): MessageViewModel {
        whenever(socketManager.events).thenReturn(emptyFlow())
        return MessageViewModel(repository, channels, socketManager, activeServerHolder, presenceTracker)
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

    @Test
    fun `empty DM opens to empty state`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "dm-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "dm-1", 50, null, null))
            .thenReturn(Result.success(emptyList()))
        whenever(messageRepository.refreshMessages("srv-1", "dm-1", 50))
            .thenReturn(Result.success(emptyList()))

        val vm = createViewModel()

        vm.loadMessages("dm-1")
        advanceUntilIdle()

        assertTrue("Empty DM should keep empty messages list", vm.state.value.messages.isEmpty())
        assertNull("Empty DM should not surface full-page load error", vm.state.value.error)
    }

    @Test
    fun `existing DM with non empty history opens successfully`() = runTest {
        val channelId = "dm/user:1/user:2"
        val history = listOf(Message(id = "m-1", channelId = channelId, content = "hello", seq = 1))

        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", channelId)).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", channelId, 50, null, null))
            .thenReturn(Result.success(history))
        whenever(messageRepository.refreshMessages("srv-1", channelId, 50))
            .thenReturn(Result.success(history))

        val vm = createViewModel()

        vm.loadMessages(channelId)
        advanceUntilIdle()

        assertEquals(channelId, vm.state.value.channelId)
        assertEquals(1, vm.state.value.messages.size)
        assertEquals("hello", vm.state.value.messages.single().content)
        assertNull("Existing-history DM should load without full-page error", vm.state.value.error)
    }

    @Test
    fun `failed first load then successful empty refresh clears error`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null))
            .thenReturn(Result.failure(Exception("network error")))
        whenever(messageRepository.refreshMessages("srv-1", "ch-1", 50))
            .thenReturn(Result.success(emptyList()))

        val vm = createViewModel()

        vm.loadMessages("ch-1")
        advanceUntilIdle()

        assertTrue(vm.state.value.messages.isEmpty())
        assertNull("Successful empty refresh must clear previous load error", vm.state.value.error)
    }

    @Test
    fun `fresh cache hit skips unconditional refresh`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")
        whenever(channelRepository.isChannelSaved("srv-1", "ch-1")).thenReturn(Result.success(false))
        whenever(messageRepository.getMessages("srv-1", "ch-1", 50, null, null))
            .thenReturn(Result.success(listOf(Message(id = "m1", content = "cached", seq = 1))))
        whenever(messageRepository.isCachedMessagesFresh(eq("ch-1"), any()))
            .thenReturn(true)

        val vm = createViewModel()

        vm.loadMessages("ch-1")
        advanceUntilIdle()

        verify(messageRepository, times(0)).refreshMessages("srv-1", "ch-1", 50)
        assertEquals(1, vm.state.value.messages.size)
        assertEquals("cached", vm.state.value.messages.single().content)
    }

    @Test
    fun `stale load from previous channel does not override new channel state`() = runTest {
        whenever(activeServerHolder.serverId).thenReturn("srv-1")

        val socketManager: SocketIOManager = mock()
        whenever(socketManager.events).thenReturn(emptyFlow())

        val channels: ChannelRepository = mock()
        whenever(channels.isChannelSaved("srv-1", "old-channel")).thenReturn(Result.success(false))
        whenever(channels.isChannelSaved("srv-1", "new-channel")).thenReturn(Result.success(false))

        val repository = ControlledMessageRepository().apply {
            cachedFreshness["old-channel"] = false
            cachedFreshness["new-channel"] = true
            getMessagesResults["old-channel"] = Result.success(
                listOf(Message(id = "old-cached", channelId = "old-channel", content = "old cached", seq = 1))
            )
            getMessagesResults["new-channel"] = Result.success(
                listOf(Message(id = "new-live", channelId = "new-channel", content = "new live", seq = 10))
            )
            refreshResults["old-channel"] = CompletableDeferred()
        }

        val vm = createViewModel(repository, channels, socketManager)

        vm.loadMessages("old-channel")
        advanceUntilIdle()

        vm.loadMessages("new-channel")
        advanceUntilIdle()

        assertEquals("new-channel", vm.state.value.channelId)
        assertEquals("new live", vm.state.value.messages.single().content)

        repository.refreshResults.getValue("old-channel")
            .complete(Result.success(listOf(Message(id = "old-live", channelId = "old-channel", content = "old live", seq = 2))))
        advanceUntilIdle()

        assertEquals("new-channel", vm.state.value.channelId)
        assertEquals(1, vm.state.value.messages.size)
        assertEquals("new live", vm.state.value.messages.single().content)
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
        whenever(messageRepository.sendMessage(any(), any(), any(), anyOrNull(), any(), anyOrNull()))
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
        whenever(messageRepository.sendMessage(any(), any(), any(), anyOrNull(), any(), anyOrNull()))
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

private class ControlledMessageRepository : MessageRepository {
    val getMessagesResults = mutableMapOf<String, Result<List<Message>>>()
    val refreshResults = mutableMapOf<String, CompletableDeferred<Result<List<Message>>>>()
    val cachedFreshness = mutableMapOf<String, Boolean>()

    override suspend fun sendMessage(serverId: String, channelId: String, content: String, attachmentIds: List<String>?, asTask: Boolean, parentMessageId: String?) =
        Result.failure<Message>(NotImplementedError())

    override suspend fun getMessages(serverId: String, channelId: String, limit: Int, before: String?, after: String?): Result<List<Message>> =
        getMessagesResults[channelId] ?: Result.success(emptyList())

    override suspend fun refreshMessages(serverId: String, channelId: String, limit: Int): Result<List<Message>> =
        refreshResults[channelId]?.await() ?: Result.success(emptyList())

    override suspend fun isCachedMessagesFresh(channelId: String, maxAgeMs: Long): Boolean =
        cachedFreshness[channelId] ?: false

    override suspend fun searchMessages(serverId: String, query: String, searchServerId: String?, channelId: String?) =
        Result.success(emptyList<Message>())

    override suspend fun getLatestMessagePerChannel(channelIds: List<String>) = emptyMap<String, Message>()

    override suspend fun uploadFile(serverId: String, fileName: String, mimeType: String, bytes: ByteArray) =
        Result.failure<UploadResponse>(NotImplementedError())
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
    fun `wrapped empty response does not require raw fallback`() = runTest {
        whenever(apiService.getMessages("ch-1", 50, "cursor-100", null))
            .thenReturn(retrofit2.Response.success(com.slock.app.data.model.MessagesResponse(messages = emptyList())))

        val result = repo.getMessages("srv-1", "ch-1", 50, before = "cursor-100", after = null)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
        verify(apiService, times(0)).getMessagesRaw(any(), any(), anyOrNull(), anyOrNull())
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
    fun `raw fallback forwards after param for forward pagination when wrapped throws`() = runTest {
        whenever(apiService.getMessages("ch-1", 50, null, "cursor-200"))
            .thenThrow(RuntimeException("deserialization error"))
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
            .substringAfter("Fallback only when wrapped parsing/shape handling fails.")
            .substringBefore("return null")
        assertTrue(
            "Raw fallback must forward before param for pagination",
            fallbackBlock.contains("before") && fallbackBlock.contains("after")
        )
    }

    @Test
    fun `wrapped success returns messages directly even when empty`() {
        val wrappedBlock = repoSource
            .substringAfter("val response = apiService.getMessages")
            .substringBefore("if (!response.isSuccessful)")
        assertTrue(
            "Wrapped successful response should return body messages directly, including empty list",
            wrappedBlock.contains("return response.body()!!.messages")
        )
    }

    @Test
    fun `loadMessages success path clears error`() {
        val loadBlock = vmSource
            .substringAfter("fun loadMessages(channelId: String)")
            .substringBefore("fun toggleSavedChannel()")
        assertTrue(
            "loadMessages success branches must clear error for empty successful responses",
            loadBlock.contains("error = null")
        )
    }
}
