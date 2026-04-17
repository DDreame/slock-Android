package com.slock.app.ui.message

import com.slock.app.data.model.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageReactionStateTest {

    @Test
    fun `toggleLocalReaction adds selected quick reaction`() {
        val result = toggleLocalReaction(emptyList(), "👍")

        assertEquals(listOf(MessageReactionUiModel(emoji = "👍", count = 1, isSelected = true)), result)
    }

    @Test
    fun `toggleLocalReaction removes single selected reaction`() {
        val current = listOf(MessageReactionUiModel(emoji = "👍", count = 1, isSelected = true))

        val result = toggleLocalReaction(current, "👍")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `toggleLocalReaction decrements existing count when unselecting shared reaction`() {
        val current = listOf(MessageReactionUiModel(emoji = "🎉", count = 3, isSelected = true))

        val result = toggleLocalReaction(current, "🎉")

        assertEquals(listOf(MessageReactionUiModel(emoji = "🎉", count = 2, isSelected = false)), result)
    }

    @Test
    fun `updateReactionOverrides stores reactions per message id`() {
        val message = Message(id = "msg-1", content = "hello")

        val result = updateReactionOverrides(emptyMap(), message, "👀")

        assertEquals(
            listOf(MessageReactionUiModel(emoji = "👀", count = 1, isSelected = true)),
            result["msg-1"]
        )
    }

    @Test
    fun `quickReactionOptions keeps defaults and appends custom emoji once`() {
        val reactions = listOf(
            MessageReactionUiModel(emoji = "🔥", count = 4, isSelected = false),
            MessageReactionUiModel(emoji = "👍", count = 2, isSelected = true)
        )

        val result = quickReactionOptions(reactions)

        assertEquals(listOf("👍", "❤️", "😂", "🎉", "👀", "🔥"), result)
    }
}
