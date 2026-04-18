package com.slock.app.ui.message

import com.slock.app.data.model.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MessageListPerformanceTest {

    private fun readSource(vararg candidates: String): String {
        return candidates
            .map(::File)
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("Unable to read source from candidates: ${candidates.joinToString()}")
    }

    private val source = readSource(
        "src/main/java/com/slock/app/ui/message/MessageListScreen.kt",
        "app/src/main/java/com/slock/app/ui/message/MessageListScreen.kt"
    )

    @Test
    fun `messageListItemKey prefers message id`() {
        val message = Message(id = "msg-1", seq = 42, createdAt = "2026-04-18T00:00:00Z", senderId = "u1")

        assertEquals("msg-1", messageListItemKey(message))
    }

    @Test
    fun `messageListItemKey falls back to stable composite when id missing`() {
        val message = Message(id = null, seq = 42, createdAt = "2026-04-18T00:00:00Z", senderId = "u1")

        assertEquals("42:2026-04-18T00:00:00Z:u1", messageListItemKey(message))
    }

    @Test
    fun `buildMessagesById indexes only messages with ids`() {
        val kept = Message(id = "quoted-1", content = "quoted")
        val ignored = Message(id = null, content = "draft")

        val result = buildMessagesById(listOf(kept, ignored))

        assertEquals(setOf("quoted-1"), result.keys)
        assertSame(kept, result["quoted-1"])
    }

    @Test
    fun `resolveQuotedMessage returns map lookup result`() {
        val quoted = Message(id = "quoted-1", content = "quoted")
        val messagesById = buildMessagesById(listOf(quoted))

        assertSame(quoted, resolveQuotedMessage("quoted-1", messagesById))
        assertNull(resolveQuotedMessage("missing", messagesById))
        assertNull(resolveQuotedMessage(null, messagesById))
    }

    @Test
    fun `LazyColumn uses keyed itemsIndexed for messages`() {
        val listBlock = source
            .substringAfter("LazyColumn(")
            .substringBefore("if (state.isLoadingMore)")

        assertTrue(
            "MessageList must use itemsIndexed for direct message iteration",
            listBlock.contains("itemsIndexed(")
        )
        assertTrue(
            "MessageList must wire stable item keys through messageListItemKey",
            listBlock.contains("key = { _, message -> messageListItemKey(message) }")
        )
    }

    @Test
    fun `quoted message lookup uses precomputed map not linear find in item render`() {
        val listBlock = source
            .substringAfter("val messagesById = remember(state.messages)")
            .substringBefore("if (state.isLoadingMore)")

        assertTrue(
            "MessageList must precompute messagesById before item rendering",
            listBlock.contains("buildMessagesById(state.messages)")
        )
        assertTrue(
            "MessageList must resolve quoted messages via O(1) helper",
            listBlock.contains("resolveQuotedMessage(message.parentMessageId, messagesById)")
        )
        assertFalse(
            "MessageList item rendering must not linearly scan state.messages for quoted messages",
            listBlock.contains("state.messages.find { it.id == message.parentMessageId }")
        )
    }
}
