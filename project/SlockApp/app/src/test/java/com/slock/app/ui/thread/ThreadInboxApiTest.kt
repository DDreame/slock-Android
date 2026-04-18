package com.slock.app.ui.thread

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThreadInboxApiTest {

    private val apiServiceSource: String = listOf(
        File("src/main/java/com/slock/app/data/api/ApiService.kt"),
        File("app/src/main/java/com/slock/app/data/api/ApiService.kt")
    ).first { it.exists() }.readText()

    private val threadRepoSource: String = listOf(
        File("src/main/java/com/slock/app/data/repository/ThreadRepository.kt"),
        File("app/src/main/java/com/slock/app/data/repository/ThreadRepository.kt")
    ).first { it.exists() }.readText()

    private val modelSource: String = listOf(
        File("src/main/java/com/slock/app/data/model/ApiResponse.kt"),
        File("app/src/main/java/com/slock/app/data/model/ApiResponse.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `ApiService has followThread endpoint`() {
        assertTrue(
            "ApiService must have POST channels/threads/follow",
            apiServiceSource.contains("channels/threads/follow") && apiServiceSource.contains("fun followThread(")
        )
    }

    @Test
    fun `ApiService has unfollowThread endpoint`() {
        assertTrue(
            "ApiService must have POST channels/threads/unfollow",
            apiServiceSource.contains("channels/threads/unfollow") && apiServiceSource.contains("fun unfollowThread(")
        )
    }

    @Test
    fun `ApiService has markThreadDone endpoint`() {
        assertTrue(
            "ApiService must have POST channels/threads/done",
            apiServiceSource.contains("channels/threads/done") && apiServiceSource.contains("fun markThreadDone(")
        )
    }

    @Test
    fun `ApiService has undoThreadDone endpoint`() {
        assertTrue(
            "ApiService must have POST channels/threads/undone",
            apiServiceSource.contains("channels/threads/undone") && apiServiceSource.contains("fun undoThreadDone(")
        )
    }

    @Test
    fun `FollowThreadRequest data class exists with parentMessageId`() {
        assertTrue(
            "FollowThreadRequest must have parentMessageId field",
            modelSource.contains("data class FollowThreadRequest") &&
                modelSource.contains("val parentMessageId: String")
        )
    }

    @Test
    fun `ThreadChannelIdRequest data class exists with threadChannelId`() {
        assertTrue(
            "ThreadChannelIdRequest must have threadChannelId field",
            modelSource.contains("data class ThreadChannelIdRequest") &&
                modelSource.contains("val threadChannelId: String")
        )
    }

    @Test
    fun `ThreadRepository interface has followThread`() {
        val interfaceBlock = threadRepoSource
            .substringAfter("interface ThreadRepository")
            .substringBefore("class ThreadRepositoryImpl")
        assertTrue(
            "ThreadRepository must declare followThread",
            interfaceBlock.contains("suspend fun followThread(")
        )
    }

    @Test
    fun `ThreadRepository interface has unfollowThread`() {
        val interfaceBlock = threadRepoSource
            .substringAfter("interface ThreadRepository")
            .substringBefore("class ThreadRepositoryImpl")
        assertTrue(
            "ThreadRepository must declare unfollowThread",
            interfaceBlock.contains("suspend fun unfollowThread(")
        )
    }

    @Test
    fun `ThreadRepository interface has markThreadDone`() {
        val interfaceBlock = threadRepoSource
            .substringAfter("interface ThreadRepository")
            .substringBefore("class ThreadRepositoryImpl")
        assertTrue(
            "ThreadRepository must declare markThreadDone",
            interfaceBlock.contains("suspend fun markThreadDone(")
        )
    }

    @Test
    fun `ThreadRepository interface has undoThreadDone`() {
        val interfaceBlock = threadRepoSource
            .substringAfter("interface ThreadRepository")
            .substringBefore("class ThreadRepositoryImpl")
        assertTrue(
            "ThreadRepository must declare undoThreadDone",
            interfaceBlock.contains("suspend fun undoThreadDone(")
        )
    }

    @Test
    fun `ThreadRepositoryImpl implements followThread calling apiService`() {
        val implBlock = threadRepoSource.substringAfter("class ThreadRepositoryImpl")
        assertTrue(
            "ThreadRepositoryImpl must implement followThread",
            implBlock.contains("override suspend fun followThread(") &&
                implBlock.contains("apiService.followThread")
        )
    }

    @Test
    fun `ThreadRepositoryImpl implements markThreadDone calling apiService`() {
        val implBlock = threadRepoSource.substringAfter("class ThreadRepositoryImpl")
        assertTrue(
            "ThreadRepositoryImpl must implement markThreadDone",
            implBlock.contains("override suspend fun markThreadDone(") &&
                implBlock.contains("apiService.markThreadDone")
        )
    }
}
