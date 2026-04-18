package com.slock.app.ui.channel

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AgentBatchControlViewModelTest {

    private val viewModelSource: String = listOf(
        File("src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/channel/ChannelViewModel.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `ChannelViewModel exposes channelAgents StateFlow`() {
        assertTrue(
            "ChannelViewModel must expose channelAgents StateFlow",
            viewModelSource.contains("val channelAgents: StateFlow<List<Agent>>")
        )
    }

    @Test
    fun `loadChannelAgents method exists and calls getChannelMembers`() {
        val method = viewModelSource
            .substringAfter("fun loadChannelAgents(")
            .substringBefore("fun stopAllChannelAgents(")
        assertTrue(
            "loadChannelAgents must call getChannelMembers",
            method.contains("getChannelMembers")
        )
        assertTrue(
            "loadChannelAgents must filter for agents",
            method.contains("agentId != null")
        )
    }

    @Test
    fun `stopAllChannelAgents uses batch API endpoint`() {
        val method = viewModelSource
            .substringAfter("fun stopAllChannelAgents(")
            .substringBefore("fun resumeAllChannelAgents(")
        assertTrue(
            "stopAllChannelAgents must call channelRepository.stopAllChannelAgents",
            method.contains("channelRepository.stopAllChannelAgents")
        )
    }

    @Test
    fun `stopAllChannelAgents updates local state optimistically`() {
        val method = viewModelSource
            .substringAfter("fun stopAllChannelAgents(")
            .substringBefore("fun resumeAllChannelAgents(")
        assertTrue(
            "stopAllChannelAgents must update _channelAgents state",
            method.contains("_channelAgents.update")
        )
    }

    @Test
    fun `resumeAllChannelAgents uses batch API with prompt parameter`() {
        val method = viewModelSource
            .substringAfter("fun resumeAllChannelAgents(")
            .substringBefore("fun consumeActionFeedback(")
        assertTrue(
            "resumeAllChannelAgents must accept prompt parameter",
            method.contains("prompt: String")
        )
        assertTrue(
            "resumeAllChannelAgents must call channelRepository.resumeAllChannelAgents",
            method.contains("channelRepository.resumeAllChannelAgents")
        )
    }

    @Test
    fun `resumeAllChannelAgents updates local state optimistically`() {
        val method = viewModelSource
            .substringAfter("fun resumeAllChannelAgents(")
            .substringBefore("fun consumeActionFeedback(")
        assertTrue(
            "resumeAllChannelAgents must update _channelAgents state",
            method.contains("_channelAgents.update")
        )
    }

    @Test
    fun `stopAllChannelAgents has success and error callbacks`() {
        val method = viewModelSource
            .substringAfter("fun stopAllChannelAgents(")
            .substringBefore("fun resumeAllChannelAgents(")
        assertTrue(
            "stopAllChannelAgents must have onSuccess callback",
            method.contains("onSuccess")
        )
        assertTrue(
            "stopAllChannelAgents must have onError callback",
            method.contains("onError")
        )
    }

    @Test
    fun `resumeAllChannelAgents has success and error callbacks`() {
        val method = viewModelSource
            .substringAfter("fun resumeAllChannelAgents(")
            .substringBefore("fun consumeActionFeedback(")
        assertTrue(
            "resumeAllChannelAgents must have onSuccess callback",
            method.contains("onSuccess")
        )
        assertTrue(
            "resumeAllChannelAgents must have onError callback",
            method.contains("onError")
        )
    }
}
