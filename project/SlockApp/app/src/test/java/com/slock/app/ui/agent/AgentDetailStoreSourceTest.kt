package com.slock.app.ui.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AgentDetailStoreSourceTest {

    private fun readSource(vararg candidates: String): String {
        return candidates.map { File(it) }.first { it.exists() }.readText()
    }

    private val detailVmSource = readSource(
        "src/main/java/com/slock/app/ui/agent/AgentDetailViewModel.kt",
        "app/src/main/java/com/slock/app/ui/agent/AgentDetailViewModel.kt"
    )

    // ── Constructor injection ──

    @Test
    fun `AgentDetailViewModel injects AgentStore`() {
        assertTrue(
            "AgentDetailViewModel constructor must include AgentStore",
            detailVmSource.contains("agentStore") && detailVmSource.contains("AgentStore")
        )
    }

    // ── Activity derived from store ──

    @Test
    fun `AgentDetailViewModel observes store activity`() {
        assertTrue(
            "AgentDetailViewModel must observe agentStore.activityByAgentId",
            detailVmSource.contains("activityByAgentId")
        )
    }

    // ── Socket observation scoped to activityLog only ──

    @Test
    fun `observeSocket does not update latestActivity`() {
        val socketBlock = detailVmSource
            .substringAfter("private fun observeSocket()")
            .substringBefore("private fun ")
        assertFalse(
            "observeSocket must not set latestActivity directly",
            socketBlock.contains("latestActivity = event")
                || socketBlock.contains("latestActivity = it")
        )
    }

    @Test
    fun `observeSocket does not update latestActivityDetail`() {
        val socketBlock = detailVmSource
            .substringAfter("private fun observeSocket()")
            .substringBefore("private fun ")
        assertFalse(
            "observeSocket must not set latestActivityDetail directly",
            socketBlock.contains("latestActivityDetail = event")
                || socketBlock.contains("latestActivityDetail = it")
        )
    }

    @Test
    fun `observeSocket still prepends to activityLog`() {
        val socketBlock = detailVmSource
            .substringAfter("private fun observeSocket()")
            .substringBefore("private fun ")
        assertTrue(
            "observeSocket must still prepend to activityLog",
            socketBlock.contains("activityLog")
        )
    }

    // ── start/stop/reset delegate to store ──

    @Test
    fun `startAgent calls agentStore clearActivity`() {
        val startBlock = detailVmSource
            .substringAfter("fun startAgent()")
            .substringBefore("\n    fun ")
        assertTrue(
            "startAgent must call agentStore.clearActivity",
            startBlock.contains("agentStore.clearActivity") || startBlock.contains("clearActivity(agentId)")
        )
    }

    @Test
    fun `stopAgent calls agentStore clearActivity`() {
        val stopBlock = detailVmSource
            .substringAfter("fun stopAgent()")
            .substringBefore("\n    fun ")
        assertTrue(
            "stopAgent must call agentStore.clearActivity",
            stopBlock.contains("agentStore.clearActivity") || stopBlock.contains("clearActivity(agentId)")
        )
    }

    @Test
    fun `resetAgent calls agentStore clearActivity`() {
        val resetBlock = detailVmSource
            .substringAfter("fun resetAgent()")
            .substringBefore("\n    fun ")
        assertTrue(
            "resetAgent must call agentStore.clearActivity",
            resetBlock.contains("agentStore.clearActivity") || resetBlock.contains("clearActivity(agentId)")
        )
    }

    // ── loadAgent does not locally seed latestActivity ──

    @Test
    fun `loadAgent does not directly set latestActivity in state copy`() {
        val loadBlock = detailVmSource
            .substringAfter("private fun loadAgent()")
            .substringBefore("\n    private fun ")
        assertFalse(
            "loadAgent must not set latestActivity directly (should go through store)",
            loadBlock.contains("latestActivity = agent?.activity")
        )
    }
}
