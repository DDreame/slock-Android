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

    private val repoSource: String = listOf(
        File("src/main/java/com/slock/app/data/repository/ChannelRepository.kt"),
        File("app/src/main/java/com/slock/app/data/repository/ChannelRepository.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `ChannelRepository interface declares getServerIdForChannel`() {
        val interfaceBlock = repoSource.substringAfter("interface ChannelRepository")
            .substringBefore("class ChannelRepositoryImpl")
        assertTrue(
            "ChannelRepository must declare getServerIdForChannel",
            interfaceBlock.contains("getServerIdForChannel")
        )
    }

    @Test
    fun `ChannelRepositoryImpl overrides getServerIdForChannel`() {
        val implBlock = repoSource.substringAfter("class ChannelRepositoryImpl")
        assertTrue(
            "ChannelRepositoryImpl must implement getServerIdForChannel",
            implBlock.contains("override suspend fun getServerIdForChannel")
        )
    }

    @Test
    fun `getServerIdForChannel uses channelDao getChannelById`() {
        val implBlock = repoSource.substringAfter("override suspend fun getServerIdForChannel")
            .substringBefore("}")
        assertTrue(
            "getServerIdForChannel must use channelDao.getChannelById",
            implBlock.contains("channelDao.getChannelById")
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
    fun `retryLoadMessages resolves serverId via channelRepository`() {
        val retryBlock = vmSource
            .substringAfter("fun retryLoadMessages()")
            .substringBefore("fun loadMoreMessages()")
        assertTrue(
            "retryLoadMessages must use channelRepository.getServerIdForChannel for precise mapping",
            retryBlock.contains("channelRepository.getServerIdForChannel")
        )
    }

    @Test
    fun `retryLoadMessages does not use serverRepository`() {
        val retryBlock = vmSource
            .substringAfter("fun retryLoadMessages()")
            .substringBefore("fun loadMoreMessages()")
        assertFalse(
            "retryLoadMessages must not blindly use serverRepository.getServers()",
            retryBlock.contains("serverRepository")
        )
    }

    @Test
    fun `retryLoadMessages still calls loadMessages after resolution`() {
        val retryBlock = vmSource
            .substringAfter("fun retryLoadMessages()")
            .substringBefore("fun loadMoreMessages()")
        assertTrue(
            "retryLoadMessages must call loadMessages",
            retryBlock.contains("loadMessages(channelId)")
        )
    }
}
