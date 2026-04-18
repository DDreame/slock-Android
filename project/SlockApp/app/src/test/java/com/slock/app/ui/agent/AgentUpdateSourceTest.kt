package com.slock.app.ui.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AgentUpdateSourceTest {

    private val vmSource: String = listOf(
        File("src/main/java/com/slock/app/ui/agent/AgentDetailViewModel.kt"),
        File("app/src/main/java/com/slock/app/ui/agent/AgentDetailViewModel.kt")
    ).first { it.exists() }.readText()

    private val screenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/agent/AgentDetailScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/agent/AgentDetailScreen.kt")
    ).first { it.exists() }.readText()

    private val navSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `AgentDetailUiState has updateFeedbackMessage field`() {
        assertTrue(
            "AgentDetailUiState must have updateFeedbackMessage for save feedback",
            vmSource.contains("val updateFeedbackMessage")
        )
    }

    @Test
    fun `AgentDetailUiState has isSaving field`() {
        assertTrue(
            "AgentDetailUiState must have isSaving for save-in-progress state",
            vmSource.contains("val isSaving")
        )
    }

    @Test
    fun `AgentDetailViewModel has updateAgent method`() {
        assertTrue(
            "AgentDetailViewModel must have updateAgent method",
            vmSource.contains("fun updateAgent(")
        )
    }

    @Test
    fun `updateAgent calls agentRepository updateAgent`() {
        val updateBlock = vmSource
            .substringAfter("fun updateAgent(")
            .substringBefore("fun consumeUpdateFeedback()")
        assertTrue(
            "updateAgent must call agentRepository.updateAgent",
            updateBlock.contains("agentRepository.updateAgent")
        )
    }

    @Test
    fun `AgentDetailViewModel has consumeUpdateFeedback method`() {
        assertTrue(
            "AgentDetailViewModel must have consumeUpdateFeedback to clear message after Toast",
            vmSource.contains("fun consumeUpdateFeedback()")
        )
    }

    @Test
    fun `AgentDetailScreen has onUpdateAgent callback`() {
        assertTrue(
            "AgentDetailScreen must have onUpdateAgent callback for edit flow",
            screenSource.contains("onUpdateAgent")
        )
    }

    @Test
    fun `AgentDetailScreen has onConsumeUpdateFeedback callback`() {
        assertTrue(
            "AgentDetailScreen must have onConsumeUpdateFeedback callback",
            screenSource.contains("onConsumeUpdateFeedback")
        )
    }

    @Test
    fun `AgentDetailScreen contains Edit action button`() {
        assertTrue(
            "AgentDetailScreen must have an Edit action button",
            screenSource.contains("\"Edit\"")
        )
    }

    @Test
    fun `AgentDetailScreen uses AgentSettingsContent for edit form`() {
        assertTrue(
            "AgentDetailScreen must reuse AgentSettingsContent for the edit form",
            screenSource.contains("AgentSettingsContent")
        )
    }

    @Test
    fun `AgentDetailScreen uses ModalBottomSheet for edit`() {
        assertTrue(
            "AgentDetailScreen must use ModalBottomSheet to show edit form",
            screenSource.contains("ModalBottomSheet")
        )
    }

    @Test
    fun `NavHost wires onUpdateAgent to viewModel`() {
        val agentDetailBlock = navSource
            .substringAfter("Routes.AGENT_DETAIL")
            .substringBefore("Routes.MACHINE_LIST")
        assertTrue(
            "NavHost must wire onUpdateAgent for AgentDetailScreen",
            agentDetailBlock.contains("onUpdateAgent")
        )
    }

    @Test
    fun `NavHost wires onConsumeUpdateFeedback to viewModel`() {
        val agentDetailBlock = navSource
            .substringAfter("Routes.AGENT_DETAIL")
            .substringBefore("Routes.MACHINE_LIST")
        assertTrue(
            "NavHost must wire onConsumeUpdateFeedback for AgentDetailScreen",
            agentDetailBlock.contains("onConsumeUpdateFeedback")
        )
    }

    @Test
    fun `updateAgent success updates agent in state`() {
        val updateBlock = vmSource
            .substringAfter("fun updateAgent(")
            .substringBefore("fun consumeUpdateFeedback()")
        assertTrue(
            "updateAgent success must update agent in state",
            updateBlock.contains("agent =")
        )
    }

    @Test
    fun `updateAgent sets feedback message on success and failure`() {
        val updateBlock = vmSource
            .substringAfter("fun updateAgent(")
            .substringBefore("fun consumeUpdateFeedback()")
        assertTrue(
            "updateAgent must set updateFeedbackMessage",
            updateBlock.contains("updateFeedbackMessage =")
        )
    }

    @Test
    fun `existing reset flow is preserved`() {
        assertTrue(
            "resetAgent method must still exist",
            vmSource.contains("fun resetAgent()")
        )
        assertTrue(
            "consumeResetFeedback must still exist",
            vmSource.contains("fun consumeResetFeedback()")
        )
    }

    @Test
    fun `existing action buttons are preserved`() {
        assertTrue(
            "DM action button must still exist",
            screenSource.contains("\"DM\"")
        )
        assertTrue(
            "Reset action button must still exist",
            screenSource.contains("\"Reset\"")
        )
    }

    @Test
    fun `AgentDetailScreen does not expose onDeleteAgent`() {
        assertFalse(
            "AgentDetailScreen must not expose delete as a new user-reachable action",
            screenSource.contains("onDeleteAgent")
        )
    }

    @Test
    fun `AgentSettingsContent onDelete is nullable for reuse without delete`() {
        val listSource: String = listOf(
            File("src/main/java/com/slock/app/ui/agent/AgentListScreen.kt"),
            File("app/src/main/java/com/slock/app/ui/agent/AgentListScreen.kt")
        ).first { it.exists() }.readText()
        assertTrue(
            "AgentSettingsContent onDelete must be nullable to hide delete button when reused",
            listSource.contains("onDelete: (() -> Unit)? = null")
        )
    }
}
