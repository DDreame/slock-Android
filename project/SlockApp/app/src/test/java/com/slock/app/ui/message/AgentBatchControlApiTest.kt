package com.slock.app.ui.message

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AgentBatchControlApiTest {

    private val apiServiceSource: String = listOf(
        File("src/main/java/com/slock/app/data/api/ApiService.kt"),
        File("app/src/main/java/com/slock/app/data/api/ApiService.kt")
    ).first { it.exists() }.readText()

    private val channelRepoSource: String = listOf(
        File("src/main/java/com/slock/app/data/repository/ChannelRepository.kt"),
        File("app/src/main/java/com/slock/app/data/repository/ChannelRepository.kt")
    ).first { it.exists() }.readText()

    private val channelModelSource: String = listOf(
        File("src/main/java/com/slock/app/data/model/Channel.kt"),
        File("app/src/main/java/com/slock/app/data/model/Channel.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `ApiService has stop-all-agents endpoint`() {
        assertTrue(
            "ApiService must have POST channels/{channelId}/stop-all-agents endpoint",
            apiServiceSource.contains("stop-all-agents") && apiServiceSource.contains("fun stopAllChannelAgents(")
        )
    }

    @Test
    fun `ApiService has resume-all-agents endpoint`() {
        assertTrue(
            "ApiService must have POST channels/{channelId}/resume-all-agents endpoint",
            apiServiceSource.contains("resume-all-agents") && apiServiceSource.contains("fun resumeAllChannelAgents(")
        )
    }

    @Test
    fun `resume-all-agents accepts ResumeAllAgentsRequest body`() {
        assertTrue(
            "resumeAllChannelAgents must accept ResumeAllAgentsRequest body",
            apiServiceSource.contains("ResumeAllAgentsRequest")
        )
    }

    @Test
    fun `ResumeAllAgentsRequest data class exists with prompt field`() {
        assertTrue(
            "ResumeAllAgentsRequest must exist with prompt field",
            channelModelSource.contains("data class ResumeAllAgentsRequest") &&
                channelModelSource.contains("val prompt: String")
        )
    }

    @Test
    fun `ChannelRepository interface has stopAllChannelAgents`() {
        val interfaceBlock = channelRepoSource
            .substringAfter("interface ChannelRepository")
            .substringBefore("class ChannelRepositoryImpl")
        assertTrue(
            "ChannelRepository interface must declare stopAllChannelAgents",
            interfaceBlock.contains("suspend fun stopAllChannelAgents(")
        )
    }

    @Test
    fun `ChannelRepository interface has resumeAllChannelAgents`() {
        val interfaceBlock = channelRepoSource
            .substringAfter("interface ChannelRepository")
            .substringBefore("class ChannelRepositoryImpl")
        assertTrue(
            "ChannelRepository interface must declare resumeAllChannelAgents",
            interfaceBlock.contains("suspend fun resumeAllChannelAgents(")
        )
    }

    @Test
    fun `ChannelRepositoryImpl implements stopAllChannelAgents`() {
        val implBlock = channelRepoSource
            .substringAfter("class ChannelRepositoryImpl")
        assertTrue(
            "ChannelRepositoryImpl must implement stopAllChannelAgents calling apiService",
            implBlock.contains("override suspend fun stopAllChannelAgents(") &&
                implBlock.contains("apiService.stopAllChannelAgents")
        )
    }

    @Test
    fun `ChannelRepositoryImpl implements resumeAllChannelAgents`() {
        val implBlock = channelRepoSource
            .substringAfter("class ChannelRepositoryImpl")
        assertTrue(
            "ChannelRepositoryImpl must implement resumeAllChannelAgents calling apiService",
            implBlock.contains("override suspend fun resumeAllChannelAgents(") &&
                implBlock.contains("apiService.resumeAllChannelAgents")
        )
    }
}
