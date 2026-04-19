package com.slock.app.ui.message

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MessageSavedMessageSourceTest {

    private val screenSource: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt")
    ).first { it.exists() }.readText()

    private val navSource: String = listOf(
        File("src/main/java/com/slock/app/ui/navigation/NavHost.kt"),
        File("app/src/main/java/com/slock/app/ui/navigation/NavHost.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `MessageActionSheet includes Save Message action`() {
        assertTrue(
            "MessageActionSheet must expose Save Message action",
            screenSource.contains("\"Save Message\"")
        )
        assertTrue(
            "MessageActionSheet must expose Unsave Message action",
            screenSource.contains("\"Unsave Message\"")
        )
    }

    @Test
    fun `NeoMessage action sheet is wired to toggleSavedMessage`() {
        val neoMessageBlock = screenSource
            .substringAfter("private fun NeoMessage(")
            .substringBefore("private fun formatMessageThreadTime(")
        assertTrue(
            "NeoMessage must accept onToggleSavedMessage callback",
            neoMessageBlock.contains("onToggleSavedMessage: () -> Unit = {}")
        )
        assertTrue(
            "NeoMessage must pass onToggleSavedMessage into MessageActionSheet",
            neoMessageBlock.contains("onToggleSavedMessage = { showMenu = false; onToggleSavedMessage() }")
        )
    }

    @Test
    fun `MessageListScreen caller wires message save state into NeoMessage`() {
        val callerBlock = screenSource
            .substringAfter("NeoMessage(")
            .substringBefore("onConvertToTask = { onShowConvertToTask(message, channelName) }")
        assertTrue(
            "MessageListScreen must drive message save state from savedMessageIds",
            callerBlock.contains("isSaved = message.id in state.savedMessageIds")
        )
        assertTrue(
            "MessageListScreen must wire per-message toggle callback",
            callerBlock.contains("onToggleSavedMessage = { onToggleSavedMessage(message) }")
        )
    }

    @Test
    fun `message screen header no longer exposes channel level save toggle`() {
        assertFalse(
            "Message screen should not keep old onToggleSavedChannel callback after message-level migration",
            screenSource.contains("onToggleSavedChannel")
        )
        assertFalse(
            "NavHost should not wire old toggleSavedChannel into MessageListScreen",
            navSource.contains("onToggleSavedChannel = viewModel::toggleSavedChannel")
        )
    }

    @Test
    fun `toggleSavedMessage updates local state before repository call`() {
        val toggleBlock = listOf(
            File("src/main/java/com/slock/app/ui/message/MessageViewModel.kt"),
            File("app/src/main/java/com/slock/app/ui/message/MessageViewModel.kt")
        ).first { it.exists() }.readText()
            .substringAfter("fun toggleSavedMessage(message: Message)")
            .substringBefore("fun consumeSavedMessageFeedback()")

        assertTrue(
            "toggleSavedMessage must update savedMessageIds optimistically",
            toggleBlock.contains("savedMessageIds = if (currentlySaved)")
        )
        assertTrue(
            "toggleSavedMessage must track in-flight ids",
            toggleBlock.contains("savingMessageIds = current.savingMessageIds + messageId")
        )
        assertTrue(
            "optimistic state update must happen before repository call",
            toggleBlock.indexOf("savedMessageIds = if (currentlySaved)") < toggleBlock.indexOf("channelRepository.saveMessage")
        )
        assertTrue(
            "remove path must use message-level repository contract",
            toggleBlock.contains("channelRepository.removeSavedMessage(serverId, messageId)")
        )
    }
}
