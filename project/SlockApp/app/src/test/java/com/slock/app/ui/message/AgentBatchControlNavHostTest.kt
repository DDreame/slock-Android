package com.slock.app.ui.message

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AgentBatchControlNavHostTest {

    private val navHostSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    private val messagesBlock: String = navHostSource
        .substringAfter("// Message List Screen")
        .substringBefore("composable(Routes.SAVED_CHANNELS)")

    @Test
    fun `NavHost MESSAGES composable creates ChannelViewModel`() {
        assertTrue(
            "MESSAGES composable must create ChannelViewModel for batch control",
            messagesBlock.contains("ChannelViewModel") && messagesBlock.contains("hiltViewModel()")
        )
    }

    @Test
    fun `NavHost loads channel agents on launch`() {
        assertTrue(
            "MESSAGES composable must call loadChannelAgents",
            messagesBlock.contains("loadChannelAgents(channelId)")
        )
    }

    @Test
    fun `NavHost collects channelAgents state`() {
        assertTrue(
            "MESSAGES composable must collect channelAgents state",
            messagesBlock.contains("channelAgents") && messagesBlock.contains("collectAsState()")
        )
    }

    @Test
    fun `NavHost passes channelAgents to MessageListScreen`() {
        assertTrue(
            "MESSAGES composable must pass channelAgents to MessageListScreen",
            messagesBlock.contains("channelAgents = channelAgents")
        )
    }

    @Test
    fun `NavHost wires onStopAllAgents with stopAllChannelAgents`() {
        assertTrue(
            "MESSAGES composable must wire onStopAllAgents",
            messagesBlock.contains("onStopAllAgents") && messagesBlock.contains("stopAllChannelAgents")
        )
    }

    @Test
    fun `NavHost wires onResumeAllAgents with resumeAllChannelAgents`() {
        assertTrue(
            "MESSAGES composable must wire onResumeAllAgents",
            messagesBlock.contains("onResumeAllAgents") && messagesBlock.contains("resumeAllChannelAgents")
        )
    }

    @Test
    fun `NavHost passes channelName to MessageListScreen`() {
        assertTrue(
            "MESSAGES composable must pass channelName_raw",
            messagesBlock.contains("channelName_raw")
        )
    }
}
