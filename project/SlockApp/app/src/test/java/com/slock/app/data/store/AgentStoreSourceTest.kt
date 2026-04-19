package com.slock.app.data.store

import org.junit.Assert.assertTrue
import org.junit.Test

class AgentStoreSourceTest {

    private val storeSource = java.io.File("src/main/java/com/slock/app/data/store/AgentStore.kt").readText()
    private val vmSource = java.io.File("src/main/java/com/slock/app/ui/agent/AgentViewModel.kt").readText()

    // ── Store Shape ──

    @Test
    fun `AgentStore is Singleton`() {
        assertTrue(
            "AgentStore must be @Singleton",
            storeSource.contains("@Singleton")
        )
    }

    @Test
    fun `AgentStore has agentsById StateFlow`() {
        assertTrue(
            "AgentStore must expose agentsById",
            storeSource.contains("agentsById")
        )
        assertTrue(
            "agentsById must be Map<String, Agent>",
            storeSource.contains("Map<String, Agent>")
        )
    }

    @Test
    fun `AgentStore has activityByAgentId StateFlow`() {
        assertTrue(
            "AgentStore must expose activityByAgentId",
            storeSource.contains("activityByAgentId")
        )
    }

    @Test
    fun `AgentStore has runtimeStatus StateFlow`() {
        assertTrue(
            "AgentStore must expose runtimeStatus",
            storeSource.contains("runtimeStatus")
        )
        assertTrue(
            "runtimeStatus must use AgentRuntimeStatus",
            storeSource.contains("AgentRuntimeStatus")
        )
    }

    // ── Reducer methods ──

    @Test
    fun `AgentStore has setAgents method`() {
        assertTrue(
            "AgentStore must have setAgents",
            storeSource.contains("fun setAgents(")
        )
    }

    @Test
    fun `AgentStore has removeAgent method`() {
        assertTrue(
            "AgentStore must have removeAgent",
            storeSource.contains("fun removeAgent(")
        )
    }

    @Test
    fun `AgentStore has updateActivity method`() {
        assertTrue(
            "AgentStore must have updateActivity",
            storeSource.contains("fun updateActivity(")
        )
    }

    @Test
    fun `AgentStore has updateRuntimeStatus method`() {
        assertTrue(
            "AgentStore must have updateRuntimeStatus",
            storeSource.contains("fun updateRuntimeStatus(")
        )
    }

    @Test
    fun `AgentStore has upsertAgent method`() {
        assertTrue(
            "AgentStore must have upsertAgent",
            storeSource.contains("fun upsertAgent(")
        )
    }

    // ── Socket observation ──

    @Test
    fun `AgentStore observes socket events`() {
        assertTrue(
            "AgentStore must consume SocketIOManager events",
            storeSource.contains("socketIOManager") || storeSource.contains("SocketIOManager")
        )
    }

    @Test
    fun `AgentStore handles AgentActivity socket event`() {
        assertTrue(
            "AgentStore must handle AgentActivity",
            storeSource.contains("AgentActivity")
        )
    }

    @Test
    fun `AgentStore handles AgentDeleted socket event`() {
        assertTrue(
            "AgentStore must handle AgentDeleted",
            storeSource.contains("AgentDeleted")
        )
    }

    @Test
    fun `AgentStore handles AgentCreated socket event`() {
        assertTrue(
            "AgentStore must handle AgentCreated",
            storeSource.contains("AgentCreated")
        )
    }

    // ── ViewModel reads from store ──

    @Test
    fun `AgentViewModel injects AgentStore`() {
        assertTrue(
            "AgentViewModel must inject AgentStore",
            vmSource.contains("AgentStore") || vmSource.contains("agentStore")
        )
    }

    @Test
    fun `AgentViewModel does not self-observe socket agent events`() {
        val socketBlock = vmSource.substringAfter("class AgentViewModel")
        val hasDirectAgentSocketHandling = socketBlock.contains("is SocketIOManager.SocketEvent.AgentActivity") ||
            socketBlock.contains("is SocketIOManager.SocketEvent.AgentDeleted") ||
            socketBlock.contains("is SocketIOManager.SocketEvent.AgentCreated")
        assertTrue(
            "AgentViewModel must not directly handle agent socket events (moved to AgentStore)",
            !hasDirectAgentSocketHandling
        )
    }

    // ── AgentRuntimeStatus ──

    @Test
    fun `AgentRuntimeStatus data class exists in store package`() {
        assertTrue(
            "AgentRuntimeStatus must exist",
            storeSource.contains("data class AgentRuntimeStatus")
        )
    }
}
