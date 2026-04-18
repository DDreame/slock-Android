package com.slock.app.ui.message

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MessageThreadIndicatorUiTest {

    private val messageListSource: String = listOf(
        File("src/main/java/com/slock/app/ui/message/MessageListScreen.kt"),
        File("app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt")
    ).first { it.exists() }.readText()

    @Test
    fun `message thread preview still renders reply count`() {
        assertTrue(
            "Message thread preview must continue to render reply count",
            messageListSource.contains("message.replyCount") &&
                messageListSource.contains("replyText") &&
                messageListSource.contains("message.replyCount > 0")
        )
    }

    @Test
    fun `message thread preview renders lastReplyAt when present`() {
        assertTrue(
            "Message thread preview must format and render lastReplyAt",
            messageListSource.contains("message.lastReplyAt") &&
                messageListSource.contains("formatMessageThreadTime")
        )
    }
}
