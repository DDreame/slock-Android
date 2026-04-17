package com.slock.app.ui.message

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AgentBatchControlUiTest {

    private val messageListSource: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `MessageListScreen accepts channelAgents parameter`() {
        val signature = messageListSource
            .substringAfter("fun MessageListScreen(")
            .substringBefore(") {")
        assertTrue(
            "MessageListScreen must accept channelAgents: List<Agent> parameter",
            signature.contains("channelAgents: List<Agent>")
        )
    }

    @Test
    fun `MessageListScreen accepts onStopAllAgents callback`() {
        val signature = messageListSource
            .substringAfter("fun MessageListScreen(")
            .substringBefore(") {")
        assertTrue(
            "MessageListScreen must accept onStopAllAgents callback",
            signature.contains("onStopAllAgents")
        )
    }

    @Test
    fun `MessageListScreen accepts onResumeAllAgents callback`() {
        val signature = messageListSource
            .substringAfter("fun MessageListScreen(")
            .substringBefore(") {")
        assertTrue(
            "MessageListScreen must accept onResumeAllAgents callback",
            signature.contains("onResumeAllAgents")
        )
    }

    @Test
    fun `MessageListScreen declares showAgentBatchSheet state`() {
        val body = messageListSource
            .substringAfter("fun MessageListScreen(")
        assertTrue(
            "MessageListScreen must declare showAgentBatchSheet state",
            body.contains("showAgentBatchSheet")
        )
    }

    @Test
    fun `ChannelHeader accepts hasAgents parameter`() {
        val signature = messageListSource
            .substringAfter("fun ChannelHeader(")
            .substringBefore(") {")
        assertTrue(
            "ChannelHeader must accept hasAgents parameter",
            signature.contains("hasAgents")
        )
    }

    @Test
    fun `ChannelHeader accepts onAgentControlClick callback`() {
        val signature = messageListSource
            .substringAfter("fun ChannelHeader(")
            .substringBefore(") {")
        assertTrue(
            "ChannelHeader must accept onAgentControlClick callback",
            signature.contains("onAgentControlClick")
        )
    }

    @Test
    fun `ChannelHeader shows robot button when hasAgents is true`() {
        val headerBody = messageListSource
            .substringAfter("fun ChannelHeader(")
            .substringBefore("fun MiniIconButton(")
        assertTrue(
            "ChannelHeader must conditionally show agent control button",
            headerBody.contains("hasAgents") && headerBody.contains("onAgentControlClick")
        )
    }

    @Test
    fun `AgentBatchControlSheet composable exists`() {
        assertTrue(
            "MessageListScreen must contain AgentBatchControlSheet composable",
            messageListSource.contains("fun AgentBatchControlSheet(")
        )
    }

    @Test
    fun `AgentBatchControlSheet uses two-phase flow`() {
        assertTrue(
            "AgentBatchControlSheet must use AgentControlPhase enum",
            messageListSource.contains("AgentControlPhase.CONFIRM_STOP") &&
                messageListSource.contains("AgentControlPhase.STOPPED")
        )
    }

    @Test
    fun `Phase 1 shows Stop All Agents button`() {
        val sheetBlock = messageListSource
            .substringAfter("fun AgentBatchControlSheet(")
            .substringBefore("fun AgentStatusRow(")
        assertTrue(
            "Phase 1 must have Stop All Agents button",
            sheetBlock.contains("STOP ALL AGENTS") && sheetBlock.contains("onStopAll")
        )
    }

    @Test
    fun `Phase 1 shows agent list`() {
        val sheetBlock = messageListSource
            .substringAfter("AgentControlPhase.CONFIRM_STOP")
            .substringBefore("AgentControlPhase.STOPPED")
        assertTrue(
            "Phase 1 must display agent rows",
            sheetBlock.contains("AgentStatusRow(")
        )
    }

    @Test
    fun `Phase 2 shows correction text field`() {
        val phase2Block = messageListSource
            .substringAfter("AgentControlPhase.STOPPED")
            .substringBefore("fun AgentStatusRow(")
        assertTrue(
            "Phase 2 must have correction text field",
            phase2Block.contains("correctionText") && phase2Block.contains("NeoTextField(")
        )
    }

    @Test
    fun `Phase 2 shows Resume All button`() {
        val phase2Block = messageListSource
            .substringAfter("AgentControlPhase.STOPPED")
            .substringBefore("fun AgentStatusRow(")
        assertTrue(
            "Phase 2 must have Resume All button",
            phase2Block.contains("RESUME ALL") && phase2Block.contains("onResumeAllWithCorrection")
        )
    }

    @Test
    fun `Phase 2 shows Keep Stopped button`() {
        val phase2Block = messageListSource
            .substringAfter("AgentControlPhase.STOPPED")
            .substringBefore("fun AgentStatusRow(")
        assertTrue(
            "Phase 2 must have Keep Stopped button",
            phase2Block.contains("KEEP STOPPED") && phase2Block.contains("onKeepStopped")
        )
    }

    @Test
    fun `Resume All sends SOS prompt with correction text`() {
        val sheetBlock = messageListSource
            .substringAfter("fun AgentBatchControlSheet(")
            .substringBefore("fun AgentStatusRow(")
        assertTrue(
            "Resume All must build SOS prompt from correction text",
            sheetBlock.contains("buildSosPrompt(")
        )
    }

    @Test
    fun `buildSosPrompt function exists and formats SOS message`() {
        assertTrue(
            "buildSosPrompt function must exist",
            messageListSource.contains("fun buildSosPrompt(")
        )
        val fnBlock = messageListSource.substringAfter("fun buildSosPrompt(")
        assertTrue(
            "buildSosPrompt must include SOS prefix",
            fnBlock.contains("[SOS]")
        )
        assertTrue(
            "buildSosPrompt must include channel name",
            fnBlock.contains("channelName")
        )
    }

    @Test
    fun `AgentStatusRow composable exists and shows agent info`() {
        assertTrue(
            "MessageListScreen must contain AgentStatusRow composable",
            messageListSource.contains("fun AgentStatusRow(")
        )
        val rowBlock = messageListSource
            .substringAfter("fun AgentStatusRow(")
        assertTrue(
            "AgentStatusRow must display agent name",
            rowBlock.contains("agent.name")
        )
        assertTrue(
            "AgentStatusRow must show active/stopped status",
            rowBlock.contains("agent.status")
        )
    }
}
