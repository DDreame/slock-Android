package com.slock.app.ui.message

import com.slock.app.data.model.Message
import org.junit.Assert.*
import org.junit.Test

class MessageSearchTest {

    private fun makeMessages(vararg contents: String): List<Message> =
        contents.mapIndexed { i, text ->
            Message(id = "msg-$i", content = text, senderName = "User$i", senderType = "user")
        }

    @Test
    fun `search state starts inactive`() {
        val state = MessageUiState()
        assertFalse(state.isSearchActive)
        assertEquals("", state.searchQuery)
        assertTrue(state.searchMatchIndices.isEmpty())
        assertEquals(-1, state.currentSearchMatchPosition)
    }

    @Test
    fun `toggle search activates and deactivates`() {
        val initial = MessageUiState()
        val activated = initial.copy(isSearchActive = true)
        assertTrue(activated.isSearchActive)

        val deactivated = activated.copy(
            isSearchActive = false,
            searchQuery = "",
            searchMatchIndices = emptyList(),
            currentSearchMatchPosition = -1
        )
        assertFalse(deactivated.isSearchActive)
        assertEquals("", deactivated.searchQuery)
    }

    @Test
    fun `search finds matching messages by index`() {
        val messages = makeMessages("Hello world", "Goodbye world", "Hello again", "Nothing here")
        val query = "Hello"
        val matches = messages.indices.filter { i ->
            messages[i].content.orEmpty().contains(query, ignoreCase = true)
        }
        assertEquals(listOf(0, 2), matches)
    }

    @Test
    fun `search is case insensitive`() {
        val messages = makeMessages("HELLO World", "hello world", "no match")
        val query = "hello"
        val matches = messages.indices.filter { i ->
            messages[i].content.orEmpty().contains(query, ignoreCase = true)
        }
        assertEquals(listOf(0, 1), matches)
    }

    @Test
    fun `blank query returns no matches`() {
        val messages = makeMessages("Hello", "World")
        val query = "   "
        val matches = if (query.isBlank()) emptyList()
        else messages.indices.filter { i ->
            messages[i].content.orEmpty().contains(query, ignoreCase = true)
        }
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `no matches when query not found`() {
        val messages = makeMessages("Hello", "World")
        val query = "xyz"
        val matches = messages.indices.filter { i ->
            messages[i].content.orEmpty().contains(query, ignoreCase = true)
        }
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `next cycles through matches`() {
        val matchIndices = listOf(0, 3, 7)
        var position = 0

        position = (position + 1) % matchIndices.size
        assertEquals(1, position)

        position = (position + 1) % matchIndices.size
        assertEquals(2, position)

        position = (position + 1) % matchIndices.size
        assertEquals(0, position) // wraps around
    }

    @Test
    fun `previous cycles through matches backwards`() {
        val matchIndices = listOf(0, 3, 7)
        var position = 0

        position = if (position <= 0) matchIndices.size - 1 else position - 1
        assertEquals(2, position) // wraps to end

        position = if (position <= 0) matchIndices.size - 1 else position - 1
        assertEquals(1, position)

        position = if (position <= 0) matchIndices.size - 1 else position - 1
        assertEquals(0, position)
    }

    @Test
    fun `next and previous are no-op with empty matches`() {
        val matchIndices = emptyList<Int>()
        val position = -1
        // next
        val nextPos = if (matchIndices.isEmpty()) position else (position + 1) % matchIndices.size
        assertEquals(-1, nextPos)
        // previous
        val prevPos = if (matchIndices.isEmpty()) position else if (position <= 0) matchIndices.size - 1 else position - 1
        assertEquals(-1, prevPos)
    }

    @Test
    fun `search match updates when query changes`() {
        val messages = makeMessages("alpha beta", "gamma", "alpha gamma", "delta")

        val query1 = "alpha"
        val matches1 = messages.indices.filter { messages[it].content.orEmpty().contains(query1, ignoreCase = true) }
        assertEquals(listOf(0, 2), matches1)

        val query2 = "gamma"
        val matches2 = messages.indices.filter { messages[it].content.orEmpty().contains(query2, ignoreCase = true) }
        assertEquals(listOf(1, 2), matches2)
    }

    @Test
    fun `deactivating search clears all search state`() {
        val active = MessageUiState(
            isSearchActive = true,
            searchQuery = "test",
            searchMatchIndices = listOf(0, 2),
            currentSearchMatchPosition = 1
        )
        val cleared = active.copy(
            isSearchActive = false,
            searchQuery = "",
            searchMatchIndices = emptyList(),
            currentSearchMatchPosition = -1
        )
        assertFalse(cleared.isSearchActive)
        assertEquals("", cleared.searchQuery)
        assertTrue(cleared.searchMatchIndices.isEmpty())
        assertEquals(-1, cleared.currentSearchMatchPosition)
    }
}
