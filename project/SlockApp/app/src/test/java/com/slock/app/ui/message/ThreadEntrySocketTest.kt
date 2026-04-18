package com.slock.app.ui.message

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ThreadEntrySocketTest {

    private val socketSource: String = listOf(
        File("src/main/java/com/slock/app/data/socket/SocketIOManager.kt"),
        File("app/src/main/java/com/slock/app/data/socket/SocketIOManager.kt")
    ).first { it.exists() }.readText()

    private val viewModelSource: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageViewModel.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `MessageNewData includes threadChannelId field`() {
        val dataClass = socketSource.substringAfter("data class MessageNewData(")
            .substringBefore(")")
        assertTrue(
            "MessageNewData must declare threadChannelId",
            dataClass.contains("threadChannelId")
        )
    }

    @Test
    fun `MessageNewData includes replyCount field`() {
        val dataClass = socketSource.substringAfter("data class MessageNewData(")
            .substringBefore(")")
        assertTrue(
            "MessageNewData must declare replyCount",
            dataClass.contains("replyCount")
        )
    }

    @Test
    fun `MessageNewData includes lastReplyAt field`() {
        val dataClass = socketSource.substringAfter("data class MessageNewData(")
            .substringBefore(")")
        assertTrue(
            "MessageNewData must declare lastReplyAt",
            dataClass.contains("lastReplyAt")
        )
    }

    @Test
    fun `MessageNewData includes parentMessageId field`() {
        val dataClass = socketSource.substringAfter("data class MessageNewData(")
            .substringBefore(")")
        assertTrue(
            "MessageNewData must declare parentMessageId",
            dataClass.contains("parentMessageId")
        )
    }

    @Test
    fun `socket message-new handler reads threadChannelId from JSON`() {
        val handler = socketSource.substringAfter("on(\"message:new\")")
            .substringBefore("on(\"message:updated\")")
        assertTrue(
            "message:new handler must read threadChannelId from JSON",
            handler.contains("threadChannelId")
        )
    }

    @Test
    fun `socket message-new handler reads replyCount from JSON`() {
        val handler = socketSource.substringAfter("on(\"message:new\")")
            .substringBefore("on(\"message:updated\")")
        assertTrue(
            "message:new handler must read replyCount from JSON",
            handler.contains("replyCount")
        )
    }

    @Test
    fun `ViewModel passes threadChannelId when constructing Message from socket event`() {
        val messageNewBlock = viewModelSource.substringAfter("is SocketIOManager.SocketEvent.MessageNew")
            .substringBefore("is SocketIOManager.SocketEvent.MessageUpdated")
        assertTrue(
            "ViewModel must pass threadChannelId to Message constructor",
            messageNewBlock.contains("threadChannelId = data.threadChannelId")
        )
    }

    @Test
    fun `ViewModel passes replyCount when constructing Message from socket event`() {
        val messageNewBlock = viewModelSource.substringAfter("is SocketIOManager.SocketEvent.MessageNew")
            .substringBefore("is SocketIOManager.SocketEvent.MessageUpdated")
        assertTrue(
            "ViewModel must pass replyCount to Message constructor",
            messageNewBlock.contains("replyCount = data.replyCount")
        )
    }
}
