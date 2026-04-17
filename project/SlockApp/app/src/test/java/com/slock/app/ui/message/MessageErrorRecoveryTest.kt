package com.slock.app.ui.message

import com.slock.app.data.model.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
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
