package com.slock.app.ui.message

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ThreadEntryClickTest {

    private val source: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `NeoMessage body click invokes onThreadClick`() {
        val neoMessageBody = source.substringAfter("private fun NeoMessage(")
        val clickableBlock = neoMessageBody.substringAfter(".combinedClickable(")
            .substringBefore("onLongClick")
        assertTrue(
            "combinedClickable onClick must invoke onThreadClick",
            clickableBlock.contains("onThreadClick")
        )
    }

    @Test
    fun `NeoMessage body click does not use empty lambda`() {
        val neoMessageBody = source.substringAfter("private fun NeoMessage(")
        val clickableBlock = neoMessageBody.substringAfter(".combinedClickable(")
            .substringBefore(")")
        assertFalse(
            "combinedClickable onClick must not be an empty lambda",
            clickableBlock.contains("onClick = { }")
        )
    }

    @Test
    fun `onThreadClick is passed to NeoMessage based on threadChannelId`() {
        val callerBlock = source.substringAfter("NeoMessage(")
            .substringBefore(")")
        assertTrue(
            "NeoMessage must receive onThreadClick based on threadChannelId != null",
            callerBlock.contains("onThreadClick") &&
                callerBlock.contains("threadChannelId")
        )
    }
}
