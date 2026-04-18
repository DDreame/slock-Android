package com.slock.app.ui.message

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DmRetryRecoverySourceTest {

    private val vmSource: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageViewModel.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `MessageViewModel constructor includes ServerRepository`() {
        val constructor = vmSource.substringAfter("class MessageViewModel")
            .substringBefore(") : ViewModel()")
        assertTrue(
            "MessageViewModel must depend on ServerRepository",
            constructor.contains("ServerRepository")
        )
    }

    @Test
    fun `retryLoadMessages checks serverId before calling loadMessages`() {
        val retryBlock = vmSource
            .substringAfter("fun retryLoadMessages()")
            .substringBefore("fun loadMoreMessages()")
        assertTrue(
            "retryLoadMessages must check activeServerHolder.serverId",
            retryBlock.contains("activeServerHolder.serverId")
        )
    }

    @Test
    fun `retryLoadMessages fetches servers when serverId is null`() {
        val retryBlock = vmSource
            .substringAfter("fun retryLoadMessages()")
            .substringBefore("fun loadMoreMessages()")
        assertTrue(
            "retryLoadMessages must call serverRepository.getServers() when serverId is null",
            retryBlock.contains("serverRepository.getServers()")
        )
    }

    @Test
    fun `retryLoadMessages sets serverId from fetched servers`() {
        val retryBlock = vmSource
            .substringAfter("fun retryLoadMessages()")
            .substringBefore("fun loadMoreMessages()")
        assertTrue(
            "retryLoadMessages must set activeServerHolder.serverId from server list",
            retryBlock.contains("activeServerHolder.serverId =")
        )
    }

    @Test
    fun `retryLoadMessages still calls loadMessages after server resolution`() {
        val retryBlock = vmSource
            .substringAfter("fun retryLoadMessages()")
            .substringBefore("fun loadMoreMessages()")
        assertTrue(
            "retryLoadMessages must still call loadMessages",
            retryBlock.contains("loadMessages(channelId)")
        )
    }

    @Test
    fun `retryLoadMessages does not blindly re-call loadMessages without server check`() {
        val retryBlock = vmSource
            .substringAfter("fun retryLoadMessages()")
            .substringBefore("fun loadMoreMessages()")
        assertFalse(
            "retryLoadMessages must not unconditionally call loadMessages without checking serverId first",
            retryBlock.trimStart().startsWith("val channelId = _state.value.channelId\n" +
                "        if (channelId.isNotBlank()) {\n" +
                "            loadMessages(channelId)")
        )
    }
}
