package com.slock.app.ui.message

import com.slock.app.data.model.Message
import org.junit.Assert.*
import org.junit.Test

class MessageSearchTest {

    private fun makeMessages(vararg contents: String): List<Message> =
        contents.mapIndexed { i, text ->
            Message(id = "msg-$i", content = text, senderName = "User$i", senderType = "user")
        }

    private fun stateWithSearch(messages: List<Message>, query: String, position: Int = 0): MessageUiState {
        val base = MessageUiState(
            messages = messages,
            isSearchActive = true,
            searchQuery = query,
            currentSearchMatchPosition = position
        )
        return computeSearchMatches(base)
    }

    @Test
    fun `initial state has search inactive`() {
        val state = MessageUiState()
        assertFalse(state.isSearchActive)
        assertEquals("", state.searchQuery)
        assertTrue(state.searchMatchIndices.isEmpty())
        assertEquals(-1, state.currentSearchMatchPosition)
    }

    @Test
    fun `computeSearchMatches finds correct indices`() {
        val messages = makeMessages("Hello world", "Goodbye world", "Hello again", "Nothing here")
        val result = stateWithSearch(messages, "Hello")
        assertEquals(listOf(0, 2), result.searchMatchIndices)
        assertEquals(0, result.currentSearchMatchPosition)
    }

    @Test
    fun `computeSearchMatches is case insensitive`() {
        val messages = makeMessages("HELLO World", "hello world", "no match")
        val result = stateWithSearch(messages, "hello")
        assertEquals(listOf(0, 1), result.searchMatchIndices)
    }

    @Test
    fun `computeSearchMatches returns no matches for blank query`() {
        val messages = makeMessages("Hello", "World")
        val base = MessageUiState(messages = messages, isSearchActive = true, searchQuery = "   ")
        val result = computeSearchMatches(base)
        assertTrue(result.searchMatchIndices.isEmpty())
    }

    @Test
    fun `computeSearchMatches returns no matches when inactive`() {
        val messages = makeMessages("Hello", "World")
        val base = MessageUiState(messages = messages, isSearchActive = false, searchQuery = "Hello")
        val result = computeSearchMatches(base)
        assertTrue(result.searchMatchIndices.isEmpty())
    }

    @Test
    fun `computeSearchMatches returns empty when query not found`() {
        val messages = makeMessages("Hello", "World")
        val result = stateWithSearch(messages, "xyz")
        assertTrue(result.searchMatchIndices.isEmpty())
        assertEquals(-1, result.currentSearchMatchPosition)
    }

    @Test
    fun `computeSearchMatches clamps position when matches shrink`() {
        val messages = makeMessages("alpha beta", "gamma", "alpha gamma")
        val result = stateWithSearch(messages, "alpha", position = 5)
        assertEquals(listOf(0, 2), result.searchMatchIndices)
        assertEquals(0, result.currentSearchMatchPosition)
    }

    @Test
    fun `computeSearchMatches preserves valid position`() {
        val messages = makeMessages("alpha", "beta", "alpha", "gamma")
        val result = stateWithSearch(messages, "alpha", position = 1)
        assertEquals(listOf(0, 2), result.searchMatchIndices)
        assertEquals(1, result.currentSearchMatchPosition)
    }

    @Test
    fun `next cycles through matches`() {
        val messages = makeMessages("a", "b", "a", "c", "a")
        val state = stateWithSearch(messages, "a")
        assertEquals(listOf(0, 2, 4), state.searchMatchIndices)
        assertEquals(0, state.currentSearchMatchPosition)

        val next1 = (state.currentSearchMatchPosition + 1) % state.searchMatchIndices.size
        assertEquals(1, next1)

        val next2 = (next1 + 1) % state.searchMatchIndices.size
        assertEquals(2, next2)

        val next3 = (next2 + 1) % state.searchMatchIndices.size
        assertEquals(0, next3) // wraps
    }

    @Test
    fun `previous cycles backwards`() {
        val messages = makeMessages("a", "b", "a", "c", "a")
        val state = stateWithSearch(messages, "a")
        val size = state.searchMatchIndices.size

        val prev1 = if (0 <= 0) size - 1 else 0 - 1
        assertEquals(2, prev1) // wraps to end

        val prev2 = if (prev1 <= 0) size - 1 else prev1 - 1
        assertEquals(1, prev2)
    }

    @Test
    fun `recompute after new message added`() {
        val original = makeMessages("hello", "world")
        val state1 = stateWithSearch(original, "hello")
        assertEquals(listOf(0), state1.searchMatchIndices)
        assertEquals("msg-0", state1.currentSearchMatchMessageId)

        val newMsg = Message(id = "msg-new", content = "hello again", senderName = "User", senderType = "user")
        val updated = state1.copy(messages = listOf(newMsg) + state1.messages)
        val state2 = computeSearchMatches(updated)
        assertEquals(listOf(0, 1), state2.searchMatchIndices)
        assertEquals(1, state2.currentSearchMatchPosition)
        assertEquals("msg-0", state2.currentSearchMatchMessageId)
    }

    @Test
    fun `recompute after message removed`() {
        val messages = makeMessages("hello", "world", "hello world")
        val state1 = stateWithSearch(messages, "hello")
        assertEquals(listOf(0, 2), state1.searchMatchIndices)

        val reduced = state1.copy(messages = listOf(messages[1], messages[2]))
        val state2 = computeSearchMatches(reduced)
        assertEquals(listOf(1), state2.searchMatchIndices)
    }

    @Test
    fun `current match tracks by message id when new match inserted before`() {
        val messages = makeMessages("alpha", "beta", "alpha end")
        val state1 = stateWithSearch(messages, "alpha")
        assertEquals(listOf(0, 2), state1.searchMatchIndices)
        assertEquals(0, state1.currentSearchMatchPosition)
        assertEquals("msg-0", state1.currentSearchMatchMessageId)

        // Navigate to second match (msg-2, "alpha end")
        val state2 = state1.copy(
            currentSearchMatchPosition = 1,
            currentSearchMatchMessageId = "msg-2"
        )

        // Insert a new matching message at the front
        val newMsg = Message(id = "msg-new", content = "alpha new", senderName = "User", senderType = "user")
        val state3 = computeSearchMatches(state2.copy(messages = listOf(newMsg) + state2.messages))

        // Matches are now [0=msg-new, 1=msg-0, 3=msg-2], current should still be on msg-2
        assertEquals("msg-2", state3.currentSearchMatchMessageId)
        val currentMsgIdx = state3.searchMatchIndices[state3.currentSearchMatchPosition]
        assertEquals("msg-2", state3.messages[currentMsgIdx].id)
    }

    @Test
    fun `current match falls back to 0 when tracked message removed`() {
        val messages = makeMessages("hello", "world", "hello world")
        val state1 = stateWithSearch(messages, "hello")
        // Navigate to second match (msg-2)
        val state2 = state1.copy(currentSearchMatchPosition = 1, currentSearchMatchMessageId = "msg-2")

        // Remove msg-2
        val reduced = state2.copy(messages = listOf(messages[0], messages[1]))
        val state3 = computeSearchMatches(reduced)
        assertEquals(listOf(0), state3.searchMatchIndices)
        assertEquals(0, state3.currentSearchMatchPosition)
        assertEquals("msg-0", state3.currentSearchMatchMessageId)
    }
}
