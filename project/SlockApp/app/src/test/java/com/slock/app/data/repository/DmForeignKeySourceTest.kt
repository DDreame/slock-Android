package com.slock.app.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DmForeignKeySourceTest {

    private val messageRepoSource: String = listOf(
        File("src/main/java/com/slock/app/data/repository/MessageRepository.kt"),
        File("app/src/main/java/com/slock/app/data/repository/MessageRepository.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `MessageRepositoryImpl depends on ChannelDao`() {
        assertTrue(
            "MessageRepositoryImpl constructor must include ChannelDao for parent channel hydration",
            messageRepoSource.contains("channelDao: ChannelDao")
        )
    }

    @Test
    fun `ensureChannelExists guard method is defined`() {
        assertTrue(
            "MessageRepositoryImpl must define ensureChannelExists to guard FK constraint",
            messageRepoSource.contains("ensureChannelExists")
        )
    }

    @Test
    fun `ensureChannelExists checks channel existence via ChannelDao`() {
        assertTrue(
            "ensureChannelExists must query channelDao.getChannelById",
            messageRepoSource.contains("channelDao.getChannelById")
        )
    }

    @Test
    fun `ensureChannelExists inserts stub ChannelEntity when missing`() {
        assertTrue(
            "ensureChannelExists must insert ChannelEntity when channel is missing",
            messageRepoSource.contains("channelDao.insertChannel")
        )
    }

    @Test
    fun `sendMessage calls ensureChannelExists before insert`() {
        val sendMessageBlock = messageRepoSource
            .substringAfter("override suspend fun sendMessage")
            .substringBefore("override suspend fun getMessages")
        assertTrue(
            "sendMessage must call ensureChannelExists before messageDao.insertMessage",
            sendMessageBlock.contains("ensureChannelExists")
        )
    }

    @Test
    fun `getMessages calls ensureChannelExists before insert`() {
        val getMessagesBlock = messageRepoSource
            .substringAfter("override suspend fun getMessages")
            .substringBefore("override suspend fun refreshMessages")
        assertTrue(
            "getMessages must call ensureChannelExists before messageDao.insertMessages",
            getMessagesBlock.contains("ensureChannelExists")
        )
    }

    @Test
    fun `refreshMessages calls ensureChannelExists before insert`() {
        val refreshBlock = messageRepoSource
            .substringAfter("override suspend fun refreshMessages")
            .substringBefore("override suspend fun isCachedMessagesFresh")
        assertTrue(
            "refreshMessages must call ensureChannelExists before messageDao.insertMessages",
            refreshBlock.contains("ensureChannelExists")
        )
    }

    @Test
    fun `existing getMessages cache path is preserved`() {
        assertTrue(
            "getMessages must still check messageDao cache before fetching from API",
            messageRepoSource.contains("messageDao.getMessagesByChannel")
        )
    }

    @Test
    fun `existing refreshMessages markCacheFresh call is preserved`() {
        val refreshBlock = messageRepoSource
            .substringAfter("override suspend fun refreshMessages")
            .substringBefore("override suspend fun isCachedMessagesFresh")
        assertTrue(
            "refreshMessages must still call markCacheFresh after insert",
            refreshBlock.contains("markCacheFresh")
        )
    }
}
