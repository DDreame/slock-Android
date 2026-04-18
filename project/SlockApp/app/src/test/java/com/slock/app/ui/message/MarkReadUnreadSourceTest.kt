package com.slock.app.ui.message

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MarkReadUnreadSourceTest {

    private val apiSource: String = listOf(
        File("src/main/java/com/slock/app/data/api/ApiService.kt"),
        File("app/src/main/java/com/slock/app/data/api/ApiService.kt")
    ).first { it.exists() }.readText()

    private val repoSource: String = listOf(
        File("src/main/java/com/slock/app/data/repository/ChannelRepository.kt"),
        File("app/src/main/java/com/slock/app/data/repository/ChannelRepository.kt")
    ).first { it.exists() }.readText()

    private val vmSource: String = listOf(
        File("src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt")
    ).first { it.exists() }.readText()

    private val screenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt")
    ).first { it.exists() }.readText()

    private val navSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    // --- API layer ---

    @Test
    fun `ApiService has markChannelUnread endpoint`() {
        assertTrue(
            "ApiService must have markChannelUnread for POST channels/{channelId}/unread",
            apiSource.contains("fun markChannelUnread(")
        )
    }

    @Test
    fun `ApiService markChannelUnread uses POST method`() {
        val beforeMethod = apiSource.substringBefore("fun markChannelUnread(")
        val lastAnnotation = beforeMethod.trimEnd().lines().last()
        assertTrue(
            "markChannelUnread must use @POST annotation",
            lastAnnotation.contains("@POST")
        )
    }

    @Test
    fun `ApiService markChannelUnread targets correct path`() {
        assertTrue(
            "markChannelUnread must target channels/{channelId}/unread",
            apiSource.contains("channels/{channelId}/unread")
        )
    }

    // --- Repository layer ---

    @Test
    fun `ChannelRepository interface has markChannelUnread`() {
        val interfaceBlock = repoSource
            .substringAfter("interface ChannelRepository")
            .substringBefore("class ChannelRepositoryImpl")
        assertTrue(
            "ChannelRepository interface must declare markChannelUnread",
            interfaceBlock.contains("suspend fun markChannelUnread(")
        )
    }

    @Test
    fun `ChannelRepositoryImpl implements markChannelUnread`() {
        val implBlock = repoSource.substringAfter("class ChannelRepositoryImpl")
        assertTrue(
            "ChannelRepositoryImpl must implement markChannelUnread",
            implBlock.contains("override suspend fun markChannelUnread(")
        )
    }

    @Test
    fun `ChannelRepositoryImpl markChannelUnread calls apiService`() {
        val implBlock = repoSource.substringAfter("override suspend fun markChannelUnread(")
            .substringBefore("override suspend fun getDMs(")
        assertTrue(
            "markChannelUnread must call apiService.markChannelUnread",
            implBlock.contains("apiService.markChannelUnread")
        )
    }

    // --- ViewModel layer ---

    @Test
    fun `ChannelViewModel has markAsRead method`() {
        assertTrue(
            "ChannelViewModel must have markAsRead method",
            vmSource.contains("fun markAsRead(")
        )
    }

    @Test
    fun `ChannelViewModel has markAsUnread method`() {
        assertTrue(
            "ChannelViewModel must have markAsUnread method",
            vmSource.contains("fun markAsUnread(")
        )
    }

    @Test
    fun `markAsRead clears unread count from state`() {
        val markReadBlock = vmSource
            .substringAfter("fun markAsRead(")
            .substringBefore("fun markAsUnread(")
        assertTrue(
            "markAsRead must remove channelId from unreadCounts",
            markReadBlock.contains("unreadCounts = it.unreadCounts - channelId")
        )
    }

    @Test
    fun `markAsRead calls markChannelRead on repository`() {
        val markReadBlock = vmSource
            .substringAfter("fun markAsRead(")
            .substringBefore("fun markAsUnread(")
        assertTrue(
            "markAsRead must call channelRepository.markChannelRead",
            markReadBlock.contains("channelRepository.markChannelRead")
        )
    }

    @Test
    fun `markAsUnread calls markChannelUnread on repository`() {
        val markUnreadBlock = vmSource
            .substringAfter("fun markAsUnread(")
            .substringBefore("fun clearCurrentChannel(")
        assertTrue(
            "markAsUnread must call channelRepository.markChannelUnread",
            markUnreadBlock.contains("channelRepository.markChannelUnread")
        )
    }

    @Test
    fun `markAsUnread refreshes unread counts on success`() {
        val markUnreadBlock = vmSource
            .substringAfter("fun markAsUnread(")
            .substringBefore("fun clearCurrentChannel(")
        assertTrue(
            "markAsUnread must reload unread counts after success",
            markUnreadBlock.contains("getUnreadChannels")
        )
    }

    @Test
    fun `markAsRead sets feedback message on failure`() {
        val markReadBlock = vmSource
            .substringAfter("fun markAsRead(")
            .substringBefore("fun markAsUnread(")
        assertTrue(
            "markAsRead must set actionFeedbackMessage on failure",
            markReadBlock.contains("actionFeedbackMessage =")
        )
    }

    @Test
    fun `markAsUnread sets feedback message on failure`() {
        val markUnreadBlock = vmSource
            .substringAfter("fun markAsUnread(")
            .substringBefore("fun clearCurrentChannel(")
        assertTrue(
            "markAsUnread must set actionFeedbackMessage on failure",
            markUnreadBlock.contains("actionFeedbackMessage =")
        )
    }

    // --- Screen layer ---

    @Test
    fun `MessageListScreen has onMarkAsRead callback`() {
        assertTrue(
            "MessageListScreen must have onMarkAsRead callback parameter",
            screenSource.contains("onMarkAsRead")
        )
    }

    @Test
    fun `MessageListScreen has onMarkAsUnread callback`() {
        assertTrue(
            "MessageListScreen must have onMarkAsUnread callback parameter",
            screenSource.contains("onMarkAsUnread")
        )
    }

    @Test
    fun `MessageActionSheet contains Mark as Read action`() {
        assertTrue(
            "MessageActionSheet must show Mark as Read menu item",
            screenSource.contains("\"Mark as Read\"")
        )
    }

    @Test
    fun `MessageActionSheet contains Mark as Unread action`() {
        assertTrue(
            "MessageActionSheet must show Mark as Unread menu item",
            screenSource.contains("\"Mark as Unread\"")
        )
    }

    @Test
    fun `MessageListScreen has markReadFeedbackMessage param`() {
        assertTrue(
            "MessageListScreen must accept markReadFeedbackMessage for Toast feedback",
            screenSource.contains("markReadFeedbackMessage")
        )
    }

    // --- NavHost wiring ---

    @Test
    fun `NavHost wires onMarkAsRead to channelVM`() {
        val messagesBlock = navSource
            .substringAfter("MessageListScreen(")
            .substringBefore("composable(Routes.SAVED_CHANNELS)")
        assertTrue(
            "NavHost must wire onMarkAsRead for MessageListScreen",
            messagesBlock.contains("onMarkAsRead")
        )
    }

    @Test
    fun `NavHost wires onMarkAsUnread to channelVM`() {
        val messagesBlock = navSource
            .substringAfter("MessageListScreen(")
            .substringBefore("composable(Routes.SAVED_CHANNELS)")
        assertTrue(
            "NavHost must wire onMarkAsUnread for MessageListScreen",
            messagesBlock.contains("onMarkAsUnread")
        )
    }

    @Test
    fun `NavHost passes markReadFeedbackMessage from channelState`() {
        val messagesBlock = navSource
            .substringAfter("MessageListScreen(")
            .substringBefore("composable(Routes.SAVED_CHANNELS)")
        assertTrue(
            "NavHost must pass markReadFeedbackMessage from channelState",
            messagesBlock.contains("markReadFeedbackMessage")
        )
    }

    @Test
    fun `NavHost wires onMarkReadFeedbackShown to channelVM`() {
        val messagesBlock = navSource
            .substringAfter("MessageListScreen(")
            .substringBefore("composable(Routes.SAVED_CHANNELS)")
        assertTrue(
            "NavHost must wire onMarkReadFeedbackShown to consume feedback",
            messagesBlock.contains("onMarkReadFeedbackShown")
        )
    }

    // --- Preservation tests ---

    @Test
    fun `existing clearUnreadCount method preserved`() {
        assertTrue(
            "clearUnreadCount must still exist for entering-channel auto-clear",
            vmSource.contains("fun clearUnreadCount(")
        )
    }

    @Test
    fun `existing action sheet items preserved`() {
        assertTrue("Quote Reply must still exist", screenSource.contains("\"Quote Reply\""))
        assertTrue("Copy Markdown must still exist", screenSource.contains("\"Copy Markdown\""))
    }
}
